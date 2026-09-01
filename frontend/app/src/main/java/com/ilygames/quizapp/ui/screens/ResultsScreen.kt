package com.ilygames.quizapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.ui.viewmodel.QuizState
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun ResultsScreen(
    authViewModel: AuthViewModel,
    quizViewModel: QuizViewModel,
    onPlayAgain: () -> Unit = {},
    onBackToHome: () -> Unit
) {
    val quizState by quizViewModel.quizState.collectAsState()
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    // Read and track persisted hearts per logged in user
    val user by authViewModel.user.collectAsState()
    val userKey = user?.id ?: user?.name ?: "default"
    val heartsPrefs = remember { context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE) }
    var currentHearts by remember(userKey) { mutableStateOf(heartsPrefs.getInt("saved_hearts_count_$userKey", 3)) }

    LaunchedEffect(Unit) {
        isVisible = true
        SoundManager.playSuccessChime()
    }

    // 3D Bubble Pop-Up Scale Bouncy Animation
    val popScale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BubblePopScale"
    )

    // Trophy Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "trophyPulse")
    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (quizState is QuizState.Complete) {
            val completeState = quizState as QuizState.Complete

            val performanceText = when {
                completeState.score >= 80 -> "Outstanding Performance! You mastered this quiz!"
                completeState.score >= 40 -> "Great job! Solid effort on this quiz!"
                else -> "Good try! Practice more to boost your score!"
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(100)) + slideInVertically(tween(100, easing = FastOutSlowInEasing)) { 30 }
            ) {
                // Single Unified Outline Quiz Summary Card (0.dp elevation shadow, clean border)
                val isDarkRes = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                val resCardBg = if (isDarkRes) Color(0xFF1C273A) else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = popScale, scaleY = popScale)
                        .shadow(12.dp, RoundedCornerShape(26.dp))
                        .background(resCardBg, RoundedCornerShape(26.dp))
                        .border(
                            1.5.dp,
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkRes) 0.35f else 0.9f),
                                    Color.Black.copy(alpha = if (isDarkRes) 0.5f else 0.08f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Animated Header Trophy Icon
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .graphicsLayer(scaleX = trophyScale, scaleY = trophyScale)
                                .shadow(6.dp, CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF386DF5),
                                            Color(0xFF255FF4),
                                            Color(0xFF0B46DA)
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Quiz Complete",
                                tint = TextGold,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎉 Quiz Completed!",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.Black,
                                color = if (isDarkRes) Color.White else Color(0xFF17181C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = performanceText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                        // Clear Performance Details Table
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Score Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDarkRes) Color(0xFF131B2A) else Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = TextGold, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "Total Score",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDarkRes) Color.White else Color(0xFF17181C)
                                    )
                                }
                                Text(
                                    text = "${completeState.score} pts",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFF255FF4)
                                )
                            }

                            // Time Spent Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDarkRes) Color(0xFF131B2A) else Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF255FF4), modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "Time Spent",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDarkRes) Color.White else Color(0xFF17181C)
                                    )
                                }
                                Text(
                                    text = "${completeState.timeTaken} seconds",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (isDarkRes) Color.White else Color(0xFF17181C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        // 3D Soft-Clay Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Retry / Play Again / Watch Ad Button (3D Glossy Blue Button)
                            Button(
                                onClick = {
                                    SoundManager.playClickSound()
                                    if (currentHearts > 0) {
                                        onPlayAgain()
                                    } else {
                                        // Watch Video Ad to gain 1 heart and play again
                                        com.ilygames.quizapp.utils.AdMobManager.showRewardedAd(
                                            context = context,
                                            onRewardEarned = {
                                                currentHearts++
                                                heartsPrefs.edit().putInt("saved_hearts_count_$userKey", currentHearts).apply()
                                                authViewModel.addAdReward(context)
                                                Toast.makeText(context, "🎉 Ad Watched! +1 Heart Regained ❤️", Toast.LENGTH_SHORT).show()
                                                onPlayAgain()
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (currentHearts > 0) Icons.Default.Refresh else Icons.Default.OndemandVideo,
                                        contentDescription = "Action",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentHearts > 0) "Play Again" else "Watch Ad",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Home Button (3D Glossy Blue Button)
                            Button(
                                onClick = {
                                    SoundManager.playClickSound()
                                    quizViewModel.markQuizJustCompleted()
                                    quizViewModel.resetQuiz()
                                    onBackToHome()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Home",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
