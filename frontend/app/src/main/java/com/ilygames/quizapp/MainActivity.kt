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
import com.ilygames.quizapp.ui.viewmodel.InfiniteQuizViewModel
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val infiniteQuizViewModel: InfiniteQuizViewModel = viewModel()
    val context = LocalContext.current

    val token by authViewModel.token.collectAsState()

    // Check for auto-login on startup & init audio sound pool
    LaunchedEffect(Unit) {
        SoundManager.init(context)
        authViewModel.tryAutoLogin(context)
    }

    // Connect realtime socket whenever we have a token
    LaunchedEffect(token) {
        val t = token ?: return@LaunchedEffect
        quizViewModel.onScoreUpdated = { authViewModel.refreshProfile() }
        quizViewModel.connectRealtime(t)
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
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
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    authViewModel.refreshProfile()
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.navigate("login") },
                onRegisterSuccess = {
                    authViewModel.refreshProfile()
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                authViewModel = authViewModel,
                onStartQuiz = {
                    token?.let {
                        quizViewModel.startQuiz(it)
                        navController.navigate("quiz")
                    }
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
                onExploreQuiz = {
                    navController.navigate("explore_quiz")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("explore_quiz") {
            token?.let { currentToken ->
                CategorySelectionScreen(
                    token = currentToken,
                    onCategorySelected = { catId, catName, difficulty ->
                        navController.navigate("infinite_quiz/$catId/$catName/ /$difficulty")
                    },
                    onAiTopicSelected = { topic, difficulty ->
                        navController.navigate("infinite_quiz/0/AI Topic/$topic/$difficulty")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("infinite_quiz/{catId}/{catName}/{aiTopic}/{difficulty}") { backStack ->
            token?.let { currentToken ->
                val catId = backStack.arguments?.getString("catId")?.toIntOrNull() ?: 9
                val catName = backStack.arguments?.getString("catName") ?: "Quiz"
                val aiTopic = backStack.arguments?.getString("aiTopic")?.trim() ?: ""
                val difficulty = backStack.arguments?.getString("difficulty") ?: "medium"
                InfiniteQuizScreen(
                    token = currentToken,
                    categoryId = catId,
                    categoryName = catName,
                    aiTopic = aiTopic,
                    difficulty = difficulty,
                    viewModel = infiniteQuizViewModel,
                    onFinished = { _, _ -> navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("quiz") {
            token?.let { currentToken ->
                QuizScreen(
                    token = currentToken,
                    quizViewModel = quizViewModel,
                    onQuizFinished = {
                        authViewModel.refreshProfile()
                        navController.navigate("home") {
                            popUpTo("quiz") { inclusive = true }
                        }
                    },
                    onExitQuiz = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("reading_quiz") {
            StudyPassagesScreen(
                onBack = { navController.popBackStack() },
                onStartDaily20Quiz = {
                    token?.let { currentToken ->
                        quizViewModel.startQuiz(currentToken)
                        navController.navigate("quiz")
                    }
                }
            )
        }

        composable("leaderboard") {
            token?.let { currentToken ->
                val currentUserState by authViewModel.user.collectAsState()
                LeaderboardScreen(
                    token = currentToken,
                    quizViewModel = quizViewModel,
                    currentUserId = currentUserState?.id ?: "",
                    currentUserName = currentUserState?.name ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("admin_panel") {
            NativeAdminScreen(
                token = token,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
