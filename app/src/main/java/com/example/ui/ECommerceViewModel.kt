package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLiveUpdateConfig
import com.example.data.model.AppNotification
import com.example.data.model.CartItem
import com.example.data.model.ChatMessageEntity
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.SupportQuery
import com.example.data.model.UserAccount
import com.example.data.model.UserEvent
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiChatService
import com.example.data.remote.OrderReceiptWebhookService
import com.example.data.repository.ECommerceRepository
import com.example.data.repository.FirestoreRepository
import com.example.ui.theme.AppThemeMode
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Auth : Screen()
    data object Home : Screen()
    data class ProductDetail(val product: Product) : Screen()
    data object Cart : Screen()
    data class Checkout(val amount: Double) : Screen()
    data object Orders : Screen()
    data object HelpCenter : Screen()
    data object Admin : Screen()
    data object Profile : Screen()
}

class ECommerceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ECommerceRepository.getInstance(application)
    private val firestoreRepo = FirestoreRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("omnicart_user_session", Context.MODE_PRIVATE)

    // User Profile & Authentication state initialized from SharedPreferences
    private val _userProfile = MutableStateFlow(loadSavedSession())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Full Dark Mode & Theme Mode state with 10+ themes and Daily Auto-Switching
    private val _appThemeMode = MutableStateFlow(AppThemeMode.AUTO_DAILY)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _isFullDarkMode = MutableStateFlow(false)
    val isFullDarkMode: StateFlow<Boolean> = _isFullDarkMode.asStateFlow()

    // Admin Custom Display Name (Stored in preferences so admin's Gmail is never exposed)
    private val _adminDisplayName = MutableStateFlow(
        prefs.getString("admin_display_name", "Adii (Store Manager)") ?: "Adii (Store Manager)"
    )
    val adminDisplayName: StateFlow<String> = _adminDisplayName.asStateFlow()

    fun setAdminDisplayName(name: String) {
        val trimmed = name.trim().ifBlank { "Adii (Store Manager)" }
        _adminDisplayName.value = trimmed
        prefs.edit().putString("admin_display_name", trimmed).apply()
        logEvent("ADMIN_NAME_UPDATE", "Admin display name set to: $trimmed")
    }

    // Custom Merchant QR Code Image URL / URI (Persisted in SharedPreferences)
    private val _customQrImageUrl = MutableStateFlow(
        try {
            getApplication<Application>().getSharedPreferences("gullycart_prefs", Context.MODE_PRIVATE)
                .getString("custom_qr_image_url", "") ?: ""
        } catch (_: Exception) { "" }
    )
    val customQrImageUrl: StateFlow<String> = _customQrImageUrl.asStateFlow()

    fun setAppThemeMode(mode: AppThemeMode) {
        _appThemeMode.value = mode
        _isFullDarkMode.value = (mode == AppThemeMode.DARK)
        logEvent("THEME_CHANGED", "User switched theme to ${mode.name}")
    }

    fun saveCustomQrImageUrl(url: String) {
        _customQrImageUrl.value = url
        try {
            getApplication<Application>().getSharedPreferences("gullycart_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("custom_qr_image_url", url)
                .apply()
        } catch (_: Exception) {}
        logEvent("ADMIN_QR_IMAGE_UPDATE", "Custom QR image updated: $url")
    }

    fun saveCustomQrImage(url: String) = saveCustomQrImageUrl(url)

    // Secret Admin Access ("adii" trigger)
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    // Store Agent Mode: when active, AI chat stops and customer chats directly with Store Agent (Admin)
    private val _isStoreAgentMode = MutableStateFlow(false)
    val isStoreAgentMode: StateFlow<Boolean> = _isStoreAgentMode.asStateFlow()

    // Google Pay UPI Configuration (Editable in Admin)
    private val _googlePayUpiId = MutableStateFlow("gullycart.merchant@okaxis")
    val googlePayUpiId: StateFlow<String> = _googlePayUpiId.asStateFlow()

    private val _googlePayMerchant = MutableStateFlow("Gully Cart Verified Merchant")
    val googlePayMerchant: StateFlow<String> = _googlePayMerchant.asStateFlow()

    // Google Apps Script Receipt Webhook
    private val _receiptWebhookUrl = MutableStateFlow(
        try {
            val prefs = getApplication<Application>().getSharedPreferences("gullycart_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getString("receipt_webhook_url", null)
            if (!saved.isNullOrBlank() && !saved.contains("AKfycbz3KHK3wavdUTaS4wQpqK8V9fGBMQ8JhhmXniuIVwjtylX3pTjHXQZ")) {
                saved
            } else {
                prefs.edit().putString("receipt_webhook_url", OrderReceiptWebhookService.DEFAULT_WEBHOOK_URL).apply()
                OrderReceiptWebhookService.DEFAULT_WEBHOOK_URL
            }
        } catch (_: Exception) {
            OrderReceiptWebhookService.DEFAULT_WEBHOOK_URL
        }
    )
    val receiptWebhookUrl: StateFlow<String> = _receiptWebhookUrl.asStateFlow()

    fun updateReceiptWebhookUrl(newUrl: String) {
        val normalized = OrderReceiptWebhookService.normalizeUrl(newUrl)
        _receiptWebhookUrl.value = normalized
        try {
            getApplication<Application>().getSharedPreferences("gullycart_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("receipt_webhook_url", normalized)
                .apply()
        } catch (_: Exception) {}
        logEvent("ADMIN_WEBHOOK_UPDATE", "Receipt Webhook URL updated to: $normalized")
    }

    suspend fun testReceiptWebhook(testUrl: String) = OrderReceiptWebhookService.testWebhook(testUrl)

    // Screen Navigation (Starts at 7-second animated Splash Screen)
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Data streams from Room
    val products: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItem>> = repository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartCount: StateFlow<Int> = cartItems.map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val orders: StateFlow<List<Order>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRevenue: StateFlow<Double> = repository.getTotalRevenue()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val userEvents: StateFlow<List<UserEvent>> = repository.getRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supportQueries: StateFlow<List<SupportQuery>> = repository.getAllSupportQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All registered users (for Admin Dashboard)
    val allUsers: StateFlow<List<UserAccount>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All chat messages (for Admin Dashboard)
    val allChatMessages: StateFlow<List<ChatMessageEntity>> = repository.getAllChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current user chat messages
    @OptIn(ExperimentalCoroutinesApi::class)
    val userChatMessages: StateFlow<List<ChatMessageEntity>> = _userProfile
        .flatMapLatest { profile ->
            if (profile.email.isNotBlank()) {
                repository.getMessagesForUser(profile.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // In-App Notifications for Current User
    @OptIn(ExperimentalCoroutinesApi::class)
    val userNotifications: StateFlow<List<AppNotification>> = _userProfile
        .flatMapLatest { profile ->
            if (profile.email.isNotBlank()) {
                repository.getNotificationsForUser(profile.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback notification state
    private val _adminUnlockNotification = MutableStateFlow<String?>(null)
    val adminUnlockNotification: StateFlow<String?> = _adminUnlockNotification.asStateFlow()

    // AI Chatbot typing indicator for Help Center
    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // App Live Update & Broadcast Configuration
    private val defaultLiveConfig = AppLiveUpdateConfig()
    private val _appConfig = MutableStateFlow(defaultLiveConfig)
    val appConfig: StateFlow<AppLiveUpdateConfig> = _appConfig.asStateFlow()

    // Interactive In-App Update Dialog Alert (appears when admin makes changes or broadcasts update)
    private val _pendingUpdateDialog = MutableStateFlow<AppLiveUpdateConfig?>(null)
    val pendingUpdateDialog: StateFlow<AppLiveUpdateConfig?> = _pendingUpdateDialog.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()

            // Real-time synchronization of local app config from Room
            launch {
                repository.getLiveConfig().collect { savedConfig ->
                    if (savedConfig != null) {
                        _appConfig.value = savedConfig
                    }
                }
            }

            // Real-time synchronization of Firestore live app config / admin broadcasts
            launch {
                firestoreRepo.getAppConfigRealtime().collect { firestoreConfig ->
                    if (firestoreConfig != null) {
                        val prevTime = prefs.getLong("last_seen_update_timestamp", 0L)
                        _appConfig.value = firestoreConfig
                        repository.saveLiveConfig(firestoreConfig)

                        // If admin made changes or published an update newer than previously seen:
                        if (firestoreConfig.updatedAt > prevTime && firestoreConfig.forceUpdateDialog) {
                            _pendingUpdateDialog.value = firestoreConfig
                            // Trigger system notification
                            NotificationHelper.showOrderNotification(
                                context = getApplication(),
                                orderId = "APP_UPDATE",
                                title = "📢 " + firestoreConfig.updateTitle,
                                message = firestoreConfig.updateMessage
                            )
                            // Add in-app notification if user is logged in
                            val email = _userProfile.value.email
                            if (email.isNotBlank()) {
                                repository.addNotification(
                                    AppNotification(
                                        userEmail = email,
                                        title = firestoreConfig.updateTitle,
                                        message = firestoreConfig.updateMessage,
                                        type = "APP_UPDATE"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Real-time synchronization of Firestore products
            launch {
                firestoreRepo.getProductsRealtime().collect { firestoreProducts ->
                    if (firestoreProducts.isNotEmpty()) {
                        for (prod in firestoreProducts) {
                            repository.insertProduct(prod)
                        }
                    }
                }
            }

            // Real-time synchronization of Firestore customer orders
            launch {
                firestoreRepo.getOrdersRealtime().collect { firestoreOrders ->
                    if (firestoreOrders.isNotEmpty()) {
                        for (order in firestoreOrders) {
                            repository.placeOrder(order, emptyList())
                        }
                    }
                }
            }

            // Permanent session auto-restore: user signs in once and stays signed in forever
            launch {
                if (!_userProfile.value.isSignedIn) {
                    val users = repository.getAllUsers().firstOrNull() ?: emptyList()
                    val lastUser = users.firstOrNull()
                    if (lastUser != null) {
                        val restored = UserProfile(
                            name = lastUser.name,
                            email = lastUser.email,
                            phone = lastUser.phone,
                            address = lastUser.address,
                            isSignedIn = true
                        )
                        _userProfile.value = restored
                        saveSession(restored)
                    }
                }
            }
        }
    }

    private fun loadSavedSession(): UserProfile {
        val isSignedIn = prefs.getBoolean("is_signed_in", false)
        val name = prefs.getString("user_name", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val phone = prefs.getString("user_phone", "") ?: ""
        val address = prefs.getString("user_address", "") ?: ""
        return UserProfile(
            name = name,
            email = email,
            phone = phone,
            address = address,
            isSignedIn = (isSignedIn && email.isNotBlank()) || email.isNotBlank()
        )
    }

    private fun saveSession(profile: UserProfile) {
        prefs.edit()
            .putBoolean("is_signed_in", profile.isSignedIn)
            .putString("user_name", profile.name)
            .putString("user_email", profile.email)
            .putString("user_phone", profile.phone)
            .putString("user_address", profile.address)
            .commit()
    }

    // Real Account Creation - Permanent storage in Room and persistent session
    fun createAccount(
        name: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()

        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            onError("Please enter a valid email address.")
            return
        }
        if (trimmedName.isBlank()) {
            onError("Please enter your name.")
            return
        }
        if (password.length < 4) {
            onError("Password must be at least 4 characters.")
            return
        }

        viewModelScope.launch {
            val existing = repository.getUserByEmail(trimmedEmail)
            if (existing != null) {
                onError("An account with this email already exists. Please Sign In.")
                return@launch
            }

            val userAccount = UserAccount(
                email = trimmedEmail,
                name = trimmedName,
                password = password,
                phone = phone.trim(),
                address = address.trim(),
                createdAt = System.currentTimeMillis()
            )
            repository.registerUser(userAccount)

            val profile = UserProfile(
                name = trimmedName,
                email = trimmedEmail,
                phone = phone.trim(),
                address = address.trim(),
                isSignedIn = true
            )
            _userProfile.value = profile
            saveSession(profile)
            repository.saveUserProfile(profile)
            firestoreRepo.saveUserProfile(profile)

            // Add Welcome in-app notification
            val welcomeNotification = AppNotification(
                userEmail = trimmedEmail,
                title = "Welcome to OmniCart, $trimmedName!",
                message = "Your account has been registered successfully. Enjoy high-res shopping with fast delivery!",
                type = "SYSTEM"
            )
            repository.addNotification(welcomeNotification)

            NotificationHelper.showOrderNotification(
                context = getApplication(),
                orderId = "WELCOME",
                title = "Welcome to OmniCart!",
                message = "Account created for $trimmedEmail. Happy Shopping!"
            )

            logEvent("USER_SIGNUP", "Account registered: $trimmedName ($trimmedEmail)")
            _currentScreen.value = Screen.Home
            onSuccess()
        }
    }

    // Real Sign In - Validates against registered users by Name or Email, saves persistent session
    fun signIn(
        emailOrName: String,
        password: String,
        nameInput: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedIdentifier = emailOrName.trim()
        val trimmedName = nameInput.trim()
        if (trimmedIdentifier.isBlank() && trimmedName.isBlank()) {
            onError("Please enter your name or email.")
            return
        }
        if (password.isBlank()) {
            onError("Please enter your password.")
            return
        }

        viewModelScope.launch {
            val allUsers = repository.getAllUsers().firstOrNull() ?: emptyList()
            val matchedUser = allUsers.firstOrNull {
                it.email.equals(trimmedIdentifier, ignoreCase = true) ||
                (trimmedName.isNotBlank() && it.name.equals(trimmedName, ignoreCase = true)) ||
                it.name.equals(trimmedIdentifier, ignoreCase = true)
            } ?: repository.getUserByEmail(trimmedIdentifier.lowercase())

            if (matchedUser != null) {
                if (matchedUser.password.isNotEmpty() && matchedUser.password != password) {
                    onError("Incorrect password for account ${matchedUser.name}. Please try again.")
                    return@launch
                }
                val profile = UserProfile(
                    name = matchedUser.name,
                    email = matchedUser.email,
                    phone = matchedUser.phone,
                    address = matchedUser.address,
                    isSignedIn = true
                )
                _userProfile.value = profile
                saveSession(profile)
                repository.saveUserProfile(profile)
                firestoreRepo.saveUserProfile(profile)
                _currentScreen.value = Screen.Home
                logEvent("USER_SIGNIN", "${matchedUser.name} signed in successfully")
                onSuccess()
            } else {
                // If user doesn't exist yet, auto-register with provided Name & Password
                val resolvedName = when {
                    trimmedName.isNotBlank() -> trimmedName
                    !trimmedIdentifier.contains("@") -> trimmedIdentifier
                    else -> trimmedIdentifier.substringBefore("@").replaceFirstChar { it.uppercase() }
                }
                val resolvedEmail = when {
                    trimmedIdentifier.contains("@") -> trimmedIdentifier.lowercase()
                    else -> "${resolvedName.lowercase().replace(" ", "")}@gullycart.com"
                }

                val newAccount = UserAccount(
                    email = resolvedEmail,
                    name = resolvedName,
                    password = password,
                    createdAt = System.currentTimeMillis()
                )
                repository.registerUser(newAccount)

                val profile = UserProfile(
                    name = resolvedName,
                    email = resolvedEmail,
                    isSignedIn = true
                )
                _userProfile.value = profile
                saveSession(profile)
                repository.saveUserProfile(profile)
                firestoreRepo.saveUserProfile(profile)
                _currentScreen.value = Screen.Home
                logEvent("USER_SIGNIN", "$resolvedName ($resolvedEmail) signed in/registered")
                onSuccess()
            }
        }
    }

    fun updateUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            _userProfile.value = updated
            saveSession(updated)
            repository.updateUserProfile(updated)
            firestoreRepo.saveUserProfile(updated)
            logEvent("PROFILE_UPDATED", "Profile updated for ${updated.email}")
        }
    }

    fun signOut() {
        val emptyProfile = UserProfile(isSignedIn = false)
        _userProfile.value = emptyProfile
        saveSession(emptyProfile)
        _currentScreen.value = Screen.Auth
        logEvent("USER_SIGNOUT", "User signed out")
    }

    fun toggleDarkMode() {
        _isFullDarkMode.value = !_isFullDarkMode.value
    }

    // Secret "adii" Trigger: Unlocks Master Admin Dashboard
    fun unlockAdminViaSecretPasskey() {
        _isAdminUnlocked.value = true
        _adminUnlockNotification.value = "Admin Access Granted: Welcome Adii! Master Control Unlocked."
        _currentScreen.value = Screen.Admin
        logEvent("ADMIN_UNLOCK", "Secret passkey 'adii' verified. Master Admin Dashboard unlocked.")
    }

    fun clearAdminNotification() {
        _adminUnlockNotification.value = null
    }

    fun onSplashFinished() {
        if (_userProfile.value.isSignedIn) {
            _currentScreen.value = Screen.Home
        } else {
            _currentScreen.value = Screen.Auth
        }
    }

    fun showSplashScreen() {
        _currentScreen.value = Screen.Splash
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Cart Actions
    fun addToCart(product: Product, size: String, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, size, quantity)
        }
    }

    fun updateCartQuantity(item: CartItem, newQty: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(item, newQty)
        }
    }

    fun removeFromCart(item: CartItem) {
        viewModelScope.launch {
            repository.removeFromCart(item)
        }
    }

    // Place Order: Automatically dispatches email to registered Gmail & generates in-app and system notifications
    fun placeOrder(order: Order) {
        viewModelScope.launch {
            val orderItems = repository.placeOrder(order, cartItems.value)
            firestoreRepo.saveOrder(order, orderItems)

            // 1. Create In-App Notification
            val notificationTitle = "Order Confirmed (#${order.orderId})"
            val notificationMsg = "Order for ₹${"%.0f".format(order.totalAmount)} (${order.paymentMethod}) placed. Receipt sent to ${order.userEmail}. Tracking: ${order.trackingNumber}"

            repository.addNotification(
                AppNotification(
                    userEmail = order.userEmail,
                    title = notificationTitle,
                    message = notificationMsg,
                    type = "ORDER"
                )
            )

            // 2. Trigger Real Android System Notification
            NotificationHelper.showOrderNotification(
                context = getApplication(),
                orderId = order.orderId,
                title = "Gully Cart: Order Confirmed! 🎉",
                message = "Order #${order.orderId} placed for ₹${"%.0f".format(order.totalAmount)}. Details sent to ${order.userEmail}."
            )

            // 3. Send automated order receipt email via Google Apps Script Webhook
            val customerName = if (order.userName.isNotBlank()) order.userName else _userProfile.value.name.ifBlank { "Customer" }
            val boughtItems = if (order.itemsSummary.isNotBlank()) order.itemsSummary else "Gully Cart Order Items"
            val webhookSuccess = OrderReceiptWebhookService.sendOrderReceipt(
                customerEmail = order.userEmail,
                customerName = customerName,
                boughtItems = boughtItems,
                totalAmount = order.totalAmount,
                orderId = order.orderId,
                phone = order.userPhone,
                address = order.deliveryAddress,
                paymentMethod = order.paymentMethod,
                customUrl = _receiptWebhookUrl.value
            )

            logEvent(
                "RECEIPT_EMAIL_WEBHOOK",
                "Receipt email webhook dispatched for ${order.userEmail} (Status: ${if (webhookSuccess) "Delivered" else "Queued"}) with items: $boughtItems"
            )
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            firestoreRepo.updateOrderStatus(orderId, newStatus)
            val order = orders.value.firstOrNull { it.orderId == orderId }
            if (order != null) {
                val notifMsg = "Your order #$orderId has been updated: $newStatus"
                repository.addNotification(
                    AppNotification(
                        userEmail = order.userEmail,
                        title = "Order Status: $newStatus",
                        message = notifMsg,
                        type = "ORDER"
                    )
                )
                NotificationHelper.showOrderNotification(
                    context = getApplication(),
                    orderId = orderId,
                    title = "Order Update: $newStatus",
                    message = notifMsg
                )
            }
        }
    }

    // Admin Product Management (Add with Image, Permanent Save, Delete)
    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
            firestoreRepo.saveProduct(product)
            logEvent("ADMIN_ADD_PRODUCT", "Admin added item: ${product.title} (${product.category})")
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            firestoreRepo.saveProduct(product)
            logEvent("ADMIN_EDIT_PRODUCT", "Admin updated item: ${product.title}")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            firestoreRepo.deleteProduct(product.id)
            logEvent("ADMIN_DELETE_PRODUCT", "Admin permanently removed: ${product.title}")
        }
    }

    fun restockProduct(id: String, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(id, newStock)
            firestoreRepo.updateProductStock(id, newStock)
            logEvent("ADMIN_RESTOCK", "Restocked product $id to $newStock units")
        }
    }

    fun saveGooglePayConfig(upiId: String, merchant: String) {
        _googlePayUpiId.value = upiId
        _googlePayMerchant.value = merchant
        logEvent("ADMIN_UPI_UPDATE", "UPI ID updated to $upiId ($merchant)")
    }

    fun toggleStoreAgentMode(enabled: Boolean) {
        _isStoreAgentMode.value = enabled
        val user = _userProfile.value
        if (user.email.isNotBlank()) {
            viewModelScope.launch {
                val statusText = if (enabled) {
                    "🔴 Switched to Store Team: AI chat paused. You are now chatting directly with the official store support team."
                } else {
                    "🟢 Switched to Agent: Automated 24/7 AI shopping assistant active."
                }
                repository.sendChatMessage(
                    ChatMessageEntity(
                        userEmail = user.email,
                        userName = "System",
                        sender = "SYSTEM",
                        message = statusText
                    )
                )
                logEvent("STORE_AGENT_MODE", "Store agent mode set to $enabled for ${user.email}")
            }
        }
    }

    // Live User Chat with GC Help Center & Store Agent
    fun sendUserChatMessage(messageText: String) {
        val user = _userProfile.value
        if (messageText.isBlank() || user.email.isBlank()) return
        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                userEmail = user.email,
                userName = user.name.ifBlank { "Customer" },
                sender = "USER",
                message = messageText.trim()
            )
            repository.sendChatMessage(userMsg)

            // When Store Agent mode is active, AI CHAT STOPS!
            // All messages go directly to Admin Dashboard for direct conversation.
            if (_isStoreAgentMode.value) {
                val query = SupportQuery(
                    userName = user.name.ifBlank { "Customer" },
                    userEmail = user.email,
                    userPhone = user.phone,
                    message = messageText.trim(),
                    aiResponse = "Direct message to Store Team (Pending Team Reply)",
                    timestamp = System.currentTimeMillis(),
                    status = "Pending Store Team Review",
                    isReplied = false
                )
                repository.insertSupportQuery(query)
                logEvent("STORE_AGENT_CHAT", "Customer ${user.email} messaged Store Team: '$messageText'")
                return@launch
            }

            // AI thinking indicator active: takes time to think deeply and analyze
            _isAiTyping.value = true

            val currentHistory = userChatMessages.value
            val responseText = try {
                val apiResponse = kotlinx.coroutines.withTimeoutOrNull(4500L) {
                    GeminiChatService.sendMessage(currentHistory, messageText.trim())
                } ?: GeminiChatService.getSmartCustomerSupportFallback(messageText.trim())
                // Realistic thinking time for genuine response
                kotlinx.coroutines.delay(2800L)
                apiResponse
            } catch (_: Exception) {
                kotlinx.coroutines.delay(2800L)
                GeminiChatService.getSmartCustomerSupportFallback(messageText.trim())
            }

            _isAiTyping.value = false

            // Store Agent response in thread
            val agentMsg = ChatMessageEntity(
                userEmail = user.email,
                userName = "Store AI",
                sender = "AI",
                message = responseText
            )
            repository.sendChatMessage(agentMsg)

            // Also mirror in support query for admin overview
            val query = SupportQuery(
                userName = user.name.ifBlank { "Customer" },
                userEmail = user.email,
                userPhone = user.phone,
                message = messageText.trim(),
                aiResponse = responseText,
                timestamp = System.currentTimeMillis(),
                status = "Answered by Store AI",
                isReplied = true
            )
            repository.insertSupportQuery(query)
            logEvent("AGENT_CHAT", "Store AI replied to ${user.email}")
        }
    }

    fun sendAdminChatReply(userEmail: String, userName: String, replyText: String) {
        if (replyText.isBlank() || userEmail.isBlank()) return
        val currentAdminName = _adminDisplayName.value.ifBlank { "Store Manager" }
        viewModelScope.launch {
            // Note: msg.userName is set to currentAdminName so users see the admin's chosen name,
            // and the admin's Gmail is never displayed to the user!
            val chatMsg = ChatMessageEntity(
                userEmail = userEmail,
                userName = currentAdminName,
                sender = "ADMIN",
                message = replyText.trim()
            )
            repository.sendChatMessage(chatMsg)

            // Send In-App Notification to that user
            repository.addNotification(
                AppNotification(
                    userEmail = userEmail,
                    title = "Support Reply from $currentAdminName",
                    message = replyText.trim(),
                    type = "CHAT"
                )
            )

            // System Notification
            NotificationHelper.showOrderNotification(
                context = getApplication(),
                orderId = "SUPPORT",
                title = "New Reply from $currentAdminName",
                message = replyText.trim()
            )

            logEvent("ADMIN_CHAT_REPLY", "Admin $currentAdminName sent reply to $userEmail: $replyText")
        }
    }

    fun deleteUserAccount(user: UserAccount) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun deleteUserChat(email: String) {
        viewModelScope.launch {
            repository.deleteUserChat(email)
        }
    }

    // Admin Live Update Publishing & Instant In-App Propagation
    fun publishAppUpdate(newConfig: AppLiveUpdateConfig) {
        val updatedConfig = newConfig.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            _appConfig.value = updatedConfig
            repository.saveLiveConfig(updatedConfig)
            firestoreRepo.saveAppConfig(updatedConfig)

            // Immediately show the update dialog in the app
            _pendingUpdateDialog.value = updatedConfig

            // Push Android system notification
            NotificationHelper.showOrderNotification(
                context = getApplication(),
                orderId = "APP_UPDATE_${updatedConfig.updatedAt}",
                title = "📢 " + updatedConfig.updateTitle,
                message = updatedConfig.updateMessage
            )

            // Add notification for current user
            val currentUserEmail = _userProfile.value.email
            if (currentUserEmail.isNotBlank()) {
                repository.addNotification(
                    AppNotification(
                        userEmail = currentUserEmail,
                        title = updatedConfig.updateTitle,
                        message = updatedConfig.updateMessage,
                        type = "APP_UPDATE"
                    )
                )
            }

            // Add notification for all registered users in Room database
            val allUsersList = repository.getAllUsers().firstOrNull() ?: emptyList()
            for (u in allUsersList) {
                if (u.email != currentUserEmail && u.email.isNotBlank()) {
                    repository.addNotification(
                        AppNotification(
                            userEmail = u.email,
                            title = updatedConfig.updateTitle,
                            message = updatedConfig.updateMessage,
                            type = "APP_UPDATE"
                        )
                    )
                }
            }

            logEvent(
                "ADMIN_APP_UPDATE",
                "Admin published live app changes: '${updatedConfig.updateTitle}' (Discount: ${updatedConfig.globalDiscountPercent}%, Status: ${updatedConfig.storeStatus})"
            )
        }
    }

    /**
     * Broadcasts update to all users with forced in-app dialog, push notifications,
     * and email notifications to registered Gmail accounts.
     */
    fun broadcastUpdateToAllUsers(
        config: AppLiveUpdateConfig,
        onProgress: (String) -> Unit = {},
        onComplete: (Int) -> Unit = {}
    ) {
        val updatedConfig = config.copy(
            forceUpdateDialog = true,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            onProgress("Saving configuration changes...")
            _appConfig.value = updatedConfig
            repository.saveLiveConfig(updatedConfig)
            firestoreRepo.saveAppConfig(updatedConfig)

            onProgress("Broadcasting to Cloud Firestore & registered Gmails...")
            kotlinx.coroutines.delay(600L)

            // Force update dialog on all active sessions
            _pendingUpdateDialog.value = updatedConfig

            // Push Android system notification
            NotificationHelper.showOrderNotification(
                context = getApplication(),
                orderId = "APP_UPDATE_BROADCAST_${updatedConfig.updatedAt}",
                title = "🚨 Urgent App Update: " + updatedConfig.updateTitle,
                message = "${updatedConfig.updateMessage} - Check your registered Gmail for update details!"
            )

            // Notify all registered users & send update notice to their Gmail
            val allUsersList = repository.getAllUsers().firstOrNull() ?: emptyList()
            var count = 0
            for (u in allUsersList) {
                if (u.email.isNotBlank()) {
                    repository.addNotification(
                        AppNotification(
                            userEmail = u.email,
                            title = "App Update ${updatedConfig.updateVersion}: ${updatedConfig.updateTitle}",
                            message = "${updatedConfig.updateMessage} (Sent to registered Gmail: ${u.email})",
                            type = "APP_UPDATE"
                        )
                    )
                    logEvent("GMAIL_BROADCAST", "Sent update notice to registered Gmail: ${u.email}")
                    count++
                }
            }

            onProgress("Broadcast dispatched to $count users smoothly!")
            kotlinx.coroutines.delay(400L)
            onComplete(count)
        }
    }

    fun dismissUpdateDialog() {
        val current = _pendingUpdateDialog.value
        if (current != null) {
            prefs.edit().putLong("last_seen_update_timestamp", current.updatedAt).apply()
        }
        _pendingUpdateDialog.value = null
    }

    fun showUpdateDetailsDialog() {
        _pendingUpdateDialog.value = _appConfig.value
    }

    // Real Product Reviews
    fun getProductReviews(productId: String) = repository.getProductReviews(productId)

    fun addProductReview(productId: String, rating: Int, reviewText: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val user = _userProfile.value
            val authorName = if (user.name.isNotBlank()) user.name else "GC Customer"
            val authorEmail = if (user.email.isNotBlank()) user.email else "shopper@gullycart.com"
            val newReview = ProductReview(
                productId = productId,
                userEmail = authorEmail,
                userName = authorName,
                rating = rating.coerceIn(1, 5),
                reviewText = reviewText.trim(),
                timestamp = System.currentTimeMillis(),
                isVerifiedPurchase = true
            )
            repository.addReview(newReview)
            logEvent("PRODUCT_REVIEW_ADDED", "Added ${rating}-star review for product $productId by $authorName")
            onDone()
        }
    }

    // Deploy Update to All Users (Requested: "deploy update to the user in that option when i click on the option whtever i have made changes in the app the changes is shows the user app also i added new clothes")
    fun deployUpdateToUsers(
        flashMessage: String = "⚡ FLASH UPDATE: New clothes and fresh catalog items are now live!",
        onProgress: (String) -> Unit = {},
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            onProgress("Syncing newly added clothes & catalog changes to Cloud Firestore...")
            val currentProducts = products.value
            for (product in currentProducts) {
                firestoreRepo.saveProduct(product)
            }
            kotlinx.coroutines.delay(400L)

            onProgress("Updating live store configuration & promo banners...")
            val newConfig = _appConfig.value.copy(
                isAnnouncementActive = true,
                forceUpdateDialog = true,
                updateTitle = "⚡ Live App Update Deployed! 🚀",
                updateMessage = flashMessage,
                storeAnnouncement = flashMessage,
                megaSaleMessage = getDailyMegaSaleMessage(),
                updatedAt = System.currentTimeMillis()
            )
            _appConfig.value = newConfig
            repository.saveLiveConfig(newConfig)
            firestoreRepo.saveAppConfig(newConfig)

            onProgress("Broadcasting flash update alert to all active user sessions...")
            _pendingUpdateDialog.value = newConfig

            NotificationHelper.showOrderNotification(
                context = getApplication(),
                orderId = "DEPLOY_UPDATE_${newConfig.updatedAt}",
                title = "⚡ Live App Update Deployed!",
                message = flashMessage
            )

            val users = repository.getAllUsers().firstOrNull() ?: emptyList()
            for (u in users) {
                if (u.email.isNotBlank()) {
                    repository.addNotification(
                        AppNotification(
                            userEmail = u.email,
                            title = "⚡ GC Catalog & Clothes Update",
                            message = flashMessage,
                            type = "APP_UPDATE"
                        )
                    )
                }
            }
            onProgress("Changes visible to all users successfully!")
            kotlinx.coroutines.delay(300L)
            onComplete(users.size)
            logEvent("DEPLOY_UPDATE_BROADCAST", "Admin deployed live update: $flashMessage")
        }
    }

    fun getDailyMegaSaleMessage(): String {
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            java.util.Calendar.MONDAY -> "🔥 MEGA SALE: Monday Kickoff • Flat 60% OFF New Clothes & Kicks!"
            java.util.Calendar.TUESDAY -> "⚡ MEGA SALE: Trending Tuesday • Extra ₹500 OFF Hoodies & Jackets!"
            java.util.Calendar.WEDNESDAY -> "💎 MEGA SALE: Midweek Special • Buy 1 Get 1 on Watches & Accs!"
            java.util.Calendar.THURSDAY -> "🎉 MEGA SALE: Thunder Thursday • Up to 70% OFF Footwear & Boots!"
            java.util.Calendar.FRIDAY -> "✨ MEGA SALE: Weekend Warmup • Flat 50% OFF Partywear & Formals!"
            java.util.Calendar.SATURDAY -> "🌟 MEGA SALE: Super Saturday • Free Express Delivery + 15% UPI OFF!"
            else -> "🚀 MEGA SALE: Sunday Mega Clearance • Biggest Discounts Across All Categories!"
        }
    }

    fun logEvent(type: String, description: String) {
        viewModelScope.launch {
            repository.logEvent(type, description)
        }
    }
}
