package customerinfo.app;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class MeterDataDisplayActivity extends AppCompatActivity {
    
    private WebView webView;
    private HTMLViewerHelper htmlViewer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_display);
        
        webView = findViewById(R.id.htmlWebView);
        htmlViewer = new HTMLViewerHelper(this, webView);
        
        // Get data from MainActivity
        String jsonData = getIntent().getStringExtra("METER_DATA");
        
        if (jsonData != null) {
            htmlViewer.displayDataFromJSON(jsonData);
        }
    }
}