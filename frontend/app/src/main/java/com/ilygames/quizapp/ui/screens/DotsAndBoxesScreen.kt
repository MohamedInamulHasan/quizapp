package com.ilygames.quizapp.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun DotsAndBoxesScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // 6 Rows x 6 Dots Grid (5 Rows x 5 Columns = 25 Claimable Boxes)
    // 0 = un-drawn/unclaimed, 1 = Player 1 (Red), 2 = Player 2 (Blue)
    var hLinesOwner by remember { mutableStateOf(Array(6) { IntArray(5) { 0 } }) }
    var vLinesOwner by remember { mutableStateOf(Array(5) { IntArray(6) { 0 } }) }
    var boxOwners by remember { mutableStateOf(Array(5) { IntArray(5) { 0 } }) }

    var currentRound by remember { mutableIntStateOf(1) }
    var p1Wins by remember { mutableIntStateOf(0) }
    var p2Wins by remember { mutableIntStateOf(0) }
    var p1BoxCount by remember { mutableIntStateOf(0) }
    var p2BoxCount by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) } // true = Red (P1), false = Blue (P2)

    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    // Check if drawing a line completed any boxes
    fun checkAndClaimCompletedBoxes(player: Int): Boolean {
        var claimedNewBox = false
        val updatedBoxOwners = Array(5) { r -> boxOwners[r].clone() }

        for (r in 0..4) {
            for (c in 0..4) {
                if (updatedBoxOwners[r][c] == 0) {
                    val topDrawn = hLinesOwner[r][c] != 0
                    val bottomDrawn = hLinesOwner[r + 1][c] != 0
                    val leftDrawn = vLinesOwner[r][c] != 0
                    val rightDrawn = vLinesOwner[r][c + 1] != 0

                    if (topDrawn && bottomDrawn && leftDrawn && rightDrawn) {
                        updatedBoxOwners[r][c] = player
                        claimedNewBox = true
                        if (player == 1) p1BoxCount++ else p2BoxCount++
                    }
                }
            }
        }

        if (claimedNewBox) {
            boxOwners = updatedBoxOwners
            SoundManager.playCorrectSound()
        }

        return claimedNewBox
    }

    fun checkGameOver() {
        if (p1BoxCount + p2BoxCount == 25) {
            if (p1BoxCount > p2BoxCount) p1Wins++
            else if (p2BoxCount > p1BoxCount) p2Wins++

            if (!rewardEarned) {
                rewardEarned = true
                authViewModel.addAdReward(context)
                Toast.makeText(context, "🪙 +50 Coins Earned for Dots & Boxes!", Toast.LENGTH_SHORT).show()
            }
            showWinnerDialog = true
        }
    }

    fun resetMatch() {
        hLinesOwner = Array(6) { IntArray(5) { 0 } }
        vLinesOwner = Array(5) { IntArray(6) { 0 } }
        boxOwners = Array(5) { IntArray(5) { 0 } }
        p1BoxCount = 0
        p2BoxCount = 0
        isPlayer1Turn = true
        showWinnerDialog = false
        rewardEarned = false
        SoundManager.playRetrySound()
    }

    fun onHorizontalLineClick(r: Int, c: Int) {
        if (hLinesOwner[r][c] != 0 || showWinnerDialog) return

        SoundManager.playPopSound()

        val player = if (isPlayer1Turn) 1 else 2
        val newHLines = Array(6) { i -> hLinesOwner[i].clone() }
        newHLines[r][c] = player
        hLinesOwner = newHLines

        val claimed = checkAndClaimCompletedBoxes(player)
        if (!claimed) {
            isPlayer1Turn = !isPlayer1Turn
        } else {
            checkGameOver()
        }
    }

    fun onVerticalLineClick(r: Int, c: Int) {
        if (vLinesOwner[r][c] != 0 || showWinnerDialog) return

        SoundManager.playPopSound()

        val player = if (isPlayer1Turn) 1 else 2
        val newVLines = Array(5) { i -> vLinesOwner[i].clone() }
        newVLines[r][c] = player
        vLinesOwner = newVLines

        val claimed = checkAndClaimCompletedBoxes(player)
        if (!claimed) {
            isPlayer1Turn = !isPlayer1Turn
        } else {
            checkGameOver()
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
                .navigationBarsPadding()
        ) {
            // ── TOP HEADER BAR (3D METALLIC BUTTONS) ──────────────────────
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

                Text(
                    text = "Dots & Boxes 6x6",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )

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
                        .clickable { resetMatch() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Match", tint = textColor, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── PLAYER CARDS ROW: AT VERY TOP DIRECTLY UNDER HEADER BAR ──────
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
                                text = "$p1BoxCount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPlayer1Turn) Color(0xFFDC2626) else Color(0xFF334155)
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
                                if (!isPlayer1Turn) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
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
                                text = "$p2BoxCount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (!isPlayer1Turn) Color(0xFF1D4ED8) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // ── CENTERED GAME BOARD AREA ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 6x6 DOTS CANVAS BOARD (5x5 BOXES GRID)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f)
                            else Color.White.copy(alpha = 0.95f)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val stepX = w / 5f
                        val stepY = h / 5f
                        val dotRadius = 8.5.dp.toPx() // Sleek 3D Glossy White Dots
                        val boldLineStrokeDrawn = 11.dp.toPx() // BOLD Player Drawn Lines
                        val boldLineStrokeUndrawn = 7.dp.toPx() // THICK Solid White Guide Lines

                        // 1. Draw THICK SOLID Claimed Box Fill Colors (Red P1 vs Blue P2)
                        for (r in 0..4) {
                            for (c in 0..4) {
                                val owner = boxOwners[r][c]
                                if (owner != 0) {
                                    val boxColor = if (owner == 1) Color(0xFFEF4444).copy(alpha = 0.88f) else Color(0xFF255FF4).copy(alpha = 0.88f)
                                    drawRoundRect(
                                        color = boxColor,
                                        topLeft = Offset(c * stepX + 5.dp.toPx(), r * stepY + 5.dp.toPx()),
                                        size = Size(stepX - 10.dp.toPx(), stepY - 10.dp.toPx()),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }
                            }
                        }

                        // 2. Draw BOLD Horizontal Lines (Thick Solid White for un-drawn, Red P1, Blue P2)
                        for (r in 0..5) {
                            for (c in 0..4) {
                                val owner = hLinesOwner[r][c]
                                val lineColor = when (owner) {
                                    1 -> Color(0xFFEF4444) // BOLD Red for Player 1
                                    2 -> Color(0xFF255FF4) // BOLD Blue for Player 2
                                    else -> Color.White // Thick Solid White Line in Normal State
                                }
                                drawLine(
                                    color = lineColor,
                                    start = Offset(c * stepX, r * stepY),
                                    end = Offset((c + 1) * stepX, r * stepY),
                                    strokeWidth = if (owner != 0) boldLineStrokeDrawn else boldLineStrokeUndrawn,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // 3. Draw BOLD Vertical Lines (Thick Solid White for un-drawn, Red P1, Blue P2)
                        for (r in 0..4) {
                            for (c in 0..5) {
                                val owner = vLinesOwner[r][c]
                                val lineColor = when (owner) {
                                    1 -> Color(0xFFEF4444) // BOLD Red for Player 1
                                    2 -> Color(0xFF255FF4) // BOLD Blue for Player 2
                                    else -> Color.White // Thick Solid White Line in Normal State
                                }
                                drawLine(
                                    color = lineColor,
                                    start = Offset(c * stepX, r * stepY),
                                    end = Offset(c * stepX, (r + 1) * stepY),
                                    strokeWidth = if (owner != 0) boldLineStrokeDrawn else boldLineStrokeUndrawn,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // 4. Draw 6x6 REAL 3D SPHERICAL GLOSSY WHITE DOTS
                        for (r in 0..5) {
                            for (c in 0..5) {
                                val center = Offset(c * stepX, r * stepY)

                                // Drop shadow behind white dot
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    center = Offset(center.x + 2.dp.toPx(), center.y + 3.dp.toPx()),
                                    radius = dotRadius
                                )

                                // Base 3D Soft White/Gray Rim
                                drawCircle(
                                    color = Color(0xFFCBD5E1),
                                    center = center,
                                    radius = dotRadius
                                )

                                // Glossy Inner Pure White Sphere
                                drawCircle(
                                    color = Color.White,
                                    center = center,
                                    radius = dotRadius * 0.85f
                                )

                                // Top-Left 3D Shine Highlight
                                drawCircle(
                                    color = Color.White,
                                    center = Offset(center.x - dotRadius * 0.35f, center.y - dotRadius * 0.35f),
                                    radius = dotRadius * 0.35f
                                )
                            }
                        }
                    }

                    // Interactive Line Touch Grid Overlay (6x6 Dots Grid)
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (r in 0..5) {
                            // Horizontal Click Strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0..4) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { onHorizontalLineClick(r, c) }
                                    )
                                }
                            }

                            if (r < 5) {
                                // Vertical Click Strip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (c in 0..5) {
                                        Box(
                                            modifier = Modifier
                                                .width(26.dp)
                                                .fillMaxHeight()
                                                .clickable { onVerticalLineClick(r, c) }
                                        )
                                        if (c < 5) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── VICTORY MODAL ────────────────────────────────────────────────
        if (showWinnerDialog) {
            val p1Won = p1BoxCount > p2BoxCount
            val p2Won = p2BoxCount > p1BoxCount

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

            androidx.compose.ui.window.Dialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
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
                                            if (p1Won) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                            else if (p2Won) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                            else listOf(Color(0xFF64748B), Color(0xFF334155))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (p1Won || p2Won) "🏆" else "🤝", fontSize = 44.sp)
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = if (p1Won) "🎉 Player 1 Wins!" else if (p2Won) "🎉 Player 2 Wins!" else "🤝 Match Tied!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Column(
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
                                        Text("Player 1 Boxes", fontSize = 14.sp, color = textColor)
                                    }
                                    Text("$p1BoxCount boxes", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF255FF4), CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Player 2 Boxes", fontSize = 14.sp, color = textColor)
                                    }
                                    Text("$p2BoxCount boxes", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF255FF4))
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

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
                                            resetMatch()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PLAY AGAIN", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
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
                                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("HOME", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
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
