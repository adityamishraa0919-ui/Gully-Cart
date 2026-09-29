package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppReceiptHelper {

    /**
     * Normalizes phone numbers for WhatsApp URL (defaults to India +91 if 10 digits).
     */
    fun cleanPhoneNumber(rawPhone: String): String {
        val digits = rawPhone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 10 -> "91$digits" // Standard 10-digit Indian mobile number
            digits.startsWith("0") && digits.length == 11 -> "91" + digits.substring(1)
            digits.startsWith("91") -> digits
            else -> digits
        }
    }

    /**
     * Formats official order details for WhatsApp with bold headers and emoji formatting.
     */
    fun formatWhatsAppReceipt(order: Order): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date(order.timestamp))

        return """
            🛍️ *GULLY CART - OFFICIAL ORDER RECEIPT* 🎉
            
            Hello *${order.userName.ifBlank { "Customer" }}*, your order has been confirmed!
            
            🧾 *Order ID:* #${order.orderId}
            📅 *Date:* $dateStr
            🏷️ *Items:* ${order.itemsSummary}
            💰 *Total Amount:* ₹${"%.0f".format(order.totalAmount)}
            💳 *Payment Method:* ${order.paymentMethod}
            📍 *Delivery Address:* ${order.deliveryAddress}
            🚚 *Tracking Number:* ${order.trackingNumber}
            ⚡ *Status:* ${order.status}
            
            🚛 *Estimated Delivery:* 2 to 3 Business Days.
            Thank you for shopping with Gully Cart! ❤️
            Need assistance? Reach us 24/7 in the Gully Cart Help Center.
        """.trimIndent()
    }

    /**
     * Launches WhatsApp directly with the pre-filled order receipt.
     * If a phone number is provided, opens the direct chat with that number.
     * Otherwise opens WhatsApp share picker with the invoice text.
     */
    fun openReceiptInWhatsApp(context: Context, order: Order, customPhone: String? = null) {
        val rawPhone = customPhone?.ifBlank { null } ?: order.userPhone.ifBlank { null }
        val message = formatWhatsAppReceipt(order)
        val encodedMessage = Uri.encode(message)

        val url = if (!rawPhone.isNullOrBlank()) {
            val cleanPhone = cleanPhoneNumber(rawPhone)
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback for WhatsApp Business or any browser/messenger
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
