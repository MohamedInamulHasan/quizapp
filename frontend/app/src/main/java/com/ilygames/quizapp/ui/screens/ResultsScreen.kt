package com.ilygames.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val quizState by quizViewModel.quizState.collectAsState()

    var showAdDialog by remember { mutableStateOf(false) }
    var adClaimed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg),
        contentAlignment = Alignment.Center
    ) {
        if (quizState is QuizState.Complete) {
            val completeState = quizState as QuizState.Complete
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Trophy/Award Visual
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Success",
                    tint = TextGold,
                    modifier = Modifier
                        .size(100.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), shape = CircleShape)
                        .border(1.5.dp, PrimaryGreen.copy(alpha = 0.4f), CircleShape)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Quiz Complete!",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )

                Text(
                    text = "Great job! Here are your stats:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Score Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Score", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${completeState.score}",
                                color = ElectricMint,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Time Taken Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = "Time", tint = TextMuted, modifier = Modifier.size(14.dp))
                                Text("Time", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${completeState.timeTaken}s",
                                color = PrimaryGreen,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Coins Earned Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = TextGold, modifier = Modifier.size(36.dp))
                            Column {
                                Text("Coins Earned", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                                Text("+ ${completeState.coinsEarned}", color = TextWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            }
                        }
                        
                        if (!adClaimed) {
                            Button(
                                onClick = { showAdDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(50.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                            ) {
                                Text("Double 2x", fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        } else {
                            Text("Claimed!", color = CorrectGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        quizViewModel.resetQuiz()
                        onBackToHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(50.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return Home", fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }
    }
}
