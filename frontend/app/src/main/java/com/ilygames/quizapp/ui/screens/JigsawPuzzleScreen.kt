package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlin.math.roundToInt
import kotlin.random.Random

// 5 Different Image Themes for Jigsaw Puzzle
enum class JigsawTheme(val title: String, val emoji: String) {
    PUPPY("Cute Golden Puppy", "🐶"),
    JUNGLE("Jungle Safari", "🦁"),
    SPACE("Cosmic Galaxy", "🚀"),
    OCEAN("Ocean World", "🐠"),
    CASTLE("Fantasy Castle", "🏰")
}

@Composable
fun JigsawPuzzleScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // Random Theme Selection on launch & restart
    var activeTheme by remember { mutableStateOf(JigsawTheme.values()[Random.nextInt(JigsawTheme.values().size)]) }

    // 16 Target Slots for 4x4 Grid Board (holds pieceId 0..15 or null)
    var boardSlots by remember { mutableStateOf(Array<Int?>(16) { null }) }
    // Unplaced Tray Pieces (Shuffled 0..15)
    var trayPieceIds by remember { mutableStateOf((0..15).shuffled()) }
    // Selected Piece ID (for tap or drag)
    var selectedPieceId by remember { mutableStateOf<Int?>(null) }

    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Board position bounds for exact drag-and-drop drop detection
    var boardBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    fun resetPuzzle() {
        // Pick a NEW DIFFERENT Image Theme on every play!
        val nextThemes = JigsawTheme.values().filter { it != activeTheme }
        activeTheme = nextThemes[Random.nextInt(nextThemes.size)]

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
                Toast.makeText(context, "🪙 +50 Coins Earned for Jigsaw!", Toast.LENGTH_SHORT).show()
            }
            showWinnerDialog = true
        }
    }

    fun placePiece(pieceId: Int, slotIndex: Int) {
        if (slotIndex !in 0..15) return
        if (boardSlots[slotIndex] != null) return // Already occupied

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
            // ── TOP HEADER BAR (3D BUTTONS & THEME PILL) ───────────────────
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

                // Theme Badge Pill (Shows current image theme name & emoji)
                Box(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))),
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 6.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "${activeTheme.emoji} ${activeTheme.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // 3D Retry Button (Generates a new image theme!)
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
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Theme", tint = textColor, modifier = Modifier.size(22.dp))
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

            // ── 4x4 IMAGE JIGSAW BOARD WITH WOODEN SLOT CUTOUTS (MATCHING SCREENSHOT) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
                    .onGloballyPositioned { coords ->
                        boardBounds = coords.boundsInRoot()
                    }
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xFF3D2314), RoundedCornerShape(20.dp)) // Dark Wooden Frame
                    .border(3.dp, Color(0xFF78350F), RoundedCornerShape(20.dp))
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
                                            else Color(0xFF5A3816), // Dark Wood Slot Cutout
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedPieceId != null && pieceId == null) Color.Yellow
                                            else Color(0xFF2C190B),
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
                                        DynamicJigsawImageCanvas(pieceId = pieceId, theme = activeTheme)
                                    } else {
                                        Text(
                                            text = "${slotIndex + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.25f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 16 PIECES TRAY (SUPPORTING DRAG AND DROP & TAP TO PLACE) ──────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(135.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (selectedPieceId != null) "✨ Drag or tap any slot to drop piece!" else "👇 Drag or tap a piece from tray to place:",
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
                        var pieceOffset by remember { mutableStateOf(Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .offset {
                                    IntOffset(
                                        pieceOffset.x.roundToInt(),
                                        pieceOffset.y.roundToInt()
                                    )
                                }
                                .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(8.dp))
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.Yellow else Color.White.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .pointerInput(pieceId) {
                                    detectDragGestures(
                                        onDragStart = {
                                            selectedPieceId = pieceId
                                            SoundManager.playPopSound()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            pieceOffset += dragAmount
                                        },
                                        onDragEnd = {
                                            // Check drop target relative to board
                                            if (pieceOffset.y < -120f) {
                                                val dropCol = ((pieceOffset.x + boardBounds.width / 2f) / (boardBounds.width / 4f)).coerceIn(0f, 3f).toInt()
                                                val dropRow = ((pieceOffset.y + boardBounds.height) / (boardBounds.height / 4f)).coerceIn(0f, 3f).toInt()
                                                val targetSlot = (dropRow * 4 + dropCol).coerceIn(0, 15)

                                                placePiece(pieceId, targetSlot)
                                            }
                                            pieceOffset = Offset.Zero
                                        }
                                    )
                                }
                                .clickable {
                                    SoundManager.playPopSound()
                                    selectedPieceId = if (isSelected) null else pieceId
                                }
                        ) {
                            DynamicJigsawImageCanvas(pieceId = pieceId, theme = activeTheme)
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
                                Text(activeTheme.emoji, fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (p1Won) "🎉 Player 1 Solved It!" else if (p2Won) "🎉 Player 2 Solved It!" else "🧩 ${activeTheme.title} Solved!",
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
                                        Text("Next Picture", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

// ── DYNAMIC MULTI-IMAGE CANVAS RENDERING SYSTEM (SUPPORTING 5 DIFFERENT PICTURE THEMES) ──
@Composable
fun DynamicJigsawImageCanvas(pieceId: Int, theme: JigsawTheme) {
    val pieceRow = pieceId / 4
    val pieceCol = pieceId % 4

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val fullW = w * 4f
        val fullH = h * 4f

        val offsetX = -pieceCol * w
        val offsetY = -pieceRow * h

        clipRect(0f, 0f, w, h) {
            when (theme) {
                JigsawTheme.PUPPY -> drawCutePuppyDogScene(fullW = fullW, fullH = fullH, offsetX = offsetX, offsetY = offsetY)
                JigsawTheme.JUNGLE -> drawJungleSafariScene(fullW = fullW, fullH = fullH, offsetX = offsetX, offsetY = offsetY)
                JigsawTheme.SPACE -> drawSpaceGalaxyScene(fullW = fullW, fullH = fullH, offsetX = offsetX, offsetY = offsetY)
                JigsawTheme.OCEAN -> drawOceanWorldScene(fullW = fullW, fullH = fullH, offsetX = offsetX, offsetY = offsetY)
                JigsawTheme.CASTLE -> drawFantasyCastleScene(fullW = fullW, fullH = fullH, offsetX = offsetX, offsetY = offsetY)
            }

            // Draw Interlocking Jigsaw Contour Lines
            drawJigsawInterlockingContour(pieceRow = pieceRow, pieceCol = pieceCol, w = w, h = h)
        }
    }
}

// 1. CUTE GOLDEN PUPPY DOG SCENE 🐶 (MATCHING USER SCREENSHOT uploaded_media_1788423535261.png)
fun DrawScope.drawCutePuppyDogScene(fullW: Float, fullH: Float, offsetX: Float, offsetY: Float) {
    // Green Garden Grass Background
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF86EFAC), Color(0xFF15803D))),
        topLeft = Offset(offsetX, offsetY),
        size = Size(fullW, fullH)
    )

    val pupX = offsetX + fullW * 0.5f
    val pupY = offsetY + fullH * 0.52f
    val pupR = fullW * 0.32f

    // Fluffy Ears
    drawCircle(color = Color(0xFFD97706), center = Offset(pupX - pupR * 0.85f, pupY - pupR * 0.2f), radius = pupR * 0.5f)
    drawCircle(color = Color(0xFFD97706), center = Offset(pupX + pupR * 0.85f, pupY - pupR * 0.2f), radius = pupR * 0.5f)

    // Puppy Fluffy Head & Muzzle
    drawCircle(color = Color(0xFFFEF3C7), center = Offset(pupX, pupY), radius = pupR)
    drawCircle(color = Color.White, center = Offset(pupX, pupY + pupR * 0.25f), radius = pupR * 0.55f)

    // Cute Dark Eyes
    drawCircle(color = Color(0xFF1E293B), center = Offset(pupX - pupR * 0.35f, pupY - pupR * 0.15f), radius = pupR * 0.16f)
    drawCircle(color = Color.White, center = Offset(pupX - pupR * 0.38f, pupY - pupR * 0.18f), radius = pupR * 0.05f)
    drawCircle(color = Color(0xFF1E293B), center = Offset(pupX + pupR * 0.35f, pupY - pupR * 0.15f), radius = pupR * 0.16f)
    drawCircle(color = Color.White, center = Offset(pupX + pupR * 0.32f, pupY - pupR * 0.18f), radius = pupR * 0.05f)

    // Nose
    drawCircle(color = Color(0xFF0F172A), center = Offset(pupX, pupY + pupR * 0.12f), radius = pupR * 0.12f)

    // Pink Tongue Output
    drawCircle(color = Color(0xFFF43F5E), center = Offset(pupX, pupY + pupR * 0.42f), radius = pupR * 0.18f)
}

