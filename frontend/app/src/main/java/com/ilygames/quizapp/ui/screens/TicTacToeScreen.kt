package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay

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

    // 10-Round Match State
    var currentRound by remember { mutableIntStateOf(1) }
    var p1Wins by remember { mutableIntStateOf(0) }
    var p2Wins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }

    // Current Round Board State (9 cells)
    var board by remember { mutableStateOf(Array(9) { "" }) }

    // Alternating Starting Chance per Round
    var isXTurn by remember { mutableStateOf(true) }
    var winningPattern by remember { mutableStateOf<List<Int>?>(null) }
    var isRoundComplete by remember { mutableStateOf(false) }
    var showMatchVictoryDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Grid Line Entrance Animation & Sound Trigger on Every Match/Round Start
    val gridLineAnim = remember { Animatable(0f) }
    LaunchedEffect(currentRound, showMatchVictoryDialog) {
        SoundManager.playRetrySound()
        gridLineAnim.snapTo(0f)
        gridLineAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

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

    fun startNextRound() {
        if (currentRound >= 10) {
            if (!rewardEarned) {
                rewardEarned = true
                authViewModel.addAdReward(context)
                Toast.makeText(context, "🪙 +50 Coins Earned for 10-Round Showdown!", Toast.LENGTH_SHORT).show()
            }
            showMatchVictoryDialog = true
            return
        }

        currentRound++
        board = Array(9) { "" }
        winningPattern = null
        isRoundComplete = false

        // Alternating First Turn
        isXTurn = (currentRound % 2 != 0)
    }

    fun resetFullMatch() {
        currentRound = 1
        p1Wins = 0
        p2Wins = 0
        draws = 0
        board = Array(9) { "" }
        isXTurn = true
        winningPattern = null
        isRoundComplete = false
        showMatchVictoryDialog = false
        rewardEarned = false
    }

    fun onCellClick(index: Int) {
        if (board[index].isNotEmpty() || isRoundComplete || showMatchVictoryDialog) return

        SoundManager.playPopSound()

        val mark = if (isXTurn) "X" else "O"
        val newBoard = board.copyOf()
        newBoard[index] = mark
        board = newBoard

        val (winner, pattern) = checkWinner(newBoard)
        if (winner != null) {
            isRoundComplete = true
            if (winner == "X") {
                SoundManager.playCorrectSound()
                p1Wins++
                winningPattern = pattern
            } else if (winner == "O") {
                SoundManager.playCorrectSound()
                p2Wins++
                winningPattern = pattern
            } else if (winner == "DRAW") {
                draws++
            }
        } else {
            isXTurn = !isXTurn
        }
    }

    // Auto-advance round after 1.8s when a round completes
    LaunchedEffect(isRoundComplete) {
        if (isRoundComplete && !showMatchVictoryDialog) {
            delay(1800)
            if (currentRound < 10) {
                startNextRound()
            } else {
                if (!rewardEarned) {
                    rewardEarned = true
                    authViewModel.addAdReward(context)
                    Toast.makeText(context, "🪙 +50 Coins Earned for 10-Round Showdown!", Toast.LENGTH_SHORT).show()
                }
                showMatchVictoryDialog = true
            }
        }
    }

    // Animated Strikeout Progress (0f -> 1f)
    val strikeAnimProgress by animateFloatAsState(
        targetValue = if (winningPattern != null) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "StrikeoutLine"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP HEADER BAR WITH 3D METALLIC GLASSMORPHISM BUTTONS ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 3D Metallic Glassmorphism Back Button
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

                // Round Counter Pill Badge
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
                        text = "ROUND $currentRound / 10",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // 3D Metallic Glassmorphism Retry Button
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

            Spacer(modifier = Modifier.height(20.dp))

            // ── PLAYER CARDS ROW: PLAYER 1 (LEFT) & PLAYER 2 (RIGHT) AT VERY TOP ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 Card (Attached to Left Edge)
                Box(
                    modifier = Modifier
                        .offset(x = (-3).dp)
                        .shadow(8.dp, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isXTurn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                        )
                        .border(
                            1.5.dp,
                            Color.White,
                            RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                        )
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
                                color = if (isXTurn) Color(0xFFDC2626) else Color(0xFF334155)
                            )
                        }
                    }
                }

                // Player 2 Card (Attached to Right Edge)
                Box(
                    modifier = Modifier
                        .offset(x = 3.dp)
                        .shadow(8.dp, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isXTurn) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        )
                        .border(
                            1.5.dp,
                            Color.White,
                            RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        )
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
                                color = if (!isXTurn) Color(0xFF1D4ED8) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // ── CENTER 3x3 TIC TAC TOE GRID WITH PURE WHITE FULL-LENGTH STRIKE LINE ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val colWidth = width / 3f
                    val rowHeight = height / 3f
                    val strokeWidth = 8.dp.toPx()

                    val lineProgress = gridLineAnim.value

                    // Vertical White Cross Lines (Animated Entrance)
                    drawLine(
                        color = Color.White,
                        start = Offset(colWidth, 0f),
                        end = Offset(colWidth, height * lineProgress),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(colWidth * 2, 0f),
                        end = Offset(colWidth * 2, height * lineProgress),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Horizontal White Cross Lines (Animated Entrance)
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, rowHeight),
                        end = Offset(width * lineProgress, rowHeight),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, rowHeight * 2),
                        end = Offset(width * lineProgress, rowHeight * 2),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // PURE WHITE SOLID COMPACT STRIKE OUT LINE
                    if (winningPattern != null && strikeAnimProgress > 0f) {
                        val pattern = winningPattern!!
                        val isRow = pattern == listOf(0, 1, 2) || pattern == listOf(3, 4, 5) || pattern == listOf(6, 7, 8)
                        val isCol = pattern == listOf(0, 3, 6) || pattern == listOf(1, 4, 7) || pattern == listOf(2, 5, 8)
                        val isDiag1 = pattern == listOf(0, 4, 8)
                        val isDiag2 = pattern == listOf(2, 4, 6)

                        val (fullStartX, fullStartY, fullEndX, fullEndY) = when {
                            isRow -> {
                                val rowIdx = pattern[0] / 3
                                val y = (rowIdx + 0.5f) * rowHeight
                                listOf(colWidth * 0.15f, y, colWidth * 2.85f, y)
                            }
                            isCol -> {
                                val colIdx = pattern[0] % 3
                                val x = (colIdx + 0.5f) * colWidth
                                listOf(x, rowHeight * 0.15f, x, rowHeight * 2.85f)
                            }
                            isDiag1 -> listOf(colWidth * 0.15f, rowHeight * 0.15f, colWidth * 2.85f, rowHeight * 2.85f)
                            isDiag2 -> listOf(colWidth * 2.85f, rowHeight * 0.15f, colWidth * 0.15f, rowHeight * 2.85f)
                            else -> listOf(colWidth * 0.15f, rowHeight * 0.15f, colWidth * 2.85f, rowHeight * 2.85f)
                        }

                        val curEndX = fullStartX + (fullEndX - fullStartX) * strikeAnimProgress
                        val curEndY = fullStartY + (fullEndY - fullStartY) * strikeAnimProgress

                        // Compact Solid White Strike Line
                        drawLine(
                            color = Color.White,
                            start = Offset(fullStartX, fullStartY),
                            end = Offset(curEndX, curEndY),
                            strokeWidth = 8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Interactive 3x3 Grid Buttons
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
                                val mark = board[idx]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onCellClick(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (mark.isNotEmpty()) {
                                        var isMarked by remember { mutableStateOf(false) }
                                        LaunchedEffect(mark) { isMarked = true }

                                        val markScale by animateFloatAsState(
                                            targetValue = if (isMarked) 1.0f else 0.0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "MarkPopAnimation"
                                        )

                                        if (mark == "X") {
                                            Text(
                                                text = "X",
                                                fontSize = 64.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.scale(markScale)
                                            )
                                        } else if (mark == "O") {
                                            Text(
                                                text = "O",
                                                fontSize = 64.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF255FF4),
                                                modifier = Modifier.scale(markScale)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 10-ROUND MATCH GRAND CHAMPION VICTORY MODAL ──────────────────
        if (showMatchVictoryDialog) {
            val p1WonMatch = p1Wins > p2Wins
            val p2WonMatch = p2Wins > p1Wins

            LaunchedEffect(Unit) {
                SoundManager.playCorrectSound()
            }

            var isCardVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isCardVisible = true
            }

            val cardScale by animateFloatAsState(
                targetValue = if (isCardVisible) 1.0f else 0.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "CardSpringEntrance"
            )

            val cardAlpha by animateFloatAsState(
                targetValue = if (isCardVisible) 1.0f else 0.0f,
                animationSpec = tween(350),
                label = "CardAlphaEntrance"
            )

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
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .scale(cardScale)
                            .graphicsLayer { alpha = cardAlpha }
                            .shadow(24.dp, RoundedCornerShape(32.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    else listOf(Color.White, Color(0xFFF1F5F9))
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(
                                2.5.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.8f else 0.95f),
                                        Color.White.copy(alpha = if (isDark) 0.2f else 0.4f)
                                    )
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .padding(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 3D Metallic Circular Trophy Badge
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(trophyScale)
                                    .shadow(12.dp, CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            if (p1WonMatch) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                            else if (p2WonMatch) listOf(Color(0xFF255FF4), Color(0xFF1E40AF))
                                            else listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (p1WonMatch || p2WonMatch) "🏆" else "🤝",
                                    fontSize = 44.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Celebration Headline
                            Text(
                                text = if (p1WonMatch) "🎉 Player 1 Wins!"
                                else if (p2WonMatch) "🎉 Player 2 Wins!"
                                else "🤝 It's a Draw!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Stacked Score Cards (3D Glassmorphism Cards)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Player 1 Score Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(16.dp))
                                        .background(
                                            if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = if (isDark) 0.2f else 0.8f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(vertical = 14.dp, horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFFEF4444), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Player 1 Score",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    Text(
                                        text = "$p1Wins pts",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFEF4444)
                                    )
                                }

                                // Player 2 Score Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(16.dp))
                                        .background(
                                            if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = if (isDark) 0.2f else 0.8f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(vertical = 14.dp, horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF255FF4), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Player 2 Score",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    Text(
                                        text = "$p2Wins pts",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF255FF4)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Side-by-side 3D Metallic Action Push-Buttons (Play Again & Home)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 3D Metallic Play Again Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .shadow(10.dp, RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.3f))
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable {
                                            SoundManager.playClickSound()
                                            resetFullMatch()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PLAY AGAIN",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }

                                // 3D Metallic Home Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .shadow(10.dp, RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                if (isDark) listOf(Color(0xFF475569), Color(0xFF334155))
                                                else listOf(Color(0xFF64748B), Color(0xFF475569))
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.3f))
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable {
                                            SoundManager.playClickSound()
                                            onBack()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HOME",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
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
}
