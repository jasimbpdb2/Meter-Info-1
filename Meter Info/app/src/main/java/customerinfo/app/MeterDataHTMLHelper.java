package customerinfo.app;

import android.content.Context;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * MeterDataHTMLHelper
 * - provides processDataForHTMLDisplay(Map, meterNumber, type, subtype)
 * - fills sensible defaults and flattens merged data into a JSON-friendly map
 *
 * MainActivity expects: MeterDataHTMLHelper helper = new MeterDataHTMLHelper();
 * Map<String,Object> processed = helper.processDataForHTMLDisplay(rawData, inputNumber, type, subType);
 */
public class MeterDataHTMLHelper {

    // Add these inner classes
    public static class PrepaidData {
        public Map<String, String> customerInfo = new HashMap<>();
        public Map<String, String> balanceInfo = new HashMap<>();
        public List<Map<String, String>> recentTransactions = new ArrayList<>();
        public String meterNumber;
        public String consumerNumber;
        public String searchTime;
        public Map<String, Object> rawData = new HashMap<>();
    }

    public static class PostpaidData {
        public Map<String, String> customerInfo = new HashMap<>();
        public Map<String, String> balanceInfo = new HashMap<>();
        public List<Map<String, Object>> billInfo = new ArrayList<>();
        public String customerNumber;
        public String meterNumber;
        public String searchTime;
        public Map<String, Object> rawData = new HashMap<>();
        public List<Map<String, Object>> customerResults = new ArrayList<>();
    }

    public MeterDataHTMLHelper() { }

    // Add these conversion methods
    public static PrepaidData convertToPrepaidData(Map<String, Object> combinedResult) {
        PrepaidData prepaidData = new PrepaidData();
        
        if (combinedResult == null) return prepaidData;
        
        // Extract customer info
        if (combinedResult.get("customer_info") instanceof Map) {
            prepaidData.customerInfo = (Map<String, String>) combinedResult.get("customer_info");
        }
        
        // Extract balance info
        if (combinedResult.get("balance_info") instanceof Map) {
            prepaidData.balanceInfo = (Map<String, String>) combinedResult.get("balance_info");
        }
        
        // Extract transactions
        if (combinedResult.get("recent_transactions") instanceof List) {
            prepaidData.recentTransactions = (List<Map<String, String>>) combinedResult.get("recent_transactions");
        }
        
        // Basic info
        prepaidData.meterNumber = String.valueOf(combinedResult.getOrDefault("meter_number", ""));
        prepaidData.consumerNumber = String.valueOf(combinedResult.getOrDefault("consumer_number", ""));
        prepaidData.searchTime = new Date().toString();
        prepaidData.rawData = new HashMap<>(combinedResult);
        
        return prepaidData;
    }

    public static PostpaidData convertToPostpaidData(Map<String, Object> finalResult) {
        PostpaidData postpaidData = new PostpaidData();
        
        if (finalResult == null) return postpaidData;
        
        // Extract customer info
        if (finalResult.get("customer_info") instanceof Map) {
            postpaidData.customerInfo = (Map<String, String>) finalResult.get("customer_info");
        }
        
        // Extract balance info
        if (finalResult.get("balance_info") instanceof Map) {
            postpaidData.balanceInfo = (Map<String, String>) finalResult.get("balance_info");
        }
        
        // Extract bill info
        if (finalResult.get("bill_info_raw") instanceof List) {
            postpaidData.billInfo = (List<Map<String, Object>>) finalResult.get("bill_info_raw");
        }
        
        // Handle multiple customers for meter lookup
        if (finalResult.containsKey("customer_results") && finalResult.get("customer_results") instanceof List) {
            postpaidData.customerResults = (List<Map<String, Object>>) finalResult.get("customer_results");
        }
        
        // Basic info
        postpaidData.customerNumber = String.valueOf(finalResult.getOrDefault("customer_number", ""));
        postpaidData.meterNumber = String.valueOf(finalResult.getOrDefault("meter_number", ""));
        postpaidData.searchTime = new Date().toString();
        postpaidData.rawData = new HashMap<>(finalResult);
        
        return postpaidData;
    }

    // Add these rendering methods
    public static String renderPrepaidHTML(Context context, PrepaidData data) {
        return generatePrepaidHTML(data);
    }

    public static String renderPostpaidHTML(Context context, PostpaidData data) {
        return generatePostpaidHTML(data);
    }

