package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLiveUpdateConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminLiveUpdatesTab(
    currentConfig: AppLiveUpdateConfig,
    onPublishUpdate: (AppLiveUpdateConfig) -> Unit,
    onPreviewDialog: () -> Unit,
    onDeployUpdateToUsers: (flashMessage: String, onProgress: (String) -> Unit, onComplete: (Int) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var version by remember(currentConfig) { mutableStateOf(currentConfig.updateVersion) }
    var title by remember(currentConfig) { mutableStateOf(currentConfig.updateTitle) }
    var message by remember(currentConfig) { mutableStateOf(currentConfig.updateMessage) }
    var announcement by remember(currentConfig) { mutableStateOf(currentConfig.storeAnnouncement) }
    var isAnnouncementActive by remember(currentConfig) { mutableStateOf(currentConfig.isAnnouncementActive) }
    var discountPercent by remember(currentConfig) { mutableIntStateOf(currentConfig.globalDiscountPercent) }
    var promoBadge by remember(currentConfig) { mutableStateOf(currentConfig.promoBadge) }
    var storeStatus by remember(currentConfig) { mutableStateOf(currentConfig.storeStatus) }
    var updateType by remember(currentConfig) { mutableStateOf(currentConfig.updateType) }
    var forceDialog by remember(currentConfig) { mutableStateOf(currentConfig.forceUpdateDialog) }

    var flashUpdateMessage by remember {
        mutableStateOf("⚡ FLASH UPDATE: New clothes, fresh catalog items and live sale discounts are now active across all users!")
    }
    var isDeployingUpdate by remember { mutableStateOf(false) }
    var deployProgressStep by remember { mutableStateOf("") }
    var deployCompletedCount by remember { mutableStateOf<Int?>(null) }

    val formattedLastUpdated = try {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(currentConfig.updatedAt))
    } catch (_: Exception) {
        "Unknown"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------------------------------------------------------------
        // DEPLOY UPDATE TO USERS (Requested: "deploy update to the user in that option when i click on the option whtever i have made changes in the app the changes is shows the user app also i added new clothes")
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(Color(0xFFFF9900), Color(0xFFFF5722)))
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().testTag("deploy_update_to_users_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF9900),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Deploy Update to Users",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Pushes all clothes, catalog & sale changes live",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "REAL-TIME SYNC",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Click 'Deploy Update to Users' to instantly sync all added clothes, price updates, stock changes, and sale banners to Cloud Firestore and flash an immediate live update to all active users.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = flashUpdateMessage,
                    onValueChange = { flashUpdateMessage = it },
                    label = { Text("Flash Update Message to Broadcast") },
                    placeholder = { Text("e.g. ⚡ FLASH UPDATE: New clothes and fresh catalog items are now live!") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFFF9900)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deploy_flash_message_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9900)
                    )
                )

                if (isDeployingUpdate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF9900).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = Color(0xFFFF9900),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = deployProgressStep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                deployCompletedCount?.let { count ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Flash Update successfully deployed to all users! Changes are live.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isDeployingUpdate = true
                        deployCompletedCount = null
                        deployProgressStep = "Starting live catalog & clothes deployment..."
                        onDeployUpdateToUsers(
                            flashUpdateMessage,
                            { step -> deployProgressStep = step },
                            { count ->
                                isDeployingUpdate = false
                                deployCompletedCount = count
                                Toast.makeText(context, "⚡ Update Deployed! All changes live for users.", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isDeployingUpdate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9900),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_deploy_update_to_users")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDeployingUpdate) "Deploying Updates Live..." else "Deploy Update to Users",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
        // Active Status Overview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF9900),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RocketLaunch,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Live App Engine & Broadcast",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Last Pushed: $formattedLastUpdated",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                text = "LIVE: ${currentConfig.updateVersion}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chips summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Store Discount", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text("${currentConfig.globalDiscountPercent}% OFF", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Status", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text(currentConfig.storeStatus.replace("_", " "), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Banner", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text(if (currentConfig.isAnnouncementActive) "Active" else "Hidden", color = if (currentConfig.isAnnouncementActive) Color(0xFFFBBF24) else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Action Push Banner Button (Top Placement for Instant Access)
        Button(
            onClick = {
                val updated = AppLiveUpdateConfig(
                    id = "live_config",
                    updateVersion = version.ifBlank { "v2.5.0" },
                    updateTitle = title.ifBlank { "Gully Cart Live Store Update" },
                    updateMessage = message.ifBlank { "Exciting new store features, price drops, and faster delivery!" },
                    updateType = updateType,
                    storeAnnouncement = announcement.ifBlank { "🔥 Festival Sale: Bonus discount applied across all items!" },
                    isAnnouncementActive = isAnnouncementActive,
                    globalDiscountPercent = discountPercent.coerceIn(0, 90),
                    promoBadge = promoBadge.ifBlank { "MEGA SALE" },
                    storeStatus = storeStatus,
                    forceUpdateDialog = forceDialog,
                    updatedAt = System.currentTimeMillis()
                )
                onPublishUpdate(updated)
                Toast.makeText(context, "🚀 App Update Published! Real-time changes broadcasted to all users.", Toast.LENGTH_LONG).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_publish_update_top_btn")
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Publish Changes & Broadcast Update to App",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        // EDITABLE FIELDS CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure App Update & Live Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Any changes you save here instantly propagate in real-time to all shoppers and trigger the App Update alert.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Version & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Version Tag") },
                        placeholder = { Text("v2.5.0") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.4f)
                            .testTag("update_version_input")
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Update Title / Header") },
                        placeholder = { Text("Festival Mega Sale Update") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("update_title_input")
                    )
                }

                // Update Message / Changelog (Multi-line)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Update Details & Changelog (Shown in Alert)") },
                    placeholder = { Text("Describe the changes, new features, discount codes, or catalog updates...") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_message_input")
                )

                // Store Announcement Banner Text
                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it },
                    label = { Text("Live Home Screen Banner Announcement") },
                    placeholder = { Text("🔥 MEGA FESTIVAL SALE: Extra 20% OFF automatically applied!") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_announcement_input")
                )

                // Promo Badge & Store Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = promoBadge,
                        onValueChange = { promoBadge = it },
                        label = { Text("Promo Badge Label") },
                        placeholder = { Text("MEGA SALE") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("update_badge_input")
                    )

                    OutlinedTextField(
                        value = storeStatus,
                        onValueChange = { storeStatus = it },
                        label = { Text("Store Status") },
                        placeholder = { Text("OPEN / EXPRESS") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("update_status_input")
                    )
                }

                // Global Extra Store Discount % (0% to 50%)
                Column {
                    Text(
                        text = "Storewide Live Extra Discount: $discountPercent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 10, 15, 20, 25, 30, 50).forEach { pct ->
                            val isSelected = discountPercent == pct
                            FilterChip(
                                selected = isSelected,
                                onClick = { discountPercent = pct },
                                label = { Text("$pct%") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display Top Announcement Banner",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Shows the announcement marquee at the top of the store home page",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAnnouncementActive,
                        onCheckedChange = { isAnnouncementActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9900)),
                        modifier = Modifier.testTag("announcement_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Popup Update Dialog for Users",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Displays interactive Update Alert modal immediately when users open the app",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = forceDialog,
                        onCheckedChange = { forceDialog = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9900)),
                        modifier = Modifier.testTag("force_dialog_switch")
                    )
                }
            }
        }

        // PREVIEW SECTION
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live In-App Announcement Preview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = onPreviewDialog,
                        modifier = Modifier.testTag("preview_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview Modal", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1B4B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF9900)
                        ) {
                            Text(
                                text = promoBadge.ifBlank { "UPDATE" },
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = announcement.ifBlank { "No announcement active." },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom Action Button
        Button(
            onClick = {
                val updated = AppLiveUpdateConfig(
                    id = "live_config",
                    updateVersion = version.ifBlank { "v2.5.0" },
                    updateTitle = title.ifBlank { "Gully Cart Live Store Update" },
                    updateMessage = message.ifBlank { "Exciting new store features, price drops, and faster delivery!" },
                    updateType = updateType,
                    storeAnnouncement = announcement.ifBlank { "🔥 Festival Sale: Bonus discount applied across all items!" },
                    isAnnouncementActive = isAnnouncementActive,
                    globalDiscountPercent = discountPercent.coerceIn(0, 90),
                    promoBadge = promoBadge.ifBlank { "MEGA SALE" },
                    storeStatus = storeStatus,
                    forceUpdateDialog = forceDialog,
                    updatedAt = System.currentTimeMillis()
                )
                onPublishUpdate(updated)
                Toast.makeText(context, "🚀 App Update Published! Changes propagated in app.", Toast.LENGTH_LONG).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_publish_update_bottom_btn")
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Publish Changes & Broadcast Update to App",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}
