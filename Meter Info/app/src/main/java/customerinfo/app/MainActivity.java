package customerinfo.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.math3.linear.ConjugateGradient;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.ProcessIdUtil;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.xerces.impl.xs.SchemaSymbols;
import org.apache.xerces.xinclude.XIncludeHandler;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "ExcelHelper";
    
    /* access modifiers changed from: private */
    public ApplicationFormHelper applicationFormHelper;
    private Button backBtn;
    private RadioButton consumerNoOption;
    private ExcelHelper excelHelper;
    private Map<String, Object> lastSearchResult;
    private LinearLayout mainLayout;
    private RadioGroup mainRadioGroup;
    private EditText meterInput;
    private RadioButton meterNoOption;
    private RadioButton postpaidBtn;
    private LinearLayout postpaidOptionsLayout;
    private RadioGroup postpaidRadioGroup;
    private String postpaidSubType = "consumer_no";
    private RadioButton prepaidBtn;
    private TextView resultView;
    private String selectedMode = "";
    private String selectedType = "prepaid";
    private LinearLayout startupLayout;
    private Button submitBtn;
    private UIHelper uiHelper;

    static {
        trustAllCertificates();
    }

    private static void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = {new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init((KeyManager[]) null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkStoragePermission();
        initViews();
        showStartupScreen();
        showResult("🔍 Enter meter/consumer number and search\n💾 Data auto-saves to Excel after each search");
    }

    private void initViews() {
        this.startupLayout = (LinearLayout) findViewById(R.id.startupLayout);
        this.mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        this.backBtn = (Button) findViewById(R.id.backBtn);
        this.meterInput = (EditText) findViewById(R.id.meterInput);
        this.prepaidBtn = (RadioButton) findViewById(R.id.prepaidBtn);
        this.postpaidBtn = (RadioButton) findViewById(R.id.postpaidBtn);
        this.submitBtn = (Button) findViewById(R.id.submitBtn);
        this.resultView = (TextView) findViewById(R.id.resultView);
        this.postpaidOptionsLayout = (LinearLayout) findViewById(R.id.postpaidOptionsLayout);
        this.postpaidRadioGroup = (RadioGroup) findViewById(R.id.postpaidRadioGroup);
        this.consumerNoOption = (RadioButton) findViewById(R.id.consumerNoOption);
        this.meterNoOption = (RadioButton) findViewById(R.id.meterNoOption);
        this.resultView.setKeyListener((KeyListener) null);
        this.resultView.setTextIsSelectable(true);
        this.resultView.setFocusableInTouchMode(true);
        this.resultView.setLongClickable(true);
        this.uiHelper = new UIHelper(this, this.resultView, (LinearLayout) findViewById(R.id.tableContainer));
        this.excelHelper = new ExcelHelper(this);
        setupClickListeners();
        this.backBtn.setOnClickListener(v -> showStartupScreen());
        updateButtonStates();
        updatePostpaidSubOptions();
        updateInputHint();
    }

    private void showStartupScreen() {
        this.startupLayout.setVisibility(View.VISIBLE);
        this.mainLayout.setVisibility(View.GONE);
        ((Button) findViewById(R.id.lookupBtn)).setOnClickListener(v -> {
            this.selectedMode = "lookup";
            showMainInterface();
            showResult("🔍 Lookup mode selected\nEnter meter/consumer number to search");
        });
        ((Button) findViewById(R.id.applicationBtn)).setOnClickListener(v -> {
            this.selectedMode = "application";
            openApplicationForm();
        });
    }

    private void showMainInterface() {
        this.startupLayout.setVisibility(View.GONE);
        this.mainLayout.setVisibility(View.VISIBLE);
        updateButtonStates();
        updatePostpaidSubOptions();
        updateInputHint();
    }

    private void setupClickListeners() {
        this.prepaidBtn.setOnClickListener(v -> {
            this.selectedType = "prepaid";
            this.postpaidOptionsLayout.setVisibility(View.GONE);
            updateButtonStates();
            updateInputHint();
            showResult("📱 Prepaid selected - Enter 12-digit meter number");
        });
        
        this.postpaidBtn.setOnClickListener(v -> {
            this.selectedType = "postpaid";
            this.postpaidOptionsLayout.setVisibility(View.VISIBLE);
            updateButtonStates();
            updateInputHint();
            showResult("💡 Postpaid selected - Choose input type");
        });
        
        this.postpaidRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.consumerNoOption) {
                this.postpaidSubType = "consumer_no";
                updatePostpaidSubOptions();
                updateInputHint();
                showResult("👤 Consumer No selected - Enter consumer number");
            } else if (checkedId == R.id.meterNoOption) {
                this.postpaidSubType = "meter_no";
                updatePostpaidSubOptions();
                updateInputHint();
                showResult("🔢 Meter No selected - Enter meter number");
            }
        });
        
        this.submitBtn.setOnClickListener(v -> {
            String inputNumber = this.meterInput.getText().toString().trim();
            if (inputNumber.isEmpty()) {
                showResult("❌ Please enter number");
            } else if (!this.selectedType.equals("prepaid") || inputNumber.length() == 12) {
                fetchData(inputNumber);
            } else {
                showResult("❌ Prepaid meter must be 12 digits");
            }
        });
        
        if (isStoragePermissionGranted()) {
            saveAndShareExcel();
        } else {
            Toast.makeText(this, "Please grant storage permission first", Toast.LENGTH_LONG).show();
            checkStoragePermission();
        }
    }

    private void updateButtonStates() {
        int i = -13330213;
        this.prepaidBtn.setBackgroundColor(this.selectedType.equals("prepaid") ? -13330213 : -6969946);
        RadioButton radioButton = this.postpaidBtn;
        if (!this.selectedType.equals("postpaid")) {
            i = -6969946;
        }
        radioButton.setBackgroundColor(i);
    }

    private void updatePostpaidSubOptions() {
        int i = -14575885;
        this.consumerNoOption.setBackgroundColor(this.postpaidSubType.equals("consumer_no") ? -14575885 : -7288071);
        RadioButton radioButton = this.meterNoOption;
        if (!this.postpaidSubType.equals("meter_no")) {
            i = -7288071;
        }
        radioButton.setBackgroundColor(i);
    }

    private void updateInputHint() {
        if (this.selectedType.equals("prepaid")) {
            this.meterInput.setHint("Enter 12-digit meter number");
        } else if (this.postpaidSubType.equals("consumer_no")) {
            this.meterInput.setHint("Enter consumer number");
        } else {
            this.meterInput.setHint("Enter meter number");
        }
    }

    private void fetchData(String inputNumber) {
        this.uiHelper.clearAll();
        showResult("🔄 Fetching " + this.selectedType + " data...\nInput: " + inputNumber);
        new Thread(() -> {
            try {
                Map<String, Object> result = fetchDataBasedOnType(inputNumber);
                String output = displayResult(result, this.selectedType);
                saveLookupToExcel(result, inputNumber);
                runOnUiThread(() -> {
                    this.uiHelper.displayTextResult(output);
                    displayTableIfAvailable(result);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showResult("❌ Error: " + e.getMessage()));
            }
        }).start();
    }

    private Map<String, Object> fetchDataBasedOnType(String inputNumber) {
        if (this.selectedType.equals("prepaid")) {
            return fetchPrepaidData(inputNumber);
        }
        return this.postpaidSubType.equals("consumer_no") ? fetchPostpaidData(inputNumber) : fetchMeterLookupData(inputNumber);
    }

    private void displayTableIfAvailable(Map<String, Object> result) {
        try {
            if (!this.selectedType.equals("postpaid") || !this.postpaidSubType.equals("meter_no") || !result.containsKey("customer_results")) {
                showTableForCustomer(result);
                return;
            }
            for (Map<String, Object> customerResult : (List) result.get("customer_results")) {
                showTableForCustomer(customerResult);
            }
        } catch (Exception e) {
        }
    }

    private void showTableForCustomer(Map<String, Object> result) {
        try {
            Object server2DataObj = result.get("SERVER2_data");
            if (server2DataObj instanceof JSONObject) {
                JSONObject server2Data = (JSONObject) server2DataObj;
                if (server2Data.has("billInfo")) {
                    JSONArray billInfo = server2Data.getJSONArray("billInfo");
                    if (billInfo.length() > 0) {
                        this.uiHelper.displayBillTable(billInfo);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void showResult(String message) {
        runOnUiThread(() -> this.resultView.setText(message));
    }

    public static Map<String, Object> getCustomerNumbersByMeter(String meterNumber) {
        String str = meterNumber;
        Map<String, Object> result = new HashMap<>();
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://billonwebapi.bpdb.gov.bd/api/BillInformation/GetCustomerMeterbyMeterNo/12/" + str).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty(XIncludeHandler.HTTP_ACCEPT, "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            System.out.println("🔍 METER LOOKUP API: Fetching customers for meter: " + str);
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    response.append(line);
                }
                JSONObject meterData = new JSONObject(response.toString());
                if (meterData.getInt(NotificationCompat.CATEGORY_STATUS) != 1 || !meterData.has("content")) {
                    result.put("error", "No customer data found for this meter");
                } else {
                    JSONArray customers2 = meterData.getJSONArray("content");
                    List<String> customerNumbers = new ArrayList<>();
                    System.out.println("✅ Found " + customers2.length() + " customer(s) for meter " + str);
                    int i = 0;
                    while (i < customers2.length()) {
                        JSONObject customer = customers2.getJSONObject(i);
                        String custNum = customer.optString("CUSTOMER_NUM");
                        if (!custNum.isEmpty()) {
                            customerNumbers.add(custNum);
                            System.out.println("   👤 Customer: " + custNum + " - " + customer.optString("CUSTOMER_NAME", "N/A"));
                        }
                        i++;
                        String str2 = meterNumber;
                    }
                    if (!customerNumbers.isEmpty()) {
                        result.put("customer_numbers", customerNumbers);
                        result.put("meter_api_data", meterData);
                    } else {
                        result.put("error", "No customer numbers found for this meter");
                    }
                }
            } else {
                result.put("error", "HTTP Error: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.out.println("❌ METER LOOKUP API Error: " + e.getMessage());
            result.put("error", "Meter API Error: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> fetchMeterLookupData(String meterNumber) {
        Map<String, Object> result = new HashMap<>();
        result.put("meter_number", meterNumber);
        result.put("customer_numbers", new ArrayList());
        result.put("customer_results", new ArrayList());
        System.out.println("🔍 Starting meter lookup for: " + meterNumber);
        Map<String, Object> meterResult = getCustomerNumbersByMeter(meterNumber);
        if (meterResult.containsKey("error")) {
            result.put("error", meterResult.get("error"));
            return result;
        } else if (!meterResult.containsKey("customer_numbers")) {
            result.put("error", "No customer numbers found for this meter");
            return result;
        } else {
            List<String> customerNumbers = (List) meterResult.get("customer_numbers");
            result.put("customer_numbers", customerNumbers);
            result.put("meter_api_data", meterResult.get("meter_api_data"));
            System.out.println("🔄 Processing " + customerNumbers.size() + " customer(s)");
            List<Map<String, Object>> customerResults = new ArrayList<>();
            for (String custNum : customerNumbers) {
                System.out.println("🔄 Processing customer: " + custNum);
                customerResults.add(fetchPostpaidData(custNum));
            }
            result.put("customer_results", customerResults);
            return result;
        }
    }

    public static Map<String, Object> SERVER1Lookup(String meterNumber) {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://web.bpdbprepaid.gov.bd/bn/token-check").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty(XIncludeHandler.HTTP_ACCEPT, "text/x-component");
            conn.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
            conn.setRequestProperty("Next-Action", "29e85b2c55c9142822fe8da82a577612d9e58bb2");
            conn.setRequestProperty("Origin", "http://web.bpdbprepaid.gov.bd");
            conn.setRequestProperty("Referer", "http://web.bpdbprepaid.gov.bd/bn/token-check");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            OutputStream os = conn.getOutputStream();
            os.write(("[{\"meterNo\":\"" + meterNumber + "\"}]").getBytes("UTF-8"));
            os.flush();
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    response.append(line);
                }
                String responseBody = response.toString();
                result.put("consumer_number", extractConsumerNumber(responseBody));
                result.put("SERVER1_data", responseBody);
            } else {
                result.put("error", "HTTP Error: " + responseCode);
            }
        } catch (Exception e) {
            result.put("error", "SERVER 1 Error: " + e.getMessage());
        }
        return result;
    }

    private static String extractConsumerNumber(String responseBody) {
        try {
            Matcher matcher = Pattern.compile("\"customerNo\"\\s*:\\s*\"(\\d+)\"").matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error extracting consumer number: " + e.getMessage());
            return null;
        }
    }

    public static Map<String, Object> SERVER3Lookup(String customerNumber) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("🔍 SERVER 3: Starting data fetch for: " + customerNumber);
        System.out.println("🔄 SERVER 3: Fetching SERVER 2 data first...");
        Map<String, Object> SERVER2Result = SERVER2Lookup(customerNumber);
        if (SERVER2Result == null || SERVER2Result.containsKey("error")) {
            System.out.println("❌ SERVER 3: SERVER2 data fetch failed");
        } else {
            result.put("SERVER2_data", SERVER2Result.get("SERVER2_data"));
            System.out.println("✅ SERVER 3: SERVER2 data fetched successfully");
        }
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://miscbillAPI.bpdb.gov.bd/API/v1/get-pre-customer_info/" + customerNumber).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty(XIncludeHandler.HTTP_ACCEPT, "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            System.out.println("🔍 SERVER 3: Fetching SERVER3 data for: " + customerNumber);
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    response.append(line);
                }
                JSONObject SERVER3Data = new JSONObject(response.toString());
                if (isValidSERVER3Data(SERVER3Data)) {
                    System.out.println("✅ SERVER 3: Valid data received");
                    result.put("SERVER3_data", SERVER3Data);
                    result.put("source", "SERVER3_with_SERVER2");
                } else {
                    System.out.println("⚠️ SERVER 3: Invalid or empty data");
                    result.put("source", "SERVER2_only");
                }
            } else {
                System.out.println("⚠️ SERVER 3: HTTP " + conn.getResponseCode());
                result.put("source", "SERVER2_only");
            }
        } catch (Exception e) {
            System.out.println("❌ SERVER 3 Error: " + e.getMessage());
            result.put("source", "SERVER2_only");
        }
        if (result.containsKey("SERVER2_data") || result.containsKey("SERVER3_data")) {
            System.out.println("✅ SERVER 3: Data fetch completed with sources: " + result.get("source"));
        } else {
            System.out.println("❌ SERVER 3: All data sources failed");
            result.put("error", "Both SERVER 3 and SERVER 2 failed to return valid data");
        }
        return result;
    }

    private static boolean isValidSERVER3Data(JSONObject SERVER3Data) {
        if (SERVER3Data == null) {
            return false;
        }
        try {
            boolean hasValidData = !SERVER3Data.optString("customerNumber", "").trim().isEmpty() && !SERVER3Data.optString("customerName", "").trim().isEmpty();
            if (!hasValidData) {
                System.out.println("❌ SERVER 3: Missing essential fields (customerNumber or customerName)");
            }
            return hasValidData;
        } catch (Exception e) {
            System.out.println("❌ SERVER 3: Data validation error: " + e.getMessage());
            return false;
        }
    }

    public static Map<String, Object> SERVER2Lookup(String accountNumber) {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://billonwebAPI.bpdb.gov.bd/API/CustomerInformation/" + accountNumber).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty(XIncludeHandler.HTTP_ACCEPT, "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            System.out.println("🔍 SERVER 2: Fetching data for: " + accountNumber);
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    response.append(line);
                }
                JSONObject SERVER2Data = new JSONObject(response.toString());
                if (isValidSERVER2Data(SERVER2Data)) {
                    System.out.println("✅ SERVER 2: Valid data received");
                    result.put("SERVER2_data", SERVER2Data);
                } else {
                    System.out.println("❌ SERVER 2: Invalid data received");
                    result.put("error", "SERVER 2 returned invalid data");
                }
            } else {
                System.out.println("❌ SERVER 2: HTTP Error: " + conn.getResponseCode());
                result.put("error", "HTTP Error: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.out.println("❌ SERVER 2 Error: " + e.getMessage());
            result.put("error", "SERVER 2 Error: " + e.getMessage());
        }
        return result;
    }

    private static boolean isValidSERVER2Data(JSONObject SERVER2Data) {
        if (SERVER2Data == null) {
            return false;
        }
        try {
            if ((!SERVER2Data.has("customerInfo") || SERVER2Data.getJSONArray("customerInfo").length() <= 0) && !SERVER2Data.has("finalBalanceInfo")) {
                if (!SERVER2Data.has("balanceInfo")) {
                    System.out.println("❌ SERVER 2: No customerInfo or balance data found");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("❌ SERVER 2: Data validation error: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> fetchPrepaidData(String meterNumber) {
        Map<String, Object> SERVER1Result = SERVER1Lookup(meterNumber);
        Map<String, Object> result = new HashMap<>();
        result.put("meter_number", meterNumber);
        result.put("SERVER1_data", SERVER1Result.get("SERVER1_data"));
        result.put("consumer_number", SERVER1Result.get("consumer_number"));
        result.put("SERVER3_data", (Object) null);
        result.put("SERVER2_data", (Object) null);
        String consumerNumber = (String) SERVER1Result.get("consumer_number");
        if (consumerNumber != null && !SERVER1Result.containsKey("error")) {
            Map<String, Object> SERVER3Result = SERVER3Lookup(consumerNumber);
            if (SERVER3Result == null || SERVER3Result.containsKey("error")) {
                System.out.println("❌ All SERVERs failed for prepaid data");
                result.put("error", "All data sources failed");
            } else {
                String source = (String) SERVER3Result.getOrDefault("source", EnvironmentCompat.MEDIA_UNKNOWN);
                System.out.println("📊 Prepaid data source: " + source);
                if (SERVER3Result.containsKey("SERVER3_data")) {
                    result.put("SERVER3_data", SERVER3Result.get("SERVER3_data"));
                }
                if (SERVER3Result.containsKey("SERVER2_data")) {
                    result.put("SERVER2_data", SERVER3Result.get("SERVER2_data"));
                }
                result.put("data_source", source);
            }
        }
        return result;
    }

    private Map<String, Object> fetchPostpaidData(String customerNumber) {
        Map<String, Object> result = new HashMap<>();
        result.put("customer_number", customerNumber);
        System.out.println("🔍 Fetching postpaid data for: " + customerNumber);
        Map<String, Object> SERVER3Result = SERVER3Lookup(customerNumber);
        if (SERVER3Result == null || SERVER3Result.containsKey("error")) {
            System.out.println("❌ All SERVERs failed for postpaid data");
            result.put("error", "All data sources failed");
        } else {
            String source = (String) SERVER3Result.getOrDefault("source", EnvironmentCompat.MEDIA_UNKNOWN);
            System.out.println("📊 Postpaid data source: " + source);
            if (SERVER3Result.containsKey("SERVER3_data")) {
                result.put("SERVER3_data", SERVER3Result.get("SERVER3_data"));
            }
            if (SERVER3Result.containsKey("SERVER2_data")) {
                result.put("SERVER2_data", SERVER3Result.get("SERVER2_data"));
            }
            result.put("data_source", source);
            System.out.println("✅ Postpaid data fetch successful");
        }
        return result;
    }

    private String extractDirectValue(JSONObject jsonObject, String key) {
        String textValue;
        try {
            if (!jsonObject.has(key)) {
                return "N/A";
            }
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                JSONObject obj = (JSONObject) value;
                if (!obj.has("_text") || (textValue = obj.getString("_text")) == null || textValue.isEmpty()) {
                    return "N/A";
                }
                if (textValue.equals("{}")) {
                    return "N/A";
                }
                return textValue.trim();
            }
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty() || stringValue.equals("{}")) {
                return "N/A";
            }
            return stringValue;
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getText(Object field) {
        Object textValue;
        if (field == null) {
            return "N/A";
        }
        try {
            if (field instanceof JSONObject) {
                JSONObject obj = (JSONObject) field;
                if (!obj.has("_text") || (textValue = obj.get("_text")) == null || textValue.toString().isEmpty() || textValue.toString().equals("{}")) {
                    return "N/A";
                }
                String text = textValue.toString().trim();
                if (text.isEmpty()) {
                    return "N/A";
                }
                return text;
            } else if (field.toString().equals("{}")) {
                return "N/A";
            } else {
                String text2 = field.toString().trim();
                if (text2.isEmpty() || text2.equals("{}")) {
                    return "N/A";
                }
                return text2;
            }
        } catch (Exception e) {
            return "N/A";
        }
    }

    private Map<String, Object> cleanSERVER1Data(Object SERVER1DataObj) {
        String responseBody;
        System.out.println("=== DEBUG cleanSERVER1Data START ===");
        if (SERVER1DataObj == null) {
            System.out.println("❌ SERVER1DataObj is NULL");
            Map<String, Object> result = new HashMap<>();
            result.put("error", "No SERVER1 data available");
            return result;
        }
        if (SERVER1DataObj instanceof String) {
            responseBody = (String) SERVER1DataObj;
        } else if (SERVER1DataObj instanceof JSONObject) {
            responseBody = SERVER1DataObj.toString();
        } else {
            responseBody = SERVER1DataObj.toString();
        }
        System.out.println("📦 Raw response preview: " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");
        Map<String, Object> cleaned = new HashMap<>();
        try {
            String jsonPart = extractActualJson(responseBody);
            System.out.println("🔍 Extracted JSON preview: " + jsonPart.substring(0, Math.min(100, jsonPart.length())) + "...");
            JSONObject SERVER1Data = new JSONObject(jsonPart);
            if (SERVER1Data.has("mCustomerData")) {
                System.out.println("✅ Found mCustomerData");
                processCustomerDataDirect(SERVER1Data.getJSONObject("mCustomerData"), cleaned);
            }
            if (SERVER1Data.has("mOrderData")) {
                System.out.println("✅ Found mOrderData - using exact pattern extraction");
                List<Map<String, String>> transactions = extractTransactionsWithExactPatterns(responseBody);
                if (!transactions.isEmpty()) {
                    cleaned.put("recent_transactions", transactions);
                    System.out.println("✅ Added " + transactions.size() + " transactions with tokens");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Exception in cleanSERVER1Data: " + e.getMessage());
            e.printStackTrace();
            cleaned.put("error", "Processing failed: " + e.getMessage());
        }
        System.out.println("🎯 Final cleaned data keys: " + cleaned.keySet());
        System.out.println("=== DEBUG cleanSERVER1Data END ===\n");
        return (Map) removeEmptyFields(cleaned);
    }

    private String extractActualJson(String responseBody) {
        try {
            int jsonStart = responseBody.indexOf("1:{");
            if (jsonStart != -1) {
                return responseBody.substring(jsonStart + 2);
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    private List<Map<String, String>> extractTransactionsWithExactPatterns(String response) {
        int index;
        List<Map<String, String>> transactions = new ArrayList<>();
        System.out.println("🔍 Looking for tokens with exact pattern...");
        int index2 = 0;
        int count = 0;
        while (index2 != -1 && count < 3 && (index = response.indexOf("\"tokens\":{\"_text\":\"", index2)) != -1) {
            int valueStart = "\"tokens\":{\"_text\":\"".length() + index;
            int valueEnd = response.indexOf("\"", valueStart);
            if (valueEnd != -1) {
                String token = response.substring(valueStart, valueEnd);
                System.out.println("🔑 FOUND TOKEN " + (count + 1) + ": " + token);
                Map<String, String> transaction = extractTransactionFields(response, index);
                transaction.put("Tokens", token);
                transactions.add(transaction);
                count++;
            }
            index2 = valueEnd + 1;
        }
        if (count == 0) {
            System.out.println("❌ No tokens found with exact pattern");
        }
        return transactions;
    }

    private Map<String, String> extractTransactionFields(String response, int tokenPosition) {
        Map<String, String> transaction = new HashMap<>();
        String searchArea = response.substring(Math.max(0, tokenPosition + NotificationManagerCompat.IMPORTANCE_UNSPECIFIED), Math.min(response.length(), tokenPosition + 200));
        transaction.put("Date", extractExactValue(searchArea, "date"));
        transaction.put("Order Number", extractExactValue(searchArea, "orderNo"));
        transaction.put("Amount", "৳" + extractExactValue(searchArea, "grossAmount"));
        transaction.put("Energy Cost", "৳" + extractExactValue(searchArea, "energyCost"));
        transaction.put("Operator", extractExactValue(searchArea, ConjugateGradient.OPERATOR));
        transaction.put("Sequence", extractExactValue(searchArea, "sequence"));
        return transaction;
    }

    private String extractExactValue(String text, String fieldName) {
        int valueStart;
        int valueEnd;
        try {
            String pattern = "\"" + fieldName + "\":{\"_text\":\"";
            int start = text.indexOf(pattern);
            if (start == -1 || (valueEnd = text.indexOf("\"", valueStart)) == -1) {
                return "N/A";
            }
            return text.substring((valueStart = pattern.length() + start), valueEnd);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private void processCustomerDataDirect(JSONObject mCustomerData, Map<String, Object> cleaned) {
        try {
            if (mCustomerData.has("result")) {
                JSONObject result = mCustomerData.getJSONObject("result");
                Map<String, String> customerInfo = new HashMap<>();
                customerInfo.put("Consumer Number", extractDirectValue(result, "customerAccountNo"));
                customerInfo.put(SchemaSymbols.ATTVAL_NAME, extractDirectValue(result, "customerName"));
                customerInfo.put("Address", extractDirectValue(result, "customerAddress"));
                customerInfo.put("Phone", extractDirectValue(result, "customerPhone"));
                customerInfo.put("Division", extractDirectValue(result, "division"));
                customerInfo.put("Sub Division", extractDirectValue(result, "sndDivision"));
                customerInfo.put("Tariff Category", extractDirectValue(result, "tariffCategory"));
                customerInfo.put("Connection Category", extractDirectValue(result, "connectionCategory"));
                customerInfo.put("Account Type", extractDirectValue(result, "accountType"));
                customerInfo.put("Meter Type", extractDirectValue(result, "meterType"));
                customerInfo.put("Sanctioned Load", extractDirectValue(result, "sanctionLoad"));
                customerInfo.put("Meter Number", extractDirectValue(result, "meterNumber"));
                customerInfo.put("Last Recharge Amount", extractDirectValue(result, "lastRechargeAmount"));
                customerInfo.put("Last Recharge Time", extractDirectValue(result, "lastRechargeTime"));
                customerInfo.put("Installation Date", extractDirectValue(result, "installationDate"));
                customerInfo.put("Lock Status", extractDirectValue(result, "lockStatus"));
                customerInfo.put("Total Recharge This Month", extractDirectValue(result, "totalRechargeThisMonth"));
                cleaned.put("customer_info", removeEmptyFields(customerInfo));
                System.out.println("✅ Added customer_info with " + customerInfo.size() + " fields");
            }
        } catch (Exception e) {
            System.out.println("❌ Error processing customer data: " + e.getMessage());
        }
    }

    private Map<String, Object> cleanSERVER2Data(JSONObject SERVER2Data) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Implementation of cleanSERVER2Data method
            // ... (keeping the method structure but simplifying for brevity)
            if (SERVER2Data != null && !SERVER2Data.has("error")) {
                Map<String, Object> cleaned = new HashMap<>();
                // Add your SERVER2 data cleaning logic here
                result.putAll(cleaned);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning SERVER2 data: " + e.getMessage());
        }
        return result;
    }

    private Map<String, String> parseFinalBalanceInfo(String balanceString) {
        Map<String, String> balanceInfo = new HashMap<>();
        if (balanceString == null || balanceString.isEmpty() || balanceString.equals("null")) {
            return balanceInfo;
        }
        try {
            System.out.println("🔍 PARSING finalBalanceInfo: " + balanceString);
            if (balanceString.contains(",") || balanceString.contains(ParameterizedMessage.ERROR_MSG_SEPARATOR)) {
                String[] parts = balanceString.split(",");
                String totalBalance = parts[0].trim();
                balanceInfo.put("Total Balance", totalBalance);
                balanceInfo.put("Arrear Amount", totalBalance);
                for (int i = 1; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (part.startsWith("PRN:")) {
                        balanceInfo.put("PRN", part.substring(4).trim());
                    } else if (part.startsWith("LPS:")) {
                        balanceInfo.put("LPS", part.substring(4).trim());
                    } else if (part.startsWith("VAT:")) {
                        balanceInfo.put("VAT", part.substring(4).trim());
                    } else if (part.startsWith("Current:")) {
                        balanceInfo.put("Current Bill", part.substring(8).trim());
                    } else if (part.startsWith("Arrear:")) {
                        balanceInfo.put("Arrear Amount", part.substring(7).trim());
                    }
                }
                System.out.println("✅ Parsed detailed balance: " + balanceInfo);
                return balanceInfo;
            }
            balanceInfo.put("Total Balance", balanceString.trim());
            balanceInfo.put("Arrear Amount", balanceString.trim());
            System.out.println("✅ Parsed as simple total: " + balanceString);
            return balanceInfo;
        } catch (Exception e) {
            System.out.println("❌ Error parsing finalBalanceInfo: " + e.getMessage());
            balanceInfo.put("Total Balance", balanceString);
            balanceInfo.put("Arrear Amount", balanceString);
        }
        return balanceInfo;
    }

    private Map<String, Object> cleanSERVER3Data(JSONObject SERVER3Data) {
        if (SERVER3Data == null) {
            return new HashMap();
        }
        Map<String, Object> cleaned = new HashMap<>();
        Map<String, String> customerInfo = new HashMap<>();
        customerInfo.put("Customer Number", SERVER3Data.optString("customerNumber"));
        customerInfo.put("Customer Name", SERVER3Data.optString("customerName"));
        customerInfo.put("Customer Address", SERVER3Data.optString("customerAddr"));
        customerInfo.put("Father Name", SERVER3Data.optString("fatherName"));
        customerInfo.put("Location Code", SERVER3Data.optString("locationCode"));
        customerInfo.put("Area Code", SERVER3Data.optString("areaCode"));
        customerInfo.put("Book Number", SERVER3Data.optString("bookNumber"));
        customerInfo.put("Bill Group", SERVER3Data.optString("billGroup"));
        customerInfo.put("Meter Number", SERVER3Data.optString("meterNum"));
        customerInfo.put("Meter Condition", SERVER3Data.optString("meterConditionDesc"));
        customerInfo.put("Sanctioned Load", SERVER3Data.optString("sanctionedLoad"));
        customerInfo.put("Tariff Description", SERVER3Data.optString("tariffDesc"));
        customerInfo.put("Walk Order", SERVER3Data.optString("walkOrder"));
        customerInfo.put("Arrear Amount", SERVER3Data.optString("arrearAmount"));
        String lastReadingSr = SERVER3Data.optString("lastBillReadingSr");
        if (lastReadingSr != null && !lastReadingSr.equals("null") && !lastReadingSr.isEmpty()) {
            customerInfo.put("Last Bill Reading SR", lastReadingSr);
            cleaned.put("current_reading_sr", Double.valueOf(Double.parseDouble(lastReadingSr)));
        }
        customerInfo.put("Last Bill Reading OF PK", SERVER3Data.optString("lastBillReadingOfPk"));
        customerInfo.put("Last Bill Reading PK", SERVER3Data.optString("lastBillReadingPk"));
        cleaned.put("customer_info", removeEmptyFields(customerInfo));
        if (SERVER3Data.has("arrearAmount")) {
            String arrearAmount = SERVER3Data.optString("arrearAmount");
            Map<String, String> balanceInfo = new HashMap<>();
            balanceInfo.put("Total Balance", arrearAmount);
            balanceInfo.put("Arrear Amount", arrearAmount);
            cleaned.put("balance_info", balanceInfo);
        }
        return (Map) removeEmptyFields(cleaned);
    }

    private Map<String, Object> mergeSERVERData(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, String> customerInfo = new HashMap<>();
            Map<String, String> balanceInfo = new HashMap<>();

            // Copy basic info
            if (source.containsKey("meter_number")) {
                result.put("meter_number", source.get("meter_number"));
            }
            if (source.containsKey("consumer_number")) {
                result.put("consumer_number", source.get("consumer_number"));
            }
            if (source.containsKey("customer_number")) {
                result.put("customer_number", source.get("customer_number"));
            }

            // Process SERVER2 data
            if (source.containsKey("SERVER2_data") && source.get("SERVER2_data") instanceof JSONObject) {
                Map<String, Object> cleanedServer2 = cleanSERVER2Data((JSONObject) source.get("SERVER2_data"));
                if (cleanedServer2.containsKey("customer_info")) {
                    customerInfo.putAll((Map) cleanedServer2.get("customer_info"));
                }
                if (cleanedServer2.containsKey("balance_info")) {
                    balanceInfo.putAll((Map) cleanedServer2.get("balance_info"));
                }
            }

            // Process SERVER3 data
            if (source.containsKey("SERVER3_data") && source.get("SERVER3_data") instanceof JSONObject) {
                Map<String, Object> cleanedServer3 = cleanSERVER3Data((JSONObject) source.get("SERVER3_data"));
                if (cleanedServer3.containsKey("customer_info")) {
                    customerInfo.putAll((Map) cleanedServer3.get("customer_info"));
                }
                if (cleanedServer3.containsKey("balance_info") && balanceInfo.isEmpty()) {
                    balanceInfo.putAll((Map) cleanedServer3.get("balance_info"));
                }
            }

            if (!customerInfo.isEmpty()) {
                result.put("customer_info", customerInfo);
            }
            if (!balanceInfo.isEmpty()) {
                result.put("balance_info", balanceInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in mergeSERVERData: " + e.getMessage());
        }
        return result;
    }

    private String formatMergedDisplayWithoutTable(Map<String, Object> data) {
        StringBuilder output = new StringBuilder();
        if (data.containsKey("customer_info")) {
            output.append("👤 CUSTOMER INFORMATION\n");
            output.append("══════════════════════════════════════════════════\n");
            Map<String, String> customerInfo = (Map) data.get("customer_info");
            for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    output.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }
        if (data.containsKey("balance_info")) {
            output.append("\n💰 BALANCE INFORMATION\n");
            output.append("══════════════════════════════════════════════════\n");
            Map<String, String> balanceInfo = (Map) data.get("balance_info");
            for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    output.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }
        return output.toString();
    }

    private Drawable createCellBackground(boolean isHeader) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(isHeader ? -3355444 : -1);
        border.setStroke(1, -7829368);
        return border;
    }

    private Map<String, Object> createBillSummary(JSONArray billInfo) {
        Map<String, Object> billSummary = new HashMap<>();
        try {
            if (billInfo.length() > 0) {
                billSummary.put("total_bills", Integer.valueOf(billInfo.length()));
                JSONObject latestBill = billInfo.getJSONObject(0);
                billSummary.put("latest_bill_date", formatBillMonth(latestBill.optString("BILL_MONTH")));
                billSummary.put("latest_bill_number", latestBill.optString("BILL_NO"));
                billSummary.put("latest_consumption", Double.valueOf(latestBill.optDouble("CONS_KWH_SR", 0.0d)));
                billSummary.put("latest_total_amount", Double.valueOf(latestBill.optDouble("TOTAL_BILL", 0.0d)));
                billSummary.put("latest_balance", Double.valueOf(latestBill.optDouble("BALANCE", 0.0d)));
                billSummary.put("current_reading_sr", Double.valueOf(latestBill.optDouble("CONS_KWH_SR", 0.0d)));
                double recentConsumption = 0.0d;
                for (int i = 0; i < Math.min(billInfo.length(), 3); i++) {
                    recentConsumption += billInfo.getJSONObject(i).optDouble("CONS_KWH_SR", 0.0d);
                }
                billSummary.put("recent_consumption", Double.valueOf(recentConsumption));
                billSummary.put("bill_months_available", Integer.valueOf(billInfo.length()));
            }
        } catch (Exception e) {
            System.out.println("❌ Error creating bill summary: " + e.getMessage());
        }
        return billSummary;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String padRight(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() > length) {
            return text.substring(0, length - 3) + "...";
        }
        return String.format("%-" + length + "s", new Object[]{text});
    }

    private String centerText(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() > length) {
            return text.substring(0, length - 3) + "...";
        }
        int padding = length - text.length();
        int leftPadding = padding / 2;
        return repeatString(" ", leftPadding) + text + repeatString(" ", padding - leftPadding);
    }

    private boolean isValidValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty() || trimmedValue.equals("N/A") || trimmedValue.equals("null") || trimmedValue.equals("{}") || trimmedValue.equals("undefined")) {
            return false;
        }
        return true;
    }

    private String formatPrepaidDisplay(Map<String, Object> cleanedData) {
        if (cleanedData == null || cleanedData.isEmpty()) {
            return "No prepaid data available";
        }
        StringBuilder output = new StringBuilder();
        if (cleanedData.containsKey("customer_info")) {
            output.append("👤 CUSTOMER INFORMATION\n");
            output.append(repeatString("=", 20)).append("\n");
            for (Map.Entry<String, String> entry : ((Map) cleanedData.get("customer_info")).entrySet()) {
                if (!entry.getValue().equals("N/A")) {
                    output.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            output.append("\n");
        }
        if (cleanedData.containsKey("recent_transactions")) {
            output.append("🔑 LAST 3 RECHARGE TOKENS\n");
            output.append(repeatString("=", 20)).append("\n\n");
            List<Map<String, String>> transactions = (List) cleanedData.get("recent_transactions");
            for (int i = 0; i < transactions.size(); i++) {
                Map<String, String> transaction = transactions.get(i);
                output.append("Order ").append(i + 1).append(":\n");
                output.append("  📅 Date: ").append(transaction.get("Date")).append("\n");
                output.append("  🧾 Order: ").append(transaction.get("Order Number")).append("\n");
                output.append("  👤 Operator: ").append(transaction.get("Operator")).append("\n");
                output.append("  🔢 Sequence: ").append(transaction.get("Sequence")).append("\n");
                output.append("  💰 Amount: ").append(transaction.get("Amount")).append("\n");
                output.append("  ⚡ Energy: ").append(transaction.get("Energy Cost")).append("\n");
                output.append("  🔑 TOKENS: ").append(transaction.get("Tokens")).append("\n\n");
            }
        }
        return output.toString();
    }

    private String getMeterStatus(String statusCode) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put(SchemaSymbols.ATTVAL_TRUE_1, "Active");
        statusMap.put("2", "Inactive");
        statusMap.put("3", "Disconnected");
        return statusMap.getOrDefault(statusCode, "Unknown (" + statusCode + ")");
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "N/A";
        }
        try {
            if (dateString.contains("T")) {
                return dateString.split("T")[0];
            }
            return dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private Map<String, String> extractBalanceInfo(String balanceString) {
        Map<String, String> balanceInfo = new HashMap<>();
        if (balanceString == null || balanceString.isEmpty()) {
            return balanceInfo;
        }
        try {
            if (!balanceString.contains(",")) {
                if (!balanceString.contains(ParameterizedMessage.ERROR_MSG_SEPARATOR)) {
                    balanceInfo.put("Total Balance", balanceString.trim());
                    balanceInfo.put("Arrear Amount", balanceString.trim());
                    return balanceInfo;
                }
            }
            String[] parts = balanceString.split(",");
            String mainBalance = parts[0].trim();
            balanceInfo.put("Total Balance", mainBalance);
            balanceInfo.put("Arrear Amount", mainBalance);
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.startsWith("PRN:")) {
                    balanceInfo.put("PRN", part.substring(4).trim());
                } else if (part.startsWith("LPS:")) {
                    balanceInfo.put("LPS", part.substring(4).trim());
                } else if (part.startsWith("VAT:")) {
                    balanceInfo.put("VAT", part.substring(4).trim());
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Balance parsing warning: " + e.getMessage());
            balanceInfo.put("Total Balance", balanceString);
        }
        return balanceInfo;
    }

    private <T> T removeEmptyFields(T data) {
        if (data instanceof Map) {
            Map<Object, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key.toString().contains("bill") || key.toString().contains("balance")) {
                    result.put(key, value);
                } else {
                    Object cleaned = removeEmptyFields(value);
                    if (cleaned != null && !cleaned.equals("") && !cleaned.equals("N/A")) {
                        if ((!(cleaned instanceof Map) || !((Map) cleaned).isEmpty()) && (!(cleaned instanceof List) || !((List) cleaned).isEmpty())) {
                            result.put(key, cleaned);
                        }
                    }
                }
            }
            return (T) result;
        } else if (!(data instanceof List)) {
            return data;
        } else {
            List<Object> result2 = new ArrayList<>();
            for (Object item : (List) data) {
                Object cleaned2 = removeEmptyFields(item);
                if (cleaned2 != null && !cleaned2.equals("") && !cleaned2.equals("N/A")) {
                    if ((!(cleaned2 instanceof Map) || !((Map) cleaned2).isEmpty()) && (!(cleaned2 instanceof List) || !((List) cleaned2).isEmpty())) {
                        result2.add(cleaned2);
                    }
                }
            }
            return (T) result2;
        }
    }

    private String formatBillDisplay(JSONObject SERVER2Data) {
        if (SERVER2Data == null) {
            return "No bill data available";
        }
        if (SERVER2Data.has("error")) {
            return "Error in bill data: " + SERVER2Data.optString("error");
        }
        StringBuilder output = new StringBuilder();
        try {
            System.out.println("📊 BILL DEBUG: SERVER2Data keys: " + getJSONKeys(SERVER2Data));
            if (SERVER2Data.has("billInfo")) {
                JSONArray billInfo = SERVER2Data.getJSONArray("billInfo");
                System.out.println("📊 BILL DEBUG: billInfo length: " + billInfo.length());
                if (billInfo.length() > 0) {
                    int billCount = Math.min(billInfo.length(), 3);
                    output.append("📊 BILL HISTORY (LAST ").append(billCount).append(" MONTHS)\n\n");
                    // Bill display logic
                } else {
                    output.append("❌ No bill history records found\n");
                }
            } else {
                output.append("❌ No bill information available in response\n");
            }
        } catch (Exception e) {
            output.append("❌ Error displaying bill data: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        return output.toString();
    }

    private String getFormattedBillValue(JSONObject bill, String fieldKey) {
        String r0 = "N/A";
        try {
            if (!bill.has(fieldKey) || bill.isNull(fieldKey)) {
                return r0;
            }
            // Implementation for formatting bill values
            return bill.optString(fieldKey, r0);
        } catch (Exception e) {
            return r0;
        }
    }

    private String formatBillMonth(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) {
            return "N/A";
        }
        try {
            String[] parts = dateStr.substring(0, 10).split(ProcessIdUtil.DEFAULT_PROCESSID);
            if (parts.length >= 2) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + ProcessIdUtil.DEFAULT_PROCESSID + year;
                }
            }
            return dateStr.substring(0, 7);
        } catch (Exception e) {
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : dateStr;
        }
    }

    private String getJSONKeys(JSONObject json) {
        try {
            Iterator<String> keys = json.keys();
            List<String> keyList = new ArrayList<>();
            while (keys.hasNext()) {
                keyList.add(keys.next());
            }
            return String.join(", ", keyList);
        } catch (Exception e) {
            return "Error getting keys";
        }
    }

    private String formatBillSummary(Map<String, Object> billSummary) {
        if (billSummary == null || billSummary.isEmpty()) {
            return "No bill summary available";
        }
        StringBuilder output = new StringBuilder();
        try {
            output.append("📅 Total Bills: ").append(billSummary.getOrDefault("total_bills", "N/A")).append("\n");
            if (billSummary.containsKey("latest_bill_date")) {
                output.append("📋 Latest Bill Date: ").append(billSummary.get("latest_bill_date")).append("\n");
            }
            if (billSummary.containsKey("latest_bill_number")) {
                output.append("🧾 Latest Bill No: ").append(billSummary.get("latest_bill_number")).append("\n");
            }
            // Add more bill summary fields as needed
        } catch (Exception e) {
            output.append("❌ Error formatting bill summary: ").append(e.getMessage()).append("\n");
        }
        return output.toString();
    }

    private boolean isValidBillDisplay(String billDisplay) {
        return billDisplay != null && !billDisplay.trim().isEmpty() && !billDisplay.contains("No bill data available") && !billDisplay.contains("No bill information available") && !billDisplay.contains("Error in bill data") && !billDisplay.contains("No bill summary available") && billDisplay.length() > 20;
    }

    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String displayResult(Map<String, Object> result, String billType) {
        if (result == null) {
            return "❌ No result data available";
        }
        StringBuilder output = new StringBuilder();
        output.append("\n══════════════════════════════════════════════════\n");
        output.append("📊 ").append(billType.toUpperCase()).append(" METER INFO\n");
        output.append("══════════════════════════════════════════════════\n");
        if (result.containsKey("error")) {
            output.append("❌ Error: ").append(result.get("error")).append("\n");
            return output.toString();
        }
        output.append("🔢 Meter Number: ").append(result.getOrDefault("meter_number", "N/A")).append("\n");
        if ("prepaid".equals(billType)) {
            if (result.get("consumer_number") != null) {
                output.append("👤 Consumer Number: ").append(result.get("consumer_number")).append("\n");
            }
            Map<String, Object> mergedData = mergeSERVERData(result);
            if (mergedData != null && !mergedData.isEmpty()) {
                output.append(formatMergedDisplayWithoutTable(mergedData));
            }
        } else if ("postpaid".equals(billType)) {
            if (result.containsKey("customer_results")) {
                List<String> customerNumbers = (List) result.get("customer_numbers");
                List<Map<String, Object>> customerResults = (List) result.get("customer_results");
                output.append("\n📊 Found ").append(customerNumbers.size()).append(" customer(s) for this meter\n\n");
                for (int i = 0; i < customerResults.size(); i++) {
                    output.append(repeatString("=", 40)).append("\n");
                    output.append("👤 CUSTOMER ").append(i + 1).append(PackagingURIHelper.FORWARD_SLASH_STRING).append(customerNumbers.size()).append(": ").append(customerNumbers.get(i)).append("\n");
                    output.append(repeatString("=", 40)).append("\n");
                    Map<String, Object> mergedData2 = mergeSERVERData(customerResults.get(i));
                    if (mergedData2 == null || mergedData2.isEmpty()) {
                        output.append("❌ No data available for this customer\n\n");
                    } else {
                        output.append(formatMergedDisplayWithoutTable(mergedData2)).append("\n");
                    }
                }
            } else {
                output.append("👤 Consumer Number: ").append(result.getOrDefault("customer_number", "N/A")).append("\n");
                Map<String, Object> mergedData3 = mergeSERVERData(result);
                if (mergedData3 == null || mergedData3.isEmpty()) {
                    output.append("❌ No customer data found\n");
                } else {
                    output.append(repeatString("=", 30)).append("\n");
                    output.append(formatMergedDisplayWithoutTable(mergedData3));
                }
            }
        }
        return output.toString();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                    intent.setData(Uri.fromParts("package", getPackageName(), (String) null));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent2 = new Intent();
                    intent2.setAction("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                    startActivity(intent2);
                }
            }
        } else if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"}, 100);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 100) {
            return;
        }
        if (grantResults.length <= 0 || grantResults[0] != 0) {
            Toast.makeText(this, "Storage permission denied - Excel saving disabled", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }

    private Map<String, String> extractDataForExcel(Map<String, Object> result, String type) {
        Map<String, String> excelData = new HashMap<>();
        try {
            if ("prepaid".equals(type)) {
                excelData.put("Meter Number", getSafeString(result.get("meter_number")));
                excelData.put("Consumer Number", getSafeString(result.get("consumer_number")));
                Map<String, Object> mergedData = mergeSERVERData(result);
                if (mergedData != null) {
                    Map<String, String> customerInfo = (Map) mergedData.get("customer_info");
                    Map<String, String> balanceInfo = (Map) mergedData.get("balance_info");
                    if (customerInfo != null) {
                        excelData.putAll(customerInfo);
                    }
                    if (balanceInfo != null) {
                        excelData.put("Arrear Amount", balanceInfo.get("Arrear Amount"));
                        excelData.put("Total Balance", balanceInfo.get("Total Balance"));
                    }
                }
            } else {
                excelData.put("Customer Number", getSafeString(result.get("customer_number")));
                Map<String, Object> mergedData2 = mergeSERVERData(result);
                if (mergedData2 != null) {
                    Map<String, String> customerInfo2 = (Map) mergedData2.get("customer_info");
                    Map<String, String> balanceInfo2 = (Map) mergedData2.get("balance_info");
                    if (customerInfo2 != null) {
                        excelData.putAll(customerInfo2);
                    }
                    if (balanceInfo2 != null) {
                        excelData.put("Arrear Amount", balanceInfo2.get("Arrear Amount"));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting data for Excel: " + e.getMessage());
        }
        return excelData;
    }

    private String getSafeString(Object value) {
        if (value == null) {
            return "N/A";
        }
        String stringValue = value.toString();
        if (stringValue.equals("null") || stringValue.isEmpty()) {
            return "N/A";
        }
        return stringValue;
    }

    private void saveLookupToExcel(Map<String, Object> result, String inputNumber) {
        if (this.excelHelper == null) {
            this.excelHelper = new ExcelHelper(this);
        }
        try {
            if (this.selectedType.equals("prepaid")) {
                this.excelHelper.savePrepaidLookup("User", inputNumber, extractDataForExcel(result, "prepaid"));
            } else if (result.containsKey("customer_results")) {
                List<Map<String, String>> excelDataList = new ArrayList<>();
                for (Map<String, Object> customerResult : (List) result.get("customer_results")) {
                    excelDataList.add(extractDataForExcel(customerResult, "postpaid"));
                }
                this.excelHelper.saveMultiplePostpaidLookups("User", excelDataList);
            } else {
                this.excelHelper.savePostpaidLookup("User", extractDataForExcel(result, "postpaid"));
            }
            Log.d(TAG, "Data automatically saved to Excel");
        } catch (Exception e) {
            Log.e(TAG, "Error auto-saving to Excel: " + e.getMessage());
        }
    }

    public void saveAndShareExcel() {
        ExcelHelper excelHelper2 = this.excelHelper;
        if (excelHelper2 == null) {
            return;
        }
        if (excelHelper2.saveExcelFile()) {
            shareExcelFile(this.excelHelper.getFilePath());
        } else {
            Toast.makeText(this, "Failed to save Excel file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openApplicationForm() {
        try {
            final WebView applicationWebView = new WebView(this);
            WebSettings webSettings = applicationWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            ApplicationFormHelper applicationFormHelper2 = new ApplicationFormHelper(this, applicationWebView);
            this.applicationFormHelper = applicationFormHelper2;
            applicationWebView.addJavascriptInterface(applicationFormHelper2, "AndroidInterface");
            applicationWebView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d("ApplicationForm", "Page finished loading: " + url);
                    showKeyboardForWebView(applicationWebView);
                }

                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e("ApplicationForm", "WebView error: " + description);
                    if (applicationFormHelper != null) {
                        applicationFormHelper.hideLoading();
                    }
                }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("আবেদনপত্র");
            builder.setView(applicationWebView);
            builder.setPositiveButton("প্রিন্ট", (dialog, which) -> {
                try {
                    applicationWebView.evaluateJavascript("window.print();", null);
                } catch (Exception e) {
                    Log.e("ApplicationForm", "Print error: " + e.getMessage());
                }
            });
            builder.setNegativeButton("বন্ধ", (dialog, which) -> {
                dialog.dismiss();
                showStartupScreen();
            });
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dialogInterface -> {
                Log.d("ApplicationForm", "Dialog shown");
                showKeyboardForWebView(applicationWebView);
            });
            dialog.setOnCancelListener(d -> {
                Log.d("ApplicationForm", "Dialog cancelled");
                showStartupScreen();
            });
            dialog.show();
            try {
                applicationWebView.loadUrl("file:///android_asset/application_form.html");
                Log.d("ApplicationForm", "Loading HTML from assets");
            } catch (Exception e) {
                Log.e("ApplicationForm", "Error loading HTML: " + e.getMessage());
                if (applicationFormHelper != null) {
                    applicationFormHelper.showError("Failed to load application form: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("ApplicationForm", "Error opening application form: " + e.getMessage());
            Toast.makeText(this, "Error opening application form", Toast.LENGTH_SHORT).show();
            showStartupScreen();
        }
    }

    /* access modifiers changed from: private */
    public void showKeyboardForWebView(final WebView webView) {
        if (webView != null) {
            webView.postDelayed(() -> {
                try {
                    webView.evaluateJavascript("javascript:document.getElementById('searchInput').focus();", null);
                    webView.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                    if (imm != null) {
                        imm.showSoftInput(webView, 1);
                    }
                } catch (Exception e) {
                    Log.e("Keyboard", "Error showing keyboard: " + e.getMessage());
                }
            }, 1000);
        }
    }

    public void fetchDataForApplicationForm(String inputNumber, String billType) {
        this.selectedType = billType;
        ApplicationFormHelper applicationFormHelper2 = this.applicationFormHelper;
        if (applicationFormHelper2 != null) {
            applicationFormHelper2.showLoading();
        }
        new Thread(() -> {
            try {
                Map<String, Object> result = fetchDataBasedOnType(inputNumber);
                runOnUiThread(() -> {
                    if (applicationFormHelper != null) {
                        applicationFormHelper.fillApplicationForm(result, billType);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (applicationFormHelper != null) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "Error: " + e.getMessage());
                        applicationFormHelper.fillApplicationForm(errorResult, billType);
                    }
                });
            }
        }).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (applicationFormHelper != null) {
                applicationFormHelper.hideLoading();
            }
        }, 30000);
    }

    private void shareExcelFile(String filePath) {
        Toast.makeText(this, "Excel file saved: " + filePath, Toast.LENGTH_LONG).show();
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        ExcelHelper excelHelper2 = this.excelHelper;
        if (excelHelper2 != null) {
            excelHelper2.close();
        }
    }
}