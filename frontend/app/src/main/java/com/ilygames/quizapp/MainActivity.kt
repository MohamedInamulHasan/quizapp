package com.ilygames.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
        com.ilygames.quizapp.ui.theme.ThemeState.init(this)
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
    val userState by authViewModel.user.collectAsState()

    // Check for auto-login on startup
    LaunchedEffect(Unit) {
        com.ilygames.quizapp.ui.theme.ThemeState.init(context)
        com.ilygames.quizapp.ui.screens.loadPersistedAdminData(context)
        SoundManager.init(context)
        com.ilygames.quizapp.utils.AdMobManager.init(context)
        authViewModel.tryAutoLogin(context)
    }

    // Connect realtime socket whenever we have a token
    LaunchedEffect(token) {
        val t = token ?: return@LaunchedEffect
        quizViewModel.onScoreUpdated = { authViewModel.refreshProfile(context) }
        quizViewModel.connectRealtime(t)
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { fadeIn(animationSpec = tween(80)) },
        exitTransition = { fadeOut(animationSpec = tween(80)) },
        popEnterTransition = { fadeIn(animationSpec = tween(80)) },
        popExitTransition = { fadeOut(animationSpec = tween(80)) }
    ) {
        composable("splash") {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LaunchedEffect(Unit) {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }

        composable("register") {
            LaunchedEffect(Unit) {
                navController.navigate("home") {
                    popUpTo("register") { inclusive = true }
                }
            }
        }

        composable("home") {
            HomeScreen(
                authViewModel = authViewModel,
                quizViewModel = quizViewModel,
                onStartQuiz = {
                    val currentToken = token ?: authViewModel.getToken(context) ?: "default_admin_token"
                    quizViewModel.startQuiz(currentToken)
                    navController.navigate("quiz")
                },
                onStartReadingQuiz = {
                    navController.navigate("reading_quiz")
                },
                onNavigateToLeaderboard = {
                    navController.navigate("leaderboard")
                },
                onNavigateToAiStudio = {
                    navController.navigate("ai_studio")
                },
                onNavigateToMemoryGame = {
                    navController.navigate("memory_game")
                },
                onNavigateToAdmin = {
                    navController.navigate("admin_panel")
                },
                onLogout = {
                    authViewModel.guestLogin(context)
                }
            )
        }

        composable("memory_game") {
            com.ilygames.quizapp.ui.screens.MemoryGameScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("ai_studio") {
            com.ilygames.quizapp.ui.screens.AiQuizStudioScreen(
                token = token ?: "guest_token",
                quizViewModel = quizViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onStartQuiz = {
                    navController.navigate("quiz")
                }
            )
        }

        composable("quiz") {
            QuizScreen(
                token = token ?: "",
                quizViewModel = quizViewModel,
                onQuizFinished = {
                    if (quizViewModel.isPracticeMode) {
                        navController.navigate("ai_studio") {
                            popUpTo("quiz") { inclusive = true }
                        }
                    } else {
                        navController.navigate("result") {
                            popUpTo("quiz") { inclusive = true }
                        }
                    }
                },
                onExitQuiz = {
                    val wasPractice = quizViewModel.isPracticeMode
                    quizViewModel.resetQuiz()
                    if (wasPractice) {
                        navController.navigate("ai_studio") {
                            popUpTo("quiz") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("reading_quiz") {
            ReadingQuizScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("result") {
            ResultsScreen(
                authViewModel = authViewModel,
                quizViewModel = quizViewModel,
                onPlayAgain = {
                    token?.let { currentToken ->
                        quizViewModel.startQuiz(currentToken)
                        navController.navigate("quiz") {
                            popUpTo("result") { inclusive = true }
                        }
                    }
                },
                onBackToHome = {
                    val wasPractice = quizViewModel.isPracticeMode
                    quizViewModel.resetQuiz()
                    if (wasPractice) {
                        navController.navigate("ai_studio") {
                            popUpTo("result") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("result") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                token = token ?: "guest_token",
                quizViewModel = quizViewModel,
                currentUserId = userState?.id ?: "",
                currentUserName = userState?.name ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable("admin_panel") {
            NativeAdminScreen(
                token = if (!token.isNullOrBlank()) token else authViewModel.getToken(context),
                quizViewModel = quizViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
