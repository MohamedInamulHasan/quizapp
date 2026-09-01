package com.ilygames.quizapp.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.utils.SoundManager
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.model.LeaderboardEntry
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import java.io.File

// ─── Google Account-Style Curated Profile Background Palette ────────────────
val googleProfileColors = listOf(
    Color(0xFF1E88E5), // Google Blue
    Color(0xFFE53935), // Google Red
    Color(0xFF43A047), // Google Green
    Color(0xFFFB8C00), // Google Orange
    Color(0xFF8E24AA), // Google Purple
    Color(0xFF00ACC1), // Google Cyan
    Color(0xFFD81B60), // Google Pink
    Color(0xFF5E35B1), // Google Deep Purple
    Color(0xFF00897B)  // Google Teal
)

fun getGoogleProfileColor(name: String): Color {
    if (name.isBlank()) return googleProfileColors[0]
    val hash = Math.abs(name.lowercase().hashCode())
    return googleProfileColors[hash % googleProfileColors.size]
}

// ─── Robust Profile Image URL Resolver ───────────────────────────────────────
fun getFullProfileImageUrl(rawUrl: String?): Any? {
    if (rawUrl.isNullOrBlank() || rawUrl == "undefined" || rawUrl == "null" || rawUrl.endsWith("/undefined") || rawUrl.endsWith("/null")) return null
    val trimmed = rawUrl.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:image/") -> trimmed
        trimmed.startsWith("content://") || trimmed.startsWith("file://") -> Uri.parse(trimmed)
        trimmed.startsWith("/") || trimmed.startsWith("c:\\", ignoreCase = true) || trimmed.startsWith("C:\\") -> {
            val f = File(trimmed)
            if (f.exists() && f.length() > 0) f else null
        }
        else -> {
            if (trimmed.contains(".") || trimmed.length > 5) {
                "${ApiClient.BASE_URL.removeSuffix("/api/")}/uploads/$trimmed"
            } else null
        }
    }
}

// ─── Helper: Get 1-2 letter initials from a name ─────────────────────────────
fun getPlayerInitials(name: String): String {
    val clean = name.trim().replace(Regex("[^a-zA-Z ]"), "")
    if (clean.isBlank()) return "P"
    val parts = clean.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
    } else if (parts[0].length >= 2) {
        "${parts[0][0].uppercaseChar()}${parts[0][1].uppercaseChar()}"
    } else {
        parts[0].take(1).uppercase()
    }
}

