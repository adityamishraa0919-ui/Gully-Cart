package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 1. PRODUCT ENTITY (Room Table: "products")
 * Represents a product item in the e-commerce catalog.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "men", "women", "accessories", "watches", "goggles", "earpods", "boots"
    val price: Double,
    val originalPrice: Double,
    val discountPercent: Int,
    val rating: Float,
    val ratingCount: Int,
    val imageUrl: String,
    val is4K: Boolean = true,
    val stock: Int,
    val availableSizes: String, // Comma-separated sizes (e.g., "S, M, L, XL, XXL" or "UK 6, UK 7, UK 8, UK 9, UK 10, UK 11")
    val badge: String = "Popular", // "Deal of the Day", "Flipkart Assured", "Meesho Special", "Best Seller"
    val brand: String = "OmniBrand",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 2. USER PROFILE ENTITY (Room Table: "user_profiles")
 * Comprehensive user profile details for account management, delivery addresses, and order tracking.
 */
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val email: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "India",
    val avatarUrl: String = "",
    val preferredPaymentMethod: String = "Google Pay UPI",
    val isSignedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Legacy/Admin User Account entity (Room Table: "users")
 */
@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey
    val email: String,
    val name: String,
    val password: String = "",
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 3. ORDER ENTITY (Room Table: "orders")
 * Represents a customer purchase order and top-level order history record.
 */
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    val orderId: String,
    val userEmail: String,
    val userName: String,
    val userPhone: String = "",
    val totalAmount: Double,
    val itemCount: Int,
    val itemsSummary: String,
    val paymentMethod: String = "Google Pay UPI", // "Cash on Delivery (COD)" or "Google Pay UPI / QR"
    val upiId: String = "",
    val paymentProof: String? = null, // UTR number or image URI of payment proof
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Confirmed & Processing", // "Confirmed & Processing", "Payment Verified", "Out for Delivery", "Delivered"
    val deliveryAddress: String,
    val city: String = "",
    val postalCode: String = "",
    val trackingNumber: String,
    val deliveredAt: Long? = null
)

/**
 * 4. ORDER ITEM ENTITY (Room Table: "order_items")
 * Normalized individual line items belonging to an Order in Order History.
 */
@Entity(
    tableName = "order_items",
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val productId: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val selectedSize: String,
    val quantity: Int = 1,
    val subtotal: Double = price * quantity
)

/**
 * 5. ORDER WITH ITEMS (Room Relation)
 * Represents an Order along with its complete list of order items for full Order History views.
 */
data class OrderWithItems(
    @Embedded
    val order: Order,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity> = emptyList()
)

/**
 * Active shopping cart item stored locally in Room.
 */
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String,
    val title: String,
    val imageUrl: String,
    val price: Double,
    val originalPrice: Double,
    val selectedSize: String,
    val quantity: Int = 1,
    val is4K: Boolean = true,
    val category: String = "all"
)

@Entity(tableName = "user_events")
data class UserEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String, // "VIEW", "CART_ADD", "PURCHASE", "SEARCH", "ADMIN_ACCESS"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_queries")
data class SupportQuery(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val userName: String,
    val userEmail: String,
    val userPhone: String = "",
    val message: String,
    val aiResponse: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending Admin Review",
    val isReplied: Boolean = false,
    val adminReply: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val userEmail: String,
    val userName: String,
    val sender: String, // "USER" or "ADMIN" or "AI"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userEmail: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "ORDER" // "ORDER", "CHAT", "SYSTEM", "APP_UPDATE"
)

@Entity(tableName = "app_config")
data class AppLiveUpdateConfig(
    @PrimaryKey
    val id: String = "live_config",
    val updateVersion: String = "v2.5.0",
    val updateTitle: String = "GC Live Mega Update! 🚀",
    val updateMessage: String = "Exciting new fashion drops & clothes added! Live flash catalog updates and store discounts are now active.",
    val updateType: String = "FEATURE", // "FEATURE", "PROMO", "MAINTENANCE", "CRITICAL"
    val storeAnnouncement: String = "🔥 MEGA SALE: Extra discounts automatically applied on all orders!",
    val isAnnouncementActive: Boolean = true,
    val globalDiscountPercent: Int = 20, // Extra storewide discount % (0 to 90%)
    val promoBadge: String = "MEGA SALE 20% OFF",
    val megaSaleMessage: String = "🔥 MEGA SALE: Up to 65% OFF on Latest Streetwear, Kicks & Apparel!",
    val storeStatus: String = "OPEN", // "OPEN", "EXPRESS_DELIVERY", "FESTIVAL_SALE", "MAINTENANCE"
    val forceUpdateDialog: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Real Product Reviews submitted by users with authentic 1-5 star ratings
 */
@Entity(
    tableName = "product_reviews",
    indices = [Index(value = ["productId"])]
)
data class ProductReview(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String,
    val userEmail: String,
    val userName: String,
    val rating: Int, // 1 to 5 stars
    val reviewText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVerifiedPurchase: Boolean = true
)
