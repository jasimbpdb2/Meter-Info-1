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

            // Create combined result with all data
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.putAll(server3Result);
            combinedResult.put("meter_number", meterNumber);
            combinedResult.put("consumer_number", consumerNumber);
            
            // Add SERVER1 data
            if (server1Result.containsKey("SERVER1_data")) {
                combinedResult.put("SERVER1_data", server1Result.get("SERVER1_data"));
            }

            // Process data for HTML display
            result = processDataForHTML(combinedResult, "prepaid");

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

            // Create combined result
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.putAll(server3Result);
            combinedResult.put("customer_number", inputNumber);

            // Process data for HTML display
            result = processDataForHTML(combinedResult, "postpaid");

        } catch (Exception e) {
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> processDataForHTML(Map<String, Object> rawData, String type) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Basic information
            result.put("type", type);
            result.put("meter_number", rawData.getOrDefault("meter_number", "N/A"));
            result.put("consumer_number", rawData.getOrDefault("consumer_number", rawData.get("customer_number")));

            // Extract SERVER2 data for customer info, balance, and bills
            if (rawData.containsKey("SERVER2_data")) {
                JSONObject server2Data = (JSONObject) rawData.get("SERVER2_data");
                extractSERVER2Data(server2Data, result);
            }

            // Extract SERVER3 data for additional customer info
            if (rawData.containsKey("SERVER3_data")) {
                JSONObject server3Data = (JSONObject) rawData.get("SERVER3_data");
                extractSERVER3Data(server3Data, result);
            }

            // Extract SERVER1 data for prepaid (tokens and customer info)
            if ("prepaid".equals(type) && rawData.containsKey("SERVER1_data")) {
                extractSERVER1Data(rawData.get("SERVER1_data"), result);
            }

        } catch (Exception e) {
            System.out.println("❌ Error processing data for HTML: " + e.getMessage());
            result.put("error", "Data processing failed: " + e.getMessage());
        }

        return result;
    }

    private void extractSERVER2Data(JSONObject server2Data, Map<String, Object> result) {
        try {
            // Extract customer info from SERVER2
            if (server2Data.has("customerInfo") && server2Data.getJSONArray("customerInfo").length() > 0) {
                JSONArray customerInfoArray = server2Data.getJSONArray("customerInfo");
                if (customerInfoArray.length() > 0 && customerInfoArray.getJSONArray(0).length() > 0) {
                    JSONObject firstCustomer = customerInfoArray.getJSONArray(0).getJSONObject(0);
                    
                    Map<String, String> customerInfo = new HashMap<>();
                    customerInfo.put("Customer Name", firstCustomer.optString("CUSTOMER_NAME", "N/A"));
                    customerInfo.put("Address", firstCustomer.optString("ADDRESS", "N/A"));
                    customerInfo.put("Tariff", firstCustomer.optString("TARIFF", "N/A"));
                    customerInfo.put("Location Code", firstCustomer.optString("LOCATION_CODE", "N/A"));
                    customerInfo.put("Bill Group", firstCustomer.optString("BILL_GROUP", "N/A"));
                    customerInfo.put("Meter Number", firstCustomer.optString("METER_NUM", "N/A"));
                    customerInfo.put("Meter Status", getMeterStatus(firstCustomer.optString("METER_STATUS")));
                    
                    result.put("customer_info", customerInfo);
                }
            }

            // Extract balance information
            Map<String, String> balanceInfo = new HashMap<>();
            if (server2Data.has("finalBalanceInfo")) {
                String balanceString = server2Data.optString("finalBalanceInfo");
                if (balanceString != null && !balanceString.equals("null") && !balanceString.isEmpty()) {
                    balanceInfo.put("Total Balance", balanceString);
                    balanceInfo.put("Arrear Amount", balanceString);
                }
            }
            
            if (!balanceInfo.isEmpty()) {
                result.put("balance_info", balanceInfo);
            }

            // Extract bill information
            if (server2Data.has("billInfo")) {
                JSONArray billArray = server2Data.getJSONArray("billInfo");
                List<Map<String, Object>> billInfo = new ArrayList<>();
                
                for (int i = 0; i < billArray.length(); i++) {
                    JSONObject bill = billArray.getJSONObject(i);
                    Map<String, Object> billData = new HashMap<>();
                    
                    billData.put("BILL_MONTH", bill.optString("BILL_MONTH", "N/A"));
                    billData.put("BILL_NO", bill.optString("BILL_NO", "N/A"));
                    billData.put("CONS_KWH_SR", bill.optDouble("CONS_KWH_SR", 0));
                    billData.put("TOTAL_BILL", bill.optDouble("TOTAL_BILL", 0));
                    billData.put("PAID_AMT", bill.optDouble("PAID_AMT", 0));
                    billData.put("BALANCE", bill.optDouble("BALANCE", 0));
                    billData.put("INVOICE_DUE_DATE", bill.optString("INVOICE_DUE_DATE", "N/A"));
                    
                    billInfo.add(billData);
                }
                
                result.put("bill_info", billInfo);
                
                // Create bill summary
                Map<String, Object> billSummary = new HashMap<>();
                billSummary.put("total_bills", billInfo.size());
                if (!billInfo.isEmpty()) {
                    double totalAmount = 0;
                    double totalPaid = 0;
                    for (Map<String, Object> bill : billInfo) {
                        totalAmount += Double.parseDouble(String.valueOf(bill.getOrDefault("TOTAL_BILL", "0")));
                        totalPaid += Double.parseDouble(String.valueOf(bill.getOrDefault("PAID_AMT", "0")));
                    }
                    billSummary.put("total_amount", totalAmount);
                    billSummary.put("total_paid", totalPaid);
                    billSummary.put("arrears", totalAmount - totalPaid);
                }
                result.put("bill_summary", billSummary);
            }

        } catch (Exception e) {
            System.out.println("❌ Error extracting SERVER2 data: " + e.getMessage());
        }
    }

    private void extractSERVER3Data(JSONObject server3Data, Map<String, Object> result) {
        try {
            // Get or create customer info
            Map<String, String> customerInfo = (Map<String, String>) result.getOrDefault("customer_info", new HashMap<>());
            
            // Add SERVER3 customer info
            customerInfo.put("Customer Name", server3Data.optString("customerName", customerInfo.getOrDefault("Customer Name", "N/A")));
            customerInfo.put("Father Name", server3Data.optString("fatherName", "N/A"));
            customerInfo.put("Customer Address", server3Data.optString("customerAddr", customerInfo.getOrDefault("Address", "N/A")));
            customerInfo.put("Location Code", server3Data.optString("locationCode", customerInfo.getOrDefault("Location Code", "N/A")));
            customerInfo.put("Area Code", server3Data.optString("areaCode", "N/A"));
            customerInfo.put("Bill Group", server3Data.optString("billGroup", customerInfo.getOrDefault("Bill Group", "N/A")));
            customerInfo.put("Book Number", server3Data.optString("bookNumber", "N/A"));
            customerInfo.put("Tariff Description", server3Data.optString("tariffDesc", customerInfo.getOrDefault("Tariff", "N/A")));
            customerInfo.put("Sanctioned Load", server3Data.optString("sanctionedLoad", "N/A"));
            customerInfo.put("Walk Order", server3Data.optString("walkOrder", "N/A"));
            customerInfo.put("Meter Number", server3Data.optString("meterNum", customerInfo.getOrDefault("Meter Number", "N/A")));
            
            result.put("customer_info", customerInfo);

            // Add meter readings
            Map<String, String> meterReadings = new HashMap<>();
            String lastReadingSr = server3Data.optString("lastBillReadingSr", "");
            if (!lastReadingSr.isEmpty() && !lastReadingSr.equals("null")) {
                meterReadings.put("Last Bill Reading SR", lastReadingSr);
            }
            result.put("meter_readings", meterReadings);

        } catch (Exception e) {
            System.out.println("❌ Error extracting SERVER3 data: " + e.getMessage());
        }
    }

    private void extractSERVER1Data(Object server1DataObj, Map<String, Object> result) {
        try {
            if (server1DataObj instanceof String) {
                String responseBody = (String) server1DataObj;
                
                // Extract tokens using your existing pattern
                List<Map<String, String>> transactions = extractTransactionsWithExactPatterns(responseBody);
                if (!transactions.isEmpty()) {
                    result.put("recharge_history", transactions);
                    result.put("total_recharges", transactions.size());
                }
                
                // Extract customer info from SERVER1
                Map<String, String> customerInfo = (Map<String, String>) result.getOrDefault("customer_info", new HashMap<>());
                
                // You can add more SERVER1 customer info extraction here if needed
                result.put("customer_info", customerInfo);
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting SERVER1 data: " + e.getMessage());
        }
    }

    // Your existing token extraction method
    private List<Map<String, String>> extractTransactionsWithExactPatterns(String response) {
        List<Map<String, String>> transactions = new ArrayList<>();
        
        System.out.println("🔍 Looking for tokens with exact pattern...");

        int index = 0;
        int count = 0;

        while (index != -1 && count < 3) {
            index = response.indexOf("\"tokens\":{\"_text\":\"", index);
            if (index == -1) break;

            int valueStart = index + "\"tokens\":{\"_text\":\"".length();
            int valueEnd = response.indexOf("\"", valueStart);

            if (valueEnd != -1) {
                String token = response.substring(valueStart, valueEnd);
                System.out.println("🔑 FOUND TOKEN " + (count + 1) + ": " + token);

                Map<String, String> transaction = extractTransactionFields(response, index);
                transaction.put("Tokens", token);
                transactions.add(transaction);
                count++;
            }

            index = valueEnd + 1;
        }

        return transactions;
    }

    private Map<String, String> extractTransactionFields(String response, int tokenPosition) {
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
        return "N/A";
    }

    private String getMeterStatus(String statusCode) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("1", "Active");
        statusMap.put("2", "Inactive");
        statusMap.put("3", "Disconnected");
        return statusMap.getOrDefault(statusCode, "Unknown (" + statusCode + ")");
    }
}