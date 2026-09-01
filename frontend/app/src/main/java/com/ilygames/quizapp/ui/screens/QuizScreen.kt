package com.ilygames.quizapp.ui.screens

import android.net.Uri
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
                    CircularProgressIndicator(color = Color(0xFF255FF4), strokeWidth = 3.5.dp, modifier = Modifier.size(42.dp))
                }
            }
            is QuizState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .shadow(10.dp, RoundedCornerShape(26.dp))
                            .background(Color(0xFF1C273A), RoundedCornerShape(26.dp))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                ),
                                RoundedCornerShape(26.dp)
                            )
                            .padding(26.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF255FF4).copy(alpha = 0.15f), CircleShape)
                                    .border(1.5.dp, Color(0xFF255FF4), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Empty",
                                    tint = Color(0xFF255FF4),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Questions Available Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The quiz database is currently empty. Tap Admin Studio on the home screen to add or Bulk Upload questions!",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onExitQuiz,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            ) {
                                Text("GO BACK TO HOME", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            is QuizState.Active -> {
                val question = state.questions[state.currentQuestionIndex]
                val progress = (state.currentQuestionIndex + 1).toFloat() / state.questions.size
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "Progress")

                LaunchedEffect(state.currentQuestionIndex) {
                    if (state.currentQuestionIndex > 0) {
                        SoundManager.playWhooshSound()
                    }
                }

                // Single Smooth Vertical Scrollable Container (No cut-off options, no double scroll)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
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
                                .size(44.dp)
                                .shadow(6.dp, CircleShape)
                                .background(Color(0xFF1C273A), CircleShape)
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.Black.copy(alpha = 0.6f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Quiz", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        
                        LinearProgressIndicator(
                            progress = animatedProgress,
                            color = Color(0xFF255FF4),
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
                                    color = Color(0xFF255FF4).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, Color(0xFF255FF4), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${state.currentQuestionIndex + 1}/${state.questions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF255FF4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3D Soft-Clay Timer & Score Deck
                    val isDarkTimerDeck = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                    val timerDeckBg = if (isDarkTimerDeck) Color(0xFF1C273A) else Color.White

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(50.dp))
                            .background(timerDeckBg, RoundedCornerShape(50.dp))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDarkTimerDeck) 0.35f else 0.9f),
                                        Color.Black.copy(alpha = if (isDarkTimerDeck) 0.5f else 0.08f)
                                    )
                                ),
                                RoundedCornerShape(50.dp)
                            )
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
                                    tint = if (timerSeconds <= 5) IncorrectRed else Color(0xFF255FF4),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${timerSeconds}s remaining",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerSeconds <= 5) IncorrectRed else (if (isDarkTimerDeck) Color.White else Color(0xFF17181C))
                                )
                            }

                            Text(
                                text = "${state.score} pts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF255FF4),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // KEYED CONTENT BLOCK
                    key(currentQuestionIndex) {
                        Column {
                            if (!question.imageUrl.isNullOrBlank()) {
                                // 1. Separate Image Card (First, above question with crisp white outline and rounded edges)
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = fadeIn(tween(400, delayMillis = 0, easing = FastOutSlowInEasing)) + 
                                            slideInVertically(tween(400, delayMillis = 0, easing = FastOutSlowInEasing)) { 40 }
                                ) {
                                    val quizImgReq = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(question.imageUrl)
                                        .crossfade(true)
                                        .build()

                                    coil.compose.SubcomposeAsyncImage(
                                        model = quizImgReq,
                                        contentDescription = "Question Image",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = Color(0xFF255FF4), modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                            }
                                        },
                                        error = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = IncorrectRed, modifier = Modifier.size(36.dp))
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color(0xFF101828))
                                            .border(2.5.dp, Color.White, RoundedCornerShape(22.dp))
                                    )
                                }

                                 // Show Question Text Card ONLY for text questions (Hide question prompt for Image Quizzes in real quiz!)
                                if (!question.question.isNullOrBlank() && question.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 2. Separate Question Text Card (Below Image)
                                    AnimatedVisibility(
                                        visible = isContentVisible,
                                        enter = fadeIn(tween(400, delayMillis = 50, easing = FastOutSlowInEasing)) + 
                                                slideInVertically(tween(400, delayMillis = 50, easing = FastOutSlowInEasing)) { 40 }
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(22.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(0.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = question.question,
                                                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 28.sp, fontSize = 18.sp),
                                                    fontWeight = FontWeight.Black,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Normal Text Question Card
                                AnimatedVisibility(
                                    visible = isContentVisible,
                                    enter = fadeIn(tween(400, delayMillis = 0, easing = FastOutSlowInEasing)) + 
                                            slideInVertically(tween(400, delayMillis = 0, easing = FastOutSlowInEasing)) { 40 }
                                ) {
                                    val isDarkQCard = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                                    val qCardBg = if (isDarkQCard) Color(0xFF1C273A) else Color.White

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(10.dp, RoundedCornerShape(26.dp))
                                            .background(qCardBg, RoundedCornerShape(26.dp))
                                            .border(
                                                1.5.dp,
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (isDarkQCard) 0.35f else 0.9f),
                                                        Color.Black.copy(alpha = if (isDarkQCard) 0.5f else 0.08f)
                                                    )
                                                ),
                                                RoundedCornerShape(26.dp)
                                            )
                                            .padding(22.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = question.question,
                                            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 30.sp, fontSize = 19.sp),
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            color = if (isDarkQCard) Color.White else Color(0xFF17181C)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Smooth Options Stack (Column instead of LazyColumn to allow full page scrolling)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic Options List
                                val activeOptions = question.getOptionsList()
                                val letterLabels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")

                                activeOptions.forEachIndexed { index, optionText ->
                                    val key = letterLabels.getOrElse(index) { (index + 1).toString() }
                                    val delayMs = (index + 1) * 100

                                    AnimatedVisibility(
                                        visible = isContentVisible,
                                        enter = fadeIn(tween(400, delayMillis = delayMs, easing = FastOutSlowInEasing)) + 
                                                slideInVertically(tween(400, delayMillis = delayMs, easing = FastOutSlowInEasing)) { 45 }
                                    ) {
                                        CleanHighlightOptionRow(
                                            text = optionText,
                                            label = key,
                                            optionKey = key,
                                            selectedOption = selectedOption,
                                            correctAnswer = question.correctAnswer,
                                            onClick = {
                                                if (selectedOption == null) {
                                                    selectedOption = key
                                                    if (key == question.correctAnswer) {
                                                        SoundManager.playCorrectSound()
                                                    } else {
                                                        SoundManager.playWrongSound()
                                                        wrongCount++
                                                    }
                                                    coroutineScope.launch {
                                                        delay(550)
                                                        quizViewModel.submitAnswer(token, key)
                                                    }
                                                }
                                            }
                                        )
                                    }
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

    val isDarkQuiz = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val baseBg = if (isDarkQuiz) Color(0xFF1C273A) else Color.White

    val backgroundColor = when {
        isSelected && isCorrectOption -> CorrectGreen.copy(alpha = 0.18f)
        isSelected && !isCorrectOption -> IncorrectRed.copy(alpha = 0.18f)
        else -> baseBg
    }

    val borderColor = when {
        isSelected && isCorrectOption -> CorrectGreen
        isSelected && !isCorrectOption -> IncorrectRed
        else -> if (isDarkQuiz) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f)
    }

    val badgeBrush = when {
        isSelected && isCorrectOption -> Brush.radialGradient(listOf(CorrectGreen, DarkGreen))
        isSelected && !isCorrectOption -> Brush.radialGradient(listOf(IncorrectRed, Color(0xFF991B1B)))
        else -> Brush.radialGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .border(
                1.5.dp,
                if (isSelected) Brush.linearGradient(listOf(borderColor, borderColor)) else Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkQuiz) 0.35f else 0.9f),
                        Color.Black.copy(alpha = if (isDarkQuiz) 0.5f else 0.08f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                enabled = !isAnswered,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3D Glossy Blue Sphere Option Letter Badge (A, B, C, D)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(4.dp, CircleShape)
                    .background(badgeBrush, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                fontWeight = if (isSelected || (isAnswered && isCorrectOption)) FontWeight.Black else FontWeight.Bold,
                color = if (isDarkQuiz) Color.White else Color(0xFF17181C),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
