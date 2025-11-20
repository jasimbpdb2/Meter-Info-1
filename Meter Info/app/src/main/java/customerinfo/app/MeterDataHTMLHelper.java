package customerinfo.app;

import android.content.Context;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * MeterDataHTMLHelper
 * - provides processDataForHTMLDisplay(Map, meterNumber, type, subtype)
 * - fills sensible defaults and flattens merged data into a JSON-friendly map
 *
 * MainActivity expects: MeterDataHTMLHelper helper = new MeterDataHTMLHelper();
 * Map<String,Object> processed = helper.processDataForHTMLDisplay(rawData, inputNumber, type, subType);
 */
public class MeterDataHTMLHelper {

    public MeterDataHTMLHelper() { }

    @SuppressWarnings("unchecked")
    public Map<String, Object> processDataForHTMLDisplay(Map<String, Object> raw, String meterNumber, String type, String subType) {
        Map<String, Object> out = new HashMap<>();

        // copy some top-level fields
        out.put("meter_number", raw.getOrDefault("meter_number", meterNumber));
        out.put("consumer_number", raw.getOrDefault("consumer_number", raw.get("customer_number")));
        out.put("search_input", raw.getOrDefault("search_input", meterNumber));
        out.put("search_type", raw.getOrDefault("search_type", type));
        out.put("timestamp", raw.getOrDefault("timestamp", new Date().toString()));

        // merged data: MainActivity's mergeSERVERData produced merged structures under keys
        Map<String, Object> merged = null;
        try {
            // If MainActivity already added merged data, prefer it
            if (raw.containsKey("customer_info") && raw.get("customer_info") instanceof Map) {
                // some callers might put customer_info at root
                merged = new HashMap<>();
                merged.put("customer_info", raw.get("customer_info"));
            } else if (raw.containsKey("merged")) {
                merged = (Map<String,Object>) raw.get("merged");
            } else {
                // Try calling mergeSERVERData equivalent -- but we don't re-run; prefer SERVER2/SERVER3 cleaned data
                merged = new HashMap<>();
                if (raw.containsKey("SERVER2_data")) merged.put("SERVER2_raw", raw.get("SERVER2_data"));
                if (raw.containsKey("SERVER3_data")) merged.put("SERVER3_raw", raw.get("SERVER3_data"));
            }
        } catch (Exception e) {
            merged = new HashMap<>();
        }

        // customer_info / balance_info / transactions / bills
        // Try to extract from known keys MainActivity uses:
        Map<String, String> customerInfo = collectMapStringString(raw, "customer_info");
        if (customerInfo.isEmpty() && merged.containsKey("customer_info")) {
            Object mm = merged.get("customer_info");
            if (mm instanceof Map) customerInfo = (Map<String,String>) mm;
        }
        out.put("customer_info", customerInfo);

        Map<String, String> balanceInfo = collectMapStringString(raw, "balance_info");
        if (balanceInfo.isEmpty() && merged.containsKey("balance_info")) {
            Object mm = merged.get("balance_info");
            if (mm instanceof Map) balanceInfo = (Map<String,String>)mm;
        }
        out.put("balance_info", balanceInfo);

        // recent transactions/tokens
        List<Map<String, String>> transactions = new ArrayList<>();
        if (raw.containsKey("SERVER1_data")) {
            // MainActivity parses tokens into recent_transactions when showing console text,
            // but it also put cleaned value under cleaned SERVER1 if used earlier. Try common keys:
            if (raw.containsKey("recent_transactions") && raw.get("recent_transactions") instanceof List) {
                transactions = (List<Map<String,String>>) raw.get("recent_transactions");
            }
            // else try server1 cleaned -> in your flow cleanSERVER1Data returns recent_transactions
            if (raw.get("SERVER1_data") instanceof Map) {
                Map s1 = (Map) raw.get("SERVER1_data");
                if (s1.containsKey("recent_transactions") && s1.get("recent_transactions") instanceof List) {
                    transactions = (List<Map<String,String>>) s1.get("recent_transactions");
                }
            }
        }
        // fallback to top-level key names
        if (transactions.isEmpty()) {
            if (raw.containsKey("recent_transactions") && raw.get("recent_transactions") instanceof List) {
                transactions = (List<Map<String,String>>) raw.get("recent_transactions");
            }
        }
        out.put("recent_transactions", transactions);

        // bills: expect bill_info_raw (JSONArray) or billInfo
        Object billsObj = raw.get("bill_info_raw");
        if (billsObj == null) billsObj = raw.get("billInfo");
        if (billsObj instanceof org.json.JSONArray) {
            // convert to java list of maps
            List<Map<String, Object>> bills = new ArrayList<>();
            org.json.JSONArray arr = (org.json.JSONArray) billsObj;
            for (int i = 0; i < arr.length(); i++) {
                try {
                    org.json.JSONObject o = arr.getJSONObject(i);
                    Map<String, Object> m = jsonObjectToMap(o);
                    bills.add(m);
                } catch (Exception ignored) {}
            }
            out.put("bill_info_raw", bills);
        } else if (billsObj instanceof List) {
            out.put("bill_info_raw", billsObj);
        } else {
            out.put("bill_info_raw", new ArrayList<>());
        }

        // merge any other helpful keys
        if (raw.containsKey("data_source")) out.put("data_source", raw.get("data_source"));

        // this final map will be serialized by MainActivity into JSON and embedded in the HTML as __METER_DATA__
        return out;
    }

    private Map<String,String> collectMapStringString(Map<String,Object> raw, String key) {
        Map<String,String> map = new HashMap<>();
        try {
            if (raw.containsKey(key) && raw.get(key) instanceof Map) {
                Map m = (Map) raw.get(key);
                for (Object k : m.keySet()) {
                    Object v = m.get(k);
                    map.put(String.valueOf(k), v==null?"":String.valueOf(v));
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private Map<String,Object> jsonObjectToMap(JSONObject o) {
        Map<String,Object> m = new HashMap<>();
        try {
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Object v = o.get(k);
                m.put(k, v);
            }
        } catch (Exception ignored) {}
        return m;
    }
}