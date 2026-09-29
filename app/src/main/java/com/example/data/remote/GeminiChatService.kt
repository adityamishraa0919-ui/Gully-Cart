package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiChatService {
    private const val TAG = "GeminiChatService"
    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_INSTRUCTION = """
You are GC Stylist & Store AI, the intelligent, genuine, and dedicated personal shopping consultant for Gully Cart (GC) in India.
Your mission:
1. Provide thoughtful, well-reasoned, and helpful answers for clothing, streetwear, boots, goggles, watches, and accessories.
2. When asked about fashion or apparel, give genuine styling tips, fabric details (e.g. breathable cotton, heavy fleece), and size recommendations (S, M, L, XL, XXL; UK 6 to UK 12 for boots).
3. Clearly explain payment options: Cash on Delivery (COD) and Online UPI QR Code (Google Pay, PhonePe, Paytm) with seamless order verification.
4. Explain store policies: 7-day hassle-free doorstep returns, free express delivery across India, and real-time tracking under the Orders tab.
5. Provide genuine, warm, and articulate assistance. Never be robotic or dismissive.
6. Note that users can connect directly with our Store Team at any time.
"""

    /**
     * Multi-turn chat using Gemini 3.5 Flash.
     * Takes the conversation history so far and the new user message.
     */
    suspend fun sendMessage(
        history: List<ChatMessageEntity>,
        newUserMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // Build request JSON
        try {
            val rootJson = JSONObject()

            // System Instruction
            val sysInstructionObj = JSONObject()
            val sysPartsArray = JSONArray().apply {
                put(JSONObject().apply { put("text", SYSTEM_INSTRUCTION) })
            }
            sysInstructionObj.put("parts", sysPartsArray)
            rootJson.put("systemInstruction", sysInstructionObj)

            // Contents array (multi-turn history)
            val contentsArray = JSONArray()

            // Add recent history (up to last 10 messages for context)
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                if (msg.message.isBlank()) continue
                val role = if (msg.sender == "USER") "user" else "model"
                val turnObj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.message) })
                    })
                }
                contentsArray.put(turnObj)
            }

            // Add the new user message
            val currentTurnObj = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", newUserMessage) })
                })
            }
            contentsArray.put(currentTurnObj)
            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            }
            rootJson.put("generationConfig", genConfig)

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
                val body = rootJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val respJson = JSONObject(responseBody)
                    val candidates = respJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) {
                                return@withContext text.trim()
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Gemini API call returned status ${response.code}: $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API request failed: ${e.message}", e)
        }

        // Smart domain-aware fallback if API key is not yet configured or network is restricted
        getSmartCustomerSupportFallback(newUserMessage)
    }

    fun getSmartCustomerSupportFallback(query: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("size") || lower.contains("boot") || lower.contains("fit") -> {
                "For our footwear collection, all boots follow standardized UK sizing (UK 6 to UK 12). If you usually wear half-sizes or plan to wear thick socks with combat or desert boots, we suggest sizing one half-step up for optimal toe room. For clothing (Shirts, Tees, Kurtas), our items are true-to-fit with 100% premium pre-shrunk cotton. Check the exact measurements on each product page!"
            }
            lower.contains("cloth") || lower.contains("shirt") || lower.contains("dress") || lower.contains("wear") -> {
                "Our latest fashion catalog features premium oversized tees, formal and casual oxford shirts, embroidered festive kurtas, and rugged utility jackets. All apparel is crafted with breathable, high-durability fabrics designed for everyday all-weather comfort. You can filter by Men, Women, or Boots from the Home categories!"
            }
            lower.contains("order") || lower.contains("track") || lower.contains("delivery") -> {
                "You can inspect live dispatch tracking anytime under the 'Orders' tab at the bottom of the screen. Every order comes with express surface and air shipping across all pin codes in India, typically arriving in 2 to 3 business days with real-time milestone updates."
            }
            lower.contains("pay") || lower.contains("cod") || lower.contains("upi") || lower.contains("qr") -> {
                "Gully Cart (GC) supports 100% secure payment methods: Cash on Delivery (COD) for zero hassle, and Instant Online Pay via UPI QR (compatible with Google Pay, PhonePe, Paytm, and BHIM). When paying online, simply scan the dynamic QR, complete the payment in your app, and enter your 12-digit UTR reference or upload screenshot proof."
            }
            lower.contains("return") || lower.contains("refund") || lower.contains("exchange") -> {
                "We provide a 100% customer-first 7-day doorstep return and exchange policy on all unworn items with tags intact. If an item doesn't fit or meet your expectations, simply reach out to our team here and we will arrange a free courier reverse pickup right from your home address."
            }
            lower.contains("admin") || lower.contains("team") || lower.contains("agent") || lower.contains("store") || lower.contains("support") -> {
                "Our Store Team is active and notified directly in the Store Hub! You can tap 'Talk to the Team' above to connect directly with our official store support specialists."
            }
            else -> {
                "Welcome to Gully Cart (GC) Support! I'm your dedicated Store AI. I can assist you with our latest clothing collections, boot size guides, Google Pay UPI or COD checkout, and express order tracking. How can I help you find the perfect outfit today?"
            }
        }
    }
}