    private static String generatePrepaidHTML(PrepaidData data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Prepaid Meter Information</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
        html.append(".header { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(".header h1 { color: #2c3e50; margin: 0; }");
        html.append(".card { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(".card h2 { color: #2c3e50; margin-top: 0; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
        html.append(".info-item { padding: 8px 0; border-bottom: 1px solid #eee; display: flex; }");
        html.append(".info-label { font-weight: bold; color: #34495e; min-width: 200px; }");
        html.append(".info-value { color: #2c3e50; }");
        html.append(".transaction { background: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 8px; border-left: 4px solid #3498db; }");
        html.append(".token { font-family: monospace; background: #2c3e50; color: white; padding: 8px; border-radius: 4px; margin: 5px 0; }");
        html.append("</style>");
        html.append("</head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>📊 PREPAID METER INFORMATION</h1>");
        html.append("<p><strong>Search Time:</strong> ").append(escapeHtml(data.searchTime)).append("</p>");
        html.append("</div>");
        
        // Basic Information
        html.append("<div class='card'>");
        html.append("<h2>🔢 BASIC INFORMATION</h2>");
        html.append("<div class='info-item'><span class='info-label'>Meter Number:</span><span class='info-value'>").append(escapeHtml(data.meterNumber)).append("</span></div>");
        html.append("<div class='info-item'><span class='info-label'>Consumer Number:</span><span class='info-value'>").append(escapeHtml(data.consumerNumber)).append("</span></div>");
        html.append("</div>");
        
        // Customer Information
        if (!data.customerInfo.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>👤 CUSTOMER INFORMATION</h2>");
            for (Map.Entry<String, String> entry : data.customerInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'><span class='info-label'>").append(escapeHtml(entry.getKey())).append(":</span><span class='info-value'>").append(escapeHtml(entry.getValue())).append("</span></div>");
                }
            }
            html.append("</div>");
        }
        
        // Balance Information
        if (!data.balanceInfo.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>💰 BALANCE INFORMATION</h2>");
            for (Map.Entry<String, String> entry : data.balanceInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'><span class='info-label'>").append(escapeHtml(entry.getKey())).append(":</span><span class='info-value'>").append(escapeHtml(entry.getValue())).append("</span></div>");
                }
            }
            html.append("</div>");
        }
        
        // Recent Transactions & Tokens
        if (!data.recentTransactions.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>🔑 RECENT RECHARGE TOKENS</h2>");
            for (int i = 0; i < data.recentTransactions.size() && i < 3; i++) {
                Map<String, String> transaction = data.recentTransactions.get(i);
                html.append("<div class='transaction'>");
                html.append("<h3>Order ").append(i + 1).append("</h3>");
                html.append("<div class='info-item'><span class='info-label'>📅 Date:</span><span class='info-value'>").append(escapeHtml(transaction.getOrDefault("Date", "N/A"))).append("</span></div>");
                html.append("<div class='info-item'><span class='info-label'>🧾 Order:</span><span class='info-value'>").append(escapeHtml(transaction.getOrDefault("Order Number", "N/A"))).append("</span></div>");
                html.append("<div class='info-item'><span class='info-label'>👤 Operator:</span><span class='info-value'>").append(escapeHtml(transaction.getOrDefault("Operator", "N/A"))).append("</span></div>");
                html.append("<div class='info-item'><span class='info-label'>💰 Amount:</span><span class='info-value'>").append(escapeHtml(transaction.getOrDefault("Amount", "N/A"))).append("</span></div>");
                html.append("<div class='info-item'><span class='info-label'>⚡ Energy:</span><span class='info-value'>").append(escapeHtml(transaction.getOrDefault("Energy Cost", "N/A"))).append("</span></div>");
                
                String tokens = transaction.get("Tokens");
                if (tokens != null && !tokens.equals("N/A")) {
                    html.append("<div class='info-item'><span class='info-label'>🔑 TOKENS:</span><span class='info-value'><div class='token'>").append(escapeHtml(tokens)).append("</div></span></div>");
                }
                html.append("</div>");
            }
            html.append("</div>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }

    private static String generatePostpaidHTML(PostpaidData data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Postpaid Meter Information</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
        html.append(".header { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(".header h1 { color: #2c3e50; margin: 0; }");
        html.append(".card { background: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(".card h2 { color: #2c3e50; margin-top: 0; border-bottom: 2px solid #e74c3c; padding-bottom: 10px; }");
        html.append(".info-item { padding: 8px 0; border-bottom: 1px solid #eee; display: flex; }");
        html.append(".info-label { font-weight: bold; color: #34495e; min-width: 200px; }");
        html.append(".info-value { color: #2c3e50; }");
        html.append(".bill-item { background: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 8px; border-left: 4px solid #e74c3c; }");
        html.append(".customer-section { background: #e8f4f8; padding: 15px; margin: 15px 0; border-radius: 8px; }");
        html.append("</style>");
        html.append("</head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>💡 POSTPAID METER INFORMATION</h1>");
        html.append("<p><strong>Search Time:</strong> ").append(escapeHtml(data.searchTime)).append("</p>");
        html.append("</div>");
        
        // Handle multiple customers (meter lookup)
        if (!data.customerResults.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>📊 MULTIPLE CUSTOMERS FOUND</h2>");
            html.append("<p>Found ").append(data.customerResults.size()).append(" customer(s) for this meter</p>");
            
            for (int i = 0; i < data.customerResults.size(); i++) {
                Map<String, Object> customerResult = data.customerResults.get(i);
                html.append("<div class='customer-section'>");
                html.append("<h3>👤 CUSTOMER ").append(i + 1).append("</h3>");
                
                // Extract customer info from each result
                if (customerResult.get("customer_info") instanceof Map) {
                    Map<String, String> custInfo = (Map<String, String>) customerResult.get("customer_info");
                    for (Map.Entry<String, String> entry : custInfo.entrySet()) {
                        if (isValidValue(entry.getValue())) {
                            html.append("<div class='info-item'><span class='info-label'>").append(escapeHtml(entry.getKey())).append(":</span><span class='info-value'>").append(escapeHtml(entry.getValue())).append("</span></div>");
                        }
                    }
                }
                html.append("</div>");
            }
            html.append("</div>");
        } else {
            // Single customer view
            // Basic Information
            html.append("<div class='card'>");
            html.append("<h2>🔢 BASIC INFORMATION</h2>");
            html.append("<div class='info-item'><span class='info-label'>Customer Number:</span><span class='info-value'>").append(escapeHtml(data.customerNumber)).append("</span></div>");
            if (!data.meterNumber.isEmpty() && !data.meterNumber.equals("N/A")) {
                html.append("<div class='info-item'><span class='info-label'>Meter Number:</span><span class='info-value'>").append(escapeHtml(data.meterNumber)).append("</span></div>");
            }
            html.append("</div>");
            
            // Customer Information
            if (!data.customerInfo.isEmpty()) {
                html.append("<div class='card'>");
                html.append("<h2>👤 CUSTOMER INFORMATION</h2>");
                for (Map.Entry<String, String> entry : data.customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        html.append("<div class='info-item'><span class='info-label'>").append(escapeHtml(entry.getKey())).append(":</span><span class='info-value'>").append(escapeHtml(entry.getValue())).append("</span></div>");
                    }
                }
                html.append("</div>");
            }
            
            // Balance Information
            if (!data.balanceInfo.isEmpty()) {
                html.append("<div class='card'>");
                html.append("<h2>💰 BALANCE INFORMATION</h2>");
                for (Map.Entry<String, String> entry : data.balanceInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        html.append("<div class='info-item'><span class='info-label'>").append(escapeHtml(entry.getKey())).append(":</span><span class='info-value'>").append(escapeHtml(entry.getValue())).append("</span></div>");
                    }
                }
                html.append("</div>");
            }
            
            // Bill Information
            if (!data.billInfo.isEmpty()) {
                html.append("<div class='card'>");
                html.append("<h2>📊 BILL HISTORY</h2>");
                html.append("<p>Showing ").append(data.billInfo.size()).append(" bill record(s)</p>");
                
                for (int i = 0; i < Math.min(data.billInfo.size(), 6); i++) {
                    Map<String, Object> bill = data.billInfo.get(i);
                    html.append("<div class='bill-item'>");
                    html.append("<h4>Bill ").append(i + 1).append("</h4>");
                    
                    for (Map.Entry<String, Object> entry : bill.entrySet()) {
                        if (isValidValue(String.valueOf(entry.getValue()))) {
                            html.append("<div class='info-item'><span class='info-label'>").append(escapeHtml(entry.getKey())).append(":</span><span class='info-value'>").append(escapeHtml(String.valueOf(entry.getValue()))).append("</span></div>");
                        }
                    }
                    html.append("</div>");
                }
                html.append("</div>");
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }

    // Utility methods
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private static boolean isValidValue(String value) {
        if (value == null) return false;
        String trimmedValue = value.trim();
        return !trimmedValue.isEmpty() &&
                !trimmedValue.equals("N/A") &&
                !trimmedValue.equals("null") &&
                !trimmedValue.equals("{}") &&
                !trimmedValue.equals("undefined");
    }

    // Your existing methods below
    @SuppressWarnings("unchecked")
    public Map<String, Object> processDataForHTMLDisplay(Map<String, Object> raw, String meterNumber, String type, String subType) {
        Map<String, Object> out = new HashMap<>();

        // copy some top-level fields
        out.put("meter_number", raw.getOrDefault("meter_number", meterNumber));
        out.put("consumer_number", raw.getOrDefault("consumer_number", raw.get("customer_number")));
        out.put("search_input", raw.getOrDefault("search_input", meterNumber));
        out.put("search_type", raw.getOrDefault("search_type", type));
        out.put("timestamp", raw.getOrDefault("timestamp", new Date().toString()));

        // merged data: MainActivity's mergeSERVERData produced merged structures under keys
        Map<String, Object> merged = null;
        try {
            // If MainActivity already added merged data, prefer it
            if (raw.containsKey("customer_info") && raw.get("customer_info") instanceof Map) {
                // some callers might put customer_info at root
                merged = new HashMap<>();
                merged.put("customer_info", raw.get("customer_info"));
            } else if (raw.containsKey("merged")) {
                merged = (Map<String,Object>) raw.get("merged");
            } else {
                // Try calling mergeSERVERData equivalent -- but we don't re-run; prefer SERVER2/SERVER3 cleaned data
                merged = new HashMap<>();
                if (raw.containsKey("SERVER2_data")) merged.put("SERVER2_raw", raw.get("SERVER2_data"));
                if (raw.containsKey("SERVER3_data")) merged.put("SERVER3_raw", raw.get("SERVER3_data"));
            }
        } catch (Exception e) {
            merged = new HashMap<>();
        }

        // customer_info / balance_info / transactions / bills
        // Try to extract from known keys MainActivity uses:
        Map<String, String> customerInfo = collectMapStringString(raw, "customer_info");
        if (customerInfo.isEmpty() && merged.containsKey("customer_info")) {
            Object mm = merged.get("customer_info");
            if (mm instanceof Map) customerInfo = (Map<String,String>) mm;
        }
        out.put("customer_info", customerInfo);

        Map<String, String> balanceInfo = collectMapStringString(raw, "balance_info");
        if (balanceInfo.isEmpty() && merged.containsKey("balance_info")) {
            Object mm = merged.get("balance_info");
            if (mm instanceof Map) balanceInfo = (Map<String,String>)mm;
        }
        out.put("balance_info", balanceInfo);

        // recent transactions/tokens
        List<Map<String, String>> transactions = new ArrayList<>();
        if (raw.containsKey("SERVER1_data")) {
            // MainActivity parses tokens into recent_transactions when showing console text,
            // but it also put cleaned value under cleaned SERVER1 if used earlier. Try common keys:
            if (raw.containsKey("recent_transactions") && raw.get("recent_transactions") instanceof List) {
                transactions = (List<Map<String,String>>) raw.get("recent_transactions");
            }
            // else try server1 cleaned -> in your flow cleanSERVER1Data returns recent_transactions
            if (raw.get("SERVER1_data") instanceof Map) {
                Map s1 = (Map) raw.get("SERVER1_data");
                if (s1.containsKey("recent_transactions") && s1.get("recent_transactions") instanceof List) {
                    transactions = (List<Map<String,String>>) s1.get("recent_transactions");
                }
            }
        }
        // fallback to top-level key names
        if (transactions.isEmpty()) {
            if (raw.containsKey("recent_transactions") && raw.get("recent_transactions") instanceof List) {
                transactions = (List<Map<String,String>>) raw.get("recent_transactions");
            }
        }
        out.put("recent_transactions", transactions);

        // bills: expect bill_info_raw (JSONArray) or billInfo
        Object billsObj = raw.get("bill_info_raw");
        if (billsObj == null) billsObj = raw.get("billInfo");
        if (billsObj instanceof org.json.JSONArray) {
            // convert to java list of maps
            List<Map<String, Object>> bills = new ArrayList<>();
            org.json.JSONArray arr = (org.json.JSONArray) billsObj;
            for (int i = 0; i < arr.length(); i++) {
                try {
                    org.json.JSONObject o = arr.getJSONObject(i);
                    Map<String, Object> m = jsonObjectToMap(o);
                    bills.add(m);
                } catch (Exception ignored) {}
            }
            out.put("bill_info_raw", bills);
        } else if (billsObj instanceof List) {
            out.put("bill_info_raw", billsObj);
        } else {
            out.put("bill_info_raw", new ArrayList<>());
        }

        // merge any other helpful keys
        if (raw.containsKey("data_source")) out.put("data_source", raw.get("data_source"));

        // this final map will be serialized by MainActivity into JSON and embedded in the HTML as __METER_DATA__
        return out;
    }

    private Map<String,String> collectMapStringString(Map<String,Object> raw, String key) {
        Map<String,String> map = new HashMap<>();
        try {
            if (raw.containsKey(key) && raw.get(key) instanceof Map) {
                Map m = (Map) raw.get(key);
                for (Object k : m.keySet()) {
                    Object v = m.get(k);
                    map.put(String.valueOf(k), v==null?"":String.valueOf(v));
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private Map<String,Object> jsonObjectToMap(JSONObject o) {
        Map<String,Object> m = new HashMap<>();
        try {
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Object v = o.get(k);
                m.put(k, v);
            }
        } catch (Exception ignored) {}
        return m;
    }
}