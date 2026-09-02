package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun TicTacToeScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val isDark = ThemeState.isDarkMode
    val context = LocalContext.current

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFEBF3FE)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    // Game Board State (3x3 grid = 9 cells: "", "X", or "O")
    var board by remember { mutableStateOf(Array(9) { "" }) }
    var isXTurn by remember { mutableStateOf(true) } // true = Player X (Red), false = Player O (Blue)
    var xScore by remember { mutableIntStateOf(0) }
    var oScore by remember { mutableIntStateOf(0) }
    var winningIndices by remember { mutableStateOf<List<Int>?>(null) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Winning Combination Patterns
    val winPatterns = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
        listOf(0, 4, 8), listOf(2, 4, 6)                  // Diagonals
    )

    fun checkWinner(currentBoard: Array<String>): Pair<String?, List<Int>?> {
        for (pattern in winPatterns) {
            val (a, b, c) = pattern
            if (currentBoard[a].isNotEmpty() && currentBoard[a] == currentBoard[b] && currentBoard[a] == currentBoard[c]) {
                return Pair(currentBoard[a], pattern)
            }
        }
        if (currentBoard.none { it.isEmpty() }) {
            return Pair("DRAW", null)
        }
        return Pair(null, null)
    }

    fun restartGame() {
        board = Array(9) { "" }
        isXTurn = true
        winningIndices = null
        showWinnerDialog = false
        rewardEarned = false
    }

    fun onCellClick(index: Int) {
        if (board[index].isNotEmpty() || winningIndices != null || showWinnerDialog) return

        SoundManager.playClickSound()

        val mark = if (isXTurn) "X" else "O"
        val newBoard = board.copyOf()
        newBoard[index] = mark
        board = newBoard

        val (winner, pattern) = checkWinner(newBoard)
        if (winner != null) {
            if (winner == "X") {
                SoundManager.playCorrectSound()
                xScore++
                winningIndices = pattern
                if (!rewardEarned) {
                    rewardEarned = true
                    authViewModel.addAdReward(context)
                    Toast.makeText(context, "🪙 +50 Coins Earned for X | O Victory!", Toast.LENGTH_SHORT).show()
                }
                showWinnerDialog = true
            } else if (winner == "O") {
                SoundManager.playCorrectSound()
                oScore++
                winningIndices = pattern
                if (!rewardEarned) {
                    rewardEarned = true
                    authViewModel.addAdReward(context)
                    Toast.makeText(context, "🪙 +50 Coins Earned for X | O Victory!", Toast.LENGTH_SHORT).show()
                }
                showWinnerDialog = true
            } else if (winner == "DRAW") {
                SoundManager.playWrongSound()
                showWinnerDialog = true
            }
        } else {
            isXTurn = !isXTurn // Pass turn to next player
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Top Header Bar ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            SoundManager.playClickSound()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(20.dp))
                }

                // Title
                Text(
                    text = "X | O Showdown",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )

                // Restart Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            SoundManager.playClickSound()
                            restartGame()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color(0xFF255FF4), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Scoreboard: PLAYER X (RED) vs PLAYER O (BLUE) ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PLAYER X Score Pill (RED)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isXTurn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            2.dp,
                            if (isXTurn) Color.White else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔴 PLAYER X", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$xScore", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                // PLAYER O Score Pill (BLUE)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isXTurn) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            2.dp,
                            if (!isXTurn) Color.White else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔵 PLAYER O", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$oScore", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 3x3 TIC TAC TOE GRID (PURE CLEAN WHITE GRID LINES) ───────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .background(
                        if (isDark) Color(0xFF1E293B) else Color(0xFF0F172A),
                        RoundedCornerShape(28.dp)
                    )
                    .border(3.dp, Color.White, RoundedCornerShape(28.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val idx = row * 3 + col
                                val isWinningCell = winningIndices?.contains(idx) == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isWinningCell) Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                            else Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                        )
                                        .border(
                                            2.dp,
                                            if (isWinningCell) Color.White else Color.White.copy(alpha = 0.7f),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable { onCellClick(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val mark = board[idx]
                                    if (mark == "X") {
                                        // RED X (Player 1)
                                        Text(
                                            text = "X",
                                            fontSize = 46.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFEF4444)
                                        )
                                    } else if (mark == "O") {
                                        // BLUE O (Player 2)
                                        Text(
                                            text = "O",
                                            fontSize = 46.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF255FF4)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── EXCITING ANIMATED 3D VICTORY CARD MODAL ──────────────────────
        if (showWinnerDialog) {
            val (winner, _) = checkWinner(board)
            val xWon = winner == "X"
            val oWon = winner == "O"

            val infiniteTransition = rememberInfiniteTransition(label = "TrophyPulse")
            val trophyScale by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "TrophyScale"
            )

            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(24.dp, RoundedCornerShape(32.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    else listOf(Color.White, Color(0xFFF1F5F9))
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(
                                2.5.dp,
                                Brush.verticalGradient(
                                    colors = if (xWon) listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
                                    else if (oWon) listOf(Color(0xFF255FF4), Color(0xFF06B6D4))
                                    else listOf(Color(0xFFF59E0B), Color(0xFFEAB308))
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Floating Animated 3D Trophy / Winner Badge
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .scale(trophyScale)
                                    .shadow(12.dp, CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = if (xWon) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                            else if (oWon) listOf(Color(0xFF255FF4), Color(0xFF1E40AF))
                                            else listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (xWon || oWon) "🏆" else "🤝",
                                    fontSize = 44.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Headline
                            Text(
                                text = if (xWon) "🔴 PLAYER X WINS!" else if (oWon) "🔵 PLAYER O WINS!" else "🤝 IT'S A DRAW!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "🎉 Masterful Tic Tac Toe Play!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = subTextColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Score Summary Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, RoundedCornerShape(20.dp))
                                    .background(
                                        if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔴 PLAYER X", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                    Text("$xScore", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
                                    Text("Wins", fontSize = 10.sp, color = Color.Gray)
                                }

                                Text("VS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔵 PLAYER O", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF255FF4))
                                    Text("$oScore", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
                                    Text("Wins", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Coin Reward Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🪙 ", fontSize = 16.sp)
                                    Text(
                                        "+50 BONUS COINS ADDED TO WALLET!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // Action Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        SoundManager.playClickSound()
                                        restartGame()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        SoundManager.playClickSound()
                                        onBack()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                                ) {
                                    Text("BACK TO HOME", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
