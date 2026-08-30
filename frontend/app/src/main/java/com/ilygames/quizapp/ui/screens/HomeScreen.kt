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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    // Sync active daily reward from backend for all users on startup
    LaunchedEffect(Unit) {
        try {
            val resp = com.ilygames.quizapp.data.api.ApiClient.apiService.getReward()
            if (resp.isSuccessful && resp.body() != null) {
                val reward = resp.body()!!
                if (!reward.title.isNullOrBlank()) {
                    globalRewardTitle.value = reward.title
                    globalRewardDescription.value = reward.description
                    globalRewardImageUrl.value = reward.imageUrl
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

fun compressImageUriToBytes(context: Context, uri: android.net.Uri, maxSizePx: Int = 500): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (originalBitmap == null) return null

        val width = originalBitmap.width
        val height = originalBitmap.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        val (targetWidth, targetHeight) = if (bitmapRatio > 1) {
            maxSizePx to (maxSizePx / bitmapRatio).toInt()
        } else {
            (maxSizePx * bitmapRatio).toInt() to maxSizePx
        }

        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, Math.max(targetWidth, 1), Math.max(targetHeight, 1), true)
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

    // Profile Photo Picker Launcher — uploads strictly to Cloudinary and saves URL in MongoDB
    val profileImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            Toast.makeText(context, "Saving profile photo...", Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.MainScope().launch {
                try {
                    val token = authViewModel.token.value
                        ?: context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                            .getString("auth_token", "") ?: ""
                    val bytes = compressImageUriToBytes(context, selectedUri, 500)
                        ?: context.contentResolver.openInputStream(selectedUri)?.use { it.readBytes() }

                    if (bytes != null && token.isNotBlank()) {
                        val mediaType = "image/jpeg".toMediaTypeOrNull()
                        val requestBody = bytes.toRequestBody(mediaType)
                        val part = okhttp3.MultipartBody.Part.createFormData(
                            "image",
                            "profile_${System.currentTimeMillis()}.jpg",
                            requestBody
                        )
                        val response = com.ilygames.quizapp.data.api.ApiClient.apiService.uploadImage(token, part)
                        val body = response.body()
                        val rawUrl = body?.imageUrl ?: body?.url
                        val cloudUrl = rawUrl?.trim()?.removeSurrounding("\"")

                        if (response.isSuccessful && !cloudUrl.isNullOrBlank() && cloudUrl.startsWith("https://res.cloudinary.com/")) {
                            val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("saved_profile_img_url", cloudUrl).apply()

                            com.ilygames.quizapp.data.api.ApiClient.apiService.updateProfile(
                                token,
                                com.ilygames.quizapp.data.model.UpdateProfileRequest(profileImageUrl = cloudUrl)
                            )
                            authViewModel.updateProfileState(profileImageUrl = cloudUrl)
                            globalProfileImageUri.value = cloudUrl
                            Toast.makeText(context, "📸 Profile photo saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                            val errDetails = if (!response.isSuccessful) {
                                "HTTP ${response.code()}${if (!errBody.isNullOrBlank()) ": $errBody" else ""}"
                            } else {
                                (rawUrl ?: "Invalid Cloudinary URL")
                            }
                            Toast.makeText(context, "❌ $errDetails", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    println("[CLOUDINARY_UPLOAD_ERROR] Exception: ${e.localizedMessage}")
                    Toast.makeText(context, "Upload error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 3 Daily Quiz Plays Attempts State (Persisted per User in SharedPreferences)
    val userKey = user?.id ?: user?.name ?: "default"
    val heartsPrefs = remember { context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE) }
    var dailyAttemptsLeft by remember(userKey) { mutableStateOf(heartsPrefs.getInt("saved_hearts_count_$userKey", 3)) }

    LaunchedEffect(userKey) {
        if (userKey != "default") {
            dailyAttemptsLeft = heartsPrefs.getInt("saved_hearts_count_$userKey", 3)
        }
    }

    LaunchedEffect(dailyAttemptsLeft, userKey) {
        if (userKey != "default") {
            heartsPrefs.edit().putInt("saved_hearts_count_$userKey", dailyAttemptsLeft).apply()
        }
    }

    // Refresh profile every time the screen is entered or user changes
    LaunchedEffect(Unit) {
        loadPersistedAdminData(context)
        authViewModel.refreshProfile()
    }

    LaunchedEffect(user) {
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val savedLocally = prefs.getString("saved_profile_img_url", null)
        val currentUser = user
        if (currentUser != null) {
            if (!currentUser.profileImageUrl.isNullOrBlank()) {
                globalProfileImageUri.value = currentUser.profileImageUrl
                prefs.edit().putString("saved_profile_img_url", currentUser.profileImageUrl).apply()
            } else if (!savedLocally.isNullOrBlank()) {
                globalProfileImageUri.value = savedLocally
            } else {
                globalProfileImageUri.value = null
                prefs.edit().remove("saved_profile_img_url").apply()
            }
            val savedName = currentUser.name ?: "Player"
            customUserNameState.value = savedName
        } else {
            globalProfileImageUri.value = null
            prefs.edit().remove("saved_profile_img_url").apply()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
                        val avatarBg = getGoogleProfileColor(customUserNameState.value)
                        val prefs = remember { context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE) }
                        val savedLocally = prefs.getString("saved_profile_img_url", null)
                        val rawProfileUrl = globalProfileImageUri.value?.ifBlank { user?.profileImageUrl ?: savedLocally }
                            ?: user?.profileImageUrl
                            ?: savedLocally
                        val avatarModel = getFullProfileImageUrl(rawProfileUrl)

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(2.dp, Color.White, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarModel != null) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = avatarModel,
                                    contentDescription = "Profile Photo",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    error = {
                                        val userInitial = (customUserNameState.value.trim().firstOrNull() ?: 'P').uppercaseChar().toString()
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().background(avatarBg)
                                        ) {
                                            Text(
                                                text = userInitial,
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize().background(avatarBg)
                                ) {
                                    val userInitial = (customUserNameState.value.trim().firstOrNull() ?: 'P').uppercaseChar().toString()
                                    Text(
                                        text = userInitial,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
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
                                                    ) { ThemeState.setDarkMode(context, !ThemeState.isDarkMode) }
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
                                                    onCheckedChange = { ThemeState.setDarkMode(context, it) },
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
                                                    ) { ThemeState.setSoundEnabled(context, !ThemeState.isSoundEnabled) }
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
                                                        text = if (ThemeState.isSoundEnabled) "Sound FX On" else "Sound Muted",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Switch(
                                                    checked = ThemeState.isSoundEnabled,
                                                    onCheckedChange = { ThemeState.setSoundEnabled(context, it) },
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

            // 2. RESTRUCTURED SCORE & CHANCES CARD (Centered CHANCES title, matching heart emojis, Latest score above High score)
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
                        // LEFT SIDE: CHANCES (EXACTLY 3 NORMAL HEARTS)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CHANCES",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 3) {
                                    val isHeartActive = i < dailyAttemptsLeft
                                    Text(
                                        text = if (isHeartActive) "❤️" else "💔",
                                        fontSize = 20.sp,
                                        modifier = Modifier.clickable {
                                            if (!isHeartActive) {
                                                com.ilygames.quizapp.utils.AdMobManager.showRewardedAd(
                                                    context = context,
                                                    onRewardEarned = {
                                                        dailyAttemptsLeft = (dailyAttemptsLeft + 1).coerceAtMost(3)
                                                        Toast.makeText(context, "🎉 Heart Restored from Video Ad!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // DIVIDER LINE
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        // RIGHT SIDE: LATEST SCORE (ALIGNED WITH CHANCES LINE) & HIGH SCORE (BIGGER, ALIGNED WITH HEARTS LINE)
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // LATEST SCORE : 0 pts (Top Line - Aligned with CHANCES label)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "LATEST SCORE :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${user?.todayScore ?: 0} pts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // HIGH SCORE : 380 pts (Bottom Line - Bigger font, aligned with Hearts)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "HIGH SCORE :",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryGreen,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${user?.highScore ?: 0} pts",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryGreen
                                )
                            }
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
                                heartsPrefs.edit().putInt("saved_hearts_count_$userKey", dailyAttemptsLeft).apply()
                                onStartQuiz()
                            } else {
                                com.ilygames.quizapp.utils.AdMobManager.showRewardedAd(
                                    context = context,
                                    onRewardEarned = {
                                        dailyAttemptsLeft++
                                        heartsPrefs.edit().putInt("saved_hearts_count_$userKey", dailyAttemptsLeft).apply()
                                        authViewModel.addAdReward(context)
                                        Toast.makeText(context, "🎉 Ad Watched! +1 Heart Regained ❤️", Toast.LENGTH_SHORT).show()
                                    }
                                )
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
                                        text = if (dailyAttemptsLeft > 0) "START QUIZ NOW" else "WATCH AD",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Black,
                                        color = if (dailyAttemptsLeft > 0) DarkGreen else PrimaryGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (dailyAttemptsLeft > 0) Icons.Default.PlayArrow else Icons.Default.OndemandVideo,
                                        contentDescription = "Action",
                                        tint = if (dailyAttemptsLeft > 0) DarkGreen else PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
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
                            description = "Watch video ad to restore 1 broken heart",
                            icon = Icons.Default.OndemandVideo,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                if (dailyAttemptsLeft < 3) {
                                    com.ilygames.quizapp.utils.AdMobManager.showRewardedAd(
                                        context = context,
                                        onRewardEarned = {
                                            dailyAttemptsLeft++
                                            heartsPrefs.edit().putInt("saved_hearts_count_$userKey", dailyAttemptsLeft).apply()
                                            authViewModel.addAdReward(context)
                                            Toast.makeText(context, "🎉 Video Complete! +1 Heart Regained ❤️", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "❤️ Hearts are already full! (3/3)", Toast.LENGTH_SHORT).show()
                                }
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
                        val modalPrefs = remember { context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE) }
                        val modalSavedLocally = modalPrefs.getString("saved_profile_img_url", null)
                        val modalRawProfileUrl = globalProfileImageUri.value?.ifBlank { user?.profileImageUrl ?: modalSavedLocally }
                            ?: user?.profileImageUrl
                            ?: modalSavedLocally
                        val modalAvatarModel = getFullProfileImageUrl(modalRawProfileUrl)

                        // Profile avatar with camera upload badge (Exact fit white outline, no shadow)
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clickable { profileImagePicker.launch("image/*") }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.5.dp, Color.White, CircleShape)
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (modalAvatarModel != null) {
                                    coil.compose.SubcomposeAsyncImage(
                                        model = modalAvatarModel,
                                        contentDescription = "Profile Photo",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        error = {
                                            val userInitial = (customUserNameState.value.trim().firstOrNull() ?: 'P').uppercaseChar().toString()
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    text = userInitial,
                                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                                                    fontWeight = FontWeight.Black,
                                                    color = PrimaryGreen
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    val userInitial = (customUserNameState.value.trim().firstOrNull() ?: 'P').uppercaseChar().toString()
                                    Text(
                                        text = userInitial,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryGreen
                                    )
                                }
                            }
                            // Camera badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Upload Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
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
                                // Block spaces and special chars — only letters, digits, _ and . (max 10 chars)
                                val filtered = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(10)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pure White Cancel Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                showSignOutConfirmationModal = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D2939)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCFD4DC)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF1D2939), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Red Sign Out Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                showSignOutConfirmationModal = false
                                authViewModel.logout(context)
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncorrectRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                dismissButton = null,
                shape = RoundedCornerShape(26.dp),
                containerColor = Color.White
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
