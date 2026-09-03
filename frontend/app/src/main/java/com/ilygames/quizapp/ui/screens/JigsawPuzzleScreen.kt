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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlin.math.roundToInt
import kotlin.random.Random

// 5 Real High-Res Photography Images
data class RealJigsawImage(
    val title: String,
    val imageUrl: String,
    val fallbackEmoji: String
)

val REAL_JIGSAW_IMAGES = listOf(
    RealJigsawImage("Golden Puppy", "https://images.unsplash.com/photo-1543466835-00a7907e9de1?q=80&w=800&auto=format&fit=crop", "🐶"),
    RealJigsawImage("Safari Lion", "https://images.unsplash.com/photo-1534188753412-3e26d0d618d6?q=80&w=800&auto=format&fit=crop", "🦁"),
    RealJigsawImage("Tropical Beach", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800&auto=format&fit=crop", "🏖️"),
    RealJigsawImage("Cosmic Nebula", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=800&auto=format&fit=crop", "🌌"),
    RealJigsawImage("Taj Mahal", "https://images.unsplash.com/photo-1564507592333-c60657eea523?q=80&w=800&auto=format&fit=crop", "🌆")
)

@Composable
fun JigsawPuzzleScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // Random Real Image Theme Selection (4 Rows x 5 Columns = 20 Pieces)
    var currentImageIndex by remember { mutableIntStateOf(Random.nextInt(REAL_JIGSAW_IMAGES.size)) }
    val currentImage = REAL_JIGSAW_IMAGES[currentImageIndex]

    // 20 Target Slots on 4x5 Grid Board (array holds pieceId 0..19 or null)
    var boardSlots by remember { mutableStateOf(Array<Int?>(20) { null }) }
    // Unplaced Pieces in Tray (Shuffled 0..19)
    var trayPieceIds by remember { mutableStateOf((0..19).shuffled()) }

    // Active Dragging State
    var draggingPieceId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Board Position Bounds on Screen for Drag & Drop calculation
    var boardBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    fun resetPuzzle() {
        // Pick a NEW Real Image on every play!
        currentImageIndex = (currentImageIndex + 1) % REAL_JIGSAW_IMAGES.size
        boardSlots = Array(20) { null }
        trayPieceIds = (0..19).shuffled()
        draggingPieceId = null
        dragOffset = Offset.Zero
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
                Toast.makeText(context, "🪙 +50 Coins Earned for 20-Piece Real Jigsaw!", Toast.LENGTH_SHORT).show()
            }
            showWinnerDialog = true
        }
    }

    fun placePieceInSlot(pieceId: Int, slotIndex: Int) {
        if (slotIndex !in 0..19) return
        if (boardSlots[slotIndex] != null) return // Slot occupied

        SoundManager.playPopSound()
        val newSlots = boardSlots.clone()
        newSlots[slotIndex] = pieceId
        boardSlots = newSlots
        trayPieceIds = trayPieceIds.filter { it != pieceId }

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

                // Image Title Badge Pill
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
                        text = "${currentImage.fallbackEmoji} ${currentImage.title} (20P)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // 3D Retry Button (Changes Image on Retry!)
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
                    Icon(Icons.Default.Refresh, contentDescription = "Next Picture", tint = textColor, modifier = Modifier.size(22.dp))
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

            // ── 20-PIECE JIGSAW PUZZLE BOARD (4 ROWS x 5 COLUMNS) ───────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp)
                    .onGloballyPositioned { coords ->
                        boardBounds = coords.boundsInRoot()
                    }
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xFF3D2314), RoundedCornerShape(20.dp)) // Dark Wooden Cutout Frame
                    .border(3.dp, Color(0xFF78350F), RoundedCornerShape(20.dp))
                    .padding(6.dp),
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
                            for (c in 0..4) {
                                val slotIndex = r * 5 + c
                                val pieceId = boardSlots[slotIndex]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (pieceId != null) Color.Transparent
                                            else Color(0xFF5A3816), // Dark Wooden Slot Cutout Texture
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF2C190B),
                                            RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (pieceId != null) {
                                        RealJigsawPieceTile(
                                            pieceId = pieceId,
                                            imageUrl = currentImage.imageUrl
                                        )
                                    } else {
                                        Text(
                                            text = "${slotIndex + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 20 PIECES TRAY (DRAG & DROP ONLY MODE) ─────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(140.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🖐️ Drag any piece from tray to drop on the board:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(10),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(trayPieceIds) { pieceId ->
                        var localDragOffset by remember { mutableStateOf(Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .offset {
                                    IntOffset(
                                        localDragOffset.x.roundToInt(),
                                        localDragOffset.y.roundToInt()
                                    )
                                }
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .pointerInput(pieceId) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingPieceId = pieceId
                                            SoundManager.playPopSound()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            localDragOffset += dragAmount
                                            dragOffset += dragAmount
                                        },
                                        onDragEnd = {
                                            // Calculate drop position relative to board
                                            if (localDragOffset.y < -100f) {
                                                val colWidth = boardBounds.width / 5f
                                                val rowHeight = boardBounds.height / 4f

                                                val dropX = localDragOffset.x + boardBounds.width / 2f
                                                val dropY = localDragOffset.y + boardBounds.height

                                                val c = (dropX / colWidth).coerceIn(0f, 4f).toInt()
                                                val r = (dropY / rowHeight).coerceIn(0f, 3f).toInt()
                                                val slotIndex = (r * 5 + c).coerceIn(0, 19)

                                                placePieceInSlot(pieceId, slotIndex)
                                            }
                                            localDragOffset = Offset.Zero
                                            draggingPieceId = null
                                        }
                                    )
                                }
                        ) {
                            RealJigsawPieceTile(
                                pieceId = pieceId,
                                imageUrl = currentImage.imageUrl
                            )
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
                                Text(currentImage.fallbackEmoji, fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (p1Won) "🎉 Player 1 Solved It!" else if (p2Won) "🎉 Player 2 Solved It!" else "🧩 ${currentImage.title} Solved!",
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

// ── REAL PHOTOGRAPHY 20-PIECE JIGSAW TILE (4 ROWS x 5 COLUMNS) ──────────────
@Composable
fun RealJigsawPieceTile(pieceId: Int, imageUrl: String) {
    val pieceRow = pieceId / 5
    val pieceCol = pieceId % 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Load Real High-Res Photography Picture via Coil
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Jigsaw Piece",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Draw Black 4x5 Interlocking Jigsaw Piece Outlines Over Image
        Canvas(modifier = Modifier.fillMaxSize()) {
            draw20PieceJigsawInterlockingContour(pieceRow = pieceRow, pieceCol = pieceCol, w = size.width, h = size.height)
        }
    }
}

// Draw Black 4x5 Interlocking Jigsaw Edge Contour (Matching User Screenshot metabetageek.com pattern)
fun androidx.compose.ui.graphics.drawscope.DrawScope.draw20PieceJigsawInterlockingContour(pieceRow: Int, pieceCol: Int, w: Float, h: Float) {
    val strokeWidth = 3f
    val contourColor = Color.Black.copy(alpha = 0.85f)
    val tabR = w * 0.22f

    val path = Path()
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
    if (pieceCol < 4) {
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
