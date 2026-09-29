package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AppLiveUpdateConfig
import com.example.data.model.FirestoreSchema
import com.example.data.model.Order
import com.example.data.model.OrderItemEntity
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Cloud Firestore Repository for Gully Cart
 * Implements real-time cloud data storage & synchronization for:
 * 1. Products Catalog
 * 2. User Profiles
 * 3. Order History & Line Items
 * 4. App Live Update Configuration
 */
class FirestoreRepository private constructor(private val context: Context) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                val app = FirebaseApp.initializeApp(context)
                if (app != null) FirebaseFirestore.getInstance() else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Firestore initialization deferred: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "FirestoreRepository"

        @Volatile
        private var INSTANCE: FirestoreRepository? = null

        fun getInstance(context: Context): FirestoreRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // =========================================================================
    // 1. PRODUCTS SCHEMA & REAL-TIME CRUD
    // =========================================================================

    /**
     * Real-time stream of all products from Cloud Firestore.
     */
    fun getProductsRealtime(): Flow<List<Product>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = db.collection(FirestoreSchema.COLLECTION_PRODUCTS)
            .orderBy(FirestoreSchema.ProductFields.CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for real-time products: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val productList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            FirestoreSchema.mapToProduct(doc.id, data)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse product document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(productList)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Fetch a single product by ID in real-time.
     */
    fun getProductById(id: String): Flow<Product?> = callbackFlow {
        val db = firestore
        if (db == null || id.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection(FirestoreSchema.COLLECTION_PRODUCTS)
            .document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching product $id: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        trySend(FirestoreSchema.mapToProduct(snapshot.id, data))
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Save or update a product in Cloud Firestore.
     */
    suspend fun saveProduct(product: Product): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        val data = FirestoreSchema.productToFirestoreMap(product)
        db.collection(FirestoreSchema.COLLECTION_PRODUCTS)
            .document(product.id)
            .set(data, SetOptions.merge())
            .await()
    }

    /**
     * Delete a product permanently from Cloud Firestore.
     */
    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        db.collection(FirestoreSchema.COLLECTION_PRODUCTS)
            .document(productId)
            .delete()
            .await()
    }

    /**
     * Update product stock quantity in Cloud Firestore.
     */
    suspend fun updateProductStock(productId: String, newStock: Int): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        db.collection(FirestoreSchema.COLLECTION_PRODUCTS)
            .document(productId)
            .update(FirestoreSchema.ProductFields.STOCK, newStock)
            .await()
    }

    // =========================================================================
    // 2. USER PROFILES SCHEMA & REAL-TIME CRUD
    // =========================================================================

    /**
     * Real-time stream of a user profile by email from Cloud Firestore.
     */
    fun getUserProfileRealtime(email: String): Flow<UserProfile?> = callbackFlow {
        val db = firestore
        val sanitizedEmail = email.trim().lowercase()
        if (db == null || sanitizedEmail.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection(FirestoreSchema.COLLECTION_USER_PROFILES)
            .document(sanitizedEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for user profile $sanitizedEmail: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        trySend(FirestoreSchema.mapToUserProfile(snapshot.id, data))
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * One-time fetch of a user profile from Cloud Firestore.
     */
    suspend fun getUserProfileOnce(email: String): UserProfile? {
        return try {
            val db = firestore ?: return null
            val sanitizedEmail = email.trim().lowercase()
            if (sanitizedEmail.isBlank()) return null

            val doc = db.collection(FirestoreSchema.COLLECTION_USER_PROFILES)
                .document(sanitizedEmail)
                .get()
                .await()

            val data = doc.data
            if (doc.exists() && data != null) {
                FirestoreSchema.mapToUserProfile(doc.id, data)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get user profile: ${e.message}")
            null
        }
    }

    /**
     * Save or update a user profile in Cloud Firestore.
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        val sanitizedEmail = profile.email.trim().lowercase()
        if (sanitizedEmail.isBlank()) throw IllegalArgumentException("User email cannot be blank")

        val data = FirestoreSchema.userProfileToFirestoreMap(profile.copy(email = sanitizedEmail))
        db.collection(FirestoreSchema.COLLECTION_USER_PROFILES)
            .document(sanitizedEmail)
            .set(data, SetOptions.merge())
            .await()
    }

    /**
     * Update user address details in Cloud Firestore.
     */
    suspend fun updateUserAddress(
        email: String,
        address: String,
        city: String,
        state: String,
        postalCode: String
    ): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        val sanitizedEmail = email.trim().lowercase()
        val updates = mapOf(
            FirestoreSchema.UserProfileFields.ADDRESS to address,
            FirestoreSchema.UserProfileFields.CITY to city,
            FirestoreSchema.UserProfileFields.STATE to state,
            FirestoreSchema.UserProfileFields.POSTAL_CODE to postalCode,
            FirestoreSchema.UserProfileFields.UPDATED_AT to System.currentTimeMillis()
        )
        db.collection(FirestoreSchema.COLLECTION_USER_PROFILES)
            .document(sanitizedEmail)
            .update(updates)
            .await()
    }

    /**
     * Real-time stream of all user profiles (for Admin oversight).
     */
    fun getAllUserProfilesRealtime(): Flow<List<UserProfile>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection(FirestoreSchema.COLLECTION_USER_PROFILES)
            .orderBy(FirestoreSchema.UserProfileFields.UPDATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching all user profiles: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        try {
                            FirestoreSchema.mapToUserProfile(doc.id, data)
                        } catch (_: Exception) { null }
                    }
                    trySend(list)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    // =========================================================================
    // 3. ORDER HISTORY SCHEMA & REAL-TIME CRUD
    // =========================================================================

    /**
     * Real-time stream of all orders from Cloud Firestore.
     */
    fun getOrdersRealtime(): Flow<List<Order>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = db.collection(FirestoreSchema.COLLECTION_ORDERS)
            .orderBy(FirestoreSchema.OrderFields.TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for real-time orders: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orderList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            FirestoreSchema.mapToOrder(doc.id, data)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse order document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(orderList)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Real-time stream of customer order history for a specific user email.
     */
    fun getOrdersForUserRealtime(userEmail: String): Flow<List<Order>> = callbackFlow {
        val db = firestore
        val sanitized = userEmail.trim().lowercase()
        if (db == null || sanitized.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = db.collection(FirestoreSchema.COLLECTION_ORDERS)
            .whereEqualTo(FirestoreSchema.OrderFields.USER_EMAIL, sanitized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for user orders: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orderList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            FirestoreSchema.mapToOrder(doc.id, data)
                        } catch (_: Exception) { null }
                    }.sortedByDescending { it.timestamp }
                    trySend(orderList)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Save customer order and order history in Cloud Firestore.
     * Also saves structured line items in the document and the subcollection `orders/{orderId}/items`.
     */
    suspend fun saveOrder(order: Order, items: List<OrderItemEntity> = emptyList()): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        val orderData = FirestoreSchema.orderToFirestoreMap(order, items)

        // Write primary order document
        val orderDocRef = db.collection(FirestoreSchema.COLLECTION_ORDERS).document(order.orderId)
        orderDocRef.set(orderData, SetOptions.merge()).await()

        // Write subcollection items for granular queries
        for (item in items) {
            val itemDocId = if (item.id > 0) item.id.toString() else item.productId
            val itemData = FirestoreSchema.orderItemToFirestoreMap(item)
            orderDocRef.collection(FirestoreSchema.SUBCOLLECTION_ORDER_ITEMS)
                .document(itemDocId)
                .set(itemData, SetOptions.merge())
                .await()
        }
    }

    /**
     * Get a specific order and its details in real-time.
     */
    fun getOrderById(orderId: String): Flow<Order?> = callbackFlow {
        val db = firestore
        if (db == null || orderId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection(FirestoreSchema.COLLECTION_ORDERS)
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for order $orderId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        trySend(FirestoreSchema.mapToOrder(snapshot.id, data))
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Update order delivery/fulfillment status in Cloud Firestore.
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        val updates = mutableMapOf<String, Any>(
            FirestoreSchema.OrderFields.STATUS to newStatus
        )
        if (newStatus.equals("Delivered", ignoreCase = true)) {
            updates[FirestoreSchema.OrderFields.DELIVERED_AT] = System.currentTimeMillis()
        }
        db.collection(FirestoreSchema.COLLECTION_ORDERS)
            .document(orderId)
            .update(updates)
            .await()
    }

    /**
     * Delete an order permanently from Cloud Firestore.
     */
    suspend fun deleteOrder(orderId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        db.collection(FirestoreSchema.COLLECTION_ORDERS)
            .document(orderId)
            .delete()
            .await()
    }

    // =========================================================================
    // 4. REAL-TIME APP CONFIG & BROADCAST UPDATE MANAGEMENT
    // =========================================================================

    /**
     * Real-time stream of live app update configuration from Cloud Firestore.
     */
    fun getAppConfigRealtime(): Flow<AppLiveUpdateConfig?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = db.collection(FirestoreSchema.COLLECTION_CONFIG)
            .document("live_config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for live app config: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val config = AppLiveUpdateConfig(
                            id = snapshot.getString("id") ?: "live_config",
                            updateVersion = snapshot.getString("updateVersion") ?: "v2.5.0",
                            updateTitle = snapshot.getString("updateTitle") ?: "OmniCart Live Festival Update!",
                            updateMessage = snapshot.getString("updateMessage") ?: "Exclusive discounts and speed improvements active!",
                            updateType = snapshot.getString("updateType") ?: "FEATURE",
                            storeAnnouncement = snapshot.getString("storeAnnouncement") ?: "🔥 FESTIVAL SALE: Extra 20% OFF automatically applied on all orders!",
                            isAnnouncementActive = snapshot.getBoolean("isAnnouncementActive") ?: true,
                            globalDiscountPercent = snapshot.getLong("globalDiscountPercent")?.toInt() ?: 20,
                            promoBadge = snapshot.getString("promoBadge") ?: "MEGA SALE 20% OFF",
                            storeStatus = snapshot.getString("storeStatus") ?: "OPEN",
                            forceUpdateDialog = snapshot.getBoolean("forceUpdateDialog") ?: true,
                            updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                        trySend(config)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse app config: ${e.message}")
                    }
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Save or publish app live update config to Cloud Firestore.
     */
    suspend fun saveAppConfig(config: AppLiveUpdateConfig): Result<Unit> = runCatching {
        val db = firestore ?: throw IllegalStateException("Firestore not initialized")
        val data = hashMapOf(
            "id" to config.id,
            "updateVersion" to config.updateVersion,
            "updateTitle" to config.updateTitle,
            "updateMessage" to config.updateMessage,
            "updateType" to config.updateType,
            "storeAnnouncement" to config.storeAnnouncement,
            "isAnnouncementActive" to config.isAnnouncementActive,
            "globalDiscountPercent" to config.globalDiscountPercent,
            "promoBadge" to config.promoBadge,
            "storeStatus" to config.storeStatus,
            "forceUpdateDialog" to config.forceUpdateDialog,
            "updatedAt" to config.updatedAt
        )
        db.collection(FirestoreSchema.COLLECTION_CONFIG)
            .document("live_config")
            .set(data, SetOptions.merge())
            .await()
    }
}
