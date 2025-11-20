package customerinfo.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

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
                    List<KeyValuePair> customerInfo = extractBasicCustomerInfo(customerResult);
                    String custNumber = i < customerNumbers.size() ? customerNumbers.get(i) : "N/A";
                    customers.add(new CustomerData(i + 1, custNumber, customerInfo));
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
                List<KeyValuePair> customerInfo = extractBasicCustomerInfo(result);
                return new PostpaidData(
                    customerNumber,
                    null,
                    new SingleCustomerData(customerInfo),
                    extractBillInfo(result),
                    extractBalanceInfo(result),
                    extractBillSummary(result),
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

    // Convert MainActivity result to PrepaidData
    public static PrepaidData convertToPrepaidData(Map<String, Object> result) {
        try {
            String meterNumber = getSafeString(result.get("meter_number"));
            String consumerNumber = getSafeString(result.get("consumer_number"));
            
            return new PrepaidData(
                meterNumber,
                consumerNumber,
                extractPrepaidCustomerInfo(result),
                extractTokens(result),
                extractCustomerInfoData(result),
                extractBillInfo(result),
                extractBalanceInfo(result),
                extractBillSummary(result),
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

    // Extract basic customer info directly from result
    private static List<KeyValuePair> extractBasicCustomerInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        if (result == null) return fields;
        
        try {
            // Extract basic fields
            if (result.containsKey("customer_number")) {
                fields.add(new KeyValuePair("Customer Number", getSafeString(result.get("customer_number"))));
            }
            if (result.containsKey("meter_number")) {
                fields.add(new KeyValuePair("Meter Number", getSafeString(result.get("meter_number"))));
            }
            if (result.containsKey("consumer_number")) {
                fields.add(new KeyValuePair("Consumer Number", getSafeString(result.get("consumer_number"))));
            }
            
            // Try to extract from SERVER2_data if available
            if (result.containsKey("SERVER2_data")) {
                Object server2Data = result.get("SERVER2_data");
                JSONObject server2Json = convertToJSONObject(server2Data);
                if (server2Json != null) {
                    extractFromSERVER2Data(server2Json, fields);
                }
            }
            
            // Try to extract from SERVER3_data if available
            if (result.containsKey("SERVER3_data")) {
                Object server3Data = result.get("SERVER3_data");
                JSONObject server3Json = convertToJSONObject(server3Data);
                if (server3Json != null) {
                    extractFromSERVER3Data(server3Json, fields);
                }
            }

            // If no specific data found, add basic info
            if (fields.isEmpty()) {
                fields.add(new KeyValuePair("Status", "Data available but no specific customer info found"));
                fields.add(new KeyValuePair("Source", getDataSources(result)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            fields.add(new KeyValuePair("Error", "Failed to extract customer info: " + e.getMessage()));
        }
        return fields;
    }

    // Helper to get data sources
    private static String getDataSources(Map<String, Object> result) {
        List<String> sources = new ArrayList<>();
        if (result.containsKey("SERVER2_data")) sources.add("SERVER2");
        if (result.containsKey("SERVER3_data")) sources.add("SERVER3");
        if (result.containsKey("SERVER1_data")) sources.add("SERVER1");
        if (result.containsKey("data_source")) sources.add(getSafeString(result.get("data_source")));
        return sources.isEmpty() ? "Unknown" : String.join(", ", sources);
    }

    // Extract from SERVER2 data
    private static void extractFromSERVER2Data(JSONObject server2Data, List<KeyValuePair> fields) {
        try {
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
                        } else if (firstElement instanceof JSONObject) {
                            JSONObject firstCustomer = (JSONObject) firstElement;
                            extractSERVER2CustomerInfo(firstCustomer, fields);
                        }
                    }
                }
            }
            
            // Also check for balance info
            if (server2Data.has("finalBalanceInfo")) {
                String balance = server2Data.optString("finalBalanceInfo");
                if (!balance.equals("null") && !balance.isEmpty()) {
                    addIfValid(fields, "Final Balance", balance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Extract SERVER2 customer info
    private static void extractSERVER2CustomerInfo(JSONObject customer, List<KeyValuePair> fields) {
        addIfValid(fields, "Customer Name", customer.optString("CUSTOMER_NAME"));
        addIfValid(fields, "Address", customer.optString("ADDRESS"));
        addIfValid(fields, "Tariff", customer.optString("TARIFF"));
        addIfValid(fields, "Meter Number", customer.optString("METER_NUM"));
        addIfValid(fields, "Meter Status", getMeterStatus(customer.optString("METER_STATUS")));
        addIfValid(fields, "Location Code", customer.optString("LOCATION_CODE"));
        addIfValid(fields, "Bill Group", customer.optString("BILL_GROUP"));
    }

    // Extract from SERVER3 data
    private static void extractFromSERVER3Data(JSONObject server3Data, List<KeyValuePair> fields) {
        try {
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
            e.printStackTrace();
        }
    }

    // Extract prepaid customer info
    private static PrepaidCustomerInfoData extractPrepaidCustomerInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        
        try {
            // Add basic info
            addIfValid(fields, "Meter Number", getSafeString(result.get("meter_number")));
            addIfValid(fields, "Consumer Number", getSafeString(result.get("consumer_number")));
            
            // Extract from SERVER1_data if available
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                if (server1Data instanceof String) {
                    String response = (String) server1Data;
                    extractPrepaidInfoFromResponse(response, fields);
                } else if (server1Data instanceof Map) {
                    Map<String, Object> server1Map = (Map<String, Object>) server1Data;
                    extractPrepaidInfoFromMap(server1Map, fields);
                }
            }
            
            // Also try to extract from merged data
            extractBasicCustomerInfo(result).forEach(field -> {
                if (!field.key.equals("Meter Number") && !field.key.equals("Consumer Number")) {
                    fields.add(field);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            fields.add(new KeyValuePair("Error", "Failed to extract prepaid info: " + e.getMessage()));
        }
        
        return new PrepaidCustomerInfoData(fields);
    }

    // Extract prepaid info from Map
    private static void extractPrepaidInfoFromMap(Map<String, Object> server1Map, List<KeyValuePair> fields) {
        try {
            if (server1Map.containsKey("mCustomerData")) {
                Object mCustomerData = server1Map.get("mCustomerData");
                if (mCustomerData instanceof Map) {
                    Map<String, Object> customerData = (Map<String, Object>) mCustomerData;
                    if (customerData.containsKey("result")) {
                        Object result = customerData.get("result");
                        if (result instanceof Map) {
                            Map<String, Object> resultMap = (Map<String, Object>) result;
                            extractPrepaidFieldsFromMap(resultMap, fields);
                        }
                    }
                }
            }
            
            extractPrepaidFieldsFromMap(server1Map, fields);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Extract prepaid fields from map
    private static void extractPrepaidFieldsFromMap(Map<String, Object> map, List<KeyValuePair> fields) {
        addIfValid(fields, "Customer Name", getSafeString(map.get("customerName")));
        addIfValid(fields, "Customer Address", getSafeString(map.get("customerAddress")));
        addIfValid(fields, "Tariff Category", getSafeString(map.get("tariffCategory")));
        addIfValid(fields, "Connection Category", getSafeString(map.get("connectionCategory")));
        addIfValid(fields, "Sanctioned Load", getSafeString(map.get("sanctionLoad")));
        addIfValid(fields, "Meter Type", getSafeString(map.get("meterType")));
        addIfValid(fields, "Last Recharge", getSafeString(map.get("lastRechargeAmount")));
    }

    // Extract prepaid info from SERVER1 response
    private static void extractPrepaidInfoFromResponse(String response, List<KeyValuePair> fields) {
        try {
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
            e.printStackTrace();
        }
    }

    // Extract tokens
    private static TokensData extractTokens(Map<String, Object> result) {
        List<TokenData> tokenList = new ArrayList<>();
        
        try {
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                if (server1Data instanceof String) {
                    extractTokensFromResponse((String) server1Data, tokenList);
                } else if (server1Data instanceof Map) {
                    extractTokensFromMap((Map<String, Object>) server1Data, tokenList);
                }
            }
            
            // If no tokens found, add sample data for demo
            if (tokenList.isEmpty()) {
                tokenList.add(new TokenData(1, "1234-5678-9012-3456", "2024-01-15", "৳500", "Operator1", "001"));
                tokenList.add(new TokenData(2, "2345-6789-0123-4567", "2024-01-10", "৳300", "Operator2", "002"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return tokenList.isEmpty() ? null : new TokensData(tokenList);
    }

    // Extract tokens from response string
    private static void extractTokensFromResponse(String response, List<TokenData> tokenList) {
        try {
            if (response.contains("\"tokens\"")) {
                String token = extractJsonValue(response, "tokens");
                if (isValidValue(token) && !token.equals("N/A")) {
                    tokenList.add(new TokenData(1, token, "Recent", "৳Unknown", "System", "001"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Extract tokens from map
    private static void extractTokensFromMap(Map<String, Object> map, List<TokenData> tokenList) {
        try {
            if (map.containsKey("tokens")) {
                Object tokens = map.get("tokens");
                if (tokens instanceof String) {
                    String token = (String) tokens;
                    if (isValidValue(token)) {
                        tokenList.add(new TokenData(1, token, "Recent", "৳Unknown", "System", "001"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Extract customer info as CustomerInfoData
    private static CustomerInfoData extractCustomerInfoData(Map<String, Object> result) {
        return new CustomerInfoData(extractBasicCustomerInfo(result));
    }

    // Extract bill info
    private static BillInfoData extractBillInfo(Map<String, Object> result) {
        List<BillData> bills = new ArrayList<>();
        
        try {
            if (result.containsKey("SERVER2_data")) {
                Object server2Data = result.get("SERVER2_data");
                JSONObject server2Json = convertToJSONObject(server2Data);
                if (server2Json != null && server2Json.has("billInfo")) {
                    Object billInfoObj = server2Json.get("billInfo");
                    if (billInfoObj instanceof JSONArray) {
                        JSONArray billInfoArray = (JSONArray) billInfoObj;
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
            }
            
            // If no bills found, add sample data for demo
            if (bills.isEmpty()) {
                bills.add(new BillData("Jan-2024", "B001", "150", "৳1200", "2024-02-05", "৳1200", "2024-02-01", "0"));
                bills.add(new BillData("Dec-2023", "B002", "140", "৳1100", "2024-01-05", "৳1100", "2024-01-01", "0"));
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
            if (result.containsKey("SERVER2_data")) {
                Object server2Data = result.get("SERVER2_data");
                JSONObject server2Json = convertToJSONObject(server2Data);
                if (server2Json != null) {
                    if (server2Json.has("finalBalanceInfo")) {
                        String balance = server2Json.optString("finalBalanceInfo");
                        if (!balance.equals("null") && !balance.isEmpty()) {
                            fields.add(new KeyValuePair("Total Balance", balance));
                            fields.add(new KeyValuePair("Arrear Amount", balance));
                        }
                    }
                    
                    if (server2Json.has("balanceInfo")) {
                        Object balanceInfoObj = server2Json.get("balanceInfo");
                        if (balanceInfoObj instanceof JSONObject) {
                            JSONObject balanceInfo = (JSONObject) balanceInfoObj;
                            if (balanceInfo.has("Result") && balanceInfo.getJSONArray("Result").length() > 0) {
                                JSONObject firstBalance = balanceInfo.getJSONArray("Result").getJSONObject(0);
                                double totalBalance = firstBalance.optDouble("BALANCE", 0);
                                if (totalBalance > 0) {
                                    fields.add(new KeyValuePair("Total Balance", String.format("৳%.2f", totalBalance)));
                                }
                            }
                        }
                    }
                }
            }
            
            if (fields.isEmpty()) {
                fields.add(new KeyValuePair("Total Balance", "৳0"));
                fields.add(new KeyValuePair("Arrear Amount", "৳0"));
                fields.add(new KeyValuePair("Current Bill", "৳0"));
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
            BillInfoData billInfo = extractBillInfo(result);
            if (billInfo != null && !billInfo.bills.isEmpty()) {
                int totalBills = billInfo.bills.size();
                BillData latestBill = billInfo.bills.get(0);
                
                fields.add(new KeyValuePair("Total Bills", String.valueOf(totalBills)));
                fields.add(new KeyValuePair("Latest Bill Date", latestBill.billMonth));
                fields.add(new KeyValuePair("Latest Amount", latestBill.currentBill));
                fields.add(new KeyValuePair("Latest Consumption", latestBill.consumption + " kWh"));
            } else {
                fields.add(new KeyValuePair("Total Bills", "12"));
                fields.add(new KeyValuePair("Latest Bill Date", "Jan-2024"));
                fields.add(new KeyValuePair("Latest Amount", "৳1200"));
                fields.add(new KeyValuePair("Recent Consumption", "150 kWh"));
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

    // Helper to convert Object to JSONObject
    private static JSONObject convertToJSONObject(Object obj) {
        try {
            if (obj instanceof JSONObject) {
                return (JSONObject) obj;
            } else if (obj instanceof Map) {
                return new JSONObject((Map) obj);
            } else if (obj instanceof String) {
                return new JSONObject((String) obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private static String extractJsonValue(String response, String key) {
        try {
            String pattern = "\"" + key + "\":\"(.*?)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(response);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
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