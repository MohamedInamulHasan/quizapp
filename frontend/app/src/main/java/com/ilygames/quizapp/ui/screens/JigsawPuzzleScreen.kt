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

// 16-Piece Jigsaw Piece Data Model (4x4 Grid)
data class ImageJigsawPiece(
    val id: Int, // 0 to 15
    val correctRow: Int, // 0 to 3
    val correctCol: Int, // 0 to 3
    val emoji: String,
    val pieceColor: Color
)

@Composable
fun JigsawPuzzleScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // Vibrant 16 Jigsaw Pieces (4x4 Grid)
    val allPieces = remember {
        val colors = listOf(
            Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4),
            Color(0xFF255FF4), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E),
            Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6),
            Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFFEF4444), Color(0xFFFFD700)
        )
        val emojis = listOf(
            "🚀", "⚡", "⭐", "💎",
            "🏆", "🔥", "🎨", "🎮",
            "👑", "🌈", "🦁", "🦄",
            "🧩", "🔮", "🛸", "🎯"
        )
        List(16) { index ->
            ImageJigsawPiece(
                id = index,
                correctRow = index / 4,
                correctCol = index % 4,
                emoji = emojis[index],
                pieceColor = colors[index]
            )
        }
    }

    // State for board slots: array of 16 (holds pieceId or null)
    var boardSlots by remember { mutableStateOf(Array<Int?>(16) { null }) }
    // State for tray pieces (unplaced piece IDs)
    var trayPieceIds by remember { mutableStateOf(allPieces.map { it.id }.shuffled()) }
    // Currently selected piece for tap/drag placement
    var selectedPieceId by remember { mutableStateOf<Int?>(null) }

    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Board position bounds for exact drop detection
    var boardBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    fun resetPuzzle() {
        boardSlots = Array(16) { null }
        trayPieceIds = allPieces.map { it.id }.shuffled()
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
                Toast.makeText(context, "🪙 +50 Coins Earned for 16-Piece Jigsaw!", Toast.LENGTH_SHORT).show()
            }
            showWinnerDialog = true
        }
    }

    fun placePieceInSlot(pieceId: Int, slotIndex: Int) {
        if (boardSlots[slotIndex] != null) return // Slot already occupied

        SoundManager.playPopSound()
        val newSlots = boardSlots.clone()
        newSlots[slotIndex] = pieceId
        boardSlots = newSlots
        trayPieceIds = trayPieceIds.filter { it != pieceId }
        selectedPieceId = null

        // Bonus score if placed in correct slot!
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
            // ── TOP HEADER BAR ─────────────────────────────────────────────
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
                        text = "JIGSAW 16 PIECES",
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

            // ── 4x4 INTERLOCKING JIGSAW TARGET BOARD (16 SLOTS) ───────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 20.dp)
                    .onGloballyPositioned { coords ->
                        boardBounds = coords.boundsInRoot()
                    }
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                    .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (r in 0..3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (c in 0..3) {
                                val slotIndex = r * 4 + c
                                val placedPieceId = boardSlots[slotIndex]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (placedPieceId != null) Color.Transparent
                                            else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedPieceId != null && placedPieceId == null) Color.Yellow
                                            else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPieceId?.let { pId ->
                                                placePieceInSlot(pId, slotIndex)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (placedPieceId != null) {
                                        val piece = allPieces[placedPieceId]
                                        JigsawTileCard(piece = piece, isSelected = false)
                                    } else {
                                        Text(
                                            text = "${slotIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 16 PIECES TRAY (TAP OR DRAG PIECE TO BOARD) ──────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(130.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (selectedPieceId != null) "✨ Tap any target slot to drop piece!" else "👇 Tap or Drag a piece to place on board:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(trayPieceIds) { pieceId ->
                        val piece = allPieces[pieceId]
                        val isSelected = selectedPieceId == pieceId

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    SoundManager.playPopSound()
                                    selectedPieceId = if (isSelected) null else pieceId
                                }
                        ) {
                            JigsawTileCard(piece = piece, isSelected = isSelected)
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

            Spacer(modifier = Modifier.height(12.dp))
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

// ── 16-PIECE INTERLOCKING JIGSAW TILE CARD ────────────────────────────────
@Composable
fun JigsawTileCard(piece: ImageJigsawPiece, isSelected: Boolean) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "TileScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scaleAnim)
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(listOf(piece.pieceColor, piece.pieceColor.copy(alpha = 0.85f))),
                RoundedCornerShape(10.dp)
            )
            .border(
                if (isSelected) 3.dp else 1.dp,
                if (isSelected) Color.Yellow else Color.White.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = piece.emoji, fontSize = 20.sp)
            Text(
                text = "${piece.id + 1}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
