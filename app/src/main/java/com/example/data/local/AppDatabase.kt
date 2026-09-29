package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
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

/**
 * Product Data Access Object
 */
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY createdAt DESC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stock = :newStock WHERE id = :id")
    suspend fun updateStock(id: String, newStock: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

/**
 * User Profile Data Access Object (Room persistence for customer profiles & shipping details)
 */
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    fun getUserProfile(email: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getUserProfileOnce(email: String): UserProfile?

    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    fun getAllUserProfiles(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Delete
    suspend fun deleteUserProfile(profile: UserProfile)

    @Query("UPDATE user_profiles SET address = :address, city = :city, state = :state, postalCode = :postalCode, updatedAt = :updatedAt WHERE email = :email")
    suspend fun updateAddress(
        email: String,
        address: String,
        city: String,
        state: String,
        postalCode: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_profiles SET totalOrders = totalOrders + 1, totalSpent = totalSpent + :amount, updatedAt = :updatedAt WHERE email = :email")
    suspend fun incrementUserOrderStats(
        email: String,
        amount: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getUserProfileCount(): Int
}

/**
 * Order & Order History Data Access Object
 */
@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getOrdersForUser(userEmail: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun getOrderById(orderId: String): Flow<Order?>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderByIdOnce(orderId: String): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("SELECT SUM(totalAmount) FROM orders")
    fun getTotalRevenue(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM orders")
    fun getTotalOrderCount(): Flow<Int>

    // Structured Order History with Items
    @Transaction
    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun getOrderWithItems(orderId: String): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getOrderHistoryWithItems(userEmail: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: String): Flow<List<OrderItemEntity>>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: String)

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)
}

/**
 * Shopping Cart Data Access Object
 */
@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun getAllCartItems(): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId AND selectedSize = :size LIMIT 1")
    suspend fun getCartItem(productId: String, size: String): CartItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Update
    suspend fun updateCartItem(item: CartItem)

    @Delete
    suspend fun deleteCartItem(item: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

/**
 * Legacy Account Data Access Object
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE userEmail = :userEmail ORDER BY timestamp ASC")
    fun getMessagesForUser(userEmail: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE userEmail = :userEmail")
    suspend fun deleteMessagesForUser(userEmail: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getNotificationsForUser(userEmail: String): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE userEmail = :userEmail")
    suspend fun clearAllForUser(userEmail: String)
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM user_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentEvents(): Flow<List<UserEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logEvent(event: UserEvent)

    @Query("SELECT COUNT(*) FROM user_events")
    fun getEventCount(): Flow<Int>
}

@Dao
interface SupportDao {
    @Query("SELECT * FROM support_queries ORDER BY timestamp DESC")
    fun getAllSupportQueries(): Flow<List<SupportQuery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupportQuery(query: SupportQuery)

    @Update
    suspend fun updateSupportQuery(query: SupportQuery)

    @Query("UPDATE support_queries SET isReplied = 1, adminReply = :reply, status = 'Replied by Admin' WHERE id = :id")
    suspend fun replyToSupportQuery(id: String, reply: String)

    @Query("DELETE FROM support_queries WHERE id = :id")
    suspend fun deleteSupportQuery(id: String)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 'live_config' LIMIT 1")
    fun getLiveConfig(): Flow<AppLiveUpdateConfig?>

    @Query("SELECT * FROM app_config WHERE id = 'live_config' LIMIT 1")
    suspend fun getLiveConfigOnce(): AppLiveUpdateConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLiveConfig(config: AppLiveUpdateConfig)
}

@Dao
interface ProductReviewDao {
    @Query("SELECT * FROM product_reviews WHERE productId = :productId ORDER BY timestamp DESC")
    fun getReviewsForProduct(productId: String): Flow<List<ProductReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ProductReview)

    @Query("SELECT AVG(rating) FROM product_reviews WHERE productId = :productId")
    suspend fun getAverageRating(productId: String): Double?

    @Query("SELECT COUNT(*) FROM product_reviews WHERE productId = :productId")
    suspend fun getReviewCount(productId: String): Int
}

@Database(
    entities = [
        Product::class,
        UserProfile::class,
        Order::class,
        OrderItemEntity::class,
        CartItem::class,
        UserAccount::class,
        UserEvent::class,
        SupportQuery::class,
        ChatMessageEntity::class,
        AppNotification::class,
        AppLiveUpdateConfig::class,
        ProductReview::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun orderDao(): OrderDao
    abstract fun cartDao(): CartDao
    abstract fun userDao(): UserDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun supportDao(): SupportDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao
    abstract fun configDao(): ConfigDao
    abstract fun reviewDao(): ProductReviewDao
}
