package customerinfo.app;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.firestore.Query;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final Context context;

    private FirebaseManager(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
        return instance;
    }

    // ==================== SAVE APPLICATION ====================
    public void saveApplication(JSONObject appData, final SaveCallback callback) {
        try {
            String appNo = appData.getString("application_no");

            // Convert JSON to Map
            Map<String, Object> appMap = jsonToMap(appData);
            appMap.put("createdAt", FieldValue.serverTimestamp());
            appMap.put("updatedAt", FieldValue.serverTimestamp());

            // Save to Firestore
            db.collection("applications")
                    .document(appNo)
                    .set(appMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Application saved: " + appNo);
                            if (callback != null) {
                                callback.onSuccess(appNo);
                            }
                            showToast("Application saved to Firebase");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Save failed: " + e.getMessage());
                            if (callback != null) {
                                callback.onError(e.getMessage());
                            }
                            showToast("Save failed: " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Save error: " + e.getMessage());
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    // ==================== LOAD ALL APPLICATIONS ====================
    public void loadApplications(final LoadCallback callback) {
        db.collection("applications")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            JSONArray apps = new JSONArray();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    JSONObject obj = new JSONObject(document.getData());
                                    obj.put("id", document.getId());
                                    apps.put(obj);
                                } catch (JSONException e) {
                                    Log.e(TAG, "JSON error: " + e.getMessage());
                                }
                            }
                            Log.d(TAG, "Loaded " + apps.length() + " applications");
                            if (callback != null) {
                                callback.onSuccess(apps);
                            }
                        } else {
                            Log.e(TAG, "Load error: " + task.getException());
                            if (callback != null) {
                                callback.onError(task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    // ==================== ADD WORKFLOW STEP ====================
    public void addWorkflowStep(String appNo, Map<String, Object> stepData) {
        DocumentReference appRef = db.collection("applications").document(appNo);

        // Create step with timestamp
        Map<String, Object> step = new HashMap<>(stepData);
        step.put("timestamp", FieldValue.serverTimestamp());

        // Add to workflowSteps array
        appRef.update("workflowSteps", FieldValue.arrayUnion(step))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Workflow step added");
                        showToast("Workflow step saved");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Workflow step failed: " + e.getMessage());
                    }
                });
    }

    // ==================== UPLOAD PDF ====================
    public void uploadPdf(String appNo, InputStream pdfStream, String fileName, final UploadCallback callback) {
        if (pdfStream == null) {
            if (callback != null) callback.onSuccess(null);
            return;
        }

        StorageReference pdfRef = storage.getReference()
                .child("applications")
                .child(appNo)
                .child(fileName);

        UploadTask uploadTask = pdfRef.putStream(pdfStream);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "PDF uploaded: " + downloadUrl);

                    // Update application with PDF URL
                    db.collection("applications")
                            .document(appNo)
                            .update("pdfUrl", downloadUrl)
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess(downloadUrl);
                            });
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "PDF upload failed: " + e.getMessage());
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void getNextApplicationNumber(String feeder, final NextNumberCallback callback) {
        Log.d(TAG, "Getting next number for feeder: " + feeder);

        // Get ALL applications
        db.collection("applications")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int highestNumber = 0;

                        Log.d(TAG, "Total documents: " + queryDocumentSnapshots.size());

                        // Loop through ALL documents
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String appNo = document.getString("application_no");

                            // Check if this belongs to our feeder
                            if (appNo != null && appNo.startsWith(feeder + "-")) {
                                try {
                                    // Get number part (after "amb-" in "amb-001")
                                    String numberPart = appNo.substring(feeder.length() + 1);
                                    int currentNum = Integer.parseInt(numberPart);

                                    Log.d(TAG, "Found: " + appNo + " -> " + currentNum);

                                    if (currentNum > highestNumber) {
                                        highestNumber = currentNum;
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Parse error: " + e.getMessage());
                                }
                            }
                        }

                        int nextNumber = highestNumber + 1;
                        String newAppNo = feeder + "-" + String.format("%03d", nextNumber);

                        Log.d(TAG, "Result: highest = " + highestNumber +
                                ", next = " + nextNumber +
                                ", returning: " + newAppNo);

                        if (callback != null) {
                            callback.onSuccess(newAppNo);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                        String newAppNo = feeder + "-001";
                        if (callback != null) {
                            callback.onSuccess(newAppNo);
                        }
                    }
                });
    }
    // ==================== HELPER METHODS ====================
    private Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        if (json != null) {
            // Use keys() iterator instead of keySet()
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);

                if (value instanceof JSONObject) {
                    value = jsonToMap((JSONObject) value);
                } else if (value instanceof JSONArray) {
                    value = jsonArrayToList((JSONArray) value);
                }

                map.put(key, value);
            }
        }

        return map;
    }

    private List<Object> jsonArrayToList(JSONArray array) throws JSONException {
        List<Object> list = new java.util.ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            }
            list.add(value);
        }
        return list;
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // ==================== CALLBACK INTERFACES ====================
    public interface SaveCallback {
        void onSuccess(String appNo);
        void onError(String error);
    }

    public interface LoadCallback {
        void onSuccess(JSONArray applications);
        void onError(String error);
    }

    public interface NextNumberCallback {
        void onSuccess(String nextNumber);
        void onError(String error);
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
    }
}
