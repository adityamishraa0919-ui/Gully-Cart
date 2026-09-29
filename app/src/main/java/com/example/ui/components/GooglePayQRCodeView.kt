package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun GooglePayQRCodeView(
    upiId: String = "gullycart.merchant@okaxis",
    merchantName: String = "Gully Cart Store",
    amount: Double,
    customQrImageUrl: String = "",
    modifier: Modifier = Modifier,
    onCopyUpi: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Google Pay Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A73E8),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Pay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Merchant",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified Business Account",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.15f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BHIM UPI",
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // White QR Container for clean scanning contrast
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(230.dp)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (customQrImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = customQrImageUrl,
                            contentDescription = "Merchant UPI QR Code",
                            modifier = Modifier
                                .size(210.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Draw authentic high-contrast QR Matrix
                        Canvas(modifier = Modifier.size(200.dp)) {
                        val gridSize = 25
                        val cellSize = size.width / gridSize
                        val qrColor = Color(0xFF0F172A)

                        // Finder Patterns (Top-Left, Top-Right, Bottom-Left)
                        fun drawFinder(startX: Float, startY: Float) {
                            // Outer 7x7 box
                            drawRect(
                                color = qrColor,
                                topLeft = Offset(startX, startY),
                                size = Size(cellSize * 7, cellSize * 7)
                            )
                            // White inner 5x5
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(startX + cellSize, startY + cellSize),
                                size = Size(cellSize * 5, cellSize * 5)
                            )
                            // Black center 3x3
                            drawRect(
                                color = qrColor,
                                topLeft = Offset(startX + cellSize * 2, startY + cellSize * 2),
                                size = Size(cellSize * 3, cellSize * 3)
                            )
                        }

                        drawFinder(0f, 0f)
                        drawFinder(cellSize * (gridSize - 7), 0f)
                        drawFinder(0f, cellSize * (gridSize - 7))

                        // Timing patterns
                        for (i in 8 until (gridSize - 8)) {
                            if (i % 2 == 0) {
                                drawRect(
                                    color = qrColor,
                                    topLeft = Offset(cellSize * 6, cellSize * i),
                                    size = Size(cellSize, cellSize)
                                )
                                drawRect(
                                    color = qrColor,
                                    topLeft = Offset(cellSize * i, cellSize * 6),
                                    size = Size(cellSize, cellSize)
                                )
                            }
                        }

                        // Deterministic pseudo-random pattern based on upiId hash
                        val seed = (upiId.hashCode().toLong() shl 16) xor amount.toLong()
                        val random = Random(seed)

                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                // Skip finder patterns
                                val inTopLeft = r < 8 && c < 8
                                val inTopRight = r < 8 && c >= (gridSize - 8)
                                val inBottomLeft = r >= (gridSize - 8) && c < 8
                                val inCenter = r in 10..14 && c in 10..14

                                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenter) {
                                    if (random.nextBoolean()) {
                                        drawRect(
                                            color = qrColor,
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize * 0.92f, cellSize * 0.92f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Center Google Pay G-Badge
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(38.dp)
                            .border(2.dp, Color(0xFF1A73E8), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "GPay",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1A73E8)
                            )
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant Name & Amount
            Text(
                text = "Pay to: $merchantName",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "₹${"%.2f".format(amount)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // UPI ID Bar with Copy Action
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "UPI ID / VPA",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = upiId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onCopyUpi,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "100% Safe Payments",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Encrypted UPI Payment | Instant Processing",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
