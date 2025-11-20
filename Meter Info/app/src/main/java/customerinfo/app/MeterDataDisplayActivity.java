package customerinfo.app;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

/**
 * MeterDataDisplayActivity
 * - Expects intent extra "METER_DATA" containing JSON (string)
 * - Loads template from assets and injects data as a JS variable then displays in WebView.
 *
 * Add this activity to AndroidManifest and add <uses-permission android:name="android.permission.INTERNET" />
 */
public class MeterDataDisplayActivity extends AppCompatActivity {
    private static final String TAG = "MeterDataDisplay";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        try {
            String jsonString = getIntent().getStringExtra("METER_DATA");
            if (jsonString == null) jsonString = "{}";
            // Make sure it's valid JSON (MainActivity already put a JSONObject)
            JSONObject json = new JSONObject(jsonString);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            // Read template from assets and inject JS variable
            // We'll build a small HTML wrapper that sets window.__METER_DATA__ then loads asset content
            String wrapper = "<html><head><meta charset='utf-8'></head><body>" +
                    "<script>window.__METER_DATA__ = " + json.toString() + ";</script>" +
                    "<iframe id='t' style='display:none' src='file:///android_asset/meter_template.html'></iframe>" +
                    "<script> " +
                    "var iframe = document.getElementById('t'); " +
                    "iframe.onload = function(){ " +
                    "  try{ " +
                    "    var doc = iframe.contentDocument || iframe.contentWindow.document; " +
                    "    // copy body html to parent body so CSS, scripts run in top context " +
                    "    document.body.innerHTML = doc.body.innerHTML; " +
                    "    // copy head elements too (styles) " +
                    "    document.head.innerHTML = doc.head.innerHTML; " +
                    "    // transfer __METER_DATA__ to the child script by ensuring child can read window.__METER_DATA__ already set " +
                    "  }catch(e){console.error(e);} " +
                    "};" +
                    "</script>" +
                    "</body></html>";

            // Use loadDataWithBaseURL so assets can be referenced
            webView.loadDataWithBaseURL("file:///android_asset/", wrapper, "text/html", "utf-8", null);

        } catch (Exception e) {
            Log.e(TAG, "Error loading meter data: " + e.getMessage(), e);
            webView.loadData("<pre>Error rendering data: " + e.getMessage() + "</pre>", "text/html", "utf-8");
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}