package customerinfo.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class TemplateHelper {

    public static final String TEMPLATE_POSTPAID = "postpaid_template.html";
    public static final String TEMPLATE_PREPAID = "prepaid_template.html";

    // Data classes
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

    // UPDATED: Convert MainActivity result to PostpaidData - FIXED
    public static PostpaidData convertToPostpaidData(Map<String, Object> result) {
        try {
            String customerNumber = extractCustomerNumber(result);
            
            // Check if it's multiple customers (meter lookup)
            if (result.containsKey("customer_results")) {
                List<String> customerNumbers = (List<String>) result.get("customer_numbers");
                List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");

                List<CustomerData> customers = new ArrayList<>();
                for (int i = 0; i < customerResults.size(); i++) {
                    Map<String, Object> customerResult = customerResults.get(i);
                    List<KeyValuePair> customerInfo = extractCustomerInfoFromMergedData(customerResult);
                    String custNumber = i < customerNumbers.size() ? customerNumbers.get(i) : "N/A";
                    customers.add(new CustomerData(i + 1, custNumber, customerInfo));
                }

                return new PostpaidData(
                    customerNumber,
                    new MultipleCustomersData(customers.size(), customers),
                    null,
                    extractBillInfoFromMergedData(result),
                    extractBalanceInfoFromMergedData(result),
                    extractBillSummaryFromMergedData(result),
                    extractError(result)
                );
            } else {
                // Single customer - use the merged data structure
                List<KeyValuePair> customerInfo = extractCustomerInfoFromMergedData(result);
                return new PostpaidData(
                    customerNumber,
                    null,
                    new SingleCustomerData(customerInfo),
                    extractBillInfoFromMergedData(result),
                    extractBalanceInfoFromMergedData(result),
                    extractBillSummaryFromMergedData(result),
                    extractError(result)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new PostpaidData(
                "N/A",
                null, null, null, null, null,
                new ErrorData("Error converting data: " + e.getMessage())
            );
        }
    }

    // UPDATED: Convert MainActivity result to PrepaidData - FIXED
    public static PrepaidData convertToPrepaidData(Map<String, Object> result) {
        try {
            String meterNumber = extractMeterNumber(result);
            String consumerNumber = extractConsumerNumber(result);

            return new PrepaidData(
                meterNumber,
                consumerNumber,
                extractPrepaidCustomerInfo(result),
                extractTokensFromMergedData(result),
                extractCustomerInfoFromMergedDataAsCustomerInfoData(result),
                extractBillInfoFromMergedData(result),
                extractBalanceInfoFromMergedData(result),
                extractBillSummaryFromMergedData(result),
                extractError(result)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new PrepaidData(
                "N/A", "N/A", null, null, null, null, null, null,
                new ErrorData("Error converting data: " + e.getMessage())
            );
        }
    }

    // NEW: Extract customer info from MainActivity's merged data structure
    private static List<KeyValuePair> extractCustomerInfoFromMergedData(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        if (result == null) return fields;

        try {
            // First try to get data from merged structure (MainActivity's mergeSERVERData)
            Map<String, Object> mergedData = getMergedData(result);
            
            if (mergedData != null && mergedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                    }
                }
            }

            // If no merged data, try direct extraction
            if (fields.isEmpty()) {
                fields = extractBasicCustomerInfoDirect(result);
            }

            // Always add basic identifiers
            addIfValid(fields, "Customer Number", extractCustomerNumber(result));
            addIfValid(fields, "Consumer Number", extractConsumerNumber(result));
            addIfValid(fields, "Meter Number", extractMeterNumber(result));

        } catch (Exception e) {
            e.printStackTrace();
            fields.add(new KeyValuePair("Error", "Failed to extract customer info: " + e.getMessage()));
        }
        return fields;
    }

    // NEW: Extract bill info from MainActivity's merged data structure
    private static BillInfoData extractBillInfoFromMergedData(Map<String, Object> result) {
        List<BillData> bills = new ArrayList<>();

        try {
            Map<String, Object> mergedData = getMergedData(result);
            if (mergedData != null && mergedData.containsKey("bill_info_raw")) {
                JSONArray billInfo = (JSONArray) mergedData.get("bill_info_raw");
                for (int i = 0; i < billInfo.length() && i < 10; i++) { // Limit to 10 bills
                    JSONObject bill = billInfo.getJSONObject(i);
                    bills.add(new BillData(
                        formatBillMonth(bill.optString("BILL_MONTH")),
                        bill.optString("BILL_NO"),
                        bill.optString("CONS_KWH_SR"),
                        formatCurrency(bill.optString("TOTAL_BILL")),
                        formatDate(bill.optString("INVOICE_DUE_DATE")),
                        formatCurrency(bill.optString("PAID_AMT")),
                        formatDate(bill.optString("RECEIPT_DATE")),
                        formatCurrency(bill.optString("BALANCE"))
                    ));
                }
            }

            // If no bills found, check for bill summary
            if (bills.isEmpty() && mergedData != null && mergedData.containsKey("bill_summary")) {
                Map<String, Object> billSummary = (Map<String, Object>) mergedData.get("bill_summary");
                if (billSummary.containsKey("all_bills")) {
                    List<Map<String, Object>> allBills = (List<Map<String, Object>>) billSummary.get("all_bills");
                    for (Map<String, Object> bill : allBills) {
                        bills.add(new BillData(
                            getSafeString(bill.get("bill_month")),
                            getSafeString(bill.get("bill_number")),
                            getSafeString(bill.get("consumption")),
                            formatCurrency(getSafeString(bill.get("total_amount"))),
                            getSafeString(bill.get("due_date")),
                            "৳0", // paid amount
                            "N/A", // receipt date
                            formatCurrency(getSafeString(bill.get("balance")))
                        ));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bills.isEmpty() ? null : new BillInfoData(bills);
    }

    // NEW: Extract balance info from MainActivity's merged data structure
    private static BalanceInfoData extractBalanceInfoFromMergedData(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Map<String, Object> mergedData = getMergedData(result);
            if (mergedData != null && mergedData.containsKey("balance_info")) {
                Map<String, String> balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), formatCurrency(entry.getValue())));
                    }
                }
            }

            // If no balance info, add default
            if (fields.isEmpty()) {
                fields.add(new KeyValuePair("Total Balance", "৳0"));
                fields.add(new KeyValuePair("Arrear Amount", "৳0"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fields.isEmpty() ? null : new BalanceInfoData(fields);
    }

    // NEW: Extract bill summary from MainActivity's merged data structure
    private static BillSummaryData extractBillSummaryFromMergedData(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Map<String, Object> mergedData = getMergedData(result);
            if (mergedData != null && mergedData.containsKey("bill_summary")) {
                Map<String, Object> billSummary = (Map<String, Object>) mergedData.get("bill_summary");
                
                addIfValid(fields, "Total Bills", getSafeString(billSummary.get("total_bills")));
                addIfValid(fields, "Latest Bill Date", getSafeString(billSummary.get("latest_bill_date")));
                addIfValid(fields, "Latest Bill Amount", formatCurrency(getSafeString(billSummary.get("latest_total_amount"))));
                addIfValid(fields, "Latest Consumption", getSafeString(billSummary.get("latest_consumption")) + " kWh");
                addIfValid(fields, "Recent Consumption (3 months)", getSafeString(billSummary.get("recent_consumption")) + " kWh");
                addIfValid(fields, "Recent Amount (3 months)", formatCurrency(getSafeString(billSummary.get("recent_amount"))));
            }

            // If no bill summary, create basic one
            if (fields.isEmpty()) {
                BillInfoData billInfo = extractBillInfoFromMergedData(result);
                if (billInfo != null && !billInfo.bills.isEmpty()) {
                    BillData latestBill = billInfo.bills.get(0);
                    fields.add(new KeyValuePair("Total Bills", String.valueOf(billInfo.bills.size())));
                    fields.add(new KeyValuePair("Latest Bill Date", latestBill.billMonth));
                    fields.add(new KeyValuePair("Latest Amount", latestBill.currentBill));
                    fields.add(new KeyValuePair("Latest Consumption", latestBill.consumption + " kWh"));
                } else {
                    fields.add(new KeyValuePair("Total Bills", "0"));
                    fields.add(new KeyValuePair("Latest Bill Date", "N/A"));
                    fields.add(new KeyValuePair("Latest Amount", "৳0"));
                    fields.add(new KeyValuePair("Status", "No bill history available"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fields.isEmpty() ? null : new BillSummaryData(fields);
    }

    // NEW: Extract prepaid customer info from MainActivity's cleaned SERVER1 data
    private static PrepaidCustomerInfoData extractPrepaidCustomerInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            // Try to get prepaid-specific data from SERVER1_data
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                Map<String, Object> cleanedData = cleanSERVER1DataForTemplate(server1Data);
                
                if (cleanedData.containsKey("customer_info")) {
                    Map<String, String> customerInfo = (Map<String, String>) cleanedData.get("customer_info");
                    for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                        if (isValidValue(entry.getValue())) {
                            fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                        }
                    }
                }
            }

            // Add basic identifiers
            addIfValid(fields, "Meter Number", extractMeterNumber(result));
            addIfValid(fields, "Consumer Number", extractConsumerNumber(result));

            // Supplement with merged data if available
            List<KeyValuePair> mergedInfo = extractCustomerInfoFromMergedData(result);
            for (KeyValuePair field : mergedInfo) {
                // Avoid duplicates
                boolean exists = false;
                for (KeyValuePair existing : fields) {
                    if (existing.key.equals(field.key)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists && isValidValue(field.value)) {
                    fields.add(field);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            fields.add(new KeyValuePair("Error", "Failed to extract prepaid info: " + e.getMessage()));
        }

        return new PrepaidCustomerInfoData(fields);
    }

    // NEW: Extract tokens from MainActivity's cleaned SERVER1 data
    private static TokensData extractTokensFromMergedData(Map<String, Object> result) {
        List<TokenData> tokenList = new ArrayList<>();

        try {
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                Map<String, Object> cleanedData = cleanSERVER1DataForTemplate(server1Data);
                
                if (cleanedData.containsKey("recent_transactions")) {
                    List<Map<String, String>> transactions = (List<Map<String, String>>) cleanedData.get("recent_transactions");
                    for (int i = 0; i < transactions.size() && i < 3; i++) {
                        Map<String, String> transaction = transactions.get(i);
                        tokenList.add(new TokenData(
                            i + 1,
                            transaction.get("Tokens"),
                            transaction.get("Date"),
                            transaction.get("Amount"),
                            transaction.get("Operator"),
                            transaction.get("Sequence")
                        ));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tokenList.isEmpty() ? null : new TokensData(tokenList);
    }

    // NEW: Helper to get merged data from MainActivity result
    private static Map<String, Object> getMergedData(Map<String, Object> result) {
        try {
            // Check if result already contains merged data (from MainActivity's mergeSERVERData)
            if (result.containsKey("customer_info") || result.containsKey("balance_info") || 
                result.containsKey("bill_info_raw") || result.containsKey("bill_summary")) {
                return result;
            }
            
            // Try to extract from unique_analysis or other merged structures
            if (result.containsKey("unique_analysis")) {
                return result;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // NEW: Clean SERVER1 data for template (simplified version of MainActivity's method)
    private static Map<String, Object> cleanSERVER1DataForTemplate(Object SERVER1DataObj) {
        Map<String, Object> cleaned = new HashMap<>();

        try {
            String responseBody;
            if (SERVER1DataObj instanceof String) {
                responseBody = (String) SERVER1DataObj;
            } else {
                responseBody = SERVER1DataObj.toString();
            }

            // Extract customer info
            Map<String, String> customerInfo = new HashMap<>();
            
            // Simple pattern matching for key fields
            extractIfContains(responseBody, "customerName", "Name", customerInfo);
            extractIfContains(responseBody, "customerAddress", "Address", customerInfo);
            extractIfContains(responseBody, "customerPhone", "Phone", customerInfo);
            extractIfContains(responseBody, "tariffCategory", "Tariff Category", customerInfo);
            extractIfContains(responseBody, "sanctionLoad", "Sanctioned Load", customerInfo);
            extractIfContains(responseBody, "meterType", "Meter Type", customerInfo);
            extractIfContains(responseBody, "accountType", "Account Type", customerInfo);
            extractIfContains(responseBody, "lastRechargeAmount", "Last Recharge Amount", customerInfo);
            extractIfContains(responseBody, "lastRechargeTime", "Last Recharge Time", customerInfo);

            if (!customerInfo.isEmpty()) {
                cleaned.put("customer_info", customerInfo);
            }

            // Extract tokens using simple pattern
            List<Map<String, String>> transactions = extractTokensWithSimplePattern(responseBody);
            if (!transactions.isEmpty()) {
                cleaned.put("recent_transactions", transactions);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cleaned;
    }

    // Helper methods
    private static void extractIfContains(String response, String jsonKey, String displayKey, Map<String, String> target) {
        try {
            String pattern = "\"" + jsonKey + "\":\"(.*?)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(response);
            if (m.find()) {
                String value = m.group(1);
                if (isValidValue(value)) {
                    target.put(displayKey, value);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static List<Map<String, String>> extractTokensWithSimplePattern(String response) {
        List<Map<String, String>> transactions = new ArrayList<>();
        
        try {
            // Look for token patterns
            String tokenPattern = "\"tokens\":\"(\\d{4}-\\d{4}-\\d{4}-\\d{4}-\\d{4})\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(tokenPattern);
            java.util.regex.Matcher matcher = pattern.matcher(response);
            
            int count = 0;
            while (matcher.find() && count < 3) {
                Map<String, String> transaction = new HashMap<>();
                transaction.put("Tokens", matcher.group(1));
                transaction.put("Date", "Recent");
                transaction.put("Amount", "৳Unknown");
                transaction.put("Operator", "System");
                transaction.put("Sequence", String.valueOf(count + 1));
                
                transactions.add(transaction);
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return transactions;
    }

    // Basic extraction methods
    private static List<KeyValuePair> extractBasicCustomerInfoDirect(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        
        addIfValid(fields, "Customer Number", extractCustomerNumber(result));
        addIfValid(fields, "Consumer Number", extractConsumerNumber(result));
        addIfValid(fields, "Meter Number", extractMeterNumber(result));
        
        return fields;
    }

    private static CustomerInfoData extractCustomerInfoFromMergedDataAsCustomerInfoData(Map<String, Object> result) {
        return new CustomerInfoData(extractCustomerInfoFromMergedData(result));
    }

    private static String extractCustomerNumber(Map<String, Object> result) {
        String[] keys = {"customer_number", "customerNumber", "CUSTOMER_NUMBER"};
        for (String key : keys) {
            if (result.containsKey(key)) {
                String value = getSafeString(result.get(key));
                if (!value.equals("N/A")) return value;
            }
        }
        return "N/A";
    }

    private static String extractConsumerNumber(Map<String, Object> result) {
        String[] keys = {"consumer_number", "consumerNumber", "customerAccountNo"};
        for (String key : keys) {
            if (result.containsKey(key)) {
                String value = getSafeString(result.get(key));
                if (!value.equals("N/A")) return value;
            }
        }
        return "N/A";
    }

    private static String extractMeterNumber(Map<String, Object> result) {
        String[] keys = {"meter_number", "meterNumber", "METER_NUM"};
        for (String key : keys) {
            if (result.containsKey(key)) {
                String value = getSafeString(result.get(key));
                if (!value.equals("N/A")) return value;
            }
        }
        return "N/A";
    }

    private static ErrorData extractError(Map<String, Object> result) {
        if (result.containsKey("error")) {
            return new ErrorData(getSafeString(result.get("error")));
        }
        return null;
    }

    // Utility methods
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

    private static void addIfValid(List<KeyValuePair> fields, String key, String value) {
        if (isValidValue(value)) {
            fields.add(new KeyValuePair(key, value));
        }
    }

    private static String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("N/A")) {
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
            return dateStr.substring(0, 7);
        } catch (Exception e) {
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : dateStr;
        }
    }

    private static String formatCurrency(String amount) {
        if (amount == null || amount.isEmpty() || amount.equals("N/A")) {
            return "৳0";
        }
        try {
            // Remove any existing currency symbols and trim
            String cleanAmount = amount.replace("৳", "").trim();
            if (cleanAmount.isEmpty() || cleanAmount.equals("0") || cleanAmount.equals("0.00")) {
                return "৳0";
            }
            // Try to parse as double to format consistently
            double value = Double.parseDouble(cleanAmount);
            return String.format("৳%.2f", value);
        } catch (Exception e) {
            return "৳" + amount;
        }
    }

    // Template rendering methods (keep your existing ones)
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

    // Keep your existing replacePostpaidPlaceholders and replacePrepaidPlaceholders methods
    // They should work fine with the corrected data structures

    private static String replacePostpaidPlaceholders(String template, PostpaidData data) {
        // Your existing implementation here
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
        // Your existing implementation here
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
}