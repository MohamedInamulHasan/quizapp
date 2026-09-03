package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

// 2x2 Interlocking Jigsaw Piece Model
data class DragJigsawPiece(
    val id: Int, // 0 = TopLeft, 1 = TopRight, 2 = BottomLeft, 3 = BottomRight
    val name: String,
    val pieceColor: Color,
    val outlineColor: Color = Color(0xFF1E293B),
    var isSnapped: Boolean = false,
    var currentOffset: Offset = Offset.Zero
)

@Composable
fun JigsawPuzzleScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // 4 Interlocking Jigsaw Pieces matching screenshot pastel colors
    // Top-Left: Pastel Lavender Purple (Color(0xFFB4ACE3))
    // Top-Right: Warm Pastel Orange (Color(0xFFF3B852))
    // Bottom-Left: Soft Pastel Green (Color(0xFFC7E294))
    // Bottom-Right: Cream Yellow (Color(0xFFF7F4D5))
    val defaultPieces = remember {
        listOf(
            DragJigsawPiece(0, "Top-Left", Color(0xFFB4ACE3)),
            DragJigsawPiece(1, "Top-Right", Color(0xFFF3B852)),
            DragJigsawPiece(2, "Bottom-Left", Color(0xFFC7E294)),
            DragJigsawPiece(3, "Bottom-Right", Color(0xFFF7F4D5))
        )
    }

    var pieces by remember { mutableStateOf(defaultPieces.map { it.copy() }) }
    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }
    var draggedPieceId by remember { mutableStateOf<Int?>(null) }

    // Board slot position bounds on root screen
    var boardBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    fun resetPuzzle() {
        pieces = defaultPieces.map { it.copy() }
        p1Score = 0
        p2Score = 0
        isPlayer1Turn = true
        showWinnerDialog = false
        rewardEarned = false
        draggedPieceId = null
        SoundManager.playRetrySound()
    }

    // Check if all pieces are snapped into place
    fun checkSolved() {
        if (pieces.all { it.isSnapped }) {
            SoundManager.playSuccessChime()
            if (!rewardEarned) {
                rewardEarned = true
                authViewModel.addAdReward(context)
                Toast.makeText(context, "🪙 +50 Coins Earned for Jigsaw Puzzle!", Toast.LENGTH_SHORT).show()
            }
            showWinnerDialog = true
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
                        text = "JIGSAW PUZZLE 2P",
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

            // ── PLAYER 1 CARD ATTACHED TO LEFT EDGE (MEMORY MATCH EXACT SIZE) ──
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

            // ── MAIN INTERLOCKING PUZZLE TARGET BOARD (CENTER SCREEN) ────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 24.dp)
                    .onGloballyPositioned { coords ->
                        boardBounds = coords.boundsInRoot()
                    }
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A), RoundedCornerShape(24.dp))
                    .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Guidelines Canvas for 2x2 Interlocking Puzzle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val halfW = w / 2f
                    val halfH = h / 2f

                    // Draw 4 Slot Dotted Outlines
                    val stroke = Stroke(width = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))

                    drawRect(color = Color.White.copy(alpha = 0.15f), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(halfW, halfH), style = stroke)
                    drawRect(color = Color.White.copy(alpha = 0.15f), topLeft = Offset(halfW, 0f), size = androidx.compose.ui.geometry.Size(halfW, halfH), style = stroke)
                    drawRect(color = Color.White.copy(alpha = 0.15f), topLeft = Offset(0f, halfH), size = androidx.compose.ui.geometry.Size(halfW, halfH), style = stroke)
                    drawRect(color = Color.White.copy(alpha = 0.15f), topLeft = Offset(halfW, halfH), size = androidx.compose.ui.geometry.Size(halfW, halfH), style = stroke)
                }

                // Render Snapped Pieces inside Board
                for (p in pieces) {
                    if (p.isSnapped) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = when (p.id) {
                                0 -> Alignment.TopStart
                                1 -> Alignment.TopEnd
                                2 -> Alignment.BottomStart
                                else -> Alignment.BottomEnd
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.5f)
                                    .padding(4.dp)
                            ) {
                                InterlockingJigsawPieceCanvas(piece = p)
                            }
                        }
                    }
                }
            }

            // ── DRAGGABLE PUZZLE PIECES CONTAINER (BOTTOM TRAY) ──────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (p in pieces) {
                    if (!p.isSnapped) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .offset {
                                    IntOffset(
                                        p.currentOffset.x.roundToInt(),
                                        p.currentOffset.y.roundToInt()
                                    )
                                }
                                .pointerInput(p.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedPieceId = p.id
                                            SoundManager.playPopSound()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            pieces = pieces.map { item ->
                                                if (item.id == p.id) {
                                                    item.copy(currentOffset = item.currentOffset + dragAmount)
                                                } else item
                                            }
                                        },
                                        onDragEnd = {
                                            draggedPieceId = null
                                            // Check drop target proximity (if dragged upward toward puzzle board)
                                            if (p.currentOffset.y < -150f) {
                                                SoundManager.playPopSound()
                                                pieces = pieces.map { item ->
                                                    if (item.id == p.id) item.copy(isSnapped = true) else item
                                                }
                                                if (isPlayer1Turn) p1Score += 10 else p2Score += 10
                                                isPlayer1Turn = !isPlayer1Turn
                                                checkSolved()
                                            } else {
                                                // Reset offset back to tray if missed
                                                pieces = pieces.map { item ->
                                                    if (item.id == p.id) item.copy(currentOffset = Offset.Zero) else item
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            InterlockingJigsawPieceCanvas(piece = p)
                        }
                    }
                }
            }

            // ── PLAYER 2 CARD ATTACHED TO RIGHT EDGE (MEMORY MATCH EXACT SIZE) ──
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

            Spacer(modifier = Modifier.height(16.dp))
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
                                Text("🧩", fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (p1Won) "🎉 Player 1 Solved It!" else if (p2Won) "🎉 Player 2 Solved It!" else "🧩 Puzzle Solved!",
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

// ── CUSTOM INTERLOCKING JIGSAW PIECE CANVAS DRAWING (MATCHING USER SCREENSHOT) ──
@Composable
fun InterlockingJigsawPieceCanvas(piece: DragJigsawPiece) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val path = Path()

        val tabRadius = w * 0.16f

        // Draw Interlocking Jigsaw Piece Paths with Knob Tabs & Socket Holes
        when (piece.id) {
            0 -> {
                // Top-Left Piece (Lavender Purple)
                path.moveTo(0f, 0f)
                path.lineTo(w, 0f)
                // Right Edge: Socket Hole dipping left
                path.lineTo(w, h * 0.35f)
                path.cubicTo(w - tabRadius, h * 0.35f, w - tabRadius, h * 0.65f, w, h * 0.65f)
                path.lineTo(w, h)
                // Bottom Edge: Knob Tab sticking down
                path.lineTo(w * 0.65f, h)
                path.cubicTo(w * 0.65f, h + tabRadius, w * 0.35f, h + tabRadius, w * 0.35f, h)
                path.lineTo(0f, h)
                path.close()
            }
            1 -> {
                // Top-Right Piece (Warm Orange)
                path.moveTo(0f, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w, h)
                // Bottom Edge: Socket Hole dipping up
                path.lineTo(w * 0.65f, h)
                path.cubicTo(w * 0.65f, h - tabRadius, w * 0.35f, h - tabRadius, w * 0.35f, h)
                path.lineTo(0f, h)
                // Left Edge: Knob Tab sticking left
                path.lineTo(0f, h * 0.65f)
                path.cubicTo(-tabRadius, h * 0.65f, -tabRadius, h * 0.35f, 0f, h * 0.35f)
                path.close()
            }
            2 -> {
                // Bottom-Left Piece (Soft Pastel Green)
                path.moveTo(0f, 0f)
                // Top Edge: Socket Hole dipping down
                path.lineTo(w * 0.35f, 0f)
                path.cubicTo(w * 0.35f, tabRadius, w * 0.65f, tabRadius, w * 0.65f, 0f)
                path.lineTo(w, 0f)
                // Right Edge: Knob Tab sticking right
                path.lineTo(w, h * 0.35f)
                path.cubicTo(w + tabRadius, h * 0.35f, w + tabRadius, h * 0.65f, w, h * 0.65f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
                path.close()
            }
            3 -> {
                // Bottom-Right Piece (Cream Yellow)
                path.moveTo(0f, 0f)
                // Top Edge: Knob Tab sticking up
                path.lineTo(w * 0.35f, 0f)
                path.cubicTo(w * 0.35f, -tabRadius, w * 0.65f, -tabRadius, w * 0.65f, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
                // Left Edge: Socket Hole dipping right
                path.lineTo(0f, h * 0.65f)
                path.cubicTo(tabRadius, h * 0.65f, tabRadius, h * 0.35f, 0f, h * 0.35f)
                path.close()
            }
        }

        // Fill Piece with Pastel Color
        drawPath(path = path, color = piece.pieceColor)

        // Draw Dark Outer Border Outline (Matching Screenshot 3D Contour)
        drawPath(path = path, color = piece.outlineColor, style = Stroke(width = 5f))
    }
}