// 2. JUNGLE SAFARI SCENE 🦁
fun DrawScope.drawJungleSafariScene(fullW: Float, fullH: Float, offsetX: Float, offsetY: Float) {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))), topLeft = Offset(offsetX, offsetY), size = Size(fullW, fullH))
    drawCircle(color = Color(0xFFA855F7), center = Offset(offsetX + fullW * 0.3f, offsetY + fullH * 0.4f), radius = fullW * 0.18f) // Elephant
    drawRect(color = Color(0xFFFBBF24), topLeft = Offset(offsetX + fullW * 0.7f, offsetY + fullH * 0.2f), size = Size(fullW * 0.12f, fullH * 0.5f)) // Giraffe
    drawCircle(color = Color(0xFFB45309), center = Offset(offsetX + fullW * 0.55f, offsetY + fullH * 0.75f), radius = fullW * 0.16f) // Lion Mane
}

// 3. COSMIC GALAXY SCENE 🚀
fun DrawScope.drawSpaceGalaxyScene(fullW: Float, fullH: Float, offsetX: Float, offsetY: Float) {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))), topLeft = Offset(offsetX, offsetY), size = Size(fullW, fullH))
    drawCircle(color = Color(0xFFF59E0B), center = Offset(offsetX + fullW * 0.75f, offsetY + fullH * 0.3f), radius = fullW * 0.15f) // Saturn
    drawCircle(color = Color(0xFFEF4444), center = Offset(offsetX + fullW * 0.3f, offsetY + fullH * 0.5f), radius = fullW * 0.1f) // Mars
    drawCircle(color = Color.White, center = Offset(offsetX + fullW * 0.2f, offsetY + fullH * 0.2f), radius = fullW * 0.02f) // Star
    drawCircle(color = Color.White, center = Offset(offsetX + fullW * 0.5f, offsetY + fullH * 0.8f), radius = fullW * 0.025f)
}

