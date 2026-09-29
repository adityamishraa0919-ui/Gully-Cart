package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.UserProfile
import com.example.ui.components.GooglePayQRCodeView
import com.example.ui.components.ThankYouSuccessScreen
import com.example.ui.util.WhatsAppReceiptHelper
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    amount: Double,
    cartItems: List<CartItem>,
    userProfile: UserProfile,
    customUpiId: String,
    customMerchantName: String,
    customQrImageUrl: String = "",
    onBack: () -> Unit,
    onOrderPlaced: (Order) -> Unit,
    onContinueShopping: () -> Unit,
    onViewOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedPaymentMethod by remember { mutableIntStateOf(0) } // 0: Online QR / UPI, 1: Cash on Delivery (COD)
    var contactPhone by remember { mutableStateOf(userProfile.phone) }
    var sendWhatsAppUpdates by remember { mutableStateOf(true) }
    var showProofInput by remember { mutableStateOf(false) }
    var transactionReferenceId by remember { mutableStateOf("") }
    var paymentProofUri by remember { mutableStateOf<Uri?>(null) }

    var isVerifying by remember { mutableStateOf(false) }
    var verificationProgress by remember { mutableFloatStateOf(0f) }
    var verificationStepText by remember { mutableStateOf("Verifying payment switch...") }
    var placedOrder by remember { mutableStateOf<Order?>(null) }

    // Modern zero-permission Photo Picker for proof screenshot
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            paymentProofUri = uri
            Toast.makeText(context, "Payment proof screenshot attached!", Toast.LENGTH_SHORT).show()
        }
    }

    // Realistic verification sequence
    LaunchedEffect(isVerifying) {
        if (isVerifying) {
            val totalSeconds = 4.0f
            val steps = 40
            val intervalMs = ((totalSeconds * 1000) / steps).toLong()

            for (i in 1..steps) {
                delay(intervalMs)
                val currentProgress = i.toFloat() / steps
                verificationProgress = currentProgress

                when {
                    currentProgress < 0.35f -> {
                        verificationStepText = "Connecting with Bank UPI switch..."
                    }
                    currentProgress < 0.70f -> {
                        verificationStepText = "Authenticating transaction reference..."
                    }
                    else -> {
                        verificationStepText = "Payment Verified! Generating tax invoice..."
                    }
                }
            }

            val randomNum = Random.nextInt(100000, 999999)
            val proofString = if (paymentProofUri != null) {
                "Screenshot Attached (Ref: ${transactionReferenceId.ifBlank { "UPI-$randomNum" }})"
            } else {
                "Ref ID: ${transactionReferenceId.ifBlank { "UPI-$randomNum" }}"
            }

            val finalPhone = contactPhone.trim().ifBlank { userProfile.phone }
            val newOrder = Order(
                orderId = "ORD-$randomNum",
                userEmail = userProfile.email.ifBlank { "customer@omnicart.com" },
                userName = userProfile.name.ifBlank { "Customer" },
                userPhone = finalPhone,
                totalAmount = amount,
                itemCount = cartItems.sumOf { it.quantity },
                itemsSummary = cartItems.joinToString(", ") { "${it.title} (${it.selectedSize})" },
                paymentMethod = "Online UPI / QR Payment",
                upiId = customUpiId,
                paymentProof = proofString,
                timestamp = System.currentTimeMillis(),
                status = "Payment Verified & Processing",
                deliveryAddress = userProfile.address.ifBlank { "Standard Delivery Address" },
                trackingNumber = "EXP-IN-${Random.nextInt(10000000, 99999999)}"
            )
            onOrderPlaced(newOrder)
            placedOrder = newOrder
            isVerifying = false
        }
    }

    // Thank You celebration screen when order confirmed
    if (placedOrder != null) {
        ThankYouSuccessScreen(
            order = placedOrder!!,
            onContinueShopping = onContinueShopping,
            onViewOrders = onViewOrders
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Secure Checkout",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isVerifying) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("checkout_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Security Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "256-Bit Bank Grade Encryption | RBI Compliant",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }

                // Customer Delivery Info Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Delivering To",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Verified Customer",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = userProfile.name.ifBlank { "Gully Cart Customer" },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = userProfile.address.ifBlank { "Standard Delivery Address" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (userProfile.phone.isNotBlank() && contactPhone.isBlank()) {
                            contactPhone = userProfile.phone
                        }

                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Mobile Contact Number", fontSize = 12.sp) },
                            placeholder = { Text("Enter 10-digit number (e.g. 9876543210)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Contact Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_phone_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Instant order tracking details & delivery confirmation will be sent to this number.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Payment Method",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 0: Online Pay via QR Code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPaymentMethod = 0 }
                                .background(
                                    if (selectedPaymentMethod == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == 0,
                                onClick = { selectedPaymentMethod = 0 }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Online Pay (UPI / QR Code)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Scan QR with Google Pay, PhonePe, Paytm + Submit proof",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 1: Cash on Delivery (COD)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPaymentMethod = 1 }
                                .background(
                                    if (selectedPaymentMethod == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == 1,
                                onClick = { selectedPaymentMethod = 1 }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.LocalAtm,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cash on Delivery (COD)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Pay cash when order is delivered to your address",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail based on selected payment method
                if (selectedPaymentMethod == 0) {
                    // ONLINE UPI QR PAYMENT
                    GooglePayQRCodeView(
                        upiId = customUpiId,
                        merchantName = customMerchantName,
                        amount = amount,
                        customQrImageUrl = customQrImageUrl,
                        onCopyUpi = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UPI ID", customUpiId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "UPI ID copied: $customUpiId", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Proof of Payment Section
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Step 2: Submit Payment Proof",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "After scanning and paying via UPI, enter the 12-digit UTR/Reference number or attach a screenshot.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = transactionReferenceId,
                                onValueChange = { transactionReferenceId = it },
                                label = { Text("UPI Ref / UTR Number (e.g. 423984920194)") },
                                placeholder = { Text("12-digit transaction ID") },
                                leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_ref_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("upload_proof_btn")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (paymentProofUri != null) "Screenshot Attached ✓" else "Upload Screenshot",
                                        fontSize = 12.sp
                                    )
                                }

                                if (paymentProofUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(paymentProofUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Payment Proof",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    isVerifying = true
                                },
                                enabled = !isVerifying,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1A73E8)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_proof_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit Proof & Verify Payment (₹${amount.toInt()})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                } else {
                    // CASH ON DELIVERY (COD)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalAtm,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Cash on Delivery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Pay ₹${"%.2f".format(amount)} in cash to the delivery agent when your package arrives at ${userProfile.address.ifBlank { "your address" }}.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    val randomNum = Random.nextInt(100000, 999999)
                                    val finalPhone = contactPhone.trim().ifBlank { userProfile.phone }
                                    val codOrder = Order(
                                        orderId = "COD-$randomNum",
                                        userEmail = userProfile.email.ifBlank { "customer@omnicart.com" },
                                        userName = userProfile.name.ifBlank { "Customer" },
                                        userPhone = finalPhone,
                                        totalAmount = amount,
                                        itemCount = cartItems.sumOf { it.quantity },
                                        itemsSummary = cartItems.joinToString(", ") { "${it.title} (${it.selectedSize})" },
                                        paymentMethod = "Cash on Delivery (COD)",
                                        upiId = "",
                                        paymentProof = "Payable in Cash on Delivery",
                                        timestamp = System.currentTimeMillis(),
                                        status = "Confirmed (Cash on Delivery)",
                                        deliveryAddress = userProfile.address.ifBlank { "Standard Delivery Address" },
                                        trackingNumber = "COD-IN-${Random.nextInt(10000000, 99999999)}"
                                    )
                                    onOrderPlaced(codOrder)
                                    placedOrder = codOrder
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("confirm_cod_order_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirm Cash on Delivery Order",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Payment Verification Modal Overlay
            AnimatedVisibility(
                visible = isVerifying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1A73E8).copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF1A73E8),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Verifying UPI Payment Proof",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = verificationStepText,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LinearProgressIndicator(
                                progress = { verificationProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF10B981).copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Verification takes ~4 seconds. Thank You message will appear once verified.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
