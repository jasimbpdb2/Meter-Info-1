package customerinfo.app;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import androidx.appcompat.app.AppCompatActivity;

public class MeterDataDisplayActivity extends AppCompatActivity {

    private WebView webView;
    private String meterDataJson;

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
        }
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject data when page loads
                if (meterDataJson != null) {
                    injectMeterData();
                }
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");
        webView.loadUrl("file:///android_asset/meter_data_display.html");
    }

    private void injectMeterData() {
        String js = "javascript:window.meterData = " + meterDataJson + "; window.displayData();";
        webView.evaluateJavascript(js, null);
    }

    public class WebAppInterface {
        @JavascriptInterface
        public String getMeterData() {
            return meterDataJson != null ? meterDataJson : "{}";
        }

        @JavascriptInterface
        public void goBack() {
            finish();
        }

        @JavascriptInterface
        public void closeApp() {
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
