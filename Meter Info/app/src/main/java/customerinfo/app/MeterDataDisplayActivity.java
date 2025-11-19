package customerinfo.app;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

public class MeterDataDisplayActivity extends AppCompatActivity {

    private WebView webView;
    private HTMLViewerHelper htmlViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_display);

        webView = findViewById(R.id.htmlWebView);
        htmlViewer = new HTMLViewerHelper(this, webView);

        // ---- Receive JSON string from MainActivity ----
        String jsonData = getIntent().getStringExtra("METER_DATA");

        if (jsonData != null && !jsonData.trim().isEmpty()) {
            // Show meter data inside WebView
            htmlViewer.displayData(jsonData);
        } else {
            htmlViewer.displayRawHTML("<h3>No Data Found</h3>");
        }
    }
}
