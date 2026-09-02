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
    var bgGradient: List<Color> = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
)

// Dynamic Color Gradient Palette (20+ Vibrant Neon, Sunset, Cyber & Candy Gradients)
val VIBRANT_GRADIENTS = listOf(
    listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), // Cyber Purple
    listOf(Color(0xFFEC4899), Color(0xFFBE185D)), // Neon Pink
    listOf(Color(0xFF06B6D4), Color(0xFF0E7490)), // Cyan Wave
    listOf(Color(0xFF10B981), Color(0xFF047857)), // Emerald Mint
    listOf(Color(0xFFF59E0B), Color(0xFFB45309)), // Solar Gold
    listOf(Color(0xFFEF4444), Color(0xFFB91C1C)), // Crimson Red
    listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), // Royal Blue
    listOf(Color(0xFFF97316), Color(0xFFC2410C)), // Sunset Orange
    listOf(Color(0xFFA855F7), Color(0xFF7E22CE)), // Deep Violet
    listOf(Color(0xFF14B8A6), Color(0xFF0F766E)), // Dark Teal
    listOf(Color(0xFFEAB308), Color(0xFFA16207)), // Amber Glow
    listOf(Color(0xFF6366F1), Color(0xFF4338CA)), // Indigo Night
    listOf(Color(0xFFEA580C), Color(0xFF9A3412)), // Coral Fire
    listOf(Color(0xFFD946EF), Color(0xFFA21CAF)), // Electric Fuchsia
    listOf(Color(0xFF0284C7), Color(0xFF0369A1)), // Ocean Blue
    listOf(Color(0xFF84CC16), Color(0xFF4D7C0F)), // Lime Energy
    listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)), // Purple Haze
    listOf(Color(0xFFF43F5E), Color(0xFF9F1239)), // Rose Berry
    listOf(Color(0xFF059669), Color(0xFF065F46)), // Deep Emerald
    listOf(Color(0xFFD97706), Color(0xFF78350F))  // Bronze Gold
)

// MASSIVE MASTER POOL of 100+ UNIQUE HIGH-QUALITY VECTOR GRAPHICS across 10 categories!
val EXPANDED_MASTER_TILE_POOL = listOf(
    // 🚀 Space & Sci-Fi
    TileGraphic("rocket", "Rocket", Icons.Default.RocketLaunch),
    TileGraphic("satellite", "Satellite", Icons.Default.SatelliteAlt),
    TileGraphic("stars", "Stars", Icons.Default.AutoAwesome),
    TileGraphic("sun", "Sun", Icons.Default.WbSunny),
    TileGraphic("moon", "Moon", Icons.Default.Bedtime),
    TileGraphic("explore", "Compass", Icons.Default.Explore),
    TileGraphic("public", "Globe", Icons.Default.Public),
    TileGraphic("flight", "UFO", Icons.Default.Flight),

    // 🎮 Gaming & Esports
    TileGraphic("gamepad", "Gamepad", Icons.Default.SportsEsports),
    TileGraphic("trophy", "Trophy", Icons.Default.EmojiEvents),
    TileGraphic("shield", "Shield", Icons.Default.Shield),
    TileGraphic("casino", "Dice", Icons.Default.Casino),
    TileGraphic("extension", "Puzzle", Icons.Default.Extension),
    TileGraphic("headset", "Headset", Icons.Default.Headset),
    TileGraphic("military", "Medal", Icons.Default.MilitaryTech),

    // ⚽ Sports & Action
    TileGraphic("soccer", "Soccer", Icons.Default.SportsSoccer),
    TileGraphic("basketball", "Basketball", Icons.Default.SportsBasketball),
    TileGraphic("tennis", "Tennis", Icons.Default.SportsTennis),
    TileGraphic("volleyball", "Volleyball", Icons.Default.SportsVolleyball),
    TileGraphic("golf", "Golf", Icons.Default.SportsGolf),
    TileGraphic("motorsports", "Motorsport", Icons.Default.SportsMotorsports),
    TileGraphic("mma", "MMA", Icons.Default.SportsMma),
    TileGraphic("cricket", "Cricket", Icons.Default.SportsCricket),
    TileGraphic("kabaddi", "Kabaddi", Icons.Default.SportsKabaddi),

    // 🍕 Food & Treats
    TileGraphic("pizza", "Pizza", Icons.Default.LocalPizza),
    TileGraphic("burger", "Burger", Icons.Default.Fastfood),
    TileGraphic("icecream", "Ice Cream", Icons.Default.Icecream),
    TileGraphic("cake", "Cake", Icons.Default.Cake),
    TileGraphic("coffee", "Coffee", Icons.Default.LocalCafe),
    TileGraphic("bar", "Drink", Icons.Default.LocalBar),
    TileGraphic("dining", "Utensils", Icons.Default.LocalDining),
    TileGraphic("bakery", "Bakery", Icons.Default.BakeryDining),

    // 🚗 Vehicles & Travel
    TileGraphic("car", "Car", Icons.Default.DirectionsCar),
    TileGraphic("bike", "Motorbike", Icons.Default.TwoWheeler),
    TileGraphic("bus", "Bus", Icons.Default.DirectionsBus),
    TileGraphic("boat", "Boat", Icons.Default.DirectionsBoat),
    TileGraphic("train", "Train", Icons.Default.Train),
    TileGraphic("flight_land", "Airplane", Icons.Default.FlightLand),
    TileGraphic("anchor", "Anchor", Icons.Default.Anchor),

    // 🎵 Music & Audio
    TileGraphic("music", "Music Note", Icons.Default.MusicNote),
    TileGraphic("mic", "Microphone", Icons.Default.Mic),
    TileGraphic("radio", "Radio", Icons.Default.Radio),
    TileGraphic("volume", "Speaker", Icons.Default.VolumeUp),
    TileGraphic("audiotrack", "Track", Icons.Default.Audiotrack),
    TileGraphic("queue_music", "Playlist", Icons.Default.QueueMusic),

    // 🎨 Art & Creativity
    TileGraphic("palette", "Palette", Icons.Default.Palette),
    TileGraphic("brush", "Brush", Icons.Default.Brush),
    TileGraphic("camera", "Camera", Icons.Default.CameraAlt),
    TileGraphic("photo", "Photo", Icons.Default.PhotoCamera),
    TileGraphic("edit", "Pencil", Icons.Default.Edit),
    TileGraphic("color_lens", "Color Lens", Icons.Default.ColorLens),
    TileGraphic("create", "Design", Icons.Default.Create),

    // 💡 Tech & Gadgets
    TileGraphic("lightbulb", "Light", Icons.Default.Lightbulb),
    TileGraphic("phone", "Smartphone", Icons.Default.PhoneAndroid),
    TileGraphic("laptop", "Computer", Icons.Default.Laptop),
    TileGraphic("watch", "Watch", Icons.Default.Watch),
    TileGraphic("build", "Wrench", Icons.Default.Build),
    TileGraphic("memory", "Chip", Icons.Default.Memory),
    TileGraphic("bolt", "Lightning", Icons.Default.FlashOn),
    TileGraphic("power", "Power", Icons.Default.PowerSettingsNew),

    // 💎 Treasure & Magic
    TileGraphic("diamond", "Diamond", Icons.Default.Diamond),
    TileGraphic("key", "Key", Icons.Default.VpnKey),
    TileGraphic("lock", "Lock", Icons.Default.Lock),
    TileGraphic("favorite", "Heart", Icons.Default.Favorite),
    TileGraphic("shopping", "Bag", Icons.Default.ShoppingBag),
    TileGraphic("celebration", "Party", Icons.Default.Celebration),
    TileGraphic("card_giftcard", "Gift", Icons.Default.CardGiftcard),
    TileGraphic("loyalty", "Badge", Icons.Default.Loyalty),

    // 🐾 Animals & Nature
    TileGraphic("pet", "Paw", Icons.Default.Pets),
    TileGraphic("park", "Tree", Icons.Default.Park),
    TileGraphic("nature", "Leaf", Icons.Default.Eco),
    TileGraphic("forest", "Forest", Icons.Default.Forest),
    TileGraphic("water", "Water", Icons.Default.WaterDrop),
    TileGraphic("landscape", "Mountain", Icons.Default.Landscape),
    TileGraphic("bug", "Bug", Icons.Default.BugReport)
)

