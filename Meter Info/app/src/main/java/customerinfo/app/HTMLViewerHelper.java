package customerinfo.app;

import android.content.Context;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class HTMLViewerHelper {

    private Context context;
    private WebView webView;

    public HTMLViewerHelper(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    private String loadHTMLTemplate(String templateName) {
        try {
            InputStream is = context.getAssets().open(templateName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
            reader.close();
            return html.toString();
        } catch (Exception e) {
            return "<html><body><h1>Error loading template</h1></body></html>";
        }
    }

    public void displayPrepaidData(Map<String, Object> result) {
        String tpl = loadHTMLTemplate("prepaid_template.html");
        String html = populatePrepaidTemplate(tpl, result);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    public void displayPostpaidData(Map<String, Object> result) {
        String tpl = loadHTMLTemplate("postpaid_template.html");
        String html = populatePostpaidTemplate(tpl, result);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    public void clear() {
        webView.loadData("", "text/html", "UTF-8");
    }


    // ------------------ PREPAID TEMPLATE ------------------ //

    private String populatePrepaidTemplate(String template, Map<String, Object> result) {
        String html = template;

        html = html.replace("{{METER_NUMBER}}", getSafeString(result.get("meter_number")));
        html = html.replace("{{CONSUMER_NUMBER}}", getSafeString(result.get("consumer_number")));

        Object server1Data = result.get("SERVER1_data");
        if (server1Data != null) {
            Map<String, Object> cleaned = MainActivity.cleanSERVER1Data(server1Data);

            if (cleaned.containsKey("customer_info")) {
                Map<String, String> customerInfo =
                        (Map<String, String>) cleaned.get("customer_info");

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        sb.append("<div class='field'>")
                          .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                          .append("<span>").append(entry.getValue()).append("</span>")
                          .append("</div>");
                    }
                }

                html = html.replace("{{#PREPAID_CUSTOMER_INFO}}", "")
                           .replace("{{/PREPAID_CUSTOMER_INFO}}", "")
                           .replace("{{PREPAID_FIELDS}}", sb.toString());
            }
        } else {
            html = html.replace("{{#PREPAID_CUSTOMER_INFO}}", "")
                       .replace("{{/PREPAID_CUSTOMER_INFO}}", "");
        }

        // ---- TOKENS SECTION ----
        if (server1Data != null) {
            Map<String, Object> cleaned = MainActivity.cleanSERVER1Data(server1Data);

            if (cleaned.containsKey("recent_transactions")) {
                List<Map<String, String>> tx =
                        (List<Map<String, String>>) cleaned.get("recent_transactions");

                StringBuilder tokens = new StringBuilder();
                for (int i = 0; i < tx.size(); i++) {
                    Map<String, String> t = tx.get(i);

                    tokens.append("<tr>")
                          .append("<td>").append(i + 1).append("</td>")
                          .append("<td>").append(t.get("Tokens")).append("</td>")
                          .append("<td>").append(t.get("Date")).append("</td>")
                          .append("<td>").append(t.get("Amount")).append("</td>")
                          .append("<td>").append(t.get("Operator")).append("</td>")
                          .append("<td>").append(t.get("Sequence")).append("</td>")
                          .append("</tr>");
                }

                html = html.replace("{{#TOKENS}}", "")
                           .replace("{{/TOKENS}}", "")
                           .replace("{{TOKEN_ROWS}}", tokens.toString());
            }
        } else {
            html = html.replace("{{#TOKENS}}", "")
                       .replace("{{/TOKENS}}", "");
        }

        // ---- MERGED POSTPAID STYLE DATA ----
        Map<String, Object> merged = MainActivity.mergeSERVERData(result);

        if (merged != null) {

            if (merged.containsKey("customer_info")) {
                Map<String, String> info =
                        (Map<String, String>) merged.get("customer_info");

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : info.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        sb.append("<div class='field'>")
                          .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                          .append("<span>").append(entry.getValue()).append("</span>")
                          .append("</div>");
                    }
                }

                html = html.replace("{{#POSTPAID_CUSTOMER_INFO}}", "")
                           .replace("{{/POSTPAID_CUSTOMER_INFO}}", "")
                           .replace("{{POSTPAID_FIELDS}}", sb.toString());
            }

            // Bill info
            if (merged.containsKey("bill_info_raw")) {
                JSONArray billInfo = (JSONArray) merged.get("bill_info_raw");

                StringBuilder bills = new StringBuilder();

                for (int i = 0; i < Math.min(billInfo.length(), 5); i++) {
                    JSONObject bill = billInfo.optJSONObject(i);
                    if (bill == null) continue;

                    bills.append("<tr>")
                         .append("<td>").append(formatBillMonth(bill.optString("BILL_MONTH"))).append("</td>")
                         .append("<td>").append(bill.optString("BILL_NO")).append("</td>")
                         .append("<td>").append(bill.optString("CONS_KWH_SR")).append("</td>")
                         .append("<td>৳").append(bill.optString("CURRENT_BILL")).append("</td>")
                         .append("<td>").append(formatDate(bill.optString("INVOICE_DUE_DATE"))).append("</td>")
                         .append("<td>৳").append(bill.optString("PAID_AMT")).append("</td>")
                         .append("<td>").append(formatDate(bill.optString("RECEIPT_DATE"))).append("</td>")
                         .append("<td>৳").append(bill.optString("BALANCE")).append("</td>")
                         .append("</tr>");
                }

                html = html.replace("{{#BILL_INFO}}", "")
                           .replace("{{/BILL_INFO}}", "")
                           .replace("{{BILL_ROWS}}", bills.toString());
            }
        }

        // Error
        if (result.containsKey("error")) {
            html = html.replace("{{#ERROR}}", "")
                       .replace("{{/ERROR}}", "")
                       .replace("{{ERROR_MESSAGE}}", result.get("error").toString());
        } else {
            html = html.replace("{{#ERROR}}", "")
                       .replace("{{/ERROR}}", "");
        }

        return html;
    }

    // ------------------ POSTPAID TEMPLATE ------------------ //

    private String populatePostpaidTemplate(String template, Map<String, Object> result) {
        String html = template;

        html = html.replace("{{CUSTOMER_NUMBER}}", getSafeString(result.get("customer_number")));

        // For meters having multiple customers
        if (result.containsKey("customer_results")) {

            List<String> customerNumbers = (List<String>) result.get("customer_numbers");
            List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");

            StringBuilder cards = new StringBuilder();

            for (int i = 0; i < customerResults.size(); i++) {

                Map<String, Object> merged = MainActivity.mergeSERVERData(customerResults.get(i));

                cards.append("<div class='customer-card'>")
                     .append("<h4>👤 Customer ").append(i + 1).append(": ")
                     .append(customerNumbers.get(i)).append("</h4>");

                if (merged != null && merged.containsKey("customer_info")) {

                    Map<String, String> info =
                            (Map<String, String>) merged.get("customer_info");

                    for (Map.Entry<String, String> entry : info.entrySet()) {

                        if (isValidValue(entry.getValue())) {
                            cards.append("<div class='field'>")
                                 .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                                 .append("<span>").append(entry.getValue()).append("</span>")
                                 .append("</div>");
                        }
                    }
                }

                cards.append("</div>");
            }

            html = html.replace("{{#MULTIPLE_CUSTOMERS}}", "")
                       .replace("{{/MULTIPLE_CUSTOMERS}}", "")
                       .replace("{{CUSTOMER_CARDS}}", cards.toString())
                       .replace("{{CUSTOMER_COUNT}}", String.valueOf(customerNumbers.size()));

            html = html.replace("{{#SINGLE_CUSTOMER}}", "")
                       .replace("{{/SINGLE_CUSTOMER}}", "");

        } else {

            html = html.replace("{{#MULTIPLE_CUSTOMERS}}", "")
                       .replace("{{/MULTIPLE_CUSTOMERS}}", "");

            Map<String, Object> merged = MainActivity.mergeSERVERData(result);

            if (merged != null && merged.containsKey("customer_info")) {

                Map<String, String> info =
                        (Map<String, String>) merged.get("customer_info");

                StringBuilder sb = new StringBuilder();

                for (Map.Entry<String, String> entry : info.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        sb.append("<div class='field'>")
                          .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                          .append("<span>").append(entry.getValue()).append("</span>")
                          .append("</div>");
                    }
                }

                html = html.replace("{{#SINGLE_CUSTOMER}}", "")
                           .replace("{{/SINGLE_CUSTOMER}}", "")
                           .replace("{{CUSTOMER_INFO}}", sb.toString());
            }
        }

        return html;
    }


    // ------------------ HELPERS ------------------ //

    private String getSafeString(Object value) {
        if (value == null) return "N/A";
        String s = value.toString();
        return (s.equals("null") || s.isEmpty()) ? "N/A" : s;
    }

    private boolean isValidValue(String value) {
        if (value == null) return false;
        String v = value.trim();
        return !(v.isEmpty() || v.equals("null") || v.equals("{}") || v.equals("undefined"));
    }

    private String formatBillMonth(String date) {
        try {
            String[] p = date.substring(0, 10).split("-");
            if (p.length >= 2) {
                int m = Integer.parseInt(p[1]);
                String[] names = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                return names[m - 1] + " " + p[0];
            }
            return date.length() >= 7 ? date.substring(0, 7) : date;
        } catch (Exception e) {
            return date;
        }
    }

    private String formatDate(String d) {
        if (d == null || d.isEmpty() || d.equals("null")) return "—";
        return d.contains("T") ? d.split("T")[0] : d;
    }
}
