package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLiveUpdateConfig
import com.example.data.model.ChatMessageEntity
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserAccount
import com.example.data.remote.OrderReceiptWebhookService
import com.example.data.remote.WebhookTestResult
import com.example.ui.components.GooglePayQRCodeView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    products: List<Product>,
    orders: List<Order>,
    allUsers: List<UserAccount>,
    allChatMessages: List<ChatMessageEntity>,
    totalRevenue: Double,
    googlePayUpiId: String,
    googlePayMerchant: String,
    customQrImageUrl: String = "",
    onSaveCustomQrImage: (String) -> Unit = {},
    receiptWebhookUrl: String = OrderReceiptWebhookService.DEFAULT_WEBHOOK_URL,
    onSaveReceiptWebhookUrl: (String) -> Unit = {},
    onTestReceiptWebhook: suspend (String) -> WebhookTestResult = { OrderReceiptWebhookService.testWebhook(it) },
    appConfig: AppLiveUpdateConfig = AppLiveUpdateConfig(),
    onPublishAppUpdate: (AppLiveUpdateConfig) -> Unit = {},
    onPreviewUpdateDialog: () -> Unit = {},
    onSaveGooglePayConfig: (String, String) -> Unit,
    onAddProduct: (Product) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onRestockProduct: (String, Int) -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit,
    onSendAdminReply: (userEmail: String, userName: String, reply: String) -> Unit,
    onDeleteUserChat: (userEmail: String) -> Unit,
    onDeleteUser: (UserAccount) -> Unit,
    adminDisplayName: String = "Adii (Store Manager)",
    onUpdateAdminDisplayName: (String) -> Unit = {},
    onDeployUpdateToUsers: (flashMessage: String, onProgress: (String) -> Unit, onComplete: (Int) -> Unit) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var selectedUserForChat by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        "Orders & Sales (${orders.size})",
        "Registered Users (${allUsers.size})",
        "Catalog Management (${products.size})",
        "Help Center Chats",
        "QR Settings",
        "Deploy Update to Users 🚀",
        "Live App Updates (${appConfig.updateVersion})"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF9900),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gully Cart Admin Hub",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Full Master Control Active",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> AdminOrdersTab(
                        orders = orders,
                        totalRevenue = totalRevenue,
                        onUpdateOrderStatus = onUpdateOrderStatus
                    )
                    1 -> AdminUsersTab(
                        users = allUsers,
                        onDeleteUser = onDeleteUser,
                        onOpenChatWithUser = { email ->
                            selectedUserForChat = email
                            selectedTab = 3
                        }
                    )
                    2 -> AdminCatalogTab(
                        products = products,
                        onOpenAddDialog = { showAddProductDialog = true },
                        onDeleteProduct = { productToDelete = it },
                        onRestockProduct = onRestockProduct
                    )
                    3 -> AdminChatsTab(
                        allChatMessages = allChatMessages,
                        allUsers = allUsers,
                        initialSelectedUser = selectedUserForChat,
                        adminDisplayName = adminDisplayName,
                        onUpdateAdminDisplayName = onUpdateAdminDisplayName,
                        onSendReply = onSendAdminReply,
                        onClearChat = onDeleteUserChat
                    )
                    4 -> AdminQrSettingsTab(
                        googlePayUpiId = googlePayUpiId,
                        googlePayMerchant = googlePayMerchant,
                        customQrImageUrl = customQrImageUrl,
                        receiptWebhookUrl = receiptWebhookUrl,
                        onSaveGooglePayConfig = onSaveGooglePayConfig,
                        onSaveCustomQrImage = onSaveCustomQrImage,
                        onSaveReceiptWebhookUrl = onSaveReceiptWebhookUrl,
                        onTestReceiptWebhook = onTestReceiptWebhook
                    )
                    5 -> AdminLiveUpdatesTab(
                        currentConfig = appConfig,
                        onPublishUpdate = onPublishAppUpdate,
                        onPreviewDialog = onPreviewUpdateDialog,
                        onDeployUpdateToUsers = onDeployUpdateToUsers
                    )
                    6 -> AdminLiveUpdatesTab(
                        currentConfig = appConfig,
                        onPublishUpdate = onPublishAppUpdate,
                        onPreviewDialog = onPreviewUpdateDialog,
                        onDeployUpdateToUsers = onDeployUpdateToUsers
                    )
                }
            }
        }
    }

    // Add Product Dialog (with Image Upload & Permanent Save)
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onAddProduct = { prod ->
                onAddProduct(prod)
                showAddProductDialog = false
                Toast.makeText(context, "Product permanently saved to catalog!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Product Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?") },
            text = {
                Text("Are you sure you want to delete '${productToDelete!!.title}'? This will permanently remove it from the catalog.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(productToDelete!!)
                        productToDelete = null
                        Toast.makeText(context, "Product permanently deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: ORDERS & SALES WITH USER GMAIL, ADDRESS, PHONE & VERIFY
// -------------------------------------------------------------
@Composable
private fun AdminOrdersTab(
    orders: List<Order>,
    totalRevenue: Double,
    onUpdateOrderStatus: (String, String) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Revenue Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Sales Revenue",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${"%.2f".format(totalRevenue)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Orders",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${orders.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Customer Orders Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "When customers buy via COD or Online QR, their orders with Gmail, phone & address will appear here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        items(orders, key = { it.orderId }) { order ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header: Order ID + Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "#${order.orderId}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dateFormat.format(Date(order.timestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (order.status.contains("Verified") || order.status.contains("Delivered")) Color(0xFF10B981).copy(alpha = 0.2f)
                            else Color(0xFFFF9900).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = order.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (order.status.contains("Verified") || order.status.contains("Delivered")) Color(0xFF10B981)
                                else Color(0xFFFF9900),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    // Customer Details Box (Gmail, Phone, Address visible)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Customer: ${order.userName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2874F0))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gmail: ${order.userEmail}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF2874F0)
                            )
                        }

                        if (order.userPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Phone: ${order.userPhone}", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Delivery: ${order.deliveryAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Order Items & Payment
                    Text(
                        text = "Items: ${order.itemsSummary}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Payment: ${order.paymentMethod}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (order.paymentMethod.contains("COD")) Color(0xFF10B981) else Color(0xFF1A73E8)
                            )
                            if (order.paymentProof != null) {
                                Text(
                                    text = "Proof: ${order.paymentProof}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "₹${"%.2f".format(order.totalAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons for Admin to update Status / Verify Payment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!order.status.contains("Verified")) {
                            Button(
                                onClick = {
                                    onUpdateOrderStatus(order.orderId, "Payment Verified & Processing")
                                    Toast.makeText(context, "Payment verified for #${order.orderId}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify & Confirm", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val nextStatus = when (order.status) {
                                    "Confirmed & Processing", "Payment Verified & Processing" -> "Out for Delivery"
                                    "Out for Delivery" -> "Delivered"
                                    else -> "Confirmed & Processing"
                                }
                                onUpdateOrderStatus(order.orderId, nextStatus)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Update Status", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: REGISTERED USERS (Gmail, Name, Phone, Address, Delete)
// -------------------------------------------------------------
@Composable
private fun AdminUsersTab(
    users: List<UserAccount>,
    onDeleteUser: (UserAccount) -> Unit,
    onOpenChatWithUser: (String) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Registered User Accounts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Users who created accounts or signed into the app",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${users.size}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        if (users.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Registered Accounts Yet", fontWeight = FontWeight.Bold)
                        Text("Accounts created by shoppers will appear here permanently.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(users, key = { it.email }) { user ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2874F0),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(onClick = {
                            onDeleteUser(user)
                            Toast.makeText(context, "User account removed", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (user.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Phone: ${user.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (user.address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Address: ${user.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Joined: ${dateFormat.format(Date(user.createdAt))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { onOpenChatWithUser(user.email) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat with User", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: CATALOG MANAGEMENT (UPLOAD PRODUCT WITH IMAGE + DELETE)
// -------------------------------------------------------------
@Composable
private fun AdminCatalogTab(
    products: List<Product>,
    onOpenAddDialog: () -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onRestockProduct: (String, Int) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Product Header Button
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Catalog Inventory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Total ${products.size} Products in Store",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onOpenAddDialog,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("admin_add_product_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Product", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(products, key = { it.id }) { product ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${product.price.toInt()}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = product.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stock: ${product.stock} units | Sizes: ${product.availableSizes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete Product Button
                    IconButton(
                        onClick = { onDeleteProduct(product) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape)
                            .testTag("delete_product_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: HELP CENTER CHATS WITH ALL USERS (User Gmail Visible)
// -------------------------------------------------------------
@Composable
private fun AdminChatsTab(
    allChatMessages: List<ChatMessageEntity>,
    allUsers: List<UserAccount>,
    initialSelectedUser: String?,
    adminDisplayName: String = "Adii (Store Manager)",
    onUpdateAdminDisplayName: (String) -> Unit = {},
    onSendReply: (userEmail: String, userName: String, reply: String) -> Unit,
    onClearChat: (userEmail: String) -> Unit
) {
    val context = LocalContext.current
    var selectedUserEmail by remember { mutableStateOf(initialSelectedUser) }
    var replyText by remember { mutableStateOf("") }
    var adminCustomName by remember(adminDisplayName) { mutableStateOf(adminDisplayName) }
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Group messages by userEmail
    val userChatMap = allChatMessages.groupBy { it.userEmail }

    // Combined unique emails from messages and registered users
    val activeChatEmails = (userChatMap.keys + allUsers.map { it.email }).distinct().filter { it.isNotBlank() }

    if (selectedUserEmail == null) {
        // Chat Conversations List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Help Center User Conversations",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "GMAIL HIDDEN",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set your public name below. Users will only see this name when you text back; your personal Gmail address will never be visible.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adminCustomName,
                            onValueChange = {
                                adminCustomName = it
                                onUpdateAdminDisplayName(it)
                            },
                            label = { Text("Admin Display Name shown to users") },
                            placeholder = { Text("e.g. Adii (Store Manager)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_display_name_input")
                        )
                    }
                }
            }

            if (activeChatEmails.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Customer Messages Yet", fontWeight = FontWeight.Bold)
                            Text("Messages sent by customers in AI Help Center will appear here in real-time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(activeChatEmails) { email ->
                val msgs = userChatMap[email] ?: emptyList()
                val lastMsg = msgs.lastOrNull()
                val user = allUsers.firstOrNull { it.email == email }
                val userName = user?.name ?: msgs.firstOrNull()?.userName ?: "Customer"

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedUserEmail = email }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Gmail: $email",
                                fontSize = 12.sp,
                                color = Color(0xFF2874F0),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastMsg?.message ?: "No messages sent yet. Tap to start chat.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (msgs.any { it.sender == "USER" }) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${msgs.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msgs.any { it.sender == "USER" }) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Individual User Chat View
        val targetEmail = selectedUserEmail!!
        val msgs = userChatMap[targetEmail] ?: emptyList()
        val user = allUsers.firstOrNull { it.email == targetEmail }
        val userName = user?.name ?: msgs.firstOrNull()?.userName ?: "Customer"

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedUserEmail = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to List")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Chat with $userName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = targetEmail, fontSize = 11.sp, color = Color(0xFF2874F0), fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = {
                    onClearChat(targetEmail)
                    Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (msgs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No messages in this conversation yet. Send a message to start.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(msgs, key = { it.id }) { msg ->
                    val isAdmin = msg.sender == "ADMIN"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAdmin) Color(0xFFFF9900).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isAdmin) "Admin (You)" else "$userName ($targetEmail)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isAdmin) Color(0xFFFF9900) else Color(0xFF2874F0)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = msg.message, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = timeFormat.format(Date(msg.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Reply Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to $targetEmail...") },
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(targetEmail, userName, replyText)
                                replyText = ""
                                Toast.makeText(context, "Reply sent to $targetEmail", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = replyText.isNotBlank(),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Reply",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: QR & UPI SETTINGS
// -------------------------------------------------------------
@Composable
private fun AdminQrSettingsTab(
    googlePayUpiId: String,
    googlePayMerchant: String,
    customQrImageUrl: String = "",
    receiptWebhookUrl: String = OrderReceiptWebhookService.DEFAULT_WEBHOOK_URL,
    onSaveGooglePayConfig: (String, String) -> Unit,
    onSaveCustomQrImage: (String) -> Unit = {},
    onSaveReceiptWebhookUrl: (String) -> Unit = {},
    onTestReceiptWebhook: suspend (String) -> WebhookTestResult = { OrderReceiptWebhookService.testWebhook(it) }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var upiId by remember { mutableStateOf(googlePayUpiId) }
    var merchant by remember { mutableStateOf(googlePayMerchant) }
    var webhookUrl by remember { mutableStateOf(receiptWebhookUrl) }

    var isTestingWebhook by remember { mutableStateOf(false) }
    var webhookTestStatus by remember { mutableStateOf<WebhookTestResult?>(null) }
    var showAppsScriptGuide by remember { mutableStateOf(false) }

    val qrPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveCustomQrImage(uri.toString())
            Toast.makeText(context, "Custom Merchant QR uploaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. UPI Configuration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configure Payment UPI & Merchant Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "This UPI ID and merchant name will be embedded into the QR code shown to customers during checkout.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("Merchant UPI ID") },
                    placeholder = { Text("e.g. gullycart.merchant@okaxis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant Display Name") },
                    placeholder = { Text("e.g. Gully Cart Store") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSaveGooglePayConfig(upiId, merchant)
                        Toast.makeText(context, "UPI settings saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save UPI Configuration", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Custom QR Image Uploader Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Merchant QR Image Upload",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "Upload a static photo of your official Google Pay / PhonePe / Paytm standee QR. It will be permanently stored and shown at checkout.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (customQrImageUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = customQrImageUrl,
                            contentDescription = "Uploaded Merchant QR",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                qrPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Replace QR")
                        }

                        Button(
                            onClick = {
                                onSaveCustomQrImage("")
                                Toast.makeText(context, "Custom QR removed. Dynamic UPI QR active.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Remove QR")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            qrPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Merchant QR Image", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 3. Automated Gmail Receipt Webhook (Google Apps Script) Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Gmail Receipt Webhook",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "When an order is completed, Gully Cart sends order details via POST to this Google Apps Script to email the customer their invoice receipt.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Google Apps Script Webhook URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Webhook test status result indicator
                if (webhookTestStatus != null) {
                    val status = webhookTestStatus!!
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (status.success) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (status.success) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (status.success) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = status.message,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (status.success) Color(0xFF047857) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingWebhook = true
                                webhookTestStatus = onTestReceiptWebhook(webhookUrl)
                                isTestingWebhook = false
                            }
                        },
                        enabled = !isTestingWebhook,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingWebhook) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...")
                        } else {
                            Text("Test Webhook")
                        }
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onSaveReceiptWebhookUrl(webhookUrl)
                            Toast.makeText(context, "Webhook URL saved!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Webhook")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle guide button
                TextButton(
                    onClick = { showAppsScriptGuide = !showAppsScriptGuide },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showAppsScriptGuide) "▲ Hide Google Apps Script Guide" else "▼ How to Setup Google Apps Script (1-Min Guide)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showAppsScriptGuide) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "1. Open script.google.com -> Click '+ New Project'\n" +
                                        "2. Replace code with this snippet:\n\n" +
                                        "function doPost(e) {\n" +
                                        "  var data = JSON.parse(e.postData.contents);\n" +
                                        "  MailApp.sendEmail({\n" +
                                        "    to: data.email,\n" +
                                        "    subject: 'Gully Cart Receipt - ₹' + data.total,\n" +
                                        "    htmlBody: '<h3>Hi ' + data.name + ',</h3><p>Your order for <b>' + data.items + '</b> totaling <b>₹' + data.total + '</b> is confirmed!</p>'\n" +
                                        "  });\n" +
                                        "  return ContentService.createTextOutput(JSON.stringify({ status: 'ok' }));\n" +
                                        "}\n\n" +
                                        "3. Click 'Deploy' > 'New Deployment'\n" +
                                        "4. Select type: 'Web app'\n" +
                                        "5. Execute as: 'Me'\n" +
                                        "6. Who has access: 'Anyone' (IMPORTANT: Must be Anyone so Google allows calls without 404)\n" +
                                        "7. Copy the Web App URL and paste above!",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Live QR Code Preview
        GooglePayQRCodeView(
            upiId = upiId,
            merchantName = merchant,
            amount = 1499.0,
            customQrImageUrl = customQrImageUrl
        )
    }
}

// -------------------------------------------------------------
// ADD PRODUCT DIALOG (WITH IMAGE UPLOAD & PERMANENT ROOM SAVE)
// -------------------------------------------------------------
@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onAddProduct: (Product) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("men") }
    var priceText by remember { mutableStateOf("") }
    var originalPriceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("25") }
    var sizesText by remember { mutableStateOf("S, M, L, XL") }
    var badgeText by remember { mutableStateOf("Trending") }
    var imageUrl by remember { mutableStateOf("") }
    var localImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            localImageUri = uri
            imageUrl = uri.toString()
            Toast.makeText(context, "Product image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val categories = listOf("men", "women", "accessories", "watches", "goggles", "earpods", "boots")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Product to Store Catalog",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Image Upload Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (localImageUri != null || imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(localImageUri ?: imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Image", fontSize = 12.sp)
                            }

                            Text("or enter URL below", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (or use Upload button)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Product Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Selector
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (cat == "boots") sizesText = "UK 7, UK 8, UK 9, UK 10, UK 11"
                            },
                            label = { Text(cat.uppercase(), fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(4).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (cat == "boots") sizesText = "UK 7, UK 8, UK 9, UK 10, UK 11"
                            },
                            label = { Text(cat.uppercase(), fontSize = 10.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = originalPriceText,
                        onValueChange = { originalPriceText = it },
                        label = { Text("MRP (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock Units") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = badgeText,
                        onValueChange = { badgeText = it },
                        label = { Text("Badge") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = sizesText,
                    onValueChange = { sizesText = it },
                    label = { Text("Available Sizes (comma separated)") },
                    placeholder = { Text("S, M, L, XL or UK 7, UK 8, UK 9") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 999.0
                    val op = originalPriceText.toDoubleOrNull() ?: (p * 1.5)
                    val discount = if (op > p) (((op - p) / op) * 100).toInt() else 20
                    val finalImage = if (imageUrl.isNotBlank()) imageUrl
                    else "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=90"

                    val newProduct = Product(
                        id = "prod_${UUID.randomUUID().toString().take(8)}",
                        title = title.ifBlank { "Gully Cart Premium $category" },
                        description = description.ifBlank { "Top quality item crafted with premium materials." },
                        category = category,
                        price = p,
                        originalPrice = op,
                        discountPercent = discount,
                        rating = 0f,
                        ratingCount = 0,
                        imageUrl = finalImage,
                        is4K = true,
                        stock = stockText.toIntOrNull() ?: 20,
                        availableSizes = sizesText.ifBlank { "Standard" },
                        badge = badgeText.ifBlank { "New Arrival" },
                        brand = "Gully Cart"
                    )
                    onAddProduct(newProduct)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save to Catalog Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// LIVE APP UPDATES & BROADCAST CONFIGURATION TAB
// -------------------------------------------------------------
@Composable
private fun AdminLiveUpdatesTab(
    currentConfig: AppLiveUpdateConfig,
    onPublishUpdate: (AppLiveUpdateConfig) -> Unit,
    onPreviewDialog: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(currentConfig) { mutableStateOf(currentConfig.updateTitle) }
    var version by remember(currentConfig) { mutableStateOf(currentConfig.updateVersion) }
    var message by remember(currentConfig) { mutableStateOf(currentConfig.updateMessage) }
    var announcement by remember(currentConfig) { mutableStateOf(currentConfig.storeAnnouncement) }
    var discountPercent by remember(currentConfig) { mutableIntStateOf(currentConfig.globalDiscountPercent) }
    var promoBadge by remember(currentConfig) { mutableStateOf(currentConfig.promoBadge) }
    var storeStatus by remember(currentConfig) { mutableStateOf(currentConfig.storeStatus) }
    var updateType by remember(currentConfig) { mutableStateOf(currentConfig.updateType) }
    var forceDialog by remember(currentConfig) { mutableStateOf(currentConfig.forceUpdateDialog) }
    var isAnnouncementActive by remember(currentConfig) { mutableStateOf(currentConfig.isAnnouncementActive) }
    var hasJustPublished by remember { mutableStateOf(false) }

    val discountOptions = listOf(0, 10, 15, 20, 25, 30, 50)
    val statusOptions = listOf(
        "OPEN" to "🟢 Normal Open",
        "EXPRESS_DELIVERY" to "⚡ 30-Min Delivery",
        "FESTIVAL_SALE" to "🎉 Festival Sale",
        "MAINTENANCE" to "🛠️ Under Maintenance"
    )
    val updateTypeOptions = listOf(
        "FEATURE" to "✨ Feature Update",
        "PROMO" to "🔥 Flash Offer",
        "MAINTENANCE" to "🛠️ System Maintenance",
        "CRITICAL" to "🚨 Urgent Notice"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Broadcast Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE SYNC ACTIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = currentConfig.updateVersion,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "App Live Updates & Remote Control",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Any change you save here propagates live across the app instantly. Users receive an in-app update popup alert, system push notification, and real-time discounts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Active Discount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentConfig.globalDiscountPercent}% OFF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Store Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentConfig.storeStatus.replace("_", " "), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Popup Alert", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (currentConfig.forceUpdateDialog) "ENABLED" else "SILENT", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick 1-Click Preset Templates
        Column {
            Text(
                text = "⚡ Quick Preset Templates",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "Apply pre-configured update packages in 1 tap:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Festival Sale Preset
                OutlinedButton(
                    onClick = {
                        title = "Gully Cart Festival Mega Sale Live! 🎉"
                        version = "v2.5.2"
                        message = "Festival special! Flat 25% instant storewide discount applied on all footwear, watches, kurtas, and accessories. Enjoy lightning 30-min delivery!"
                        announcement = "🎉 FESTIVAL SALE: Extra 25% OFF automatically applied on all orders!"
                        discountPercent = 25
                        promoBadge = "FESTIVAL 25% OFF"
                        storeStatus = "FESTIVAL_SALE"
                        updateType = "PROMO"
                        forceDialog = true
                        isAnnouncementActive = true
                        Toast.makeText(context, "Festival preset selected! Click 'Publish' below to push.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🎪 Festival Sale", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Flash Drop Preset
                OutlinedButton(
                    onClick = {
                        title = "Limited Stock Flash Drop! ⚡"
                        version = "v2.5.3"
                        message = "Flash drop is now live! Exclusive 35% discount on limited inventory electronics, boots, and goggles for the next 2 hours."
                        announcement = "⚡ FLASH DROP: 35% OFF limited stock drops across all collections!"
                        discountPercent = 35
                        promoBadge = "FLASH DROP 35% OFF"
                        storeStatus = "EXPRESS_DELIVERY"
                        updateType = "PROMO"
                        forceDialog = true
                        isAnnouncementActive = true
                        Toast.makeText(context, "Flash Drop preset selected! Click 'Publish' below to push.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⚡ Flash Drop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Feature Upgrade Preset
                OutlinedButton(
                    onClick = {
                        title = "Gully Cart v2.5 Experience Upgrade 🚀"
                        version = "v2.5.0"
                        message = "We've upgraded the app with ultra-res high quality images, real-time Firestore database synchronization, and Google Pay instant QR checkout."
                        announcement = "🚀 NEW VERSION: Live order tracking & verified Google Pay checkout active!"
                        discountPercent = 15
                        promoBadge = "NEW V2.5"
                        storeStatus = "OPEN"
                        updateType = "FEATURE"
                        forceDialog = true
                        isAnnouncementActive = true
                        Toast.makeText(context, "Feature update preset selected! Click 'Publish' below.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🚀 Feature v2.5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Normal Open Preset
                OutlinedButton(
                    onClick = {
                        title = "Gully Cart Store Live Update 🟢"
                        version = "v2.5.0"
                        message = "Welcome to Gully Cart! Standard catalog active with verified COD, UPI QR, and rapid delivery."
                        announcement = "✨ Welcome to Gully Cart: High-Res Shopping with Cash on Delivery & UPI!"
                        discountPercent = 10
                        promoBadge = "STANDARD 10% OFF"
                        storeStatus = "OPEN"
                        updateType = "FEATURE"
                        forceDialog = false
                        isAnnouncementActive = true
                        Toast.makeText(context, "Normal Clean preset selected! Click 'Publish' below.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🟢 Reset Clean", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Editable Custom Fields
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Broadcast Update Message & Content",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Update Title & Version
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Update Popup Headline") },
                        placeholder = { Text("e.g. OmniCart Festival Update!") },
                        singleLine = true,
                        modifier = Modifier.weight(1.8f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Version") },
                        placeholder = { Text("v2.5.0") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Update Message (What's New)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("What's New / Update Message for Customers") },
                    placeholder = { Text("Explain what changed in the app, new discounts, new collections, or delivery updates...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Store Announcement Banner
                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it },
                    label = { Text("Home Screen Announcement Banner") },
                    placeholder = { Text("e.g. 🔥 FESTIVAL SALE: Extra 20% OFF automatically applied!") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Promo Badge Text
                OutlinedTextField(
                    value = promoBadge,
                    onValueChange = { promoBadge = it },
                    label = { Text("Promo Badge Label") },
                    placeholder = { Text("e.g. MEGA DEAL 25% OFF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Global Storewide Discount Selector
                Column {
                    Text(
                        text = "Global Storewide Extra Discount: $discountPercent%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        discountOptions.forEach { disc ->
                            FilterChip(
                                selected = discountPercent == disc,
                                onClick = { discountPercent = disc },
                                label = { Text(if (disc == 0) "None" else "$disc%", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Store Operating Mode Selector
                Column {
                    Text(
                        text = "Store Operating Status Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusOptions.take(2).forEach { (stKey, stLabel) ->
                            FilterChip(
                                selected = storeStatus == stKey,
                                onClick = { storeStatus = stKey },
                                label = { Text(stLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusOptions.drop(2).forEach { (stKey, stLabel) ->
                            FilterChip(
                                selected = storeStatus == stKey,
                                onClick = { storeStatus = stKey },
                                label = { Text(stLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Update Urgency / Type Selector
                Column {
                    Text(
                        text = "Update Category / Urgency",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        updateTypeOptions.take(2).forEach { (uKey, uLabel) ->
                            FilterChip(
                                selected = updateType == uKey,
                                onClick = { updateType = uKey },
                                label = { Text(uLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        updateTypeOptions.drop(2).forEach { (uKey, uLabel) ->
                            FilterChip(
                                selected = updateType == uKey,
                                onClick = { updateType = uKey },
                                label = { Text(uLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Update Dialog on Customer Screen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Interactive update modal pops up immediately for users", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = forceDialog,
                        onCheckedChange = { forceDialog = it },
                        modifier = Modifier.testTag("toggle_force_dialog")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Live Announcement Banner", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Prominent marquee banner on Home Screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isAnnouncementActive,
                        onCheckedChange = { isAnnouncementActive = it },
                        modifier = Modifier.testTag("toggle_announcement")
                    )
                }
            }
        }

        // Live Preview Box
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👀 Live Customer Preview",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(onClick = onPreviewDialog) {
                        Text("Preview Modal Dialog", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Simulated Home Banner Preview
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF9900)
                            ) {
                                Text(
                                    text = promoBadge.ifBlank { "LIVE UPDATE" },
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• $version",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = announcement.ifBlank { "Welcome to Gully Cart!" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Broadcast Action Button
        Button(
            onClick = {
                val newConfig = AppLiveUpdateConfig(
                    id = "live_config",
                    updateVersion = version.ifBlank { "v2.5.0" },
                    updateTitle = title.ifBlank { "Gully Cart Live Store Update" },
                    updateMessage = message.ifBlank { "Exciting changes have been applied to the app!" },
                    updateType = updateType,
                    storeAnnouncement = announcement.ifBlank { "🔥 SPECIAL LIVE OFFER: Storewide discounts active!" },
                    isAnnouncementActive = isAnnouncementActive,
                    globalDiscountPercent = discountPercent,
                    promoBadge = promoBadge.ifBlank { "LIVE OFFER" },
                    storeStatus = storeStatus,
                    forceUpdateDialog = forceDialog,
                    updatedAt = System.currentTimeMillis()
                )
                onPublishUpdate(newConfig)
                hasJustPublished = true
                Toast.makeText(
                    context,
                    "🚀 App Update Broadcasted! Real-time changes, update message & notifications pushed to all users.",
                    Toast.LENGTH_LONG
                ).show()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9900),
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_live_app_update_btn")
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Changes & Broadcast Update to App",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (hasJustPublished) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Changes are now live in user apps! The update message and notification have been triggered.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

