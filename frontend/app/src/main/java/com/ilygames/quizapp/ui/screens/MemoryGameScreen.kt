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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.delay
import kotlin.random.Random

// Sealed structure for card image items (Supports both high-quality remote URLs and fallback vector icons)
data class TileGraphic(
    val key: String,
    val imageUrl: String? = null,
    val iconVector: ImageVector? = null,
    val bgGradient: List<Color> = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
)

// Master Pool of 50+ diverse images & vector graphic tiles so every round picks NEW unused items!
val MASTER_TILE_POOL = listOf(
    TileGraphic("space_rocket", "https://images.unsplash.com/photo-1517976487492-5750f3195933?w=300&auto=format&fit=crop&q=80", Icons.Default.RocketLaunch, listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
    TileGraphic("gamepad", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=300&auto=format&fit=crop&q=80", Icons.Default.SportsEsports, listOf(Color(0xFFEC4899), Color(0xFFBE185D))),
    TileGraphic("diamond", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop&q=80", Icons.Default.Diamond, listOf(Color(0xFF06B6D4), Color(0xFF0E7490))),
    TileGraphic("soccer", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=300&auto=format&fit=crop&q=80", Icons.Default.SportsSoccer, listOf(Color(0xFF10B981), Color(0xFF047857))),
    TileGraphic("lightning", "https://images.unsplash.com/photo-1508921912186-1d1a45ebb3c1?w=300&auto=format&fit=crop&q=80", Icons.Default.FlashOn, listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
    TileGraphic("trophy", "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=300&auto=format&fit=crop&q=80", Icons.Default.EmojiEvents, listOf(Color(0xFFEAB308), Color(0xFFA16207))),
    TileGraphic("car", "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=300&auto=format&fit=crop&q=80", Icons.Default.DirectionsCar, listOf(Color(0xFFEF4444), Color(0xFFB91C1C))),
    TileGraphic("pizza", "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=300&auto=format&fit=crop&q=80", Icons.Default.LocalPizza, listOf(Color(0xFFF97316), Color(0xFFC2410C))),
    TileGraphic("star", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&auto=format&fit=crop&q=80", Icons.Default.AutoAwesome, listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
    TileGraphic("shield", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300&auto=format&fit=crop&q=80", Icons.Default.Shield, listOf(Color(0xFF6366F1), Color(0xFF4338CA))),
    TileGraphic("basketball", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=300&auto=format&fit=crop&q=80", Icons.Default.SportsBasketball, listOf(Color(0xFFEA580C), Color(0xFF9A3412))),
    TileGraphic("plane", "https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=300&auto=format&fit=crop&q=80", Icons.Default.Flight, listOf(Color(0xFF0EA5E9), Color(0xFF0369A1))),
    TileGraphic("music", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&auto=format&fit=crop&q=80", Icons.Default.MusicNote, listOf(Color(0xFFA855F7), Color(0xFF7E22CE))),
    TileGraphic("camera", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=300&auto=format&fit=crop&q=80", Icons.Default.CameraAlt, listOf(Color(0xFF64748B), Color(0xFF334155))),
    TileGraphic("sun", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop&q=80", Icons.Default.WbSunny, listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
    TileGraphic("moon", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=300&auto=format&fit=crop&q=80", Icons.Default.Bedtime, listOf(Color(0xFF1E1B4B), Color(0xFF312E81))),
    TileGraphic("palette", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=300&auto=format&fit=crop&q=80", Icons.Default.Palette, listOf(Color(0xFFD946EF), Color(0xFFA21CAF))),
    TileGraphic("lightbulb", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=300&auto=format&fit=crop&q=80", Icons.Default.Lightbulb, listOf(Color(0xFFFACC15), Color(0xFFCA8A04))),
    TileGraphic("headset", "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=300&auto=format&fit=crop&q=80", Icons.Default.Headset, listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
    TileGraphic("brain", "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=300&auto=format&fit=crop&q=80", Icons.Default.Psychology, listOf(Color(0xFFF43F5E), Color(0xFFBE123C)))
)

// Global set to track used image indices across rounds so every play uses fresh images
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

    // Function to select 10 distinct, previously unused images for every new match
    fun select10FreshGraphics(): List<TileGraphic> {
        val totalAvailable = MASTER_TILE_POOL.size
        val availableIndices = (0 until totalAvailable).filter { !usedImageIndices.contains(it) }

        val selectedIndices = if (availableIndices.size >= 10) {
            availableIndices.shuffled().take(10)
        } else {
            // Reset used set if pool exhausted
            usedImageIndices.clear()
            (0 until totalAvailable).shuffled().take(10)
        }

        usedImageIndices.addAll(selectedIndices)
        return selectedIndices.map { MASTER_TILE_POOL[it] }
    }

    fun createFreshDeck(): List<TwoPlayerCard> {
        val 10Graphics = select10FreshGraphics()
        val pairs = (10Graphics + 10Graphics).shuffled()
        return pairs.mapIndexed { idx, graphic ->
            TwoPlayerCard(id = idx, graphic = graphic)
        }
    }

    // State Variables
    var cards by remember { mutableStateOf(createFreshDeck()) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isPlayerTurn by remember { mutableStateOf(true) } // true = You, false = AI Bot
    var playerScore by remember { mutableIntStateOf(0) }
    var aiScore by remember { mutableIntStateOf(0) }
    var isProcessingTurn by remember { mutableStateOf(false) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    // AI Bot Memory (remembers card indices it has seen when flipped)
    val aiMemory = remember { mutableStateMapOf<Int, String>() }

    fun restartGame() {
        cards = createFreshDeck()
        selectedIndices = emptyList()
        isPlayerTurn = true
        playerScore = 0
        aiScore = 0
        isProcessingTurn = false
        showWinnerDialog = false
        rewardEarned = false
        aiMemory.clear()
    }

    // ── Player Tap Handler ───────────────────────────────────────────────
    fun onPlayerCardClick(index: Int) {
        if (!isPlayerTurn || isProcessingTurn) return
        val card = cards[index]
        if (card.isFlipped || card.isMatched) return

        SoundManager.playClickSound()

        // Flip card and remember in AI memory
        cards = cards.toMutableList().also {
            it[index] = it[index].copy(isFlipped = true)
        }
        aiMemory[index] = card.graphic.key

        val newSelected = selectedIndices + index
        selectedIndices = newSelected

        if (newSelected.size == 2) {
            isProcessingTurn = true
            val firstIdx = newSelected[0]
            val secondIdx = newSelected[1]

            if (cards[firstIdx].graphic.key == cards[secondIdx].graphic.key) {
                // Match!
                SoundManager.playCorrectSound()
                cards = cards.toMutableList().also {
                    it[firstIdx] = it[firstIdx].copy(isMatched = true)
                    it[secondIdx] = it[secondIdx].copy(isMatched = true)
                }
                playerScore++
                selectedIndices = emptyList()
                isProcessingTurn = false

                // Check game over
                if (playerScore + aiScore == 10) {
                    if (playerScore > aiScore && !rewardEarned) {
                        rewardEarned = true
                        authViewModel.addAdReward(context)
                        Toast.makeText(context, "🏆 You beat the AI Bot! +50 Coins Earned!", Toast.LENGTH_SHORT).show()
                    }
                    showWinnerDialog = true
                }
                // Player matched, gets another turn!
            } else {
                // No match ➔ flip back & switch turn to AI Bot
                SoundManager.playWrongSound()
            }
        }
    }

    // ── Delayed Flip Back & Turn Switch for Player ──────────────────────
    LaunchedEffect(selectedIndices, isProcessingTurn, isPlayerTurn) {
        if (isPlayerTurn && selectedIndices.size == 2 && isProcessingTurn) {
            val firstIdx = selectedIndices[0]
            val secondIdx = selectedIndices[1]
            if (cards[firstIdx].graphic.key != cards[secondIdx].graphic.key) {
                delay(850)
                cards = cards.toMutableList().also {
                    it[firstIdx] = it[firstIdx].copy(isFlipped = false)
                    it[secondIdx] = it[secondIdx].copy(isFlipped = false)
                }
                selectedIndices = emptyList()
                isProcessingTurn = false
                isPlayerTurn = false // Switch turn to AI Bot!
            }
        }
    }

    // ── AI Bot Automator ────────────────────────────────────────────────
    LaunchedEffect(isPlayerTurn, isProcessingTurn, playerScore, aiScore) {
        if (!isPlayerTurn && !isProcessingTurn && (playerScore + aiScore < 10)) {
            isProcessingTurn = true
            delay(900) // AI thinking time

            val unrevealedIndices = cards.indices.filter { !cards[it].isMatched && !cards[it].isFlipped }
            if (unrevealedIndices.size < 2) {
                isProcessingTurn = false
                return@LaunchedEffect
            }

            // AI Decision Strategy:
            // 1. Check if AI remembers a matching pair in memory
            var firstPickIdx = -1
            var secondPickIdx = -1

            val memoryGroup = aiMemory.filterKeys { k -> !cards[k].isMatched && !cards[k].isFlipped }
                .entries.groupBy { it.value }

            val knownPair = memoryGroup.values.firstOrNull { it.size >= 2 }
            if (knownPair != null) {
                firstPickIdx = knownPair[0].key
                secondPickIdx = knownPair[1].key
            } else {
                // Pick 1 random unrevealed card
                firstPickIdx = unrevealedIndices.random()
                val firstKey = cards[firstPickIdx].graphic.key

                // Check if its twin is in memory
                val twinInMemory = aiMemory.entries.firstOrNull { it.value == firstKey && it.key != firstPickIdx && !cards[it.key].isMatched && !cards[it.key].isFlipped }
                if (twinInMemory != null && Random.nextFloat() < 0.85f) { // 85% smart memory accuracy
                    secondPickIdx = twinInMemory.key
                } else {
                    val remainingUnrevealed = unrevealedIndices.filter { it != firstPickIdx }
                    secondPickIdx = remainingUnrevealed.random()
                }
            }

            // Flip AI's 1st Pick
            cards = cards.toMutableList().also {
                it[firstPickIdx] = it[firstPickIdx].copy(isFlipped = true)
            }
            aiMemory[firstPickIdx] = cards[firstPickIdx].graphic.key
            SoundManager.playClickSound()

            delay(750) // Short delay before AI flips 2nd card

            // Flip AI's 2nd Pick
            cards = cards.toMutableList().also {
                it[secondPickIdx] = it[secondPickIdx].copy(isFlipped = true)
            }
            aiMemory[secondPickIdx] = cards[secondPickIdx].graphic.key
            SoundManager.playClickSound()

            delay(850)

            // Check AI Match
            if (cards[firstPickIdx].graphic.key == cards[secondPickIdx].graphic.key) {
                SoundManager.playCorrectSound()
                cards = cards.toMutableList().also {
                    it[firstPickIdx] = it[firstPickIdx].copy(isMatched = true)
                    it[secondPickIdx] = it[secondPickIdx].copy(isMatched = true)
                }
                aiScore++
                isProcessingTurn = false

                if (playerScore + aiScore == 10) {
                    showWinnerDialog = true
                }
                // AI matched, stays on AI turn!
            } else {
                SoundManager.playWrongSound()
                // Flip back AI cards
                cards = cards.toMutableList().also {
                    it[firstPickIdx] = it[firstPickIdx].copy(isFlipped = false)
                    it[secondPickIdx] = it[secondPickIdx].copy(isFlipped = false)
                }
                isProcessingTurn = false
                isPlayerTurn = true // Turn returns to YOU!
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

            // ── Scoreboard: YOU vs AI BOT (Clean Score Counters - No Turn/Pair Text) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // YOU Score Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isPlayerTurn) listOf(Color(0xFF10B981), Color(0xFF059669))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            2.dp,
                            if (isPlayerTurn) Color.White else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("👤 YOU", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$playerScore", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                // AI BOT Score Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isPlayerTurn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            2.dp,
                            if (!isPlayerTurn) Color.White else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🤖 AI BOT", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$aiScore", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
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
                        if (isPlayerTurn) Color(0xFF10B981).copy(alpha = 0.15f)
                        else Color(0xFFEF4444).copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        if (isPlayerTurn) Color(0xFF10B981) else Color(0xFFEF4444),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlayerTurn) "🟢 YOUR TURN - TAP TO MATCH" else "🤖 AI BOT IS THINKING...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPlayerTurn) Color(0xFF10B981) else Color(0xFFEF4444)
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
                        onClick = { onPlayerCardClick(idx) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Winner Dialog Modal ─────────────────────────────────────────
        if (showWinnerDialog) {
            val playerWon = playerScore > aiScore
            val isDraw = playerScore == aiScore

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
                        text = if (playerWon) "🏆 YOU WON!" else if (isDraw) "🤝 IT'S A DRAW!" else "🤖 AI BOT WON!",
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
                            text = "Score: YOU $playerScore - $aiScore AI BOT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (playerWon) {
                            Text("🪙 +50 Bonus Coins Added to Wallet!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFEAB308))
                        } else {
                            Text("Better luck next time!", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
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

// ── PERFECT SQUARE MEMORY CARD TILE (WITH DYNAMIC GRAPHIC IMAGES & VECTORS) ──
@Composable
fun SquareMemoryCardTile(
    card: TwoPlayerCard,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "SquareFlipAnimation"
    )

    val isFrontVisible = rotation > 90f

    // Card Colors & Gradients
    val unflippedBg = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))) // Glossy 3D Gold
    val matchedBg = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))   // Emerald Green
    val flippedBg = if (isDark) Color(0xFF1E293B) else Color.White

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Perfect Square Shape!
            .shadow(if (card.isMatched) 2.dp else 6.dp, RoundedCornerShape(16.dp))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (card.isMatched) matchedBg
                else if (isFrontVisible) Brush.verticalGradient(listOf(flippedBg, flippedBg))
                else unflippedBg
            )
            .border(
                1.5.dp,
                if (card.isMatched) Color.White.copy(alpha = 0.9f)
                else if (isFrontVisible) Color(0xFF255FF4).copy(alpha = 0.5f)
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
                if (!card.graphic.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = card.graphic.imageUrl,
                        contentDescription = card.graphic.key,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(card.graphic.bgGradient), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                card.graphic.iconVector?.let { vec ->
                                    Icon(vec, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(card.graphic.bgGradient), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                card.graphic.iconVector?.let { vec ->
                                    Icon(vec, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(card.graphic.bgGradient), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        card.graphic.iconVector?.let { vec ->
                            Icon(vec, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }
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
