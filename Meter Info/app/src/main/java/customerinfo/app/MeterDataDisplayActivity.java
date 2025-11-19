package customerinfo.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.lang.Exception

class MeterDataDisplayActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meter_display)

        webView = findViewById(R.id.webView)
        
        // Enable JavaScript (if needed)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        // Set WebView client to handle links internally
        webView.webViewClient = WebViewClient()

        // Get the data from intent
        val meterDataJson = intent.getStringExtra("METER_DATA")
        if (meterDataJson != null) {
            try {
                val jsonObject = JSONObject(meterDataJson)
                val resultMap = jsonObjectToMap(jsonObject)
                
                // Determine if it's prepaid or postpaid based on available fields
                val isPrepaid = resultMap.containsKey("meter_number") && resultMap.containsKey("consumer_number")
                
                val htmlContent = if (isPrepaid) {
                    val prepaidData = TemplateHelper.convertToPrepaidData(resultMap)
                    TemplateHelper.renderPrepaidTemplate(this, prepaidData)
                } else {
                    val postpaidData = TemplateHelper.convertToPostpaidData(resultMap)
                    TemplateHelper.renderPostpaidTemplate(this, postpaidData)
                }
                
                // Load the HTML content
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
                
            } catch (e: Exception) {
                // Show error page
                webView.loadDataWithBaseURL(
                    null,
                    """
                    <html>
                    <body style="font-family: Arial; padding: 20px; background: #f5f5f5;">
                        <div style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                            <h2 style="color: #e74c3c;">❌ Error</h2>
                            <p>Failed to load meter data: ${e.message}</p>
                            <p>Please try again.</p>
                        </div>
                    </body>
                    </html>
                    """,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        } else {
            // No data provided
            webView.loadDataWithBaseURL(
                null,
                """
                <html>
                <body style="font-family: Arial; padding: 20px; background: #f5f5f5;">
                    <div style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <h2 style="color: #e74c3c;">❌ No Data</h2>
                        <p>No meter data provided. Please go back and search again.</p>
                    </div>
                </body>
                </html>
                """,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    // Helper function to convert JSONObject to Map
    private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonObjectToMap(value)
                else -> value
            }
        }
        return map
    }

    // Handle back button
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