// ─── Main Screen ─────────────────────────────────────────────────────────────
@Composable
fun LeaderboardScreen(
    token: String,
    quizViewModel: QuizViewModel,
    currentUserId: String = "",
    currentUserName: String = "",
    onBack: () -> Unit
) {
    val leaderboard by quizViewModel.leaderboard.collectAsState()

    // Auto-refresh leaderboard: quietly refresh every 15s while screen is open
    LaunchedEffect(Unit) {
        while (true) {
            quizViewModel.loadLeaderboard(token, true)
            delay(15_000L)
        }
    }

    // Strictly sort leaderboard descending by numerical score & reassign ranks 1..N
    val validLeaderboard = remember(leaderboard) {
        leaderboard
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .mapIndexed { index, player ->
                player.copy(rank = index + 1)
            }
    }

    val isDarkHeader = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val backBtnBg = if (isDarkHeader) Color(0xFF1C273A) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Header (Title Centered) ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // 3D Soft-Clay Glossy Back Icon Button
            IconButton(
                onClick = {
                    SoundManager.playClickSound()
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .background(backBtnBg, CircleShape)
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkHeader) 0.35f else 0.9f),
                                Color.Black.copy(alpha = if (isDarkHeader) 0.5f else 0.08f)
                            )
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDarkHeader) Color.White else Color(0xFF17181C),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF255FF4),
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        if (validLeaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color(0xFF255FF4),
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(42.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // Podium for top 3
                item {
                    LeaderboardPodium(
                        entries = validLeaderboard.take(3),
                        currentUserId = currentUserId,
                        currentUserName = currentUserName
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                item {
                    if (validLeaderboard.size > 3) {
                        Text(
                            text = "ALL RANKINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }

                itemsIndexed(
                    items = validLeaderboard,
                    key = { _, player -> if (player.id.isNotBlank()) player.id else player.name }
                ) { _, player ->
                    LeaderboardRow(player = player, currentUserId = currentUserId, currentUserName = currentUserName)
                }
            }
        }
    }
}

// ─── Podium (Top 3 Modern UI/UX Design) ─────────────────────────────────────
@Composable
fun LeaderboardPodium(entries: List<LeaderboardEntry>, currentUserId: String, currentUserName: String) {
    val first  = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third  = entries.getOrNull(2)

    val isDarkLdr = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val ldrCardBg = if (isDarkLdr) Color(0xFF1C273A) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .shadow(10.dp, RoundedCornerShape(26.dp))
            .background(ldrCardBg, RoundedCornerShape(26.dp))
            .border(
                1.5.dp,
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkLdr) 0.35f else 0.9f),
                        Color.Black.copy(alpha = if (isDarkLdr) 0.5f else 0.08f)
                    )
                ),
                RoundedCornerShape(26.dp)
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 16.dp, start = 10.dp, end = 10.dp, bottom = 0.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place — left
                if (second != null) {
                    PodiumColumn(
                        rank = 2, player = second,
                        currentUserId = currentUserId, currentUserName = currentUserName,
                        pillarHeight = 95.dp, avatarSize = 60.dp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 1st Place — center (tallest & featured)
                if (first != null) {
                    PodiumColumn(
                        rank = 1, player = first,
                        currentUserId = currentUserId, currentUserName = currentUserName,
                        pillarHeight = 125.dp, avatarSize = 74.dp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3rd Place — right
                if (third != null) {
                    PodiumColumn(
                        rank = 3, player = third,
                        currentUserId = currentUserId, currentUserName = currentUserName,
                        pillarHeight = 78.dp, avatarSize = 54.dp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PodiumColumn(
    rank: Int,
    player: LeaderboardEntry,
    currentUserId: String, currentUserName: String,
    pillarHeight: Dp,
    avatarSize: Dp,
    modifier: Modifier = Modifier
) {
    var animationTriggered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(rank * 120L)
        animationTriggered = true
    }
    val animatedPillarHeight by animateDpAsState(
        targetValue = if (animationTriggered) pillarHeight else 0.dp,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "PodiumRise"
    )

    val isMe = if (currentUserId.isNotBlank()) player.id == currentUserId else currentUserName.isNotBlank() && player.name.equals(currentUserName, ignoreCase = true)
    val firstName = player.name.split(" ").firstOrNull() ?: player.name
    val displayName = if (isMe) "$firstName (You)" else firstName

    val rawImageUrl = if (isMe && !globalProfileImageUri.value.isNullOrBlank()) globalProfileImageUri.value else player.profileImageUrl
    val model = getFullProfileImageUrl(rawImageUrl)

    // Uniform 3D Royal Blue gradient for all 3 podium pillars
    val pillarBrush = Brush.verticalGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA)))
    val avatarBg = getGoogleProfileColor(player.name)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Crown / Badge Header
        if (rank == 1) {
            Text("👑", fontSize = 22.sp)
        } else {
            Spacer(modifier = Modifier.height(26.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Avatar Container (Flat circle, no placeholder shadow)
        Surface(
            shape = CircleShape,
            color = if (model != null) Color.Transparent else avatarBg,
            shadowElevation = 0.dp,
            modifier = Modifier.size(avatarSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (model != null) {
                    coil.compose.AsyncImage(
                        model = model,
                        contentDescription = "Profile",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = getPlayerInitials(player.name),
                        fontSize = if (rank == 1) 20.sp else 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Player Name
        Text(
            text = displayName,
            fontSize = if (rank == 1) 13.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMe) Color(0xFF255FF4) else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Sleek UI/UX Podium Pillar Container (Smooth Rising Animation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(animatedPillarHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(pillarBrush)
                .border(
                    BorderStroke(1.dp, Color(0xFF386DF5).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${player.score}",
                    fontSize = if (rank == 1) 22.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "pts",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ─── Leaderboard Row (Staggered Waterfall Slide & Arrange Animation) ─────────────────
@Composable
fun LeaderboardRow(player: LeaderboardEntry, currentUserId: String, currentUserName: String) {
    var isCardVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(player.rank) {
        delay((player.rank * 70L).coerceAtMost(700L))
        isCardVisible = true
    }
    val cardOffsetY by animateDpAsState(
        targetValue = if (isCardVisible) 0.dp else 40.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "CardOffset"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isCardVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "CardAlpha"
    )

    val isMe = if (currentUserId.isNotBlank()) player.id == currentUserId else currentUserName.isNotBlank() && player.name.equals(currentUserName, ignoreCase = true)
    val displayName = if (isMe) "${player.name} (You)" else player.name
    val medalEmoji = when (player.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }

    val rawImageUrl = if (isMe && !globalProfileImageUri.value.isNullOrBlank()) globalProfileImageUri.value else player.profileImageUrl
    val model = getFullProfileImageUrl(rawImageUrl)
    val avatarBg = getGoogleProfileColor(player.name)

    val isDarkRow = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val rowBg = if (isMe) Color(0xFF255FF4).copy(alpha = 0.15f) else (if (isDarkRow) Color(0xFF1C273A) else Color.White)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = cardOffsetY)
            .graphicsLayer(alpha = cardAlpha)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(rowBg, RoundedCornerShape(18.dp))
            .border(
                1.5.dp,
                if (isMe) Brush.linearGradient(listOf(Color(0xFF255FF4), Color(0xFF255FF4))) else Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkRow) 0.3f else 0.9f),
                        Color.Black.copy(alpha = if (isDarkRow) 0.4f else 0.06f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Medal emoji OR rank number for #4+
                Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    if (medalEmoji != null) {
                        Text(medalEmoji, fontSize = 20.sp)
                    } else {
                        Text(
                            text = "#${player.rank}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                // Avatar Container (Flat circle, no placeholder shadow)
                Surface(
                    shape = CircleShape,
                    color = if (model != null) Color.Transparent else avatarBg,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (model != null) {
                            coil.compose.AsyncImage(
                                model = model,
                                contentDescription = "Profile",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = getPlayerInitials(player.name),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Name
                Text(
                    text = displayName,
                    fontWeight = if (isMe) FontWeight.Black else FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isMe) Color(0xFF255FF4) else (if (isDarkRow) Color.White else Color(0xFF17181C)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Score Badge
            Surface(
                color = if (isMe) Color(0xFF255FF4) else (if (isDarkRow) Color(0xFF131B2A) else Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${player.score} pts",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
