package customerinfo.app;

import android.content.Context;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTMLViewerHelper {
    private Context context;
    private WebView webView;

    public HTMLViewerHelper(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    // Load HTML template from assets
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

    // Display prepaid data in HTML
    public void displayPrepaidData(Map<String, Object> result) {
        String htmlTemplate = loadHTMLTemplate("prepaid_template.html");
        String finalHTML = populatePrepaidTemplate(htmlTemplate, result);
        webView.loadDataWithBaseURL(null, finalHTML, "text/html", "UTF-8", null);
    }

    // Display postpaid data in HTML
    public void displayPostpaidData(Map<String, Object> result) {
        String htmlTemplate = loadHTMLTemplate("postpaid_template.html");
        String finalHTML = populatePostpaidTemplate(htmlTemplate, result);
        webView.loadDataWithBaseURL(null, finalHTML, "text/html", "UTF-8", null);
    }

    // Clear HTML view
    public void clear() {
        webView.loadData("", "text/html", "UTF-8");
    }

    // Populate prepaid template with data
    private String populatePrepaidTemplate(String template, Map<String, Object> result) {
        String html = template;

        // Basic information
        html = html.replace("{{METER_NUMBER}}", getSafeString(result.get("meter_number")));
        html = html.replace("{{CONSUMER_NUMBER}}", getSafeString(result.get("consumer_number")));

        // Prepaid customer info (from SERVER1)
        Object server1Data = result.get("SERVER1_data");
        if (server1Data != null) {
            Map<String, Object> cleanedData = cleanSERVER1Data(server1Data);
            if (cleanedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) cleanedData.get("customer_info");
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.append("<div class='field'>")
                              .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                              .append("<span>").append(entry.getValue()).append("</span>")
                              .append("</div>");
                    }
                }
                html = html.replace("{{#PREPAID_CUSTOMER_INFO}}", "")
                          .replace("{{/PREPAID_CUSTOMER_INFO}}", "")
                          .replace("{{#FIELDS}}", fields.toString())
                          .replace("{{/FIELDS}}", "");
            }
        } else {
            html = html.replace("{{#PREPAID_CUSTOMER_INFO}}", "")
                      .replace("{{/PREPAID_CUSTOMER_INFO}}", "");
        }

        // Tokens
        if (server1Data != null) {
            Map<String, Object> cleanedData = cleanSERVER1Data(server1Data);
            if (cleanedData.containsKey("recent_transactions")) {
                List<Map<String, String>> transactions = (List<Map<String, String>>) cleanedData.get("recent_transactions");
                StringBuilder tokenList = new StringBuilder();
                for (int i = 0; i < transactions.size(); i++) {
                    Map<String, String> transaction = transactions.get(i);
                    tokenList.append("<div class='token'>")
                            .append("<strong>Order ").append(i + 1).append(":</strong><br>")
                            .append("<strong>Token:</strong> ").append(transaction.get("Tokens")).append("<br>")
                            .append("<strong>Date:</strong> ").append(transaction.get("Date")).append("<br>")
                            .append("<strong>Amount:</strong> ").append(transaction.get("Amount")).append("<br>")
                            .append("<strong>Operator:</strong> ").append(transaction.get("Operator")).append("<br>")
                            .append("<strong>Sequence:</strong> ").append(transaction.get("Sequence"))
                            .append("</div>");
                }
                html = html.replace("{{#TOKENS}}", "")
                          .replace("{{/TOKENS}}", "")
                          .replace("{{#TOKEN_LIST}}", tokenList.toString())
                          .replace("{{/TOKEN_LIST}}", "");
            }
        } else {
            html = html.replace("{{#TOKENS}}", "")
                      .replace("{{/TOKENS}}", "");
        }

        // Postpaid-style customer info (from merged data)
        Map<String, Object> mergedData = mergeSERVERData(result);
        if (mergedData != null) {
            // Customer info
            if (mergedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.append("<div class='field'>")
                              .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                              .append("<span>").append(entry.getValue()).append("</span>")
                              .append("</div>");
                    }
                }
                html = html.replace("{{#POSTPAID_CUSTOMER_INFO}}", "")
                          .replace("{{/POSTPAID_CUSTOMER_INFO}}", "")
                          .replace("{{#FIELDS}}", fields.toString())
                          .replace("{{/FIELDS}}", "");
            }

            // Bill info
            if (mergedData.containsKey("bill_info_raw")) {
                JSONArray billInfo = (JSONArray) mergedData.get("bill_info_raw");
                StringBuilder billsHTML = new StringBuilder();
                for (int i = 0; i < Math.min(billInfo.length(), 5); i++) {
                    try {
                        JSONObject bill = billInfo.getJSONObject(i);
                        billsHTML.append("<tr>")
                                .append("<td>").append(formatBillMonth(bill.optString("BILL_MONTH"))).append("</td>")
                                .append("<td>").append(bill.optString("BILL_NO")).append("</td>")
                                .append("<td>").append(bill.optDouble("CONS_KWH_SR", 0)).append("</td>")
                                .append("<td>৳").append(bill.optDouble("CURRENT_BILL", 0)).append("</td>")
                                .append("<td>").append(formatDate(bill.optString("INVOICE_DUE_DATE"))).append("</td>")
                                .append("<td>৳").append(bill.optDouble("PAID_AMT", 0)).append("</td>")
                                .append("<td>").append(formatDate(bill.optString("RECEIPT_DATE"))).append("</td>")
                                .append("<td>৳").append(bill.optDouble("BALANCE", 0)).append("</td>")
                                .append("</tr>");
                    } catch (Exception e) {
                        // Skip invalid bills
                    }
                }
                html = html.replace("{{#BILL_INFO}}", "")
                          .replace("{{/BILL_INFO}}", "")
                          .replace("{{#BILLS}}", billsHTML.toString())
                          .replace("{{/BILLS}}", "");
            }

            // Balance info
            if (mergedData.containsKey("balance_info")) {
                Map<String, String> balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.append("<div class='field'>")
                              .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                              .append("<span>").append(entry.getValue()).append("</span>")
                              .append("</div>");
                    }
                }
                html = html.replace("{{#BALANCE_INFO}}", "")
                          .replace("{{/BALANCE_INFO}}", "")
                          .replace("{{#FIELDS}}", fields.toString())
                          .replace("{{/FIELDS}}", "");
            }

            // Bill summary
            if (mergedData.containsKey("bill_summary")) {
                Map<String, Object> billSummary = (Map<String, Object>) mergedData.get("bill_summary");
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, Object> entry : billSummary.entrySet()) {
                    if (isValidValue(entry.getValue().toString())) {
                        fields.append("<div class='field'>")
                              .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                              .append("<span>").append(entry.getValue()).append("</span>")
                              .append("</div>");
                    }
                }
                html = html.replace("{{#BILL_SUMMARY}}", "")
                          .replace("{{/BILL_SUMMARY}}", "")
                          .replace("{{#FIELDS}}", fields.toString())
                          .replace("{{/FIELDS}}", "");
            }
        } else {
            html = html.replace("{{#POSTPAID_CUSTOMER_INFO}}", "")
                      .replace("{{/POSTPAID_CUSTOMER_INFO}}", "")
                      .replace("{{#BILL_INFO}}", "")
                      .replace("{{/BILL_INFO}}", "")
                      .replace("{{#BALANCE_INFO}}", "")
                      .replace("{{/BALANCE_INFO}}", "")
                      .replace("{{#BILL_SUMMARY}}", "")
                      .replace("{{/BILL_SUMMARY}}", "");
        }

        // Error handling
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

    // Populate postpaid template with data
    private String populatePostpaidTemplate(String template, Map<String, Object> result) {
        String html = template;

        // Basic information
        html = html.replace("{{CUSTOMER_NUMBER}}", getSafeString(result.get("customer_number")));

        // Multiple customers (meter lookup)
        if (result.containsKey("customer_results")) {
            List<String> customerNumbers = (List<String>) result.get("customer_numbers");
            List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");
            
            StringBuilder customersHTML = new StringBuilder();
            for (int i = 0; i < customerResults.size(); i++) {
                Map<String, Object> customerResult = customerResults.get(i);
                Map<String, Object> mergedData = mergeSERVERData(customerResult);
                
                customersHTML.append("<div class='customer-card'>")
                            .append("<h4>👤 Customer ").append(i + 1).append(": ").append(customerNumbers.get(i)).append("</h4>");
                
                if (mergedData != null && mergedData.containsKey("customer_info")) {
                    Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                    for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                        if (isValidValue(entry.getValue())) {
                            customersHTML.append("<div class='field'>")
                                        .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                                        .append("<span>").append(entry.getValue()).append("</span>")
                                        .append("</div>");
                        }
                    }
                }
                customersHTML.append("</div>");
            }
            
            html = html.replace("{{#MULTIPLE_CUSTOMERS}}", "")
                      .replace("{{/MULTIPLE_CUSTOMERS}}", "")
                      .replace("{{CUSTOMER_COUNT}}", String.valueOf(customerNumbers.size()))
                      .replace("{{#CUSTOMERS}}", customersHTML.toString())
                      .replace("{{/CUSTOMERS}}", "")
                      .replace("{{#SINGLE_CUSTOMER}}", "")
                      .replace("{{/SINGLE_CUSTOMER}}", "");
        } else {
            // Single customer
            html = html.replace("{{#MULTIPLE_CUSTOMERS}}", "")
                      .replace("{{/MULTIPLE_CUSTOMERS}}", "");
            
            Map<String, Object> mergedData = mergeSERVERData(result);
            if (mergedData != null && mergedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.append("<div class='field'>")
                              .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                              .append("<span>").append(entry.getValue()).append("</span>")
                              .append("</div>");
                    }
                }
                html = html.replace("{{#SINGLE_CUSTOMER}}", "")
                          .replace("{{/SINGLE_CUSTOMER}}", "")
                          .replace("{{#CUSTOMER_INFO}}", fields.toString())
                          .replace("{{/CUSTOMER_INFO}}", "");
            } else {
                html = html.replace("{{#SINGLE_CUSTOMER}}", "")
                          .replace("{{/SINGLE_CUSTOMER}}", "");
            }

            // Bill info, balance info, bill summary (same as prepaid)
            if (mergedData != null) {
                // Bill info
                if (mergedData.containsKey("bill_info_raw")) {
                    JSONArray billInfo = (JSONArray) mergedData.get("bill_info_raw");
                    StringBuilder billsHTML = new StringBuilder();
                    for (int i = 0; i < Math.min(billInfo.length(), 5); i++) {
                        try {
                            JSONObject bill = billInfo.getJSONObject(i);
                            billsHTML.append("<tr>")
                                    .append("<td>").append(formatBillMonth(bill.optString("BILL_MONTH"))).append("</td>")
                                    .append("<td>").append(bill.optString("BILL_NO")).append("</td>")
                                    .append("<td>").append(bill.optDouble("CONS_KWH_SR", 0)).append("</td>")
                                    .append("<td>৳").append(bill.optDouble("CURRENT_BILL", 0)).append("</td>")
                                    .append("<td>").append(formatDate(bill.optString("INVOICE_DUE_DATE"))).append("</td>")
                                    .append("<td>৳").append(bill.optDouble("PAID_AMT", 0)).append("</td>")
                                    .append("<td>").append(formatDate(bill.optString("RECEIPT_DATE"))).append("</td>")
                                    .append("<td>৳").append(bill.optDouble("BALANCE", 0)).append("</td>")
                                    .append("</tr>");
                        } catch (Exception e) {
                            // Skip invalid bills
                        }
                    }
                    html = html.replace("{{#BILL_INFO}}", "")
                              .replace("{{/BILL_INFO}}", "")
                              .replace("{{#BILLS}}", billsHTML.toString())
                              .replace("{{/BILLS}}", "");
                }

                // Balance info
                if (mergedData.containsKey("balance_info")) {
                    Map<String, String> balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                    StringBuilder fields = new StringBuilder();
                    for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
                        if (isValidValue(entry.getValue())) {
                            fields.append("<div class='field'>")
                                  .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                                  .append("<span>").append(entry.getValue()).append("</span>")
                                  .append("</div>");
                        }
                    }
                    html = html.replace("{{#BALANCE_INFO}}", "")
                              .replace("{{/BALANCE_INFO}}", "")
                              .replace("{{#FIELDS}}", fields.toString())
                              .replace("{{/FIELDS}}", "");
                }

                // Bill summary
                if (mergedData.containsKey("bill_summary")) {
                    Map<String, Object> billSummary = (Map<String, Object>) mergedData.get("bill_summary");
                    StringBuilder fields = new StringBuilder();
                    for (Map.Entry<String, Object> entry : billSummary.entrySet()) {
                        if (isValidValue(entry.getValue().toString())) {
                            fields.append("<div class='field'>")
                                  .append("<span><strong>").append(entry.getKey()).append(":</strong></span>")
                                  .append("<span>").append(entry.getValue()).append("</span>")
                                  .append("</div>");
                        }
                    }
                    html = html.replace("{{#BILL_SUMMARY}}", "")
                              .replace("{{/BILL_SUMMARY}}", "")
                              .replace("{{#FIELDS}}", fields.toString())
                              .replace("{{/FIELDS}}", "");
                }
            }
        }

        // Error handling
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

    // Helper methods (copy from your MainActivity)
    private String getSafeString(Object value) {
        if (value == null) return "N/A";
        String stringValue = value.toString();
        return (stringValue.equals("null") || stringValue.isEmpty()) ? "N/A" : stringValue;
    }

    private boolean isValidValue(String value) {
        if (value == null) return false;
        String trimmedValue = value.trim();
        return !trimmedValue.isEmpty() &&
                !trimmedValue.equals("N/A") &&
                !trimmedValue.equals("null") &&
                !trimmedValue.equals("{}") &&
                !trimmedValue.equals("undefined");
    }

    private String formatBillMonth(String dateStr) {
        try {
            String[] parts = dateStr.substring(0, 10).split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                return month >= 1 && month <= 12 ? monthNames[month-1] + " " + parts[0] : dateStr.substring(0,7);
            }
            return dateStr.length() >= 7 ? dateStr.substring(0,7) : dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) return "—";
        try {
            return dateString.contains("T") ? dateString.split("T")[0] : dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    // These methods need to be copied from your MainActivity:
    public Map<String, Object> cleanSERVER1Data(Object SERVER1DataObj) {
        // COPY THE EXACT cleanSERVER1Data METHOD FROM YOUR MainActivity
        return MainActivity.cleanSERVER1Data(SERVER1DataObj);
    }

    public Map<String, Object> mergeSERVERData(Map<String, Object> result) {
        // COPY THE EXACT mergeSERVERData METHOD FROM YOUR MainActivity
        return MainActivity.mergeSERVERData(result);
    }
}
