package customerinfo.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class TemplateHelper {

    public static final String TEMPLATE_POSTPAID = "postpaid_template.html";
    public static final String TEMPLATE_PREPAID = "prepaid_template.html";

    // Data classes (keep your existing ones)
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

    // MAIN FIX: Convert MainActivity result to PostpaidData
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
                    List<KeyValuePair> customerInfo = extractCustomerInfoFromMergedData(customerResult);
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
                List<KeyValuePair> customerInfo = extractCustomerInfoFromMergedData(result);
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

    // MAIN FIX: Convert MainActivity result to PrepaidData
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

    // FIXED: Extract customer info using MainActivity's mergeSERVERData method
    private static List<KeyValuePair> extractCustomerInfoFromMergedData(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();
        if (result == null) return fields;

        try {
            // First, try to get merged data using MainActivity's logic
            Map<String, Object> mergedData = mergeSERVERData(result);
            
            if (mergedData != null && mergedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                
                // Add all customer info fields
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), entry.getValue()));
                    }
                }
            }

            // If no merged data, try direct extraction
            if (fields.isEmpty()) {
                extractBasicInfoDirectly(result, fields);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fields.add(new KeyValuePair("Error", "Failed to extract customer info: " + e.getMessage()));
        }

        return fields;
    }

    // FIXED: Extract prepaid customer info
    private static PrepaidCustomerInfoData extractPrepaidCustomerInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            // Add basic info
            addIfValid(fields, "Meter Number", getSafeString(result.get("meter_number")));
            addIfValid(fields, "Consumer Number", getSafeString(result.get("consumer_number")));

            // Try to extract from SERVER1_data using MainActivity's cleaning logic
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                Map<String, Object> cleanedData = cleanSERVER1Data(server1Data);
                
                if (cleanedData.containsKey("customer_info")) {
                    Map<String, String> customerInfo = (Map<String, String>) cleanedData.get("customer_info");
                    for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                        addIfValid(fields, entry.getKey(), entry.getValue());
                    }
                }
            }

            // Also try merged data
            Map<String, Object> mergedData = mergeSERVERData(result);
            if (mergedData != null && mergedData.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) mergedData.get("customer_info");
                for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                    // Don't duplicate basic fields
                    if (!entry.getKey().equals("Meter Number") && !entry.getKey().equals("Consumer Number")) {
                        addIfValid(fields, entry.getKey(), entry.getValue());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            fields.add(new KeyValuePair("Error", "Failed to extract prepaid info: " + e.getMessage()));
        }

        return new PrepaidCustomerInfoData(fields);
    }

    // FIXED: Extract tokens using MainActivity's logic
    private static TokensData extractTokens(Map<String, Object> result) {
        List<TokenData> tokenList = new ArrayList<>();

        try {
            if (result.containsKey("SERVER1_data")) {
                Object server1Data = result.get("SERVER1_data");
                Map<String, Object> cleanedData = cleanSERVER1Data(server1Data);
                
                if (cleanedData.containsKey("recent_transactions")) {
                    List<Map<String, String>> transactions = (List<Map<String, String>>) cleanedData.get("recent_transactions");
                    
                    for (int i = 0; i < transactions.size(); i++) {
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

    // FIXED: Extract customer info data
    private static CustomerInfoData extractCustomerInfoData(Map<String, Object> result) {
        return new CustomerInfoData(extractCustomerInfoFromMergedData(result));
    }

    // FIXED: Extract bill info using MainActivity's merged data
    private static BillInfoData extractBillInfo(Map<String, Object> result) {
        List<BillData> bills = new ArrayList<>();

        try {
            Map<String, Object> mergedData = mergeSERVERData(result);
            
            if (mergedData != null && mergedData.containsKey("bill_info_raw")) {
                JSONArray billInfo = (JSONArray) mergedData.get("bill_info_raw");
                
                for (int i = 0; i < billInfo.length(); i++) {
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bills.isEmpty() ? null : new BillInfoData(bills);
    }

    // FIXED: Extract balance info using MainActivity's merged data
    private static BalanceInfoData extractBalanceInfo(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Map<String, Object> mergedData = mergeSERVERData(result);
            
            if (mergedData != null && mergedData.containsKey("balance_info")) {
                Map<String, String> balanceInfo = (Map<String, String>) mergedData.get("balance_info");
                
                for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        fields.add(new KeyValuePair(entry.getKey(), formatCurrency(entry.getValue())));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fields.isEmpty() ? null : new BalanceInfoData(fields);
    }

    // FIXED: Extract bill summary using MainActivity's merged data
    private static BillSummaryData extractBillSummary(Map<String, Object> result) {
        List<KeyValuePair> fields = new ArrayList<>();

        try {
            Map<String, Object> mergedData = mergeSERVERData(result);
            
            if (mergedData != null && mergedData.containsKey("bill_summary")) {
                Map<String, Object> billSummary = (Map<String, Object>) mergedData.get("bill_summary");
                
                addIfValid(fields, "Total Bills", String.valueOf(billSummary.get("total_bills")));
                addIfValid(fields, "Latest Bill Date", getSafeString(billSummary.get("latest_bill_date")));
                addIfValid(fields, "Latest Consumption", getSafeString(billSummary.get("latest_consumption")) + " kWh");
                addIfValid(fields, "Latest Amount", formatCurrency(getSafeString(billSummary.get("latest_total_amount"))));
                
                // Calculate first and last bill periods
                if (mergedData.containsKey("bill_info_raw")) {
                    JSONArray billInfo = (JSONArray) mergedData.get("bill_info_raw");
                    if (billInfo.length() > 0) {
                        JSONObject firstBill = billInfo.getJSONObject(billInfo.length() - 1);
                        JSONObject lastBill = billInfo.getJSONObject(0);
                        
                        addIfValid(fields, "First Bill Period", formatBillMonth(firstBill.optString("BILL_MONTH")));
                        addIfValid(fields, "Last Bill Period", formatBillMonth(lastBill.optString("BILL_MONTH")));
                        
                        // Calculate totals
                        double totalAmount = 0;
                        double totalPaid = 0;
                        for (int i = 0; i < billInfo.length(); i++) {
                            JSONObject bill = billInfo.getJSONObject(i);
                            totalAmount += bill.optDouble("TOTAL_BILL", 0);
                            totalPaid += bill.optDouble("PAID_AMT", 0);
                        }
                        double arrears = totalAmount - totalPaid;
                        
                        addIfValid(fields, "Total Amount", formatCurrency(String.valueOf(totalAmount)));
                        addIfValid(fields, "Total Paid", formatCurrency(String.valueOf(totalPaid)));
                        addIfValid(fields, "Arrears", formatCurrency(String.valueOf(arrears)));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fields.isEmpty() ? null : new BillSummaryData(fields);
    }

    // FIXED: Extract error
    private static ErrorData extractError(Map<String, Object> result) {
        if (result.containsKey("error")) {
            return new ErrorData(result.get("error").toString());
        }
        return null;
    }

    // ========== CRITICAL: Replicate MainActivity's data processing methods ==========

    // Replicate MainActivity's mergeSERVERData method
    private static Map<String, Object> mergeSERVERData(Map<String, Object> result) {
        try {
            JSONObject SERVER3Data = (JSONObject) result.get("SERVER3_data");
            Object SERVER2DataObj = result.get("SERVER2_data");
            JSONObject SERVER2Data = null;

            if (SERVER2DataObj instanceof JSONObject) {
                SERVER2Data = (JSONObject) SERVER2DataObj;
            }

            if (SERVER3Data == null && (SERVER2Data == null)) {
                return null;
            }

            Map<String, Object> merged = new HashMap<>();

            // Process SERVER2 data first
            if (SERVER2Data != null) {
                Map<String, Object> cleanedSERVER2 = cleanSERVER2Data(SERVER2Data);
                
                // Preserve bill information
                if (cleanedSERVER2.containsKey("bill_info_raw")) {
                    merged.put("bill_info_raw", cleanedSERVER2.get("bill_info_raw"));
                }
                if (cleanedSERVER2.containsKey("bill_summary")) {
                    merged.put("bill_summary", cleanedSERVER2.get("bill_summary"));
                }
                
                // Customer info
                if (cleanedSERVER2.containsKey("customer_info")) {
                    merged.put("customer_info", cleanedSERVER2.get("customer_info"));
                }
                
                // Balance info
                if (cleanedSERVER2.containsKey("balance_info")) {
                    merged.put("balance_info", cleanedSERVER2.get("balance_info"));
                }
            }

            // Supplement with SERVER3 data
            if (SERVER3Data != null) {
                Map<String, Object> cleanedSERVER3 = cleanSERVER3Data(SERVER3Data);
                
                if (!merged.containsKey("customer_info") && cleanedSERVER3.containsKey("customer_info")) {
                    merged.put("customer_info", cleanedSERVER3.get("customer_info"));
                }
                
                if (!merged.containsKey("balance_info") && cleanedSERVER3.containsKey("balance_info")) {
                    merged.put("balance_info", cleanedSERVER3.get("balance_info"));
                }
            }

            return removeEmptyFields(merged);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Replicate MainActivity's cleanSERVER1Data method
    private static Map<String, Object> cleanSERVER1Data(Object SERVER1DataObj) {
        Map<String, Object> cleaned = new HashMap<>();

        try {
            String responseBody;
            if (SERVER1DataObj instanceof String) {
                responseBody = (String) SERVER1DataObj;
            } else {
                responseBody = SERVER1DataObj.toString();
            }

            // Extract the actual JSON part
            String jsonPart = extractActualJson(responseBody);
            JSONObject SERVER1Data = new JSONObject(jsonPart);

            // Process mCustomerData
            if (SERVER1Data.has("mCustomerData")) {
                JSONObject mCustomerData = SERVER1Data.getJSONObject("mCustomerData");
                if (mCustomerData.has("result")) {
                    JSONObject result = mCustomerData.getJSONObject("result");

                    Map<String, String> customerInfo = new HashMap<>();
                    customerInfo.put("Name", extractDirectValue(result, "customerName"));
                    customerInfo.put("Address", extractDirectValue(result, "customerAddress"));
                    customerInfo.put("Phone", extractDirectValue(result, "customerPhone"));
                    customerInfo.put("Division", extractDirectValue(result, "division"));
                    customerInfo.put("Sub Division", extractDirectValue(result, "sndDivision"));
                    customerInfo.put("Tariff Category", extractDirectValue(result, "tariffCategory"));
                    customerInfo.put("Connection Category", extractDirectValue(result, "connectionCategory"));
                    customerInfo.put("Account Type", extractDirectValue(result, "accountType"));
                    customerInfo.put("Meter Type", extractDirectValue(result, "meterType"));
                    customerInfo.put("Sanctioned Load", extractDirectValue(result, "sanctionLoad"));
                    customerInfo.put("Last Recharge Amount", extractDirectValue(result, "lastRechargeAmount"));
                    customerInfo.put("Last Recharge Time", extractDirectValue(result, "lastRechargeTime"));
                    customerInfo.put("Installation Date", extractDirectValue(result, "installationDate"));
                    customerInfo.put("Lock Status", extractDirectValue(result, "lockStatus"));
                    customerInfo.put("Total Recharge This Month", extractDirectValue(result, "totalRechargeThisMonth"));

                    cleaned.put("customer_info", removeEmptyFields(customerInfo));
                }
            }

            // Process transactions
            if (SERVER1Data.has("mOrderData")) {
                List<Map<String, String>> transactions = extractTransactionsWithExactPatterns(responseBody);
                if (!transactions.isEmpty()) {
                    cleaned.put("recent_transactions", transactions);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return removeEmptyFields(cleaned);
    }

    // Replicate MainActivity's cleanSERVER2Data method
    private static Map<String, Object> cleanSERVER2Data(JSONObject SERVER2Data) {
        Map<String, Object> cleaned = new HashMap<>();

        try {
            // Extract Customer Information
            if (SERVER2Data.has("customerInfo") && SERVER2Data.getJSONArray("customerInfo").length() > 0) {
                JSONArray customerInfoArray = SERVER2Data.getJSONArray("customerInfo");
                if (customerInfoArray.length() > 0 && customerInfoArray.getJSONArray(0).length() > 0) {
                    JSONObject firstCustomer = customerInfoArray.getJSONArray(0).getJSONObject(0);
                    Map<String, String> customerInfo = new HashMap<>();

                    customerInfo.put("Customer Name", firstCustomer.optString("CUSTOMER_NAME"));
                    customerInfo.put("Address", firstCustomer.optString("ADDRESS"));
                    customerInfo.put("Tariff Description", firstCustomer.optString("TARIFF"));
                    customerInfo.put("Location Code", firstCustomer.optString("LOCATION_CODE"));
                    customerInfo.put("Bill Group", firstCustomer.optString("BILL_GROUP"));
                    customerInfo.put("Book Number", firstCustomer.optString("BOOK"));
                    customerInfo.put("Walk Order", firstCustomer.optString("WALKING_SEQUENCE"));
                    customerInfo.put("Meter Number", firstCustomer.optString("METER_NUM"));
                    customerInfo.put("Meter Status", getMeterStatus(firstCustomer.optString("METER_STATUS")));
                    customerInfo.put("Connection Date", formatDate(firstCustomer.optString("METER_CONNECT_DATE")));
                    customerInfo.put("Description", firstCustomer.optString("DESCR"));
                    customerInfo.put("Account_Number", firstCustomer.optString("CONS_EXTG_NUM"));
                    customerInfo.put("Usage Type", firstCustomer.optString("USAGE_TYPE"));
                    customerInfo.put("Start Bill Cycle", firstCustomer.optString("START_BILL_CYCLE"));

                    cleaned.put("customer_info", removeEmptyFields(customerInfo));
                }
            }

            // Extract Balance Information
            Map<String, String> balanceInfo = new HashMap<>();
            if (SERVER2Data.has("finalBalanceInfo")) {
                String balanceString = SERVER2Data.optString("finalBalanceInfo");
                if (balanceString != null && !balanceString.equals("null") && !balanceString.isEmpty()) {
                    balanceInfo = parseFinalBalanceInfo(balanceString);
                }
            }
            
            if (!balanceInfo.isEmpty()) {
                cleaned.put("balance_info", balanceInfo);
            }

            // Extract Bill Information
            if (SERVER2Data.has("billInfo")) {
                JSONArray billInfo = SERVER2Data.getJSONArray("billInfo");
                cleaned.put("bill_info_raw", billInfo);

                // Create bill summary
                Map<String, Object> billSummary = new HashMap<>();
                billSummary.put("total_bills", billInfo.length());

                if (billInfo.length() > 0) {
                    JSONObject latestBill = billInfo.getJSONObject(0);
                    billSummary.put("latest_bill_date", formatBillMonth(latestBill.optString("BILL_MONTH")));
                    billSummary.put("latest_bill_number", latestBill.optString("BILL_NO"));
                    billSummary.put("latest_consumption", latestBill.optDouble("CONS_KWH_SR", 0));
                    billSummary.put("latest_total_amount", latestBill.optDouble("TOTAL_BILL", 0));
                    billSummary.put("latest_balance", latestBill.optDouble("BALANCE", 0));
                }

                cleaned.put("bill_summary", billSummary);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return removeEmptyFields(cleaned);
    }

    // Replicate MainActivity's cleanSERVER3Data method
    private static Map<String, Object> cleanSERVER3Data(JSONObject SERVER3Data) {
        Map<String, Object> cleaned = new HashMap<>();
        Map<String, String> customerInfo = new HashMap<>();

        customerInfo.put("Customer Name", SERVER3Data.optString("customerName"));
        customerInfo.put("Customer Address", SERVER3Data.optString("customerAddr"));
        customerInfo.put("Father Name", SERVER3Data.optString("fatherName"));
        customerInfo.put("Location Code", SERVER3Data.optString("locationCode"));
        customerInfo.put("Area Code", SERVER3Data.optString("areaCode"));
        customerInfo.put("Book Number", SERVER3Data.optString("bookNumber"));
        customerInfo.put("Bill Group", SERVER3Data.optString("billGroup"));
        customerInfo.put("Meter Number", SERVER3Data.optString("meterNum"));
        customerInfo.put("Meter Condition", SERVER3Data.optString("meterConditionDesc"));
        customerInfo.put("Sanctioned Load", SERVER3Data.optString("sanctionedLoad"));
        customerInfo.put("Tariff Description", SERVER3Data.optString("tariffDesc"));
        customerInfo.put("Walk Order", SERVER3Data.optString("walkOrder"));
        customerInfo.put("Arrear Amount", SERVER3Data.optString("arrearAmount"));
        customerInfo.put("Last Bill Reading SR", SERVER3Data.optString("lastBillReadingSr"));
        customerInfo.put("Last Bill Reading OF PK", SERVER3Data.optString("lastBillReadingOfPk"));
        customerInfo.put("Last Bill Reading PK", SERVER3Data.optString("lastBillReadingPk"));

        cleaned.put("customer_info", removeEmptyFields(customerInfo));

        if (SERVER3Data.has("arrearAmount")) {
            String arrearAmount = SERVER3Data.optString("arrearAmount");
            Map<String, String> balanceInfo = new HashMap<>();
            balanceInfo.put("Total Balance", arrearAmount);
            balanceInfo.put("Arrear Amount", arrearAmount);
            cleaned.put("balance_info", balanceInfo);
        }

        return removeEmptyFields(cleaned);
    }

    // ========== HELPER METHODS ==========

    private static void extractBasicInfoDirectly(Map<String, Object> result, List<KeyValuePair> fields) {
        addIfValid(fields, "Customer Number", getSafeString(result.get("customer_number")));
        addIfValid(fields, "Meter Number", getSafeString(result.get("meter_number")));
        addIfValid(fields, "Consumer Number", getSafeString(result.get("consumer_number")));
    }

    private static String extractActualJson(String responseBody) {
        try {
            int jsonStart = responseBody.indexOf("1:{");
            if (jsonStart != -1) {
                return responseBody.substring(jsonStart + 2);
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    private static List<Map<String, String>> extractTransactionsWithExactPatterns(String response) {
        List<Map<String, String>> transactions = new ArrayList<>();
        int index = 0;
        int count = 0;

        while (index != -1 && count < 3) {
            index = response.indexOf("\"tokens\":{\"_text\":\"", index);
            if (index == -1) break;

            int valueStart = index + "\"tokens\":{\"_text\":\"".length();
            int valueEnd = response.indexOf("\"", valueStart);

            if (valueEnd != -1) {
                String token = response.substring(valueStart, valueEnd);
                Map<String, String> transaction = extractTransactionFields(response, index);
                transaction.put("Tokens", token);
                transactions.add(transaction);
                count++;
            }

            index = valueEnd + 1;
        }

        return transactions;
    }

    private static Map<String, String> extractTransactionFields(String response, int tokenPosition) {
        Map<String, String> transaction = new HashMap<>();
        int searchStart = Math.max(0, tokenPosition - 1000);
        int searchEnd = Math.min(response.length(), tokenPosition + 200);
        String searchArea = response.substring(searchStart, searchEnd);

        transaction.put("Date", extractExactValue(searchArea, "date"));
        transaction.put("Order Number", extractExactValue(searchArea, "orderNo"));
        transaction.put("Amount", "৳" + extractExactValue(searchArea, "grossAmount"));
        transaction.put("Energy Cost", "৳" + extractExactValue(searchArea, "energyCost"));
        transaction.put("Operator", extractExactValue(searchArea, "operator"));
        transaction.put("Sequence", extractExactValue(searchArea, "sequence"));

        return transaction;
    }

    private static String extractExactValue(String text, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\":{\"_text\":\"";
            int start = text.indexOf(pattern);
            if (start != -1) {
                int valueStart = start + pattern.length();
                int valueEnd = text.indexOf("\"", valueStart);
                if (valueEnd != -1) {
                    return text.substring(valueStart, valueEnd);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "N/A";
    }

    private static String extractDirectValue(JSONObject jsonObject, String key) {
        try {
            if (!jsonObject.has(key)) {
                return "N/A";
            }

            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                JSONObject obj = (JSONObject) value;
                if (obj.has("_text")) {
                    String textValue = obj.getString("_text");
                    return (textValue == null || textValue.isEmpty() || textValue.equals("{}")) ? "N/A" : textValue.trim();
                }
                return "N/A";
            }

            String stringValue = value.toString().trim();
            return (stringValue.isEmpty() || stringValue.equals("{}")) ? "N/A" : stringValue;

        } catch (Exception e) {
            return "N/A";
        }
    }

    private static Map<String, String> parseFinalBalanceInfo(String balanceString) {
        Map<String, String> balanceInfo = new HashMap<>();

        if (balanceString == null || balanceString.isEmpty() || balanceString.equals("null")) {
            return balanceInfo;
        }

        try {
            if (!balanceString.contains(",") && !balanceString.contains(":")) {
                balanceInfo.put("Total Balance", balanceString.trim());
                balanceInfo.put("Arrear Amount", balanceString.trim());
                return balanceInfo;
            }

            String[] parts = balanceString.split(",");
            String totalBalance = parts[0].trim();
            balanceInfo.put("Total Balance", totalBalance);
            balanceInfo.put("Arrear Amount", totalBalance);

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.startsWith("PRN:")) {
                    balanceInfo.put("PRN", part.substring(4).trim());
                } else if (part.startsWith("LPS:")) {
                    balanceInfo.put("LPS", part.substring(4).trim());
                } else if (part.startsWith("VAT:")) {
                    balanceInfo.put("VAT", part.substring(4).trim());
                }
            }

        } catch (Exception e) {
            balanceInfo.put("Total Balance", balanceString);
            balanceInfo.put("Arrear Amount", balanceString);
        }

        return balanceInfo;
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

    @SuppressWarnings("unchecked")
    private static <T> T removeEmptyFields(T data) {
        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            Map<Object, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                if (key.toString().contains("bill") || key.toString().contains("balance")) {
                    result.put(key, value);
                    continue;
                }

                Object cleaned = removeEmptyFields(value);
                if (cleaned != null &&
                        !cleaned.equals("") &&
                        !cleaned.equals("N/A") &&
                        !(cleaned instanceof Map && ((Map<?, ?>) cleaned).isEmpty()) &&
                        !(cleaned instanceof List && ((List<?>) cleaned).isEmpty())) {
                    result.put(key, cleaned);
                }
            }
            return (T) result;
        } else if (data instanceof List) {
            List<?> list = (List<?>) data;
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                Object cleaned = removeEmptyFields(item);
                if (cleaned != null &&
                        !cleaned.equals("") &&
                        !cleaned.equals("N/A") &&
                        !(cleaned instanceof Map && ((Map<?, ?>) cleaned).isEmpty()) &&
                        !(cleaned instanceof List && ((List<?>) cleaned).isEmpty())) {
                    result.add(cleaned);
                }
            }
            return (T) result;
        } else {
            return data;
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
    // They should work fine with the fixed data extraction above

    private static String replacePostpaidPlaceholders(String template, PostpaidData data) {
        // Your existing implementation
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
        // Your existing implementation
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