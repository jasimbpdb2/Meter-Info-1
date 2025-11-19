package customerinfo.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class MeterDataHTMLHelper {

    public MeterDataHTMLHelper() {
        // Empty constructor
    }

    /**
     * Main method to process meter data and return structured objects for HTML display
     */
    public Map<String, Object> processMeterDataForHTML(String inputNumber, String type, String subType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("🔍 METER DATA HELPER: Processing " + type + " data for: " + inputNumber);

            if ("prepaid".equals(type)) {
                result = processPrepaidData(inputNumber);
            } else {
                result = processPostpaidData(inputNumber, subType);
            }

            // Add metadata
            result.put("search_type", type);
            result.put("search_input", inputNumber);
            result.put("timestamp", new Date().toString());

        } catch (Exception e) {
            System.out.println("❌ METER DATA HELPER ERROR: " + e.getMessage());
            result.put("error", "Data processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process prepaid meter data into structured objects
     */
    private Map<String, Object> processPrepaidData(String meterNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Step 1: Get basic meter info from SERVER1 - USE THE SAME METHOD AS MainActivity
            Map<String, Object> server1Result = MainActivity.SERVER1Lookup(meterNumber);
            System.out.println("🔍 PREPAID: SERVER1 result - " + (server1Result.containsKey("error") ? "ERROR: " + server1Result.get("error") : "SUCCESS"));
            
            String consumerNumber = (String) server1Result.get("consumer_number");

            if (consumerNumber == null || server1Result.containsKey("error")) {
                String errorMsg = server1Result.containsKey("error") ? 
                    server1Result.get("error").toString() : "Invalid meter number or data not found";
                result.put("error", errorMsg);
                return result;
            }

            System.out.println("✅ PREPAID: Found consumer number: " + consumerNumber);

            // Step 2: Get detailed customer info from SERVER3 - USE THE SAME METHOD AS MainActivity
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(consumerNumber);
            System.out.println("🔍 PREPAID: SERVER3 result - " + (server3Result.containsKey("error") ? "ERROR: " + server3Result.get("error") : "SUCCESS"));

            if (server3Result.containsKey("error")) {
                result.put("error", "Customer data not found: " + server3Result.get("error"));
                return result;
            }

            // Step 3: Extract and structure the data
            extractPrepaidCustomerInfo(server1Result, server3Result, result, meterNumber);
            extractPrepaidBalanceInfo(server3Result, result);
            extractPrepaidTransactions(server1Result, result);

        } catch (Exception e) {
            System.out.println("❌ PREPAID DATA PROCESSING ERROR: " + e.getMessage());
            result.put("error", "Prepaid data processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process postpaid data into structured objects
     */
    private Map<String, Object> processPostpaidData(String inputNumber, String subType) {
        Map<String, Object> result = new HashMap<>();

        try {
            if ("meter_no".equals(subType)) {
                // Handle meter number lookup (multiple customers possible)
                result = processPostpaidMeterLookup(inputNumber);
            } else {
                // Handle consumer number lookup (single customer)
                result = processPostpaidConsumerLookup(inputNumber);
            }
        } catch (Exception e) {
            System.out.println("❌ POSTPAID DATA PROCESSING ERROR: " + e.getMessage());
            result.put("error", "Postpaid data processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process postpaid consumer number lookup
     */
    private Map<String, Object> processPostpaidConsumerLookup(String consumerNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🔍 POSTPAID: Looking up consumer: " + consumerNumber);
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(consumerNumber);
            System.out.println("🔍 POSTPAID: SERVER3 result - " + (server3Result.containsKey("error") ? "ERROR: " + server3Result.get("error") : "SUCCESS"));

            if (server3Result.containsKey("error")) {
                result.put("error", "Customer data not found: " + server3Result.get("error"));
                return result;
            }

            extractPostpaidCustomerInfo(server3Result, result, consumerNumber);
            extractPostpaidBalanceInfo(server3Result, result);
            extractPostpaidBillInfo(server3Result, result);

        } catch (Exception e) {
            System.out.println("❌ POSTPAID CONSUMER LOOKUP ERROR: " + e.getMessage());
            result.put("error", "Postpaid consumer lookup failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Process postpaid meter number lookup (multiple customers)
     */
    private Map<String, Object> processPostpaidMeterLookup(String meterNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🔍 POSTPAID METER: Looking up meter: " + meterNumber);
            // Get customer numbers from meter
            Map<String, Object> meterResult = MainActivity.getCustomerNumbersByMeter(meterNumber);
            System.out.println("🔍 POSTPAID METER: Meter lookup result - " + (meterResult.containsKey("error") ? "ERROR: " + meterResult.get("error") : "SUCCESS"));

            if (meterResult.containsKey("error")) {
                result.put("error", meterResult.get("error"));
                return result;
            }

            List<String> customerNumbers = (List<String>) meterResult.get("customer_numbers");
            List<Map<String, Object>> customerResults = new ArrayList<>();

            System.out.println("🔄 POSTPAID METER: Processing " + customerNumbers.size() + " customer(s)");

            // Process each customer
            for (String custNum : customerNumbers) {
                System.out.println("🔄 POSTPAID METER: Processing customer: " + custNum);
                Map<String, Object> customerResult = processPostpaidConsumerLookup(custNum);
                customerResults.add(customerResult);
            }

            result.put("meter_number", meterNumber);
            result.put("customer_count", customerNumbers.size());
            result.put("customers", customerResults);
            result.put("is_multi_customer", true);

        } catch (Exception e) {
            System.out.println("❌ POSTPAID METER LOOKUP ERROR: " + e.getMessage());
            result.put("error", "Postpaid meter lookup failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Extract prepaid customer information into structured objects
     */
    private void extractPrepaidCustomerInfo(Map<String, Object> server1Result, 
                                          Map<String, Object> server3Result,
                                          Map<String, Object> result, 
                                          String meterNumber) {
        Map<String, String> customerInfo = new HashMap<>();

        try {
            // Extract from SERVER1 first
            Object server1DataObj = server1Result.get("SERVER1_data");
            if (server1DataObj instanceof String) {
                extractCustomerInfoFromSERVER1((String) server1DataObj, customerInfo);
            }

            // Supplement with SERVER3 data if available
            JSONObject server3Data = (JSONObject) server3Result.get("SERVER3_data");
            if (server3Data != null) {
                supplementWithSERVER3Data(server3Data, customerInfo);
            }

            // Ensure meter number is set
            customerInfo.put("meter_number", meterNumber);
            customerInfo.put("consumer_number", (String) server1Result.get("consumer_number"));

            result.put("customer_info", customerInfo);
            System.out.println("✅ PREPAID: Customer info extracted with " + customerInfo.size() + " fields");

        } catch (Exception e) {
            System.out.println("❌ Error extracting prepaid customer info: " + e.getMessage());
        }
    }

    /**
     * Extract customer info from SERVER1 response
     */
    private void extractCustomerInfoFromSERVER1(String responseBody, Map<String, String> customerInfo) {
        try {
            String jsonPart = extractActualJson(responseBody);
            JSONObject SERVER1Data = new JSONObject(jsonPart);

            if (SERVER1Data.has("mCustomerData")) {
                JSONObject mCustomerData = SERVER1Data.getJSONObject("mCustomerData");
                if (mCustomerData.has("result")) {
                    JSONObject result = mCustomerData.getJSONObject("result");

                    customerInfo.put("customer_name", extractDirectValue(result, "customerName"));
                    customerInfo.put("address", extractDirectValue(result, "customerAddress"));
                    customerInfo.put("mobile_no", extractDirectValue(result, "customerPhone"));
                    customerInfo.put("account_type", extractDirectValue(result, "accountType"));
                    customerInfo.put("tariff_category", extractDirectValue(result, "tariffCategory"));
                    customerInfo.put("sanctioned_load", extractDirectValue(result, "sanctionLoad"));
                    customerInfo.put("division", extractDirectValue(result, "division"));
                    customerInfo.put("sub_division", extractDirectValue(result, "sndDivision"));
                    
                    System.out.println("✅ SERVER1: Extracted customer info");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting from SERVER1: " + e.getMessage());
        }
    }

    /**
     * Supplement customer info with SERVER3 data
     */
    private void supplementWithSERVER3Data(JSONObject server3Data, Map<String, String> customerInfo) {
        try {
            if (customerInfo.get("customer_name") == null || customerInfo.get("customer_name").isEmpty()) {
                customerInfo.put("customer_name", server3Data.optString("customerName", ""));
            }
            if (customerInfo.get("address") == null || customerInfo.get("address").isEmpty()) {
                customerInfo.put("address", server3Data.optString("customerAddr", ""));
            }
            if (customerInfo.get("father_name") == null || customerInfo.get("father_name").isEmpty()) {
                customerInfo.put("father_name", server3Data.optString("fatherName", ""));
            }

            // Additional SERVER3 fields
            customerInfo.put("location_code", server3Data.optString("locationCode", ""));
            customerInfo.put("area_code", server3Data.optString("areaCode", ""));
            customerInfo.put("bill_group", server3Data.optString("billGroup", ""));
            customerInfo.put("book_number", server3Data.optString("bookNumber", ""));
            customerInfo.put("tariff_description", server3Data.optString("tariffDesc", ""));
            customerInfo.put("walk_order", server3Data.optString("walkOrder", ""));

            System.out.println("✅ SERVER3: Supplemented customer info");

        } catch (Exception e) {
            System.out.println("❌ Error supplementing with SERVER3 data: " + e.getMessage());
        }
    }

    /**
     * Extract postpaid customer information
     */
    private void extractPostpaidCustomerInfo(Map<String, Object> server3Result, 
                                           Map<String, Object> result, 
                                           String consumerNumber) {
        Map<String, String> customerInfo = new HashMap<>();

        try {
            JSONObject server3Data = (JSONObject) server3Result.get("SERVER3_data");
            Object server2DataObj = server3Result.get("SERVER2_data");

            if (server3Data != null) {
                customerInfo.put("customer_name", server3Data.optString("customerName", ""));
                customerInfo.put("father_name", server3Data.optString("fatherName", ""));
                customerInfo.put("address", server3Data.optString("customerAddr", ""));
                customerInfo.put("consumer_number", consumerNumber);
                customerInfo.put("meter_number", server3Data.optString("meterNum", ""));
                customerInfo.put("location_code", server3Data.optString("locationCode", ""));
                customerInfo.put("area_code", server3Data.optString("areaCode", ""));
                customerInfo.put("bill_group", server3Data.optString("billGroup", ""));
                customerInfo.put("book_number", server3Data.optString("bookNumber", ""));
                customerInfo.put("tariff_description", server3Data.optString("tariffDesc", ""));
                customerInfo.put("sanctioned_load", server3Data.optString("sanctionedLoad", ""));
                customerInfo.put("walk_order", server3Data.optString("walkOrder", ""));
                customerInfo.put("meter_condition", server3Data.optString("meterConditionDesc", ""));
            }

            // Supplement with SERVER2 data if available
            if (server2DataObj instanceof JSONObject) {
                supplementWithSERVER2Data((JSONObject) server2DataObj, customerInfo);
            }

            result.put("customer_info", customerInfo);
            System.out.println("✅ POSTPAID: Customer info extracted with " + customerInfo.size() + " fields");

        } catch (Exception e) {
            System.out.println("❌ Error extracting postpaid customer info: " + e.getMessage());
        }
    }

    /**
     * Supplement with SERVER2 customer data
     */
    private void supplementWithSERVER2Data(JSONObject server2Data, Map<String, String> customerInfo) {
        try {
            if (server2Data.has("customerInfo")) {
                JSONArray customerInfoArray = server2Data.getJSONArray("customerInfo");
                if (customerInfoArray.length() > 0) {
                    JSONArray firstArray = customerInfoArray.getJSONArray(0);
                    if (firstArray.length() > 0) {
                        JSONObject firstCustomer = firstArray.getJSONObject(0);

                        if (customerInfo.get("customer_name") == null || customerInfo.get("customer_name").isEmpty()) {
                            customerInfo.put("customer_name", firstCustomer.optString("CUSTOMER_NAME", ""));
                        }
                        if (customerInfo.get("address") == null || customerInfo.get("address").isEmpty()) {
                            customerInfo.put("address", firstCustomer.optString("ADDRESS", ""));
                        }
                        if (customerInfo.get("meter_number") == null || customerInfo.get("meter_number").isEmpty()) {
                            customerInfo.put("meter_number", firstCustomer.optString("METER_NUM", ""));
                        }

                        // Additional SERVER2 fields
                        customerInfo.put("meter_status", getMeterStatus(firstCustomer.optString("METER_STATUS")));
                        customerInfo.put("connection_date", formatDate(firstCustomer.optString("METER_CONNECT_DATE")));
                        customerInfo.put("usage_type", firstCustomer.optString("USAGE_TYPE", ""));
                        customerInfo.put("description", firstCustomer.optString("DESCR", ""));
                        
                        System.out.println("✅ SERVER2: Supplemented customer info");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error supplementing with SERVER2 data: " + e.getMessage());
        }
    }

    /**
     * Extract prepaid balance information
     */
    private void extractPrepaidBalanceInfo(Map<String, Object> server3Result, Map<String, Object> result) {
        Map<String, String> balanceInfo = new HashMap<>();

        try {
            JSONObject server3Data = (JSONObject) server3Result.get("SERVER3_data");
            Object server2DataObj = server3Result.get("SERVER2_data");

            // Try SERVER2 first for detailed balance
            if (server2DataObj instanceof JSONObject) {
                JSONObject server2Data = (JSONObject) server2DataObj;
                extractBalanceFromSERVER2(server2Data, balanceInfo);
            }

            // Fallback to SERVER3
            if (balanceInfo.isEmpty() && server3Data != null && server3Data.has("arrearAmount")) {
                String arrear = server3Data.optString("arrearAmount");
                if (isValidValue(arrear)) {
                    balanceInfo.put("arrear_amount", arrear);
                    balanceInfo.put("total_balance", arrear);
                }
            }

            result.put("balance_info", balanceInfo);
            System.out.println("✅ PREPAID: Balance info extracted");

        } catch (Exception e) {
            System.out.println("❌ Error extracting prepaid balance info: " + e.getMessage());
        }
    }

    /**
     * Extract postpaid balance information
     */
    private void extractPostpaidBalanceInfo(Map<String, Object> server3Result, Map<String, Object> result) {
        Map<String, String> balanceInfo = new HashMap<>();

        try {
            Object server2DataObj = server3Result.get("SERVER2_data");

            if (server2DataObj instanceof JSONObject) {
                JSONObject server2Data = (JSONObject) server2DataObj;
                extractBalanceFromSERVER2(server2Data, balanceInfo);
            }

            // SERVER3 fallback
            JSONObject server3Data = (JSONObject) server3Result.get("SERVER3_data");
            if (balanceInfo.isEmpty() && server3Data != null && server3Data.has("arrearAmount")) {
                String arrear = server3Data.optString("arrearAmount");
                if (isValidValue(arrear)) {
                    balanceInfo.put("arrear_amount", arrear);
                    balanceInfo.put("total_balance", arrear);
                }
            }

            result.put("balance_info", balanceInfo);
            System.out.println("✅ POSTPAID: Balance info extracted");

        } catch (Exception e) {
            System.out.println("❌ Error extracting postpaid balance info: " + e.getMessage());
        }
    }

    /**
     * Extract balance information from SERVER2
     */
    private void extractBalanceFromSERVER2(JSONObject server2Data, Map<String, String> balanceInfo) {
        try {
            // Method 1: finalBalanceInfo
            if (server2Data.has("finalBalanceInfo")) {
                String balanceString = server2Data.optString("finalBalanceInfo");
                if (isValidValue(balanceString)) {
                    Map<String, String> parsedBalance = parseFinalBalanceInfo(balanceString);
                    balanceInfo.putAll(parsedBalance);
                }
            }

            // Method 2: balanceInfo object
            if (balanceInfo.isEmpty() && server2Data.has("balanceInfo")) {
                JSONObject balanceInfoObj = server2Data.getJSONObject("balanceInfo");
                if (balanceInfoObj.has("Result") && balanceInfoObj.getJSONArray("Result").length() > 0) {
                    JSONObject firstBalance = balanceInfoObj.getJSONArray("Result").getJSONObject(0);

                    double totalBalance = firstBalance.optDouble("BALANCE", 0);
                    double currentBill = firstBalance.optDouble("CURRENT_BILL", 0);
                    double arrearBill = firstBalance.optDouble("ARREAR_BILL", 0);
                    double paidAmount = firstBalance.optDouble("PAID_AMT", 0);

                    if (totalBalance > 0) {
                        balanceInfo.put("total_balance", String.format("%.2f", totalBalance));
                        balanceInfo.put("current_bill", String.format("%.2f", currentBill));
                        balanceInfo.put("arrear_amount", String.format("%.2f", arrearBill));
                        balanceInfo.put("paid_amount", String.format("%.2f", paidAmount));
                    }
                }
            }

            System.out.println("✅ SERVER2: Balance info extracted");

        } catch (Exception e) {
            System.out.println("❌ Error extracting balance from SERVER2: " + e.getMessage());
        }
    }

    /**
     * Extract prepaid transaction history
     */
    private void extractPrepaidTransactions(Map<String, Object> server1Result, Map<String, Object> result) {
        List<Map<String, String>> transactions = new ArrayList<>();

        try {
            Object server1DataObj = server1Result.get("SERVER1_data");
            if (server1DataObj instanceof String) {
                transactions = extractRechargeTransactions((String) server1DataObj);
            }

            result.put("transactions", transactions);
            result.put("transaction_count", transactions.size());
            System.out.println("✅ PREPAID: Extracted " + transactions.size() + " transactions");

        } catch (Exception e) {
            System.out.println("❌ Error extracting prepaid transactions: " + e.getMessage());
        }
    }

    /**
     * Extract postpaid bill information
     */
    private void extractPostpaidBillInfo(Map<String, Object> server3Result, Map<String, Object> result) {
        try {
            Object server2DataObj = server3Result.get("SERVER2_data");

            if (server2DataObj instanceof JSONObject) {
                JSONObject server2Data = (JSONObject) server2DataObj;
                
                if (server2Data.has("billInfo")) {
                    JSONArray billInfo = server2Data.getJSONArray("billInfo");
                    List<Map<String, Object>> bills = new ArrayList<>();

                    for (int i = 0; i < billInfo.length(); i++) {
                        JSONObject bill = billInfo.getJSONObject(i);
                        Map<String, Object> billData = new HashMap<>();

                        billData.put("bill_month", formatBillMonth(bill.optString("BILL_MONTH")));
                        billData.put("bill_number", bill.optString("BILL_NO"));
                        billData.put("consumption", bill.optDouble("CONS_KWH_SR", 0));
                        billData.put("current_bill", bill.optDouble("CURRENT_BILL", 0));
                        billData.put("total_bill", bill.optDouble("TOTAL_BILL", 0));
                        billData.put("paid_amount", bill.optDouble("PAID_AMT", 0));
                        billData.put("balance", bill.optDouble("BALANCE", 0));
                        billData.put("due_date", formatDate(bill.optString("INVOICE_DUE_DATE")));

                        bills.add(billData);
                    }

                    result.put("bill_history", bills);
                    result.put("bill_count", bills.size());

                    // Add bill summary
                    if (!bills.isEmpty()) {
                        Map<String, Object> billSummary = createBillSummary(bills);
                        result.put("bill_summary", billSummary);
                    }
                    
                    System.out.println("✅ POSTPAID: Extracted " + bills.size() + " bills");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Error extracting postpaid bill info: " + e.getMessage());
        }
    }

    /**
     * Create bill summary from bill history
     */
    private Map<String, Object> createBillSummary(List<Map<String, Object>> bills) {
        Map<String, Object> summary = new HashMap<>();

        try {
            double totalConsumption = 0;
            double totalAmount = 0;
            double totalPaid = 0;
            double currentBalance = 0;

            for (Map<String, Object> bill : bills) {
                totalConsumption += (Double) bill.getOrDefault("consumption", 0.0);
                totalAmount += (Double) bill.getOrDefault("total_bill", 0.0);
                totalPaid += (Double) bill.getOrDefault("paid_amount", 0.0);
            }

            // Current balance from the latest bill
            if (!bills.isEmpty()) {
                currentBalance = (Double) bills.get(0).getOrDefault("balance", 0.0);
            }

            summary.put("total_consumption", totalConsumption);
            summary.put("total_amount", totalAmount);
            summary.put("total_paid", totalPaid);
            summary.put("current_balance", currentBalance);
            summary.put("bill_periods", bills.size());

        } catch (Exception e) {
            System.out.println("❌ Error creating bill summary: " + e.getMessage());
        }

        return summary;
    }

    /**
     * Extract recharge transactions from SERVER1 response
     */
    private List<Map<String, String>> extractRechargeTransactions(String responseBody) {
        List<Map<String, String>> transactions = new ArrayList<>();

        try {
            int index = 0;
            int count = 0;

            while (index != -1 && count < 10) {
                index = responseBody.indexOf("\"tokens\":{\"_text\":\"", index);
                if (index == -1) break;

                int tokenStart = index + "\"tokens\":{\"_text\":\"".length();
                int tokenEnd = responseBody.indexOf("\"", tokenStart);

                if (tokenEnd != -1) {
                    Map<String, String> transaction = extractTransactionFields(responseBody, index);
                    transactions.add(transaction);
                    count++;
                }

                index = tokenEnd + 1;
            }

        } catch (Exception e) {
            System.out.println("❌ Error extracting recharge transactions: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * Extract individual transaction fields
     */
    private Map<String, String> extractTransactionFields(String response, int tokenPosition) {
        Map<String, String> transaction = new HashMap<>();

        try {
            int searchStart = Math.max(0, tokenPosition - 1000);
            int searchEnd = Math.min(response.length(), tokenPosition + 200);
            String searchArea = response.substring(searchStart, searchEnd);

            transaction.put("date", extractExactValue(searchArea, "date"));
            transaction.put("order_number", extractExactValue(searchArea, "orderNo"));
            transaction.put("amount", extractExactValue(searchArea, "grossAmount"));
            transaction.put("energy_cost", extractExactValue(searchArea, "energyCost"));
            transaction.put("operator", extractExactValue(searchArea, "operator"));
            transaction.put("sequence", extractExactValue(searchArea, "sequence"));
            transaction.put("tokens", extractExactValue(searchArea, "tokens"));

        } catch (Exception e) {
            System.out.println("❌ Error extracting transaction fields: " + e.getMessage());
        }

        return transaction;
    }

    // ==================== UTILITY METHODS ====================

    private String extractDirectValue(JSONObject jsonObject, String key) {
        try {
            if (!jsonObject.has(key)) return "";

            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                JSONObject obj = (JSONObject) value;
                if (obj.has("_text")) {
                    String textValue = obj.getString("_text");
                    return (textValue == null || textValue.isEmpty() || textValue.equals("{}")) ? "" : textValue.trim();
                }
                return "";
            }

            String stringValue = value.toString().trim();
            return (stringValue.isEmpty() || stringValue.equals("{}")) ? "" : stringValue;

        } catch (Exception e) {
            return "";
        }
    }

    private String extractActualJson(String responseBody) {
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

    private String extractExactValue(String text, String fieldName) {
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
        return "";
    }

    private Map<String, String> parseFinalBalanceInfo(String balanceString) {
        Map<String, String> balanceInfo = new HashMap<>();

        if (balanceString == null || balanceString.isEmpty() || balanceString.equals("null")) {
            return balanceInfo;
        }

        try {
            if (!balanceString.contains(",") && !balanceString.contains(":")) {
                balanceInfo.put("total_balance", balanceString.trim());
                balanceInfo.put("arrear_amount", balanceString.trim());
                return balanceInfo;
            }

            String[] parts = balanceString.split(",");
            String totalBalance = parts[0].trim();
            balanceInfo.put("total_balance", totalBalance);
            balanceInfo.put("arrear_amount", totalBalance);

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.startsWith("PRN:")) {
                    balanceInfo.put("principal", part.substring(4).trim());
                } else if (part.startsWith("LPS:")) {
                    balanceInfo.put("late_payment_surcharge", part.substring(4).trim());
                } else if (part.startsWith("VAT:")) {
                    balanceInfo.put("vat", part.substring(4).trim());
                }
            }

        } catch (Exception e) {
            balanceInfo.put("total_balance", balanceString);
        }

        return balanceInfo;
    }

    private String getMeterStatus(String statusCode) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("1", "Active");
        statusMap.put("2", "Inactive");
        statusMap.put("3", "Disconnected");
        return statusMap.getOrDefault(statusCode, "Unknown (" + statusCode + ")");
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "";
        try {
            return dateString.contains("T") ? dateString.split("T")[0] : dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private String formatBillMonth(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) return "";
        try {
            String[] parts = dateStr.substring(0, 10).split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + " " + parts[0];
                }
            }
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : dateStr;
        } catch (Exception e) {
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : dateStr;
        }
    }

    private boolean isValidValue(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return !trimmed.isEmpty() &&
               !trimmed.equals("N/A") &&
               !trimmed.equals("null") &&
               !trimmed.equals("{}") &&
               !trimmed.equals("undefined") &&
               !trimmed.equals("0");
    }
}