// 4. OCEAN WORLD SCENE 🐠
fun DrawScope.drawOceanWorldScene(fullW: Float, fullH: Float, offsetX: Float, offsetY: Float) {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0369A1))), topLeft = Offset(offsetX, offsetY), size = Size(fullW, fullH))
    drawCircle(color = Color(0xFFF97316), center = Offset(offsetX + fullW * 0.4f, offsetY + fullH * 0.45f), radius = fullW * 0.12f) // Clownfish
    drawCircle(color = Color(0xFF10B981), center = Offset(offsetX + fullW * 0.75f, offsetY + fullH * 0.7f), radius = fullW * 0.14f) // Sea Turtle
}

// 5. FANTASY CASTLE SCENE 🏰
fun DrawScope.drawFantasyCastleScene(fullW: Float, fullH: Float, offsetX: Float, offsetY: Float) {
    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFF472B6), Color(0xFF8B5CF6))), topLeft = Offset(offsetX, offsetY), size = Size(fullW, fullH))
    drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(offsetX + fullW * 0.35f, offsetY + fullH * 0.4f), size = Size(fullW * 0.3f, fullH * 0.45f)) // Castle
    drawRect(color = Color(0xFFEC4899), topLeft = Offset(offsetX + fullW * 0.38f, offsetY + fullH * 0.25f), size = Size(fullW * 0.08f, fullH * 0.15f)) // Spire 1
    drawRect(color = Color(0xFFEC4899), topLeft = Offset(offsetX + fullW * 0.54f, offsetY + fullH * 0.25f), size = Size(fullW * 0.08f, fullH * 0.15f)) // Spire 2
}
