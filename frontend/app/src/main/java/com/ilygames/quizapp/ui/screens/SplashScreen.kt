package com.ilygames.quizapp.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

private val QuizzyGreen = Color(0xFF128A58)
private val OffWhiteBg = Color(0xFFF5F5F0)

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    var progressTarget by remember { mutableStateOf(0.1f) }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        progressTarget = 1.0f
        delay(400)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhiteBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = QuizzyGreen.copy(alpha = 0.3f))
                    .background(QuizzyGreen, RoundedCornerShape(32.dp)),
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
                color = QuizzyGreen,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .width(140.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = QuizzyGreen,
                trackColor = Color(0xFFE2E2DC)
            )
        }
    }
}
