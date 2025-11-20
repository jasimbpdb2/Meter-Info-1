package customerinfo.app;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TemplateHelper {

    public static class PrepaidData {
        public String meterNumber;
        public String consumerNumber;
        public Map<String, String> customerInfo;
        public List<Map<String, String>> recentTransactions;
        public Map<String, String> personalInfo;
        public Map<String, String> meterInfo;
        public Map<String, String> billingInfo;
        public Map<String, String> technicalInfo;
        public Map<String, String> readingInfo;
        public Map<String, String> billSummary;
        public Map<String, String> balanceInfo;
        public JSONArray billTableData;
        public String searchTime;
    }

    public static class PostpaidData {
        public String customerNumber;
        public String meterNumber;
        public Map<String, String> customerInfo;
        public Map<String, String> personalInfo;
        public Map<String, String> meterInfo;
        public Map<String, String> billingInfo;
        public Map<String, String> technicalInfo;
        public Map<String, String> readingInfo;
        public Map<String, String> billSummary;
        public Map<String, String> balanceInfo;
        public JSONArray billTableData;
        public String searchTime;
        public List<CustomerResult> multipleCustomers;
    }

    public static class CustomerResult {
        public String customerNumber;
        public Map<String, String> customerInfo;
        public Map<String, String> billSummary;
        public Map<String, String> balanceInfo;
    }

    // Convert MainActivity result to PrepaidData
    public static PrepaidData convertToPrepaidData(Map<String, Object> result) {
        PrepaidData data = new PrepaidData();
        
        data.meterNumber = getSafeString(result.get("meter_number"));
        data.consumerNumber = getSafeString(result.get("consumer_number"));
        data.searchTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
        
        // Use MainActivity's static methods to get processed data
        Map<String, Object> mergedData = MainActivity.mergeSERVERData(result);
        Map<String, Object> cleanedSERVER1 = MainActivity.cleanSERVER1Data(result.get("SERVER1_data"));
        
        // Combine all data
        if (mergedData != null) {
            // Customer Info sections from merged data
            if (mergedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                data.customerInfo = customerInfo;
                
                // Split into sections like MainActivity
                data.personalInfo = extractSection(customerInfo, new String[]{
                    "Customer Name", "Father Name", "Customer Address"
                });
                
                data.meterInfo = extractSection(customerInfo, new String[]{
                    "Meter Number", "Meter Condition", "Meter Status", "Connection Date"
                });
                
                data.billingInfo = extractSection(customerInfo, new String[]{
                    "Customer Number", "Location Code", "Area Code", "Bill Group", 
                    "Book Number", "Tariff Description", "Sanctioned Load", "Walk Order", "Account_Number"
                });
                
                data.technicalInfo = extractSection(customerInfo, new String[]{
                    "Usage Type", "Description", "Start Bill Cycle"
                });
                
                data.readingInfo = extractSection(customerInfo, new String[]{
                    "Arrear Amount", "Current Reading SR", "Last Bill Reading SR",
                    "Last Bill Reading OF PK", "Last Bill Reading PK"
                });
            }
            
            // Balance Info
            if (mergedData.containsKey("balance_info")) {
                data.balanceInfo = (Map<String, String>) mergedData.get("balance_info");
            }
            
            // Bill Summary
            if (mergedData.containsKey("bill_summary")) {
                Map<String, Object> billSummaryObj = (Map<String, Object>) mergedData.get("bill_summary");
                data.billSummary = new HashMap<>();
                data.billSummary.put("first_bill_period", getSafeString(billSummaryObj.get("first_bill_period")));
                data.billSummary.put("last_bill_period", getSafeString(billSummaryObj.get("last_bill_period")));
                data.billSummary.put("total_bills", getSafeString(billSummaryObj.get("total_bills")));
                data.billSummary.put("total_amount", getSafeString(billSummaryObj.get("total_amount")));
                data.billSummary.put("total_paid", getSafeString(billSummaryObj.get("total_paid")));
                data.billSummary.put("arrears", getSafeString(billSummaryObj.get("arrears")));
            }
            
            // Bill Table Data
            if (mergedData.containsKey("bill_info_raw")) {
                data.billTableData = (JSONArray) mergedData.get("bill_info_raw");
            }
        }
        
        // Prepaid specific data from SERVER1
        if (cleanedSERVER1 != null) {
            if (cleanedSERVER1.containsKey("recent_transactions")) {
                data.recentTransactions = (List<Map<String, String>>) cleanedSERVER1.get("recent_transactions");
            }
            if (cleanedSERVER1.containsKey("customer_info") && data.customerInfo == null) {
                data.customerInfo = (Map<String, String>) cleanedSERVER1.get("customer_info");
            }
        }
        
        return data;
    }

    // Convert MainActivity result to PostpaidData
    public static PostpaidData convertToPostpaidData(Map<String, Object> result) {
        PostpaidData data = new PostpaidData();
        
        data.customerNumber = getSafeString(result.get("customer_number"));
        data.meterNumber = getSafeString(result.get("meter_number"));
        data.searchTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
        
        // Use MainActivity's static method to get processed data
        Map<String, Object> mergedData = MainActivity.mergeSERVERData(result);
        
        if (mergedData != null) {
            // Handle multiple customers from meter lookup
            if (result.containsKey("customer_results")) {
                data.multipleCustomers = new ArrayList<>();
                List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");
                List<String> customerNumbers = (List<String>) result.get("customer_numbers");
                
                for (int i = 0; i < customerResults.size(); i++) {
                    CustomerResult customer = new CustomerResult();
                    customer.customerNumber = customerNumbers.get(i);
                    
                    Map<String, Object> customerMergedData = MainActivity.mergeSERVERData(customerResults.get(i));
                    if (customerMergedData != null) {
                        if (customerMergedData.containsKey("customer_info")) {
                            customer.customerInfo = (Map<String, String>) customerMergedData.get("customer_info");
                        }
                        if (customerMergedData.containsKey("bill_summary")) {
                            Map<String, Object> billSummaryObj = (Map<String, Object>) customerMergedData.get("bill_summary");
                            customer.billSummary = new HashMap<>();
                            customer.billSummary.put("total_bills", getSafeString(billSummaryObj.get("total_bills")));
                            customer.billSummary.put("total_amount", getSafeString(billSummaryObj.get("total_amount")));
                            customer.billSummary.put("arrears", getSafeString(billSummaryObj.get("arrears")));
                        }
                        if (customerMergedData.containsKey("balance_info")) {
                            customer.balanceInfo = (Map<String, String>) customerMergedData.get("balance_info");
                        }
                    }
                    data.multipleCustomers.add(customer);
                }
            } else {
                // Single customer
                if (mergedData.containsKey("customer_info")) {
                    Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                    data.customerInfo = customerInfo;
                    
                    // Split into sections
                    data.personalInfo = extractSection(customerInfo, new String[]{
                        "Customer Name", "Father Name", "Customer Address"
                    });
                    
                    data.meterInfo = extractSection(customerInfo, new String[]{
                        "Meter Number", "Meter Condition", "Meter Status", "Connection Date"
                    });
                    
                    data.billingInfo = extractSection(customerInfo, new String[]{
                        "Customer Number", "Location Code", "Area Code", "Bill Group", 
                        "Book Number", "Tariff Description", "Sanctioned Load", "Walk Order", "Account_Number"
                    });
                    
                    data.technicalInfo = extractSection(customerInfo, new String[]{
                        "Usage Type", "Description", "Start Bill Cycle"
                    });
                    
                    data.readingInfo = extractSection(customerInfo, new String[]{
                        "Arrear Amount", "Current Reading SR", "Last Bill Reading SR",
                        "Last Bill Reading OF PK", "Last Bill Reading PK"
                    });
                }
                
                if (mergedData.containsKey("balance_info")) {
                    data.balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                }
                
                if (mergedData.containsKey("bill_summary")) {
                    Map<String, Object> billSummaryObj = (Map<String, Object>) mergedData.get("bill_summary");
                    data.billSummary = new HashMap<>();
                    data.billSummary.put("first_bill_period", getSafeString(billSummaryObj.get("first_bill_period")));
                    data.billSummary.put("last_bill_period", getSafeString(billSummaryObj.get("last_bill_period")));
                    data.billSummary.put("total_bills", getSafeString(billSummaryObj.get("total_bills")));
                    data.billSummary.put("total_amount", getSafeString(billSummaryObj.get("total_amount")));
                    data.billSummary.put("total_paid", getSafeString(billSummaryObj.get("total_paid")));
                    data.billSummary.put("arrears", getSafeString(billSummaryObj.get("arrears")));
                }
                
                if (mergedData.containsKey("bill_info_raw")) {
                    data.billTableData = (JSONArray) mergedData.get("bill_info_raw");
                }
            }
        }
        
        return data;
    }

    // HTML Template Rendering (KEEP YOUR EXISTING RENDER METHODS AS THEY ARE)
    public static String renderPrepaidTemplate(Context context, PrepaidData data) {
        // YOUR EXISTING renderPrepaidTemplate METHOD - DON'T CHANGE IT
        // This should already have all the beautiful HTML formatting
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Prepaid Meter Information</title>");
        html.append("<style>");
        html.append(getCommonCSS());
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>📊 PREPAID METER INFORMATION</h1>");
        html.append("<div class='search-info'>");
        html.append("<p><strong>Search Time:</strong> ").append(data.searchTime).append("</p>");
        html.append("</div>");
        html.append("</div>");
        
        // Continue with your existing HTML rendering logic...
        // Basic Info Card
        html.append("<div class='card'>");
        html.append("<h2>🔢 Basic Information</h2>");
        html.append("<div class='info-grid'>");
        html.append("<div><strong>Meter Number:</strong> ").append(escapeHtml(data.meterNumber)).append("</div>");
        html.append("<div><strong>Consumer Number:</strong> ").append(escapeHtml(data.consumerNumber)).append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Add all your other sections here...
        // Prepaid Customer Details, Recent Tokens, Customer Sections, Bill Summary, etc.
        
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    public static String renderPostpaidTemplate(Context context, PostpaidData data) {
        // YOUR EXISTING renderPostpaidTemplate METHOD - DON'T CHANGE IT
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Postpaid Meter Information</title>");
        html.append("<style>");
        html.append(getCommonCSS());
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // Your existing postpaid HTML rendering logic...
        
        html.append("</body>");
        html.append("</html>");
        
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
        String trimmed = value.trim();
        return !trimmed.isEmpty() && 
               !trimmed.equals("N/A") && 
               !trimmed.equals("null") && 
               !trimmed.equals("undefined") &&
               !trimmed.equals("{}");
    }

    private static String getSafeString(Object obj) {
        if (obj == null) return "N/A";
        String str = obj.toString();
        return str.equals("null") || str.isEmpty() ? "N/A" : str;
    }

    private static Map<String, String> extractSection(Map<String, String> source, String[] keys) {
        Map<String, String> section = new HashMap<>();
        for (String key : keys) {
            if (source.containsKey(key) && isValidValue(source.get(key))) {
                section.put(key, source.get(key));
            }
        }
        return section;
    }

    private static String getCommonCSS() {
        return "*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;padding:20px}.header{background:white;padding:25px;border-radius:15px;box-shadow:0 8px 25px rgba(0,0,0,0.1);margin-bottom:25px;text-align:center;border-left:5px solid #3498db}.header h1{color:#2c3e50;margin-bottom:10px;font-size:24px}.search-info{color:#7f8c8d;font-size:14px}.card{background:white;padding:25px;border-radius:15px;box-shadow:0 8px 25px rgba(0,0,0,0.1);margin-bottom:25px;border-left:5px solid #2ecc71}.card h2{color:#2c3e50;margin-bottom:20px;padding-bottom:10px;border-bottom:2px solid #ecf0f1;font-size:20px}.info-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:15px;margin-bottom:15px}.info-grid div{padding:12px;background:#f8f9fa;border-radius:8px;border-left:3px solid #3498db}@media (max-width:768px){body{padding:10px}.card{padding:15px}.info-grid{grid-template-columns:1fr}}";
    }
}