package customerinfo.app;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
        
        // FIX: Add WebViewClient to handle page loading properly
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loaded successfully: " + url);
                // Test JavaScript communication
                webView.evaluateJavascript(
                    "javascript:if(typeof testAndroidInterface === 'function') { testAndroidInterface(); } else { console.log('testAndroidInterface not found'); }",
                    null
                );
            }
        });
        
        // Add JavaScript interface
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");
        
        // Load the HTML file
        try {
            webView.loadUrl("file:///android_asset/application_form.html");
            Log.d(TAG, "Loading application_form.html from assets");
        } catch (Exception e) {
            Log.e(TAG, "Error loading HTML: " + e.getMessage());
            Toast.makeText(this, "Error loading form", Toast.LENGTH_SHORT).show();
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public String fetchDataForApplication(String inputNumber, String type) {
            Log.d(TAG, "fetchDataForApplication called: " + inputNumber + ", " + type);
            try {
                ApplicationFormHelper helper = new ApplicationFormHelper();
                java.util.Map<String, Object> result = helper.fetchDataForApplicationForm(inputNumber, type);
                org.json.JSONObject jsonResult = new org.json.JSONObject(result);
                String jsonString = jsonResult.toString();
                Log.d(TAG, "Returning data: " + jsonString);
                return jsonString;
            } catch (Exception e) {
                Log.e(TAG, "Error fetching data: " + e.getMessage());
                return "{\"error\":\"Data fetch failed: " + e.getMessage() + "\"}";
            }
        }

        @JavascriptInterface
        public void closeApplication() {
            finish();
        }

        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(HtmlActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}