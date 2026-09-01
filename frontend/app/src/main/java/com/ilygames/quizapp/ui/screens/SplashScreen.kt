package com.ilygames.quizapp.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel? = null,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var progressTarget by remember { mutableStateOf(0.1f) }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    // Infinite shimmer shine animation for progress bar
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    LaunchedEffect(Unit) {
        SoundManager.playIntroSound()
        progressTarget = 0.85f
        authViewModel?.tryAutoLogin(context)
        delay(400)
        progressTarget = 1.0f

        onNavigateToHome()
    }

    val isDark = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF5F5F0)
    val cardBg = if (isDark) Color(0xFF1C273A) else Color.White
    val trackBg = if (isDark) Color(0xFF121B2B) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D SOFT-CLAY SQUARE CARD WITH 3D ROYAL BLUE "Q"
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp))
                    .background(cardBg, RoundedCornerShape(28.dp))
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.45f else 0.95f),
                                Color.Black.copy(alpha = if (isDark) 0.6f else 0.1f)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Quizzy",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF255FF4),
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 3D THICK SHORT PROGRESS BAR WITH GLOSSY SHINE STREAK
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(16.dp)
                    .shadow(6.dp, CircleShape)
                    .background(trackBg, CircleShape)
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = if (isDark) 0.5f else 0.1f),
                                Color.White.copy(alpha = if (isDark) 0.2f else 0.8f)
                            )
                        ),
                        CircleShape
                    )
            ) {
                // Fill progress
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                ) {
                    // Shimmer Shine Highlight Streak
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(shimmerTranslateAnim - 300f, 0f),
                        end = androidx.compose.ui.geometry.Offset(shimmerTranslateAnim, 0f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}
