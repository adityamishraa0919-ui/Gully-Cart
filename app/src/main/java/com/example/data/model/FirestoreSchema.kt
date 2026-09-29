package com.example.data.model

/**
 * FIRESTORE SCHEMA SPECIFICATION & CONTRACTS
 * E-Commerce Cloud Database Architecture for Gully Cart
 *
 * 1. Products Collection: `products/{productId}`
 *    Stores product catalog, pricing, category, 4K media assets, real-time stock, and ratings.
 *
 * 2. User Profiles Collection: `user_profiles/{email}` (or `users/{email}`)
 *    Stores customer profiles, addresses, contact information, lifetime order counts, and spend totals.
 *
 * 3. Order History Collection: `orders/{orderId}`
 *    Stores customer orders, structured line item details, payment status (UPI/COD), tracking numbers, and delivery states.
 *    Also supports subcollection `orders/{orderId}/items/{itemId}` for granular line item queries.
 */
object FirestoreSchema {

    // Collection Names
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_USER_PROFILES = "user_profiles"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ORDERS = "orders"
    const val SUBCOLLECTION_ORDER_ITEMS = "items"
    const val COLLECTION_CONFIG = "app_config"
    const val COLLECTION_ANALYTICS = "analytics"

    // Field Keys - Products
    object ProductFields {
        const val ID = "id"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val CATEGORY = "category"
        const val PRICE = "price"
        const val ORIGINAL_PRICE = "originalPrice"
        const val DISCOUNT_PERCENT = "discountPercent"
        const val RATING = "rating"
        const val RATING_COUNT = "ratingCount"
        const val IMAGE_URL = "imageUrl"
        const val IS_4K = "is4K"
        const val STOCK = "stock"
        const val AVAILABLE_SIZES = "availableSizes"
        const val BADGE = "badge"
        const val BRAND = "brand"
        const val TAGS = "tags"
        const val CREATED_AT = "createdAt"
    }

    // Field Keys - User Profiles
    object UserProfileFields {
        const val EMAIL = "email"
        const val USER_ID = "userId"
        const val NAME = "name"
        const val PHONE = "phone"
        const val ADDRESS = "address"
        const val CITY = "city"
        const val STATE = "state"
        const val POSTAL_CODE = "postalCode"
        const val COUNTRY = "country"
        const val AVATAR_URL = "avatarUrl"
        const val PREFERRED_PAYMENT_METHOD = "preferredPaymentMethod"
        const val IS_ADMIN = "isAdmin"
        const val TOTAL_ORDERS = "totalOrders"
        const val TOTAL_SPENT = "totalSpent"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    // Field Keys - Orders
    object OrderFields {
        const val ORDER_ID = "orderId"
        const val USER_EMAIL = "userEmail"
        const val USER_NAME = "userName"
        const val USER_PHONE = "userPhone"
        const val TOTAL_AMOUNT = "totalAmount"
        const val ITEM_COUNT = "itemCount"
        const val ITEMS_SUMMARY = "itemsSummary"
        const val ITEMS = "items" // Embedded array of order items
        const val PAYMENT_METHOD = "paymentMethod"
        const val UPI_ID = "upiId"
        const val PAYMENT_PROOF = "paymentProof"
        const val TIMESTAMP = "timestamp"
        const val STATUS = "status"
        const val DELIVERY_ADDRESS = "deliveryAddress"
        const val CITY = "city"
        const val POSTAL_CODE = "postalCode"
        const val TRACKING_NUMBER = "trackingNumber"
        const val DELIVERED_AT = "deliveredAt"
    }

    // Field Keys - Order Items
    object OrderItemFields {
        const val ID = "id"
        const val ORDER_ID = "orderId"
        const val PRODUCT_ID = "productId"
        const val TITLE = "title"
        const val IMAGE_URL = "imageUrl"
        const val PRICE = "price"
        const val SELECTED_SIZE = "selectedSize"
        const val QUANTITY = "quantity"
        const val SUBTOTAL = "subtotal"
    }

    // Converter: Product -> Firestore Map
    fun productToFirestoreMap(product: Product): Map<String, Any?> = hashMapOf(
        ProductFields.ID to product.id,
        ProductFields.TITLE to product.title,
        ProductFields.DESCRIPTION to product.description,
        ProductFields.CATEGORY to product.category,
        ProductFields.PRICE to product.price,
        ProductFields.ORIGINAL_PRICE to product.originalPrice,
        ProductFields.DISCOUNT_PERCENT to product.discountPercent,
        ProductFields.RATING to product.rating.toDouble(),
        ProductFields.RATING_COUNT to product.ratingCount,
        ProductFields.IMAGE_URL to product.imageUrl,
        ProductFields.IS_4K to product.is4K,
        ProductFields.STOCK to product.stock,
        ProductFields.AVAILABLE_SIZES to product.availableSizes,
        ProductFields.BADGE to product.badge,
        ProductFields.BRAND to product.brand,
        ProductFields.TAGS to product.tags,
        ProductFields.CREATED_AT to product.createdAt
    )

