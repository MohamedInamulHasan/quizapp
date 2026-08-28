package com.ilygames.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.data.model.LeaderboardEntry
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

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
    var isDaily by remember { mutableStateOf(true) }

    // Auto-refresh leaderboard: immediately + every 15s while screen is open
    LaunchedEffect(isDaily) {
        while (true) {
            quizViewModel.loadLeaderboard(token, isDaily)
            delay(15_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.EmojiEvents, contentDescription = null,
                tint = TextGold, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Leaderboard", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        }

        // Filter out any players with 0 points
        val validLeaderboard = remember(leaderboard) { leaderboard.filter { it.score > 0 } }

        // ── Content ───────────────────────────────────────────────────────────
        if (validLeaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏆", fontSize = 52.sp)
                    Text("No rankings recorded yet!", fontWeight = FontWeight.Black, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("Play a quiz to claim the #1 rank!", color = TextMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Podium for top 3
                item {
                    LeaderboardPodium(
                        entries = validLeaderboard.take(3),
                        currentUserId = currentUserId,
                        currentUserName = currentUserName
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    if (validLeaderboard.size > 3) {
                        Text("ALL RANKINGS", fontSize = 11.sp, fontWeight = FontWeight.Black,
                            color = TextMuted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    }
                }

                itemsIndexed(validLeaderboard) { index, player ->
                    LeaderboardRow(player = player, currentUserId = currentUserId, currentUserName = currentUserName)
                }
            }
        }
    }
}

// ─── Podium (Top 3) ───────────────────────────────────────────────────────────
@Composable
fun LeaderboardPodium(entries: List<LeaderboardEntry>, currentUserId: String, currentUserName: String) {
    val first  = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third  = entries.getOrNull(2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(
                listOf(PrimaryGreen.copy(alpha = 0.10f), MaterialTheme.colorScheme.surface)
            ))
            .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd — left
            if (second != null) {
                PodiumColumn(
                    rank = 2, id = second.id, name = second.name, score = second.score,
                    currentUserId = currentUserId, currentUserName = currentUserName,
                    pillarHeight = 90.dp, avatarSize = 62.dp,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // 1st — center (tallest)
            if (first != null) {
                PodiumColumn(
                    rank = 1, id = first.id, name = first.name, score = first.score,
                    currentUserId = currentUserId, currentUserName = currentUserName,
                    pillarHeight = 120.dp, avatarSize = 76.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3rd — right
            if (third != null) {
                PodiumColumn(
                    rank = 3, id = third.id, name = third.name, score = third.score,
                    currentUserId = currentUserId, currentUserName = currentUserName,
                    pillarHeight = 70.dp, avatarSize = 54.dp,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PodiumColumn(
    rank: Int,
    id: String,
    name: String,
    score: Int,
    currentUserId: String,
    currentUserName: String,
    pillarHeight: Dp,
    avatarSize: Dp,
    modifier: Modifier = Modifier
) {
    val isMe = if (currentUserId.isNotBlank()) id == currentUserId else currentUserName.isNotBlank() && name.equals(currentUserName, ignoreCase = true)
    val firstName = name.split(" ").firstOrNull() ?: name
    val displayName = if (isMe) "$firstName (You)" else firstName

    val borderColor = when (rank) {
        1 -> TextGold
        2 -> Color(0xFFADB5BD)
        3 -> Color(0xFFCD7F32)
        else -> PrimaryGreen
    }
    val pillarBrush = when (rank) {
        1 -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFBCC0C7), Color(0xFF6C757D)))
        3 -> Brush.verticalGradient(listOf(Color(0xFFCD7F32), Color(0xFF7B4F1D)))
        else -> Brush.verticalGradient(listOf(PrimaryGreen, EmeraldGlow))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Crown for #1
        if (rank == 1) {
            Text("👑", fontSize = 22.sp)
        } else {
            Spacer(modifier = Modifier.height(26.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(avatarSize)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isMe && !globalProfileImageUri.value.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = globalProfileImageUri.value,
                    contentDescription = "Profile",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    text = getPlayerInitials(name),
                    fontSize = if (rank == 1) 20.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    color = borderColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Name
        Text(
            text = displayName,
            fontSize = if (rank == 1) 13.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMe) PrimaryGreen else MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pillar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(pillarHeight)
                .background(pillarBrush, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    fontSize = if (rank == 1) 20.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text("pts", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
fun LeaderboardRow(player: LeaderboardEntry, currentUserId: String, currentUserName: String) {
    val isMe = if (currentUserId.isNotBlank()) player.id == currentUserId else currentUserName.isNotBlank() && player.name.equals(currentUserName, ignoreCase = true)
    val displayName = if (isMe) "${player.name} (You)" else player.name
    val medal = when (player.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isMe) PrimaryGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isMe) PrimaryGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
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
                    if (medal != null) {
                        Text(medal, fontSize = 20.sp)
                    } else {
                        Text("#${player.rank}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = TextMuted)
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMe && !globalProfileImageUri.value.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = globalProfileImageUri.value,
                            contentDescription = "Profile",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = getPlayerInitials(player.name),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }

                // Name
                Text(
                    text = displayName,
                    fontWeight = if (isMe) FontWeight.Black else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isMe) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Score Badge
            Box(
                modifier = Modifier
                    .background(
                        if (isMe) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${player.score} pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

