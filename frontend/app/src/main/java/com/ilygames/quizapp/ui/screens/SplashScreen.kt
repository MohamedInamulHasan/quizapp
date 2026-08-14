package com.ilygames.quizapp.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ElectricMint
import com.ilygames.quizapp.ui.theme.PrimaryGreen
import com.ilygames.quizapp.ui.viewmodel.AuthState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        val sharedPrefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val hasToken = !sharedPrefs.getString("auth_token", null).isNullOrBlank()

        if (!hasToken) {
            onNavigateToLogin()
        } else {
            when (authState) {
                is AuthState.Success -> onNavigateToHome()
                is AuthState.Error -> onNavigateToLogin()
                else -> {} // Idle or Loading: keep waiting
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(PrimaryGreen.copy(alpha = 0.18f), ElectricMint.copy(alpha = 0.1f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.5.dp, PrimaryGreen.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(46.dp)
                )
            }

            Text(
                text = "QuizApp 🌅",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 36.sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFFFF512F),
                            androidx.compose.ui.graphics.Color(0xFFF09819),
                            androidx.compose.ui.graphics.Color(0xFFFFD700)
                        )
                    )
                ),
                fontWeight = FontWeight.Black
            )

        }
    }
}