    // Converter: Firestore Map -> Product
    fun mapToProduct(docId: String, data: Map<String, Any?>): Product {
        return Product(
            id = (data[ProductFields.ID] as? String)?.takeIf { it.isNotBlank() } ?: docId,
            title = (data[ProductFields.TITLE] as? String) ?: "",
            description = (data[ProductFields.DESCRIPTION] as? String) ?: "",
            category = (data[ProductFields.CATEGORY] as? String) ?: "all",
            price = (data[ProductFields.PRICE] as? Number)?.toDouble() ?: 0.0,
            originalPrice = (data[ProductFields.ORIGINAL_PRICE] as? Number)?.toDouble() ?: 0.0,
            discountPercent = (data[ProductFields.DISCOUNT_PERCENT] as? Number)?.toInt() ?: 0,
            rating = (data[ProductFields.RATING] as? Number)?.toFloat() ?: 4.5f,
            ratingCount = (data[ProductFields.RATING_COUNT] as? Number)?.toInt() ?: 100,
            imageUrl = (data[ProductFields.IMAGE_URL] as? String) ?: "",
            is4K = (data[ProductFields.IS_4K] as? Boolean) ?: true,
            stock = (data[ProductFields.STOCK] as? Number)?.toInt() ?: 10,
            availableSizes = (data[ProductFields.AVAILABLE_SIZES] as? String) ?: "Standard",
            badge = (data[ProductFields.BADGE] as? String) ?: "Popular",
            brand = (data[ProductFields.BRAND] as? String) ?: "OmniBrand",
            tags = (data[ProductFields.TAGS] as? String) ?: "",
            createdAt = (data[ProductFields.CREATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    // Converter: UserProfile -> Firestore Map
    fun userProfileToFirestoreMap(profile: UserProfile): Map<String, Any?> = hashMapOf(
        UserProfileFields.EMAIL to profile.email,
        UserProfileFields.USER_ID to profile.userId,
        UserProfileFields.NAME to profile.name,
        UserProfileFields.PHONE to profile.phone,
        UserProfileFields.ADDRESS to profile.address,
        UserProfileFields.CITY to profile.city,
        UserProfileFields.STATE to profile.state,
        UserProfileFields.POSTAL_CODE to profile.postalCode,
        UserProfileFields.COUNTRY to profile.country,
        UserProfileFields.AVATAR_URL to profile.avatarUrl,
        UserProfileFields.PREFERRED_PAYMENT_METHOD to profile.preferredPaymentMethod,
        UserProfileFields.IS_ADMIN to profile.isAdmin,
        UserProfileFields.TOTAL_ORDERS to profile.totalOrders,
        UserProfileFields.TOTAL_SPENT to profile.totalSpent,
        UserProfileFields.CREATED_AT to profile.createdAt,
        UserProfileFields.UPDATED_AT to profile.updatedAt
    )

    // Converter: Firestore Map -> UserProfile
    fun mapToUserProfile(emailKey: String, data: Map<String, Any?>): UserProfile {
        return UserProfile(
            email = (data[UserProfileFields.EMAIL] as? String)?.takeIf { it.isNotBlank() } ?: emailKey,
            userId = (data[UserProfileFields.USER_ID] as? String) ?: "",
            name = (data[UserProfileFields.NAME] as? String) ?: "",
            phone = (data[UserProfileFields.PHONE] as? String) ?: "",
            address = (data[UserProfileFields.ADDRESS] as? String) ?: "",
            city = (data[UserProfileFields.CITY] as? String) ?: "",
            state = (data[UserProfileFields.STATE] as? String) ?: "",
            postalCode = (data[UserProfileFields.POSTAL_CODE] as? String) ?: "",
            country = (data[UserProfileFields.COUNTRY] as? String) ?: "India",
            avatarUrl = (data[UserProfileFields.AVATAR_URL] as? String) ?: "",
            preferredPaymentMethod = (data[UserProfileFields.PREFERRED_PAYMENT_METHOD] as? String) ?: "Google Pay UPI",
            isSignedIn = true,
            isAdmin = (data[UserProfileFields.IS_ADMIN] as? Boolean) ?: false,
            totalOrders = (data[UserProfileFields.TOTAL_ORDERS] as? Number)?.toInt() ?: 0,
            totalSpent = (data[UserProfileFields.TOTAL_SPENT] as? Number)?.toDouble() ?: 0.0,
            createdAt = (data[UserProfileFields.CREATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data[UserProfileFields.UPDATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    // Converter: OrderItemEntity -> Firestore Map
    fun orderItemToFirestoreMap(item: OrderItemEntity): Map<String, Any?> = hashMapOf(
        OrderItemFields.ID to item.id,
        OrderItemFields.ORDER_ID to item.orderId,
        OrderItemFields.PRODUCT_ID to item.productId,
        OrderItemFields.TITLE to item.title,
        OrderItemFields.IMAGE_URL to item.imageUrl,
        OrderItemFields.PRICE to item.price,
        OrderItemFields.SELECTED_SIZE to item.selectedSize,
        OrderItemFields.QUANTITY to item.quantity,
        OrderItemFields.SUBTOTAL to item.subtotal
    )

    // Converter: Firestore Map -> OrderItemEntity
    fun mapToOrderItem(orderId: String, data: Map<String, Any?>): OrderItemEntity {
        val price = (data[OrderItemFields.PRICE] as? Number)?.toDouble() ?: 0.0
        val qty = (data[OrderItemFields.QUANTITY] as? Number)?.toInt() ?: 1
        return OrderItemEntity(
            id = (data[OrderItemFields.ID] as? Number)?.toLong() ?: 0L,
            orderId = (data[OrderItemFields.ORDER_ID] as? String) ?: orderId,
            productId = (data[OrderItemFields.PRODUCT_ID] as? String) ?: "",
            title = (data[OrderItemFields.TITLE] as? String) ?: "",
            imageUrl = (data[OrderItemFields.IMAGE_URL] as? String) ?: "",
            price = price,
            selectedSize = (data[OrderItemFields.SELECTED_SIZE] as? String) ?: "",
            quantity = qty,
            subtotal = (data[OrderItemFields.SUBTOTAL] as? Number)?.toDouble() ?: (price * qty)
        )
    }

    // Converter: Order -> Firestore Map (including embedded items list)
    fun orderToFirestoreMap(order: Order, items: List<OrderItemEntity> = emptyList()): Map<String, Any?> {
        val itemsListMaps = items.map { orderItemToFirestoreMap(it) }
        return hashMapOf(
            OrderFields.ORDER_ID to order.orderId,
            OrderFields.USER_EMAIL to order.userEmail,
            OrderFields.USER_NAME to order.userName,
            OrderFields.USER_PHONE to order.userPhone,
            OrderFields.TOTAL_AMOUNT to order.totalAmount,
            OrderFields.ITEM_COUNT to order.itemCount,
            OrderFields.ITEMS_SUMMARY to order.itemsSummary,
            OrderFields.ITEMS to itemsListMaps,
            OrderFields.PAYMENT_METHOD to order.paymentMethod,
            OrderFields.UPI_ID to order.upiId,
            OrderFields.PAYMENT_PROOF to order.paymentProof,
            OrderFields.TIMESTAMP to order.timestamp,
            OrderFields.STATUS to order.status,
            OrderFields.DELIVERY_ADDRESS to order.deliveryAddress,
            OrderFields.CITY to order.city,
            OrderFields.POSTAL_CODE to order.postalCode,
            OrderFields.TRACKING_NUMBER to order.trackingNumber,
            OrderFields.DELIVERED_AT to order.deliveredAt
        )
    }

    // Converter: Firestore Map -> Order
    fun mapToOrder(orderIdKey: String, data: Map<String, Any?>): Order {
        return Order(
            orderId = (data[OrderFields.ORDER_ID] as? String)?.takeIf { it.isNotBlank() } ?: orderIdKey,
            userEmail = (data[OrderFields.USER_EMAIL] as? String) ?: "",
            userName = (data[OrderFields.USER_NAME] as? String) ?: "",
            userPhone = (data[OrderFields.USER_PHONE] as? String) ?: "",
            totalAmount = (data[OrderFields.TOTAL_AMOUNT] as? Number)?.toDouble() ?: 0.0,
            itemCount = (data[OrderFields.ITEM_COUNT] as? Number)?.toInt() ?: 1,
            itemsSummary = (data[OrderFields.ITEMS_SUMMARY] as? String) ?: "",
            paymentMethod = (data[OrderFields.PAYMENT_METHOD] as? String) ?: "Google Pay UPI",
            upiId = (data[OrderFields.UPI_ID] as? String) ?: "",
            paymentProof = data[OrderFields.PAYMENT_PROOF] as? String,
            timestamp = (data[OrderFields.TIMESTAMP] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = (data[OrderFields.STATUS] as? String) ?: "Confirmed & Processing",
            deliveryAddress = (data[OrderFields.DELIVERY_ADDRESS] as? String) ?: "",
            city = (data[OrderFields.CITY] as? String) ?: "",
            postalCode = (data[OrderFields.POSTAL_CODE] as? String) ?: "",
            trackingNumber = (data[OrderFields.TRACKING_NUMBER] as? String) ?: "",
            deliveredAt = (data[OrderFields.DELIVERED_AT] as? Number)?.toLong()
        )
    }

    // Extract Order Items from Firestore Order Map
    @Suppress("UNCHECKED_CAST")
    fun extractOrderItems(orderId: String, data: Map<String, Any?>): List<OrderItemEntity> {
        val rawList = data[OrderFields.ITEMS] as? List<Map<String, Any?>> ?: return emptyList()
        return rawList.mapNotNull { itemMap ->
            try {
                mapToOrderItem(orderId, itemMap)
            } catch (_: Exception) {
                null
            }
        }
    }
}
