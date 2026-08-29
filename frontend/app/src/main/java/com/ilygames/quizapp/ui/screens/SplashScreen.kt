package com.ilygames.quizapp.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

private val QuizzyGreen = Color(0xFF128A58)
private val OffWhiteBg = Color(0xFFF5F5F0)

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

    LaunchedEffect(Unit) {
        progressTarget = 0.85f
        authViewModel?.tryAutoLogin(context)
        delay(400)
        progressTarget = 1.0f

        val sharedPrefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val hasToken = !sharedPrefs.getString("auth_token", null).isNullOrBlank() || authViewModel?.token?.value != null

        if (!hasToken && onNavigateToLogin != null) {
            onNavigateToLogin()
        } else {
            onNavigateToHome()
        }
    }

    val isDark = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val bgColor = if (isDark) Color(0xFF0F172A) else OffWhiteBg
    val titleColor = if (isDark) Color(0xFF10B981) else QuizzyGreen
    val trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E2DC)

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
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = titleColor.copy(alpha = 0.3f))
                    .background(titleColor, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Quizzy",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = titleColor,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .width(140.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = titleColor,
                trackColor = trackColor
            )
        }
    }
}
