package customerinfo.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class TemplateHelper {

    companion object {
        const val TEMPLATE_POSTPAID = "postpaid_template.html"
        const val TEMPLATE_PREPAID = "prepaid_template.html"

        // Postpaid template data structure
        data class PostpaidData(
            val customerNumber: String,
            val multipleCustomers: MultipleCustomersData? = null,
            val singleCustomer: SingleCustomerData? = null,
            val billInfo: BillInfoData? = null,
            val balanceInfo: BalanceInfoData? = null,
            val billSummary: BillSummaryData? = null,
            val error: ErrorData? = null
        )

        data class MultipleCustomersData(
            val customerCount: Int,
            val customers: List<CustomerData>
        )

        data class CustomerData(
            val index: Int,
            val customerNumber: String,
            val customerInfo: List<KeyValuePair>
        )

        data class SingleCustomerData(
            val customerInfo: List<KeyValuePair>
        )

        data class BillInfoData(
            val bills: List<BillData>
        )

        data class BillData(
            val billMonth: String,
            val billNo: String,
            val consumption: String,
            val currentBill: String,
            val dueDate: String,
            val paidAmt: String,
            val receiptDate: String,
            val balance: String
        )

        data class BalanceInfoData(
            val fields: List<KeyValuePair>
        )

        data class BillSummaryData(
            val fields: List<KeyValuePair>
        )

        // Prepaid template data structure
        data class PrepaidData(
            val meterNumber: String,
            val consumerNumber: String,
            val prepaidCustomerInfo: PrepaidCustomerInfoData? = null,
            val tokens: TokensData? = null,
            val postpaidCustomerInfo: CustomerInfoData? = null,
            val billInfo: BillInfoData? = null,
            val balanceInfo: BalanceInfoData? = null,
            val billSummary: BillSummaryData? = null,
            val error: ErrorData? = null
        )

        data class PrepaidCustomerInfoData(
            val fields: List<KeyValuePair>
        )

        data class TokensData(
            val tokenList: List<TokenData>
        )

        data class TokenData(
            val index: Int,
            val token: String,
            val date: String,
            val amount: String,
            val operator: String,
            val sequence: String
        )

        data class CustomerInfoData(
            val fields: List<KeyValuePair>
        )

        // Common data structures
        data class KeyValuePair(
            val key: String,
            val value: String
        )

        data class ErrorData(
            val errorMessage: String
        )

        // Convert MainActivity result to PostpaidData
        fun convertToPostpaidData(result: Map<String, Any>): PostpaidData {
            return try {
                val customerNumber = result["customer_number"]?.toString() ?: "N/A"
                
                // Check if it's multiple customers (meter lookup)
                if (result.containsKey("customer_results")) {
                    val customerNumbers = result["customer_numbers"] as? List<String> ?: emptyList()
                    val customerResults = result["customer_results"] as? List<Map<String, Any>> ?: emptyList()
                    
                    val customers = mutableListOf<CustomerData>()
                    for ((index, customerResult) in customerResults.withIndex()) {
                        val mergedData = MainActivity().mergeSERVERData(customerResult as Map<String, Object>)
                        if (mergedData != null) {
                            val customerInfo = extractCustomerInfo(mergedData)
                            customers.add(
                                CustomerData(
                                    index = index + 1,
                                    customerNumber = customerNumbers.getOrNull(index) ?: "N/A",
                                    customerInfo = customerInfo
                                )
                            )
                        }
                    }
                    
                    PostpaidData(
                        customerNumber = customerNumber,
                        multipleCustomers = MultipleCustomersData(
                            customerCount = customers.size,
                            customers = customers
                        ),
                        billInfo = extractBillInfo(result),
                        balanceInfo = extractBalanceInfo(result),
                        billSummary = extractBillSummary(result),
                        error = extractError(result)
                    )
                } else {
                    // Single customer
                    val mergedData = MainActivity().mergeSERVERData(result as Map<String, Object>)
                    PostpaidData(
                        customerNumber = customerNumber,
                        singleCustomer = SingleCustomerData(
                            customerInfo = extractCustomerInfo(mergedData)
                        ),
                        billInfo = extractBillInfo(result),
                        balanceInfo = extractBalanceInfo(result),
                        billSummary = extractBillSummary(result),
                        error = extractError(result)
                    )
                }
            } catch (e: Exception) {
                PostpaidData(
                    customerNumber = "N/A",
                    error = ErrorData("Error converting data: ${e.message}")
                )
            }
        }

        // Convert MainActivity result to PrepaidData
        fun convertToPrepaidData(result: Map<String, Any>): PrepaidData {
            return try {
                val meterNumber = result["meter_number"]?.toString() ?: "N/A"
                val consumerNumber = result["consumer_number"]?.toString() ?: "N/A"
                
                // Extract SERVER1 data for prepaid info
                val SERVER1Data = result["SERVER1_data"]
                val cleanedSERVER1Data = if (SERVER1Data != null) {
                    MainActivity().cleanSERVER1Data(SERVER1Data)
                } else {
                    emptyMap<String, Any>()
                }
                
                // Extract merged data for customer info
                val mergedData = MainActivity().mergeSERVERData(result as Map<String, Object>)
                
                PrepaidData(
                    meterNumber = meterNumber,
                    consumerNumber = consumerNumber,
                    prepaidCustomerInfo = extractPrepaidCustomerInfo(cleanedSERVER1Data),
                    tokens = extractTokens(cleanedSERVER1Data),
                    postpaidCustomerInfo = extractCustomerInfoData(mergedData),
                    billInfo = extractBillInfo(result),
                    balanceInfo = extractBalanceInfo(result),
                    billSummary = extractBillSummary(result),
                    error = extractError(result)
                )
            } catch (e: Exception) {
                PrepaidData(
                    meterNumber = "N/A",
                    consumerNumber = "N/A",
                    error = ErrorData("Error converting data: ${e.message}")
                )
            }
        }

        // Extract customer info from merged data
        private fun extractCustomerInfo(mergedData: Map<String, Any>?): List<KeyValuePair> {
            val fields = mutableListOf<KeyValuePair>()
            if (mergedData == null) return fields
            
            try {
                val customerInfo = mergedData["customer_info"] as? Map<String, String>
                customerInfo?.forEach { (key, value) ->
                    if (isValidValue(value)) {
                        fields.add(KeyValuePair(key, value))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return fields
        }

        // Extract prepaid customer info from SERVER1 data
        private fun extractPrepaidCustomerInfo(cleanedSERVER1Data: Map<String, Any>): PrepaidCustomerInfoData {
            val fields = mutableListOf<KeyValuePair>()
            
            try {
                val customerInfo = cleanedSERVER1Data["customer_info"] as? Map<String, String>
                customerInfo?.forEach { (key, value) ->
                    if (isValidValue(value)) {
                        fields.add(KeyValuePair(key, value))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return PrepaidCustomerInfoData(fields = fields)
        }

        // Extract tokens from SERVER1 data
        private fun extractTokens(cleanedSERVER1Data: Map<String, Any>): TokensData? {
            val tokenList = mutableListOf<TokenData>()
            
            try {
                val transactions = cleanedSERVER1Data["recent_transactions"] as? List<Map<String, String>>
                transactions?.take(3)?.forEachIndexed { index, transaction ->
                    tokenList.add(
                        TokenData(
                            index = index + 1,
                            token = transaction["Tokens"] ?: "N/A",
                            date = transaction["Date"] ?: "N/A",
                            amount = transaction["Amount"] ?: "N/A",
                            operator = transaction["Operator"] ?: "N/A",
                            sequence = transaction["Sequence"] ?: "N/A"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return if (tokenList.isNotEmpty()) TokensData(tokenList) else null
        }

        // Extract customer info as CustomerInfoData
        private fun extractCustomerInfoData(mergedData: Map<String, Any>?): CustomerInfoData {
            val fields = mutableListOf<KeyValuePair>()
            if (mergedData == null) return CustomerInfoData(fields)
            
            try {
                val customerInfo = mergedData["customer_info"] as? Map<String, String>
                customerInfo?.forEach { (key, value) ->
                    if (isValidValue(value)) {
                        fields.add(KeyValuePair(key, value))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return CustomerInfoData(fields = fields)
        }

        // Extract bill info
        private fun extractBillInfo(result: Map<String, Any>): BillInfoData? {
            val bills = mutableListOf<BillData>()
            
            try {
                val mergedData = MainActivity().mergeSERVERData(result as Map<String, Object>)
                if (mergedData != null && mergedData.containsKey("bill_info_raw")) {
                    val billInfoArray = mergedData["bill_info_raw"] as? JSONArray
                    billInfoArray?.let { array ->
                        for (i in 0 until array.length()) {
                            val bill = array.getJSONObject(i)
                            bills.add(
                                BillData(
                                    billMonth = formatBillMonth(bill.optString("BILL_MONTH")),
                                    billNo = bill.optString("BILL_NO"),
                                    consumption = bill.optString("CONS_KWH_SR"),
                                    currentBill = bill.optString("TOTAL_BILL"),
                                    dueDate = formatDate(bill.optString("INVOICE_DUE_DATE")),
                                    paidAmt = bill.optString("PAID_AMT"),
                                    receiptDate = formatDate(bill.optString("RECEIPT_DATE")),
                                    balance = bill.optString("BALANCE")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return if (bills.isNotEmpty()) BillInfoData(bills) else null
        }

        // Extract balance info
        private fun extractBalanceInfo(result: Map<String, Any>): BalanceInfoData? {
            val fields = mutableListOf<KeyValuePair>()
            
            try {
                val mergedData = MainActivity().mergeSERVERData(result as Map<String, Object>)
                if (mergedData != null && mergedData.containsKey("balance_info")) {
                    val balanceInfo = mergedData["balance_info"] as? Map<String, String>
                    balanceInfo?.forEach { (key, value) ->
                        if (isValidValue(value)) {
                            fields.add(KeyValuePair(key, value))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return if (fields.isNotEmpty()) BalanceInfoData(fields) else null
        }

        // Extract bill summary
        private fun extractBillSummary(result: Map<String, Any>): BillSummaryData? {
            val fields = mutableListOf<KeyValuePair>()
            
            try {
                val mergedData = MainActivity().mergeSERVERData(result as Map<String, Object>)
                if (mergedData != null && mergedData.containsKey("bill_summary")) {
                    val billSummary = mergedData["bill_summary"] as? Map<String, Any>
                    
                    // Add relevant summary fields
                    billSummary?.let { summary ->
                        summary["total_bills"]?.let {
                            fields.add(KeyValuePair("Total Bills", it.toString()))
                        }
                        summary["latest_bill_date"]?.let {
                            fields.add(KeyValuePair("Latest Bill Date", it.toString()))
                        }
                        summary["latest_total_amount"]?.let {
                            fields.add(KeyValuePair("Latest Amount", "৳$it"))
                        }
                        summary["recent_consumption"]?.let {
                            fields.add(KeyValuePair("Recent Consumption", "$it kWh"))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return if (fields.isNotEmpty()) BillSummaryData(fields) else null
        }

        // Extract error
        private fun extractError(result: Map<String, Any>): ErrorData? {
            return if (result.containsKey("error")) {
                ErrorData(result["error"].toString())
            } else {
                null
            }
        }

        // Helper methods from MainActivity
        private fun isValidValue(value: String): Boolean {
            if (value == null) return false
            val trimmedValue = value.trim()
            return !trimmedValue.isEmpty() &&
                    !trimmedValue.equals("N/A") &&
                    !trimmedValue.equals("null") &&
                    !trimmedValue.equals("{}") &&
                    !trimmedValue.equals("undefined")
        }

        private fun formatDate(dateString: String): String {
            if (dateString == null || dateString.isEmpty()) {
                return "N/A"
            }
            try {
                if (dateString.contains("T")) {
                    return dateString.split("T")[0]
                }
                return dateString
            } catch (e: Exception) {
                return dateString
            }
        }

        private fun formatBillMonth(dateStr: String): String {
            if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) {
                return "N/A"
            }

            try {
                val parts = dateStr.substring(0, 10).split("-")
                if (parts.size >= 2) {
                    val month = parts[1].toInt()
                    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                    if (month in 1..12) {
                        return "${monthNames[month - 1]}-${parts[0]}"
                    }
                }
                return dateStr.substring(0, 7) // Fallback to YYYY-MM
            } catch (e: Exception) {
                return if (dateStr.length >= 7) dateStr.substring(0, 7) else dateStr
            }
        }

        // Template rendering methods
        fun renderPostpaidTemplate(context: Context, data: PostpaidData): String {
            val template = loadTemplate(context, TEMPLATE_POSTPAID)
            return replacePostpaidPlaceholders(template, data)
        }

        fun renderPrepaidTemplate(context: Context, data: PrepaidData): String {
            val template = loadTemplate(context, TEMPLATE_PREPAID)
            return replacePrepaidPlaceholders(template, data)
        }

        private fun loadTemplate(context: Context, templateName: String): String {
            return try {
                val inputStream = context.assets.open(templateName)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                String(buffer, Charsets.UTF_8)
            } catch (e: Exception) {
                // Fallback template
                """
                <html>
                <body>
                    <h1>Error loading template</h1>
                    <p>${e.message}</p>
                </body>
                </html>
                """
            }
        }

        private fun replacePostpaidPlaceholders(template: String, data: PostpaidData): String {
            var result = template.replace("{{CUSTOMER_NUMBER}}", data.customerNumber)

            // Handle multiple customers
            data.multipleCustomers?.let { multiple ->
                result = result.replace("{{#MULTIPLE_CUSTOMERS}}", "")
                    .replace("{{/MULTIPLE_CUSTOMERS}}", "")
                
                result = result.replace("{{CUSTOMER_COUNT}}", multiple.customerCount.toString())
                
                val customersHtml = StringBuilder()
                multiple.customers.forEach { customer ->
                    var customerHtml = """
                        <div class="customer-card">
                            <h4>👤 Customer ${customer.index}: ${customer.customerNumber}</h4>
                    """
                    
                    customer.customerInfo.forEach { info ->
                        customerHtml += """
                            <div class="field">
                                <span><strong>${info.key}:</strong></span>
                                <span>${info.value}</span>
                            </div>
                        """
                    }
                    
                    customerHtml += "</div>"
                    customersHtml.append(customerHtml)
                }
                
                result = result.replace("{{#CUSTOMERS}}{{/CUSTOMERS}}", customersHtml.toString())
            } ?: run {
                result = result.replace("{{#MULTIPLE_CUSTOMERS}}", "<!--")
                    .replace("{{/MULTIPLE_CUSTOMERS}}", "-->")
            }

            // Handle single customer
            data.singleCustomer?.let { single ->
                result = result.replace("{{#SINGLE_CUSTOMER}}", "")
                    .replace("{{/SINGLE_CUSTOMER}}", "")
                
                val customerInfoHtml = StringBuilder()
                single.customerInfo.forEach { info ->
                    customerInfoHtml.append("""
                        <div class="field">
                            <span><strong>${info.key}:</strong></span>
                            <span>${info.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#CUSTOMER_INFO}}{{/CUSTOMER_INFO}}", customerInfoHtml.toString())
            } ?: run {
                result = result.replace("{{#SINGLE_CUSTOMER}}", "<!--")
                    .replace("{{/SINGLE_CUSTOMER}}", "-->")
            }

            // Handle bill info
            data.billInfo?.let { billInfo ->
                result = result.replace("{{#BILL_INFO}}", "")
                    .replace("{{/BILL_INFO}}", "")
                
                val billsHtml = StringBuilder()
                billInfo.bills.forEach { bill ->
                    billsHtml.append("""
                        <tr>
                            <td>${bill.billMonth}</td>
                            <td>${bill.billNo}</td>
                            <td>${bill.consumption}</td>
                            <td>${bill.currentBill}</td>
                            <td>${bill.dueDate}</td>
                            <td>${bill.paidAmt}</td>
                            <td>${bill.receiptDate}</td>
                            <td>${bill.balance}</td>
                        </tr>
                    """)
                }
                
                result = result.replace("{{#BILLS}}{{/BILLS}}", billsHtml.toString())
            } ?: run {
                result = result.replace("{{#BILL_INFO}}", "<!--")
                    .replace("{{/BILL_INFO}}", "-->")
            }

            // Handle balance info
            data.balanceInfo?.let { balanceInfo ->
                result = result.replace("{{#BALANCE_INFO}}", "")
                    .replace("{{/BALANCE_INFO}}", "")
                
                val balanceHtml = StringBuilder()
                balanceInfo.fields.forEach { field ->
                    balanceHtml.append("""
                        <div class="field">
                            <span><strong>${field.key}:</strong></span>
                            <span>${field.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#FIELDS}}{{/FIELDS}}", balanceHtml.toString())
            } ?: run {
                result = result.replace("{{#BALANCE_INFO}}", "<!--")
                    .replace("{{/BALANCE_INFO}}", "-->")
            }

            // Handle bill summary
            data.billSummary?.let { billSummary ->
                result = result.replace("{{#BILL_SUMMARY}}", "")
                    .replace("{{/BILL_SUMMARY}}", "")
                
                val summaryHtml = StringBuilder()
                billSummary.fields.forEach { field ->
                    summaryHtml.append("""
                        <div class="field">
                            <span><strong>${field.key}:</strong></span>
                            <span>${field.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#FIELDS}}{{/FIELDS}}", summaryHtml.toString())
            } ?: run {
                result = result.replace("{{#BILL_SUMMARY}}", "<!--")
                    .replace("{{/BILL_SUMMARY}}", "-->")
            }

            // Handle error
            data.error?.let { error ->
                result = result.replace("{{#ERROR}}", "")
                    .replace("{{/ERROR}}", "")
                result = result.replace("{{ERROR_MESSAGE}}", error.errorMessage)
            } ?: run {
                result = result.replace("{{#ERROR}}", "<!--")
                    .replace("{{/ERROR}}", "-->")
            }

            return result
        }

        private fun replacePrepaidPlaceholders(template: String, data: PrepaidData): String {
            var result = template.replace("{{METER_NUMBER}}", data.meterNumber)
                .replace("{{CONSUMER_NUMBER}}", data.consumerNumber)

            // Handle prepaid customer info
            data.prepaidCustomerInfo?.let { prepaidInfo ->
                result = result.replace("{{#PREPAID_CUSTOMER_INFO}}", "")
                    .replace("{{/PREPAID_CUSTOMER_INFO}}", "")
                
                val prepaidHtml = StringBuilder()
                prepaidInfo.fields.forEach { field ->
                    prepaidHtml.append("""
                        <div class="field">
                            <span><strong>${field.key}:</strong></span>
                            <span>${field.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#FIELDS}}{{/FIELDS}}", prepaidHtml.toString())
            } ?: run {
                result = result.replace("{{#PREPAID_CUSTOMER_INFO}}", "<!--")
                    .replace("{{/PREPAID_CUSTOMER_INFO}}", "-->")
            }

            // Handle tokens
            data.tokens?.let { tokens ->
                result = result.replace("{{#TOKENS}}", "")
                    .replace("{{/TOKENS}}", "")
                
                val tokensHtml = StringBuilder()
                tokens.tokenList.forEach { token ->
                    tokensHtml.append("""
                        <div class="token">
                            <strong>Order ${token.index}:</strong><br>
                            <strong>Token:</strong> ${token.token}<br>
                            <strong>Date:</strong> ${token.date}<br>
                            <strong>Amount:</strong> ${token.amount}<br>
                            <strong>Operator:</strong> ${token.operator}<br>
                            <strong>Sequence:</strong> ${token.sequence}
                        </div>
                    """)
                }
                
                result = result.replace("{{#TOKEN_LIST}}{{/TOKEN_LIST}}", tokensHtml.toString())
            } ?: run {
                result = result.replace("{{#TOKENS}}", "<!--")
                    .replace("{{/TOKENS}}", "-->")
            }

            // Handle postpaid customer info
            data.postpaidCustomerInfo?.let { postpaidInfo ->
                result = result.replace("{{#POSTPAID_CUSTOMER_INFO}}", "")
                    .replace("{{/POSTPAID_CUSTOMER_INFO}}", "")
                
                val postpaidHtml = StringBuilder()
                postpaidInfo.fields.forEach { field ->
                    postpaidHtml.append("""
                        <div class="field">
                            <span><strong>${field.key}:</strong></span>
                            <span>${field.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#FIELDS}}{{/FIELDS}}", postpaidHtml.toString())
            } ?: run {
                result = result.replace("{{#POSTPAID_CUSTOMER_INFO}}", "<!--")
                    .replace("{{/POSTPAID_CUSTOMER_INFO}}", "-->")
            }

            // Handle bill info, balance info, bill summary, and error (same as postpaid)
            data.billInfo?.let { billInfo ->
                result = result.replace("{{#BILL_INFO}}", "")
                    .replace("{{/BILL_INFO}}", "")
                
                val billsHtml = StringBuilder()
                billInfo.bills.forEach { bill ->
                    billsHtml.append("""
                        <tr>
                            <td>${bill.billMonth}</td>
                            <td>${bill.billNo}</td>
                            <td>${bill.consumption}</td>
                            <td>${bill.currentBill}</td>
                            <td>${bill.dueDate}</td>
                            <td>${bill.paidAmt}</td>
                            <td>${bill.receiptDate}</td>
                            <td>${bill.balance}</td>
                        </tr>
                    """)
                }
                
                result = result.replace("{{#BILLS}}{{/BILLS}}", billsHtml.toString())
            } ?: run {
                result = result.replace("{{#BILL_INFO}}", "<!--")
                    .replace("{{/BILL_INFO}}", "-->")
            }

            data.balanceInfo?.let { balanceInfo ->
                result = result.replace("{{#BALANCE_INFO}}", "")
                    .replace("{{/BALANCE_INFO}}", "")
                
                val balanceHtml = StringBuilder()
                balanceInfo.fields.forEach { field ->
                    balanceHtml.append("""
                        <div class="field">
                            <span><strong>${field.key}:</strong></span>
                            <span>${field.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#FIELDS}}{{/FIELDS}}", balanceHtml.toString())
            } ?: run {
                result = result.replace("{{#BALANCE_INFO}}", "<!--")
                    .replace("{{/BALANCE_INFO}}", "-->")
            }

            data.billSummary?.let { billSummary ->
                result = result.replace("{{#BILL_SUMMARY}}", "")
                    .replace("{{/BILL_SUMMARY}}", "")
                
                val summaryHtml = StringBuilder()
                billSummary.fields.forEach { field ->
                    summaryHtml.append("""
                        <div class="field">
                            <span><strong>${field.key}:</strong></span>
                            <span>${field.value}</span>
                        </div>
                    """)
                }
                
                result = result.replace("{{#FIELDS}}{{/FIELDS}}", summaryHtml.toString())
            } ?: run {
                result = result.replace("{{#BILL_SUMMARY}}", "<!--")
                    .replace("{{/BILL_SUMMARY}}", "-->")
            }

            data.error?.let { error ->
                result = result.replace("{{#ERROR}}", "")
                    .replace("{{/ERROR}}", "")
                result = result.replace("{{ERROR_MESSAGE}}", error.errorMessage)
            } ?: run {
                result = result.replace("{{#ERROR}}", "<!--")
                    .replace("{{/ERROR}}", "-->")
            }

            return result
        }
    }
}
