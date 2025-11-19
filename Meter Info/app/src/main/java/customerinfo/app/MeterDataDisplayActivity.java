package customerinfo.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class MeterDataDisplayActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_display);

        webView = findViewById(R.id.webView);
        
        // Enable JavaScript (if needed)
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        // Set WebView client to handle links internally
        webView.setWebViewClient(new WebViewClient());

        // Get the data from intent
        String meterDataJson = getIntent().getStringExtra("METER_DATA");
        if (meterDataJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(meterDataJson);
                Map<String, Object> resultMap = jsonObjectToMap(jsonObject);
                
                // Determine if it's prepaid or postpaid based on available fields
                boolean isPrepaid = resultMap.containsKey("meter_number") && resultMap.containsKey("consumer_number");
                
                String htmlContent;
                if (isPrepaid) {
                    TemplateHelper.PrepaidData prepaidData = TemplateHelper.convertToPrepaidData(resultMap);
                    htmlContent = TemplateHelper.renderPrepaidTemplate(this, prepaidData);
                } else {
                    TemplateHelper.PostpaidData postpaidData = TemplateHelper.convertToPostpaidData(resultMap);
                    htmlContent = TemplateHelper.renderPostpaidTemplate(this, postpaidData);
                }
                
                // Load the HTML content
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                );
                
            } catch (Exception e) {
                // Show error page
                webView.loadDataWithBaseURL(
                    null,
                    "<html><body style='font-family: Arial; padding: 20px; background: #f5f5f5;'>" +
                    "<div style='background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                    "<h2 style='color: #e74c3c;'>❌ Error</h2>" +
                    "<p>Failed to load meter data: " + e.getMessage() + "</p>" +
                    "<p>Please try again.</p>" +
                    "</div></body></html>",
                    "text/html",
                    "UTF-8",
                    null
                );
            }
        } else {
            // No data provided
            webView.loadDataWithBaseURL(
                null,
                "<html><body style='font-family: Arial; padding: 20px; background: #f5f5f5;'>" +
                "<div style='background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                "<h2 style='color: #e74c3c;'>❌ No Data</h2>" +
                "<p>No meter data provided. Please go back and search again.</p>" +
                "</div></body></html>",
                "text/html",
                "UTF-8",
                null
            );
        }
    }

    // Helper function to convert JSONObject to Map
    private Map<String, Object> jsonObjectToMap(JSONObject jsonObject) throws Exception {
        Map<String, Object> map = new HashMap<>();
        java.util.Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    // Handle back button
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}