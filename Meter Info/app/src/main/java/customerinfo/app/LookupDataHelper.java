package customerinfo.app;

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
            e.printStackTrace();
            result.put("error", "Data fetch failed: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> fetchPrepaidDataForLookup(String meterNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Step 1: Get consumer number from SERVER1
            System.out.println("📡 PREPAID: Calling SERVER1Lookup for meter: " + meterNumber);
            Map<String, Object> server1Result = MainActivity.SERVER1Lookup(meterNumber);
            debugData("SERVER1 Result", server1Result);
            
            String consumerNumber = (String) server1Result.get("consumer_number");
            System.out.println("📋 PREPAID: Consumer number from SERVER1: " + consumerNumber);

            if (consumerNumber == null || server1Result.containsKey("error")) {
                result.put("error", "মিটার নং ভুল বা ডেটা পাওয়া যায়নি");
                return result;
            }

            // Step 2: Get detailed data from SERVER3 (which includes SERVER2)
            System.out.println("📡 PREPAID: Calling SERVER3Lookup for consumer: " + consumerNumber);
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(consumerNumber);
            debugData("SERVER3 Result", server3Result);

            if (server3Result.containsKey("error")) {
                result.put("error", "গ্রাহক তথ্য পাওয়া যায়নি: " + server3Result.get("error"));
                return result;
            }

            // Combine all data directly without MainActivity methods
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.putAll(server3Result);
            combinedResult.put("meter_number", meterNumber);
            combinedResult.put("consumer_number", consumerNumber);

            // Add SERVER1 data if available - FIXED CAST
            if (server1Result.containsKey("SERVER1_data")) {
                Object server1Data = server1Result.get("SERVER1_data");
                System.out.println("🔧 PREPAID: SERVER1_data type: " + (server1Data != null ? server1Data.getClass().getSimpleName() : "NULL"));
                
                if (server1Data instanceof Map) {
                    combinedResult.putAll((Map) server1Data);
                    System.out.println("✅ PREPAID: Added SERVER1_data Map to combined result");
                } else {
                    System.out.println("⚠️ PREPAID: SERVER1_data is not a Map, it's: " + (server1Data != null ? server1Data.getClass().getSimpleName() : "NULL"));
                    // Try to handle it as string or other type
                    combinedResult.put("SERVER1_data_raw", server1Data);
                }
            }

            debugData("PREPAID Combined Result", combinedResult);
            
            // Format the data for HTML display
            System.out.println("🎨 PREPAID: Formatting data for display...");
            result = formatLookupDataForDisplay(combinedResult, "prepaid");
            debugData("PREPAID Final Result", result);

        } catch (Exception e) {
            System.out.println("❌ PREPAID ERROR: " + e.getMessage());
            e.printStackTrace();
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> fetchPostpaidDataForLookup(String inputNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // For postpaid, directly use SERVER3 lookup
            System.out.println("📡 POSTPAID: Calling SERVER3Lookup for: " + inputNumber);
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(inputNumber);
            debugData("POSTPAID SERVER3 Result", server3Result);

            if (server3Result.containsKey("error")) {
                result.put("error", "গ্রাহক তথ্য পাওয়া যায়নি: " + server3Result.get("error"));
                return result;
            }

            // Combine data directly
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.putAll(server3Result);
            combinedResult.put("customer_number", inputNumber);

            debugData("POSTPAID Combined Result", combinedResult);
            
            // Format the data for HTML display
            System.out.println("🎨 POSTPAID: Formatting data for display...");
            result = formatLookupDataForDisplay(combinedResult, "postpaid");
            debugData("POSTPAID Final Result", result);

        } catch (Exception e) {
            System.out.println("❌ POSTPAID ERROR: " + e.getMessage());
            e.printStackTrace();
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> formatLookupDataForDisplay(Map<String, Object> rawData, String type) {
        System.out.println("🔄 FORMAT: Calling MainActivity.getLookupData with type: " + type);
        debugData("FORMAT Input Data", rawData);
        
        Map<String, Object> result = MainActivity.getLookupData(rawData, type);
        
        debugData("FORMAT Output Data", result);
        return result;
    }

    // Debug method to see what data we're getting
    private void debugData(String label, Map<String, Object> data) {
        System.out.println("🔍 === " + label + " ===");
        if (data == null) {
            System.out.println("🔍 NULL DATA");
            return;
        }
        
        if (data.isEmpty()) {
            System.out.println("🔍 EMPTY DATA");
            return;
        }
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                System.out.println("🔍 " + key + ": NULL");
            } else if (value instanceof Map) {
                Map<?, ?> mapValue = (Map<?, ?>) value;
                System.out.println("🔍 " + key + ": MAP with " + mapValue.size() + " entries");
                // Print first few entries of map
                int count = 0;
                for (Map.Entry<?, ?> mapEntry : mapValue.entrySet()) {
                    if (count < 3) { // Show first 3 entries only
                        System.out.println("🔍   " + mapEntry.getKey() + ": " + mapEntry.getValue());
                        count++;
                    } else {
                        System.out.println("🔍   ... and " + (mapValue.size() - 3) + " more");
                        break;
                    }
                }
            } else if (value instanceof List) {
                List<?> listValue = (List<?>) value;
                System.out.println("🔍 " + key + ": LIST with " + listValue.size() + " items");
                // Print first few items of list
                if (!listValue.isEmpty()) {
                    for (int i = 0; i < Math.min(2, listValue.size()); i++) {
                        System.out.println("🔍   [" + i + "]: " + listValue.get(i));
                    }
                    if (listValue.size() > 2) {
                        System.out.println("🔍   ... and " + (listValue.size() - 2) + " more");
                    }
                }
            } else if (value instanceof String) {
                String strValue = (String) value;
                System.out.println("🔍 " + key + ": STRING = '" + strValue + "'");
            } else {
                System.out.println("🔍 " + key + ": " + value.getClass().getSimpleName() + " = " + value);
            }
        }
        System.out.println("🔍 ==================");
    }
}