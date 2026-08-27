package com.ilygames.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ilygames.quizapp.ui.screens.*
import com.ilygames.quizapp.ui.theme.QuizAppTheme
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow smooth screen mirroring and screenshots on PC during development
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            QuizAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val quizViewModel: QuizViewModel = viewModel()
    val context = LocalContext.current

    val token by authViewModel.token.collectAsState()
    val activeToken = token ?: "bypass_auth_token_123"

    // Load admin preferences & initialize audio sound pool
    LaunchedEffect(Unit) {
        com.ilygames.quizapp.ui.screens.loadPersistedAdminData(context)
        SoundManager.init(context)
        authViewModel.tryAutoLogin(context)
    }

    // Connect realtime socket
    LaunchedEffect(activeToken) {
        quizViewModel.onScoreUpdated = { authViewModel.refreshProfile() }
        quizViewModel.connectRealtime(activeToken)
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("splash") {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                authViewModel = authViewModel,
                onStartQuiz = {
                    quizViewModel.startQuiz(activeToken)
                    navController.navigate("quiz")
                },
                onStartReadingQuiz = {
                    navController.navigate("reading_quiz")
                },
                onNavigateToLeaderboard = {
                    navController.navigate("leaderboard")
                },
                onNavigateToAdmin = {
                    navController.navigate("admin_panel")
                },
                onLogout = {
                    authViewModel.logout(context)
                }
            )
        }

        composable("quiz") {
            QuizScreen(
                token = activeToken,
                quizViewModel = quizViewModel,
                onQuizFinished = {
                    navController.navigate("result") {
                        popUpTo("quiz") { inclusive = true }
                    }
                }
            )
        }

        composable("reading_quiz") {
            ReadingQuizScreen(
                authViewModel = authViewModel,
                onQuizFinished = { score, coins ->
                    navController.popBackStack()
                }
            )
        }

        composable("result") {
            ResultScreen(
                quizViewModel = quizViewModel,
                onPlayAgain = {
                    quizViewModel.startQuiz(activeToken)
                    navController.navigate("quiz") {
                        popUpTo("result") { inclusive = true }
                    }
                },
                onGoHome = {
                    navController.navigate("home") {
                        popUpTo("result") { inclusive = true }
                    }
                }
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("admin_panel") {
            NativeAdminScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
