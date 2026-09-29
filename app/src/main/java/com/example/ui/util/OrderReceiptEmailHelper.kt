package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OrderReceiptEmailHelper {

    /**
     * Formats receipt text for an order.
     */
    fun formatReceiptBody(order: Order): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date(order.timestamp))

        return """
            ===========================================
                      GULLY CART - OFFICIAL RECEIPT
            ===========================================
            Order ID: #${order.orderId}
            Order Date: $dateStr
            Customer Name: ${order.userName.ifBlank { "Valued Customer" }}
            Customer Email: ${order.userEmail}
            Delivery Address: ${order.deliveryAddress}
            
            -------------------------------------------
            ITEMS ORDERED:
            -------------------------------------------
            ${order.itemsSummary}
            
            -------------------------------------------
            PAYMENT SUMMARY:
            -------------------------------------------
            Payment Method: ${order.paymentMethod}
            Transaction Status: ${order.status}
            Tracking Number: ${order.trackingNumber}
            
            TOTAL AMOUNT PAID: ₹${"%.0f".format(order.totalAmount)}
            
            ===========================================
            Thank you for shopping with Gully Cart!
            For 24/7 support, contact us in the Gully Cart Help Center.
            ===========================================
        """.trimIndent()
    }

    /**
     * Launches the Gmail or default email client pre-filled with the invoice receipt.
     */
    fun openReceiptInGmail(context: Context, order: Order) {
        val recipientEmail = order.userEmail.ifBlank { "customer@gullycart.com" }
        val subject = "🧾 Gully Cart Receipt - Order #${order.orderId} (₹${"%.0f".format(order.totalAmount)})"
        val body = formatReceiptBody(order)

        try {
            // Intent specifically targeted to email apps
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(emailIntent, "Send Order Receipt via Gmail"))
        } catch (_: Exception) {
            try {
                // Fallback generic send
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(genericIntent, "Send Order Receipt"))
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open email app: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
