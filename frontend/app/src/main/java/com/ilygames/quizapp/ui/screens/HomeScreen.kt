package com.ilygames.quizapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


// Hardware-Accelerated Tactile Pop-Up Bounce Click Modifier
@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BounceScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// Global Profile Photo State
var globalProfileImageUri = mutableStateOf<String?>(null)

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    onStartQuiz: () -> Unit,
    onStartReadingQuiz: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    onLogout: () -> Unit
) {
    val user by authViewModel.user.collectAsState()
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showSignOutConfirmationModal by remember { mutableStateOf(false) }
    var showRewardShowcaseModal by remember { mutableStateOf(false) }
    var showEditNameModal by remember { mutableStateOf(false) }
    var showGPayProfileModal by remember { mutableStateOf(false) }
    var editNameError by remember { mutableStateOf("") }
    var isUpdatingName by remember { mutableStateOf(false) }

    var customUserNameState = remember { mutableStateOf(user?.name ?: "Player") }
    var tempNameInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Profile Photo Picker Launcher — uploads to server and saves URL in MongoDB
    val profileImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            kotlinx.coroutines.MainScope().launch {
                try {
                    val token = authViewModel.token.value
                        ?: context.getSharedPreferences("quiz_prefs", android.content.Context.MODE_PRIVATE)
                            .getString("auth_token", "") ?: ""
                    val stream = context.contentResolver.openInputStream(selectedUri)
                    val mimeType = context.contentResolver.getType(selectedUri) ?: "image/jpeg"
                    val ext = when {
                        mimeType.contains("png") -> ".png"
                        mimeType.contains("gif") -> ".gif"
                        mimeType.contains("webp") -> ".webp"
                        else -> ".jpg"
                    }
                    val bytes = stream?.readBytes()
                    stream?.close()
                    if (bytes != null && token.isNotBlank()) {
                        val mediaType = mimeType.toMediaTypeOrNull()
                        val requestBody = bytes.toRequestBody(mediaType)
                        val part = okhttp3.MultipartBody.Part.createFormData(
                            "image",
                            "profile_${System.currentTimeMillis()}$ext",
                            requestBody
                        )
                        val response = com.ilygames.quizapp.data.api.ApiClient.apiService.uploadImage(token, part)
                        if (response.isSuccessful && response.body()?.imageUrl != null) {
                            val imageUrl = response.body()!!.imageUrl
                            val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("saved_profile_img_url", imageUrl).apply()

                            com.ilygames.quizapp.data.api.ApiClient.apiService.updateProfile(
                                token,
                                com.ilygames.quizapp.data.model.UpdateProfileRequest(profileImageUrl = imageUrl)
                            )
                            authViewModel.updateProfileState(profileImageUrl = imageUrl)
                            globalProfileImageUri.value = imageUrl
                            Toast.makeText(context, "📸 Profile photo saved!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val localUriStr = selectedUri.toString()
                            val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("saved_profile_img_url", localUriStr).apply()
                            authViewModel.updateProfileState(profileImageUrl = localUriStr)
                            globalProfileImageUri.value = localUriStr
                            Toast.makeText(context, "📸 Profile photo saved!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else if (token.isBlank()) {
                        val localUriStr = selectedUri.toString()
                        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("saved_profile_img_url", localUriStr).apply()
                        authViewModel.updateProfileState(profileImageUrl = localUriStr)
                        globalProfileImageUri.value = localUriStr
                        Toast.makeText(context, "📸 Profile photo saved!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    val localUriStr = selectedUri.toString()
                    val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("saved_profile_img_url", localUriStr).apply()
                    authViewModel.updateProfileState(profileImageUrl = localUriStr)
                    globalProfileImageUri.value = localUriStr
                    Toast.makeText(context, "📸 Profile photo saved!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 2 Daily Quiz Plays Attempts State
    var dailyAttemptsLeft by remember { mutableStateOf(2) }

    // Refresh profile every time the screen is entered or user changes
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val localProfileImg = prefs.getString("saved_profile_img_url", null)
        if (!localProfileImg.isNullOrBlank()) {
            globalProfileImageUri.value = localProfileImg
        }
        loadPersistedAdminData(context)
        authViewModel.refreshProfile()
    }

    LaunchedEffect(user) {
        // Load profile image URL from user object (saved in MongoDB)
        user?.profileImageUrl?.let {
            if (it.isNotBlank()) {
                globalProfileImageUri.value = it
                val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("saved_profile_img_url", it).apply()
            }
        }
        val savedName = user?.name ?: "Player"
        customUserNameState.value = savedName
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
        ) {
            // 1. MINIMALIST TOP HEADER (User Profile & Right-Aligned Small Settings Dropdown)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                SoundManager.playClickSound()
                                showGPayProfileModal = true
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!globalProfileImageUri.value.isNullOrBlank()) {
                                    coil.compose.AsyncImage(
                                        model = globalProfileImageUri.value,
                                        contentDescription = "Profile Photo",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = "WELCOME BACK",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = customUserNameState.value,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // Right-Aligned Settings Button & Clean Popup Card
                        Box {
                            IconButton(
                                onClick = {
                                    SoundManager.playClickSound()
                                    showSettingsMenu = !showSettingsMenu
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (showSettingsMenu) {
                                Popup(
                                    alignment = Alignment.TopEnd,
                                    offset = IntOffset(0, 110),
                                    onDismissRequest = { showSettingsMenu = false }
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        modifier = Modifier
                                            .width(230.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "App Settings",
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                            // Dark Mode Switch Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { ThemeState.isDarkMode = !ThemeState.isDarkMode }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (ThemeState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                        contentDescription = "Theme",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (ThemeState.isDarkMode) "Dark Theme" else "Light Theme",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Switch(
                                                    checked = ThemeState.isDarkMode,
                                                    onCheckedChange = { ThemeState.isDarkMode = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = PrimaryGreen,
                                                        uncheckedThumbColor = DarkGreen,
                                                        uncheckedTrackColor = SurfaceGray
                                                    )
                                                )
                                            }

                                            // Sound FX Switch Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { ThemeState.isSoundEnabled = !ThemeState.isSoundEnabled }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (ThemeState.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                                        contentDescription = "Sound",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Sound FX",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Switch(
                                                    checked = ThemeState.isSoundEnabled,
                                                    onCheckedChange = { ThemeState.isSoundEnabled = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = PrimaryGreen,
                                                        uncheckedThumbColor = DarkGreen,
                                                        uncheckedTrackColor = SurfaceGray
                                                    )
                                                )
                                            }

                                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                            // Admin Studio Option (Visible exclusively for mohamedinamulhasan0@gmail.com / mohmaedinamulhasan0@gmail.com)
                                            if (user?.isAdmin == true || user?.email?.equals("mohamedinamulhasan0@gmail.com", ignoreCase = true) == true || user?.email?.equals("mohmaedinamulhasan0@gmail.com", ignoreCase = true) == true) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            showSettingsMenu = false
                                                            onNavigateToAdmin()
                                                        }
                                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AdminPanelSettings,
                                                        contentDescription = "Admin",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Admin Studio",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.sp
                                                    )
                                                }

                                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                            }

                                            // Sign Out Option
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        SoundManager.playClickSound()
                                                        showSettingsMenu = false
                                                        showSignOutConfirmationModal = true
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ExitToApp,
                                                    contentDescription = "Sign Out",
                                                    tint = IncorrectRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Sign Out",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IncorrectRed,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. RESTORED ORIGINAL CLEAN SURFACE SCORE CARD (Showing Today's Score & High Score Side-by-Side)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Today's Score
                        Column {
                            Text(
                                text = "LATEST SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${user?.todayScore ?: 0} pts",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider Line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        // Right: Total High Score
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "HIGH SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${user?.highScore ?: 0} pts",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
            }

            // 3. GREEN GRADIENT DAILY QUIZ CARD (SLIGHTLY BIGGER, NO TAGS, NO HEARTS, BOUNCE CLICK ANIMATION)
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .bounceClick {
                            SoundManager.playClickSound()
                            if (dailyAttemptsLeft > 0) {
                                dailyAttemptsLeft--
                                onStartQuiz()
                            } else {
                                Toast.makeText(context, "No quiz attempts left today! Tap 'Watch for Extra Chance' below to unlock +1 play.", Toast.LENGTH_LONG).show()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PrimaryGreen, EmeraldGlow)
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .border(1.dp, ElectricMint.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Daily Quiz Challenge",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Answer fast-paced questions, earn instant points, climb the global leaderboard & win today's reward!",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = Color.White.copy(alpha = 0.95f)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White)
                                    .padding(vertical = 12.dp, horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (dailyAttemptsLeft > 0) "START QUIZ NOW" else "OUT OF ATTEMPTS TODAY",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (dailyAttemptsLeft > 0) DarkGreen else IncorrectRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (dailyAttemptsLeft > 0) Icons.Default.PlayArrow else Icons.Default.Lock,
                                        contentDescription = "Action",
                                        tint = if (dailyAttemptsLeft > 0) DarkGreen else IncorrectRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. UNIFIED 2x2 FEATURE GRID (GRADIENT GREEN OUTLINE & TACTILE BOUNCE ANIMATION)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "EXPLORE & REWARDS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen,
                        letterSpacing = 1.sp
                    )

                    // Row 1: Passage Study, Leaderboard & Explore Quiz
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UnifiedEmeraldCard(
                            title = "Passage Study",
                            description = "Read study passages to prepare for quiz",
                            icon = Icons.Default.MenuBook,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onStartReadingQuiz()
                            }
                        )

                        UnifiedEmeraldCard(
                            title = "Leaderboard",
                            description = "View top ranked players worldwide",
                            icon = Icons.Default.Leaderboard,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onNavigateToLeaderboard()
                            }
                        )
                    }

                    // Row 2: Today's Reward & Extra Chance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UnifiedEmeraldCard(
                            title = "Today's Reward",
                            description = "Tap to view today's physical prize",
                            icon = Icons.Default.CardGiftcard,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                showRewardShowcaseModal = true
                            }
                        )

                        UnifiedEmeraldCard(
                            title = "Extra Chance",
                            description = "Watch video ad for +1 extra chance",
                            icon = Icons.Default.OndemandVideo,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                dailyAttemptsLeft++
                                authViewModel.addAdReward(context)
                                Toast.makeText(context, "📺 Video Complete! +1 Extra Attempt Granted!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // TODAY'S REWARD POPUP CARD (MATCHING USER'S EXACT DESIGN SPECIFICATIONS)
        if (showRewardShowcaseModal) {
            AlertDialog(
                onDismissRequest = { showRewardShowcaseModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(32.dp),
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Today's Reward",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = { showRewardShowcaseModal = false },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // REWARD IMAGE (WITHOUT BACKGROUND CONTAINER / WITHOUT BORDER BOX)
                        if (!globalRewardImageUrl.value.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = globalRewardImageUrl.value,
                                contentDescription = "Reward Prize Image",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Reward Prize",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(110.dp)
                            )
                        }

                        // BELOW TITLE & DESCRIPTION (DIRECTLY BELOW IMAGE)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = globalRewardTitle.value,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = globalRewardDescription.value,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 14.sp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                confirmButton = {} // NO BUTTON PER USER DIRECTIVE
            )
        }

        // GPay-Style Profile & Account Modal
        if (showGPayProfileModal) {
            AlertDialog(
                onDismissRequest = { showGPayProfileModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(32.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = PrimaryGreen
                        )
                        IconButton(onClick = { showGPayProfileModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Profile avatar with camera upload badge (Restored)
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clickable { profileImagePicker.launch("image/*") }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (!globalProfileImageUri.value.isNullOrBlank()) {
                                        coil.compose.AsyncImage(
                                            model = globalProfileImageUri.value,
                                            contentDescription = "Profile Photo",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }
                            // Camera badge
                            Surface(
                                shape = CircleShape,
                                color = PrimaryGreen,
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.BottomEnd)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Upload Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Editable name (Restored)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    tempNameInput = customUserNameState.value
                                    showEditNameModal = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = customUserNameState.value,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tap caption (Restored)
                        Text(
                            text = "Tap name to change username",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextMuted
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showGPayProfileModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Edit Player Username Modal
        if (showEditNameModal) {
            AlertDialog(
                onDismissRequest = {
                    showEditNameModal = false
                    editNameError = ""
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Change Username",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = {
                            showEditNameModal = false
                            editNameError = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Enter a new unique username for the leaderboard.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = tempNameInput,
                            onValueChange = { input ->
                                // Block spaces and special chars — only letters, digits, _ and . (max 12 chars)
                                val filtered = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(12)
                                tempNameInput = filtered
                                editNameError = ""
                            },
                            label = { Text("New Username", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            ),
                            isError = editNameError.isNotBlank(),
                            supportingText = if (editNameError.isNotBlank()) {
                                { Text(editNameError, color = IncorrectRed) }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                errorBorderColor = IncorrectRed,
                                focusedLabelColor = PrimaryGreen,
                                unfocusedLabelColor = TextMuted,
                                cursorColor = PrimaryGreen,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempNameInput.isNotBlank()) {
                                isUpdatingName = true
                                editNameError = ""
                                kotlinx.coroutines.MainScope().launch {
                                    try {
                                        val token = authViewModel.token.value
                                            ?: context.getSharedPreferences("quiz_prefs", android.content.Context.MODE_PRIVATE)
                                                .getString("auth_token", "") ?: ""
                                        val response = com.ilygames.quizapp.data.api.ApiClient.apiService.updateProfile(
                                            token,
                                            com.ilygames.quizapp.data.model.UpdateProfileRequest(name = tempNameInput)
                                        )
                                        if (response.isSuccessful) {
                                            customUserNameState.value = tempNameInput
                                            authViewModel.updateProfileState(name = tempNameInput)
                                            showEditNameModal = false
                                            editNameError = ""
                                            Toast.makeText(context, "✏️ Username updated!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val rawError = response.errorBody()?.string() ?: ""
                                            val cleanMsg = try {
                                                org.json.JSONObject(rawError).optString("msg", "")
                                            } catch (e: Exception) { "" }
                                            editNameError = when {
                                                cleanMsg.contains("already in use", ignoreCase = true) ->
                                                    "Username is already in use, try another"
                                                cleanMsg.isNotBlank() -> cleanMsg
                                                else -> "Failed to update. Please try again."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        editNameError = "Network error. Please try again."
                                    } finally {
                                        isUpdatingName = false
                                    }
                                }
                            }
                        },
                        enabled = tempNameInput.isNotBlank() && !isUpdatingName,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isUpdatingName) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Username", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // Sign Out Confirmation Modal Popup
        if (showSignOutConfirmationModal) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirmationModal = false },
                title = {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to sign out of Quizzy?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            showSignOutConfirmationModal = false
                            authViewModel.logout(context)
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            SoundManager.playClickSound()
                            showSignOutConfirmationModal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun UnifiedEmeraldCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
            .bounceClick { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = TextMuted
            )
        }
    }
}
