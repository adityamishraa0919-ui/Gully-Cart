package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fade loading states across 4 seconds total
    var showLogo by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Step 1: Immediate fade-in for GC logo
        delay(150L)
        showLogo = true

        // Step 2: Fade-in for "Gully Cart" text after logo appears
        delay(1350L)
        showTitle = true

        // Step 3: Subtle tagline fade-in
        delay(800L)
        showTagline = true

        // Total 4 seconds loading animation, then proceed into the app
        delay(1700L)
        onSplashComplete()
    }

    // Logo fade animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (showLogo) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "logo_fade"
    )

    // Title fade animation
    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "title_fade"
    )

    // Gentle pulse animation for GC logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground) // AMOLED pitch black background
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient radial glow behind logo
        if (showLogo) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryAmber.copy(alpha = glowAlpha * 0.35f),
                                Color(0xFFFFD700).copy(alpha = glowAlpha * 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // 1. First GC Logo Appears with smooth fade animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .scale(if (showLogo) logoScale else 0.85f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = PrimaryAmber,
                        spotColor = PrimaryAmber
                    )
            ) {
                // Gold gradient border
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryAmber, Color(0xFFFFD700), Color(0xFFFF8800))
                            )
                        )
                        .padding(3.dp)
                ) {
                    // Actual App Logo from drawable
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo_1790257228605),
                        contentDescription = "GC Gully Cart Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(30.dp))
                            .testTag("splash_app_logo")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Then "Gully Cart" Text Appears with smooth fade animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(titleAlpha)
            ) {
                Text(
                    text = "Gully Cart",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_app_title")
                )

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedVisibility(
                    visible = showTagline,
                    enter = fadeIn(animationSpec = tween(600))
                ) {
                    Text(
                        text = "Fashion • Footwear • Lifestyle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
