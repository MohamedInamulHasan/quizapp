@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.BorderStroke
import com.ilygames.quizapp.data.model.TriviaQuestion
import com.ilygames.quizapp.ui.viewmodel.InfiniteQuizViewModel
import kotlinx.coroutines.delay

@Composable
fun InfiniteQuizScreen(
    token: String,
    categoryId: Int,
    categoryName: String,
    aiTopic: String,
    difficulty: String,
    viewModel: InfiniteQuizViewModel,
    onFinished: (score: Int, total: Int) -> Unit,
    onBack: () -> Unit
) {
    val questions by viewModel.questions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(20) }
    var showResult by remember { mutableStateOf(false) }
    var answers by remember { mutableStateOf(listOf<Boolean>()) }

    val isAi = aiTopic.isNotBlank() && aiTopic != "none"

    LaunchedEffect(Unit) {
        if (isAi) {
            viewModel.generateAIQuestions(token, aiTopic, difficulty)
        } else {
            viewModel.loadTriviaQuestions(token, categoryId, difficulty)
        }
    }

    // Timer countdown
    LaunchedEffect(currentIndex, showAnswer) {
        if (!showAnswer && questions.isNotEmpty() && !showResult) {
            timeLeft = 20
            while (timeLeft > 0 && !showAnswer) {
                delay(1000)
                timeLeft--
            }
            if (!showAnswer) {
                // Time's up — mark wrong
                showAnswer = true
                answers = answers + false
                delay(1500)
                if (currentIndex < questions.size - 1) {
                    currentIndex++
                    selectedOption = null
                    showAnswer = false
                } else {
                    showResult = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        when {
            isLoading -> LoadingQuizState(categoryName = if (isAi) "🤖 $aiTopic" else categoryName)
            error != null -> ErrorQuizState(error = error!!, onRetry = {
                if (isAi) viewModel.generateAIQuestions(token, aiTopic, difficulty)
                else viewModel.loadTriviaQuestions(token, categoryId, difficulty)
            }, onBack = onBack)
            showResult -> InfiniteQuizResultScreen(
                score = score,
                total = questions.size,
                answers = answers,
                categoryName = if (isAi) aiTopic else categoryName,
                onPlayAgain = {
                    currentIndex = 0; score = 0; selectedOption = null
                    showAnswer = false; showResult = false; answers = listOf()
                    if (isAi) viewModel.generateAIQuestions(token, aiTopic, difficulty)
                    else viewModel.loadTriviaQuestions(token, categoryId, difficulty)
                },
                onBack = onBack
            )
            questions.isNotEmpty() -> {
                val q = questions[currentIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(52.dp))
                    // Top bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isAi) "🤖 $aiTopic" else categoryName,
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${currentIndex + 1} / ${questions.size}",
                                color = Color.White.copy(0.6f), fontSize = 11.sp
                            )
                        }
                        // Timer circle
                        TimerCircle(timeLeft = timeLeft, totalTime = 20)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = (currentIndex + 1).toFloat() / questions.size,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF6C63FF),
                        trackColor = Color.White.copy(0.1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Score chip
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF6C63FF).copy(0.2f)
                        ) {
                            Text(
                                "⭐ Score: $score",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Question card
                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            (slideInHorizontally { it } + fadeIn()).with(slideOutHorizontally { -it } + fadeOut())
                        }, label = "question_anim"
                    ) { idx ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B3A)),
                            elevation = CardDefaults.cardElevation(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF2D2A4A), Color(0xFF1E1B3A))
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = questions.getOrNull(idx)?.question ?: "",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 26.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(20.dp))

                    // Options
                    q.options.forEach { option ->
                        val isSelected = selectedOption == option
                        val isCorrect = option == q.answer
                        val bgColor = when {
                            showAnswer && isCorrect -> Color(0xFF00C853)
                            showAnswer && isSelected && !isCorrect -> Color(0xFFD50000)
                            isSelected -> Color(0xFF6C63FF)
                            else -> Color.White.copy(alpha = 0.07f)
                        }
                        val borderColor = when {
                            showAnswer && isCorrect -> Color(0xFF00C853)
                            showAnswer && isSelected -> Color(0xFFD50000)
                            isSelected -> Color(0xFF6C63FF)
                            else -> Color.White.copy(0.15f)
                        }

                        Card(
                            onClick = {
                                if (!showAnswer) {
                                    selectedOption = option
                                    showAnswer = true
                                    val correct = option == q.answer
                                    if (correct) score++
                                    answers = answers + correct
                                    // Auto-advance after 1.5 seconds
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(1.5.dp, borderColor),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (showAnswer && isCorrect) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                } else if (showAnswer && isSelected && !isCorrect) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Auto-advance after showing answer
                    if (showAnswer) {
                        LaunchedEffect(currentIndex) {
                            delay(1800)
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                                selectedOption = null
                                showAnswer = false
                            } else {
                                showResult = true
                            }
                        }
                    }

                    // Explanation
                    if (showAnswer && q.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(0.05f)
                        ) {
                            Text(
                                "💡 ${q.explanation}",
                                color = Color.White.copy(0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerCircle(timeLeft: Int, totalTime: Int) {
    val progress = timeLeft.toFloat() / totalTime
    val color = when {
        progress > 0.5f -> Color(0xFF00C853)
        progress > 0.25f -> Color(0xFFFFD600)
        else -> Color(0xFFD50000)
    }
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress,
            color = color,
            strokeWidth = 4.dp,
            modifier = Modifier.size(44.dp)
        )
        Text(text = "$timeLeft", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LoadingQuizState(categoryName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val rotation by infiniteTransition.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(1500, easing = LinearEasing)),
            label = "spin"
        )
        Text("🤖", fontSize = 60.sp, modifier = Modifier.graphicsLayer { rotationZ = rotation })
        Spacer(modifier = Modifier.height(24.dp))
        Text("Generating questions...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(categoryName, color = Color(0xFF6C63FF), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            color = Color(0xFF6C63FF),
            trackColor = Color.White.copy(0.1f),
            modifier = Modifier.width(200.dp).clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun ErrorQuizState(error: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("😢", fontSize = 60.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Something went wrong", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(error, color = Color.White.copy(0.6f), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))) {
            Text("Try Again", color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Go Back", color = Color.White.copy(0.6f))
        }
    }
}

@Composable
fun InfiniteQuizResultScreen(
    score: Int,
    total: Int,
    answers: List<Boolean>,
    categoryName: String,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val percent = if (total > 0) (score * 100 / total) else 0
    val emoji = when {
        percent >= 80 -> "🏆"
        percent >= 60 -> "🌟"
        percent >= 40 -> "👍"
        else -> "💪"
    }
    val message = when {
        percent >= 80 -> "Excellent!"
        percent >= 60 -> "Great job!"
        percent >= 40 -> "Good effort!"
        else -> "Keep practicing!"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 80.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(categoryName, color = Color(0xFF6C63FF), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            // Score card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B3A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$score / $total", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Correct Answers", color = Color.White.copy(0.6f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = percent / 100f,
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if (percent >= 60) Color(0xFF00C853) else Color(0xFFFFD600),
                        trackColor = Color.White.copy(0.1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$percent%", color = Color(0xFF6C63FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Answer breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val correct = answers.count { it }
                val wrong = answers.count { !it }
                StatChip(label = "✅ Correct", value = "$correct", color = Color(0xFF00C853), modifier = Modifier.weight(1f))
                StatChip(label = "❌ Wrong", value = "$wrong", color = Color(0xFFD50000), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text("🔄 Play Again", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.3f))
            ) {
                Text("← Change Category", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.White.copy(0.7f), fontSize = 12.sp)
        }
    }
}
