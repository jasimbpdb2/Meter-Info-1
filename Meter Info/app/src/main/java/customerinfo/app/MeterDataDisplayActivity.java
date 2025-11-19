package customerinfo.app;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class MeterDataDisplayActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_data_display);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // Get the JSON data from MainActivity
        String jsonData = getIntent().getStringExtra("METER_DATA");
        
        if (jsonData != null) {
            // Load HTML template with the data
            String htmlContent = generateHTMLContent(jsonData);
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } else {
            webView.loadData("<h1>Error: No data received</h1>", "text/html", "UTF-8");
        }
    }

    private String generateHTMLContent(String jsonData) {
        try {
            JSONObject data = new JSONObject(jsonData);
            
            // Simple HTML template that works with MainActivity's data structure
            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }" +
                    ".container { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                    ".header { background: #2196F3; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }" +
                    ".section { margin-bottom: 20px; padding: 15px; border-left: 4px solid #2196F3; background: #f9f9f9; }" +
                    ".error { background: #ffebee; border-left: 4px solid #f44336; color: #c62828; }" +
                    ".success { background: #e8f5e8; border-left: 4px solid #4caf50; }" +
                    "h1, h2, h3 { color: #333; }" +
                    "pre { background: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto; }" +
                    "</style></head><body>" +
                    "<div class='container'>" +
                    "<div class='header'><h1>📊 Meter Information</h1></div>";

            // Check for errors first
            if (data.has("error")) {
                html += "<div class='section error'><h2>❌ Error</h2><p>" + data.optString("error") + "</p></div>";
            }

            // Show search info
            html += "<div class='section'><h2>🔍 Search Details</h2>" +
                    "<p><strong>Input:</strong> " + data.optString("search_input", "N/A") + "</p>" +
                    "<p><strong>Type:</strong> " + data.optString("search_type", "N/A") + "</p>" +
                    "<p><strong>Time:</strong> " + data.optString("timestamp", "N/A") + "</p></div>";

            // Show formatted text (from MainActivity's working display)
            if (data.has("formatted_text")) {
                html += "<div class='section success'><h2>📋 Customer Information</h2>" +
                        "<pre>" + data.optString("formatted_text") + "</pre></div>";
            }

            // Show customer info if available
            if (data.has("customer_info")) {
                html += "<div class='section'><h2>👤 Customer Details</h2>";
                try {
                    JSONObject customerInfo = data.getJSONObject("customer_info");
                    html += "<table width='100%'>";
                    for (String key : customerInfo.keySet()) {
                        html += "<tr><td><strong>" + key + ":</strong></td><td>" + customerInfo.optString(key) + "</td></tr>";
                    }
                    html += "</table></div>";
                } catch (Exception e) {
                    html += "<p>Error displaying customer info</p></div>";
                }
            }

            // Show balance info if available
            if (data.has("balance_info")) {
                html += "<div class='section'><h2>💰 Balance Information</h2>";
                try {
                    JSONObject balanceInfo = data.getJSONObject("balance_info");
                    html += "<table width='100%'>";
                    for (String key : balanceInfo.keySet()) {
                        html += "<tr><td><strong>" + key + ":</strong></td><td>" + balanceInfo.optString(key) + "</td></tr>";
                    }
                    html += "</table></div>";
                } catch (Exception e) {
                    html += "<p>Error displaying balance info</p></div>";
                }
            }

            html += "</div></body></html>";
            return html;

        } catch (Exception e) {
            return "<h1>Error generating HTML: " + e.getMessage() + "</h1>";
        }
    }
}