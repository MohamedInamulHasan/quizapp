package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun LiveQuizScreen(
    token: String,
    quizViewModel: QuizViewModel,
    onBack: () -> Unit
) {
    val liveQuizStatus by quizViewModel.liveQuizStatus.collectAsState()
    val liveQuizTimer by quizViewModel.liveQuizTimer.collectAsState()
    val liveQuizParticipantCount by quizViewModel.liveQuizParticipantCount.collectAsState()
    val liveQuizQuestion by quizViewModel.liveQuizQuestion.collectAsState()
    val liveQuizQuestionIndex by quizViewModel.liveQuizQuestionIndex.collectAsState()
    val liveQuizTotalQuestions by quizViewModel.liveQuizTotalQuestions.collectAsState()
    val liveQuizCorrectAnswer by quizViewModel.liveQuizCorrectAnswer.collectAsState()
    val liveQuizSelectedAnswer by quizViewModel.liveQuizSelectedAnswer.collectAsState()
    val liveQuizScore by quizViewModel.liveQuizScore.collectAsState()
    val liveQuizStandings by quizViewModel.liveQuizStandings.collectAsState()
    val liveQuizLogs by quizViewModel.liveQuizLogs.collectAsState()

    LaunchedEffect(Unit) {
        quizViewModel.joinLiveQuiz(token)
    }

    DisposableEffect(Unit) {
        onDispose {
            quizViewModel.leaveLiveQuiz()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            SoundManager.playClickSound()
                            onBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(GlassSurface, CircleShape)
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Live Quiz Deck",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextWhite
                    )
                }

                Text(
                    text = "Score: $liveQuizScore pts",
                    color = ElectricMint,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Sync Stats Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassSurface, RoundedCornerShape(50.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(50.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.People, contentDescription = "Players", tint = PrimaryGreen)
                    Text("Players: $liveQuizParticipantCount", fontWeight = FontWeight.Bold, color = TextWhite)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = ElectricMint)
                    Text("${liveQuizTimer}s", fontWeight = FontWeight.Black, color = ElectricMint)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Display Pane
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                contentAlignment = Alignment.Center
            ) {
                when (liveQuizStatus) {
                    "idle" -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                            colors = CardDefaults.cardColors(containerColor = GlassSurface),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Idle", tint = TextMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Awaiting Live Event...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The live quiz starts at 8:00 PM. Please wait for the administrator to launch the event from the Admin Web Panel.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    "waiting" -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                            colors = CardDefaults.cardColors(containerColor = GlassSurface),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "LOBBY WAITING ROOM",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricMint
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Quiz starts in $liveQuizTimer seconds. Get ready!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    "playing" -> {
                        val q = liveQuizQuestion
                        if (q != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    shape = RoundedCornerShape(26.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "QUESTION ${liveQuizQuestionIndex + 1} OF $liveQuizTotalQuestions",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricMint,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = q.question,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            color = TextWhite
                                        )
                                    }
                                }

                                // Animated Cylindrical Option Rows Stack (Dynamic Options Support)
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1.5f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val activeOptions = q.getOptionsList()
                                    val letterLabels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")

                                    items(activeOptions.indices.toList()) { index ->
                                        val key = letterLabels.getOrElse(index) { (index + 1).toString() }
                                        val optText = activeOptions[index]
                                        AnimatedLiveOptionRow(
                                            text = optText,
                                            label = key,
                                            isSelected = liveQuizSelectedAnswer == key,
                                            isCorrect = liveQuizCorrectAnswer == key,
                                            isDisabled = liveQuizSelectedAnswer != null || liveQuizCorrectAnswer != null,
                                            onClick = { quizViewModel.submitLiveAnswer(key) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "ended" -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                            colors = CardDefaults.cardColors(containerColor = GlassSurface),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "LIVE SCOREBOARD STANDINGS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricMint
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(liveQuizStandings) { standing ->
                                        Text(
                                            text = standing,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextWhite,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SurfaceElevated, RoundedCornerShape(14.dp))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                                .padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Console Log
            Text(
                text = "REAL-TIME LOG CONSOLE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(GlassSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(liveQuizLogs.reversed()) { log ->
                        Text(
                            text = log,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (log.contains("Error")) IncorrectRed else if (log.contains("Correct!")) ElectricMint else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedLiveOptionRow(
    text: String,
    label: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "OptionScale"
    )

    val outlineColor by animateColorAsState(
        targetValue = if (isDisabled) {
            if (isCorrect) CorrectGreen
            else if (isSelected) IncorrectRed
            else GlassBorder
        } else {
            if (isSelected) PrimaryGreen else GlassBorder
        },
        animationSpec = tween(250),
        label = "OutlineGlow"
    )

    val cardBg by animateColorAsState(
        targetValue = if (isDisabled && isCorrect) {
            CorrectGreen.copy(alpha = 0.2f)
        } else if (isDisabled && isSelected) {
            IncorrectRed.copy(alpha = 0.2f)
        } else if (isSelected) {
            PrimaryGreen.copy(alpha = 0.15f)
        } else {
            GlassSurface
        },
        animationSpec = tween(250),
        label = "CardBg"
    )

    Card(
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .clickable(
                enabled = !isDisabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .border(1.5.dp, outlineColor, RoundedCornerShape(50.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isDisabled && isCorrect) CorrectGreen else PrimaryGreen, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.weight(1f)
            )

            if (isSelected || (isDisabled && isCorrect)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Status",
                    tint = if (isDisabled && isCorrect) CorrectGreen else ElectricMint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
