package customerinfo.app;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class HTMLViewerHelper {
    private Context context;
    private WebView webView;

    public HTMLViewerHelper(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        
        // Basic WebView setup
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
    }

    public void displayData(String jsonData) {
        try {
            // Simple HTML display with JSON data
            String htmlContent = createSimpleHTML(jsonData);
            webView.loadData(htmlContent, "text/html", "UTF-8");
        } catch (Exception e) {
            webView.loadData("<h1>Error displaying data</h1>", "text/html", "UTF-8");
        }
    }

    public void displayRawHTML(String htmlContent) {
        webView.loadData(htmlContent, "text/html", "UTF-8");
    }

    private String createSimpleHTML(String jsonData) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial; padding: 20px; background: #f5f5f5; }" +
                ".card { background: white; padding: 15px; margin: 10px 0; border-radius: 8px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='card'>" +
                "<h1>📊 Meter Data</h1>" +
                "<p>Raw JSON Data:</p>" +
                "<pre>" + jsonData + "</pre>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Simple versions that don't call MainActivity methods
    public void displayPrepaidData(Map<String, Object> result) {
        displayData("Prepaid: " + result.toString());
    }

    public void displayPostpaidData(Map<String, Object> result) {
        displayData("Postpaid: " + result.toString());
    }
}