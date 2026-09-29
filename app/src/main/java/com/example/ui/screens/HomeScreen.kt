package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLiveUpdateConfig
import com.example.data.model.Product
import com.example.ui.util.bounceClick
import com.example.ui.components.ProductCard

data class CategoryMeta(
    val id: String,
    val name: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    products: List<Product>,
    cartCount: Int,
    isAdminUnlocked: Boolean,
    isFullDarkMode: Boolean,
    appConfig: AppLiveUpdateConfig = AppLiveUpdateConfig(),
    gridState: LazyGridState = rememberLazyGridState(),
    onToggleDarkMode: () -> Unit,
    onSecretAdiiTriggered: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToHelpCenter: () -> Unit,
    onOpenUpdateDialog: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onLogEvent: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var showSecretVoiceModal by remember { mutableStateOf(false) }

    // Speech Recognizer: matches user's voice to catalog search query
    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()?.trim() ?: ""
            if (spokenText.isNotBlank()) {
                searchQuery = spokenText
                onLogEvent("VOICE_SEARCH", "User spoke: '$spokenText'")
                Toast.makeText(context, "Voice Search: \"$spokenText\"", Toast.LENGTH_SHORT).show()
                if (spokenText.equals("adii", ignoreCase = true)) {
                    onSecretAdiiTriggered()
                }
            }
        }
    }

    // Microphone Permission Launcher (Asks first for mic permission as requested!)
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search clothes, boots, watches or say 'adii'...")
            }
            try {
                speechRecognitionLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Speech recognition not available on device", Toast.LENGTH_SHORT).show()
                showSecretVoiceModal = true
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice search", Toast.LENGTH_LONG).show()
        }
    }

    fun startVoiceSearch() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search clothes, boots, watches or say 'adii'...")
            }
            try {
                speechRecognitionLauncher.launch(intent)
            } catch (e: Exception) {
                showSecretVoiceModal = true
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val categories = listOf(
        CategoryMeta("all", "All", "Explore All"),
        CategoryMeta("men", "Men", "Shirts & Tees"),
        CategoryMeta("women", "Women", "Dresses & Kurtas"),
        CategoryMeta("boots", "Boots", "UK 6 to 12"),
        CategoryMeta("watches", "Watches", "Luxury & Smart"),
        CategoryMeta("goggles", "Goggles", "Polarized UV"),
        CategoryMeta("earpods", "Earpods", "ANC & Gaming"),
        CategoryMeta("accessories", "Accessories", "Belts & Wallets")
    )

    // Filter products
    val displayedProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { p ->
            (selectedCategory == "all" || p.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isEmpty() || p.title.contains(searchQuery, ignoreCase = true) || p.brand.contains(searchQuery, ignoreCase = true) || p.category.contains(searchQuery, ignoreCase = true))
        }
    }

    val flashDeals = remember(products) {
        products.filter { it.discountPercent >= 50 }
    }

    // Dynamic sale ticker: Changes offers automatically every day and periodic intervals
    var secondsElapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            secondsElapsed++
        }
    }
    val currentMinuteIndex = (secondsElapsed / 60) % 5
    val secondsLeftInMinute = 60 - (secondsElapsed % 60)

    val dayOfWeek = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) }
    val dailyMegaSales = remember(dayOfWeek) {
        listOf(
            Pair("🔥 GC MEGA SALE (SUNDAY SPECIAL)", "Flat 65% OFF on Streetwear, Hoodies & Kicks!"),
            Pair("⚡ GC MONDAY POWER SALE", "Up to 60% OFF on Boots, High-Tops & Utility Shirts!"),
            Pair("💎 GC TUESDAY LUXE CARNIVAL", "Buy 1 Get 1 Free on Luxury Watches & Accessories!"),
            Pair("🎉 GC WEDNESDAY MEGA SALE", "Instant ₹500 Cashback with Google Pay UPI Checkout!"),
            Pair("✨ GC THURSDAY FESTIVE DROP", "Flat 70% Off Designer Kurtas, Shirts & Pants!"),
            Pair("🚀 GC FRIDAY FLASH SALE", "Massive Deals on Polarized UV Goggles & ANC Earpods!"),
            Pair("🌟 GC SATURDAY SUPER BLOWOUT", "Free 2-Hour Express Delivery & 20% Extra Storewide!")
        )
    }
    val todaySale = dailyMegaSales[(dayOfWeek - 1).coerceIn(0, 6)]

    val timedDrops = remember {
        listOf(
            Pair("🔥 LIVE DROP • ROTATING DEALS", "Special Clearance on Winter & Summer Wear!"),
            Pair("⚡ FLASH DROP • LIVE NOW", "Exclusive 15% Bonus Discount on GPay UPI!"),
            Pair("🎉 MEGA CARNIVAL • TODAY ONLY", "New Apparel & Boots Collection Just Added!"),
            Pair("💎 GC VIP HOUR • LIVE STORE", "Free Express Doorstep Delivery Across India!"),
            Pair("✨ FESTIVAL DROP • HOT PICKS", "Top Trending Streetwear & Casual Shoes on Sale!")
        )
    }
    val activeFestivalDeal = if (secondsElapsed % 120 < 60) todaySale else timedDrops[currentMinuteIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gully Cart",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Premium Official Store",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            actions = {
                // Secret Admin Button (Visible once "adii" is spoken or typed!)
                if (isAdminUnlocked) {
                    IconButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.testTag("top_admin_hub_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF9900),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Full Dark Mode AMOLED toggle (Explicitly requested)
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.testTag("dark_mode_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isFullDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Full Dark Mode",
                        tint = if (isFullDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // AI Help Center Button
                IconButton(
                    onClick = onNavigateToHelpCenter,
                    modifier = Modifier.testTag("top_help_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = "AI Help Center",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Cart Icon with Badge
                IconButton(
                    onClick = onNavigateToCart,
                    modifier = Modifier.testTag("top_cart_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(text = cartCount.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Search Bar with Secret "adii" voice & text trigger!
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { text ->
                        searchQuery = text
                        // Secret "adii" trigger:
                        if (text.trim().equals("adii", ignoreCase = true)) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onSecretAdiiTriggered()
                            searchQuery = ""
                        } else if (text.isNotBlank()) {
                            onLogEvent("SEARCH", "Searched for '$text'")
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search fashion, boots, watches and more...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    startVoiceSearch()
                                },
                                modifier = Modifier
                                    .bounceClick()
                                    .testTag("voice_search_mic_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )
            }
        }

        // Main Content Grid (State is preserved so navigating back from Product Detail keeps exact scroll position!)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Admin Broadcast Announcement & Update Banner
            if (appConfig.isAnnouncementActive) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenUpdateDialog() }
                            .testTag("home_live_update_banner")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFF9900)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Campaign,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = appConfig.promoBadge,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        if (appConfig.globalDiscountPercent > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981)
                                            ) {
                                                Text(
                                                    text = "+${appConfig.globalDiscountPercent}% OFF",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.clickable { onOpenUpdateDialog() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "What's New",
                                                color = Color(0xFFFF9900),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = appConfig.storeAnnouncement,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Secret Admin Active Indicator Banner
            if (isAdminUnlocked) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAdmin() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF9900),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Admin Mode Active (Adii)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Tap to open Command Center & Media Uploader",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF9900)
                            ) {
                                Text(
                                    text = "OPEN",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Categories Row (Men, Women, Boots, Watches, Goggles, Earpods, Accessories)
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        text = "Shop by Category",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            val isSelected = selectedCategory == cat.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                                shadowElevation = if (isSelected) 3.dp else 1.dp,
                                modifier = Modifier
                                    .bounceClick {
                                        selectedCategory = cat.id
                                        onLogEvent("CATEGORY_BROWSE", "Browsing category: ${cat.name}")
                                    }
                                    .testTag("category_pill_${cat.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = cat.subtitle,
                                        fontSize = 9.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mega E-commerce Hero Promotional Banner
            if (searchQuery.isEmpty() && selectedCategory == "all") {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF9900),
                                            Color(0xFFE11D48),
                                            Color(0xFF6366F1)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFFFF9900), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = activeFestivalDeal.first,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = activeFestivalDeal.second,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )

                                Text(
                                    text = "Instant 10% Discount with Google Pay UPI | Code: GULLY_SPECIAL",
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Flash Deals / Deals of the Day (Horizontal Strip)
                item(span = { GridItemSpan(2) }) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFF9900), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Deals of the Day (50%+ OFF)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF9900).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "FEATURED DEALS",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFF9900),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(flashDeals, key = { "flash_${it.id}" }) { item ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .width(140.dp)
                                        .bounceClick { onProductClick(item) }
                                ) {
                                    Column {
                                        Box(modifier = Modifier.height(110.dp)) {
                                            AsyncImage(
                                                model = item.imageUrl,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "${item.discountPercent}% OFF",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = item.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "₹${item.price.toInt()}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Products Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "all") "All Featured Products (${displayedProducts.size})"
                        else "${selectedCategory.uppercase()} Collection (${displayedProducts.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "GC Collection",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9900),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Products 2-Column Grid
            items(displayedProducts, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onProductClick = onProductClick,
                    onAddToCart = onAddToCart
                )
            }
        }
    }

    // Voice recognition simulator modal for "adii" secret word!
    if (showSecretVoiceModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSecretVoiceModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFFF9900))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voice Assistant & Admin Access")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Say or tap the secret passkey 'adii' to instantly unlock the Master Admin Dashboard with high-res asset uploader & live telemetry.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSecretAdiiTriggered()
                                showSecretVoiceModal = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Say / Authenticate: \"adii\"",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSecretVoiceModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}