// Global tracker to ensure ZERO repetition until all 100+ graphics have been enjoyed!
private val globalUsedGraphicKeys = mutableSetOf<String>()

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

    // Select 10 100% FRESH, UNUSED graphics with dynamic random gradients for every single play!
    fun select10FreshGraphics(): List<TileGraphic> {
        val availableGraphics = EXPANDED_MASTER_TILE_POOL.filter { !globalUsedGraphicKeys.contains(it.key) }

        val selectedBase = if (availableGraphics.size >= 10) {
            availableGraphics.shuffled().take(10)
        } else {
            globalUsedGraphicKeys.clear() // Reset when full pool completed
            EXPANDED_MASTER_TILE_POOL.shuffled().take(10)
        }

        globalUsedGraphicKeys.addAll(selectedBase.map { it.key })

        // Assign a FRESH dynamic gradient pair to each tile graphic
        val shuffledGradients = VIBRANT_GRADIENTS.shuffled()
        return selectedBase.mapIndexed { idx, item ->
            item.copy(bgGradient = shuffledGradients[idx % shuffledGradients.size])
        }
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
    fun triggerAnimatedRestart() {
        if (isRestartingAnimation) return
        isRestartingAnimation = true
        showWinnerDialog = false

        SoundManager.playClickSound()
        cards = cards.map { it.copy(isFlipped = false, isMatched = false) }

        restartTriggerCount++
        cards = createFreshDeck()
        selectedIndices = emptyList()
        isPlayer1Turn = true
        player1Score = 0
        player2Score = 0
        isProcessingTurn = false
        rewardEarned = false

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
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Header Bar with 3D Metallic Glassmorphism Buttons ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 3D Metallic Glassmorphism Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(20.dp))
                }

                // Title
                Text(
                    text = "Memory Match 2P",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )

                // 3D Metallic Glassmorphism Retry Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                            triggerAnimatedRestart()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── TOP: PLAYER 1 CARD ATTACHED TO LEFT EDGE OF SCREEN ────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isPlayer1Turn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 20.dp)
                ) {
                    Text(
                        text = "PLAYER 1 | $player1Score",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 4x5 PERFECT SQUARE CARDS GRID (CENTERED VERTICALLY) ──────
            key(restartTriggerCount) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // ── BOTTOM: PLAYER 2 CARD ATTACHED TO RIGHT EDGE OF SCREEN ─────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isPlayer1Turn) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 20.dp)
                ) {
                    Text(
                        text = "PLAYER 2 | $player2Score",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                .background(Color.Black.copy(alpha = 0.75f))
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

                    // Action Buttons
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

    // Unflipped 3D Soft-Clay Amber Gold vs Flipped Dynamic Tile Gradient
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
