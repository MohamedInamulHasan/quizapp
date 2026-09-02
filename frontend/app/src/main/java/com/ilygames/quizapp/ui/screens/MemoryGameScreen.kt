package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

// Master Pool of 30+ distinct pre-loaded 3D vector graphic tiles with vibrant jewel gradients
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

    // Re-deal / Retry Flip Animation Trigger State
    var isRestartingAnimation by remember { mutableStateOf(false) }
    var restartTriggerCount by remember { mutableIntStateOf(0) }

    // Animated Card Re-deal Handler
    val coroutineScope = rememberCoroutineScope()
    fun triggerAnimatedRestart() {
        if (isRestartingAnimation) return
        isRestartingAnimation = true
        showWinnerDialog = false

        // Step 1: Flip all cards back first with flip sound effect
        SoundManager.playClickSound()
        cards = cards.map { it.copy(isFlipped = false, isMatched = false) }

        // Step 2: Delay for flip-back transition then generate fresh deck
        kotlinx.coroutines.MainScope().run {
            kotlinx.coroutines.GlobalScope.run {
                // Short pause before dealing new cards
            }
        }

        // Trigger scale re-deal animation
        restartTriggerCount++
        cards = createFreshDeck()
        selectedIndices = emptyList()
        isPlayer1Turn = true
        player1Score = 0
        player2Score = 0
        isProcessingTurn = false
        rewardEarned = false

        // End restart animation state
        isRestartingAnimation = false
    }

    // ── Player Tap Handler (Normal 2-Player Pass & Play) ─────────────────
    fun onCardClick(index: Int) {
        if (isProcessingTurn || isRestartingAnimation) return
        val card = cards[index]
        if (card.isFlipped || card.isMatched) return

        SoundManager.playClickSound()

        // Flip card instantly
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

                // Animated Retry / Re-deal Button
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
                            triggerAnimatedRestart()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color(0xFF255FF4),
                        modifier = Modifier.size(20.dp)
                    )
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

            // ── 4x5 PERFECT SQUARE CARDS GRID WITH RE-DEAL ANIMATION ─────
            key(restartTriggerCount) {
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
                            cardIndex = idx,
                            onClick = { onCardClick(idx) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── EXCITING ANIMATED 3D VICTORY CARD MODAL ──────────────────────
        if (showWinnerDialog) {
            ExcitingVictoryCardModal(
                player1Score = player1Score,
                player2Score = player2Score,
                isDark = isDark,
                onPlayAgain = {
                    triggerAnimatedRestart()
                },
                onBackToHome = {
                    SoundManager.playClickSound()
                    onBack()
                }
            )
        }
    }
}

// ── ULTRA EXCITING 3D VICTORY CARD MODAL COMPONENT ──────────────────────
@Composable
fun ExcitingVictoryCardModal(
    player1Score: Int,
    player2Score: Int,
    isDark: Boolean,
    onPlayAgain: () -> Unit,
    onBackToHome: () -> Unit
) {
    val p1Won = player1Score > player2Score
    val p2Won = player2Score > player1Score
    val isDraw = player1Score == player2Score

    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    // Pulsing Trophy Animation Scale
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
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            else listOf(Color.White, Color(0xFFF1F5F9))
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .border(
                        2.5.dp,
                        Brush.verticalGradient(
                            colors = if (p1Won) listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
                            else if (p2Won) listOf(Color(0xFF255FF4), Color(0xFF06B6D4))
                            else listOf(Color(0xFFF59E0B), Color(0xFFEAB308))
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Floating Animated 3D Trophy Badge
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .scale(trophyScale)
                            .shadow(12.dp, CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (p1Won) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                    else if (p2Won) listOf(Color(0xFF255FF4), Color(0xFF1E40AF))
                                    else listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                ),
                                CircleShape
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (p1Won || p2Won) "🏆" else "🤝",
                            fontSize = 44.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Celebration Headline
                    Text(
                        text = if (p1Won) "🔴 PLAYER 1 WINS!" else if (p2Won) "🔵 PLAYER 2 WINS!" else "🤝 IT'S A DRAW!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "🎉 What an Epic Memory Battle!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Score Card Comparison Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp))
                            .background(
                                if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Player 1
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔴 PLAYER 1", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                            Text("$player1Score", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
                            Text("Matched Pairs", fontSize = 10.sp, color = Color.Gray)
                        }

                        Text("VS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))

                        // Player 2
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔵 PLAYER 2", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF255FF4))
                            Text("$player2Score", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
                            Text("Matched Pairs", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 🪙 Bonus Coin Reward Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙 ", fontSize = 16.sp)
                            Text(
                                "+50 BONUS COINS ADDED TO WALLET!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Excitation Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PLAY AGAIN 3D Pill Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                onPlayAgain()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        // BACK TO HOME Button
                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                        ) {
                            Text("BACK TO HOME", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            }
        }
    }
}

// ── PERFECT SQUARE MEMORY CARD TILE WITH STAGGERED RE-DEAL ANIMATION ────────
@Composable
fun SquareMemoryCardTile(
    card: TwoPlayerCard,
    isDark: Boolean,
    cardIndex: Int,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "SquareFlipAnimation"
    )

    // Staggered Re-deal Entrance Scale Animation
    var isDealt by remember { mutableStateOf(false) }
    LaunchedEffect(card.id) {
        delay(cardIndex * 35L) // Staggered deal delay
        isDealt = true
    }

    val dealScale by animateFloatAsState(
        targetValue = if (isDealt) 1.0f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DealScale"
    )

    val isFrontVisible = rotation > 90f

    // Unflipped 3D Soft-Clay Amber Gold vs Matched Radiant Jewel Gradient vs Flipped Dynamic Tile Gradient
    val unflippedBg = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))) // Glossy 3D Gold

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Perfect Square Shape
            .scale(dealScale)
            .shadow(if (card.isMatched) 3.dp else 7.dp, RoundedCornerShape(16.dp))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFrontVisible) Brush.linearGradient(card.graphic.bgGradient)
                else unflippedBg
            )
            .border(
                1.8.dp,
                if (card.isMatched) Color.White.copy(alpha = 0.95f)
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
                // Instant pre-loaded vector icon rendering with zero lag!
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
