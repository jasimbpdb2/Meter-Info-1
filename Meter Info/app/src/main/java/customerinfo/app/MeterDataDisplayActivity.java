package customerinfo.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MeterDataDisplayActivity extends AppCompatActivity {

    private WebView webView;
    private Button backButton, shareButton;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_data_display);

        initializeViews();
        setupWebView();
        loadHTMLContent();
        setupClickListeners();
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        backButton = findViewById(R.id.backButton);
        shareButton = findViewById(R.id.shareButton);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Page loaded successfully
                shareButton.setEnabled(true);
            }
        });
    }

    private void loadHTMLContent() {
        String htmlContent = getIntent().getStringExtra("HTML_CONTENT");
        String inputNumber = getIntent().getStringExtra("INPUT_NUMBER");
        String accountType = getIntent().getStringExtra("ACCOUNT_TYPE");

        if (htmlContent != null && !htmlContent.isEmpty()) {
            // Load HTML content with base URL for proper rendering
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            );
            
            // Update title based on account type
            String title = accountType.equals("prepaid") ? 
                "Prepaid Meter: " + inputNumber : "Postpaid Account: " + inputNumber;
            setTitle(title);
            
        } else {
            showError("No HTML content available");
            finish();
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        shareButton.setOnClickListener(v -> shareHTMLContent());
    }

    private void shareHTMLContent() {
        try {
            String htmlContent = getIntent().getStringExtra("HTML_CONTENT");
            String inputNumber = getIntent().getStringExtra("INPUT_NUMBER");
            String accountType = getIntent().getStringExtra("ACCOUNT_TYPE");
            
            if (htmlContent != null) {
                // Create a plain text version for sharing
                String shareText = createShareableText(htmlContent, inputNumber, accountType);
                
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
                    accountType.equals("prepaid") ? "Prepaid Meter Information" : "Postpaid Account Information");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
                
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Meter Information"));
            } else {
                Toast.makeText(this, "No content to share", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing content", Toast.LENGTH_SHORT).show();
        }
    }

    private String createShareableText(String htmlContent, String inputNumber, String accountType) {
        StringBuilder text = new StringBuilder();
        
        text.append(accountType.equals("prepaid") ? "📱 PREPAID METER INFORMATION\n" : "💡 POSTPAID ACCOUNT INFORMATION\n");
        text.append("========================================\n\n");
        text.append("Account: ").append(inputNumber).append("\n");
        text.append("Type: ").append(accountType.toUpperCase()).append("\n");
        text.append("Generated: ").append(java.time.LocalDateTime.now().toString()).append("\n\n");
        text.append("View full details in the app for complete information including:\n");
        text.append("• Customer Details\n");
        text.append("• Meter Information\n");
        text.append("• Billing Summary\n");
        text.append("• Balance Information\n");
        
        if (accountType.equals("prepaid")) {
            text.append("• Recharge Tokens\n");
        }
        
        text.append("• Bill History Table\n\n");
        text.append("--- Generated by Customer Info App ---");
        
        return text.toString();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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