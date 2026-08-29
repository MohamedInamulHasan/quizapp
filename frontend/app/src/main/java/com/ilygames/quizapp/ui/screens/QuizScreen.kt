package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.QuizState
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(
    token: String,
    quizViewModel: QuizViewModel,
    onQuizFinished: () -> Unit,
    onExitQuiz: () -> Unit
) {
    val quizState by quizViewModel.quizState.collectAsState()
    val timerSeconds by quizViewModel.timerState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val currentQuestionIndex = (quizState as? QuizState.Active)?.currentQuestionIndex ?: 0
    var selectedOption by remember(currentQuestionIndex) { mutableStateOf<String?>(null) }
    var wrongCount by remember { mutableStateOf(0) }
    
    // Keyed Waterfall Reveal Trigger
    var isContentVisible by remember(currentQuestionIndex) { mutableStateOf(false) }

    LaunchedEffect(currentQuestionIndex) {
        isContentVisible = false
        delay(50)
        isContentVisible = true
    }

    // Fast urgent clock ticking sound when timer <= 5s
    LaunchedEffect(timerSeconds) {
        if (timerSeconds in 1..5 && selectedOption == null && quizState is QuizState.Active) {
            SoundManager.playFastUrgentTick()
        }
    }

    LaunchedEffect(quizState) {
        if (quizState is QuizState.Complete) {
            onQuizFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = quizState) {
            is QuizState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 3.dp)
                        Text(
                            text = "Preparing Questions...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }
            is QuizState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Questions Available Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The quiz database is currently empty. Tap Admin Studio on the home screen to add or Bulk Upload questions!",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onExitQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GO BACK TO HOME", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
            is QuizState.Active -> {
                val question = state.questions[state.currentQuestionIndex]
                val progress = (state.currentQuestionIndex + 1).toFloat() / state.questions.size
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "Progress")

                // Single Smooth Vertical Scrollable Container (No cut-off options, no double scroll)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Top Navigation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onExitQuiz,
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Quiz", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        LinearProgressIndicator(
                            progress = animatedProgress,
                            color = PrimaryGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(listOf(PrimaryGreen.copy(alpha = 0.2f), EmeraldGlow.copy(alpha = 0.1f))),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, PrimaryGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${state.currentQuestionIndex + 1}/${state.questions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timer & Score Deck
                    Card(
                        shape = RoundedCornerShape(50.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (timerSeconds <= 5) IncorrectRed else PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${timerSeconds}s remaining",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerSeconds <= 5) IncorrectRed else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${state.score} pts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // KEYED CONTENT BLOCK
                    key(currentQuestionIndex) {
                        Column {
                            // 1. Question Card
                            AnimatedVisibility(
                                visible = isContentVisible,
                                enter = fadeIn(tween(400, delayMillis = 0, easing = FastOutSlowInEasing)) + 
                                        slideInVertically(tween(400, delayMillis = 0, easing = FastOutSlowInEasing)) { 40 }
                            ) {
                                Card(
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (!question.imageUrl.isNullOrBlank()) {
                                            // Small, compact question image container
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                                    .clip(RoundedCornerShape(16.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                coil.compose.AsyncImage(
                                                    model = question.imageUrl,
                                                    contentDescription = "Question Image",
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = question.question,
                                                style = MaterialTheme.typography.titleMedium.copy(lineHeight = 28.sp, fontSize = 18.sp),
                                                fontWeight = FontWeight.Black,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(22.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = question.question,
                                                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 30.sp, fontSize = 19.sp),
                                                    fontWeight = FontWeight.Black,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Smooth Options Stack (Column instead of LazyColumn to allow full page scrolling)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Option A
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = fadeIn(tween(400, delayMillis = 100, easing = FastOutSlowInEasing)) + 
                                            slideInVertically(tween(400, delayMillis = 100, easing = FastOutSlowInEasing)) { 45 }
                                ) {
                                    CleanHighlightOptionRow(
                                        text = question.optionA,
                                        label = "A",
                                        optionKey = "A",
                                        selectedOption = selectedOption,
                                        correctAnswer = question.correctAnswer,
                                        onClick = {
                                            if (selectedOption == null) {
                                                selectedOption = "A"
                                                if ("A" == question.correctAnswer) {
                                                    SoundManager.playCorrectSound()
                                                } else {
                                                    SoundManager.playWrongSound()
                                                    wrongCount++
                                                }
                                                coroutineScope.launch {
                                                    delay(550)
                                                    quizViewModel.submitAnswer(token, "A")
                                                }
                                            }
                                        }
                                    )
                                }

                                // Option B
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = fadeIn(tween(400, delayMillis = 200, easing = FastOutSlowInEasing)) + 
                                            slideInVertically(tween(400, delayMillis = 200, easing = FastOutSlowInEasing)) { 45 }
                                ) {
                                    CleanHighlightOptionRow(
                                        text = question.optionB,
                                        label = "B",
                                        optionKey = "B",
                                        selectedOption = selectedOption,
                                        correctAnswer = question.correctAnswer,
                                        onClick = {
                                            if (selectedOption == null) {
                                                selectedOption = "B"
                                                if ("B" == question.correctAnswer) {
                                                    SoundManager.playCorrectSound()
                                                } else {
                                                    SoundManager.playWrongSound()
                                                    wrongCount++
                                                }
                                                coroutineScope.launch {
                                                    delay(550)
                                                    quizViewModel.submitAnswer(token, "B")
                                                }
                                            }
                                        }
                                    )
                                }

                                // Option C
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = fadeIn(tween(400, delayMillis = 300, easing = FastOutSlowInEasing)) + 
                                            slideInVertically(tween(400, delayMillis = 300, easing = FastOutSlowInEasing)) { 45 }
                                ) {
                                    CleanHighlightOptionRow(
                                        text = question.optionC,
                                        label = "C",
                                        optionKey = "C",
                                        selectedOption = selectedOption,
                                        correctAnswer = question.correctAnswer,
                                        onClick = {
                                            if (selectedOption == null) {
                                                selectedOption = "C"
                                                if ("C" == question.correctAnswer) {
                                                    SoundManager.playCorrectSound()
                                                } else {
                                                    SoundManager.playWrongSound()
                                                    wrongCount++
                                                }
                                                coroutineScope.launch {
                                                    delay(550)
                                                    quizViewModel.submitAnswer(token, "C")
                                                }
                                            }
                                        }
                                    )
                                }

                                // Option D
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = fadeIn(tween(400, delayMillis = 400, easing = FastOutSlowInEasing)) + 
                                            slideInVertically(tween(400, delayMillis = 400, easing = FastOutSlowInEasing)) { 45 }
                                ) {
                                    CleanHighlightOptionRow(
                                        text = question.optionD,
                                        label = "D",
                                        optionKey = "D",
                                        selectedOption = selectedOption,
                                        correctAnswer = question.correctAnswer,
                                        onClick = {
                                            if (selectedOption == null) {
                                                selectedOption = "D"
                                                if ("D" == question.correctAnswer) {
                                                    SoundManager.playCorrectSound()
                                                } else {
                                                    SoundManager.playWrongSound()
                                                    wrongCount++
                                                }
                                                coroutineScope.launch {
                                                    delay(550)
                                                    quizViewModel.submitAnswer(token, "D")
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Extra bottom padding to ensure all options scroll completely into view
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun CleanHighlightOptionRow(
    text: String,
    label: String,
    optionKey: String,
    selectedOption: String?,
    correctAnswer: String,
    onClick: () -> Unit
) {
    val isSelected = selectedOption == optionKey
    val isAnswered = selectedOption != null
    val isCorrectOption = optionKey == correctAnswer

    val backgroundColor = when {
        isSelected && isCorrectOption -> CorrectGreen.copy(alpha = 0.15f)
        isSelected && !isCorrectOption -> IncorrectRed.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSelected && isCorrectOption -> CorrectGreen
        isSelected && !isCorrectOption -> IncorrectRed
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val labelBgColor = when {
        isSelected && isCorrectOption -> CorrectGreen
        isSelected && !isCorrectOption -> IncorrectRed
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val labelTextColor = when {
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(enabled = !isAnswered, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(labelBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = labelTextColor
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                fontWeight = if (isSelected || (isAnswered && isCorrectOption)) FontWeight.Black else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
