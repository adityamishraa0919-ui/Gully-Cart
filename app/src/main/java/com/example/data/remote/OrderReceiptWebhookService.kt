package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WebhookTestResult(
    val success: Boolean,
    val statusCode: Int,
    val message: String
)

object OrderReceiptWebhookService {
    private const val TAG = "OrderReceiptWebhook"
    const val DEFAULT_WEBHOOK_URL =
        "https://script.google.com/macros/s/AKfycbxfMS91Ro8YPRJepGkrIPzunPOi2lds8TUfMwlC4_VYbD2EZ6kXfKhgKKotZww1Vo4R/exec"

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return when {
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "https://")
            trimmed.startsWith("://") -> "https$trimmed"
            trimmed.isNotBlank() -> "https://$trimmed"
            else -> DEFAULT_WEBHOOK_URL
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Sends order details to Google Apps Script webhook to automatically send customer a receipt email / log order in Google Sheets.
     * JSON payload: { "email": customerEmail, "name": customerName, "items": boughtItems, "total": totalAmount, ... }
     */
    suspend fun sendOrderReceipt(
        customerEmail: String,
        customerName: String,
        boughtItems: String,
        totalAmount: Double,
        orderId: String = "",
        phone: String = "",
        address: String = "",
        paymentMethod: String = "",
        customUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val targetUrl = normalizeUrl(customUrl ?: DEFAULT_WEBHOOK_URL)
        try {
            val jsonObject = JSONObject().apply {
                put("email", customerEmail)
                put("name", customerName)
                put("items", boughtItems)
                put("total", totalAmount)
                if (orderId.isNotBlank()) put("orderId", orderId)
                if (phone.isNotBlank()) {
                    put("phone", phone)
                    put("whatsapp", phone)
                    put("mobile", phone)
                    put("phoneNumber", phone)
                    put(
                        "whatsappMessage",
                        "Gully Cart Order Confirmed! Order #$orderId for ₹${"%.0f".format(totalAmount)}. Items: $boughtItems. Delivery to: $address."
                    )
                }
                if (address.isNotBlank()) put("address", address)
                if (paymentMethod.isNotBlank()) put("paymentMethod", paymentMethod)
                put("timestamp", System.currentTimeMillis())
            }

            val requestBody = jsonObject.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .addHeader("User-Agent", "GullyCart-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodySnippet = response.body?.string()?.take(500) ?: ""
                Log.d(TAG, "Receipt email webhook response code: $code for $customerEmail, body: $bodySnippet")
                if (bodySnippet.contains("doPost") && (bodySnippet.contains("找不到") || bodySnippet.contains("not found"))) {
                    Log.w(TAG, "Apps Script warning: 'doPost' function missing in script.")
                    return@withContext false
                }
                code in 200..399
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send order receipt email to webhook for $customerEmail: ${e.message}", e)
            false
        }
    }

    /**
     * Diagnostic test for Google Apps Script Webhook URL.
     */
    suspend fun testWebhook(testUrl: String): WebhookTestResult = withContext(Dispatchers.IO) {
        val targetUrl = normalizeUrl(testUrl.ifBlank { DEFAULT_WEBHOOK_URL })
        try {
            val jsonObject = JSONObject().apply {
                put("email", "test@gullycart.com")
                put("name", "Test User")
                put("items", "Gully Cart Test Item x 1")
                put("total", 999.0)
            }

            val requestBody = jsonObject.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .addHeader("User-Agent", "GullyCart-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string().orEmpty()
                if (responseBody.contains("doPost") && (responseBody.contains("找不到") || responseBody.contains("not found"))) {
                    WebhookTestResult(
                        false,
                        code,
                        "Script reachable, but function doPost(e) is missing! In Google Apps Script, make sure your function is named exactly doPost(e)."
                    )
                } else if (code in 200..399) {
                    WebhookTestResult(true, code, "HTTP $code: Webhook connected and received test payload successfully!")
                } else if (code == 404) {
                    WebhookTestResult(
                        false,
                        code,
                        "HTTP 404: Google cannot find this Web App. In Google Apps Script, make sure to click: Deploy > New Deployment > Web app > Execute as: Me > Who has access: Anyone."
                    )
                } else {
                    WebhookTestResult(false, code, "HTTP $code: ${response.message}")
                }
            }
        } catch (e: Exception) {
            WebhookTestResult(false, -1, "Connection error: ${e.localizedMessage}")
        }
    }
}
