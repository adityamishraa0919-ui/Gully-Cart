package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.model.AppLiveUpdateConfig
import com.example.data.model.AppNotification
import com.example.data.model.CartItem
import com.example.data.model.ChatMessageEntity
import com.example.data.model.Order
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.SupportQuery
import com.example.data.model.UserAccount
import com.example.data.model.UserEvent
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class ECommerceRepository private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "omnicart_database.db"
    ).fallbackToDestructiveMigration(true).build()

    private val productDao = db.productDao()
    private val userProfileDao = db.userProfileDao()
    private val cartDao = db.cartDao()
    private val orderDao = db.orderDao()
    private val analyticsDao = db.analyticsDao()
    private val supportDao = db.supportDao()
    private val userDao = db.userDao()
    private val chatDao = db.chatDao()
    private val notificationDao = db.notificationDao()
    private val configDao = db.configDao()
    private val reviewDao = db.reviewDao()

    companion object {
        @Volatile
        private var INSTANCE: ECommerceRepository? = null

        fun getInstance(context: Context): ECommerceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ECommerceRepository(context).also { INSTANCE = it }
            }
        }
    }

    // Products
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    fun getProductsByCategory(category: String): Flow<List<Product>> =
        if (category == "all") productDao.getAllProducts() else productDao.getProductsByCategory(category)
    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)
    suspend fun getProductById(id: String): Product? = productDao.getProductById(id)
    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun updateStock(id: String, newStock: Int) = productDao.updateStock(id, newStock)

    // Real Product Reviews
    fun getProductReviews(productId: String): Flow<List<ProductReview>> = reviewDao.getReviewsForProduct(productId)
    suspend fun addReview(review: ProductReview) {
        reviewDao.insertReview(review)
        val avg = reviewDao.getAverageRating(review.productId) ?: review.rating.toDouble()
        val count = reviewDao.getReviewCount(review.productId)
        val p = productDao.getProductById(review.productId)
        if (p != null) {
            val rounded = (Math.round(avg * 10.0) / 10.0).toFloat()
            productDao.updateProduct(p.copy(rating = rounded, ratingCount = count))
        }
    }

    // Cart
    fun getCartItems(): Flow<List<CartItem>> = cartDao.getAllCartItems()
    suspend fun addToCart(product: Product, size: String, quantity: Int = 1) {
        val existing = cartDao.getCartItem(product.id, size)
        if (existing != null) {
            cartDao.updateCartItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            cartDao.insertCartItem(
                CartItem(
                    productId = product.id,
                    title = product.title,
                    imageUrl = product.imageUrl,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    selectedSize = size,
                    quantity = quantity,
                    is4K = product.is4K,
                    category = product.category
                )
            )
        }
        logEvent("ADD_TO_CART", "Added ${product.title} (Size: $size) to Cart")
    }

    suspend fun updateCartQuantity(item: CartItem, newQty: Int) {
        if (newQty <= 0) {
            cartDao.deleteCartItem(item)
            logEvent("CART_REMOVE", "Removed ${item.title} from Cart")
        } else {
            cartDao.updateCartItem(item.copy(quantity = newQty))
        }
    }

    suspend fun removeFromCart(item: CartItem) {
        cartDao.deleteCartItem(item)
        logEvent("CART_REMOVE", "Removed ${item.title} from Cart")
    }

    suspend fun clearCart() = cartDao.clearCart()

    // Orders & Order History
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()
    fun getOrdersForUser(email: String): Flow<List<Order>> = orderDao.getOrdersForUser(email)
    fun getOrderById(orderId: String): Flow<Order?> = orderDao.getOrderById(orderId)
    fun getOrderWithItems(orderId: String): Flow<OrderWithItems?> = orderDao.getOrderWithItems(orderId)
    fun getOrderHistoryWithItems(email: String): Flow<List<OrderWithItems>> = orderDao.getOrderHistoryWithItems(email)
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItems()
    fun getTotalRevenue(): Flow<Double?> = orderDao.getTotalRevenue()
    fun getTotalOrderCount(): Flow<Int> = orderDao.getTotalOrderCount()

    suspend fun placeOrder(order: Order, cartItems: List<CartItem>): List<OrderItemEntity> {
        orderDao.insertOrder(order)

        // Convert cart items to structured order items for normalized order history
        val orderItems = cartItems.map { item ->
            OrderItemEntity(
                orderId = order.orderId,
                productId = item.productId,
                title = item.title,
                imageUrl = item.imageUrl,
                price = item.price,
                selectedSize = item.selectedSize,
                quantity = item.quantity,
                subtotal = item.price * item.quantity
            )
        }
        if (orderItems.isNotEmpty()) {
            orderDao.insertOrderItems(orderItems)
        }

        // Increment user profile total order count and spend stats in Room
        userProfileDao.incrementUserOrderStats(order.userEmail, order.totalAmount)

        // Deduct inventory in real-time
        for (item in cartItems) {
            val prod = productDao.getProductById(item.productId)
            if (prod != null) {
                val newStock = (prod.stock - item.quantity).coerceAtLeast(0)
                productDao.updateStock(prod.id, newStock)
            }
        }
        cartDao.clearCart()
        logEvent(
            "PURCHASE",
            "Order #${order.orderId} placed for ₹${order.totalAmount} via ${order.paymentMethod} by ${order.userName} (${order.userEmail})"
        )
        return orderItems
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        orderDao.updateOrderStatus(orderId, status)
        logEvent("ORDER_STATUS", "Order #$orderId status updated to: $status")
    }

    // User Profiles (Room Persistence)
    fun getUserProfile(email: String): Flow<UserProfile?> = userProfileDao.getUserProfile(email)
    suspend fun getUserProfileOnce(email: String): UserProfile? = userProfileDao.getUserProfileOnce(email)
    fun getAllUserProfiles(): Flow<List<UserProfile>> = userProfileDao.getAllUserProfiles()
    suspend fun saveUserProfile(profile: UserProfile) = userProfileDao.insertUserProfile(profile)
    suspend fun updateUserProfile(profile: UserProfile) = userProfileDao.updateUserProfile(profile)
    suspend fun deleteUserProfile(profile: UserProfile) = userProfileDao.deleteUserProfile(profile)
    suspend fun updateUserAddress(
        email: String,
        address: String,
        city: String,
        state: String,
        postalCode: String
    ) = userProfileDao.updateAddress(email, address, city, state, postalCode)

    // Registered Users & Account Persistence
    fun getAllUsers(): Flow<List<UserAccount>> = userDao.getAllUsers()
    suspend fun getUserByEmail(email: String): UserAccount? = userDao.getUserByEmail(email)
    suspend fun registerUser(user: UserAccount) {
        userDao.insertUser(user)
        // Also ensure user profile entity is initialized in Room
        val existingProfile = userProfileDao.getUserProfileOnce(user.email)
        if (existingProfile == null) {
            userProfileDao.insertUserProfile(
                UserProfile(
                    email = user.email,
                    name = user.name,
                    phone = user.phone,
                    address = user.address,
                    isSignedIn = true,
                    createdAt = user.createdAt
                )
            )
        }
        logEvent("USER_REGISTER", "New user registered: ${user.name} (${user.email})")
    }
    suspend fun deleteUser(user: UserAccount) {
        userDao.deleteUser(user)
        logEvent("ADMIN_DELETE_USER", "Admin removed user: ${user.email}")
    }

    // Help Center Live Chats
    fun getMessagesForUser(email: String): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForUser(email)
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    suspend fun sendChatMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
        logEvent("CHAT_MESSAGE", "${message.sender} message for ${message.userEmail}")
    }
    suspend fun deleteUserChat(email: String) {
        chatDao.deleteMessagesForUser(email)
    }

    // In-App Notifications
    fun getNotificationsForUser(email: String): Flow<List<AppNotification>> = notificationDao.getNotificationsForUser(email)
    suspend fun addNotification(notification: AppNotification) {
        notificationDao.insertNotification(notification)
    }
    suspend fun markNotificationAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }
    suspend fun clearNotificationsForUser(email: String) {
        notificationDao.clearAllForUser(email)
    }

    // Live App Configuration & Broadcast Updates
    fun getLiveConfig(): Flow<AppLiveUpdateConfig?> = configDao.getLiveConfig()
    suspend fun getLiveConfigOnce(): AppLiveUpdateConfig? = configDao.getLiveConfigOnce()
    suspend fun saveLiveConfig(config: AppLiveUpdateConfig) {
        configDao.saveLiveConfig(config)
    }

    // Analytics
    fun getRecentEvents(): Flow<List<UserEvent>> = analyticsDao.getRecentEvents()
    fun getTotalEventsCount(): Flow<Int> = analyticsDao.getEventCount()
    suspend fun logEvent(type: String, description: String) {
        analyticsDao.logEvent(
            UserEvent(
                eventType = type,
                description = description,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Support Queries & Live Logs
    fun getAllSupportQueries(): Flow<List<SupportQuery>> = supportDao.getAllSupportQueries()
    suspend fun insertSupportQuery(query: SupportQuery) {
        supportDao.insertSupportQuery(query)
        logEvent("USER_CHAT_QUERY", "User ${query.userName} (${query.userEmail}) sent support message")
    }
    suspend fun replyToSupportQuery(id: String, reply: String) {
        supportDao.replyToSupportQuery(id, reply)
        logEvent("ADMIN_CHAT_REPLY", "Admin replied to customer inquiry #$id")
    }
    suspend fun deleteSupportQuery(id: String) {
        supportDao.deleteSupportQuery(id)
    }

    // Initial Data Seeding
    suspend fun seedInitialDataIfNeeded() {
        val count = productDao.getProductCount()
        if (count > 0) return

        val sampleProducts = listOf(
            // MEN
            Product(
                id = "men_1",
                title = "Slim-Fit Oxford Cotton Formal & Casual Shirt",
                description = "Crafted from 100% long-staple Egyptian cotton. Breathable, wrinkle-resistant finish with modern tailored slim silhouette.",
                category = "men",
                price = 1299.0,
                originalPrice = 2799.0,
                discountPercent = 53,
                rating = 4.8f,
                ratingCount = 3840,
                imageUrl = "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 45,
                availableSizes = "S, M, L, XL, XXL",
                badge = "GC Verified",
                brand = "UrbanElegance"
            ),
            Product(
                id = "men_2",
                title = "Vintage Washed Stretch Denim Trucker Jacket",
                description = "Classic distressed vintage trucker denim jacket with reinforced brass rivets, dual chest pockets, and thermal comfort lining.",
                category = "men",
                price = 2499.0,
                originalPrice = 4999.0,
                discountPercent = 50,
                rating = 4.7f,
                ratingCount = 2190,
                imageUrl = "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 28,
                availableSizes = "S, M, L, XL, XXL",
                badge = "Top Pick",
                brand = "RoughDenim"
            ),
            Product(
                id = "men_3",
                title = "Supima Heavyweight Minimalist Oversized Tee",
                description = "240 GSM organic Supima cotton. Drop shoulder contemporary drape designed for all-day comfort and street style.",
                category = "men",
                price = 799.0,
                originalPrice = 1499.0,
                discountPercent = 46,
                rating = 4.6f,
                ratingCount = 5410,
                imageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 75,
                availableSizes = "S, M, L, XL, XXL",
                badge = "GC Verified",
                brand = "MinimalCore"
            ),

            // WOMEN
            Product(
                id = "women_1",
                title = "Floral Tiered Bohemian Chiffon Midi Dress",
                description = "Elegantly flowing chiffon fabric with botanical pastel motifs, smocked elastic waist, and delicate flutter sleeves.",
                category = "women",
                price = 1699.0,
                originalPrice = 3499.0,
                discountPercent = 51,
                rating = 4.9f,
                ratingCount = 4280,
                imageUrl = "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 32,
                availableSizes = "XS, S, M, L, XL",
                badge = "Gully Trending",
                brand = "AuraFloral"
            ),
            Product(
                id = "women_2",
                title = "Handcrafted Chanderi Silk Anarkali Kurta Set",
                description = "Luxurious gold zardozi hand embroidery on royal Chanderi silk. Includes matching organza dupatta and comfort churidar.",
                category = "women",
                price = 2899.0,
                originalPrice = 5999.0,
                discountPercent = 52,
                rating = 4.8f,
                ratingCount = 1890,
                imageUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 19,
                availableSizes = "XS, S, M, L, XL",
                badge = "Festive Deal",
                brand = "RoyalRajwadi"
            ),
            Product(
                id = "women_3",
                title = "Tailored Power-Shoulder Blazer & Trouser Co-ord",
                description = "Structured double-breasted formal blazer paired with high-waist straight fit trousers. Wrinkle-free bi-stretch fabric.",
                category = "women",
                price = 3299.0,
                originalPrice = 6499.0,
                discountPercent = 49,
                rating = 4.7f,
                ratingCount = 1250,
                imageUrl = "https://images.unsplash.com/photo-1548624313-0396c75e4b1a?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 14,
                availableSizes = "XS, S, M, L, XL",
                badge = "GC Verified",
                brand = "VoguePro"
            ),

            // ACCESSORIES
            Product(
                id = "acc_1",
                title = "Full-Grain Italian Vegetable-Tanned Leather Belt",
                description = "Handcrafted from 100% thick top-grain leather with antique brushed nickel alloy buckle. Built to age with character.",
                category = "accessories",
                price = 899.0,
                originalPrice = 1999.0,
                discountPercent = 55,
                rating = 4.7f,
                ratingCount = 2840,
                imageUrl = "https://images.unsplash.com/photo-1624222247344-550fb60583dc?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 60,
                availableSizes = "Free Size",
                badge = "Trending Choice",
                brand = "TuscanCraft"
            ),
            Product(
                id = "acc_2",
                title = "Ultra-Slim RFID-Shield Carbon Fiber Bi-Fold Wallet",
                description = "Aerospace carbon fiber weave with aluminum internal core. Holds 12 cards and currency with total RFID anti-theft protection.",
                category = "accessories",
                price = 649.0,
                originalPrice = 1499.0,
                discountPercent = 56,
                rating = 4.8f,
                ratingCount = 6720,
                imageUrl = "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 85,
                availableSizes = "Standard",
                badge = "Deal of the Day",
                brand = "ArmorShield"
            ),

            // WATCHES
            Product(
                id = "wat_1",
                title = "Precision Chronograph Sapphire Tachymeter Watch",
                description = "Japanese quartz movement with triple sub-dials, scratch-resistant sapphire crystal, 100m water resistance, and 316L stainless casing.",
                category = "watches",
                price = 3999.0,
                originalPrice = 8999.0,
                discountPercent = 55,
                rating = 4.9f,
                ratingCount = 3100,
                imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 22,
                availableSizes = "42mm Dial",
                badge = "GC Verified",
                brand = "ChronosGeneva"
            ),
            Product(
                id = "wat_2",
                title = "Ultra AMOLED Bluetooth Calling Titanium Smartwatch",
                description = "1.96-inch Always-on 1000-nit AMOLED display, titanium chassis, 120+ sports modes, SpO2 & continuous heart rate tracking, 14-day battery.",
                category = "watches",
                price = 2499.0,
                originalPrice = 5999.0,
                discountPercent = 58,
                rating = 4.6f,
                ratingCount = 9810,
                imageUrl = "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 50,
                availableSizes = "46mm Titanium",
                badge = "Trending",
                brand = "TechNova"
            ),

            // GOGGLES (SUNGLASSES)
            Product(
                id = "gog_1",
                title = "Military Classic Polarized Pilot Aviator Sunglasses",
                description = "9-layer TAC polarized lenses blocking 100% UVA/UVB rays. Ultra-light spring hinge memory metal frame with silicone nose pads.",
                category = "goggles",
                price = 999.0,
                originalPrice = 2499.0,
                discountPercent = 60,
                rating = 4.8f,
                ratingCount = 4590,
                imageUrl = "https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 40,
                availableSizes = "Medium (58mm), Large (62mm)",
                badge = "Top Rated",
                brand = "RayShield"
            ),
            Product(
                id = "gog_2",
                title = "Retro Hexagonal Gold Metal Frame UV400 Goggles",
                description = "Iconic geometric silhouette with anti-reflective emerald green tint. High-durability corrosion-resistant electroplated finish.",
                category = "goggles",
                price = 799.0,
                originalPrice = 1899.0,
                discountPercent = 58,
                rating = 4.7f,
                ratingCount = 2340,
                imageUrl = "https://images.unsplash.com/photo-1572635196237-14b3f281503f?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 35,
                availableSizes = "Universal Fit",
                badge = "Top Rated",
                brand = "UrbanVibe"
            ),

            // EARPODS
            Product(
                id = "ear_1",
                title = "Pro Active Noise Cancelling Wireless Earpods",
                description = "Hybrid 42dB Active Noise Cancellation with Transparency mode, 12mm titanium dynamic drivers, ultra-low 35ms gaming latency, 50H battery.",
                category = "earpods",
                price = 1999.0,
                originalPrice = 4999.0,
                discountPercent = 60,
                rating = 4.9f,
                ratingCount = 8920,
                imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 48,
                availableSizes = "Standard (3 Ear Tip Sizes S/M/L)",
                badge = "Pro Choice",
                brand = "SonicPro"
            ),
            Product(
                id = "ear_2",
                title = "BassMaster Deep Bass True Wireless Gaming Earbuds",
                description = "Dual MEMS environmental noise cancelling quad mics for crystal clear calls. Cyberpunk RGB case with rapid wireless charging.",
                category = "earpods",
                price = 1399.0,
                originalPrice = 3299.0,
                discountPercent = 57,
                rating = 4.7f,
                ratingCount = 5120,
                imageUrl = "https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 38,
                availableSizes = "One Size",
                badge = "GC Verified",
                brand = "AudioStrike"
            ),

            // BOOTS (BOOT SECTION WITH UK/US SIZES)
            Product(
                id = "boot_1",
                title = "Handcrafted Full-Grain Leather Chelsea Ankle Boots",
                description = "Hand-burnished genuine cowhide leather with elasticated side gussets, Goodyear welted construction, and slip-resistant Vibram rubber outsole.",
                category = "boots",
                price = 3499.0,
                originalPrice = 7999.0,
                discountPercent = 56,
                rating = 4.9f,
                ratingCount = 3490,
                imageUrl = "https://images.unsplash.com/photo-1608256246200-53e635b5b65f?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 25,
                availableSizes = "UK 6, UK 7, UK 8, UK 9, UK 10, UK 11, UK 12",
                badge = "GC Verified",
                brand = "CobblerCo"
            ),
            Product(
                id = "boot_2",
                title = "Tactical Waterproof Combat Military Trekking Boots",
                description = "Breathable Cordura nylon with abrasion-resistant suede toe cap. Shock-absorbing EVA midsole engineered for extreme terrain and all-day wear.",
                category = "boots",
                price = 2799.0,
                originalPrice = 5499.0,
                discountPercent = 49,
                rating = 4.8f,
                ratingCount = 2810,
                imageUrl = "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 18,
                availableSizes = "UK 6, UK 7, UK 8, UK 9, UK 10, UK 11, UK 12",
                badge = "GC Verified",
                brand = "ApexTactical"
            ),
            Product(
                id = "boot_3",
                title = "Classic Suede Desert Chukka Ankle Boots",
                description = "Supple Italian oiled suede leather with natural crepe rubber sole. Casual, lightweight, and versatile for work and weekend excursions.",
                category = "boots",
                price = 2199.0,
                originalPrice = 4299.0,
                discountPercent = 48,
                rating = 4.7f,
                ratingCount = 1940,
                imageUrl = "https://images.unsplash.com/photo-1520639888713-7851133b1ed0?auto=format&fit=crop&w=1200&q=90",
                is4K = true,
                stock = 30,
                availableSizes = "UK 6, UK 7, UK 8, UK 9, UK 10, UK 11, UK 12",
                badge = "Classic Choice",
                brand = "HeritageFootwear"
            )
        )

        productDao.insertProducts(sampleProducts)
        logEvent("SYSTEM_INIT", "Gully Cart product catalog initialized")

        if (configDao.getLiveConfigOnce() == null) {
            configDao.saveLiveConfig(
                AppLiveUpdateConfig(
                    id = "live_config",
                    updateVersion = "v2.5.0",
                    updateTitle = "Gully Cart Festival Edition Live!",
                    updateMessage = "Welcome to the latest Gully Cart update! Enjoy lightning fast 30-min express deliveries, 20% festival bonus savings, and pristine high-resolution catalog.",
                    updateType = "FEATURE",
                    storeAnnouncement = "🎉 MEGA FESTIVAL SALE: Extra 20% OFF automatically applied on all orders!",
                    isAnnouncementActive = true,
                    globalDiscountPercent = 20,
                    promoBadge = "FESTIVAL 20% OFF",
                    storeStatus = "OPEN",
                    forceUpdateDialog = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
