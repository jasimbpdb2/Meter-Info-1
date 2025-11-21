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

            // DEBUG: Show what raw data we have
            result.put("debug_raw_keys", getKeysAsString(rawData));

            // Extract SERVER2 data for customer info, balance, and bills
            if (rawData.containsKey("SERVER2_data")) {
                JSONObject server2Data = (JSONObject) rawData.get("SERVER2_data");

                // DEBUG: Show SERVER2 structure
                result.put("debug_server2_keys", getJSONKeys(server2Data));

                extractSERVER2Data(server2Data, result);
            } else {
                result.put("debug_server2", "NO SERVER2 DATA");
            }

            // Extract SERVER3 data for additional customer info
            if (rawData.containsKey("SERVER3_data")) {
                JSONObject server3Data = (JSONObject) rawData.get("SERVER3_data");
                result.put("debug_server3_keys", getJSONKeys(server3Data));
                extractSERVER3Data(server3Data, result);
            } else {
                result.put("debug_server3", "NO SERVER3 DATA");
            }

            // Extract SERVER1 data for prepaid (tokens and customer info)
            if ("prepaid".equals(type) && rawData.containsKey("SERVER1_data")) {
                result.put("debug_server1", "HAS SERVER1 DATA");
                extractSERVER1Data(rawData.get("SERVER1_data"), result);
            } else {
                result.put("debug_server1", "NO SERVER1 DATA");
            }

            // Process bill information for table display
            processBillInformation(result);

        } catch (Exception e) {
            System.out.println("❌ Error processing data for HTML: " + e.getMessage());
            result.put("error", "Data processing failed: " + e.getMessage());
        }

        return result;
    }

    private void processBillInformation(Map<String, Object> result) {
        try {
            if (result.containsKey("bill_info")) {
                List<Map<String, Object>> billInfo = (List<Map<String, Object>>) result.get("bill_info");
                
                // Calculate bill summary
                Map<String, Object> billSummary = new HashMap<>();
                int totalBills = billInfo.size();
                double totalAmount = 0;
                double totalPaid = 0;
                double totalArrears = 0;

                for (Map<String, Object> bill : billInfo) {
                    double billAmount = getDoubleValue(bill, "TOTAL_BILL");
                    double paidAmount = getDoubleValue(bill, "PAID_AMT");
                    double balance = getDoubleValue(bill, "BALANCE");

                    totalAmount += billAmount;
                    totalPaid += paidAmount;
                    totalArrears += balance;
                }

                billSummary.put("total_bills", totalBills);
                billSummary.put("total_amount", totalAmount);
                billSummary.put("total_paid", totalPaid);
                billSummary.put("arrears", totalArrears);

                result.put("bill_summary", billSummary);

                // Format bill dates for display
                for (Map<String, Object> bill : billInfo) {
                    if (bill.containsKey("BILL_MONTH")) {
                        String billMonth = bill.get("BILL_MONTH").toString();
                        bill.put("formatted_month", formatBillMonth(billMonth));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error processing bill information: " + e.getMessage());
        }
    }

    private void extractSERVER2Data(JSONObject server2Data, Map<String, Object> result) {
        try {
            // Extract customer info from SERVER2
            if (server2Data.has("customerInfo")) {
                Object customerInfoObj = server2Data.get("customerInfo");
                System.out.println("🔍 SERVER2 customerInfo type: " + customerInfoObj.getClass().getSimpleName());

                if (customerInfoObj instanceof JSONArray) {
                    JSONArray customerInfoArray = (JSONArray) customerInfoObj;
                    System.out.println("🔍 SERVER2 customerInfo array length: " + customerInfoArray.length());

                    if (customerInfoArray.length() > 0) {
                        Object firstElement = customerInfoArray.get(0);
                        if (firstElement instanceof JSONArray) {
                            JSONArray innerArray = (JSONArray) firstElement;
                            if (innerArray.length() > 0) {
                                JSONObject customerData = innerArray.getJSONObject(0);
                                extractCustomerInfoFromSERVER2(customerData, result);
                            }
                        } else if (firstElement instanceof JSONObject) {
                            extractCustomerInfoFromSERVER2((JSONObject) firstElement, result);
                        }
                    }
                }
            }

            // Extract balance information
            Map<String, String> balanceInfo = new HashMap<>();
            if (server2Data.has("finalBalanceInfo")) {
                String balanceString = server2Data.optString("finalBalanceInfo");
                if (balanceString != null && !balanceString.equals("null") && !balanceString.isEmpty()) {
                    try {
                        double balance = Double.parseDouble(balanceString);
                        balanceInfo.put("Total Balance", String.format("৳ %.2f", balance));
                        balanceInfo.put("Arrear Amount", String.format("৳ %.2f", balance));
                    } catch (NumberFormatException e) {
                        balanceInfo.put("Total Balance", balanceString);
                        balanceInfo.put("Arrear Amount", balanceString);
                    }
                }
            }

            if (!balanceInfo.isEmpty()) {
                result.put("balance_info", balanceInfo);
            }

            // Extract bill information - ENHANCED FOR TABLE DISPLAY
            if (server2Data.has("billInfo")) {
                JSONArray billArray = server2Data.getJSONArray("billInfo");
                List<Map<String, Object>> billInfo = new ArrayList<>();

                for (int i = 0; i < billArray.length(); i++) {
                    JSONObject bill = billArray.getJSONObject(i);
                    Map<String, Object> billData = new HashMap<>();

                    // Extract all bill fields for table display
                    billData.put("BILL_MONTH", bill.optString("BILL_MONTH", "N/A"));
                    billData.put("BILL_NO", bill.optString("BILL_NO", "N/A"));
                    billData.put("CONS_KWH_SR", bill.optDouble("CONS_KWH_SR", 0));
                    billData.put("TOTAL_BILL", bill.optDouble("TOTAL_BILL", 0));
                    billData.put("PAID_AMT", bill.optDouble("PAID_AMT", 0));
                    billData.put("BALANCE", bill.optDouble("BALANCE", 0));
                    billData.put("INVOICE_DUE_DATE", bill.optString("INVOICE_DUE_DATE", "N/A"));
                    billData.put("CURRENT_BILL", bill.optDouble("CURRENT_BILL", 0));
                    billData.put("ARREAR_BILL", bill.optDouble("ARREAR_BILL", 0));

                    billInfo.add(billData);
                }

                result.put("bill_info", billInfo);
            }

        } catch (Exception e) {
            System.out.println("❌ Error extracting SERVER2 data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void extractCustomerInfoFromSERVER2(JSONObject customerData, Map<String, Object> result) {
        try {
            Map<String, String> customerInfo = (Map<String, String>) result.getOrDefault("customer_info", new HashMap<>());

            // Extract all available fields from SERVER2
            customerInfo.put("Customer Name", getStringValue(customerData, "CUSTOMER_NAME"));
            customerInfo.put("Address", getStringValue(customerData, "ADDRESS"));
            customerInfo.put("Tariff", getStringValue(customerData, "TARIFF"));
            customerInfo.put("Location Code", getStringValue(customerData, "LOCATION_CODE"));
            customerInfo.put("Bill Group", getStringValue(customerData, "BILL_GROUP"));
            customerInfo.put("Meter Number", getStringValue(customerData, "METER_NUM"));
            customerInfo.put("Meter Status", getMeterStatus(getStringValue(customerData, "METER_STATUS")));
            customerInfo.put("Usage Type", getStringValue(customerData, "USAGE_TYPE"));
            customerInfo.put("Description", getStringValue(customerData, "DESCR"));
            customerInfo.put("Start Bill Cycle", getStringValue(customerData, "START_BILL_CYCLE"));

            // Remove null values
            customerInfo.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().equals("N/A"));

            if (!customerInfo.isEmpty()) {
                result.put("customer_info", customerInfo);
            }

        } catch (Exception e) {
            System.out.println("❌ Error extracting customer info from SERVER2: " + e.getMessage());
        }
    }

    private void extractSERVER3Data(JSONObject server3Data, Map<String, Object> result) {
        try {
            // Get or create customer info
            Map<String, String> customerInfo = (Map<String, String>) result.getOrDefault("customer_info", new HashMap<>());

            // Add SERVER3 customer info - use optString with fallback to existing values
            customerInfo.put("Customer Name", getStringValue(server3Data, "customerName", customerInfo.get("Customer Name")));
            customerInfo.put("Father Name", getStringValue(server3Data, "fatherName"));
            customerInfo.put("Customer Address", getStringValue(server3Data, "customerAddr", customerInfo.get("Address")));
            customerInfo.put("Location Code", getStringValue(server3Data, "locationCode", customerInfo.get("Location Code")));
            customerInfo.put("Area Code", getStringValue(server3Data, "areaCode"));
            customerInfo.put("Bill Group", getStringValue(server3Data, "billGroup", customerInfo.get("Bill Group")));
            customerInfo.put("Book Number", getStringValue(server3Data, "bookNumber"));
            customerInfo.put("Tariff Description", getStringValue(server3Data, "tariffDesc", customerInfo.get("Tariff")));
            customerInfo.put("Sanctioned Load", getStringValue(server3Data, "sanctionedLoad"));
            customerInfo.put("Walk Order", getStringValue(server3Data, "walkOrder"));
            customerInfo.put("Meter Number", getStringValue(server3Data, "meterNum", customerInfo.get("Meter Number")));
            customerInfo.put("Usage Type", getStringValue(server3Data, "usageType"));
            customerInfo.put("Description", getStringValue(server3Data, "description"));
            customerInfo.put("Start Bill Cycle", getStringValue(server3Data, "startBillCycle"));
            customerInfo.put("Meter Condition", getStringValue(server3Data, "meterConditionDesc"));

            // Remove null values
            customerInfo.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().equals("N/A"));

            if (!customerInfo.isEmpty()) {
                result.put("customer_info", customerInfo);
            }

            // Add meter readings
            Map<String, String> meterReadings = new HashMap<>();
            String lastReadingSr = getStringValue(server3Data, "lastBillReadingSr");
            if (!lastReadingSr.equals("N/A")) {
                meterReadings.put("Last Bill Reading SR", lastReadingSr);
            }

            String lastReadingOfPk = getStringValue(server3Data, "lastBillReadingOfPk");
            if (!lastReadingOfPk.equals("N/A")) {
                meterReadings.put("Last Bill Reading OF PK", lastReadingOfPk);
            }

            String lastReadingPk = getStringValue(server3Data, "lastBillReadingPk");
            if (!lastReadingPk.equals("N/A")) {
                meterReadings.put("Last Bill Reading PK", lastReadingPk);
            }

            if (!meterReadings.isEmpty()) {
                result.put("meter_readings", meterReadings);
            }

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

                // Extract customer info from SERVER1 if available
                Map<String, String> customerInfo = (Map<String, String>) result.getOrDefault("customer_info", new HashMap<>());

                // Add SERVER1 specific customer info
                customerInfo.put("Consumer Number", extractValueFromSERVER1(responseBody, "customerAccountNo"));
                customerInfo.put("Name", extractValueFromSERVER1(responseBody, "customerName"));
                customerInfo.put("Address", extractValueFromSERVER1(responseBody, "customerAddress"));
                customerInfo.put("Phone", extractValueFromSERVER1(responseBody, "customerPhone"));
                customerInfo.put("Division", extractValueFromSERVER1(responseBody, "division"));
                customerInfo.put("Sub Division", extractValueFromSERVER1(responseBody, "sndDivision"));
                customerInfo.put("Tariff Category", extractValueFromSERVER1(responseBody, "tariffCategory"));
                customerInfo.put("Connection Category", extractValueFromSERVER1(responseBody, "connectionCategory"));
                customerInfo.put("Account Type", extractValueFromSERVER1(responseBody, "accountType"));
                customerInfo.put("Meter Type", extractValueFromSERVER1(responseBody, "meterType"));
                customerInfo.put("Sanctioned Load", extractValueFromSERVER1(responseBody, "sanctionLoad"));
                customerInfo.put("Meter Number", extractValueFromSERVER1(responseBody, "meterNumber"));
                customerInfo.put("Last Recharge Amount", extractValueFromSERVER1(responseBody, "lastRechargeAmount"));
                customerInfo.put("Last Recharge Time", extractValueFromSERVER1(responseBody, "lastRechargeTime"));
                customerInfo.put("Installation Date", extractValueFromSERVER1(responseBody, "installationDate"));
                customerInfo.put("Lock Status", extractValueFromSERVER1(responseBody, "lockStatus"));
                customerInfo.put("Total Recharge This Month", extractValueFromSERVER1(responseBody, "totalRechargeThisMonth"));

                // Remove null values
                customerInfo.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().equals("N/A"));

                if (!customerInfo.isEmpty()) {
                    result.put("customer_info", customerInfo);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting SERVER1 data: " + e.getMessage());
        }
    }

    // Helper methods for safe value extraction
    private String getStringValue(JSONObject json, String key) {
        return getStringValue(json, key, "N/A");
    }

    private String getStringValue(JSONObject json, String key, String defaultValue) {
        try {
            if (json.has(key)) {
                String value = json.optString(key, defaultValue);
                return (value == null || value.equals("null") || value.isEmpty()) ? defaultValue : value;
            }
        } catch (Exception e) {
            // Ignore and return default
        }
        return defaultValue;
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0.0;
    }

    private String extractValueFromSERVER1(String responseBody, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\":{\"_text\":\"";
            int start = responseBody.indexOf(pattern);
            if (start != -1) {
                int valueStart = start + pattern.length();
                int valueEnd = responseBody.indexOf("\"", valueStart);
                if (valueEnd != -1) {
                    String value = responseBody.substring(valueStart, valueEnd);
                    return (value == null || value.isEmpty() || value.equals("{}")) ? "N/A" : value;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "N/A";
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
        if (statusCode == null || statusCode.equals("N/A")) {
            return "N/A";
        }
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("1", "Active");
        statusMap.put("2", "Inactive");
        statusMap.put("3", "Disconnected");
        return statusMap.getOrDefault(statusCode, "Unknown (" + statusCode + ")");
    }

    private String formatBillMonth(String dateStr) {
        if (dateStr == null || dateStr.equals("N/A")) return "N/A";
        
        try {
            String[] parts = dateStr.split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                
                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + "-" + parts[0];
                }
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    // Debug helper methods
    private String getKeysAsString(Map<String, Object> map) {
        return String.join(", ", map.keySet());
    }

    private String getJSONKeys(JSONObject json) {
        try {
            Iterator<String> keys = json.keys();
            List<String> keyList = new ArrayList<>();
            while (keys.hasNext()) {
                keyList.add(keys.next());
            }
            return String.join(", ", keyList);
        } catch (Exception e) {
            return "Error getting keys: " + e.getMessage();
        }
    }
}