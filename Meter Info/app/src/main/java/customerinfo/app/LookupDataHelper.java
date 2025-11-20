package customerinfo.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class LookupDataHelper {

    private String currentType;

    public LookupDataHelper() {
        // Empty constructor
    }

    public Map<String, Object> fetchDataForLookup(String inputNumber, String type) {
        Map<String, Object> result = new HashMap<>();
        this.currentType = type;

        try {
            System.out.println("🔍 LOOKUP DATA HELPER: Fetching " + type + " data for: " + inputNumber);

            if ("prepaid".equals(type)) {
                result = fetchPrepaidDataForLookup(inputNumber);
            } else {
                result = fetchPostpaidDataForLookup(inputNumber);
            }

        } catch (Exception e) {
            System.out.println("❌ LOOKUP DATA HELPER ERROR: " + e.getMessage());
            result.put("error", "Data fetch failed: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> fetchPrepaidDataForLookup(String meterNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Step 1: Get consumer number from SERVER1
            Map<String, Object> server1Result = MainActivity.SERVER1Lookup(meterNumber);
            String consumerNumber = (String) server1Result.get("consumer_number");

            if (consumerNumber == null || server1Result.containsKey("error")) {
                result.put("error", "মিটার নং ভুল বা ডেটা পাওয়া যায়নি");
                return result;
            }

            // Step 2: Get detailed data from SERVER3 (which includes SERVER2)
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(consumerNumber);

            if (server3Result.containsKey("error")) {
                result.put("error", "গ্রাহক তথ্য পাওয়া যায়নি: " + server3Result.get("error"));
                return result;
            }

            // Step 3: Extract and merge all data
            extractLookupData(server3Result, server1Result, result, meterNumber, "prepaid");

        } catch (Exception e) {
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> fetchPostpaidDataForLookup(String inputNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // For postpaid, directly use SERVER3 lookup
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(inputNumber);

            if (server3Result.containsKey("error")) {
                result.put("error", "গ্রাহক তথ্য পাওয়া যায়নি: " + server3Result.get("error"));
                return result;
            }

            extractLookupData(server3Result, null, result, inputNumber, "postpaid");

        } catch (Exception e) {
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private void extractLookupData(Map<String, Object> server3Result, 
                                  Map<String, Object> server1Result, 
                                  Map<String, Object> result, 
                                  String inputNumber,
                                  String type) {
        try {
            JSONObject server3Data = (JSONObject) server3Result.get("SERVER3_data");
            Object server2DataObj = server3Result.get("SERVER2_data");
            JSONObject server2Data = null;

            if (server2DataObj instanceof JSONObject) {
                server2Data = (JSONObject) server2DataObj;
            }

            // Extract customer information
            extractCustomerInfoForLookup(server3Data, server2Data, server1Result, result, inputNumber, type);
            
            // Extract balance information
            extractBalanceInfoForLookup(server3Data, server2Data, result);
            
            // Extract bill information for postpaid
            if ("postpaid".equals(type)) {
                extractBillInfoForLookup(server2Data, result);
            }
            
            // Extract recharge history for prepaid
            if ("prepaid".equals(type) && server1Result != null) {
                extractRechargeHistoryForLookup(server1Result, result);
            }

            // Extract meter information
            extractMeterInfoForLookup(server3Data, server2Data, result);

        } catch (Exception e) {
            System.out.println("❌ Error extracting lookup data: " + e.getMessage());
            result.put("error", "লুকআপ ডেটা এক্সট্রাক্ট ব্যর্থ: " + e.getMessage());
        }
    }

    private void extractCustomerInfoForLookup(JSONObject server3Data, JSONObject server2Data, 
                                            Map<String, Object> server1Result, 
                                            Map<String, Object> result, 
                                            String inputNumber, String type) {
        Map<String, String> customerInfo = new HashMap<>();

        // Basic information
        customerInfo.put("type", type);
        customerInfo.put("input_number", inputNumber);

        // Extract from SERVER3 first
        if (server3Data != null) {
            customerInfo.put("customer_name", server3Data.optString("customerName", ""));
            customerInfo.put("father_name", server3Data.optString("fatherName", ""));
            customerInfo.put("address", server3Data.optString("customerAddr", ""));
            customerInfo.put("consumer_no", server3Data.optString("customerNumber", ""));
            customerInfo.put("meter_no", server3Data.optString("meterNum", ""));
            customerInfo.put("tariff", server3Data.optString("tariffDesc", ""));
            customerInfo.put("sanctioned_load", server3Data.optString("sanctionedLoad", ""));
            customerInfo.put("location_code", server3Data.optString("locationCode", ""));
            customerInfo.put("area_code", server3Data.optString("areaCode", ""));
            customerInfo.put("bill_group", server3Data.optString("billGroup", ""));
            customerInfo.put("book_number", server3Data.optString("bookNumber", ""));
        }

        // Supplement with SERVER2 data
        if (server2Data != null) {
            try {
                if (server2Data.has("customerInfo")) {
                    JSONArray customerInfoArray = server2Data.getJSONArray("customerInfo");
                    if (customerInfoArray.length() > 0) {
                        JSONArray firstArray = customerInfoArray.getJSONArray(0);
                        if (firstArray.length() > 0) {
                            JSONObject firstCustomer = firstArray.getJSONObject(0);
                            
                            // Fill missing fields from SERVER2
                            if (customerInfo.get("customer_name").isEmpty()) {
                                customerInfo.put("customer_name", firstCustomer.optString("CUSTOMER_NAME", ""));
                            }
                            if (customerInfo.get("address").isEmpty()) {
                                customerInfo.put("address", firstCustomer.optString("ADDRESS", ""));
                            }
                            if (customerInfo.get("meter_no").isEmpty()) {
                                customerInfo.put("meter_no", firstCustomer.optString("METER_NUM", ""));
                            }
                            if (customerInfo.get("consumer_no").isEmpty()) {
                                customerInfo.put("consumer_no", firstCustomer.optString("CUSTOMER_NUMBER", ""));
                            }
                            if (customerInfo.get("tariff").isEmpty()) {
                                customerInfo.put("tariff", firstCustomer.optString("TARIFF", ""));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error extracting customer info from SERVER2: " + e.getMessage());
            }
        }

        // For prepaid, get mobile number from SERVER1
        if ("prepaid".equals(type) && server1Result != null) {
            try {
                Object server1DataObj = server1Result.get("SERVER1_data");
                if (server1DataObj instanceof String) {
                    String responseBody = (String) server1DataObj;
                    String mobileNo = extractMobileFromSERVER1(responseBody);
                    if (!mobileNo.isEmpty()) {
                        customerInfo.put("mobile_no", mobileNo);
                    }
                    
                    // Also extract additional prepaid info
                    extractPrepaidInfoFromSERVER1(responseBody, customerInfo);
                }
            } catch (Exception e) {
                System.out.println("❌ Error extracting prepaid info from SERVER1: " + e.getMessage());
            }
        }

        // Clean empty fields
        for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.equals("null") || value.isEmpty()) {
                customerInfo.put(entry.getKey(), "N/A");
            }
        }

        result.putAll(customerInfo);
    }

    private void extractBalanceInfoForLookup(JSONObject server3Data, JSONObject server2Data, Map<String, Object> result) {
        Map<String, String> balanceInfo = new HashMap<>();

        // Try SERVER2 first for detailed balance
        if (server2Data != null && server2Data.has("finalBalanceInfo")) {
            String balanceString = server2Data.optString("finalBalanceInfo");
            if (isValidValue(balanceString)) {
                balanceInfo = parseDetailedBalance(balanceString);
            }
        }

        // Fallback to SERVER2 balanceInfo
        if (balanceInfo.isEmpty() && server2Data != null && server2Data.has("balanceInfo")) {
            try {
                JSONObject balanceInfoObj = server2Data.getJSONObject("balanceInfo");
                if (balanceInfoObj.has("Result") && balanceInfoObj.getJSONArray("Result").length() > 0) {
                    JSONObject balanceResult = balanceInfoObj.getJSONArray("Result").getJSONObject(0);
                    
                    double totalBalance = balanceResult.optDouble("BALANCE", 0);
                    double currentBill = balanceResult.optDouble("CURRENT_BILL", 0);
                    double arrearBill = balanceResult.optDouble("ARREAR_BILL", 0);
                    double paidAmount = balanceResult.optDouble("PAID_AMT", 0);

                    balanceInfo.put("total_balance", String.format("%.2f", totalBalance));
                    balanceInfo.put("current_bill", String.format("%.2f", currentBill));
                    balanceInfo.put("arrear_amount", String.format("%.2f", arrearBill));
                    balanceInfo.put("paid_amount", String.format("%.2f", paidAmount));
                }
            } catch (Exception e) {
                System.out.println("❌ Error parsing balanceInfo: " + e.getMessage());
            }
        }

        // Fallback to SERVER3 arrear amount
        if (balanceInfo.isEmpty() && server3Data != null && server3Data.has("arrearAmount")) {
            String server3Arrear = server3Data.optString("arrearAmount");
            if (isValidValue(server3Arrear)) {
                balanceInfo.put("arrear_amount", server3Arrear);
                balanceInfo.put("total_balance", server3Arrear);
            }
        }

        // Clean balance info
        for (Map.Entry<String, String> entry : balanceInfo.entrySet()) {
            String value = entry.getValue();
            if (!isValidValue(value)) {
                balanceInfo.put(entry.getKey(), "0.00");
            }
        }

        result.put("balance_info", balanceInfo);
    }

    private void extractBillInfoForLookup(JSONObject server2Data, Map<String, Object> result) {
        List<Map<String, String>> billHistory = new ArrayList<>();

        try {
            if (server2Data != null && server2Data.has("billInfo")) {
                JSONArray billInfo = server2Data.getJSONArray("billInfo");
                
                for (int i = 0; i < Math.min(billInfo.length(), 12); i++) {
                    JSONObject bill = billInfo.getJSONObject(i);
                    Map<String, String> billData = new HashMap<>();
                    
                    billData.put("bill_month", formatBillMonth(bill.optString("BILL_MONTH")));
                    billData.put("bill_number", bill.optString("BILL_NO"));
                    billData.put("consumption", String.format("%.0f", bill.optDouble("CONS_KWH_SR", 0)));
                    billData.put("total_amount", String.format("%.0f", bill.optDouble("TOTAL_BILL", 0)));
                    billData.put("paid_amount", String.format("%.0f", bill.optDouble("PAID_AMT", 0)));
                    billData.put("balance", String.format("%.0f", bill.optDouble("BALANCE", 0)));
                    
                    billHistory.add(billData);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting bill info: " + e.getMessage());
        }

        result.put("bill_history", billHistory);
        result.put("total_bills", billHistory.size());
    }

    private void extractRechargeHistoryForLookup(Map<String, Object> server1Result, Map<String, Object> result) {
        List<Map<String, String>> recharges = new ArrayList<>();

        try {
            Object server1DataObj = server1Result.get("SERVER1_data");
            if (server1DataObj instanceof String) {
                String responseBody = (String) server1DataObj;
                recharges = extractRechargeTransactions(responseBody);
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting recharge history: " + e.getMessage());
        }

        result.put("recharge_history", recharges.subList(0, Math.min(recharges.size(), 10)));
        result.put("total_recharges", recharges.size());
    }

    private void extractMeterInfoForLookup(JSONObject server3Data, JSONObject server2Data, Map<String, Object> result) {
        Map<String, String> meterInfo = new HashMap<>();

        if (server3Data != null) {
            meterInfo.put("meter_condition", server3Data.optString("meterConditionDesc", ""));
            meterInfo.put("walk_order", server3Data.optString("walkOrder", ""));
            meterInfo.put("last_reading_sr", server3Data.optString("lastBillReadingSr", ""));
            meterInfo.put("last_reading_of_pk", server3Data.optString("lastBillReadingOfPk", ""));
            meterInfo.put("last_reading_pk", server3Data.optString("lastBillReadingPk", ""));
        }

        // Clean meter info
        for (Map.Entry<String, String> entry : meterInfo.entrySet()) {
            String value = entry.getValue();
            if (!isValidValue(value)) {
                meterInfo.put(entry.getKey(), "N/A");
            }
        }

        result.put("meter_info", meterInfo);
    }

    private void extractPrepaidInfoFromSERVER1(String responseBody, Map<String, String> customerInfo) {
        try {
            String jsonPart = extractActualJson(responseBody);
            JSONObject SERVER1Data = new JSONObject(jsonPart);

            if (SERVER1Data.has("mCustomerData")) {
                JSONObject mCustomerData = SERVER1Data.getJSONObject("mCustomerData");
                if (mCustomerData.has("result")) {
                    JSONObject result = mCustomerData.getJSONObject("result");

                    customerInfo.put("division", extractDirectValue(result, "division"));
                    customerInfo.put("sub_division", extractDirectValue(result, "sndDivision"));
                    customerInfo.put("tariff_category", extractDirectValue(result, "tariffCategory"));
                    customerInfo.put("connection_category", extractDirectValue(result, "connectionCategory"));
                    customerInfo.put("account_type", extractDirectValue(result, "accountType"));
                    customerInfo.put("meter_type", extractDirectValue(result, "meterType"));
                    customerInfo.put("installation_date", extractDirectValue(result, "installationDate"));
                    customerInfo.put("lock_status", extractDirectValue(result, "lockStatus"));
                    customerInfo.put("last_recharge_amount", extractDirectValue(result, "lastRechargeAmount"));
                    customerInfo.put("last_recharge_time", extractDirectValue(result, "lastRechargeTime"));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting prepaid info from SERVER1: " + e.getMessage());
        }
    }

    // ========== UTILITY METHODS ==========

    private String extractMobileFromSERVER1(String responseBody) {
        try {
            int phoneIndex = responseBody.indexOf("\"customerPhone\":{\"_text\":\"");
            if (phoneIndex != -1) {
                int valueStart = phoneIndex + "\"customerPhone\":{\"_text\":\"".length();
                int valueEnd = responseBody.indexOf("\"", valueStart);
                if (valueEnd != -1) {
                    String mobileNo = responseBody.substring(valueStart, valueEnd);
                    if (!mobileNo.isEmpty() && !mobileNo.equals("null")) {
                        return mobileNo;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting mobile from SERVER1: " + e.getMessage());
        }
        return "";
    }

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

    private Map<String, String> extractTransactionFields(String response, int tokenPosition) {
        Map<String, String> transaction = new HashMap<>();

        try {
            int searchStart = Math.max(0, tokenPosition - 1000);
            int searchEnd = Math.min(response.length(), tokenPosition + 200);
            String searchArea = response.substring(searchStart, searchEnd);

            transaction.put("date", extractExactValue(searchArea, "date"));
            transaction.put("order_no", extractExactValue(searchArea, "orderNo"));
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

    private Map<String, String> parseDetailedBalance(String balanceString) {
        Map<String, String> balanceInfo = new HashMap<>();

        try {
            if (!balanceString.contains(",") && !balanceString.contains(":")) {
                balanceInfo.put("total_balance", balanceString.trim());
                balanceInfo.put("arrear_amount", balanceString.trim());
                return balanceInfo;
            }

            String[] parts = balanceString.split(",");
            if (parts.length > 0) {
                balanceInfo.put("total_balance", parts[0].trim());
                balanceInfo.put("arrear_amount", parts[0].trim());
            }

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.startsWith("PRN:")) {
                    balanceInfo.put("principal", part.substring(4).trim());
                } else if (part.startsWith("LPS:")) {
                    balanceInfo.put("lps", part.substring(4).trim());
                } else if (part.startsWith("VAT:")) {
                    balanceInfo.put("vat", part.substring(4).trim());
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }

        return balanceInfo;
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

    private String extractDirectValue(JSONObject jsonObject, String key) {
        try {
            if (!jsonObject.has(key)) {
                return "";
            }

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

    private String formatBillMonth(String dateStr) {
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

    private boolean isValidValue(String value) {
        if (value == null) return false;

        String trimmed = value.trim();
        return !trimmed.isEmpty() &&
               !trimmed.equals("N/A") &&
               !trimmed.equals("null") &&
               !trimmed.equals("{}") &&
               !trimmed.equals("undefined") &&
               !trimmed.equals("0") &&
               !trimmed.equals("0.00");
    }
}