package customerinfo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Map;

public class HtmlActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TAG = "HtmlActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.htmlactivity);

        webView = findViewById(R.id.webView);
        setupWebView();
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        try {
            webView.loadUrl("file:///android_asset/application_form.html");
        } catch (Exception e) {
            Log.e(TAG, "HTML load error: " + e.getMessage());
        }
    }

    /* ==================================================
            ANDROID ↔ JAVASCRIPT INTERFACE
       ================================================== */
    public class WebAppInterface {

        // ---------------- APPLICATION NUMBER GENERATION -------------------
        @JavascriptInterface
        public void getNextApplicationNumber(String feeder) {
            FirebaseManager.getInstance(HtmlActivity.this)
                    .getNextApplicationNumber(feeder, new FirebaseManager.NextNumberCallback() {
                        @Override
                        public void onSuccess(String nextNumber) {
                            String js = "handleNextAppNumber('" + nextNumber + "');";
                            webView.post(() -> webView.evaluateJavascript(js, null));
                        }

                        @Override
                        public void onError(String error) {
                            // On error, use local counter
                            String nextNumber = getLocalNextNumber(feeder);
                            String js = "handleNextAppNumber('" + nextNumber + "');";
                            webView.post(() -> webView.evaluateJavascript(js, null));
                        }
                    });
        }

        @JavascriptInterface
        public void loadLastApplicationNumber(String feeder) {
            // Query for the last application number for this feeder
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("applications")
                    .whereEqualTo("feeder", feeder)
                    .orderBy("application_no", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot lastDoc = queryDocumentSnapshots.getDocuments().get(0);
                            String lastAppNo = lastDoc.getString("application_no");
                            String js = "handleLastAppNumber('" + lastAppNo + "');";
                            webView.post(() -> webView.evaluateJavascript(js, null));
                        } else {
                            String js = "handleLastAppNumber('');";
                            webView.post(() -> webView.evaluateJavascript(js, null));
                        }
                    })
                    .addOnFailureListener(e -> {
                        String js = "handleLastAppNumber('');";
                        webView.post(() -> webView.evaluateJavascript(js, null));
                    });
        }

        private String getLocalNextNumber(String feeder) {
            // Get from SharedPreferences as backup
            SharedPreferences prefs = getSharedPreferences("AppCounters", MODE_PRIVATE);
            String key = "counter_" + feeder;
            int counter = prefs.getInt(key, 1);

            // Save incremented value for next time
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(key, counter + 1);
            editor.apply();

            // Simple format: feeder-001
            return feeder + "-" + String.format("%03d", counter);
        }

        // ---------------- FETCH DATA -------------------
        @JavascriptInterface
        public String fetchDataForApplication(String inputNumber, String type) {
            try {
                ApplicationFormHelper helper = new ApplicationFormHelper();
                Map<String, Object> result =
                        helper.fetchDataForApplicationForm(inputNumber, type);

                return new JSONObject(result).toString();

            } catch (Exception e) {
                return "{\"error\":\"Data fetch failed: " + e.getMessage() + "\"}";
            }
        }

        // ---------------- SAVE APPLICATION -------------------
        @JavascriptInterface
        public void saveApplication(String jsonData, String pdfFileName) {
            runOnUiThread(() -> {
                try {
                    JSONObject appData = new JSONObject(jsonData);

                    FirebaseManager.getInstance(HtmlActivity.this)
                            .saveApplication(appData, new FirebaseManager.SaveCallback() {
                                @Override
                                public void onSuccess(String appNo) {
                                    showToast("Saved to Firebase: " + appNo);

                                    // If there's a PDF, upload it
                                    if (pdfFileName != null && !pdfFileName.isEmpty()) {
                                        try {
                                            InputStream pdfStream = getAssets().open(pdfFileName);
                                            FirebaseManager.getInstance(HtmlActivity.this)
                                                    .uploadPdf(appNo, pdfStream, pdfFileName,
                                                            new FirebaseManager.UploadCallback() {
                                                                @Override
                                                                public void onSuccess(String downloadUrl) {
                                                                    showToast("PDF uploaded: " + downloadUrl);
                                                                }

                                                                @Override
                                                                public void onError(String error) {
                                                                    showToast("PDF upload failed: " + error);
                                                                }
                                                            });
                                        } catch (Exception e) {
                                            showToast("PDF error: " + e.getMessage());
                                        }
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    showToast("Save failed: " + error);
                                }
                            });

                } catch (Exception e) {
                    showToast("Save failed: " + e.getMessage());
                }
            });
        }

        // ---------------- LOAD APPLICATIONS -------------------
        @JavascriptInterface
        public void loadApplications() {
            FirebaseManager.getInstance(HtmlActivity.this)
                    .loadApplications(new FirebaseManager.LoadCallback() {
                        @Override
                        public void onSuccess(JSONArray apps) {
                            String js = "handleLoadedApplications(" + apps.toString() + ");";
                            webView.post(() -> webView.evaluateJavascript(js, null));
                        }

                        @Override
                        public void onError(String error) {
                            showToast("Load failed: " + error);
                        }
                    });
        }

        // ---------------- WORKFLOW UPDATE -------------------
        @JavascriptInterface
        public void addWorkflowStep(String appNo, String step, String user) {
            // This method is now handled in JavaScript
            showToast("Workflow step added: " + step);
        }

        // ---------------- EXCEL EXPORT -------------------
        @JavascriptInterface
        public void exportToExcel(String jsonApps) {
            try {
                JSONArray arr = new JSONArray(jsonApps);
                String csv = generateExcel(arr);

                boolean ok = saveExcel(csv);

                if (ok) showToast("Excel saved in Downloads");
                else showToast("Failed to save Excel");

            } catch (Exception e) {
                showToast("Excel error: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void openExcelView() {
            runOnUiThread(() -> {
                try {
                    // Load the Excel view HTML
                    webView.loadUrl("file:///android_asset/excel_view.html");
                    showToast("এক্সেল ভিউ লোড হচ্ছে...");
                } catch (Exception e) {
                    showToast("এক্সেল ভিউ লোড করতে সমস্যা: " + e.getMessage());
                }
            });
        }

        // ---------------- PDF EXPORT -------------------
        @JavascriptInterface
        public void saveAsPDF() {
            runOnUiThread(HtmlActivity.this::createPdfFromWebView);
        }

        // ---------------- UTIL -------------------
        @JavascriptInterface
        public void closeApplication() {
            finish();
        }

        @JavascriptInterface
        public void showToast(String msg) {
            Toast.makeText(HtmlActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /* ==================================================
                     EXCEL GENERATION
       ================================================== */
    private String generateExcel(JSONArray apps) {
        StringBuilder csv = new StringBuilder();
        csv.append("Application No,Name,Father,Address,Mobile,Meter,Consumer,Status,Feeder,Created\n");

        try {
            for (int i = 0; i < apps.length(); i++) {
                JSONObject a = apps.getJSONObject(i);

                csv.append("\"").append(a.optString("application_no")).append("\",");
                csv.append("\"").append(a.optString("customer_name")).append("\",");
                csv.append("\"").append(a.optString("father_name")).append("\",");
                csv.append("\"").append(a.optString("address")).append("\",");
                csv.append("\"").append(a.optString("mobile_no")).append("\",");
                csv.append("\"").append(a.optString("meter_no")).append("\",");
                csv.append("\"").append(a.optString("consumer_no")).append("\",");
                csv.append("\"").append(a.optString("status")).append("\",");
                csv.append("\"").append(a.optString("feeder")).append("\",");
                csv.append("\"").append(a.optString("createdAt")).append("\"\n");
            }
        } catch (Exception ignore) {
        }

        return csv.toString();
    }

    private boolean saveExcel(String content) {
        try {
            File dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS);

            String name = "applications_" + System.currentTimeMillis() + ".csv";
            File file = new File(dir, name);

            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Excel save error: " + e.getMessage());
            return false;
        }
    }

    /* ==================================================
                        PDF GENERATION
       ================================================== */
    private void createPdfFromWebView() {
        try {
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);

            PrintDocumentAdapter adapter =
                    webView.createPrintDocumentAdapter("AppForm_" + System.currentTimeMillis());

            PrintAttributes attr = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            pm.print("AppForm", adapter, attr);

        } catch (Exception e) {
            Toast.makeText(this, "PDF Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /* ==================================================
                        BACK BUTTON
       ================================================== */
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
