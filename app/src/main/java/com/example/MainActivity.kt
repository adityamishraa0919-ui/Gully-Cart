package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.components.AppUpdateDialog
import com.example.ui.ECommerceViewModel
import com.example.ui.Screen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HelpCenterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private val viewModel: ECommerceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val isFullDarkMode by viewModel.isFullDarkMode.collectAsStateWithLifecycle()
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = appThemeMode, darkTheme = isFullDarkMode) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: ECommerceViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isFullDarkMode by viewModel.isFullDarkMode.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allChatMessages by viewModel.allChatMessages.collectAsStateWithLifecycle()
    val userChatMessages by viewModel.userChatMessages.collectAsStateWithLifecycle()
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val isAiTyping by viewModel.isAiTyping.collectAsStateWithLifecycle()
    val isStoreAgentMode by viewModel.isStoreAgentMode.collectAsStateWithLifecycle()
    val googlePayUpiId by viewModel.googlePayUpiId.collectAsStateWithLifecycle()
    val googlePayMerchant by viewModel.googlePayMerchant.collectAsStateWithLifecycle()
    val customQrImageUrl by viewModel.customQrImageUrl.collectAsStateWithLifecycle()
    val receiptWebhookUrl by viewModel.receiptWebhookUrl.collectAsStateWithLifecycle()
    val adminNotification by viewModel.adminUnlockNotification.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val pendingUpdateDialog by viewModel.pendingUpdateDialog.collectAsStateWithLifecycle()
    val adminDisplayName by viewModel.adminDisplayName.collectAsStateWithLifecycle()

    val homeGridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Admin unlock notification
    LaunchedEffect(adminNotification) {
        adminNotification?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAdminNotification()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentScreen) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // Hardware back navigation handling
    BackHandler(enabled = currentScreen !is Screen.Home && currentScreen !is Screen.Auth && currentScreen !is Screen.Splash) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        when (currentScreen) {
            is Screen.Checkout -> viewModel.navigateTo(Screen.Cart)
            is Screen.ProductDetail -> viewModel.navigateTo(Screen.Home)
            is Screen.Cart -> viewModel.navigateTo(Screen.Home)
            is Screen.Orders -> viewModel.navigateTo(Screen.Home)
            is Screen.HelpCenter -> viewModel.navigateTo(Screen.Home)
            is Screen.Admin -> viewModel.navigateTo(Screen.Home)
            is Screen.Profile -> viewModel.navigateTo(Screen.Home)
            else -> {}
        }
    }

    val showBottomBar = currentScreen !is Screen.Auth && currentScreen !is Screen.Checkout && currentScreen !is Screen.Splash

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_home_item")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Cart,
                        onClick = { viewModel.navigateTo(Screen.Cart) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text(cartCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                            }
                        },
                        label = { Text("Cart", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_cart_item")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Orders,
                        onClick = { viewModel.navigateTo(Screen.Orders) },
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Orders") },
                        label = { Text("Orders", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_orders_item")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.HelpCenter,
                        onClick = { viewModel.navigateTo(Screen.HelpCenter) },
                        icon = { Icon(Icons.Default.HeadsetMic, contentDescription = "Help") },
                        label = { Text("AI Help", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_help_item")
                    )

                    // Admin Tab: Only visible if secret "adii" has been spoken or typed!
                    if (isAdminUnlocked) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Admin,
                            onClick = { viewModel.navigateTo(Screen.Admin) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = Color(0xFFFF9900)
                                )
                            },
                            label = {
                                Text(
                                    text = "Admin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9900)
                                )
                            },
                            modifier = Modifier.testTag("nav_admin_item")
                        )
                    }

                    NavigationBarItem(
                        selected = currentScreen is Screen.Profile,
                        onClick = { viewModel.navigateTo(Screen.Profile) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_profile_item")
                    )
                }
            }
        }
    ) { innerPadding ->
        // Real-Time In-App Update Alert Modal (Broadcasts whenever admin publishes changes)
        pendingUpdateDialog?.let { config ->
            AppUpdateDialog(
                config = config,
                onDismiss = { viewModel.dismissUpdateDialog() },
                onExplore = {
                    viewModel.dismissUpdateDialog()
                    viewModel.navigateTo(Screen.Home)
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Splash -> {
                    SplashScreen(
                        onSplashComplete = {
                            viewModel.onSplashFinished()
                        }
                    )
                }

                is Screen.Auth -> {
                    AuthScreen(
                        onSignIn = { emailOrName, password, onError ->
                            viewModel.signIn(
                                emailOrName = emailOrName,
                                password = password,
                                onSuccess = {
                                    Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                },
                                onError = onError
                            )
                        },
                        onCreateAccount = { name, email, password, phone, address, onError ->
                            viewModel.createAccount(
                                name = name,
                                email = email,
                                password = password,
                                phone = phone,
                                address = address,
                                onSuccess = {
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = onError
                            )
                        }
                    )
                }

                is Screen.Home -> {
                    HomeScreen(
                        products = products,
                        cartCount = cartCount,
                        isAdminUnlocked = isAdminUnlocked,
                        isFullDarkMode = isFullDarkMode,
                        appConfig = appConfig,
                        gridState = homeGridState,
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onSecretAdiiTriggered = { viewModel.unlockAdminViaSecretPasskey() },
                        onNavigateToAdmin = { viewModel.navigateTo(Screen.Admin) },
                        onNavigateToCart = { viewModel.navigateTo(Screen.Cart) },
                        onNavigateToHelpCenter = { viewModel.navigateTo(Screen.HelpCenter) },
                        onOpenUpdateDialog = { viewModel.showUpdateDetailsDialog() },
                        onProductClick = { product ->
                            viewModel.navigateTo(Screen.ProductDetail(product))
                        },
                        onAddToCart = { product ->
                            val defaultSize = product.availableSizes.split(",").firstOrNull()?.trim() ?: "Standard"
                            viewModel.addToCart(product, defaultSize, 1)
                            Toast.makeText(context, "Added ${product.title} to Cart!", Toast.LENGTH_SHORT).show()
                        },
                        onLogEvent = { type, desc ->
                            viewModel.logEvent(type, desc)
                        }
                    )
                }

                is Screen.ProductDetail -> {
                    val reviews by viewModel.getProductReviews(screen.product.id)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    ProductDetailScreen(
                        product = screen.product,
                        reviews = reviews,
                        userProfile = userProfile,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onAddToCart = { product, size, qty ->
                            viewModel.addToCart(product, size, qty)
                            Toast.makeText(context, "Added $qty x ${product.title} ($size) to Cart!", Toast.LENGTH_SHORT).show()
                        },
                        onBuyNow = { product, size, qty ->
                            viewModel.addToCart(product, size, qty)
                            viewModel.navigateTo(Screen.Cart)
                        },
                        onSubmitReview = { rating, reviewText ->
                            viewModel.addProductReview(screen.product.id, rating, reviewText) {
                                Toast.makeText(context, "Real Review submitted! Rating updated.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLogEvent = { type, desc ->
                            viewModel.logEvent(type, desc)
                        }
                    )
                }

                is Screen.Cart -> {
                    CartScreen(
                        cartItems = cartItems,
                        userProfile = userProfile,
                        appConfig = appConfig,
                        onUpdateQuantity = { item, newQty ->
                            viewModel.updateCartQuantity(item, newQty)
                        },
                        onRemoveItem = { item ->
                            viewModel.removeFromCart(item)
                            Toast.makeText(context, "Removed item from cart", Toast.LENGTH_SHORT).show()
                        },
                        onProceedToCheckout = { total, _ ->
                            viewModel.navigateTo(Screen.Checkout(total))
                        },
                        onStartShopping = { viewModel.navigateTo(Screen.Home) }
                    )
                }

                is Screen.Checkout -> {
                    CheckoutScreen(
                        amount = screen.amount,
                        cartItems = cartItems,
                        userProfile = userProfile,
                        customUpiId = googlePayUpiId,
                        customMerchantName = googlePayMerchant,
                        customQrImageUrl = customQrImageUrl,
                        onBack = { viewModel.navigateTo(Screen.Cart) },
                        onOrderPlaced = { order ->
                            viewModel.placeOrder(order)
                        },
                        onContinueShopping = { viewModel.navigateTo(Screen.Home) },
                        onViewOrders = { viewModel.navigateTo(Screen.Orders) }
                    )
                }

                is Screen.Orders -> {
                    OrdersScreen(
                        orders = orders,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onStartShopping = { viewModel.navigateTo(Screen.Home) }
                    )
                }

                is Screen.HelpCenter -> {
                    HelpCenterScreen(
                        userName = userProfile.name.ifBlank { "Shopper" },
                        userEmail = userProfile.email.ifBlank { "shopper@gullycart.com" },
                        chatMessages = userChatMessages,
                        isAiTyping = isAiTyping,
                        isStoreAgentMode = isStoreAgentMode,
                        onToggleStoreAgentMode = { enabled ->
                            viewModel.toggleStoreAgentMode(enabled)
                        },
                        onSendMessage = { msg ->
                            viewModel.sendUserChatMessage(msg)
                        },
                        onBack = { viewModel.navigateTo(Screen.Home) }
                    )
                }

                is Screen.Admin -> {
                    AdminScreen(
                        products = products,
                        orders = orders,
                        allUsers = allUsers,
                        allChatMessages = allChatMessages,
                        totalRevenue = totalRevenue,
                        googlePayUpiId = googlePayUpiId,
                        googlePayMerchant = googlePayMerchant,
                        appConfig = appConfig,
                        onPublishAppUpdate = { newConf ->
                            viewModel.publishAppUpdate(newConf)
                        },
                        onPreviewUpdateDialog = {
                            viewModel.showUpdateDetailsDialog()
                        },
                        onSaveGooglePayConfig = { upi, merchant ->
                            viewModel.saveGooglePayConfig(upi, merchant)
                        },
                        customQrImageUrl = customQrImageUrl,
                        onSaveCustomQrImage = { viewModel.saveCustomQrImage(it) },
                        receiptWebhookUrl = receiptWebhookUrl,
                        onSaveReceiptWebhookUrl = { viewModel.updateReceiptWebhookUrl(it) },
                        onTestReceiptWebhook = { viewModel.testReceiptWebhook(it) },
                        onAddProduct = { product ->
                            viewModel.addProduct(product)
                        },
                        onUpdateProduct = { product ->
                            viewModel.updateProduct(product)
                        },
                        onDeleteProduct = { product ->
                            viewModel.deleteProduct(product)
                        },
                        onRestockProduct = { id, newStock ->
                            viewModel.restockProduct(id, newStock)
                        },
                        onUpdateOrderStatus = { orderId, status ->
                            viewModel.updateOrderStatus(orderId, status)
                        },
                        onSendAdminReply = { userEmail, userName, reply ->
                            viewModel.sendAdminChatReply(userEmail, userName, reply)
                        },
                        onDeleteUserChat = { userEmail ->
                            viewModel.deleteUserChat(userEmail)
                        },
                        onDeleteUser = { user ->
                            viewModel.deleteUserAccount(user)
                        },
                        adminDisplayName = adminDisplayName,
                        onUpdateAdminDisplayName = { viewModel.setAdminDisplayName(it) },
                        onDeployUpdateToUsers = { flash, onProg, onComp ->
                            viewModel.deployUpdateToUsers(flash, onProg, onComp)
                        },
                        onBack = { viewModel.navigateTo(Screen.Home) }
                    )
                }

                is Screen.Profile -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        isAdminUnlocked = isAdminUnlocked,
                        isFullDarkMode = isFullDarkMode,
                        appThemeMode = appThemeMode,
                        appConfig = appConfig,
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onSelectThemeMode = { viewModel.setAppThemeMode(it) },
                        onNavigateToOrders = { viewModel.navigateTo(Screen.Orders) },
                        onNavigateToHelpCenter = { viewModel.navigateTo(Screen.HelpCenter) },
                        onNavigateToAdmin = { viewModel.navigateTo(Screen.Admin) },
                        onOpenUpdateDialog = { viewModel.showUpdateDetailsDialog() },
                        onSignOut = { viewModel.signOut() }
                    )
                }
            }
        }
    }
}
