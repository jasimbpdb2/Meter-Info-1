package customerinfo.app;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class MeterDataDisplayActivity extends AppCompatActivity {

    private WebView webView;
    private String meterDataJson;
    private static final String TAG = "MeterDataDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meter_data_display);

        webView = findViewById(R.id.webView);
        setupWebView();

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("METER_DATA")) {
            meterDataJson = intent.getStringExtra("METER_DATA");
            Log.d(TAG, "Data received in intent: " + (meterDataJson != null ? meterDataJson.length() + " characters" : "NULL"));
            if (meterDataJson != null) {
                Log.d(TAG, "First 200 chars: " + meterDataJson.substring(0, Math.min(200, meterDataJson.length())));
            }
        } else {
            Log.e(TAG, "No METER_DATA found in intent");
            meterDataJson = "{\"error\":\"No data received from MainActivity\"}";
        }
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebView page finished loading: " + url);
                // Inject data when page loads
                if (meterDataJson != null) {
                    injectMeterData();
                } else {
                    Log.e(TAG, "No data to inject");
                }
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + description + " (" + errorCode + ")");
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");
        Log.d(TAG, "Loading HTML from assets");
        webView.loadUrl("file:///android_asset/meter_data_display.html");
    }

    private void injectMeterData() {
        try {
            Log.d(TAG, "Injecting data into WebView");
            String js = "javascript:window.meterData = " + meterDataJson + "; window.displayData();";
            Log.d(TAG, "JavaScript to execute: " + js.substring(0, Math.min(100, js.length())) + "...");
            
            webView.evaluateJavascript(js, value -> {
                Log.d(TAG, "JavaScript evaluation completed: " + value);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error injecting data: " + e.getMessage());
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public String getMeterData() {
            Log.d(TAG, "getMeterData() called from JavaScript");
            return meterDataJson != null ? meterDataJson : "{\"error\":\"No data available\"}";
        }

        @JavascriptInterface
        public void goBack() {
            Log.d(TAG, "goBack() called from JavaScript");
            finish();
        }

        @JavascriptInterface
        public void closeApp() {
            Log.d(TAG, "closeApp() called from JavaScript");
            finish();
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
