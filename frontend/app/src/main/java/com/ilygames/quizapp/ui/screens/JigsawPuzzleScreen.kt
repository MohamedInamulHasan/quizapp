package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun JigsawPuzzleScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // 16 Target Slots for 4x4 Grid Board (array holds pieceId 0..15 or null)
    var boardSlots by remember { mutableStateOf(Array<Int?>(16) { null }) }
    // Unplaced Tray Pieces (Shuffled 0..15)
    var trayPieceIds by remember { mutableStateOf((0..15).shuffled()) }
    // Currently highlighted piece in tray
    var selectedPieceId by remember { mutableStateOf<Int?>(null) }

    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    fun resetPuzzle() {
        boardSlots = Array(16) { null }
        trayPieceIds = (0..15).shuffled()
        selectedPieceId = null
        p1Score = 0
        p2Score = 0
        isPlayer1Turn = true
        showWinnerDialog = false
        rewardEarned = false
        SoundManager.playRetrySound()
    }

    fun checkSolved() {
        if (boardSlots.all { it != null }) {
            SoundManager.playSuccessChime()
            if (!rewardEarned) {
                rewardEarned = true
                authViewModel.addAdReward(context)
                Toast.makeText(context, "🪙 +50 Coins Earned for Jungle Jigsaw!", Toast.LENGTH_SHORT).show()
            }
            showWinnerDialog = true
        }
    }

    fun placePiece(pieceId: Int, slotIndex: Int) {
        if (boardSlots[slotIndex] != null) return // Already filled

        SoundManager.playPopSound()
        val newSlots = boardSlots.clone()
        newSlots[slotIndex] = pieceId
        boardSlots = newSlots
        trayPieceIds = trayPieceIds.filter { it != pieceId }
        selectedPieceId = null

        if (pieceId == slotIndex) {
            SoundManager.playCorrectSound()
            if (isPlayer1Turn) p1Score += 15 else p2Score += 15
        } else {
            if (isPlayer1Turn) p1Score += 5 else p2Score += 5
        }

        isPlayer1Turn = !isPlayer1Turn
        checkSolved()
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
            // ── TOP HEADER BAR (3D BUTTONS & PILL) ─────────────────────────
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

                // Title Pill Badge
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
                        text = "JUNGLE JIGSAW 16P",
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
                            resetPuzzle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = textColor, modifier = Modifier.size(22.dp))
                }
            }

            // ── PLAYER 1 CARD ATTACHED TO LEFT EDGE (EXACT MEMORY MATCH SIZE) ──
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
                                text = "$p1Score",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPlayer1Turn) Color(0xFFDC2626) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // ── 4x4 CARTOON JUNGLE JIGSAW PUZZLE BOARD ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                    .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (r in 0..3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (c in 0..3) {
                                val slotIndex = r * 4 + c
                                val pieceId = boardSlots[slotIndex]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (pieceId != null) Color.Transparent
                                            else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedPieceId != null && pieceId == null) Color.Yellow
                                            else Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            selectedPieceId?.let { pId ->
                                                placePiece(pId, slotIndex)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (pieceId != null) {
                                        JigsawImageSliceCanvas(pieceId = pieceId)
                                    } else {
                                        Text(
                                            text = "${slotIndex + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.35f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 16 CARTOON IMAGE PIECES TRAY ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(135.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (selectedPieceId != null) "✨ Tap any target slot to drop piece!" else "👇 Tap a piece from tray to place on board:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(trayPieceIds) { pieceId ->
                        val isSelected = selectedPieceId == pieceId

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(8.dp))
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.Yellow else Color.White.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    SoundManager.playPopSound()
                                    selectedPieceId = if (isSelected) null else pieceId
                                }
                        ) {
                            JigsawImageSliceCanvas(pieceId = pieceId)
                        }
                    }
                }
            }

            // ── PLAYER 2 CARD ATTACHED TO RIGHT EDGE (EXACT MEMORY MATCH SIZE) ──
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
                                text = "$p2Score",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (!isPlayer1Turn) Color(0xFF1D4ED8) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── VICTORY MODAL ────────────────────────────────────────────────
        if (showWinnerDialog) {
            val p1Won = p1Score > p2Score
            val p2Won = p2Score > p1Score

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
                                            else if (p2Won) listOf(Color(0xFF255FF4), Color(0xFF1E40AF))
                                            else listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🦁", fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (p1Won) "🎉 Player 1 Solved It!" else if (p2Won) "🎉 Player 2 Solved It!" else "🧩 Jungle Jigsaw Solved!",
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
                                        Text("Player 1 Points", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("$p1Score pts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                        Text("Player 2 Points", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("$p2Score pts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                        resetPuzzle()
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

// ── CANVAS DRAWING FOR CARTOON JUNGLE ANIMAL SCENE (16 CROPPED JIGSAW SLICES) ──
@Composable
fun JigsawImageSliceCanvas(pieceId: Int) {
    val pieceRow = pieceId / 4
    val pieceCol = pieceId % 4

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Full virtual picture is 4x size of a single piece
        val fullW = w * 4f
        val fullH = h * 4f

        val offsetX = -pieceCol * w
        val offsetY = -pieceRow * h

        clipRect(0f, 0f, w, h) {
            // Draw Full Cartoon Jungle Picture offset by piece coordinates!
            drawCartoonJungleArt(fullW = fullW, fullH = fullH, offsetX = offsetX, offsetY = offsetY)

            // Draw Interlocking Jigsaw Contour Lines Over Piece
            drawJigsawInterlockingContour(pieceRow = pieceRow, pieceCol = pieceCol, w = w, h = h)
        }
    }
}

// Draw Full Cartoon Jungle Safari Art Scene on Canvas (Matching Screenshot)
fun DrawScope.drawCartoonJungleArt(fullW: Float, fullH: Float, offsetX: Float, offsetY: Float) {
    // 1. Lush Green Jungle Background
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
        topLeft = Offset(offsetX, offsetY),
        size = Size(fullW, fullH)
    )

    // Jungle Tree Branch (Top)
    drawRect(
        color = Color(0xFF78350F),
        topLeft = Offset(offsetX, offsetY),
        size = Size(fullW, fullH * 0.22f)
    )
    drawCircle(
        color = Color(0xFF047857),
        center = Offset(offsetX + fullW * 0.5f, offsetY + fullH * 0.1f),
        radius = fullW * 0.35f
    )

    // 2. Cute Purple Elephant 🐘 (Top-Left / Center-Left)
    val eleCenterX = offsetX + fullW * 0.32f
    val eleCenterY = offsetY + fullH * 0.42f
    val eleRadius = fullW * 0.18f

    // Ears
    drawCircle(color = Color(0xFFC084FC), center = Offset(eleCenterX - eleRadius * 0.8f, eleCenterY - eleRadius * 0.2f), radius = eleRadius * 0.65f)
    drawCircle(color = Color(0xFFE9D5FF), center = Offset(eleCenterX - eleRadius * 0.8f, eleCenterY - eleRadius * 0.2f), radius = eleRadius * 0.4f)
    // Head & Body
    drawCircle(color = Color(0xFFA855F7), center = Offset(eleCenterX, eleCenterY), radius = eleRadius)
    // Eyes
    drawCircle(color = Color.White, center = Offset(eleCenterX + eleRadius * 0.2f, eleCenterY - eleRadius * 0.3f), radius = eleRadius * 0.25f)
    drawCircle(color = Color(0xFF2563EB), center = Offset(eleCenterX + eleRadius * 0.25f, eleCenterY - eleRadius * 0.3f), radius = eleRadius * 0.14f)
    // Trunk
    drawArc(
        color = Color(0xFFA855F7),
        startAngle = 180f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(eleCenterX - eleRadius * 0.9f, eleCenterY - eleRadius * 0.1f),
        size = Size(eleRadius * 1.2f, eleRadius * 1.2f),
        style = Stroke(width = eleRadius * 0.3f)
    )

    // 3. Cute Yellow Giraffe 🦒 (Right Side)
    val girX = offsetX + fullW * 0.78f
    val girY = offsetY + fullH * 0.48f

    // Neck
    drawRect(color = Color(0xFFFBBF24), topLeft = Offset(girX - fullW * 0.05f, girY - fullH * 0.25f), size = Size(fullW * 0.12f, fullH * 0.45f))
    // Head
    drawCircle(color = Color(0xFFF59E0B), center = Offset(girX, girY - fullH * 0.25f), radius = fullW * 0.1f)
    // Spots
    drawCircle(color = Color(0xFFB45309), center = Offset(girX, girY - fullH * 0.15f), radius = fullW * 0.025f)
    drawCircle(color = Color(0xFFB45309), center = Offset(girX + fullW * 0.03f, girY), radius = fullW * 0.03f)
    // Eye
    drawCircle(color = Color.White, center = Offset(girX - fullW * 0.02f, girY - fullH * 0.27f), radius = fullW * 0.025f)
    drawCircle(color = Color.Black, center = Offset(girX - fullW * 0.02f, girY - fullH * 0.27f), radius = fullW * 0.012f)

    // 4. Cute Cheerful Lion 🦁 (Bottom Center)
    val lionX = offsetX + fullW * 0.58f
    val lionY = offsetY + fullH * 0.76f
    val lionR = fullW * 0.14f

    // Mane
    drawCircle(color = Color(0xFFB45309), center = Offset(lionX, lionY), radius = lionR * 1.35f)
    // Face
    drawCircle(color = Color(0xFFF59E0B), center = Offset(lionX, lionY), radius = lionR)
    // Eyes
    drawCircle(color = Color.White, center = Offset(lionX - lionR * 0.35f, lionY - lionR * 0.25f), radius = lionR * 0.28f)
    drawCircle(color = Color(0xFF1D4ED8), center = Offset(lionX - lionR * 0.35f, lionY - lionR * 0.25f), radius = lionR * 0.14f)
    drawCircle(color = Color.White, center = Offset(lionX + lionR * 0.35f, lionY - lionR * 0.25f), radius = lionR * 0.28f)
    drawCircle(color = Color(0xFF1D4ED8), center = Offset(lionX + lionR * 0.35f, lionY - lionR * 0.25f), radius = lionR * 0.14f)
    // Nose & Mouth
    drawCircle(color = Color(0xFFDC2626), center = Offset(lionX, lionY + lionR * 0.15f), radius = lionR * 0.2f)

    // 5. Cute Black & White Striped Zebra 🦓 (Bottom Left)
    val zebX = offsetX + fullW * 0.2f
    val zebY = offsetY + fullH * 0.78f
    val zebR = fullW * 0.12f

    drawCircle(color = Color.White, center = Offset(zebX, zebY), radius = zebR)
    // Black Stripes
    drawRoundRect(color = Color.Black, topLeft = Offset(zebX - zebR * 0.8f, zebY - zebR * 0.5f), size = Size(zebR * 0.5f, zebR * 0.2f), cornerRadius = CornerRadius(6f))
    drawRoundRect(color = Color.Black, topLeft = Offset(zebX - zebR * 0.8f, zebY + zebR * 0.1f), size = Size(zebR * 0.6f, zebR * 0.2f), cornerRadius = CornerRadius(6f))
    // Eye
    drawCircle(color = Color(0xFF2563EB), center = Offset(zebX + zebR * 0.3f, zebY - zebR * 0.3f), radius = zebR * 0.22f)

    // 6. Playful Monkey 🐒 (Top Center hanging)
    val monX = offsetX + fullW * 0.56f
    val monY = offsetY + fullH * 0.25f
    val monR = fullW * 0.08f

    drawCircle(color = Color(0xFF92400E), center = Offset(monX, monY), radius = monR)
    drawCircle(color = Color(0xFFFDE68A), center = Offset(monX, monY + monR * 0.1f), radius = monR * 0.7f)
    drawCircle(color = Color.Black, center = Offset(monX - monR * 0.25f, monY - monR * 0.1f), radius = monR * 0.15f)
    drawCircle(color = Color.Black, center = Offset(monX + monR * 0.25f, monY - monR * 0.1f), radius = monR * 0.15f)

    // 7. Pink Butterfly 🦋
    drawCircle(color = Color(0xFFF43F5E), center = Offset(offsetX + fullW * 0.18f, offsetY + fullH * 0.22f), radius = fullW * 0.035f)
    drawCircle(color = Color(0xFFFB7185), center = Offset(offsetX + fullW * 0.22f, offsetY + fullH * 0.22f), radius = fullW * 0.035f)
}

// Draw Classic Dark Interlocking Jigsaw Outline Edges
fun DrawScope.drawJigsawInterlockingContour(pieceRow: Int, pieceCol: Int, w: Float, h: Float) {
    val strokeWidth = 3f
    val contourColor = Color.Black.copy(alpha = 0.5f)
    val tabR = w * 0.18f

    val path = Path()

    // Outer Piece Rect Outline with Interlocking Tabs & Socket Cutouts
    path.moveTo(0f, 0f)

    // Top Edge
    if (pieceRow > 0) {
        val midX = w / 2f
        path.lineTo(midX - tabR, 0f)
        if (pieceRow % 2 == 0) {
            path.cubicTo(midX - tabR, tabR, midX + tabR, tabR, midX + tabR, 0f)
        } else {
            path.cubicTo(midX - tabR, -tabR, midX + tabR, -tabR, midX + tabR, 0f)
        }
    }
    path.lineTo(w, 0f)

    // Right Edge
    if (pieceCol < 3) {
        val midY = h / 2f
        path.lineTo(w, midY - tabR)
        if (pieceCol % 2 == 0) {
            path.cubicTo(w + tabR, midY - tabR, w + tabR, midY + tabR, w, midY + tabR)
        } else {
            path.cubicTo(w - tabR, midY - tabR, w - tabR, midY + tabR, w, midY + tabR)
        }
    }
    path.lineTo(w, h)

    // Bottom Edge
    if (pieceRow < 3) {
        val midX = w / 2f
        path.lineTo(midX + tabR, h)
        if (pieceRow % 2 == 0) {
            path.cubicTo(midX + tabR, h + tabR, midX - tabR, h + tabR, midX - tabR, h)
        } else {
            path.cubicTo(midX + tabR, h - tabR, midX - tabR, h - tabR, midX - tabR, h)
        }
    }
    path.lineTo(0f, h)

    // Left Edge
    if (pieceCol > 0) {
        val midY = h / 2f
        path.lineTo(0f, midY + tabR)
        if (pieceCol % 2 == 0) {
            path.cubicTo(-tabR, midY + tabR, -tabR, midY - tabR, 0f, midY - tabR)
        } else {
            path.cubicTo(tabR, midY + tabR, tabR, midY - tabR, 0f, midY - tabR)
        }
    }
    path.lineTo(0f, 0f)

    drawPath(path = path, color = contourColor, style = Stroke(width = strokeWidth))
}
