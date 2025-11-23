package customerinfo.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class LookupDataHelper {

    private String currentType;

    public LookupDataHelper() {
        // Empty constructor
    }

    public Map<String, Object> fetchDataForLookup(String inputNumber, String type, String subType) {
        Map<String, Object> result = new HashMap<>();
        this.currentType = type;

        try {
            System.out.println("🔍 LOOKUP DATA HELPER: Fetching " + type + " data for: " + inputNumber + ", subType: " + subType);

            if ("prepaid".equals(type)) {
                result = fetchPrepaidDataForLookup(inputNumber);
            } else {
                result = fetchPostpaidDataForLookup(inputNumber, subType);
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

            // ✅ STEP 2: Try SERVER3 but DON'T treat failure as error for prepaid
            Map<String, Object> server3Result = MainActivity.SERVER3Lookup(consumerNumber);

            // Create combined result with ALL available data
            Map<String, Object> combinedResult = new HashMap<>();
            combinedResult.put("meter_number", meterNumber);
            combinedResult.put("consumer_number", consumerNumber);

            // ✅ ALWAYS add SERVER1 data (prepaid details)
            if (server1Result.containsKey("SERVER1_data")) {
                combinedResult.put("SERVER1_data", server1Result.get("SERVER1_data"));
            }

            // ✅ Add SERVER3 data ONLY if available (not an error)
            if (server3Result != null && !server3Result.containsKey("error")) {
                combinedResult.putAll(server3Result);
                combinedResult.put("data_status", "complete"); // Has both prepaid + postpaid data
                System.out.println("✅ Prepaid with postpaid history data found");
            } else {
                // ✅ SERVER2/3 failure is NORMAL for lifelong prepaid customers
                combinedResult.put("data_status", "prepaid_only"); // Only prepaid data available
                combinedResult.put("server23_status", "no_postpaid_history");
                System.out.println("✅ Lifelong prepaid customer - no postpaid history");
            }

            // Process data for HTML display
            result = processDataForHTML(combinedResult, "prepaid");

        } catch (Exception e) {
            result.put("error", "ডেটা প্রসেসিং ব্যর্থ: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> fetchPostpaidDataForLookup(String inputNumber, String subType) {
        Map<String, Object> result = new HashMap<>();

        try {
            if ("consumer_no".equals(subType)) {
                // ✅ Existing logic for consumer number
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

            } else {
                // ✅ NEW logic for meter number
                // Step 1: Get customer numbers from meter number
                Map<String, Object> meterLookupResult = MainActivity.getCustomerNumbersByMeter(inputNumber);

                if (meterLookupResult.containsKey("error")) {
                    result.put("error", "মিটার নং ভুল বা ডেটা পাওয়া যায়নি: " + meterLookupResult.get("error"));
                    return result;
                }

                List<String> customerNumbers = (List<String>) meterLookupResult.get("customer_numbers");
                if (customerNumbers == null || customerNumbers.isEmpty()) {
                    result.put("error", "এই মিটার নং এর জন্য কোন গ্রাহক পাওয়া যায়নি");
                    return result;
                }

                // Step 2: Get detailed data for the first customer
                String firstCustomerNumber = customerNumbers.get(0);
                Map<String, Object> server3Result = MainActivity.SERVER3Lookup(firstCustomerNumber);

                if (server3Result.containsKey("error")) {
                    result.put("error", "গ্রাহক তথ্য পাওয়া যায়নি: " + server3Result.get("error"));
                    return result;
                }

                // Create combined result with all data
                Map<String, Object> combinedResult = new HashMap<>();
                combinedResult.putAll(server3Result);
                combinedResult.put("meter_number", inputNumber);
                combinedResult.put("customer_number", firstCustomerNumber);
                combinedResult.put("all_customer_numbers", customerNumbers);

                // Add meter lookup data
                if (meterLookupResult.containsKey("meter_api_data")) {
                    combinedResult.put("meter_api_data", meterLookupResult.get("meter_api_data"));
                }

                // Process data for HTML display
                result = processDataForHTML(combinedResult, "postpaid");
            }

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

            System.out.println("🔍 PROCESS DATA FOR HTML: Raw data keys = " + rawData.keySet());

            // ✅ FIXED: Extract SERVER2 data first (has bill info)
            if (rawData.containsKey("SERVER2_data")) {
                Object server2Data = rawData.get("SERVER2_data");
                System.out.println("✅ PROCESS DATA: Found SERVER2_data, type: " +
                        (server2Data != null ? server2Data.getClass().getSimpleName() : "null"));

                if (server2Data instanceof JSONObject) {
                    extractSERVER2Data((JSONObject) server2Data, result);
                } else if (server2Data instanceof String) {
                    try {
                        JSONObject jsonData = new JSONObject((String) server2Data);
                        extractSERVER2Data(jsonData, result);
                    } catch (Exception e) {
                        System.out.println("❌ PROCESS DATA: Failed to parse SERVER2 string: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("❌ PROCESS DATA: No SERVER2_data found");
            }

            // ✅ FIXED: Extract SERVER3 data
            if (rawData.containsKey("SERVER3_data")) {
                Object server3Data = rawData.get("SERVER3_data");
                System.out.println("✅ PROCESS DATA: Found SERVER3_data, type: " +
                        (server3Data != null ? server3Data.getClass().getSimpleName() : "null"));

                if (server3Data instanceof JSONObject) {
                    extractSERVER3Data((JSONObject) server3Data, result);
                }
            } else {
                System.out.println("❌ PROCESS DATA: No SERVER3_data found");
            }

            // ✅ FIXED: Extract prepaid data
            if ("prepaid".equals(type) && rawData.containsKey("SERVER1_data")) {
                extractSERVER1Data(rawData.get("SERVER1_data"), result);
            }

            // Process bill information
            processBillInformation(result);

            // ✅ ADD THIS SINGLE LINE: Apply SERVER2 fallback for missing SERVER3 data
            supplementWithSERVER2Data(result);

            // ✅ FIXED: Enhanced merge that actually works
            Map<String, Object> mergedData = enhancedMergeSERVERData(result);
            result.putAll(mergedData);

            // ✅ NEW: Fetch meter reading data after we have all other data
            fetchAndAddMeterReadingData(result);

            // ✅ DEBUG: Add diagnostic information
            result.put("debug_has_server2", result.containsKey("customer_info") || result.containsKey("bill_info"));
            result.put("debug_has_server3", result.containsKey("SERVER3_customer_info"));
            result.put("debug_has_bills", result.containsKey("bill_info"));
            result.put("debug_raw_keys", rawData.keySet().toString());

        } catch (Exception e) {
            System.out.println("❌ PROCESS DATA ERROR: " + e.getMessage());
            e.printStackTrace();
            result.put("error", "Data processing failed: " + e.getMessage());
        }

        return result;
    }

    // ✅ NEW: Fetch and add meter reading data
    private void fetchAndAddMeterReadingData(Map<String, Object> result) {
        try {
            String consumerNumber = (String) result.get("consumer_number");
            if (consumerNumber == null) {
                consumerNumber = (String) result.get("customer_number");
            }

            String locationCode = getLocationCode(result);
            String billMonth = getLatestBillMonth(result);

            if (consumerNumber != null && locationCode != null && billMonth != null) {
                System.out.println("🔍 FETCHING METER READINGS: Consumer=" + consumerNumber +
                        ", Location=" + locationCode + ", Month=" + billMonth);

                Map<String, Object> meterReadings = fetchMeterReadingData(consumerNumber, locationCode, billMonth);
                if (!meterReadings.isEmpty()) {
                    result.put("meter_readings", meterReadings);
                    System.out.println("✅ Added meter readings data to result");
                }
            } else {
                System.out.println("❌ Missing parameters for meter reading API: " +
                        "Consumer=" + (consumerNumber != null) +
                        ", Location=" + (locationCode != null) +
                        ", BillMonth=" + (billMonth != null));
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching meter reading data: " + e.getMessage());
        }
    }

    // ✅ FIXED: Enhanced merge method that properly uses the fallback data
    private Map<String, Object> enhancedMergeSERVERData(Map<String, Object> rawData) {
        Map<String, Object> merged = new HashMap<>();

        try {
            // Create organized customer info
            Map<String, String> customerInfo = new HashMap<>();

            // ✅ STEP 1: DEBUG - Check what data we have BEFORE merging
            System.out.println("=== 🔍 PRE-MERGE DEBUG ===");
            if (rawData.containsKey("SERVER3_customer_info")) {
                Map<String, String> server3 = (Map<String, String>) rawData.get("SERVER3_customer_info");
                System.out.println("SERVER3_customer_info fields: " + server3.keySet());

                // Check critical fields that should have been filled by fallback
                String[] checkFields = {"Father Name", "Area Code", "Book Number", "Walk Order", "Meter Condition"};
                for (String field : checkFields) {
                    String value = server3.get(field);
                    System.out.println("SERVER3 " + field + ": " + (isValidValue(value) ? "✅ " + value : "❌ NULL/MISSING"));
                }
            }
            if (rawData.containsKey("customer_info")) {
                Map<String, String> server2 = (Map<String, String>) rawData.get("customer_info");
                System.out.println("customer_info (SERVER2) fields: " + server2.keySet());
            }
            System.out.println("=== 🎯 END PRE-MERGE DEBUG ===");

            // ✅ STEP 2: Start with SERVER3 data (which should have fallback values)
            if (rawData.containsKey("SERVER3_customer_info")) {
                Map<String, String> server3Info = (Map<String, String>) rawData.get("SERVER3_customer_info");

                // Add ALL fields from SERVER3 (including fallback values)
                for (Map.Entry<String, String> entry : server3Info.entrySet()) {
                    if (isValidValue(entry.getValue())) {
                        customerInfo.put(entry.getKey(), entry.getValue());
                    }
                }
                System.out.println("✅ MERGE: Added " + server3Info.size() + " fields from SERVER3_customer_info");
            }

            // ✅ STEP 3: Add SERVER2 data ONLY for completely missing fields
            if (rawData.containsKey("customer_info")) {
                Map<String, String> server2Info = (Map<String, String>) rawData.get("customer_info");
                int server2Additions = 0;

                for (Map.Entry<String, String> entry : server2Info.entrySet()) {
                    if (!customerInfo.containsKey(entry.getKey()) && isValidValue(entry.getValue())) {
                        customerInfo.put(entry.getKey(), entry.getValue());
                        server2Additions++;
                        System.out.println("✅ MERGE: Added missing field from SERVER2: " + entry.getKey());
                    }
                }
                System.out.println("✅ MERGE: Added " + server2Additions + " fields from SERVER2");
            }

            // ✅ STEP 4: Ensure all HTML categories have at least some data
            ensureHTMLCategoriesHaveData(customerInfo);

            // ✅ STEP 5: Add to merged result
            if (!customerInfo.isEmpty()) {
                merged.put("customer_info", customerInfo);
                System.out.println("✅ MERGE: Final customer info has " + customerInfo.size() + " fields");

                // Debug: Show final fields
                System.out.println("🔍 FINAL CUSTOMER INFO FIELDS: " + customerInfo.keySet());
            }

            // ✅ STEP 6: Preserve all other important data
            String[] preserveKeys = {
                    "bill_info", "balance_info", "bill_summary",
                    "prepaid_customer_details", "recharge_history",
                    "meter_number", "consumer_number", "customer_number",
                    "type", "data_status", "server23_status", "meter_readings"
            };

            for (String key : preserveKeys) {
                if (rawData.containsKey(key)) {
                    merged.put(key, rawData.get(key));
                }
            }

            System.out.println("✅ MERGE COMPLETE: Final keys = " + merged.keySet());

        } catch (Exception e) {
            System.out.println("❌ MERGE ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return merged;
    }

    // ✅ NEW: Helper to ensure HTML categories have data
    private void ensureHTMLCategoriesHaveData(Map<String, String> customerInfo) {
        // Ensure Personal Information category has data
        if (!customerInfo.containsKey("Customer Name") && customerInfo.containsKey("Customer Number")) {
            customerInfo.put("Customer Name", "Customer " + customerInfo.get("Customer Number"));
        }

        // Ensure Meter Information category has data
        if (!customerInfo.containsKey("Meter Status") && customerInfo.containsKey("Meter Number")) {
            customerInfo.put("Meter Status", "Active");
        }

        // Ensure Billing Information category has data
        if (!customerInfo.containsKey("Account_Number") && customerInfo.containsKey("Customer Number")) {
            customerInfo.put("Account_Number", customerInfo.get("Customer Number"));
        }

        // Ensure Tariff & Load category has data
        if (!customerInfo.containsKey("Connection Category")) {
            customerInfo.put("Connection Category", "Postpaid");
        }
        if (!customerInfo.containsKey("Account Type")) {
            customerInfo.put("Account Type", "Active");
        }

        // Ensure Technical Information category has data
        if (!customerInfo.containsKey("Usage Type")) {
            customerInfo.put("Usage Type", "Residential");
        }
    }

    // ✅ FIXED: Enhanced SERVER2 data extraction
    private void extractSERVER2Data(JSONObject server2Data, Map<String, Object> result) {
        try {
            System.out.println("🔍 EXTRACT SERVER2: Starting extraction");

            // Extract customer info from SERVER2
            if (server2Data.has("customerInfo")) {
                Object customerInfoObj = server2Data.get("customerInfo");

                if (customerInfoObj instanceof JSONArray) {
                    JSONArray customerInfoArray = (JSONArray) customerInfoObj;

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
            } else {
                System.out.println("❌ EXTRACT SERVER2: No customerInfo found");
            }

            // Extract balance information
            Map<String, String> balanceInfo = new HashMap<>();
            if (server2Data.has("finalBalanceInfo")) {
                String balanceString = server2Data.optString("finalBalanceInfo");
                System.out.println("🔍 EXTRACT SERVER2: finalBalanceInfo = " + balanceString);

                if (balanceString != null && !balanceString.equals("null") && !balanceString.isEmpty()) {
                    try {
                        double balance = Double.parseDouble(balanceString);
                        balanceInfo.put("Total Balance", String.format("৳%.0f", balance));
                        balanceInfo.put("Arrear Amount", String.format("৳%.0f", balance));
                    } catch (NumberFormatException e) {
                        balanceInfo.put("Total Balance", balanceString);
                        balanceInfo.put("Arrear Amount", balanceString);
                    }
                }
            }

            if (!balanceInfo.isEmpty()) {
                result.put("balance_info", balanceInfo);
                System.out.println("✅ EXTRACT SERVER2: Balance info added");
            }

            // ✅ FIXED: Extract bill information - CRITICAL!
            if (server2Data.has("billInfo")) {
                try {
                    JSONArray billArray = server2Data.getJSONArray("billInfo");
                    List<Map<String, Object>> billInfo = new ArrayList<>();

                    System.out.println("✅ EXTRACT SERVER2: Found " + billArray.length() + " bills");

                    for (int i = 0; i < billArray.length(); i++) {
                        JSONObject bill = billArray.getJSONObject(i);
                        Map<String, Object> billData = new HashMap<>();

                        billData.put("BILL_MONTH", bill.optString("BILL_MONTH", "N/A"));
                        billData.put("BILL_NO", bill.optString("BILL_NO", "N/A"));
                        billData.put("CONS_KWH_SR", bill.optDouble("CONS_KWH_SR", 0));
                        billData.put("TOTAL_BILL", bill.optDouble("TOTAL_BILL", 0));
                        billData.put("CURRENT_BILL", bill.optDouble("CURRENT_BILL", 0)); // Map to CURRENT_BILL for HTML
                        billData.put("PAID_AMT", bill.optDouble("PAID_AMT", 0));
                        billData.put("BALANCE", bill.optDouble("BALANCE", 0));
                        billData.put("RECEIPT_DATE", bill.optString("RECEIPT_DATE", "N/A"));
                        billData.put("INVOICE_DUE_DATE", bill.optString("INVOICE_DUE_DATE", "N/A"));

                        billInfo.add(billData);
                    }

                    result.put("bill_info", billInfo);
                    System.out.println("✅ EXTRACT SERVER2: Added " + billInfo.size() + " bills to result");

                } catch (Exception e) {
                    System.out.println("❌ EXTRACT SERVER2: Error processing bills: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("❌ EXTRACT SERVER2: No billInfo found");
            }

        } catch (Exception e) {
            System.out.println("❌ EXTRACT SERVER2 ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void extractCustomerInfoFromSERVER2(JSONObject customerData, Map<String, Object> result) {
        try {
            Map<String, String> customerInfo = (Map<String, String>) result.getOrDefault("customer_info", new HashMap<>());

            // ✅ FIXED: Map SERVER2 fields to match HTML expected names
            customerInfo.put("Customer Number", getStringValue(customerData, "CUSTOMER_NUMBER"));
            customerInfo.put("Customer Name", getStringValue(customerData, "CUSTOMER_NAME"));
            customerInfo.put("Customer Address", getStringValue(customerData, "ADDRESS")); // Changed from "Address" to "Customer Address"
            customerInfo.put("Location Code", getStringValue(customerData, "LOCATION_CODE"));
            customerInfo.put("Area Code", getStringValue(customerData, "AREA_CODE"));
            customerInfo.put("Bill Group", getStringValue(customerData, "BILL_GROUP"));
            customerInfo.put("Book Number", getStringValue(customerData, "BOOK"));
            customerInfo.put("Tariff", getStringValue(customerData, "TARIFF"));
            customerInfo.put("Tariff Description", getStringValue(customerData, "TARIFF")); // Map TARIFF to Tariff Description
            customerInfo.put("Sanctioned Load", getStringValue(customerData, "SANCTIONED_LOAD"));
            customerInfo.put("Meter Number", getStringValue(customerData, "METER_NUM"));
            customerInfo.put("Meter Condition", getStringValue(customerData, "METER_CONDITION"));
            customerInfo.put("Meter Status", getStringValue(customerData, "METER_STATUS"));
            customerInfo.put("Walk Order", getStringValue(customerData, "WALKING_SEQUENCE"));
            customerInfo.put("Connection Date", getStringValue(customerData, "METER_CONNECT_DATE"));
            customerInfo.put("Account_Number", getStringValue(customerData, "CONS_EXTG_NUM"));
            customerInfo.put("Usage Type", getStringValue(customerData, "USAGE_TYPE"));
            customerInfo.put("Start Bill Cycle", getStringValue(customerData, "START_BILL_CYCLE"));
            customerInfo.put("Description", getStringValue(customerData, "DESCR"));

            // ✅ ADD: Map to HTML expected fields
            customerInfo.put("Connection Category", "Postpaid"); // Default for postpaid
            customerInfo.put("Account Type", "Active"); // Default for postpaid

            // Remove null values
            customerInfo.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().equals("N/A"));

            if (!customerInfo.isEmpty()) {
                result.put("customer_info", customerInfo);
                System.out.println("✅ EXTRACT SERVER2: Customer info added with " + customerInfo.size() + " fields");
            }

        } catch (Exception e) {
            System.out.println("❌ EXTRACT SERVER2 CUSTOMER ERROR: " + e.getMessage());
        }
    }

    // ✅ FIXED: Enhanced SERVER3 data extraction with HTML field mapping
    private void extractSERVER3Data(JSONObject server3Data, Map<String, Object> result) {
        try {
            System.out.println("🔍 EXTRACT SERVER3: Starting extraction");

            Map<String, String> server3CustomerInfo = new HashMap<>();

            // ✅ FIXED: Map SERVER3 fields to match HTML expected names
            server3CustomerInfo.put("Customer Number", getStringValue(server3Data, "customerNumber"));
            server3CustomerInfo.put("Customer Name", getStringValue(server3Data, "customerName"));
            server3CustomerInfo.put("Customer Address", getStringValue(server3Data, "customerAddr"));
            server3CustomerInfo.put("Father Name", getStringValue(server3Data, "fatherName"));
            server3CustomerInfo.put("Location Code", getStringValue(server3Data, "locationCode"));
            server3CustomerInfo.put("Area Code", getStringValue(server3Data, "areaCode"));
            server3CustomerInfo.put("Bill Group", getStringValue(server3Data, "billGroup"));
            server3CustomerInfo.put("Book Number", getStringValue(server3Data, "bookNumber"));
            server3CustomerInfo.put("Tariff Description", getStringValue(server3Data, "tariffDesc"));
            server3CustomerInfo.put("Sanctioned Load", getStringValue(server3Data, "sanctionedLoad"));
            server3CustomerInfo.put("Meter Number", getStringValue(server3Data, "meterNum"));
            server3CustomerInfo.put("Meter Condition", getStringValue(server3Data, "meterConditionDesc"));
            server3CustomerInfo.put("Walk Order", getStringValue(server3Data, "walkOrder"));
            server3CustomerInfo.put("Arrear Amount", getStringValue(server3Data, "arrearAmount"));
            server3CustomerInfo.put("Phone", getStringValue(server3Data, "phone"));
            server3CustomerInfo.put("Division", getStringValue(server3Data, "division"));
            server3CustomerInfo.put("Sub Division", getStringValue(server3Data, "subDivision"));

            // Meter readings - Map to HTML expected fields
            server3CustomerInfo.put("Last Bill Reading SR", getStringValue(server3Data, "lastBillReadingSr"));
            server3CustomerInfo.put("Last Bill Reading OF PK", getStringValue(server3Data, "lastBillReadingOfPk"));
            server3CustomerInfo.put("Last Bill Reading PK", getStringValue(server3Data, "lastBillReadingPk"));
            server3CustomerInfo.put("Current Reading SR", getStringValue(server3Data, "currentReadingSr"));

            // ✅ ADD: Map to HTML expected fields
            server3CustomerInfo.put("Total Balance", getStringValue(server3Data, "arrearAmount")); // Map arrearAmount to Total Balance
            server3CustomerInfo.put("Connection Category", "Postpaid");
            server3CustomerInfo.put("Account Type", "Active");
            server3CustomerInfo.put("Meter Status", "Active"); // Default for SERVER3
            server3CustomerInfo.put("Connection Date", "N/A"); // Default if not available
            server3CustomerInfo.put("Installation Date", "N/A"); // Default if not available
            server3CustomerInfo.put("Meter Type", "Postpaid"); // Default for postpaid
            server3CustomerInfo.put("Tariff Category", getStringValue(server3Data, "tariffDesc")); // Map tariffDesc to Tariff Category

            // Remove null values
            server3CustomerInfo.entrySet().removeIf(entry ->
                    entry.getValue() == null ||
                            entry.getValue().equals("N/A") ||
                            entry.getValue().equals("null") ||
                            entry.getValue().isEmpty()
            );

            // Store SERVER3 data separately for merging
            result.put("SERVER3_customer_info", server3CustomerInfo);
            System.out.println("✅ EXTRACT SERVER3: Added " + server3CustomerInfo.size() + " fields to SERVER3_customer_info");

        } catch (Exception e) {
            System.out.println("❌ EXTRACT SERVER3 ERROR: " + e.getMessage());
        }
    }

    private void extractSERVER1Data(Object server1DataObj, Map<String, Object> result) {
        try {
            if (server1DataObj instanceof String) {
                String responseBody = (String) server1DataObj;

                System.out.println("🔍 EXTRACTING SERVER1 DATA FOR PREPAID DETAILS");

                // Extract prepaid customer details (like your MainActivity shows)
                Map<String, String> prepaidCustomerDetails = new HashMap<>();

                // Extract from SERVER1 response using patterns
                prepaidCustomerDetails.put("Address", extractValueFromSERVER1(responseBody, "customerAddress"));
                prepaidCustomerDetails.put("Installation Date", extractValueFromSERVER1(responseBody, "installationDate"));
                prepaidCustomerDetails.put("Sanctioned Load", extractValueFromSERVER1(responseBody, "sanctionLoad"));
                prepaidCustomerDetails.put("Sub Division", extractValueFromSERVER1(responseBody, "sndDivision"));
                prepaidCustomerDetails.put("Lock Status", extractValueFromSERVER1(responseBody, "lockStatus"));
                prepaidCustomerDetails.put("Name", extractValueFromSERVER1(responseBody, "customerName"));
                prepaidCustomerDetails.put("Tariff Category", extractValueFromSERVER1(responseBody, "tariffCategory"));
                prepaidCustomerDetails.put("Phone", extractValueFromSERVER1(responseBody, "customerPhone"));
                prepaidCustomerDetails.put("Meter Type", extractValueFromSERVER1(responseBody, "meterType"));
                prepaidCustomerDetails.put("Connection Category", extractValueFromSERVER1(responseBody, "connectionCategory"));
                prepaidCustomerDetails.put("Division", extractValueFromSERVER1(responseBody, "division"));
                prepaidCustomerDetails.put("Meter Number", extractValueFromSERVER1(responseBody, "meterNumber"));
                prepaidCustomerDetails.put("Last Recharge Time", extractValueFromSERVER1(responseBody, "lastRechargeTime"));
                prepaidCustomerDetails.put("Account Type", extractValueFromSERVER1(responseBody, "accountType"));
                prepaidCustomerDetails.put("Last Recharge Amount", extractValueFromSERVER1(responseBody, "lastRechargeAmount"));
                prepaidCustomerDetails.put("Consumer Number", extractValueFromSERVER1(responseBody, "customerAccountNo"));
                prepaidCustomerDetails.put("Total Recharge This Month", extractValueFromSERVER1(responseBody, "totalRechargeThisMonth"));

                // Clean up the data
                prepaidCustomerDetails = cleanPrepaidCustomerDetails(prepaidCustomerDetails);

                if (!prepaidCustomerDetails.isEmpty()) {
                    result.put("prepaid_customer_details", prepaidCustomerDetails);
                    System.out.println("✅ Added prepaid customer details: " + prepaidCustomerDetails.keySet());
                }

                // Extract recharge tokens
                List<Map<String, String>> transactions = extractTransactionsWithExactPatterns(responseBody);
                if (!transactions.isEmpty()) {
                    result.put("recharge_history", transactions);
                    result.put("total_recharges", transactions.size());
                    System.out.println("✅ Added " + transactions.size() + " recharge transactions");
                }

            }
        } catch (Exception e) {
            System.out.println("❌ Error extracting SERVER1 data: " + e.getMessage());
        }
    }

    private Map<String, String> cleanPrepaidCustomerDetails(Map<String, String> details) {
        Map<String, String> cleaned = new HashMap<>();

        for (Map.Entry<String, String> entry : details.entrySet()) {
            if (entry.getValue() != null &&
                    !entry.getValue().equals("N/A") &&
                    !entry.getValue().isEmpty() &&
                    !entry.getValue().equals("{}")) {

                // Format specific fields
                String value = entry.getValue();
                switch (entry.getKey()) {
                    case "Lock Status":
    if (value.equals("{}")) {
        value = "UNLOCKED";
    } else if (value.contains("UNLOCKED")) {
        value = "UNLOCKED"; 
    } else if (value.contains("LOCKED")) {
        value = "LOCKED";
    } else {
        value = "UNKNOWN"; // Fallback
    }
    break;
                    case "Account Type":
                        value = "Active (Prepaid)";
                        break;
                    case "Last Recharge Amount":
                        if (!value.equals("0") && !value.equals("0.0")) {
                            value = "৳" + value;
                        }
                        break;
                    case "Installation Date":
                        value = formatInstallationDate(value);
                        break;
                }

                cleaned.put(entry.getKey(), value);
            }
        }

        return cleaned;
    }

    private String formatInstallationDate(String dateStr) {
        try {
            if (dateStr.contains("-")) {
                String[] parts = dateStr.split("-");
                if (parts.length >= 3) {
                    String day = parts[2].length() > 2 ? parts[2].substring(0, 2) : parts[2];
                    String month = getMonthName(parts[1]);
                    String year = parts[0].length() > 2 ? parts[0].substring(2) : parts[0];
                    return day + "-" + month + "-" + year;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return dateStr;
    }

    private String getMonthName(String monthNum) {
        Map<String, String> months = new HashMap<>();
        months.put("01", "Jan"); months.put("02", "Feb"); months.put("03", "Mar");
        months.put("04", "Apr"); months.put("05", "May"); months.put("06", "Jun");
        months.put("07", "Jul"); months.put("08", "Aug"); months.put("09", "Sep");
        months.put("10", "Oct"); months.put("11", "Nov"); months.put("12", "Dec");
        return months.getOrDefault(monthNum, monthNum);
    }
    private String getStringValue(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception e) {
            System.out.println("❌ Error getting string value for key: " + key + " - " + e.getMessage());
        }
        return "N/A";
    }

    private void processBillInformation(Map<String, Object> result) {
        try {
            if (result.containsKey("bill_info")) {
                List<Map<String, Object>> billInfo = (List<Map<String, Object>>) result.get("bill_info");

                // Create detailed bill summary
                Map<String, Object> billSummary = new HashMap<>();
                billSummary.put("total_bills", billInfo.size());

                // Get latest bill details (first bill in the list)
                if (!billInfo.isEmpty()) {
                    Map<String, Object> latestBill = billInfo.get(0);
                    billSummary.put("latest_bill_date", formatBillMonth(getStringValue(latestBill, "BILL_MONTH")));
                    billSummary.put("latest_bill_number", getStringValue(latestBill, "BILL_NO"));
                    billSummary.put("latest_consumption", getDoubleValue(latestBill, "CONS_KWH_SR"));
                    billSummary.put("latest_total_amount", getDoubleValue(latestBill, "CURRENT_BILL"));
                    billSummary.put("latest_balance", getDoubleValue(latestBill, "BALANCE"));
                }

                // Get first bill details (last bill in the list)
                if (!billInfo.isEmpty()) {
                    Map<String, Object> firstBill = billInfo.get(billInfo.size() - 1);
                    billSummary.put("first_bill_date", formatBillMonth(getStringValue(firstBill, "BILL_MONTH")));
                }

                // Calculate totals from all bills
                double totalAmount = 0;
                double totalPaid = 0;

                for (Map<String, Object> bill : billInfo) {
                    totalAmount += getDoubleValue(bill, "CURRENT_BILL");
                    totalPaid += getDoubleValue(bill, "PAID_AMT");
                }

                billSummary.put("total_amount", totalAmount);
                billSummary.put("total_paid", totalPaid);

                // Extract all bills for detailed display
                List<Map<String, Object>> allBills = new ArrayList<>();
                for (Map<String, Object> bill : billInfo) {
                    Map<String, Object> billDetail = new HashMap<>();
                    billDetail.put("bill_month", formatBillMonth(getStringValue(bill, "BILL_MONTH")));
                    billDetail.put("bill_number", getStringValue(bill, "BILL_NO"));
                    billDetail.put("consumption", getDoubleValue(bill, "CONS_KWH_SR"));
                    billDetail.put("total_amount", getDoubleValue(bill, "CURRENT_BILL"));
                    billDetail.put("balance", getDoubleValue(bill, "BALANCE"));
                    billDetail.put("due_date", formatDate(getStringValue(bill, "INVOICE_DUE_DATE")));
                    allBills.add(billDetail);
                }

                billSummary.put("all_bills", allBills);
                result.put("bill_summary", billSummary);

                System.out.println("✅ BILL SUMMARY: " + billInfo.size() + " bills | Total Amount: ৳" + totalAmount + " | Total Paid: ৳" + totalPaid);
            } else {
                System.out.println("❌ CLEAN SERVER2: No billInfo found in SERVER2 data");
            }
        } catch (Exception e) {
            System.out.println("❌ CLEAN SERVER2: Error processing billInfo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add these helper methods if you don't have them:
    private String formatBillMonth(String billMonth) {
        try {
            if (billMonth != null && billMonth.length() == 6) {
                String year = billMonth.substring(0, 4);
                String month = billMonth.substring(4, 6);

                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int monthIndex = Integer.parseInt(month) - 1;
                if (monthIndex >= 0 && monthIndex < months.length) {
                    return months[monthIndex] + " " + year;
                }
            }
            return billMonth;
        } catch (Exception e) {
            return billMonth;
        }
    }

    private String formatDate(String dateString) {
        try {
            if (dateString != null && !dateString.isEmpty()) {
                // Add your date parsing and formatting logic here
                // Example: Convert "2024-01-15" to "15 Jan 2024"
                return dateString; // Return as is for now, implement your formatting
            }
            return "";
        } catch (Exception e) {
            return dateString;
        }
    }

    // ✅ FIXED: Fetch meter reading data from bill API
    private Map<String, Object> fetchMeterReadingData(String consumerNumber, String locationCode, String billMonth) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate required parameters
            if (consumerNumber == null || locationCode == null || billMonth == null) {
                System.out.println("❌ METER READING API: Missing required parameters");
                return result;
            }

            System.out.println("🔍 METER READING API: Fetching for " + consumerNumber +
                    ", Location: " + locationCode + ", Month: " + billMonth);

            String url = "https://billonwebapi.bpdb.gov.bd/api/BillInformation/GenerateBill/" +
                    consumerNumber + "/" + locationCode + "/" + billMonth;

            System.out.println("🔍 API URL: " + url);

            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            System.out.println("🔍 API Response Code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String responseBody = response.toString();
                System.out.println("🔍 API Response: " + responseBody);

                JSONObject apiResponse = new JSONObject(responseBody);

                // ✅ FIX: Check for status 0 OR 1 (both indicate success in different APIs)
                if (apiResponse.has("status")) {
                    int status = apiResponse.getInt("status");
                    System.out.println("🔍 API Status: " + status);

                    // ✅ FIX: Accept both status 0 and 1 as success
                    if (status == 0 || status == 1) {
                        if (apiResponse.has("content")) {
                            Object content = apiResponse.get("content");

                            if (content instanceof JSONArray) {
                                JSONArray contentArray = (JSONArray) content;
                                if (contentArray.length() > 0) {
                                    JSONObject billData = contentArray.getJSONObject(0);
                                    result = extractMeterReadings(billData);
                                    System.out.println("✅ METER READING API: Successfully extracted " + result.size() + " readings");
                                } else {
                                    System.out.println("❌ METER READING API: Empty content array");
                                }
                            } else {
                                System.out.println("❌ METER READING API: Content is not an array");
                            }
                        } else {
                            System.out.println("❌ METER READING API: No content field in response");
                        }
                    } else {
                        String message = apiResponse.optString("message", "Unknown error");
                        System.out.println("❌ METER READING API: API returned error status: " + status + ", message: " + message);
                    }
                } else {
                    System.out.println("❌ METER READING API: No status field in response");
                }
            } else {
                System.out.println("❌ METER READING API: HTTP Error " + responseCode);
            }

        } catch (Exception e) {
            System.out.println("❌ METER READING API ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // ✅ FIXED: Enhanced meter readings extraction with validation
    private Map<String, Object> extractMeterReadings(JSONObject billData) {
        Map<String, Object> readings = new HashMap<>();

        try {
            System.out.println("🔍 EXTRACTING METER READINGS FROM: " + billData.toString());

            // Extract the key meter readings with validation
            double srReading = billData.optDouble("CLS_KWH_SR_RDNG", 0);
            double pkReading = billData.optDouble("CLS_KWH_PK_RDNG", 0);
            double ofpkReading = billData.optDouble("CLS_KWH_OFPK_RDNG", 0);

            // Only add readings that have valid values
            if (srReading > 0) {
                readings.put("CLS_KWH_SR_RDNG", srReading);
            }
            if (pkReading > 0) {
                readings.put("CLS_KWH_PK_RDNG", pkReading);
            }
            if (ofpkReading > 0) {
                readings.put("CLS_KWH_OFPK_RDNG", ofpkReading);
            }

            // Extract opening readings
            double openSr = billData.optDouble("OPN_KWH_SR_RDNG", 0);
            double openPk = billData.optDouble("OPN_KWH_PK_RDNG", 0);
            double openOfpk = billData.optDouble("OPN_KWH_OFPK_RDNG", 0);

            if (openSr > 0) readings.put("OPN_KWH_SR_RDNG", openSr);
            if (openPk > 0) readings.put("OPN_KWH_PK_RDNG", openPk);
            if (openOfpk > 0) readings.put("OPN_KWH_OFPK_RDNG", openOfpk);

            // Extract consumption values
            double consSr = billData.optDouble("CONS_KWH_SR", 0);
            double consPk = billData.optDouble("CONS_KWH_PK", 0);
            double consOfpk = billData.optDouble("CONS_KWH_OFPK", 0);

            if (consSr > 0) readings.put("CONS_KWH_SR", consSr);
            if (consPk > 0) readings.put("CONS_KWH_PK", consPk);
            if (consOfpk > 0) readings.put("CONS_KWH_OFPK", consOfpk);

            // Extract reading dates
            String prevDate = billData.optString("PREV_READING_DATE", "N/A");
            String currDate = billData.optString("CURR_READING_DATE", "N/A");

            if (!prevDate.equals("N/A") && !prevDate.equals("null")) {
                readings.put("PREV_READING_DATE", prevDate);
            }
            if (!currDate.equals("N/A") && !currDate.equals("null")) {
                readings.put("CURR_READING_DATE", currDate);
            }

            System.out.println("✅ EXTRACTED " + readings.size() + " METER READINGS: " + readings);

        } catch (Exception e) {
            System.out.println("❌ Error extracting meter readings: " + e.getMessage());
            e.printStackTrace();
        }

        return readings;
    }

    // ✅ FIXED: Get latest bill month with proper formatting
    private String getLatestBillMonth(Map<String, Object> result) {
        try {
            if (result.containsKey("bill_info")) {
                List<Map<String, Object>> billInfo = (List<Map<String, Object>>) result.get("bill_info");
                if (billInfo != null && billInfo.size() > 0) {
                    // Get the most recent bill (first in list)
                    String billMonth = (String) billInfo.get(0).get("BILL_MONTH");
                    System.out.println("🔍 Raw bill month from bill_info: " + billMonth);

                    if (billMonth != null && !billMonth.equals("N/A") && !billMonth.isEmpty()) {
                        // Format: "2025-10-01" -> "202510"
                        String formatted = formatBillMonthForAPI(billMonth);
                        System.out.println("🔍 Formatted bill month: " + formatted);
                        return formatted;
                    }
                }
            }

            // Fallback: Check for BILL_CYCLE_CODE in the API response
            if (result.containsKey("SERVER2_data")) {
                try {
                    Object server2Data = result.get("SERVER2_data");
                    if (server2Data instanceof JSONObject) {
                        JSONObject server2Json = (JSONObject) server2Data;
                        if (server2Json.has("billInfo")) {
                            JSONArray billArray = server2Json.getJSONArray("billInfo");
                            if (billArray.length() > 0) {
                                JSONObject firstBill = billArray.getJSONObject(0);
                                String billCycle = firstBill.optString("BILL_CYCLE_CODE", null);
                                if (billCycle != null) {
                                    System.out.println("🔍 Found BILL_CYCLE_CODE: " + billCycle);
                                    return billCycle; // Already in "202510" format
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("❌ Error extracting BILL_CYCLE_CODE: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Error getting latest bill month: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("❌ No valid bill month found");
        return null;
    }

    // ✅ FIXED: Format bill month for API
    private String formatBillMonthForAPI(String billMonth) {
        if (billMonth == null || billMonth.equals("N/A")) {
            return null;
        }

        try {
            // Handle "2025-10-01" format
            if (billMonth.contains("-")) {
                String[] parts = billMonth.split("-");
                if (parts.length >= 2) {
                    String year = parts[0];
                    String month = parts[1];
                    // Ensure 2-digit month
                    if (month.length() == 1) {
                        month = "0" + month;
                    }
                    return year + month; // Returns "202510"
                }
            }
            // Handle "202510" format (already correct)
            else if (billMonth.length() == 6) {
                return billMonth;
            }
        } catch (Exception e) {
            System.out.println("❌ Error formatting bill month: " + billMonth + ", error: " + e.getMessage());
        }

        return null;
    }

    // ✅ FIXED: Get location code from customer info
    private String getLocationCode(Map<String, Object> result) {
        try {
            // Try customer_info first
            if (result.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) result.get("customer_info");
                if (customerInfo != null && customerInfo.containsKey("Location Code")) {
                    String locationCode = customerInfo.get("Location Code");
                    System.out.println("🔍 Found Location Code in customer_info: " + locationCode);
                    return locationCode != null ? locationCode.toLowerCase() : null; // Convert to lowercase
                }
            }

            // Try SERVER3 data
            if (result.containsKey("SERVER3_customer_info")) {
                Map<String, String> server3Info = (Map<String, String>) result.get("SERVER3_customer_info");
                if (server3Info != null && server3Info.containsKey("Location Code")) {
                    String locationCode = server3Info.get("Location Code");
                    System.out.println("🔍 Found Location Code in SERVER3: " + locationCode);
                    return locationCode != null ? locationCode.toLowerCase() : null; // Convert to lowercase
                }
            }

            // Check raw data
            if (result.containsKey("LOCATION_CODE")) {
                String locationCode = (String) result.get("LOCATION_CODE");
                System.out.println("🔍 Found Location Code in raw data: " + locationCode);
                return locationCode != null ? locationCode.toLowerCase() : null;
            }

        } catch (Exception e) {
            System.out.println("❌ Error getting location code: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("❌ No location code found in any data source");
        return null;
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

    // Helper methods
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

    // ✅ WORKING FALLBACK: Using the same pattern as ApplicationFormHelper
    private void supplementWithSERVER2Data(Map<String, Object> result) {
        try {
            System.out.println("🔍 APPLYING SERVER2 FALLBACK (ApplicationFormHelper pattern)...");

            if (result.containsKey("SERVER3_customer_info") && result.containsKey("SERVER2_data")) {
                @SuppressWarnings("unchecked")
                Map<String, String> server3Info = (Map<String, String>) result.get("SERVER3_customer_info");
                Object server2Data = result.get("SERVER2_data");

                // Extract customer info from SERVER2 using the same pattern as ApplicationFormHelper
                Map<String, String> server2CustomerInfo = extractCustomerInfoFromSERVER2ForFallback(server2Data);

                if (server2CustomerInfo != null && !server2CustomerInfo.isEmpty()) {
                    System.out.println("✅ FOUND SERVER2 DATA WITH " + server2CustomerInfo.size() + " FIELDS FOR FALLBACK");

                    // ✅ CORRECTED: Use proper field mapping that matches MainActivity
                    applyFieldFallback(server3Info, server2CustomerInfo, "Customer Name", "CUSTOMER_NAME");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Father Name", "FATHER_NAME");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Customer Address", "ADDRESS"); // ← Maps "ADDRESS" to "Customer Address"
                    applyFieldFallback(server3Info, server2CustomerInfo, "Meter Number", "METER_NUM");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Location Code", "LOCATION_CODE");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Area Code", "AREA_CODE");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Bill Group", "BILL_GROUP");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Book Number", "BOOK"); // ← Maps "BOOK" to "Book Number"
                    applyFieldFallback(server3Info, server2CustomerInfo, "Tariff", "TARIFF");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Sanctioned Load", "SANCTIONED_LOAD");
                    applyFieldFallback(server3Info, server2CustomerInfo, "Walk Order", "WALKING_SEQUENCE"); // ← Maps "WALKING_SEQUENCE" to "Walk Order"
                    applyFieldFallback(server3Info, server2CustomerInfo, "Meter Condition", "METER_CONDITION");

                    System.out.println("✅ SERVER2 FALLBACK COMPLETED");
                } else {
                    System.out.println("⚠️ SERVER2 data extraction returned empty result");
                }
            } else {
                System.out.println("⚠️ Cannot apply SERVER2 fallback - missing required data");
                if (!result.containsKey("SERVER3_customer_info")) {
                    System.out.println("❌ MISSING: SERVER3_customer_info");
                }
                if (!result.containsKey("SERVER2_data")) {
                    System.out.println("❌ MISSING: SERVER2_data");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error in SERVER2 fallback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ EXACT SAME PATTERN as ApplicationFormHelper.extractCustomerInfoFromSERVER2
    private Map<String, String> extractCustomerInfoFromSERVER2ForFallback(Object server2Data) {
        try {
            Map<String, String> customerInfo = new HashMap<>();

            if (server2Data instanceof JSONObject) {
                JSONObject jsonData = (JSONObject) server2Data;

                // ✅ EXACT SAME LOGIC as ApplicationFormHelper
                if (jsonData.has("customerInfo")) {
                    JSONArray customerInfoArray = jsonData.getJSONArray("customerInfo");
                    if (customerInfoArray.length() > 0) {
                        // Handle the double array structure: customerInfo[0][0] - SAME PATTERN
                        JSONArray firstArray = customerInfoArray.getJSONArray(0);
                        if (firstArray.length() > 0) {
                            JSONObject firstCustomer = firstArray.getJSONObject(0);

                            System.out.println("✅ EXTRACTING FROM SERVER2 customerInfo (ApplicationFormHelper pattern)");

                            // ✅ CORRECTED: Use EXACT field names from MainActivity
                            customerInfo.put("CUSTOMER_NAME", firstCustomer.optString("CUSTOMER_NAME", ""));
                            customerInfo.put("FATHER_NAME", firstCustomer.optString("FATHER_NAME", ""));
                            customerInfo.put("ADDRESS", firstCustomer.optString("ADDRESS", "")); // ← "ADDRESS" not "CUSTOMER_ADDRESS"
                            customerInfo.put("METER_NUM", firstCustomer.optString("METER_NUM", ""));
                            customerInfo.put("LOCATION_CODE", firstCustomer.optString("LOCATION_CODE", ""));
                            customerInfo.put("AREA_CODE", firstCustomer.optString("AREA_CODE", ""));
                            customerInfo.put("BILL_GROUP", firstCustomer.optString("BILL_GROUP", ""));
                            customerInfo.put("BOOK", firstCustomer.optString("BOOK", "")); // ← "BOOK" not "BOOK_NUMBER"
                            customerInfo.put("TARIFF", firstCustomer.optString("TARIFF", ""));
                            customerInfo.put("SANCTIONED_LOAD", firstCustomer.optString("SANCTIONED_LOAD", ""));
                            customerInfo.put("WALKING_SEQUENCE", firstCustomer.optString("WALKING_SEQUENCE", "")); // ← "WALKING_SEQUENCE" not "WALK_ORDER"
                            customerInfo.put("METER_CONDITION", firstCustomer.optString("METER_CONDITION", ""));

                            // Clean empty fields - SAME PATTERN as ApplicationFormHelper
                            for (Map.Entry<String, String> entry : customerInfo.entrySet()) {
                                String value = entry.getValue();
                                if (value == null || value.equals("null") || value.isEmpty()) {
                                    customerInfo.put(entry.getKey(), "");
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("✅ EXTRACTED " + customerInfo.size() + " FIELDS FROM SERVER2 FOR FALLBACK");
            return customerInfo;

        } catch (Exception e) {
            System.out.println("❌ Error extracting customer info from SERVER2: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ✅ HELPER: Apply fallback for individual fields (same logic pattern)
    private void applyFieldFallback(Map<String, String> server3Info, Map<String, String> server2CustomerInfo,
                                    String fieldName, String server2FieldKey) {
        try {
            String server3Value = server3Info.get(fieldName);
            String server2Value = server2CustomerInfo.get(server2FieldKey);

            // ✅ SAME LOGIC: If SERVER3 has empty value but SERVER2 has valid data
            if ((server3Value == null || server3Value.isEmpty() || server3Value.equals("N/A"))
                    && server2Value != null && !server2Value.isEmpty()) {

                server3Info.put(fieldName, server2Value);
                System.out.println("🔄 REPLACED '" + fieldName + "' from SERVER2: " + server2Value);
            }
        } catch (Exception e) {
            System.out.println("❌ Error applying fallback for " + fieldName + ": " + e.getMessage());
        }
    }
    // ✅ NEW: Create unified customer information without categories
    private void createUnifiedCustomerInfo(Map<String, Object> result) {
        try {
            if (result.containsKey("customer_info")) {
                Map<String, String> customerInfo = (Map<String, String>) result.get("customer_info");

                // Create a new map with fields in the desired order
                Map<String, String> unifiedInfo = new LinkedHashMap<>();

                // Personal Information
                addIfValid(unifiedInfo, customerInfo, "Customer Name");
                addIfValid(unifiedInfo, customerInfo, "Father Name");
                addIfValid(unifiedInfo, customerInfo, "Customer Address");

                // Meter Information
                addIfValid(unifiedInfo, customerInfo, "Meter Number");
                addIfValid(unifiedInfo, customerInfo, "Meter Condition");
                addIfValid(unifiedInfo, customerInfo, "Meter Status");
                addIfValid(unifiedInfo, customerInfo, "Connection Date");

                // Billing Information
                addIfValid(unifiedInfo, customerInfo, "Customer Number");
                addIfValid(unifiedInfo, customerInfo, "Location Code");
                addIfValid(unifiedInfo, customerInfo, "Area Code");
                addIfValid(unifiedInfo, customerInfo, "Bill Group");
                addIfValid(unifiedInfo, customerInfo, "Book Number");
                addIfValid(unifiedInfo, customerInfo, "Account_Number");
                addIfValid(unifiedInfo, customerInfo, "Walk Order");

                // Tariff & Load
                addIfValid(unifiedInfo, customerInfo, "Tariff");
                addIfValid(unifiedInfo, customerInfo, "Sanctioned Load");
                addIfValid(unifiedInfo, customerInfo, "Connection Category");
                addIfValid(unifiedInfo, customerInfo, "Account Type");

                // Technical Information
                addIfValid(unifiedInfo, customerInfo, "Usage Type");
                addIfValid(unifiedInfo, customerInfo, "Description");
                addIfValid(unifiedInfo, customerInfo, "Start Bill Cycle");
                addIfValid(unifiedInfo, customerInfo, "Division");
                addIfValid(unifiedInfo, customerInfo, "Sub Division");
                addIfValid(unifiedInfo, customerInfo, "Phone");

                // Meter Readings & Balance
                addIfValid(unifiedInfo, customerInfo, "Current Reading SR");
                addIfValid(unifiedInfo, customerInfo, "Last Bill Reading SR");
                addIfValid(unifiedInfo, customerInfo, "Last Bill Reading OF PK");
                addIfValid(unifiedInfo, customerInfo, "Last Bill Reading PK");
                addIfValid(unifiedInfo, customerInfo, "Arrear Amount");
                addIfValid(unifiedInfo, customerInfo, "Total Balance");

                // Replace the original customer_info with unified version
                result.put("customer_info", unifiedInfo);
                System.out.println("✅ UNIFIED CUSTOMER INFO: Created with " + unifiedInfo.size() + " fields");
            }
        } catch (Exception e) {
            System.out.println("❌ Error creating unified customer info: " + e.getMessage());
        }
    }

    // Helper method to add valid fields
    private void addIfValid(Map<String, String> target, Map<String, String> source, String key) {
        if (source.containsKey(key) && isValidValue(source.get(key))) {
            target.put(key, source.get(key));
        }
    }

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

    // ✅ NEW: Add this improved validation method
    private boolean isValidValue(String value) {
        return value != null &&
                !value.trim().isEmpty() &&
                !value.equalsIgnoreCase("N/A") &&
                !value.equalsIgnoreCase("null") &&
                !value.equalsIgnoreCase("undefined") &&
                !value.equals("{}");
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