package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.delay

// Built-in vector graphic structure (100% pre-loaded locally with ZERO network loading lag)
data class TileGraphic(
    val key: String,
    val title: String,
    val iconVector: ImageVector,
    val bgGradient: List<Color>
)

// Master Pool of 30+ distinct pre-loaded 3D vector graphic tiles so every round picks NEW unused items!
val MASTER_TILE_POOL = listOf(
    TileGraphic("rocket", "Rocket", Icons.Default.RocketLaunch, listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
    TileGraphic("gamepad", "Gamepad", Icons.Default.SportsEsports, listOf(Color(0xFFEC4899), Color(0xFFBE185D))),
    TileGraphic("diamond", "Diamond", Icons.Default.Diamond, listOf(Color(0xFF06B6D4), Color(0xFF0E7490))),
    TileGraphic("soccer", "Soccer", Icons.Default.SportsSoccer, listOf(Color(0xFF10B981), Color(0xFF047857))),
    TileGraphic("lightning", "Flash", Icons.Default.FlashOn, listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
    TileGraphic("trophy", "Trophy", Icons.Default.EmojiEvents, listOf(Color(0xFFEAB308), Color(0xFFA16207))),
    TileGraphic("car", "Car", Icons.Default.DirectionsCar, listOf(Color(0xFFEF4444), Color(0xFFB91C1C))),
    TileGraphic("pizza", "Pizza", Icons.Default.LocalPizza, listOf(Color(0xFFF97316), Color(0xFFC2410C))),
    TileGraphic("star", "Star", Icons.Default.AutoAwesome, listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
    TileGraphic("shield", "Shield", Icons.Default.Shield, listOf(Color(0xFF6366F1), Color(0xFF4338CA))),
    TileGraphic("basketball", "Basketball", Icons.Default.SportsBasketball, listOf(Color(0xFFEA580C), Color(0xFF9A3412))),
    TileGraphic("plane", "Plane", Icons.Default.Flight, listOf(Color(0xFF0EA5E9), Color(0xFF0369A1))),
    TileGraphic("music", "Music", Icons.Default.MusicNote, listOf(Color(0xFFA855F7), Color(0xFF7E22CE))),
    TileGraphic("camera", "Camera", Icons.Default.CameraAlt, listOf(Color(0xFF64748B), Color(0xFF334155))),
    TileGraphic("sun", "Sun", Icons.Default.WbSunny, listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
    TileGraphic("moon", "Moon", Icons.Default.Bedtime, listOf(Color(0xFF1E1B4B), Color(0xFF312E81))),
    TileGraphic("palette", "Palette", Icons.Default.Palette, listOf(Color(0xFFD946EF), Color(0xFFA21CAF))),
    TileGraphic("lightbulb", "Light", Icons.Default.Lightbulb, listOf(Color(0xFFFACC15), Color(0xFFCA8A04))),
    TileGraphic("headset", "Headset", Icons.Default.Headset, listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
    TileGraphic("brain", "Brain", Icons.Default.Psychology, listOf(Color(0xFFF43F5E), Color(0xFFBE123C))),
    TileGraphic("lock", "Key", Icons.Default.VpnKey, listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
    TileGraphic("favorite", "Heart", Icons.Default.Favorite, listOf(Color(0xFFE11D48), Color(0xFF9F1239))),
    TileGraphic("casino", "Dice", Icons.Default.Casino, listOf(Color(0xFF84CC16), Color(0xFF4D7C0F))),
    TileGraphic("extension", "Puzzle", Icons.Default.Extension, listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
    TileGraphic("shopping", "Bag", Icons.Default.ShoppingBag, listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))),
    TileGraphic("pet", "Paw", Icons.Default.Pets, listOf(Color(0xFFD97706), Color(0xFF92400E))),
    TileGraphic("fastfood", "Burger", Icons.Default.Fastfood, listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
    TileGraphic("park", "Tree", Icons.Default.Park, listOf(Color(0xFF16A34A), Color(0xFF15803D))),
    TileGraphic("anchor", "Anchor", Icons.Default.Anchor, listOf(Color(0xFF0284C7), Color(0xFF075985))),
    TileGraphic("celebration", "Party", Icons.Default.Celebration, listOf(Color(0xFFF43F5E), Color(0xFF9F1239)))
)

// Global set to track used indices across rounds so every round picks NEW unused items
private val usedImageIndices = mutableSetOf<Int>()

data class TwoPlayerCard(
    val id: Int,
    val graphic: TileGraphic,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

@Composable
fun MemoryGameScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val isDark = ThemeState.isDarkMode
    val context = LocalContext.current

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFEBF3FE)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    // Select 10 fresh, unused graphics for each new match
    fun select10FreshGraphics(): List<TileGraphic> {
        val totalAvailable = MASTER_TILE_POOL.size
        val availableIndices = (0 until totalAvailable).filter { !usedImageIndices.contains(it) }

        val selectedIndices = if (availableIndices.size >= 10) {
            availableIndices.shuffled().take(10)
        } else {
            usedImageIndices.clear()
            (0 until totalAvailable).shuffled().take(10)
        }

        usedImageIndices.addAll(selectedIndices)
        return selectedIndices.map { MASTER_TILE_POOL[it] }
    }

    fun createFreshDeck(): List<TwoPlayerCard> {
        val freshGraphics = select10FreshGraphics()
        val pairs = (freshGraphics + freshGraphics).shuffled()
        return pairs.mapIndexed { idx, graphic ->
            TwoPlayerCard(id = idx, graphic = graphic)
        }
    }

    // State Variables for Normal 2-Player Pass & Play (Player 1 vs Player 2)
    var cards by remember { mutableStateOf(createFreshDeck()) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isPlayer1Turn by remember { mutableStateOf(true) } // true = Player 1, false = Player 2
    var player1Score by remember { mutableIntStateOf(0) }
    var player2Score by remember { mutableIntStateOf(0) }
    var isProcessingTurn by remember { mutableStateOf(false) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    fun restartGame() {
        cards = createFreshDeck()
        selectedIndices = emptyList()
        isPlayer1Turn = true
        player1Score = 0
        player2Score = 0
        isProcessingTurn = false
        showWinnerDialog = false
        rewardEarned = false
    }

    // ── Player Tap Handler (Normal 2-Player Pass & Play) ─────────────────
    fun onCardClick(index: Int) {
        if (isProcessingTurn) return
        val card = cards[index]
        if (card.isFlipped || card.isMatched) return

        SoundManager.playClickSound()

        // Flip card instantly with zero network delay
        cards = cards.toMutableList().also {
            it[index] = it[index].copy(isFlipped = true)
        }

        val newSelected = selectedIndices + index
        selectedIndices = newSelected

        if (newSelected.size == 2) {
            isProcessingTurn = true
            val firstIdx = newSelected[0]
            val secondIdx = newSelected[1]

            if (cards[firstIdx].graphic.key == cards[secondIdx].graphic.key) {
                // Match Found!
                SoundManager.playCorrectSound()
                cards = cards.toMutableList().also {
                    it[firstIdx] = it[firstIdx].copy(isMatched = true)
                    it[secondIdx] = it[secondIdx].copy(isMatched = true)
                }

                if (isPlayer1Turn) {
                    player1Score++
                } else {
                    player2Score++
                }

                selectedIndices = emptyList()
                isProcessingTurn = false

                // Check Game Over
                if (player1Score + player2Score == 10) {
                    if (!rewardEarned) {
                        rewardEarned = true
                        authViewModel.addAdReward(context)
                        Toast.makeText(context, "🪙 +50 Coins Earned for Memory Match!", Toast.LENGTH_SHORT).show()
                    }
                    showWinnerDialog = true
                }
                // Matcher gets another turn!
            } else {
                // No Match ➔ flip back after 800ms and switch turn!
                SoundManager.playWrongSound()
            }
        }
    }

    // Delayed flip back for non-matching pair & turn switch
    LaunchedEffect(selectedIndices, isProcessingTurn) {
        if (selectedIndices.size == 2 && isProcessingTurn) {
            val firstIdx = selectedIndices[0]
            val secondIdx = selectedIndices[1]
            if (cards[firstIdx].graphic.key != cards[secondIdx].graphic.key) {
                delay(800)
                cards = cards.toMutableList().also {
                    it[firstIdx] = it[firstIdx].copy(isFlipped = false)
                    it[secondIdx] = it[secondIdx].copy(isFlipped = false)
                }
                selectedIndices = emptyList()
                isProcessingTurn = false
                isPlayer1Turn = !isPlayer1Turn // Switch turn between Player 1 & Player 2!
            }
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

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
                    text = "Memory Match 2P",
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

            Spacer(modifier = Modifier.height(16.dp))

            // ── Scoreboard: PLAYER 1 vs PLAYER 2 ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PLAYER 1 Score Pill (Red/Coral)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isPlayer1Turn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            2.dp,
                            if (isPlayer1Turn) Color.White else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔴 PLAYER 1", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$player1Score", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                // PLAYER 2 Score Pill (Blue/Indigo)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isPlayer1Turn) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            2.dp,
                            if (!isPlayer1Turn) Color.White else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔵 PLAYER 2", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$player2Score", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Active Turn Status Banner ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .background(
                        if (isPlayer1Turn) Color(0xFFEF4444).copy(alpha = 0.15f)
                        else Color(0xFF255FF4).copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        if (isPlayer1Turn) Color(0xFFEF4444) else Color(0xFF255FF4),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlayer1Turn) "🔴 PLAYER 1'S TURN - TAP 2 TILES" else "🔵 PLAYER 2'S TURN - TAP 2 TILES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPlayer1Turn) Color(0xFFEF4444) else Color(0xFF255FF4)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 4x5 PERFECT SQUARE CARDS GRID (NO OUTER CARD CONTAINER WRAPPER) ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(cards) { idx, card ->
                    SquareMemoryCardTile(
                        card = card,
                        isDark = isDark,
                        onClick = { onCardClick(idx) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Winner Dialog Modal ─────────────────────────────────────────
        if (showWinnerDialog) {
            val p1Won = player1Score > player2Score
            val p2Won = player2Score > player1Score
            val isDraw = player1Score == player2Score

            AlertDialog(
                onDismissRequest = { showWinnerDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            restartGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("PLAY AGAIN", fontWeight = FontWeight.Black, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            SoundManager.playClickSound()
                            onBack()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("BACK TO HOME", fontWeight = FontWeight.Bold, color = textColor)
                    }
                },
                title = {
                    Text(
                        text = if (p1Won) "🏆 PLAYER 1 WINS!" else if (p2Won) "🏆 PLAYER 2 WINS!" else "🤝 IT'S A DRAW!",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Final Score: 🔴 $player1Score - $player2Score 🔵",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🪙 +50 Bonus Coins Awarded!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFEAB308))
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                titleContentColor = textColor,
                textContentColor = subTextColor
            )
        }
    }
}

// ── PERFECT SQUARE MEMORY CARD TILE (PRE-LOADED VECTORS FOR ZERO FLIP LAG) ──
@Composable
fun SquareMemoryCardTile(
    card: TwoPlayerCard,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "SquareFlipAnimation"
    )

    val isFrontVisible = rotation > 90f

    // Soft-Clay 3D Gold Unflipped vs Emerald Matched vs Pre-loaded Gradient Flipped
    val unflippedBg = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))) // Glossy 3D Gold
    val matchedBg = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))   // Emerald Green

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Perfect Square Shape
            .shadow(if (card.isMatched) 2.dp else 6.dp, RoundedCornerShape(16.dp))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (card.isMatched) matchedBg
                else if (isFrontVisible) Brush.linearGradient(card.graphic.bgGradient)
                else unflippedBg
            )
            .border(
                1.5.dp,
                if (card.isMatched) Color.White.copy(alpha = 0.9f)
                else if (isFrontVisible) Color.White.copy(alpha = 0.8f)
                else Color.White.copy(alpha = 0.8f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !card.isFlipped && !card.isMatched, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isFrontVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .graphicsLayer { rotationY = 180f }, // Flip right-side up
                contentAlignment = Alignment.Center
            ) {
                // Instant pre-loaded vector icon rendering (ZERO network delay!)
                Icon(
                    imageVector = card.graphic.iconVector,
                    contentDescription = card.graphic.title,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        } else {
            // Unflipped tile pattern icon
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
