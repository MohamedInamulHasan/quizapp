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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    quizViewModel: com.ilygames.quizapp.ui.viewmodel.QuizViewModel? = null,
    onStartQuiz: () -> Unit,
    onStartReadingQuiz: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAiStudio: () -> Unit = {},
    onNavigateToMemoryGame: () -> Unit = {},
    onNavigateToTicTacToe: () -> Unit = {},
    onNavigateToConnectFour: () -> Unit = {},
    onNavigateToDotsAndBoxes: () -> Unit = {},
    onNavigateToGuessWho: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onLogout: () -> Unit
) {
    val user by authViewModel.user.collectAsState()
    var showSettingsMenu by remember { mutableStateOf(false) }
    var lastSettingsClickTime by remember { mutableLongStateOf(0L) }
    var secretAdminTapCount by remember { mutableIntStateOf(0) }
    var lastSecretTapTime by remember { mutableLongStateOf(0L) }
    var isSecretAdminUnlocked by remember { mutableStateOf(false) }
    var showAdminPasswordModal by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var isAdminPassVisible by remember { mutableStateOf(false) }
    var adminPasswordError by remember { mutableStateOf("") }
    var showSignOutConfirmationModal by remember { mutableStateOf(false) }
    var showRewardShowcaseModal by remember { mutableStateOf(false) }
    var showEditNameModal by remember { mutableStateOf(false) }
    var showGPayProfileModal by remember { mutableStateOf(false) }
    var editNameError by remember { mutableStateOf("") }
    var isUpdatingName by remember { mutableStateOf(false) }

    var showAiQuizStudioModal by remember { mutableStateOf(false) }
    var aiPromptInput by remember { mutableStateOf("") }
    var selectedQuestionCount by remember { mutableIntStateOf(10) }
    var isGeneratingAiQuiz by remember { mutableStateOf(false) }
    var generatedCustomQuestions by remember { mutableStateOf<List<com.ilygames.quizapp.data.model.Question>?>(null) }
    var generatedQuizPrompt by remember { mutableStateOf("") }

    var customUserNameState = remember { mutableStateOf(user?.name ?: "Player") }
    var tempNameInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
            val token = authViewModel.getToken(context) ?: "default_admin_token"
            if (token.isNotBlank()) {
                quizViewModel?.loadLeaderboard(token, true)
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
            contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp)
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
                                text = "WELCOME",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
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
                            val isDarkSetBtn = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                            val setBtnBg = if (isDarkSetBtn) Color(0xFF1C273A) else Color.White

                            IconButton(
                                onClick = {
                                    SoundManager.playClickSound()
                                    val now = System.currentTimeMillis()
                                    if (now - lastSecretTapTime > 3000L) {
                                        secretAdminTapCount = 1
                                    } else {
                                        secretAdminTapCount++
                                    }
                                    lastSecretTapTime = now

                                    if (secretAdminTapCount >= 10) {
                                        secretAdminTapCount = 0
                                        showSettingsMenu = false
                                        adminPasswordInput = ""
                                        adminPasswordError = ""
                                        showAdminPasswordModal = true
                                        SoundManager.playCorrectSound()
                                    } else {
                                        if (secretAdminTapCount >= 6) {
                                            Toast.makeText(context, "${10 - secretAdminTapCount} more taps to access Admin Security...", Toast.LENGTH_SHORT).show()
                                        }
                                        if (now - lastSettingsClickTime > 250L) {
                                            lastSettingsClickTime = now
                                            showSettingsMenu = !showSettingsMenu
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(setBtnBg, CircleShape)
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkSetBtn) 0.35f else 0.9f),
                                                Color.Black.copy(alpha = if (isDarkSetBtn) 0.5f else 0.1f)
                                            )
                                        ),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (isDarkSetBtn) Color.White else Color(0xFF17181C),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            if (showSettingsMenu) {
                                Popup(
                                    alignment = Alignment.TopEnd,
                                    offset = androidx.compose.ui.unit.IntOffset(0, 160),
                                    onDismissRequest = {
                                        lastSettingsClickTime = System.currentTimeMillis()
                                        showSettingsMenu = false
                                    }
                                ) {
                                    val isDarkSettings = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                                    val settingsCardBg = if (isDarkSettings) Color(0xFF1C273A) else Color.White

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showSettingsMenu,
                                        enter = androidx.compose.animation.fadeIn(animationSpec = tween(220)) + androidx.compose.animation.expandVertically(animationSpec = tween(220)),
                                        exit = androidx.compose.animation.fadeOut(animationSpec = tween(220)) + androidx.compose.animation.shrinkVertically(animationSpec = tween(220))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(230.dp)
                                                .shadow(12.dp, RoundedCornerShape(22.dp))
                                                .background(settingsCardBg, RoundedCornerShape(22.dp))
                                                .border(
                                                    1.5.dp,
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = if (isDarkSettings) 0.35f else 0.9f),
                                                            Color.Black.copy(alpha = if (isDarkSettings) 0.5f else 0.08f)
                                                        )
                                                    ),
                                                    RoundedCornerShape(22.dp)
                                                )
                                        ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "App Settings",
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                                fontWeight = FontWeight.Black,
                                                color = if (isDarkSettings) Color.White else Color(0xFF17181C),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )



                                            // Sound FX Switch Row (3D ROYAL BLUE SWITCH)
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
                                                        tint = if (isDarkSettings) Color.White else Color(0xFF17181C),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (ThemeState.isSoundEnabled) "Sound FX On" else "Sound Muted",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkSettings) Color.White else Color(0xFF17181C),
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Switch(
                                                    checked = ThemeState.isSoundEnabled,
                                                    onCheckedChange = { ThemeState.setSoundEnabled(context, it) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = Color(0xFF255FF4),
                                                        uncheckedThumbColor = Color(0xFF255FF4),
                                                        uncheckedTrackColor = SurfaceGray
                                                    )
                                                )
                                            }

                                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                            // Admin Studio Option (Unlocked via 10-tap secret gesture or admin email)
                                            if (isSecretAdminUnlocked || user?.isAdmin == true || user?.email?.equals("mohamedinamulhasan0@gmail.com", ignoreCase = true) == true || user?.email?.equals("mohmaedinamulhasan0@gmail.com", ignoreCase = true) == true) {
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

                                            // New Gamer Profile Option
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
                                                        authViewModel.guestLogin(context)
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "New Profile",
                                                    tint = IncorrectRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "New Gamer Profile",
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
            }

            // 2. RESTRUCTURED SCORE & CHANCES CARD (Centered CHANCES title, matching heart emojis, Latest score above High score)
            item {
                val isDarkHeader = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                val scoreCardBg = if (isDarkHeader) Color(0xFF1C273A) else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp))
                        .background(scoreCardBg, RoundedCornerShape(24.dp))
                        .border(
                            1.5.dp,
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkHeader) 0.35f else 0.9f),
                                    Color.Black.copy(alpha = if (isDarkHeader) 0.5f else 0.08f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
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
                                color = if (isDarkHeader) Color.White else Color(0xFF17181C),
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

                        // RIGHT SIDE: LATEST SCORE & HIGH SCORE
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Animated LATEST SCORE counter ONLY when user clicks Home from Quiz Complete screen! HIGH SCORE is static!
                            val justCompleted = remember { quizViewModel?.consumeQuizJustCompleted() ?: false }

                            var startAnim by remember { mutableStateOf(false) }
                            LaunchedEffect(justCompleted) {
                                if (justCompleted) {
                                    startAnim = true
                                }
                            }

                            val targetTodayScore = user?.todayScore ?: 0
                            val targetHighScore = user?.highScore ?: 0

                            val animatedLatestScore by animateIntAsState(
                                targetValue = if (startAnim) targetTodayScore else (if (justCompleted) 0 else targetTodayScore),
                                animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                                label = "LatestScoreAnim"
                            )

                            // LATEST SCORE : 0 pts (White text)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "LATEST SCORE :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkHeader) Color.White else Color(0xFF17181C),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$animatedLatestScore pts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkHeader) Color.White else Color(0xFF17181C)
                                )
                            }

                            // HIGH SCORE : 380 pts (Bottom Line - Static, no animation)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "HIGH SCORE :",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF255FF4),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$targetHighScore pts",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF255FF4)
                                )
                            }
                        }
                    }
                }
            }

            // 3. 3D ROYAL BLUE DAILY QUIZ CHALLENGE CARD WITH 3D GLOSSY WHITE BUTTON
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(26.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color(0xFF386DF5).copy(alpha = 0.3f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
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

                        // 3D GLOSSY WHITE START QUIZ BUTTON
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(50.dp))
                                .background(Color.White, RoundedCornerShape(50.dp))
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFE2E8F0)
                                        )
                                    ),
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(vertical = 13.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (dailyAttemptsLeft > 0) "START QUIZ NOW" else "WATCH AD",
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0B46DA)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (dailyAttemptsLeft > 0) Icons.Default.PlayArrow else Icons.Default.OndemandVideo,
                                    contentDescription = "Action",
                                    tint = Color(0xFF0B46DA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. UNIFIED 2x2 FEATURE GRID (3D ROYAL BLUE STYLING)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "EXPLORE & REWARDS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF255FF4),
                        letterSpacing = 1.sp
                    )

                    // Row 1: Passage Study & Leaderboard (Next to each other!)
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

                    // Row 2: Today's Reward & Extra Chance (Next to each other!)
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
                            description = "Watch ad to restore 1 broken heart",
                            icon = Icons.Default.Favorite,
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
                                            Toast.makeText(context, "❤️ Heart Restored!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "❤️ You already have max hearts!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // 5. 2 PLAYER GAMES SECTION (MATCHING EXPLORE & REWARDS SPACING)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "2 PLAYER GAMES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF255FF4),
                        letterSpacing = 1.sp
                    )

                    // 2 Player Games Grid Row 1: Memory Match & X | O Showdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UnifiedEmeraldCard(
                            title = "Memory Match",
                            description = "Flip cards & match pairs to win",
                            icon = Icons.Default.Style,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onNavigateToMemoryGame()
                            }
                        )

                        UnifiedEmeraldCard(
                            title = "X | O Showdown",
                            description = "Classic 2 Player Tic Tac Toe game",
                            icon = Icons.Default.Close,
                            customIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("X", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text("|", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 1.dp))
                                    Text("O", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onNavigateToTicTacToe()
                            }
                        )
                    }

                    // 2 Player Games Grid Row 2: Connect 4 & Dots & Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UnifiedEmeraldCard(
                            title = "Connect 4",
                            description = "Drop discs to connect 4 in a row",
                            icon = Icons.Default.Lens,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onNavigateToConnectFour()
                            }
                        )

                        UnifiedEmeraldCard(
                            title = "Dots & Boxes",
                            description = "Connect lines & claim boxes to win",
                            icon = Icons.Default.GridOn,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onNavigateToDotsAndBoxes()
                            }
                        )
                    // 2 Player Games Grid Row 3: Guess Who?
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UnifiedEmeraldCard(
                            title = "Guess Who?",
                            description = "Secret character deduction game",
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundManager.playClickSound()
                                onNavigateToGuessWho()
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // TODAY'S REWARD POPUP CARD (ULTRA-EXCITING 3D VICTORY CARD STYLING)
        if (showRewardShowcaseModal) {
            val isDarkRewModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val rewModalBg = if (isDarkRewModal) Color(0xFF1E293B) else Color.White
            val textColorRew = if (isDarkRewModal) Color.White else Color(0xFF0F172A)
            val subTextColorRew = if (isDarkRewModal) Color.White.copy(alpha = 0.75f) else Color(0xFF64748B)

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showRewardShowcaseModal = false },
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
                            .shadow(24.dp, RoundedCornerShape(32.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isDarkRewModal) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    else listOf(Color.White, Color(0xFFF1F5F9))
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(
                                2.5.dp,
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF59E0B), Color(0xFFEAB308), Color(0xFF255FF4))
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
                            // Top Floating Gift Badge
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .shadow(12.dp, CircleShape)
                                    .background(
                                        Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎁", fontSize = 40.sp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "TODAY'S EXCLUSIVE REWARD",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = textColorRew,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // REWARD IMAGE (WITHOUT BACKGROUND CONTAINER / WITHOUT BORDER BOX)
                            if (!globalRewardImageUrl.value.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = globalRewardImageUrl.value,
                                    contentDescription = "Reward Prize Image",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Reward Prize",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(90.dp)
                                )
                            }

                            val hasActiveReward = !globalRewardTitle.value.isNullOrBlank()

                            Spacer(modifier = Modifier.height(14.dp))

                            // TITLE & DESCRIPTION
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (hasActiveReward) globalRewardTitle.value else "No Active Reward Today",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = textColorRew
                                )

                                Text(
                                    text = if (hasActiveReward) globalRewardDescription.value else "Check back tomorrow for exciting physical rewards and daily prizes!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = subTextColorRew,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 3D Action Button
                            Button(
                                onClick = {
                                    SoundManager.playClickSound()
                                    showRewardShowcaseModal = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("AWESOME!", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // GPay-Style Profile & Account Modal (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showGPayProfileModal) {
            val isDarkProfileModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val profileModalBg = if (isDarkProfileModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = { showGPayProfileModal = false },
                containerColor = profileModalBg,
                titleContentColor = if (isDarkProfileModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkProfileModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkProfileModal) 0.5f else 0.08f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                ),
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
                            color = Color(0xFF255FF4)
                        )
                        IconButton(
                            onClick = { showGPayProfileModal = false },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(profileModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkProfileModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkProfileModal) 0.5f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkProfileModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
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

                        // Profile avatar with camera upload badge
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
                                    .background(Color(0xFF255FF4).copy(alpha = 0.15f)),
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
                                                    color = Color(0xFF255FF4)
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
                                        color = Color(0xFF255FF4)
                                    )
                                }
                            }
                            // Camera badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF255FF4))
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

                        // Editable name
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
                                color = if (isDarkProfileModal) Color.White else Color(0xFF17181C)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = Color(0xFF255FF4),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tap caption
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            )
        }

        // Edit Player Username Modal (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showEditNameModal) {
            val isDarkEditNameModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val editNameModalBg = if (isDarkEditNameModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = {
                    showEditNameModal = false
                    editNameError = ""
                },
                containerColor = editNameModalBg,
                titleContentColor = if (isDarkEditNameModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkEditNameModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkEditNameModal) 0.5f else 0.08f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                ),
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
                            color = if (isDarkEditNameModal) Color.White else Color(0xFF17181C)
                        )
                        IconButton(
                            onClick = {
                                showEditNameModal = false
                                editNameError = ""
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(editNameModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkEditNameModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkEditNameModal) 0.5f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkEditNameModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
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
                                val filtered = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
                                tempNameInput = filtered
                                editNameError = ""
                            },
                            label = { Text("New Username", color = Color(0xFF255FF4)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = if (isDarkEditNameModal) Color.White else Color(0xFF17181C),
                                fontSize = 16.sp
                            ),
                            isError = editNameError.isNotBlank(),
                            supportingText = if (editNameError.isNotBlank()) {
                                { Text(editNameError, color = IncorrectRed) }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF255FF4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                errorBorderColor = IncorrectRed,
                                focusedLabelColor = Color(0xFF255FF4),
                                unfocusedLabelColor = TextMuted,
                                cursorColor = Color(0xFF255FF4),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                focusedTextColor = if (isDarkEditNameModal) Color.White else Color(0xFF17181C),
                                unfocusedTextColor = if (isDarkEditNameModal) Color.White else Color(0xFF17181C)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        if (isUpdatingName) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Username", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            )
        }

        // Sign Out Confirmation Modal Popup
        if (showSignOutConfirmationModal) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirmationModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(26.dp),
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
                        fontWeight = FontWeight.Medium,
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
                        // Cancel Button (High contrast container in Light & Dark mode)
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                showSignOutConfirmationModal = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                dismissButton = null
            )
        }

        // 3D ADMIN SECURITY PASSWORD VERIFICATION MODAL
        if (showAdminPasswordModal) {
            val isDarkPassModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val passModalBg = if (isDarkPassModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = {
                    showAdminPasswordModal = false
                    adminPasswordInput = ""
                    adminPasswordError = ""
                },
                containerColor = passModalBg,
                titleContentColor = if (isDarkPassModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkPassModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkPassModal) 0.5f else 0.08f)
                        )
                    ),
                    RoundedCornerShape(26.dp)
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(4.dp, CircleShape)
                                .background(Color(0xFF255FF4).copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, Color(0xFF255FF4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Security", tint = Color(0xFF255FF4), modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "Admin Verification",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (isDarkPassModal) Color.White else Color(0xFF17181C)
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Enter Admin Security Passcode to access Admin Studio:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkPassModal) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)
                        )

                        OutlinedTextField(
                            value = adminPasswordInput,
                            onValueChange = {
                                adminPasswordInput = it
                                adminPasswordError = ""
                            },
                            placeholder = { Text("Enter Passcode...", color = Color.White.copy(alpha = 0.6f)) },
                            singleLine = true,
                            visualTransformation = if (isAdminPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isAdminPassVisible = !isAdminPassVisible }) {
                                    Icon(
                                        imageVector = if (isAdminPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF255FF4),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF121B2B), RoundedCornerShape(14.dp))
                        )

                        if (adminPasswordError.isNotBlank()) {
                            Text(
                                text = adminPasswordError,
                                color = IncorrectRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            if (adminPasswordInput.trim() == "Moh@2004") {
                                isSecretAdminUnlocked = true
                                showAdminPasswordModal = false
                                adminPasswordInput = ""
                                adminPasswordError = ""
                                SoundManager.playSuccessChime()
                                Toast.makeText(context, "👑 Admin Passcode Verified! Opening Admin Studio...", Toast.LENGTH_LONG).show()
                                onNavigateToAdmin()
                            } else {
                                SoundManager.playWrongSound()
                                adminPasswordError = "❌ Incorrect Admin Passcode"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))),
                                RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Text("Unlock Studio", color = Color.White, fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            showAdminPasswordModal = false
                            adminPasswordInput = ""
                            adminPasswordError = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFB91C1C))),
                                RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            )
        }

        // ── AI QUIZ STUDIO MODAL DIALOG ──────────────────────────────────────
        if (showAiQuizStudioModal) {
            val isDarkModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val modalBg = if (isDarkModal) Color(0xFF1C273A) else Color.White
            val modalTextColor = if (isDarkModal) Color.White else Color(0xFF17181C)

            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    showAiQuizStudioModal = false
                    aiPromptInput = ""
                    generatedCustomQuestions = null
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = modalBg,
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkModal) 0.35f else 0.9f),
                                Color.Black.copy(alpha = if (isDarkModal) 0.5f else 0.08f)
                            )
                        )
                    ),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Bar: Header + Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(
                                            Brush.radialGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = "✨ AI Quiz Studio",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = modalTextColor
                                )
                            }

                            IconButton(
                                onClick = {
                                    SoundManager.playClickSound()
                                    showAiQuizStudioModal = false
                                    aiPromptInput = ""
                                    generatedCustomQuestions = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = modalTextColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search Input Prompt Field
                        OutlinedTextField(
                            value = aiPromptInput,
                            onValueChange = { aiPromptInput = it },
                            placeholder = { Text("Search prompt e.g. Naruto, Tamil Nadu, Ronaldo...", fontSize = 13.sp, color = Color.Gray) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF255FF4)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = modalTextColor,
                                unfocusedTextColor = modalTextColor,
                                focusedBorderColor = Color(0xFF255FF4),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Select Question Count (10, 20, 30, 40, 50)
                        Text(
                            text = "SELECT NUMBER OF QUESTIONS:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF255FF4),
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(10, 20, 30, 40, 50).forEach { countOpt ->
                                val isSelected = selectedQuestionCount == countOpt
                                val pillBg = if (isSelected) Color(0xFF255FF4) else (if (isDarkModal) Color(0xFF101828) else Color(0xFFF1F5F9))
                                val pillText = if (isSelected) Color.White else (if (isDarkModal) Color.White.copy(alpha = 0.8f) else Color(0xFF475569))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .height(38.dp)
                                        .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(12.dp))
                                        .background(pillBg, RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.White else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            SoundManager.playClickSound()
                                            selectedQuestionCount = countOpt
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$countOpt",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = pillText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Generate Quiz Action Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                if (aiPromptInput.trim().isBlank()) {
                                    Toast.makeText(context, "Please enter a topic or prompt first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val activeToken = authViewModel.getToken(context) ?: "default_admin_token"
                                isGeneratingAiQuiz = true
                                generatedCustomQuestions = null
                                coroutineScope.launch {
                                    try {
                                        val response = com.ilygames.quizapp.data.api.ApiClient.apiService.generateCustomAiQuiz(
                                            activeToken,
                                            com.ilygames.quizapp.data.api.CustomAiQuizRequest(
                                                prompt = aiPromptInput.trim(),
                                                count = selectedQuestionCount
                                            )
                                        )
                                        isGeneratingAiQuiz = false
                                        if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                                            val body = response.body()!!
                                            if (!body.questions.isNullOrEmpty()) {
                                                generatedQuizPrompt = body.prompt ?: aiPromptInput.trim()
                                                generatedCustomQuestions = body.questions
                                                SoundManager.playCorrectSound()
                                                Toast.makeText(context, "🎯 ${body.questions.size} Questions Generated!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No questions generated. Try another topic!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Failed to generate AI quiz. Please try again!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        isGeneratingAiQuiz = false
                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isGeneratingAiQuiz,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        ) {
                            if (isGeneratingAiQuiz) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Text("⚡ GENERATE QUIZ WITH AI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }
                        }

                        // Generated Quiz Card Box (Appears when quiz is generated!)
                        if (generatedCustomQuestions != null) {
                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF255FF4).copy(alpha = 0.15f),
                                                Color(0xFF386DF5).copy(alpha = 0.25f)
                                            )
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(1.5.dp, Color(0xFF255FF4), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🎯 Quiz Ready: ${generatedQuizPrompt.uppercase()}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = modalTextColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🎮 ${generatedCustomQuestions!!.size} Questions • Practice Mode (No Score)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF255FF4)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            SoundManager.playClickSound()
                                            quizViewModel?.startCustomAiQuiz(generatedCustomQuestions!!)
                                            showAiQuizStudioModal = false
                                            aiPromptInput = ""
                                            generatedCustomQuestions = null
                                            onStartQuiz()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(vertical = 10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .shadow(6.dp, RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Text("▶️ PLAY NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
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
}

@Composable
fun UnifiedEmeraldCard(
    title: String,
    description: String,
    icon: ImageVector,
    customIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode

    // 3D Soft-Clay card: Pure White in light mode, Dark (#1C273A) in dark mode
    val cardBg = if (isDark) Color(0xFF1C273A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF17181C)
    val descColor = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF5A6E85)

    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .background(cardBg, RoundedCornerShape(22.dp))
            .border(
                1.5.dp,
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.9f),
                        Color.Black.copy(alpha = if (isDark) 0.5f else 0.08f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .bounceClick { onClick() }
            .padding(16.dp)
    ) {
        Column {
            // Glossy 3D Blue Sphere Icon Button (From your CSS radio button styling)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(6.dp, CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF386DF5),
                                Color(0xFF255FF4),
                                Color(0xFF0B46DA)
                            )
                        ),
                        CircleShape
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon != null) {
                    customIcon()
                } else {
                    Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Black,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                fontWeight = FontWeight.Medium,
                color = descColor
            )
        }
    }
}
