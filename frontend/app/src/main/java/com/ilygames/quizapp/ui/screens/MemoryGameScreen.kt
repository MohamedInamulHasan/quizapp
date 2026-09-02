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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.delay

data class MemoryCard(
    val id: Int,
    val emoji: String,
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
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    // Emoji pool: 10 distinct pairs for a 4x5 grid of 20 cards
    val baseEmojis = listOf(
        "🤡", "🫶", "🚀", "💎", "⚽", 
        "👑", "🦁", "⚡", "🍕", "🎯"
    )

    fun createFreshDeck(): List<MemoryCard> {
        val pairs = (baseEmojis + baseEmojis).shuffled()
        return pairs.mapIndexed { index, emoji ->
            MemoryCard(id = index, emoji = emoji)
        }
    }

    var cards by remember { mutableStateOf(createFreshDeck()) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var turnsCount by remember { mutableIntStateOf(0) }
    var pairsMatched by remember { mutableIntStateOf(0) }
    var isProcessingMatch by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    var rewardEarned by remember { mutableStateOf(false) }

    fun restartGame() {
        cards = createFreshDeck()
        selectedIndices = emptyList()
        turnsCount = 0
        pairsMatched = 0
        isProcessingMatch = false
        showWinDialog = false
        rewardEarned = false
    }

    // Handle Card Tap Logic
    fun onCardClick(index: Int) {
        if (isProcessingMatch) return
        val card = cards[index]
        if (card.isFlipped || card.isMatched) return

        SoundManager.playClickSound()

        // Flip the clicked card
        cards = cards.toMutableList().also {
            it[index] = it[index].copy(isFlipped = true)
        }

        val newSelected = selectedIndices + index
        selectedIndices = newSelected

        if (newSelected.size == 2) {
            turnsCount++
            isProcessingMatch = true
            val firstIdx = newSelected[0]
            val secondIdx = newSelected[1]

            if (cards[firstIdx].emoji == cards[secondIdx].emoji) {
                // Match Found!
                SoundManager.playCorrectSound()
                cards = cards.toMutableList().also {
                    it[firstIdx] = it[firstIdx].copy(isMatched = true)
                    it[secondIdx] = it[secondIdx].copy(isMatched = true)
                }
                pairsMatched++
                selectedIndices = emptyList()
                isProcessingMatch = false

                if (pairsMatched == baseEmojis.size) {
                    // Victory!
                    if (!rewardEarned) {
                        rewardEarned = true
                        authViewModel.addAdReward(context)
                        Toast.makeText(context, "🪙 +50 Coins Earned for Memory Match!", Toast.LENGTH_SHORT).show()
                    }
                    showWinDialog = true
                }
            } else {
                // No match -> Flip back after delay
                SoundManager.playWrongSound()
            }
        }
    }

    // Delayed flip back for non-matching pair
    LaunchedEffect(selectedIndices, isProcessingMatch) {
        if (selectedIndices.size == 2 && isProcessingMatch) {
            val firstIdx = selectedIndices[0]
            val secondIdx = selectedIndices[1]
            if (cards[firstIdx].emoji != cards[secondIdx].emoji) {
                delay(800)
                cards = cards.toMutableList().also {
                    it[firstIdx] = it[firstIdx].copy(isFlipped = false)
                    it[secondIdx] = it[secondIdx].copy(isFlipped = false)
                }
                selectedIndices = emptyList()
                isProcessingMatch = false
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Top Bar Header ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 3D Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.3f else 0.9f),
                                    Color.Black.copy(alpha = 0.1f)
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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Memory Match",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            text = "Flip & Match Emoji Pairs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor
                        )
                    }
                }

                // Restart Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.3f else 0.9f),
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            SoundManager.playClickSound()
                            restartGame()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color(0xFF255FF4),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Dashboard Stat Cards ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Turns Counter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626))),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("YOUR TURN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                            Text("TURNS", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text("$turnsCount", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                // Pairs Found Counter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MATCHES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                            Text("PAIRS", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text("$pairsMatched/${baseEmojis.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 4x5 Memory Card Grid ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(12.dp, RoundedCornerShape(26.dp))
                    .background(cardBg, RoundedCornerShape(26.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.25f else 0.9f),
                                Color.Black.copy(alpha = if (isDark) 0.4f else 0.06f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(cards) { idx, card ->
                        MemoryCardTile(
                            card = card,
                            isDark = isDark,
                            onClick = { onCardClick(idx) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Win / Victory Modal Dialog ───────────────────────────────────
        if (showWinDialog) {
            AlertDialog(
                onDismissRequest = { showWinDialog = false },
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
                        text = "🏆 Memory Champion!",
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
                        Text("🎉 You matched all pairs in $turnsCount turns!", fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("🪙 +50 Coins Added to Your Wallet!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFEAB308))
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = cardBg,
                titleContentColor = textColor,
                textContentColor = subTextColor
            )
        }
    }
}

// ── 3D Soft-Clay Memory Card Tile Component ─────────────────────────────
@Composable
fun MemoryCardTile(
    card: MemoryCard,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "TileFlipAnimation"
    )

    val isFrontVisible = rotation > 90f

    // Soft-Clay 3D Yellow/Gold Unflipped Tile vs White/Cyan Flipped Tile
    val unflippedBg = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))
    val matchedBg = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
    val flippedBg = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)

    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .shadow(if (card.isMatched) 2.dp else 6.dp, RoundedCornerShape(18.dp))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (card.isMatched) matchedBg
                else if (isFrontVisible) Brush.verticalGradient(listOf(flippedBg, flippedBg))
                else unflippedBg
            )
            .border(
                1.5.dp,
                if (card.isMatched) Color.White.copy(alpha = 0.8f)
                else if (isFrontVisible) Color(0xFF255FF4).copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.8f),
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = !card.isFlipped && !card.isMatched, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isFrontVisible) {
            Text(
                text = card.emoji,
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer {
                    rotationY = 180f // Flip back text so it displays right-side up
                }
            )
        } else {
            // Unflipped tile pattern icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
