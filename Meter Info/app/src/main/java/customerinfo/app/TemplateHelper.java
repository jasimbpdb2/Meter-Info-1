package customerinfo.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class TemplateHelper {

    public static final String TEMPLATE_POSTPAID = "postpaid_template.html";
    public static final String TEMPLATE_PREPAID = "prepaid_template.html";

    // Data classes as static nested classes
    public static class PostpaidData {
        public String customerNumber;
        public MultipleCustomersData multipleCustomers;
        public SingleCustomerData singleCustomer;
        public BillInfoData billInfo;
        public BalanceInfoData balanceInfo;
        public BillSummaryData billSummary;
        public ErrorData error;

        public PostpaidData(String customerNumber, MultipleCustomersData multipleCustomers, 
                          SingleCustomerData singleCustomer, BillInfoData billInfo, 
                          BalanceInfoData balanceInfo, BillSummaryData billSummary, ErrorData error) {
            this.customerNumber = customerNumber;
            this.multipleCustomers = multipleCustomers;
            this.singleCustomer = singleCustomer;
            this.billInfo = billInfo;
            this.balanceInfo = balanceInfo;
            this.billSummary = billSummary;
            this.error = error;
        }
    }

    public static class MultipleCustomersData {
        public int customerCount;
        public List<CustomerData> customers;

        public MultipleCustomersData(int customerCount, List<CustomerData> customers) {
            this.customerCount = customerCount;
            this.customers = customers;
        }
    }

    public static class CustomerData {
        public int index;
        public String customerNumber;
        public List<KeyValuePair> customerInfo;

        public CustomerData(int index, String customerNumber, List<KeyValuePair> customerInfo) {
            this.index = index;
            this.customerNumber = customerNumber;
            this.customerInfo = customerInfo;
        }
    }

    public static class SingleCustomerData {
        public List<KeyValuePair> customerInfo;

        public SingleCustomerData(List<KeyValuePair> customerInfo) {
            this.customerInfo = customerInfo;
        }
    }

    public static class BillInfoData {
        public List<BillData> bills;

        public BillInfoData(List<BillData> bills) {
            this.bills = bills;
        }
    }

    public static class BillData {
        public String billMonth;
        public String billNo;
        public String consumption;
        public String currentBill;
        public String dueDate;
        public String paidAmt;
        public String receiptDate;
        public String balance;

        public BillData(String billMonth, String billNo, String consumption, String currentBill, 
                       String dueDate, String paidAmt, String receiptDate, String balance) {
            this.billMonth = billMonth;
            this.billNo = billNo;
            this.consumption = consumption;
            this.currentBill = currentBill;
            this.dueDate = dueDate;
            this.paidAmt = paidAmt;
            this.receiptDate = receiptDate;
            this.balance = balance;
        }
    }

    public static class BalanceInfoData {
        public List<KeyValuePair> fields;

        public BalanceInfoData(List<KeyValuePair> fields) {
            this.fields = fields;
        }
    }

    public static class BillSummaryData {
        public List<KeyValuePair> fields;

        public BillSummaryData(List<KeyValuePair> fields) {
            this.fields = fields;
        }
    }

    public static class PrepaidData {
        public String meterNumber;
        public String consumerNumber;
        public PrepaidCustomerInfoData prepaidCustomerInfo;
        public TokensData tokens;
        public CustomerInfoData postpaidCustomerInfo;
        public BillInfoData billInfo;
        public BalanceInfoData balanceInfo;
        public BillSummaryData billSummary;
        public ErrorData error;

        public PrepaidData(String meterNumber, String consumerNumber, PrepaidCustomerInfoData prepaidCustomerInfo,
                          TokensData tokens, CustomerInfoData postpaidCustomerInfo, BillInfoData billInfo,
                          BalanceInfoData balanceInfo, BillSummaryData billSummary, ErrorData error) {
            this.meterNumber = meterNumber;
            this.consumerNumber = consumerNumber;
            this.prepaidCustomerInfo = prepaidCustomerInfo;
            this.tokens = tokens;
            this.postpaidCustomerInfo = postpaidCustomerInfo;
            this.billInfo = billInfo;
            this.balanceInfo = balanceInfo;
            this.billSummary = billSummary;
            this.error = error;
        }
    }

    public static class PrepaidCustomerInfoData {
        public List<KeyValuePair> fields;

        public PrepaidCustomerInfoData(List<KeyValuePair> fields) {
            this.fields = fields;
        }
    }

    public static class TokensData {
        public List<TokenData> tokenList;

        public TokensData(List<TokenData> tokenList) {
            this.tokenList = tokenList;
        }
    }

    public static class TokenData {
        public int index;
        public String token;
        public String date;
        public String amount;
        public String operator;
        public String sequence;

        public TokenData(int index, String token, String date, String amount, String operator, String sequence) {
            this.index = index;
            this.token = token;
            this.date = date;
            this.amount = amount;
            this.operator = operator;
            this.sequence = sequence;
        }
    }

    public static class CustomerInfoData {
        public List<KeyValuePair> fields;

        public CustomerInfoData(List<KeyValuePair> fields) {
            this.fields = fields;
        }
    }

    public static class KeyValuePair {
        public String key;
        public String value;

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class ErrorData {
        public String errorMessage;

        public ErrorData(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    // Convert MainActivity result to PostpaidData
    public static PostpaidData convertToPostpaidData(Map<String, Object> result) {
        try {
            String customerNumber = getSafeString(result.get("customer_number"));
            
            // Check if it's multiple customers (meter lookup)
            if (result.containsKey("customer_results")) {
                List<String> customerNumbers = (List<String>) result.get("customer_numbers");
                List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");
                
                List<CustomerData> customers = new ArrayList<>();
                for (int i = 0; i < customerResults.size(); i++) {
                    Map<String, Object> customerResult = customerResults.get(i);
                    Map<String, Object> mergedData = new MainActivity().mergeSERVERData((Map<String, Object>) customerResult);
                    if (mergedData != null) {
                        List<KeyValuePair> customerInfo = extractCustomerInfo(mergedData);
                        String custNumber = i < customerNumbers.size() ? customerNumbers.get(i) : "N/A";
                        customers.add(new CustomerData(i + 1, custNumber, customerInfo));
                    }
                }
                
                return new PostpaidData(
                    customerNumber,
                    new MultipleCustomersData(customers.size(), customers),
                    null,
                    extractBillInfo(result),
                    extractBalanceInfo(result),
                    extractBillSummary(result),
                    extractError(result)
                );
            } else {
                // Single customer
                Map<String, Object> mergedData = new MainActivity().mergeSERVERData(result);
                return new PostpaidData(
                    customerNumber,
                    null,
                    new SingleCustomerData(extractCustomerInfo(mergedData)),
                    extractBillInfo(result),
                    extractBalanceInfo(result),
                    extractBillSummary(result),
                    extractError(result)
                );
            }
        } catch (Exception e) {
            return new PostpaidData(
                "N/A",
                null, null, null, null, null,
                new ErrorData("Error converting data: " + e.getMessage())
            );
        }
    }

    // Convert MainActivity result to PrepaidData
    public static PrepaidData convertToPrepaidData(Map<String, Object> result) {
        try {
            String meterNumber = getSafeString(result.get("meter_number"));
            String consumerNumber = getSafeString(result.get("consumer_number"));
            
            // Extract SERVER1 data for prepaid info
            Object SERVER1Data = result.get("SERVER1_data");
            Map<String, Object> cleanedSERVER1Data;
            if (SERVER1Data != null) {
                cleanedSERVER1Data = new MainActivity().cleanSERVER1Data(SERVER1Data);
            } else {
                cleanedSERVER1Data = new HashMap<>();
            }
            
            // Extract merged data for customer info
            Map<String, Object> mergedData = new MainActivity().mergeSERVERData(result);
            
            return new PrepaidData(
                meterNumber,
                consumerNumber,
                extractPrepaidCustomerInfo(cleanedSERVER1Data),
                extractTokens(cleanedSERVER1Data),
                extractCustomerInfoData(mergedData),
                extractBillInfo(result),
                extractBalanceInfo(result),
                extractBillSummary(result),
                extractError(result)
            );
        } catch (Exception e) {
            return new PrepaidData(
                "N/A", "N/A", null, null, null, null, null, null,
                new ErrorData("Error converting data: " + e.getMessage())
            );
        }
    }

    // Extract customer info from merged data
    private static List<KeyValuePair> extractCustomerInfo(Map<String, Object> mergedData) {
        List<KeyValuePair> fields = new ArrayList<>();
        if (mergedData == null) return fields;
        
        try {
            Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
            if (customerInfo != null) {
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fields;
    }

    // Extract prepaid customer info from SERVER1 data
    private static PrepaidCustomerInfoData extractPrepaidCustomerInfo(Map<String, Object> cleanedSERVER1Data) {
        List<KeyValuePair> fields = new ArrayList<>();
        
        try {
            Map<String, String> customerInfo = (Map<String, String>) cleanedSERVER1Data.get("customer_info");
            if (customerInfo != null) {
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new PrepaidCustomerInfoData(fields);
    }

    // Extract tokens from SERVER1 data
    private static TokensData extractTokens(Map<String, Object> cleanedSERVER1Data) {
        List<TokenData> tokenList = new ArrayList<>();
        
        try {
            List<Map<String, String>> transactions = (List<Map<String, String>>) cleanedSERVER1Data.get("recent_transactions");
            if (transactions != null) {
                int count = Math.min(transactions.size(), 3);
                for (int i = 0; i < count; i++) {
                    Map<String, String> transaction = transactions.get(i);
                    tokenList.add(new TokenData(
                        i + 1,
                        getSafeString(transaction.get("Tokens")),
                        getSafeString(transaction.get("Date")),
                        getSafeString(transaction.get("Amount")),
                        getSafeString(transaction.get("Operator")),
                        getSafeString(transaction.get("Sequence"))
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return tokenList.isEmpty() ? null : new TokensData(tokenList);
    }

    // Extract customer info as CustomerInfoData
    private static CustomerInfoData extractCustomerInfoData(Map<String, Object> mergedData) {
        List<KeyValuePair> fields = new ArrayList<>();
        if (mergedData == null) return new CustomerInfoData(fields);
        
        try {
            Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
            if (customerInfo != null) {
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new CustomerInfoData(fields);
    }

    // Extract bill info
    private static BillInfoData extractBillInfo(Map<String, Object> result) {
        List<BillData> bills = new ArrayList<>();
        
        try {
            Map<String, Object> mergedData = new MainActivity().mergeSERVERData(result);
            if (mergedData != null && mergedData.containsKey("bill_info_raw")) {
                JSONArray billInfoArray = (JSONArray) mergedData.get("bill_info_raw");
                if (billInfoArray != null) {
                    for (int i = 0; i < billInfoArray.length(); i++) {
                        JSONObject bill = billInfoArray.getJSONObject(i);
                        bills.add(new BillData(
                            formatBillMonth(bill.optString("BILL_MONTH")),
                            bill.optString("BILL_NO"),
                            bill.optString("CONS_KWH_SR"),
                            bill.optString("TOTAL_BILL"),
                            formatDate(bill.optString("INVOICE_DUE_DATE")),
                            bill.optString("PAID_AMT"),
                            formatDate(bill.optString("RECEIPT_DATE")),
                            bill.optString("BALANCE")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return bills.isEmpty() ? null : new BillInfoData(bills);
    }

    // Extract balance info
    private static BalanceInfoData extractBalanceInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        
        try {
            Map<String, Object> mergedData = new MainActivity().mergeSERVERData(result);
            if (mergedData != null && mergedData.containsKey("balance_info")) {
                Map<String, String> balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                if (balanceInfo != null) {
                    for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
                        if (isValidValue(entry.getValue())) {
                            fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return fields.isEmpty() ? null : new BalanceInfoData(fields);
    }

    // Extract bill summary
    private static BillSummaryData extractBillSummary(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        
        try {
            Map<String, Object> mergedData = new MainActivity().mergeSERVERData(result);
            if (mergedData != null && mergedData.containsKey("bill_summary")) {
                Map<String, Object> billSummary = (Map<String, Object>) mergedData.get("bill_summary");
                
                // Add relevant summary fields
                if (billSummary.containsKey("total_bills")) {
                    fields.add(new KeyValuePair("Total Bills", billSummary.get("total_bills").toString()));
                }
                if (billSummary.containsKey("latest_bill_date")) {
                    fields.add(new KeyValuePair("Latest Bill Date", billSummary.get("latest_bill_date").toString()));
                }
                if (billSummary.containsKey("latest_total_amount")) {
                    fields.add(new KeyValuePair("Latest Amount", "৳" + billSummary.get("latest_total_amount")));
                }
                if (billSummary.containsKey("recent_consumption")) {
                    fields.add(new KeyValuePair("Recent Consumption", billSummary.get("recent_consumption") + " kWh"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return fields.isEmpty() ? null : new BillSummaryData(fields);
    }

    // Extract error
    private static ErrorData extractError(Map<String, Object> result) {
        if (result.containsKey("error")) {
            return new ErrorData(result.get("error").toString());
        }
        return null;
    }

    // Helper methods
    private static boolean isValidValue(String value) {
        if (value == null) return false;
        String trimmedValue = value.trim();
        return !trimmedValue.isEmpty() &&
                !trimmedValue.equals("N/A") &&
                !trimmedValue.equals("null") &&
                !trimmedValue.equals("{}") &&
                !trimmedValue.equals("undefined");
    }

    private static String getSafeString(Object value) {
        if (value == null) return "N/A";
        String stringValue = value.toString();
        return (stringValue.equals("null") || stringValue.isEmpty()) ? "N/A" : stringValue;
    }

    private static String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "N/A";
        }
        try {
            if (dateString.contains("T")) {
                return dateString.split("T")[0];
            }
            return dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private static String formatBillMonth(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) {
            return "N/A";
        }

        try {
            String[] parts = dateStr.substring(0, 10).split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + "-" + parts[0];
                }
            }
            return dateStr.substring(0, 7); // Fallback to YYYY-MM
        } catch (Exception e) {
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : dateStr;
        }
    }

    // Template rendering methods
    public static String renderPostpaidTemplate(Context context, PostpaidData data) {
        String template = loadTemplate(context, TEMPLATE_POSTPAID);
        return replacePostpaidPlaceholders(template, data);
    }

    public static String renderPrepaidTemplate(Context context, PrepaidData data) {
        String template = loadTemplate(context, TEMPLATE_PREPAID);
        return replacePrepaidPlaceholders(template, data);
    }

    private static String loadTemplate(Context context, String templateName) {
        try {
            java.io.InputStream inputStream = context.getAssets().open(templateName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer, "UTF-8");
        } catch (Exception e) {
            return "<html><body><h1>Error loading template: " + e.getMessage() + "</h1></body></html>";
        }
    }

    private static String replacePostpaidPlaceholders(String template, PostpaidData data) {
        String result = template.replace("{{CUSTOMER_NUMBER}}", data.customerNumber);

        // Handle multiple customers
        if (data.multipleCustomers != null) {
            result = result.replace("{{#MULTIPLE_CUSTOMERS}}", "")
                          .replace("{{/MULTIPLE_CUSTOMERS}}", "");
            
            result = result.replace("{{CUSTOMER_COUNT}}", String.valueOf(data.multipleCustomers.customerCount));
            
            StringBuilder customersHtml = new StringBuilder();
            for (CustomerData customer : data.multipleCustomers.customers) {
                StringBuilder customerHtml = new StringBuilder();
                customerHtml.append("<div class=\"customer-card\">")
                           .append("<h4>👤 Customer ").append(customer.index).append(": ").append(customer.customerNumber).append("</h4>");
                
                for (KeyValuePair info : customer.customerInfo) {
                    customerHtml.append("<div class=\"field\">")
                               .append("<span><strong>").append(info.key).append(":</strong></span>")
                               .append("<span>").append(info.value).append("</span>")
                               .append("</div>");
                }
                
                customerHtml.append("</div>");
                customersHtml.append(customerHtml.toString());
            }
            
            result = result.replace("{{#CUSTOMERS}}{{/CUSTOMERS}}", customersHtml.toString());
        } else {
            result = result.replace("{{#MULTIPLE_CUSTOMERS}}", "<!--")
                          .replace("{{/MULTIPLE_CUSTOMERS}}", "-->");
        }

        // Handle single customer
        if (data.singleCustomer != null) {
            result = result.replace("{{#SINGLE_CUSTOMER}}", "")
                          .replace("{{/SINGLE_CUSTOMER}}", "");
            
            StringBuilder customerInfoHtml = new StringBuilder();
            for (KeyValuePair info : data.singleCustomer.customerInfo) {
                customerInfoHtml.append("<div class=\"field\">")
                               .append("<span><strong>").append(info.key).append(":</strong></span>")
                               .append("<span>").append(info.value).append("</span>")
                               .append("</div>");
            }
            
            result = result.replace("{{#CUSTOMER_INFO}}{{/CUSTOMER_INFO}}", customerInfoHtml.toString());
        } else {
            result = result.replace("{{#SINGLE_CUSTOMER}}", "<!--")
                          .replace("{{/SINGLE_CUSTOMER}}", "-->");
        }

        // Handle bill info
        if (data.billInfo != null) {
            result = result.replace("{{#BILL_INFO}}", "")
                          .replace("{{/BILL_INFO}}", "");
            
            StringBuilder billsHtml = new StringBuilder();
            for (BillData bill : data.billInfo.bills) {
                billsHtml.append("<tr>")
                        .append("<td>").append(bill.billMonth).append("</td>")
                        .append("<td>").append(bill.billNo).append("</td>")
                        .append("<td>").append(bill.consumption).append("</td>")
                        .append("<td>").append(bill.currentBill).append("</td>")
                        .append("<td>").append(bill.dueDate).append("</td>")
                        .append("<td>").append(bill.paidAmt).append("</td>")
                        .append("<td>").append(bill.receiptDate).append("</td>")
                        .append("<td>").append(bill.balance).append("</td>")
                        .append("</tr>");
            }
            
            result = result.replace("{{#BILLS}}{{/BILLS}}", billsHtml.toString());
        } else {
            result = result.replace("{{#BILL_INFO}}", "<!--")
                          .replace("{{/BILL_INFO}}", "-->");
        }

        // Handle balance info
        if (data.balanceInfo != null) {
            result = result.replace("{{#BALANCE_INFO}}", "")
                          .replace("{{/BALANCE_INFO}}", "");
            
            StringBuilder balanceHtml = new StringBuilder();
            for (KeyValuePair field : data.balanceInfo.fields) {
                balanceHtml.append("<div class=\"field\">")
                          .append("<span><strong>").append(field.key).append(":</strong></span>")
                          .append("<span>").append(field.value).append("</span>")
                          .append("</div>");
            }
            
            result = result.replace("{{#FIELDS}}{{/FIELDS}}", balanceHtml.toString());
        } else {
            result = result.replace("{{#BALANCE_INFO}}", "<!--")
                          .replace("{{/BALANCE_INFO}}", "-->");
        }

        // Handle bill summary
        if (data.billSummary != null) {
            result = result.replace("{{#BILL_SUMMARY}}", "")
                          .replace("{{/BILL_SUMMARY}}", "");
            
            StringBuilder summaryHtml = new StringBuilder();
            for (KeyValuePair field : data.billSummary.fields) {
                summaryHtml.append("<div class=\"field\">")
                          .append("<span><strong>").append(field.key).append(":</strong></span>")
                          .append("<span>").append(field.value).append("</span>")
                          .append("</div>");
            }
            
            result = result.replace("{{#FIELDS}}{{/FIELDS}}", summaryHtml.toString());
        } else {
            result = result.replace("{{#BILL_SUMMARY}}", "<!--")
                          .replace("{{/BILL_SUMMARY}}", "-->");
        }

        // Handle error
        if (data.error != null) {
            result = result.replace("{{#ERROR}}", "")
                          .replace("{{/ERROR}}", "")
                          .replace("{{ERROR_MESSAGE}}", data.error.errorMessage);
        } else {
            result = result.replace("{{#ERROR}}", "<!--")
                          .replace("{{/ERROR}}", "-->");
        }

        return result;
    }

    private static String replacePrepaidPlaceholders(String template, PrepaidData data) {
        String result = template.replace("{{METER_NUMBER}}", data.meterNumber)
                               .replace("{{CONSUMER_NUMBER}}", data.consumerNumber);

        // Handle prepaid customer info
        if (data.prepaidCustomerInfo != null) {
            result = result.replace("{{#PREPAID_CUSTOMER_INFO}}", "")
                          .replace("{{/PREPAID_CUSTOMER_INFO}}", "");
            
            StringBuilder prepaidHtml = new StringBuilder();
            for (KeyValuePair field : data.prepaidCustomerInfo.fields) {
                prepaidHtml.append("<div class=\"field\">")
                          .append("<span><strong>").append(field.key).append(":</strong></span>")
                          .append("<span>").append(field.value).append("</span>")
                          .append("</div>");
            }
            
            result = result.replace("{{#FIELDS}}{{/FIELDS}}", prepaidHtml.toString());
        } else {
            result = result.replace("{{#PREPAID_CUSTOMER_INFO}}", "<!--")
                          .replace("{{/PREPAID_CUSTOMER_INFO}}", "-->");
        }

        // Handle tokens
        if (data.tokens != null) {
            result = result.replace("{{#TOKENS}}", "")
                          .replace("{{/TOKENS}}", "");
            
            StringBuilder tokensHtml = new StringBuilder();
            for (TokenData token : data.tokens.tokenList) {
                tokensHtml.append("<div class=\"token\">")
                         .append("<strong>Order ").append(token.index).append(":</strong><br>")
                         .append("<strong>Token:</strong> ").append(token.token).append("<br>")
                         .append("<strong>Date:</strong> ").append(token.date).append("<br>")
                         .append("<strong>Amount:</strong> ").append(token.amount).append("<br>")
                         .append("<strong>Operator:</strong> ").append(token.operator).append("<br>")
                         .append("<strong>Sequence:</strong> ").append(token.sequence)
                         .append("</div>");
            }
            
            result = result.replace("{{#TOKEN_LIST}}{{/TOKEN_LIST}}", tokensHtml.toString());
        } else {
            result = result.replace("{{#TOKENS}}", "<!--")
                          .replace("{{/TOKENS}}", "-->");
        }

        // Handle postpaid customer info
        if (data.postpaidCustomerInfo != null) {
            result = result.replace("{{#POSTPAID_CUSTOMER_INFO}}", "")
                          .replace("{{/POSTPAID_CUSTOMER_INFO}}", "");
            
            StringBuilder postpaidHtml = new StringBuilder();
            for (KeyValuePair field : data.postpaidCustomerInfo.fields) {
                postpaidHtml.append("<div class=\"field\">")
                           .append("<span><strong>").append(field.key).append(":</strong></span>")
                           .append("<span>").append(field.value).append("</span>")
                           .append("</div>");
            }
            
            result = result.replace("{{#FIELDS}}{{/FIELDS}}", postpaidHtml.toString());
        } else {
            result = result.replace("{{#POSTPAID_CUSTOMER_INFO}}", "<!--")
                          .replace("{{/POSTPAID_CUSTOMER_INFO}}", "-->");
        }

        // Handle bill info, balance info, bill summary, and error (same as postpaid)
        if (data.billInfo != null) {
            result = result.replace("{{#BILL_INFO}}", "")
                          .replace("{{/BILL_INFO}}", "");
            
            StringBuilder billsHtml = new StringBuilder();
            for (BillData bill : data.billInfo.bills) {
                billsHtml.append("<tr>")
                        .append("<td>").append(bill.billMonth).append("</td>")
                        .append("<td>").append(bill.billNo).append("</td>")
                        .append("<td>").append(bill.consumption).append("</td>")
                        .append("<td>").append(bill.currentBill).append("</td>")
                        .append("<td>").append(bill.dueDate).append("</td>")
                        .append("<td>").append(bill.paidAmt).append("</td>")
                        .append("<td>").append(bill.receiptDate).append("</td>")
                        .append("<td>").append(bill.balance).append("</td>")
                        .append("</tr>");
            }
            
            result = result.replace("{{#BILLS}}{{/BILLS}}", billsHtml.toString());
        } else {
            result = result.replace("{{#BILL_INFO}}", "<!--")
                          .replace("{{/BILL_INFO}}", "-->");
        }

        if (data.balanceInfo != null) {
            result = result.replace("{{#BALANCE_INFO}}", "")
                          .replace("{{/BALANCE_INFO}}", "");
            
            StringBuilder balanceHtml = new StringBuilder();
            for (KeyValuePair field : data.balanceInfo.fields) {
                balanceHtml.append("<div class=\"field\">")
                          .append("<span><strong>").append(field.key).append(":</strong></span>")
                          .append("<span>").append(field.value).append("</span>")
                          .append("</div>");
            }
            
            result = result.replace("{{#FIELDS}}{{/FIELDS}}", balanceHtml.toString());
        } else {
            result = result.replace("{{#BALANCE_INFO}}", "<!--")
                          .replace("{{/BALANCE_INFO}}", "-->");
        }

        if (data.billSummary != null) {
            result = result.replace("{{#BILL_SUMMARY}}", "")
                          .replace("{{/BILL_SUMMARY}}", "");
            
            StringBuilder summaryHtml = new StringBuilder();
            for (KeyValuePair field : data.billSummary.fields) {
                summaryHtml.append("<div class=\"field\">")
                          .append("<span><strong>").append(field.key).append(":</strong></span>")
                          .append("<span>").append(field.value).append("</span>")
                          .append("</div>");
            }
            
            result = result.replace("{{#FIELDS}}{{/FIELDS}}", summaryHtml.toString());
        } else {
            result = result.replace("{{#BILL_SUMMARY}}", "<!--")
                          .replace("{{/BILL_SUMMARY}}", "-->");
        }

        if (data.error != null) {
            result = result.replace("{{#ERROR}}", "")
                          .replace("{{/ERROR}}", "")
                          .replace("{{ERROR_MESSAGE}}", data.error.errorMessage);
        } else {
            result = result.replace("{{#ERROR}}", "<!--")
                          .replace("{{/ERROR}}", "-->");
        }

        return result;
    }
}