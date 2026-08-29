package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onBackToHome: () -> Unit
) {
    val quizState by quizViewModel.quizState.collectAsState()
    val token = authViewModel.token.value ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (quizState is QuizState.Complete) {
            val completeState = quizState as QuizState.Complete

            val performanceText = when {
                completeState.score >= 80 -> "🌟 Outstanding Masterclass!"
                completeState.score >= 40 -> "👏 Great Effort & Solid Score!"
                else -> "💪 Good Attempt! Keep Practice Going!"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // ── 1. Hero Animated Trophy & Victory Badge Container ───
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.5.dp, Brush.linearGradient(listOf(PrimaryGreen, EmeraldGlow))),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Glowing Trophy Circle
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape)
                                .border(2.dp, PrimaryGreen.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = TextGold,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        // Badge Pill
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "🎉 QUIZ COMPLETED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "Great Job!",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = performanceText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── 2. Hero Performance Stats Grid Cards ─────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Final Score Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = TextGold, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "TOTAL SCORE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted
                                )
                            }

                            Text(
                                text = "${completeState.score}",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen
                            )

                            Text(
                                text = "pts earned",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }

                    // Time Taken Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "TIME SPENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted
                                )
                            }

                            Text(
                                text = "${completeState.timeTaken}s",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "total duration",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 3. Dual Action Navigation Buttons ────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play Again / Retry Quiz Button
                    OutlinedButton(
                        onClick = {
                            SoundManager.playClickSound()
                            quizViewModel.startQuiz(token)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Play Again", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PLAY ANOTHER QUIZ",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = PrimaryGreen
                        )
                    }

                    // Return Home Button
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            quizViewModel.resetQuiz()
                            onBackToHome()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RETURN TO HOME",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
