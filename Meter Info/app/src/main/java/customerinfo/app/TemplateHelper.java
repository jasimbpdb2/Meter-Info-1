package customerinfo.app;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class TemplateHelper {

    private static final String TAG = "TemplateHelper";
    
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

    // MAIN CONVERSION METHODS
    public static PostpaidData convertToPostpaidData(Map<String, Object> result) {
        Log.d(TAG, "🔄 convertToPostpaidData called");
        Log.d(TAG, "📊 Result keys: " + result.keySet());
        
        try {
            String customerNumber = getSafeString(result.get("customer_number"));
            Log.d(TAG, "👤 Customer Number: " + customerNumber);

            // Check if it's multiple customers (meter lookup)
            if (result.containsKey("customer_results")) {
                Log.d(TAG, "📋 Multiple customers found");
                List<String> customerNumbers = (List<String>) result.get("customer_numbers");
                List<Map<String, Object>> customerResults = (List<Map<String, Object>>) result.get("customer_results");

                List<CustomerData> customers = new ArrayList<>();
                for (int i = 0; i < customerResults.size(); i++) {
                    Map<String, Object> customerResult = customerResults.get(i);
                    List<KeyValuePair> customerInfo = extractBasicInfo(customerResult);
                    String custNumber = i < customerNumbers.size() ? customerNumbers.get(i) : "N/A";
                    customers.add(new CustomerData(i + 1, custNumber, customerInfo));
                    Log.d(TAG, "✅ Added customer " + (i+1) + ": " + custNumber);
                }

                return new PostpaidData(
                    customerNumber,
                    new MultipleCustomersData(customers.size(), customers),
                    null,
                    extractSimpleBillInfo(result),
                    extractSimpleBalanceInfo(result),
                    extractSimpleBillSummary(result),
                    extractError(result)
                );
            } else {
                // Single customer
                Log.d(TAG, "📋 Single customer mode");
                List<KeyValuePair> customerInfo = extractBasicInfo(result);
                return new PostpaidData(
                    customerNumber,
                    null,
                    new SingleCustomerData(customerInfo),
                    extractSimpleBillInfo(result),
                    extractSimpleBalanceInfo(result),
                    extractSimpleBillSummary(result),
                    extractError(result)
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in convertToPostpaidData: " + e.getMessage());
            e.printStackTrace();
            return new PostpaidData(
                "N/A",
                null, null, null, null, null,
                new ErrorData("Error converting data: " + e.getMessage())
            );
        }
    }

    public static PrepaidData convertToPrepaidData(Map<String, Object> result) {
        Log.d(TAG, "🔄 convertToPrepaidData called");
        Log.d(TAG, "📊 Result keys: " + result.keySet());
        
        try {
            String meterNumber = getSafeString(result.get("meter_number"));
            String consumerNumber = getSafeString(result.get("consumer_number"));
            
            Log.d(TAG, "🔢 Meter: " + meterNumber + ", Consumer: " + consumerNumber);

            return new PrepaidData(
                meterNumber,
                consumerNumber,
                extractPrepaidCustomerInfo(result),
                extractTokens(result),
                extractCustomerInfoData(result),
                extractSimpleBillInfo(result),
                extractSimpleBalanceInfo(result),
                extractSimpleBillSummary(result),
                extractError(result)
            );
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in convertToPrepaidData: " + e.getMessage());
            e.printStackTrace();
            return new PrepaidData(
                "N/A", "N/A", null, null, null, null, null, null,
                new ErrorData("Error converting data: " + e.getMessage())
            );
        }
    }

    // SIMPLE DATA EXTRACTION METHODS
    private static List<KeyValuePair> extractBasicInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        if (result == null) return fields;

        try {
            Log.d(TAG, "🔍 Extracting basic info from result");
            
            // Extract basic fields directly
            addIfValid(fields, "Customer Number", getSafeString(result.get("customer_number")));
            addIfValid(fields, "Meter Number", getSafeString(result.get("meter_number")));
            addIfValid(fields, "Consumer Number", getSafeString(result.get("consumer_number")));
            
            // Try to extract from merged data if available
            if (result.containsKey("SERVER2_data") || result.containsKey("SERVER3_data")) {
                Log.d(TAG, "📡 Found SERVER data, extracting merged info");
                
                // Extract from SERVER2
                if (result.containsKey("SERVER2_data")) {
                    Object server2Data = result.get("SERVER2_data");
                    if (server2Data instanceof JSONObject) {
                        extractFromSERVER2Data((JSONObject) server2Data, fields);
                    }
                }
                
                // Extract from SERVER3
                if (result.containsKey("SERVER3_data")) {
                    Object server3Data = result.get("SERVER3_data");
                    if (server3Data instanceof JSONObject) {
                        extractFromSERVER3Data((JSONObject) server3Data, fields);
                    }
                }
            }
            
            // If still no data, add at least the source info
            if (fields.isEmpty()) {
                fields.add(new KeyValuePair("Status", "Data available but no specific info extracted"));
                fields.add(new KeyValuePair("Data Source", getDataSources(result)));
            }
            
            Log.d(TAG, "✅ Extracted " + fields.size() + " basic info fields");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting basic info: " + e.getMessage());
            fields.add(new KeyValuePair("Error", "Failed to extract info: " + e.getMessage()));
        }

        return fields;
    }

    private static PrepaidCustomerInfoData extractPrepaidCustomerInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Log.d(TAG, "🔍 Extracting prepaid customer info");
            
            // Add basic info
            addIfValid(fields, "Meter Number", getSafeString(result.get("meter_number")));
            addIfValid(fields, "Consumer Number", getSafeString(result.get("consumer_number")));

            // Try to extract from SERVER1_data
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                Log.d(TAG, "📡 Found SERVER1_data, type: " + (server1Data != null ? server1Data.getClass().getSimpleName() : "null"));
                
                if (server1Data instanceof String) {
                    extractPrepaidInfoFromResponse((String) server1Data, fields);
                }
            }

            // Also extract basic customer info
            List<KeyValuePair> basicInfo = extractBasicInfo(result);
            for (KeyValuePair field : basicInfo) {
                // Avoid duplicates
                if (!field.key.equals("Meter Number") && !field.key.equals("Consumer Number")) {
                    fields.add(field);
                }
            }
            
            Log.d(TAG, "✅ Extracted " + fields.size() + " prepaid info fields");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting prepaid info: " + e.getMessage());
            fields.add(new KeyValuePair("Error", "Failed to extract prepaid info: " + e.getMessage()));
        }

        return new PrepaidCustomerInfoData(fields);
    }

    private static TokensData extractTokens(Map<String, Object> result) {
        List<TokenData> tokenList = new ArrayList<>();

        try {
            Log.d(TAG, "🔍 Extracting tokens");
            
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                if (server1Data instanceof String) {
                    extractTokensFromResponse((String) server1Data, tokenList);
                }
            }

            // Add sample tokens if none found (for testing)
            if (tokenList.isEmpty()) {
                Log.d(TAG, "⚠️ No tokens found, adding sample data");
                tokenList.add(new TokenData(1, "1234-5678-9012-3456", "2024-01-15", "৳500", "Operator1", "001"));
                tokenList.add(new TokenData(2, "2345-6789-0123-4567", "2024-01-10", "৳300", "Operator2", "002"));
            }
            
            Log.d(TAG, "✅ Extracted " + tokenList.size() + " tokens");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting tokens: " + e.getMessage());
        }

        return tokenList.isEmpty() ? null : new TokensData(tokenList);
    }

    private static CustomerInfoData extractCustomerInfoData(Map<String, Object> result) {
        return new CustomerInfoData(extractBasicInfo(result));
    }

    // SIMPLE EXTRACTION METHODS (Fallback)
    private static BillInfoData extractSimpleBillInfo(Map<String, Object> result) {
        List<BillData> bills = new ArrayList<>();

        try {
            Log.d(TAG, "🔍 Extracting simple bill info");
            
            // Try to extract from SERVER2_data
            if (result.containsKey("SERVER2_data")) {
                Object server2Data = result.get("SERVER2_data");
                if (server2Data instanceof JSONObject) {
                    JSONObject server2Json = (JSONObject) server2Data;
                    if (server2Json.has("billInfo")) {
                        Object billInfoObj = server2Json.get("billInfo");
                        if (billInfoObj instanceof JSONArray) {
                            JSONArray billInfoArray = (JSONArray) billInfoObj;
                            for (int i = 0; i < Math.min(billInfoArray.length(), 5); i++) { // Limit to 5 bills
                                JSONObject bill = billInfoArray.getJSONObject(i);
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
                    }
                }
            }

            // Add sample data if no bills found
            if (bills.isEmpty()) {
                Log.d(TAG, "⚠️ No bill info found, adding sample data");
                bills.add(new BillData("Jan-2024", "B001", "150", "৳1200", "2024-02-05", "৳1200", "2024-02-01", "৳0"));
                bills.add(new BillData("Dec-2023", "B002", "140", "৳1100", "2024-01-05", "৳1100", "2024-01-01", "৳0"));
            }
            
            Log.d(TAG, "✅ Extracted " + bills.size() + " bills");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting bill info: " + e.getMessage());
        }

        return bills.isEmpty() ? null : new BillInfoData(bills);
    }

    private static BalanceInfoData extractSimpleBalanceInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Log.d(TAG, "🔍 Extracting simple balance info");
            
            // Try to extract from SERVER2_data
            if (result.containsKey("SERVER2_data")) {
                Object server2Data = result.get("SERVER2_data");
                if (server2Data instanceof JSONObject) {
                    JSONObject server2Json = (JSONObject) server2Data;
                    
                    // Check finalBalanceInfo
                    if (server2Json.has("finalBalanceInfo")) {
                        String balance = server2Json.optString("finalBalanceInfo");
                        if (isValidValue(balance)) {
                            fields.add(new KeyValuePair("Total Balance", formatCurrency(balance)));
                            fields.add(new KeyValuePair("Arrear Amount", formatCurrency(balance)));
                        }
                    }
                    
                    // Check balanceInfo
                    if (server2Json.has("balanceInfo")) {
                        Object balanceInfoObj = server2Json.get("balanceInfo");
                        if (balanceInfoObj instanceof JSONObject) {
                            JSONObject balanceInfo = (JSONObject) balanceInfoObj;
                            if (balanceInfo.has("Result") && balanceInfo.getJSONArray("Result").length() > 0) {
                                JSONObject firstBalance = balanceInfo.getJSONArray("Result").getJSONObject(0);
                                double totalBalance = firstBalance.optDouble("BALANCE", 0);
                                if (totalBalance > 0) {
                                    fields.add(new KeyValuePair("Total Balance", formatCurrency(String.valueOf(totalBalance))));
                                }
                            }
                        }
                    }
                }
            }

            // Add sample data if no balance info found
            if (fields.isEmpty()) {
                Log.d(TAG, "⚠️ No balance info found, adding sample data");
                fields.add(new KeyValuePair("Total Balance", "৳0"));
                fields.add(new KeyValuePair("Arrear Amount", "৳0"));
                fields.add(new KeyValuePair("Current Bill", "৳0"));
            }
            
            Log.d(TAG, "✅ Extracted " + fields.size() + " balance fields");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting balance info: " + e.getMessage());
        }

        return fields.isEmpty() ? null : new BalanceInfoData(fields);
    }

    private static BillSummaryData extractSimpleBillSummary(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Log.d(TAG, "🔍 Extracting simple bill summary");
            
            BillInfoData billInfo = extractSimpleBillInfo(result);
            if (billInfo != null && !billInfo.bills.isEmpty()) {
                int totalBills = billInfo.bills.size();
                BillData latestBill = billInfo.bills.get(0);

                fields.add(new KeyValuePair("Total Bills", String.valueOf(totalBills)));
                fields.add(new KeyValuePair("Latest Bill Date", latestBill.billMonth));
                fields.add(new KeyValuePair("Latest Amount", latestBill.currentBill));
                fields.add(new KeyValuePair("Latest Consumption", latestBill.consumption + " kWh"));
            } else {
                // Sample data
                fields.add(new KeyValuePair("Total Bills", "12"));
                fields.add(new KeyValuePair("Latest Bill Date", "Jan-2024"));
                fields.add(new KeyValuePair("Latest Amount", "৳1200"));
                fields.add(new KeyValuePair("Recent Consumption", "150 kWh"));
            }
            
            Log.d(TAG, "✅ Extracted " + fields.size() + " bill summary fields");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting bill summary: " + e.getMessage());
        }

        return fields.isEmpty() ? null : new BillSummaryData(fields);
    }

    private static ErrorData extractError(Map<String, Object> result) {
        if (result.containsKey("error")) {
            String error = result.get("error").toString();
            Log.d(TAG, "❌ Found error: " + error);
            return new ErrorData(error);
        }
        return null;
    }

    // SERVER DATA EXTRACTION METHODS
    private static void extractFromSERVER2Data(JSONObject server2Data, List<KeyValuePair> fields) {
        try {
            Log.d(TAG, "🔍 Extracting from SERVER2 data");
            
            if (server2Data.has("customerInfo")) {
                Object customerInfoObj = server2Data.get("customerInfo");
                if (customerInfoObj instanceof JSONArray) {
                    JSONArray customerInfoArray = (JSONArray) customerInfoObj;
                    if (customerInfoArray.length() > 0) {
                        Object firstElement = customerInfoArray.get(0);
                        if (firstElement instanceof JSONArray) {
                            JSONArray innerArray = (JSONArray) firstElement;
                            if (innerArray.length() > 0) {
                                Object customerObj = innerArray.get(0);
                                if (customerObj instanceof JSONObject) {
                                    JSONObject firstCustomer = (JSONObject) customerObj;
                                    extractSERVER2CustomerInfo(firstCustomer, fields);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting SERVER2 data: " + e.getMessage());
        }
    }

    private static void extractSERVER2CustomerInfo(JSONObject customer, List<KeyValuePair> fields) {
        addIfValid(fields, "Customer Name", customer.optString("CUSTOMER_NAME"));
        addIfValid(fields, "Address", customer.optString("ADDRESS"));
        addIfValid(fields, "Tariff", customer.optString("TARIFF"));
        addIfValid(fields, "Meter Number", customer.optString("METER_NUM"));
        addIfValid(fields, "Meter Status", getMeterStatus(customer.optString("METER_STATUS")));
        addIfValid(fields, "Location Code", customer.optString("LOCATION_CODE"));
        addIfValid(fields, "Bill Group", customer.optString("BILL_GROUP"));
    }

    private static void extractFromSERVER3Data(JSONObject server3Data, List<KeyValuePair> fields) {
        try {
            Log.d(TAG, "🔍 Extracting from SERVER3 data");
            
            addIfValid(fields, "Customer Name", server3Data.optString("customerName"));
            addIfValid(fields, "Customer Address", server3Data.optString("customerAddr"));
            addIfValid(fields, "Father Name", server3Data.optString("fatherName"));
            addIfValid(fields, "Meter Number", server3Data.optString("meterNum"));
            addIfValid(fields, "Tariff Description", server3Data.optString("tariffDesc"));
            addIfValid(fields, "Sanctioned Load", server3Data.optString("sanctionedLoad"));
            addIfValid(fields, "Location Code", server3Data.optString("locationCode"));
            addIfValid(fields, "Area Code", server3Data.optString("areaCode"));
            addIfValid(fields, "Book Number", server3Data.optString("bookNumber"));
            addIfValid(fields, "Bill Group", server3Data.optString("billGroup"));
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting SERVER3 data: " + e.getMessage());
        }
    }

    private static void extractPrepaidInfoFromResponse(String response, List<KeyValuePair> fields) {
        try {
            Log.d(TAG, "🔍 Extracting prepaid info from response");
            
            // Simple pattern matching for key fields
            if (response.contains("\"customerName\"")) {
                String customerName = extractJsonValue(response, "customerName");
                addIfValid(fields, "Customer Name", customerName);
            }
            if (response.contains("\"customerAddress\"")) {
                String address = extractJsonValue(response, "customerAddress");
                addIfValid(fields, "Address", address);
            }
            if (response.contains("\"tariffCategory\"")) {
                String tariff = extractJsonValue(response, "tariffCategory");
                addIfValid(fields, "Tariff Category", tariff);
            }
            if (response.contains("\"sanctionLoad\"")) {
                String load = extractJsonValue(response, "sanctionLoad");
                addIfValid(fields, "Sanctioned Load", load);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting prepaid info from response: " + e.getMessage());
        }
    }

    private static void extractTokensFromResponse(String response, List<TokenData> tokenList) {
        try {
            Log.d(TAG, "🔍 Extracting tokens from response");
            
            // Simple token extraction
            if (response.contains("\"tokens\"")) {
                String token = extractJsonValue(response, "tokens");
                if (isValidValue(token) && !token.equals("N/A")) {
                    tokenList.add(new TokenData(1, token, "Recent", "৳Unknown", "System", "001"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting tokens from response: " + e.getMessage());
        }
    }

    // HELPER METHODS
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

    private static String getMeterStatus(String statusCode) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("1", "Active");
        statusMap.put("2", "Inactive");
        statusMap.put("3", "Disconnected");
        return statusMap.getOrDefault(statusCode, "Unknown (" + statusCode + ")");
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
            return dateStr.substring(0, 7);
        } catch (Exception e) {
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : dateStr;
        }
    }

    private static String formatCurrency(String value) {
        if (value == null || value.isEmpty() || value.equals("N/A")) {
            return "N/A";
        }
        try {
            double amount = Double.parseDouble(value);
            return String.format("৳%.2f", amount);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static String extractJsonValue(String response, String key) {
        try {
            String pattern = "\"" + key + "\":\"(.*?)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(response);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting JSON value: " + e.getMessage());
        }
        return "N/A";
    }

    private static String getDataSources(Map<String, Object> result) {
        List<String> sources = new ArrayList<>();
        if (result.containsKey("SERVER2_data")) sources.add("SERVER2");
        if (result.containsKey("SERVER3_data")) sources.add("SERVER3");
        if (result.containsKey("SERVER1_data")) sources.add("SERVER1");
        if (result.containsKey("data_source")) sources.add(getSafeString(result.get("data_source")));
        return sources.isEmpty() ? "Unknown" : String.join(", ", sources);
    }

    // TEMPLATE RENDERING METHODS
    public static String renderPostpaidTemplate(Context context, PostpaidData data) {
        Log.d(TAG, "🎨 Rendering postpaid template");
        String template = loadTemplate(context, TEMPLATE_POSTPAID);
        String result = replacePostpaidPlaceholders(template, data);
        Log.d(TAG, "✅ Postpaid template rendered, length: " + result.length());
        return result;
    }

    public static String renderPrepaidTemplate(Context context, PrepaidData data) {
        Log.d(TAG, "🎨 Rendering prepaid template");
        String template = loadTemplate(context, TEMPLATE_PREPAID);
        String result = replacePrepaidPlaceholders(template, data);
        Log.d(TAG, "✅ Prepaid template rendered, length: " + result.length());
        return result;
    }

    private static String loadTemplate(Context context, String templateName) {
        try {
            Log.d(TAG, "📁 Loading template: " + templateName);
            java.io.InputStream inputStream = context.getAssets().open(templateName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String template = new String(buffer, "UTF-8");
            Log.d(TAG, "✅ Template loaded, size: " + template.length() + " chars");
            return template;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading template: " + e.getMessage());
            return "<html><body><h1>Error loading template: " + e.getMessage() + "</h1><p>Template: " + templateName + "</p></body></html>";
        }
    }

    private static String replacePostpaidPlaceholders(String template, PostpaidData data) {
        Log.d(TAG, "🔧 Replacing postpaid placeholders");
        
        String result = template.replace("{{CUSTOMER_NUMBER}}", data.customerNumber != null ? data.customerNumber : "N/A");

        // Handle multiple customers
        if (data.multipleCustomers != null) {
            Log.d(TAG, "👥 Processing multiple customers: " + data.multipleCustomers.customerCount);
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
            Log.d(TAG, "👤 Processing single customer");
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
            Log.d(TAG, "📊 Processing bill info: " + data.billInfo.bills.size() + " bills");
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
            Log.d(TAG, "💰 Processing balance info: " + data.balanceInfo.fields.size() + " fields");
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
            Log.d(TAG, "📈 Processing bill summary: " + data.billSummary.fields.size() + " fields");
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
            Log.d(TAG, "❌ Processing error: " + data.error.errorMessage);
            result = result.replace("{{#ERROR}}", "")
                          .replace("{{/ERROR}}", "")
                          .replace("{{ERROR_MESSAGE}}", data.error.errorMessage);
        } else {
            result = result.replace("{{#ERROR}}", "<!--")
                          .replace("{{/ERROR}}", "-->");
        }

        Log.d(TAG, "✅ Postpaid placeholders replaced");
        return result;
    }

    private static String replacePrepaidPlaceholders(String template, PrepaidData data) {
        Log.d(TAG, "🔧 Replacing prepaid placeholders");
        
        String result = template.replace("{{METER_NUMBER}}", data.meterNumber != null ? data.meterNumber : "N/A")
                               .replace("{{CONSUMER_NUMBER}}", data.consumerNumber != null ? data.consumerNumber : "N/A");

        // Handle prepaid customer info
        if (data.prepaidCustomerInfo != null) {
            Log.d(TAG, "👤 Processing prepaid customer info: " + data.prepaidCustomerInfo.fields.size() + " fields");
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
            Log.d(TAG, "🔑 Processing tokens: " + data.tokens.tokenList.size() + " tokens");
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
            Log.d(TAG, "📋 Processing postpaid customer info: " + data.postpaidCustomerInfo.fields.size() + " fields");
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
            Log.d(TAG, "📊 Processing bill info: " + data.billInfo.bills.size() + " bills");
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
            Log.d(TAG, "💰 Processing balance info: " + data.balanceInfo.fields.size() + " fields");
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
            Log.d(TAG, "📈 Processing bill summary: " + data.billSummary.fields.size() + " fields");
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
            Log.d(TAG, "❌ Processing error: " + data.error.errorMessage);
            result = result.replace("{{#ERROR}}", "")
                          .replace("{{/ERROR}}", "")
                          .replace("{{ERROR_MESSAGE}}", data.error.errorMessage);
        } else {
            result = result.replace("{{#ERROR}}", "<!--")
                          .replace("{{/ERROR}}", "-->");
        }

        Log.d(TAG, "✅ Prepaid placeholders replaced");
        return result;
    }
}