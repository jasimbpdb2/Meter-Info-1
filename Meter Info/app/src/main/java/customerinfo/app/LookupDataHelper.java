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

            // Step 3: Extract and merge all data using MainActivity's methods
            MainActivity mainActivity = new MainActivity();
            
            // Get SERVER1 cleaned data
            Map<String, Object> cleanedSERVER1 = mainActivity.cleanSERVER1Data(server1Result.get("SERVER1_data"));
            
            // Merge SERVER2 and SERVER3 data
            Map<String, Object> mergedData = mainActivity.mergeSERVERData(server3Result);
            
            // Combine all data
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.putAll(server3Result);
            combinedResult.put("meter_number", meterNumber);
            combinedResult.put("consumer_number", consumerNumber);
            
            if (mergedData != null) {
                combinedResult.putAll(mergedData);
            }
            if (cleanedSERVER1 != null) {
                combinedResult.putAll(cleanedSERVER1);
            }

            // Format the data exactly like MainActivity's displayResult method
            result = formatLookupDataForDisplay(combinedResult, "prepaid");

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

            // Use MainActivity's methods to process data
            MainActivity mainActivity = new MainActivity();
            
            // Merge SERVER2 and SERVER3 data
            Map<String, Object> mergedData = mainActivity.mergeSERVERData(server3Result);
            
            // Combine all data
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.putAll(server3Result);
            combinedResult.put("customer_number", inputNumber);
            
            if (mergedData != null) {
                combinedResult.putAll(mergedData);
            }

            // Format the data exactly like MainActivity's displayResult method
            result = formatLookupDataForDisplay(combinedResult, "postpaid");

        } catch (Exception e) {
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> formatLookupDataForDisplay(Map<String, Object> rawData, String type) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            MainActivity mainActivity = new MainActivity();
            
            // Get the formatted text display (to understand the structure)
            String textDisplay = mainActivity.displayResult(rawData, type);
            
            // Extract all the structured data for HTML display
            extractStructuredData(rawData, result, type);
            
        } catch (Exception e) {
            System.out.println("❌ Error formatting lookup data: " + e.getMessage());
            result.put("error", "Data formatting failed: " + e.getMessage());
        }
        
        return result;
    }

    private void extractStructuredData(Map<String, Object> rawData, Map<String, Object> result, String type) {
        // Basic information
        result.put("type", type);
        result.put("meter_number", rawData.getOrDefault("meter_number", "N/A"));
        result.put("consumer_number", rawData.getOrDefault("consumer_number", rawData.get("customer_number")));
        
        // Extract customer information
        extractCustomerInfo(rawData, result);
        
        // Extract balance information
        extractBalanceInfo(rawData, result);
        
        // Extract bill information (for both prepaid and postpaid)
        extractBillInfo(rawData, result);
        
        // Extract recharge history for prepaid
        if ("prepaid".equals(type)) {
            extractRechargeInfo(rawData, result);
        }
        
        // Extract meter readings
        extractMeterReadings(rawData, result);
    }

    private void extractCustomerInfo(Map<String, Object> rawData, Map<String, Object> result) {
        Map<String, String> customerInfo = new HashMap<>();
        
        // Try to get customer info from merged data first
        if (rawData.containsKey("customer_info") && rawData.get("customer_info") instanceof Map) {
            customerInfo.putAll((Map<String, String>) rawData.get("customer_info"));
        }
        
        // Fallback to individual fields
        if (customerInfo.isEmpty()) {
            // Extract from SERVER3 data
            if (rawData.containsKey("SERVER3_data")) {
                JSONObject server3Data = (JSONObject) rawData.get("SERVER3_data");
                customerInfo.put("Customer Name", server3Data.optString("customerName", "N/A"));
                customerInfo.put("Father Name", server3Data.optString("fatherName", "N/A"));
                customerInfo.put("Customer Address", server3Data.optString("customerAddr", "N/A"));
                customerInfo.put("Location Code", server3Data.optString("locationCode", "N/A"));
                customerInfo.put("Area Code", server3Data.optString("areaCode", "N/A"));
                customerInfo.put("Bill Group", server3Data.optString("billGroup", "N/A"));
                customerInfo.put("Book Number", server3Data.optString("bookNumber", "N/A"));
                customerInfo.put("Tariff Description", server3Data.optString("tariffDesc", "N/A"));
                customerInfo.put("Sanctioned Load", server3Data.optString("sanctionedLoad", "N/A"));
                customerInfo.put("Walk Order", server3Data.optString("walkOrder", "N/A"));
            }
            
            // Extract from SERVER1 data (for prepaid)
            if (rawData.containsKey("customer_info") && rawData.get("customer_info") instanceof Map) {
                Map<String, String> server1Info = (Map<String, String>) rawData.get("customer_info");
                customerInfo.putAll(server1Info);
            }
        }
        
        result.put("customer_info", customerInfo);
    }

    private void extractBalanceInfo(Map<String, Object> rawData, Map<String, Object> result) {
        Map<String, String> balanceInfo = new HashMap<>();
        
        // Try to get balance info from merged data
        if (rawData.containsKey("balance_info") && rawData.get("balance_info") instanceof Map) {
            balanceInfo.putAll((Map<String, String>) rawData.get("balance_info"));
        }
        
        // If no balance info found, try to extract from SERVER3
        if (balanceInfo.isEmpty() && rawData.containsKey("SERVER3_data")) {
            JSONObject server3Data = (JSONObject) rawData.get("SERVER3_data");
            String arrearAmount = server3Data.optString("arrearAmount", "");
            if (!arrearAmount.isEmpty() && !arrearAmount.equals("0") && !arrearAmount.equals("0.00")) {
                balanceInfo.put("Total Balance", arrearAmount);
                balanceInfo.put("Arrear Amount", arrearAmount);
            }
        }
        
        result.put("balance_info", balanceInfo);
    }

    private void extractBillInfo(Map<String, Object> rawData, Map<String, Object> result) {
        List<Map<String, Object>> billInfo = new ArrayList<>();
        
        // Try to get bill info from merged data
        if (rawData.containsKey("bill_info_raw") && rawData.get("bill_info_raw") instanceof JSONArray) {
            JSONArray billArray = (JSONArray) rawData.get("bill_info_raw");
            for (int i = 0; i < billArray.length(); i++) {
                try {
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
                } catch (Exception e) {
                    // Skip invalid bill records
                }
            }
        }
        
        result.put("bill_info", billInfo);
        
        // Also add bill summary
        if (rawData.containsKey("bill_summary") && rawData.get("bill_summary") instanceof Map) {
            result.put("bill_summary", rawData.get("bill_summary"));
        } else {
            // Create basic bill summary
            Map<String, Object> billSummary = new HashMap<>();
            billSummary.put("total_bills", billInfo.size());
            if (!billInfo.isEmpty()) {
                Map<String, Object> firstBill = billInfo.get(0);
                Map<String, Object> lastBill = billInfo.get(billInfo.size() - 1);
                
                billSummary.put("first_bill_period", formatBillMonth(String.valueOf(firstBill.get("BILL_MONTH"))));
                billSummary.put("last_bill_period", formatBillMonth(String.valueOf(lastBill.get("BILL_MONTH"))));
                
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
    }

    private void extractRechargeInfo(Map<String, Object> rawData, Map<String, Object> result) {
        List<Map<String, String>> rechargeHistory = new ArrayList<>();
        
        // Try to get recharge transactions
        if (rawData.containsKey("recent_transactions") && rawData.get("recent_transactions") instanceof List) {
            rechargeHistory = (List<Map<String, String>>) rawData.get("recent_transactions");
        }
        
        result.put("recharge_history", rechargeHistory);
        result.put("total_recharges", rechargeHistory.size());
    }

    private void extractMeterReadings(Map<String, Object> rawData, Map<String, Object> result) {
        Map<String, String> meterReadings = new HashMap<>();
        
        // Extract from customer info
        if (rawData.containsKey("customer_info") && rawData.get("customer_info") instanceof Map) {
            Map<String, String> customerInfo = (Map<String, String>) rawData.get("customer_info");
            
            String[] readingKeys = {
                "Current Reading SR", "Last Bill Reading SR", 
                "Last Bill Reading OF PK", "Last Bill Reading PK"
            };
            
            for (String key : readingKeys) {
                if (customerInfo.containsKey(key)) {
                    meterReadings.put(key, customerInfo.get(key));
                }
            }
        }
        
        // Extract from SERVER3 data
        if (rawData.containsKey("SERVER3_data")) {
            JSONObject server3Data = (JSONObject) rawData.get("SERVER3_data");
            
            String lastReadingSr = server3Data.optString("lastBillReadingSr", "");
            if (!lastReadingSr.isEmpty() && !lastReadingSr.equals("null")) {
                meterReadings.put("Last Bill Reading SR", lastReadingSr);
            }
            
            String lastReadingOfPk = server3Data.optString("lastBillReadingOfPk", "");
            if (!lastReadingOfPk.isEmpty() && !lastReadingOfPk.equals("null")) {
                meterReadings.put("Last Bill Reading OF PK", lastReadingOfPk);
            }
            
            String lastReadingPk = server3Data.optString("lastBillReadingPk", "");
            if (!lastReadingPk.isEmpty() && !lastReadingPk.equals("null")) {
                meterReadings.put("Last Bill Reading PK", lastReadingPk);
            }
        }
        
        result.put("meter_readings", meterReadings);
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
}