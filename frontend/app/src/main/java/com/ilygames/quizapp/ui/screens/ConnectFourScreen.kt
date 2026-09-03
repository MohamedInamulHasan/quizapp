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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ConnectFourScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val scope = rememberCoroutineScope()

    // 6 Rows x 7 Columns Connect 4 Grid
    // 0 = Empty, 1 = Red (Player 1), 2 = Yellow (Player 2)
    var board by remember { mutableStateOf(Array(6) { IntArray(7) { 0 } }) }
    var currentRound by remember { mutableIntStateOf(1) }
    var p1Wins by remember { mutableIntStateOf(0) }
    var p2Wins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) } // true = Red (P1), false = Yellow (P2)
    var winningCoords by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var isRoundComplete by remember { mutableStateOf(false) }
    var showMatchVictoryDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Drop Disc Animation State
    var activeDroppingCol by remember { mutableStateOf<Int?>(null) }
    val dropAnimProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    LaunchedEffect(currentRound, showMatchVictoryDialog) {
        SoundManager.playRetrySound()
    }

    // Check Connect 4 Winner (4 in a row)
    fun checkConnectFourWinner(grid: Array<IntArray>): Pair<Int?, List<Pair<Int, Int>>?> {
        // Horizontal
        for (r in 0..5) {
            for (c in 0..3) {
                val p = grid[r][c]
                if (p != 0 && p == grid[r][c + 1] && p == grid[r][c + 2] && p == grid[r][c + 3]) {
                    return Pair(p, listOf(Pair(r, c), Pair(r, c + 1), Pair(r, c + 2), Pair(r, c + 3)))
                }
            }
        }
        // Vertical
        for (r in 0..2) {
            for (c in 0..6) {
                val p = grid[r][c]
                if (p != 0 && p == grid[r + 1][c] && p == grid[r + 2][c] && p == grid[r + 3][c]) {
                    return Pair(p, listOf(Pair(r, c), Pair(r + 1, c), Pair(r + 2, c), Pair(r + 3, c)))
                }
            }
        }
        // Diagonal ↘
        for (r in 0..2) {
            for (c in 0..3) {
                val p = grid[r][c]
                if (p != 0 && p == grid[r + 1][c + 1] && p == grid[r + 2][c + 2] && p == grid[r + 3][c + 3]) {
                    return Pair(p, listOf(Pair(r, c), Pair(r + 1, c + 1), Pair(r + 2, c + 2), Pair(r + 3, c + 3)))
                }
            }
        }
        // Diagonal ↗
        for (r in 3..5) {
            for (c in 0..3) {
                val p = grid[r][c]
                if (p != 0 && p == grid[r - 1][c + 1] && p == grid[r - 2][c + 2] && p == grid[r - 3][c + 3]) {
                    return Pair(p, listOf(Pair(r, c), Pair(r - 1, c + 1), Pair(r - 2, c + 2), Pair(r - 3, c + 3)))
                }
            }
        }

        // Draw check
        var isFull = true
        for (r in 0..5) {
            for (c in 0..6) {
                if (grid[r][c] == 0) isFull = false
            }
        }
        if (isFull) return Pair(0, null) // Draw

        return Pair(null, null)
    }

    fun startNextRound() {
        if (currentRound >= 10) {
            if (!rewardEarned) {
                rewardEarned = true
                authViewModel.addAdReward(context)
                Toast.makeText(context, "🪙 +50 Coins Earned for Connect 4 Showdown!", Toast.LENGTH_SHORT).show()
            }
            showMatchVictoryDialog = true
            return
        }

        currentRound++
        board = Array(6) { IntArray(7) { 0 } }
        winningCoords = null
        isRoundComplete = false
        isPlayer1Turn = (currentRound % 2 != 0)
    }

    fun resetFullMatch() {
        currentRound = 1
        p1Wins = 0
        p2Wins = 0
        draws = 0
        board = Array(6) { IntArray(7) { 0 } }
        isPlayer1Turn = true
        winningCoords = null
        isRoundComplete = false
        showMatchVictoryDialog = false
        rewardEarned = false
        SoundManager.playRetrySound()
    }

    fun dropDiscInColumn(col: Int) {
        if (isRoundComplete || showMatchVictoryDialog || activeDroppingCol != null) return

        // Find lowest empty row in column col
        var targetRow = -1
        for (r in 5 downTo 0) {
            if (board[r][col] == 0) {
                targetRow = r
                break
            }
        }

        if (targetRow == -1) return // Column is full

        SoundManager.playPopSound()

        scope.launch {
            activeDroppingCol = col
            dropAnimProgress.snapTo(0f)
            dropAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )

            val playerDisc = if (isPlayer1Turn) 1 else 2
            val newBoard = Array(6) { r -> board[r].clone() }
            newBoard[targetRow][col] = playerDisc
            board = newBoard
            activeDroppingCol = null

            val (winner, coords) = checkConnectFourWinner(newBoard)
            if (winner != null) {
                isRoundComplete = true
                if (winner == 1) {
                    SoundManager.playCorrectSound()
                    p1Wins++
                    winningCoords = coords
                } else if (winner == 2) {
                    SoundManager.playCorrectSound()
                    p2Wins++
                    winningCoords = coords
                } else if (winner == 0) {
                    draws++
                }
            } else {
                isPlayer1Turn = !isPlayer1Turn
            }
        }
    }

    // Auto-advance round after 1.8s
    LaunchedEffect(isRoundComplete) {
        if (isRoundComplete && !showMatchVictoryDialog) {
            kotlinx.coroutines.delay(1800)
            if (currentRound < 10) {
                startNextRound()
            } else {
                if (!rewardEarned) {
                    rewardEarned = true
                    authViewModel.addAdReward(context)
                    Toast.makeText(context, "🪙 +50 Coins Earned for Connect 4 Showdown!", Toast.LENGTH_SHORT).show()
                }
                showMatchVictoryDialog = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP HEADER BAR (3D BUTTONS & ROUND PILL) ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 3D Back Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.5f else 0.9f),
                                    Color.Black.copy(alpha = if (isDark) 0.6f else 0.15f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            SoundManager.playClickSound()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(22.dp))
                }

                // Round Badge Pill
                Box(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))),
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 6.dp, horizontal = 18.dp)
                ) {
                    Text(
                        text = "ROUND $currentRound OF 10",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // 3D Retry Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.5f else 0.9f),
                                    Color.Black.copy(alpha = if (isDark) 0.6f else 0.15f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            SoundManager.playRetrySound()
                            resetFullMatch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Match", tint = textColor, modifier = Modifier.size(22.dp))
                }
            }

            // ── PLAYER 1 CARD ATTACHED TO LEFT EDGE ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-3).dp)
                        .shadow(8.dp, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isPlayer1Turn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PLAYER 1",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$p1Wins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPlayer1Turn) Color(0xFFDC2626) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // ── TOP COLUMN DROP INDICATOR BUTTONS (DISCS ABOVE BOARD) ───────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0..6) {
                    val activePlayerColor = if (isPlayer1Turn) Color(0xFFEF4444) else Color(0xFFFFD700)
                    val isDroppingThisCol = activeDroppingCol == c

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(activePlayerColor, activePlayerColor.copy(alpha = 0.75f))
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .clickable { dropDiscInColumn(c) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Drop Column $c",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── 6x7 CONNECT 4 BOARD (ROYAL BLUE CONTAINER WITH ANIMATED DROP DISCS) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF1E40AF), Color(0xFF1E3A8A))),
                            RoundedCornerShape(20.dp)
                        )
                        .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (r in 0..5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (c in 0..6) {
                                    val cellVal = board[r][c]
                                    val isWinningDisc = winningCoords?.contains(Pair(r, c)) == true
                                    val isDroppingInThisCol = activeDroppingCol == c

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .shadow(if (isWinningDisc) 8.dp else 2.dp, CircleShape)
                                            .background(
                                                when (cellVal) {
                                                    1 -> Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFD32F2F)))
                                                    2 -> Brush.radialGradient(listOf(Color(0xFFFFEB3B), Color(0xFFFBC02D)))
                                                    else -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                                },
                                                CircleShape
                                            )
                                            .border(
                                                if (isWinningDisc) 3.dp else 1.dp,
                                                if (isWinningDisc) Color.White else Color.White.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                            .clickable { dropDiscInColumn(c) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cellVal != 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.85f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (cellVal == 1) Color(0xFFEF4444) else Color(0xFFFFD700)
                                                    )
                                            )
                                        } else if (isDroppingInThisCol) {
                                            // Gravity Drop Animation Disc falling from top to bottom
                                            val animatedOffsetY = (dropAnimProgress.value * (r + 1) * 35f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.85f)
                                                    .offset(y = (-animatedOffsetY).dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isPlayer1Turn) Color(0xFFEF4444) else Color(0xFFFFD700)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── PLAYER 2 CARD ATTACHED TO RIGHT EDGE ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 3.dp)
                        .shadow(8.dp, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isPlayer1Turn) listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PLAYER 2",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$p2Wins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (!isPlayer1Turn) Color(0xFFD97706) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── VICTORY MODAL ────────────────────────────────────────────────
        if (showMatchVictoryDialog) {
            val p1Won = p1Wins > p2Wins
            val p2Won = p2Wins > p1Wins

            androidx.compose.ui.window.Dialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
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
                            .shadow(24.dp, RoundedCornerShape(28.dp))
                            .background(Color(0xFF1E293B), RoundedCornerShape(28.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .shadow(10.dp, CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            if (p1Won) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                            else if (p2Won) listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                            else listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (p1Won || p2Won) "🏆" else "🤝", fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (p1Won) "🎉 Player 1 Wins Match!" else if (p2Won) "🎉 Player 2 Wins Match!" else "🤝 Connect 4 Match Tied!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444), CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Player 1 Victories", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("$p1Wins Wins", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFD700), CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Player 2 Victories", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("$p2Wins Wins", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        SoundManager.playClickSound()
                                        resetFullMatch()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Again", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Button(
                                    onClick = {
                                        SoundManager.playClickSound()
                                        onBack()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Home", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
