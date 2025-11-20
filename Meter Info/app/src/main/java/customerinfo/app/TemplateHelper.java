package customerinfo.app;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

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
        
        // Get merged data
        Map<String, Object> mergedData = mergeSERVERData(result);
        if (mergedData != null) {
            // Customer Info sections
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
        Map<String, Object> cleanedSERVER1 = cleanSERVER1Data(result.get("SERVER1_data"));
        if (cleanedSERVER1 != null && cleanedSERVER1.containsKey("recent_transactions")) {
            data.recentTransactions = (List<Map<String, String>>) cleanedSERVER1.get("recent_transactions");
        }
        
        return data;
    }

    // Convert MainActivity result to PostpaidData
    public static PostpaidData convertToPostpaidData(Map<String, Object> result) {
        PostpaidData data = new PostpaidData();
        
        data.customerNumber = getSafeString(result.get("customer_number"));
        data.meterNumber = getSafeString(result.get("meter_number"));
        data.searchTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
        
        // Handle multiple customers from meter lookup
        if (result.containsKey("customer_results")) {
            data.multipleCustomers = new ArrayList<>();
            List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");
            List<String> customerNumbers = (List<String>) result.get("customer_numbers");
            
            for (int i = 0; i < customerResults.size(); i++) {
                CustomerResult customer = new CustomerResult();
                customer.customerNumber = customerNumbers.get(i);
                
                Map<String, Object> mergedData = mergeSERVERData(customerResults.get(i));
                if (mergedData != null) {
                    if (mergedData.containsKey("customer_info")) {
                        customer.customerInfo = (Map<String, String>) mergedData.get("customer_info");
                    }
                    if (mergedData.containsKey("bill_summary")) {
                        Map<String, Object> billSummaryObj = (Map<String, Object>) mergedData.get("bill_summary");
                        customer.billSummary = new HashMap<>();
                        customer.billSummary.put("total_bills", getSafeString(billSummaryObj.get("total_bills")));
                        customer.billSummary.put("total_amount", getSafeString(billSummaryObj.get("total_amount")));
                        customer.billSummary.put("arrears", getSafeString(billSummaryObj.get("arrears")));
                    }
                    if (mergedData.containsKey("balance_info")) {
                        customer.balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                    }
                }
                data.multipleCustomers.add(customer);
            }
        } else {
            // Single customer
            Map<String, Object> mergedData = mergeSERVERData(result);
            if (mergedData != null) {
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

    // HTML Template Rendering
    public static String renderPrepaidTemplate(Context context, PrepaidData data) {
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
        
        // Basic Info Card
        html.append("<div class='card'>");
        html.append("<h2>🔢 Basic Information</h2>");
        html.append("<div class='info-grid'>");
        html.append("<div><strong>Meter Number:</strong> ").append(escapeHtml(data.meterNumber)).append("</div>");
        html.append("<div><strong>Consumer Number:</strong> ").append(escapeHtml(data.consumerNumber)).append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Prepaid Customer Details
        if (data.customerInfo != null && !data.customerInfo.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>📋 PREPAID CUSTOMER DETAILS</h2>");
            
            html.append("<div class='section'>");
            html.append("<h3>👤 CUSTOMER INFORMATION</h3>");
            html.append("<div class='info-list'>");
            for (Map.Entry<String, String> entry : data.customerInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
            html.append("</div>");
        }
        
        // Recent Tokens
        if (data.recentTransactions != null && !data.recentTransactions.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>🔑 LAST RECHARGE TOKENS</h2>");
            html.append("<div class='tokens-container'>");
            
            for (int i = 0; i < data.recentTransactions.size(); i++) {
                Map<String, String> transaction = data.recentTransactions.get(i);
                html.append("<div class='token-card'>");
                html.append("<h3>Order ").append(i + 1).append("</h3>");
                html.append("<div class='token-details'>");
                html.append("<p><span class='emoji'>📅</span> Date: ").append(escapeHtml(transaction.get("Date"))).append("</p>");
                html.append("<p><span class='emoji'>🧾</span> Order: ").append(escapeHtml(transaction.get("Order Number"))).append("</p>");
                html.append("<p><span class='emoji'>👤</span> Operator: ").append(escapeHtml(transaction.get("Operator"))).append("</p>");
                html.append("<p><span class='emoji'>🔢</span> Sequence: ").append(escapeHtml(transaction.get("Sequence"))).append("</p>");
                html.append("<p><span class='emoji'>💰</span> Amount: ").append(escapeHtml(transaction.get("Amount"))).append("</p>");
                html.append("<p><span class='emoji'>⚡</span> Energy: ").append(escapeHtml(transaction.get("Energy Cost"))).append("</p>");
                html.append("<p><span class='emoji'>🔑</span> TOKENS: <strong class='token'>").append(escapeHtml(transaction.get("Tokens"))).append("</strong></p>");
                html.append("</div>");
                html.append("</div>");
            }
            
            html.append("</div>");
            html.append("</div>");
        }
        
        // Merged Customer Information Sections
        html.append(renderCustomerSections(data.personalInfo, data.meterInfo, data.billingInfo, data.technicalInfo, data.readingInfo));
        
        // Bill Summary
        if (data.billSummary != null && !data.billSummary.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>📊 BILL SUMMARY</h2>");
            html.append("<div class='bill-summary'>");
            html.append("<div class='summary-grid'>");
            html.append("<div><strong>First Bill Period:</strong> ").append(escapeHtml(data.billSummary.get("first_bill_period"))).append("</div>");
            html.append("<div><strong>Last Bill Period:</strong> ").append(escapeHtml(data.billSummary.get("last_bill_period"))).append("</div>");
            html.append("<div><strong>Total Bills:</strong> ").append(escapeHtml(data.billSummary.get("total_bills"))).append("</div>");
            html.append("<div><strong>Total Amount:</strong> ৳").append(escapeHtml(data.billSummary.get("total_amount"))).append("</div>");
            html.append("<div><strong>Total Paid:</strong> ৳").append(escapeHtml(data.billSummary.get("total_paid"))).append("</div>");
            html.append("<div><strong>Arrears:</strong> ৳").append(escapeHtml(data.billSummary.get("arrears"))).append("</div>");
            html.append("</div>");
            html.append("</div>");
            html.append("</div>");
        }
        
        // Balance Details
        if (data.balanceInfo != null && !data.balanceInfo.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>💰 FINAL BALANCE DETAILS</h2>");
            html.append("<div class='balance-details'>");
            String[] balanceOrder = {"Total Balance", "Arrear Amount", "PRN", "LPS", "VAT"};
            for (String key : balanceOrder) {
                if (data.balanceInfo.containsKey(key) && isValidValue(data.balanceInfo.get(key))) {
                    html.append("<div class='balance-item'>• ").append(escapeHtml(key)).append(": ").append(escapeHtml(data.balanceInfo.get(key))).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
        }
        
        // Bill Table
        if (data.billTableData != null && data.billTableData.length() > 0) {
            html.append(renderBillTable(data.billTableData));
        }
        
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    public static String renderPostpaidTemplate(Context context, PostpaidData data) {
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
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>💡 POSTPAID METER INFORMATION</h1>");
        html.append("<div class='search-info'>");
        html.append("<p><strong>Search Time:</strong> ").append(data.searchTime).append("</p>");
        html.append("</div>");
        html.append("</div>");
        
        // Multiple Customers
        if (data.multipleCustomers != null && !data.multipleCustomers.isEmpty()) {
            html.append("<div class='card'>");
            html.append("<h2>📊 Found ").append(data.multipleCustomers.size()).append(" Customer(s) for This Meter</h2>");
            
            for (int i = 0; i < data.multipleCustomers.size(); i++) {
                CustomerResult customer = data.multipleCustomers.get(i);
                html.append("<div class='customer-section'>");
                html.append("<h3 class='customer-header'>👤 CUSTOMER ").append(i + 1).append("/").append(data.multipleCustomers.size()).append(": ").append(escapeHtml(customer.customerNumber)).append("</h3>");
                
                if (customer.customerInfo != null) {
                    html.append("<div class='info-list'>");
                    for (Map.Entry<String, String> entry : customer.customerInfo.entrySet()) {
                        if (isValidValue(entry.getValue())) {
                            html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                        }
                    }
                    html.append("</div>");
                }
                
                if (customer.billSummary != null) {
                    html.append("<div class='bill-summary'>");
                    html.append("<h4>Bill Summary</h4>");
                    html.append("<div class='summary-grid'>");
                    html.append("<div><strong>Total Bills:</strong> ").append(escapeHtml(customer.billSummary.get("total_bills"))).append("</div>");
                    html.append("<div><strong>Total Amount:</strong> ৳").append(escapeHtml(customer.billSummary.get("total_amount"))).append("</div>");
                    html.append("<div><strong>Arrears:</strong> ৳").append(escapeHtml(customer.billSummary.get("arrears"))).append("</div>");
                    html.append("</div>");
                    html.append("</div>");
                }
                
                html.append("</div>");
            }
            html.append("</div>");
        } else {
            // Single Customer
            html.append("<div class='card'>");
            html.append("<h2>🔢 Basic Information</h2>");
            html.append("<div class='info-grid'>");
            html.append("<div><strong>Consumer Number:</strong> ").append(escapeHtml(data.customerNumber)).append("</div>");
            if (data.meterNumber != null && !data.meterNumber.equals("N/A")) {
                html.append("<div><strong>Meter Number:</strong> ").append(escapeHtml(data.meterNumber)).append("</div>");
            }
            html.append("</div>");
            html.append("</div>");
            
            // Customer Information Sections
            html.append(renderCustomerSections(data.personalInfo, data.meterInfo, data.billingInfo, data.technicalInfo, data.readingInfo));
            
            // Bill Summary
            if (data.billSummary != null && !data.billSummary.isEmpty()) {
                html.append("<div class='card'>");
                html.append("<h2>📊 BILL SUMMARY</h2>");
                html.append("<div class='bill-summary'>");
                html.append("<div class='summary-grid'>");
                html.append("<div><strong>First Bill Period:</strong> ").append(escapeHtml(data.billSummary.get("first_bill_period"))).append("</div>");
                html.append("<div><strong>Last Bill Period:</strong> ").append(escapeHtml(data.billSummary.get("last_bill_period"))).append("</div>");
                html.append("<div><strong>Total Bills:</strong> ").append(escapeHtml(data.billSummary.get("total_bills"))).append("</div>");
                html.append("<div><strong>Total Amount:</strong> ৳").append(escapeHtml(data.billSummary.get("total_amount"))).append("</div>");
                html.append("<div><strong>Total Paid:</strong> ৳").append(escapeHtml(data.billSummary.get("total_paid"))).append("</div>");
                html.append("<div><strong>Arrears:</strong> ৳").append(escapeHtml(data.billSummary.get("arrears"))).append("</div>");
                html.append("</div>");
                html.append("</div>");
                html.append("</div>");
            }
            
            // Balance Details
            if (data.balanceInfo != null && !data.balanceInfo.isEmpty()) {
                html.append("<div class='card'>");
                html.append("<h2>💰 FINAL BALANCE DETAILS</h2>");
                html.append("<div class='balance-details'>");
                String[] balanceOrder = {"Total Balance", "Arrear Amount", "PRN", "LPS", "VAT"};
                for (String key : balanceOrder) {
                    if (data.balanceInfo.containsKey(key) && isValidValue(data.balanceInfo.get(key))) {
                        html.append("<div class='balance-item'>• ").append(escapeHtml(key)).append(": ").append(escapeHtml(data.balanceInfo.get(key))).append("</div>");
                    }
                }
                html.append("</div>");
                html.append("</div>");
            }
            
            // Bill Table
            if (data.billTableData != null && data.billTableData.length() > 0) {
                html.append(renderBillTable(data.billTableData));
            }
        }
        
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    private static String renderCustomerSections(Map<String, String> personalInfo, Map<String, String> meterInfo, 
                                               Map<String, String> billingInfo, Map<String, String> technicalInfo, 
                                               Map<String, String> readingInfo) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class='card'>");
        html.append("<h2>👤 CUSTOMER INFORMATION</h2>");
        
        if (personalInfo != null && !personalInfo.isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>📋 PERSONAL INFORMATION</h3>");
            html.append("<div class='info-list'>");
            for (Map.Entry<String, String> entry : personalInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
        }
        
        if (meterInfo != null && !meterInfo.isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>🔧 METER INFORMATION</h3>");
            html.append("<div class='info-list'>");
            for (Map.Entry<String, String> entry : meterInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
        }
        
        if (billingInfo != null && !billingInfo.isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>💳 BILLING INFORMATION</h3>");
            html.append("<div class='info-list'>");
            for (Map.Entry<String, String> entry : billingInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
        }
        
        if (technicalInfo != null && !technicalInfo.isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>⚙️ TECHNICAL INFORMATION</h3>");
            html.append("<div class='info-list'>");
            for (Map.Entry<String, String> entry : technicalInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
        }
        
        if (readingInfo != null && !readingInfo.isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>📊 METER READINGS</h3>");
            html.append("<div class='info-list'>");
            for (Map.Entry<String, String> entry : readingInfo.entrySet()) {
                if (isValidValue(entry.getValue())) {
                    html.append("<div class='info-item'>• ").append(escapeHtml(entry.getKey())).append(": ").append(escapeHtml(entry.getValue())).append("</div>");
                }
            }
            html.append("</div>");
            html.append("</div>");
        }
        
        html.append("</div>");
        return html.toString();
    }

    private static String renderBillTable(JSONArray billData) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class='card'>");
        html.append("<h2>📋 BILL HISTORY TABLE</h2>");
        html.append("<div class='table-container'>");
        html.append("<table class='bill-table'>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Bill Month</th>");
        html.append("<th>Bill No</th>");
        html.append("<th>Consumption</th>");
        html.append("<th>Current Bill</th>");
        html.append("<th>Due Date</th>");
        html.append("<th>Paid</th>");
        html.append("<th>Pay Date</th>");
        html.append("<th>Balance</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        try {
            int displayCount = Math.min(billData.length(), 10); // Show max 10 bills
            for (int i = 0; i < displayCount; i++) {
                JSONObject bill = billData.getJSONObject(i);
                html.append("<tr>");
                html.append("<td>").append(escapeHtml(formatBillMonth(bill.optString("BILL_MONTH")))).append("</td>");
                html.append("<td>").append(escapeHtml(bill.optString("BILL_NO"))).append("</td>");
                html.append("<td>").append(escapeHtml(formatConsumption(bill.optDouble("CONS_KWH_SR", 0)))).append("</td>");
                html.append("<td>").append(escapeHtml(formatAmount(bill.optDouble("CURRENT_BILL", 0)))).append("</td>");
                html.append("<td>").append(escapeHtml(formatDate(bill.optString("INVOICE_DUE_DATE")))).append("</td>");
                html.append("<td>").append(escapeHtml(formatAmount(bill.optDouble("PAID_AMT", 0)))).append("</td>");
                html.append("<td>").append(escapeHtml(formatDate(bill.optString("RECEIPT_DATE")))).append("</td>");
                html.append("<td>").append(escapeHtml(formatAmount(bill.optDouble("BALANCE", 0)))).append("</td>");
                html.append("</tr>");
            }
        } catch (Exception e) {
            html.append("<tr><td colspan='8'>Error loading bill data</td></tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        html.append("</div>");
        
        return html.toString();
    }

    private static String getCommonCSS() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                color: #333;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                padding: 20px;
            }
            .header {
                background: white;
                padding: 25px;
                border-radius: 15px;
                box-shadow: 0 8px 25px rgba(0,0,0,0.1);
                margin-bottom: 25px;
                text-align: center;
                border-left: 5px solid #3498db;
            }
            .header h1 {
                color: #2c3e50;
                margin-bottom: 10px;
                font-size: 24px;
            }
            .search-info {
                color: #7f8c8d;
                font-size: 14px;
            }
            .card {
                background: white;
                padding: 25px;
                border-radius: 15px;
                box-shadow: 0 8px 25px rgba(0,0,0,0.1);
                margin-bottom: 25px;
                border-left: 5px solid #2ecc71;
            }
            .card h2 {
                color: #2c3e50;
                margin-bottom: 20px;
                padding-bottom: 10px;
                border-bottom: 2px solid #ecf0f1;
                font-size: 20px;
            }
            .card h3 {
                color: #34495e;
                margin: 20px 0 15px 0;
                font-size: 16px;
            }
            .info-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 15px;
                margin-bottom: 15px;
            }
            .info-grid div {
                padding: 12px;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 3px solid #3498db;
            }
            .info-list {
                display: flex;
                flex-direction: column;
                gap: 8px;
            }
            .info-item {
                padding: 10px 15px;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 3px solid #27ae60;
                margin-bottom: 5px;
            }
            .section {
                margin-bottom: 25px;
            }
            .section:last-child {
                margin-bottom: 0;
            }
            .tokens-container {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                gap: 20px;
                margin-top: 20px;
            }
            .token-card {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 20px;
                border-radius: 12px;
                box-shadow: 0 4px 15px rgba(0,0,0,0.2);
            }
            .token-card h3 {
                color: white;
                margin-bottom: 15px;
                text-align: center;
                border-bottom: 1px solid rgba(255,255,255,0.3);
                padding-bottom: 10px;
            }
            .token-details p {
                margin-bottom: 8px;
                display: flex;
                align-items: center;
            }
            .emoji {
                margin-right: 8px;
                font-size: 16px;
            }
            .token {
                background: rgba(255,255,255,0.2);
                padding: 5px 10px;
                border-radius: 6px;
                font-family: monospace;
                font-weight: bold;
            }
            .bill-summary {
                background: #f8f9fa;
                padding: 20px;
                border-radius: 10px;
                border: 1px solid #e9ecef;
            }
            .summary-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 15px;
            }
            .summary-grid div {
                padding: 12px;
                background: white;
                border-radius: 8px;
                text-align: center;
                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            }
            .balance-details {
                background: #f8f9fa;
                padding: 20px;
                border-radius: 10px;
                border: 1px solid #e9ecef;
            }
            .balance-item {
                padding: 10px 15px;
                background: white;
                border-radius: 8px;
                margin-bottom: 8px;
                border-left: 3px solid #e74c3c;
            }
            .customer-section {
                background: #f8f9fa;
                padding: 20px;
                border-radius: 10px;
                margin-bottom: 20px;
                border: 1px solid #e9ecef;
            }
            .customer-header {
                color: #2c3e50;
                margin-bottom: 15px;
                padding-bottom: 10px;
                border-bottom: 2px solid #bdc3c7;
            }
            .table-container {
                overflow-x: auto;
                margin-top: 15px;
            }
            .bill-table {
                width: 100%;
                border-collapse: collapse;
                background: white;
                border-radius: 8px;
                overflow: hidden;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .bill-table th {
                background: #34495e;
                color: white;
                padding: 12px 15px;
                text-align: left;
                font-weight: 600;
            }
            .bill-table td {
                padding: 12px 15px;
                border-bottom: 1px solid #ecf0f1;
            }
            .bill-table tr:nth-child(even) {
                background: #f8f9fa;
            }
            .bill-table tr:hover {
                background: #e3f2fd;
            }
            @media (max-width: 768px) {
                body {
                    padding: 10px;
                }
                .card {
                    padding: 15px;
                }
                .info-grid {
                    grid-template-columns: 1fr;
                }
                .summary-grid {
                    grid-template-columns: 1fr;
                }
                .tokens-container {
                    grid-template-columns: 1fr;
                }
                .bill-table {
                    font-size: 14px;
                }
                .bill-table th,
                .bill-table td {
                    padding: 8px 10px;
                }
            }
            """;
    }

    // Utility methods (you'll need to implement these or copy from MainActivity)
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

    // These methods should be copied from your MainActivity:
    private static Map<String, Object> mergeSERVERData(Map<String, Object> result) {
        // Copy this method from your MainActivity
        return MainActivity.mergeSERVERData(result);
    }

    private static Map<String, Object> cleanSERVER1Data(Object SERVER1DataObj) {
        // Copy this method from your MainActivity  
        return MainActivity.cleanSERVER1Data(SERVER1DataObj);
    }

    private static String formatBillMonth(String dateStr) {
        // Copy from MainActivity
        try {
            if (dateStr == null || dateStr.equals("null")) return "—";
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

    private static String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) return "—";
        try {
            return dateString.contains("T") ? dateString.split("T")[0] : dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private static String formatConsumption(double consumption) {
        return consumption == 0 ? "—" : String.format("%.0f", consumption);
    }

    private static String formatAmount(double amount) {
        return amount == 0 ? "—" : "৳" + String.format("%.0f", amount);
    }
}