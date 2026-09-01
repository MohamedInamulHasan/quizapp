package com.ilygames.quizapp.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.model.Question
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Global Shared State for Passages, Rewards & Question Limit across screens
var globalPassagesList = mutableStateListOf<StudyArticle>()
var globalRewardTitle = mutableStateOf("")
var globalRewardDescription = mutableStateOf("")
var globalRewardImageUrl = mutableStateOf<String?>(null)
var globalQuizQuestionLimit = mutableStateOf(20)
var globalQuizTimerSeconds = mutableStateOf(20)
var globalQuizChances = mutableStateOf(5)
private var isDataLoadedFromPrefs = false

@Composable
fun defaultAdminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF255FF4),
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedLabelColor = Color(0xFF255FF4),
    unfocusedLabelColor = TextMuted,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    cursorColor = Color(0xFF255FF4)
)

fun loadPersistedAdminData(context: Context) {
    if (isDataLoadedFromPrefs) return
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)

    // Load Quiz Question Limit, Timer Seconds & Chances (Lives)
    val savedLimit = prefs.getInt("quiz_questions_limit_key", 20)
    val savedTimer = prefs.getInt("quiz_timer_seconds_key", 20)
    val savedChances = prefs.getInt("quiz_chances_key", 5)
    globalQuizQuestionLimit.value = savedLimit
    globalQuizTimerSeconds.value = savedTimer
    globalQuizChances.value = savedChances

    // Load Saved Reward Image
    val savedRewardImg = prefs.getString("saved_reward_img_key", null)
    if (!savedRewardImg.isNullOrBlank()) {
        globalRewardImageUrl.value = savedRewardImg
    }

    // Load Saved Passages
    val passagesJson = prefs.getString("saved_passages_key", null)
    if (passagesJson != null) {
        try {
            val type = object : TypeToken<List<StudyArticle>>() {}.type
            val savedList: List<StudyArticle> = Gson().fromJson(passagesJson, type)
            globalPassagesList.clear()
            globalPassagesList.addAll(savedList)
        } catch (e: Exception) {
            globalPassagesList.clear()
        }
    } else {
        globalPassagesList.clear()
    }

    // Load Saved Rewards
    val savedTitle = prefs.getString("saved_reward_title_key", null)
    val savedDesc = prefs.getString("saved_reward_desc_key", null)
    if (!savedTitle.isNullOrBlank()) {
        globalRewardTitle.value = savedTitle
    }
    if (!savedDesc.isNullOrBlank()) {
        globalRewardDescription.value = savedDesc
    }

    // Sync active daily reward from backend for all users
    kotlinx.coroutines.MainScope().launch {
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

    isDataLoadedFromPrefs = true
}

fun saveQuizSettingsToPrefs(context: Context, limit: Int, timerSeconds: Int, chances: Int = 5) {
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("quiz_questions_limit_key", limit)
        .putInt("quiz_timer_seconds_key", timerSeconds)
        .putInt("quiz_chances_key", chances)
        .apply()
    globalQuizQuestionLimit.value = limit
    globalQuizTimerSeconds.value = timerSeconds
    globalQuizChances.value = chances
}

fun savePassagesToPrefs(context: Context) {
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(globalPassagesList.toList())
    prefs.edit().putString("saved_passages_key", json).apply()
}



fun saveRewardToPrefs(context: Context, title: String, desc: String, imgUrl: String? = globalRewardImageUrl.value, token: String = "") {
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("saved_reward_title_key", title)
        .putString("saved_reward_desc_key", desc)
        .putString("saved_reward_img_key", imgUrl ?: "")
        .apply()
    globalRewardTitle.value = title
    globalRewardDescription.value = desc
    globalRewardImageUrl.value = imgUrl

    if (token.isNotBlank()) {
        kotlinx.coroutines.MainScope().launch {
            try {
                com.ilygames.quizapp.data.api.ApiClient.apiService.publishReward(
                    token,
                    com.ilygames.quizapp.data.api.RewardSyncRequest(title, desc, imgUrl)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// Simple Clean Image Picker
@Composable
fun ProfessionalImageDropzone(
    title: String = "Question Image",
    subtitle: String = "Pick photo or paste URL",
    currentImageUrl: String?,
    onPickGallery: () -> Unit,
    onUrlChange: (String) -> Unit,
    onClearImage: () -> Unit
) {
    var showUrlInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentImageUrl == "uploading...") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Color(0xFF255FF4), modifier = Modifier.size(24.dp))
                    Text("Uploading image...", fontSize = 12.sp, color = Color(0xFF255FF4), fontWeight = FontWeight.Bold)
                }
            }
        } else if (!currentImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentImageUrl,
                contentDescription = "Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101828))
                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickGallery,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick Gallery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { showUrlInput = !showUrlInput },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF255FF4)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF255FF4), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showUrlInput) "Hide URL" else "Paste URL", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (showUrlInput) {
            OutlinedTextField(
                value = currentImageUrl ?: "",
                onValueChange = onUrlChange,
                label = { Text("Direct Image URL", color = Color(0xFF255FF4), fontSize = 11.sp) },
                placeholder = { Text("https://example.com/photo.jpg", fontSize = 11.sp, color = TextMuted) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF255FF4),
                    cursorColor = Color(0xFF255FF4)
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// Smart Bulletproof Bulk Question Parser Function
fun parseBulkQuestionsText(rawText: String, defaultCategory: String = "Passage Study"): List<Question> {
    val result = mutableListOf<Question>()
    val blocks = rawText.split(Regex("(?i)(?:\\*\\*)*Question\\s*\\d+:?(?:\\*\\*)*"))
    for (block in blocks) {
        if (block.isBlank()) continue

        val cleanBlock = block.trim()

        // Extract image URL if present (Image: ..., Img: ..., Pic: ..., Url: ..., Photo: ...)
        val imgMatch = Regex("(?i)(?:Image|Img|Pic|Photo|Url|Link)[:\\*\\s]*(https?://[^\\s\\n]+)").find(cleanBlock)
            ?: Regex("(https?://[^\\s\\n]+\\.(?:jpg|jpeg|png|webp|gif|svg)(?:\\?[^\\s\\n]*)?)", RegexOption.IGNORE_CASE).find(cleanBlock)
        val extractedImgUrl = imgMatch?.groupValues?.get(1)?.trim()

        val questionMatch = Regex("(?s)^(.*?)(?=[\\*]*\\bA[:\\)])").find(cleanBlock)
        var qText = questionMatch?.groupValues?.get(1)?.trim()
            ?.replace(Regex("^[*\\s:]+"), "")
            ?.replace(Regex("[*\\s:]+$"), "")?.trim() ?: ""

        // Clean out any Image: ... line from question text
        if (extractedImgUrl != null) {
            qText = qText.replace(Regex("(?i)(?:Image|Img|Pic|Photo|Url|Link)[:\\*\\s]*" + Regex.escape(extractedImgUrl)), "")
                .replace(extractedImgUrl, "")
                .trim()
                .replace(Regex("^[*\\s:]+"), "")
                .replace(Regex("[*\\s:]+$"), "").trim()
        }

        val optAMatch = Regex("(?s)[\\*]*\\bA[:\\)]\\s*(.*?)(?=[\\*]*\\bB[:\\)])").find(cleanBlock)
        val optA = optAMatch?.groupValues?.get(1)?.trim()?.replace(Regex("[*]+"), "")?.trim() ?: ""

        val optBMatch = Regex("(?s)[\\*]*\\bB[:\\)]\\s*(.*?)(?=[\\*]*\\bC[:\\)])").find(cleanBlock)
        val optB = optBMatch?.groupValues?.get(1)?.trim()?.replace(Regex("[*]+"), "")?.trim() ?: ""

        val optCMatch = Regex("(?s)[\\*]*\\bC[:\\)]\\s*(.*?)(?=[\\*]*\\bD[:\\)])").find(cleanBlock)
        val optC = optCMatch?.groupValues?.get(1)?.trim()?.replace(Regex("[*]+"), "")?.trim() ?: ""

        val optDMatch = Regex("(?s)[\\*]*\\bD[:\\)]\\s*(.*?)(?=(?:[*\\s]|\\b)*(?:Correct|Answer))").find(cleanBlock)
        val optD = optDMatch?.groupValues?.get(1)?.trim()?.replace(Regex("[*]+"), "")?.trim() ?: ""

        val correctMatch = Regex("(?i)(?:Correct|Answer)[:\\*\\s]*([A-D])").find(cleanBlock)
        val correctAns = correctMatch?.groupValues?.get(1)?.uppercase() ?: "A"

        if (qText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
            result.add(
                Question(
                    id = null,
                    question = qText,
                    optionA = optA,
                    optionB = optB,
                    optionC = optC.ifBlank { "N/A" },
                    optionD = optD.ifBlank { "N/A" },
                    correctAnswer = correctAns,
                    category = defaultCategory,
                    difficulty = "medium",
                    imageUrl = if (!extractedImgUrl.isNullOrBlank()) extractedImgUrl else null
                )
            )
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeAdminScreen(
    token: String?,
    quizViewModel: QuizViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadPersistedAdminData(context)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Questions, 1: Passages, 2: Rewards, 3: Users

    val defaultAdminUsers = listOf(
        com.ilygames.quizapp.data.model.User(
            id = "admin_user_001",
            name = "Hasan",
            email = "mohamedinamulhasan0@gmail.com",
            coins = 100,
            isAdmin = true
        ),
        com.ilygames.quizapp.data.model.User(
            id = "admin_user_002",
            name = "Mohamed Inamul Hasan",
            email = "mphamedinamulhasan0@gmail.com",
            coins = 85,
            isAdmin = true
        ),
        com.ilygames.quizapp.data.model.User(
            id = "admin_user_003",
            name = "Nohamed Inamul Hasan",
            email = "nohamedinamulhasan0@gmail.com",
            coins = 60,
            isAdmin = true
        )
    )

    // Users State
    var usersList by remember { mutableStateOf<List<com.ilygames.quizapp.data.model.User>>(defaultAdminUsers) }
    var isLoadingUsers by remember { mutableStateOf(false) }

    // Quiz Question Limit & Timing Settings Modal State
    var showQuizSettingsModal by remember { mutableStateOf(false) }
    var tempLimitText by remember { mutableStateOf(globalQuizQuestionLimit.value.toString()) }
    var tempTimerText by remember { mutableStateOf(globalQuizTimerSeconds.value.toString()) }
    var tempChances by remember { mutableStateOf(globalQuizChances.value) }

    // Questions State
    var questionsStateList by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoadingQuestions by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<Question?>(null) }
    var showQuestionModal by remember { mutableStateOf(false) }
    var showBulkUploadModal by remember { mutableStateOf(false) }

    // Image Quiz vs Text Quiz Form Fields
    var isImageQuiz by remember { mutableStateOf(false) }
    var questionImageUrl by remember { mutableStateOf("") }

    var questionText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("General") }
    val dynamicOptions = remember { mutableStateListOf("", "") }
    var correctAnswer by remember { mutableStateOf("A") }
    var difficulty by remember { mutableStateOf("easy") }

    // Bulk Field
    var bulkTextRaw by remember { mutableStateOf("") }
    var bulkCategory by remember { mutableStateOf("Passage Study") }

    // Passages State
    var editingPassage by remember { mutableStateOf<StudyArticle?>(null) }
    var passageTitle by remember { mutableStateOf("") }
    var passageCategory by remember { mutableStateOf("GENERAL KNOWLEDGE") }
    var passageParagraph by remember { mutableStateOf("") }
    var showPassageModal by remember { mutableStateOf(false) }

    // Rewards State
    var inputRewardTitle by remember { mutableStateOf("") }
    var inputRewardDesc by remember { mutableStateOf("") }
    var inputRewardImgUrl by remember { mutableStateOf("") }
    var showRewardModal by remember { mutableStateOf(false) }
    var showResetScoresConfirmModal by remember { mutableStateOf(false) }

    // AI 16:9 Quiz Generator State
    var showAiGeneratorModal by remember { mutableStateOf(false) }
    var selectedAiCategory by remember { mutableStateOf("naruto") }
    var customAiQuery by remember { mutableStateOf("") }
    var aiQuestionCount by remember { mutableStateOf(5) }
    var isGeneratingAiQuizzes by remember { mutableStateOf(false) }

    // Helper: Upload image file to backend server → returns full http:// URL stored on server
    suspend fun uploadImageToServer(uri: Uri, prefix: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mimeType.contains("png") -> ".png"
                mimeType.contains("gif") -> ".gif"
                mimeType.contains("webp") -> ".webp"
                else -> ".jpg"
            }
            val byteArray = inputStream.readBytes()
            inputStream.close()
            val mediaType = mimeType.toMediaTypeOrNull()
            val requestBody = byteArray.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData(
                "image",
                "${prefix}_${System.currentTimeMillis()}$ext",
                requestBody
            )
            val resp = ApiClient.apiService.uploadImage(token ?: "", part)
            if (resp.isSuccessful) resp.body()?.imageUrl else null
        } catch (e: Exception) {
            null
        }
    }

    // Image Pickers
    val questionImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            questionImageUrl = "uploading..."
            coroutineScope.launch {
                val serverUrl = uploadImageToServer(pickedUri, "quiz_img")
                val finalUrl = serverUrl ?: com.ilygames.quizapp.utils.ImageStorageHelper.saveUriToInternalStorage(context, pickedUri, "quiz_img") ?: pickedUri.toString()
                questionImageUrl = finalUrl
                Toast.makeText(context, "✅ Image saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val rewardImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            coroutineScope.launch {
                val serverUrl = uploadImageToServer(pickedUri, "reward_img")
                val finalUrl = serverUrl ?: com.ilygames.quizapp.utils.ImageStorageHelper.saveUriToInternalStorage(context, pickedUri, "reward_img") ?: pickedUri.toString()
                inputRewardImgUrl = finalUrl
                globalRewardImageUrl.value = finalUrl
                saveRewardToPrefs(context, inputRewardTitle, inputRewardDesc, finalUrl)
                Toast.makeText(context, "🖼️ Reward Image Saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadExistingQuestions() {
        isLoadingQuestions = true
        coroutineScope.launch {
            try {
                // Fetch ALL questions directly from MongoDB database
                val resp = ApiClient.apiService.getAdminQuestions(token ?: "")
                if (resp.isSuccessful && resp.body() != null) {
                    questionsStateList = resp.body()!!
                } else {
                    questionsStateList = emptyList()
                }
            } catch (_: Exception) {
                questionsStateList = emptyList()
            } finally {
                isLoadingQuestions = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadExistingQuestions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Bar (3D BACK BUTTON & 3D ROYAL BLUE BADGE)
            val isDarkAdminBack = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val adminBackBg = if (isDarkAdminBack) Color(0xFF1C273A) else Color.White

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(6.dp, CircleShape)
                        .background(adminBackBg, CircleShape)
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkAdminBack) 0.35f else 0.9f),
                                    Color.Black.copy(alpha = if (isDarkAdminBack) 0.5f else 0.1f)
                                )
                            ),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDarkAdminBack) Color.White else Color(0xFF17181C))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color(0xFF255FF4).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF255FF4), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Admin Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Main Settings Icon for Quiz Question Limit Configuration
                val isDarkAdminSet = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                val adminSetBg = if (isDarkAdminSet) Color(0xFF1C273A) else Color.White

                IconButton(
                    onClick = {
                        SoundManager.playClickSound()
                        if (showQuizSettingsModal) {
                            showQuizSettingsModal = false
                        } else {
                            tempLimitText = globalQuizQuestionLimit.value.toString()
                            tempTimerText = globalQuizTimerSeconds.value.toString()
                            tempChances = globalQuizChances.value
                            showQuizSettingsModal = true
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, CircleShape)
                        .background(adminSetBg, CircleShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkAdminSet) 0.35f else 0.9f),
                                    Color.Black.copy(alpha = if (isDarkAdminSet) 0.5f else 0.1f)
                                )
                            ),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quiz Settings",
                        tint = if (isDarkAdminSet) Color.White else Color(0xFF17181C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Tab 3D Soft-Clay Selector Bar
            val isDarkTabBar = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val tabBarBg = if (isDarkTabBar) Color(0xFF1C273A) else Color.White

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .background(tabBarBg, RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkTabBar) 0.35f else 0.9f),
                                Color.Black.copy(alpha = if (isDarkTabBar) 0.5f else 0.08f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Questions", "Passages", "Rewards", "Users").forEachIndexed { idx, label ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF255FF4) else Color.Transparent)
                                .clickable {
                                    SoundManager.playClickSound()
                                    selectedTab = idx
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else (if (isDarkTabBar) Color.White else Color(0xFF17181C))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB 1: QUESTIONS MANAGER
            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Create Single Question Button (3D GLOSSY ROYAL BLUE)
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                editingQuestion = null
                                questionText = ""
                                dynamicOptions.clear()
                                dynamicOptions.addAll(listOf("", ""))
                                correctAnswer = "A"
                                isImageQuiz = false
                                questionImageUrl = ""
                                showQuestionModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        // Bulk Upload Button (3D GLOSSY ROYAL BLUE)
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                bulkTextRaw = ""
                                showBulkUploadModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Bulk", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bulk", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        // Delete All Button (3D GLOSSY RED)
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                token?.let { authToken ->
                                    coroutineScope.launch {
                                        try {
                                            val res = ApiClient.apiService.deleteAllQuestions(authToken)
                                            if (res.isSuccessful) {
                                                questionsStateList = emptyList()
                                                Toast.makeText(context, "🗑️ All Questions Wiped from MongoDB!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "❌ Wipe failed (${res.code()})", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ Wipe error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF991B1B))
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Wipe", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe All", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingQuestions) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    } else if (questionsStateList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Help, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Questions in Database!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Tap 'Bulk Upload' or 'New' above to add questions.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(questionsStateList) { q ->
                                val isDarkQCardAdmin = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                                val qCardBgAdmin = if (isDarkQCardAdmin) Color(0xFF1C273A) else Color.White

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(20.dp))
                                        .background(qCardBgAdmin, RoundedCornerShape(20.dp))
                                        .border(
                                            1.5.dp,
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = if (isDarkQCardAdmin) 0.35f else 0.9f),
                                                    Color.Black.copy(alpha = if (isDarkQCardAdmin) 0.5f else 0.08f)
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (!q.category.equals("GENERAL", ignoreCase = true) && q.category.isNotBlank()) {
                                                    Text(
                                                        text = q.category.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF255FF4)
                                                    )
                                                }
                                                if (!q.imageUrl.isNullOrBlank()) {
                                                    Surface(
                                                        color = Color(0xFF255FF4).copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "🖼️ IMAGE QUIZ",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF255FF4),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Edit Button
                                                IconButton(
                                                    onClick = {
                                                        SoundManager.playClickSound()
                                                        editingQuestion = q
                                                        questionText = q.question
                                                        categoryText = "General"
                                                        dynamicOptions.clear()
                                                        val opts = listOf(q.optionA, q.optionB, q.optionC, q.optionD).filter { it.isNotBlank() }
                                                        if (opts.size >= 2) {
                                                            dynamicOptions.addAll(opts)
                                                        } else {
                                                            dynamicOptions.addAll(listOf("", ""))
                                                        }
                                                        correctAnswer = q.correctAnswer
                                                        isImageQuiz = !q.imageUrl.isNullOrBlank()
                                                        questionImageUrl = q.imageUrl ?: ""
                                                        showQuestionModal = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF255FF4), modifier = Modifier.size(18.dp))
                                                }

                                                // Delete Button
                                                IconButton(
                                                    onClick = {
                                                        SoundManager.playClickSound()
                                                        val targetId = q.id
                                                        val targetQuestionText = q.question
                                                        if (!targetId.isNullOrBlank()) {
                                                            token?.let { authToken ->
                                                                coroutineScope.launch {
                                                                    try {
                                                                        val res = ApiClient.apiService.deleteQuestion(authToken, targetId)
                                                                        if (res.isSuccessful) {
                                                                            questionsStateList = questionsStateList.filter { item -> item.id != targetId }
                                                                            Toast.makeText(context, "🗑️ Question Deleted from MongoDB!", Toast.LENGTH_SHORT).show()
                                                                        } else {
                                                                            Toast.makeText(context, "❌ Delete failed (${res.code()})", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        Toast.makeText(context, "❌ Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            questionsStateList = questionsStateList.filter { item -> item.question != targetQuestionText }
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IncorrectRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (!q.imageUrl.isNullOrBlank()) {
                                            val imgRequest = coil.request.ImageRequest.Builder(context)
                                                .data(q.imageUrl)
                                                .crossfade(true)
                                                .build()

                                            coil.compose.SubcomposeAsyncImage(
                                                model = imgRequest,
                                                contentDescription = "Question Image Preview",
                                                contentScale = ContentScale.Crop,
                                                loading = {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                                    }
                                                },
                                                error = {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = IncorrectRed, modifier = Modifier.size(24.dp))
                                                            Text("Image Link Expired", fontSize = 9.sp, color = TextMuted)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color(0xFF101828))
                                                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        Text(
                                            text = q.question,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkQCardAdmin) Color.White else Color(0xFF17181C)
                                        )

                                        val opts = q.getOptionsList()
                                        val letterLabels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
                                        val optionsText = opts.mapIndexed { idx, opt -> "${letterLabels.getOrElse(idx) { (idx + 1).toString() }}: $opt" }.joinToString(" | ")

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "$optionsText\nCorrect Option: ${q.correctAnswer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkQCardAdmin) Color.White.copy(alpha = 0.95f) else Color(0xFF334155),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: PASSAGES MANAGER (3D GLOSSY BLUE BUTTON & 3D SOFT-CLAY CARDS)
            if (selectedTab == 1) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            editingPassage = null
                            passageTitle = ""
                            passageCategory = "GENERAL KNOWLEDGE"
                            passageParagraph = ""
                            showPassageModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Passage", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(globalPassagesList) { article ->
                            val isDarkPCardAdmin = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                            val pCardBgAdmin = if (isDarkPCardAdmin) Color(0xFF1C273A) else Color.White

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, RoundedCornerShape(20.dp))
                                    .background(pCardBgAdmin, RoundedCornerShape(20.dp))
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkPCardAdmin) 0.35f else 0.9f),
                                                Color.Black.copy(alpha = if (isDarkPCardAdmin) 0.5f else 0.08f)
                                            )
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = article.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF255FF4)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    SoundManager.playClickSound()
                                                    editingPassage = article
                                                    passageTitle = article.title
                                                    passageCategory = article.category
                                                    passageParagraph = article.paragraph
                                                    showPassageModal = true
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF255FF4), modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    SoundManager.playClickSound()
                                                    globalPassagesList.remove(article)
                                                    savePassagesToPrefs(context)
                                                    Toast.makeText(context, "🗑️ Passage Deleted!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IncorrectRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = article.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                        fontWeight = FontWeight.Black,
                                        color = if (isDarkPCardAdmin) Color.White else Color(0xFF17181C)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = article.paragraph,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.sp),
                                        color = if (isDarkPCardAdmin) Color.White.copy(alpha = 0.95f) else Color(0xFF334155),
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: REWARDS MANAGER (3D GLOSSY BLUE BUTTON & 3D SOFT-CLAY CARDS)
            if (selectedTab == 2) {
                val isRewardPublished = globalRewardTitle.value.isNotBlank()

                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            inputRewardTitle = globalRewardTitle.value
                            inputRewardDesc = globalRewardDescription.value
                            inputRewardImgUrl = globalRewardImageUrl.value ?: ""
                            showRewardModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Publish", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRewardPublished) "Edit Daily Prize" else "Publish New Reward", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isRewardPublished) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            item {
                                val isDarkRCardAdmin = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                                val rCardBgAdmin = if (isDarkRCardAdmin) Color(0xFF1C273A) else Color.White

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(20.dp))
                                        .background(rCardBgAdmin, RoundedCornerShape(20.dp))
                                        .border(
                                            1.5.dp,
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = if (isDarkRCardAdmin) 0.35f else 0.9f),
                                                    Color.Black.copy(alpha = if (isDarkRCardAdmin) 0.5f else 0.08f)
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DAILY REWARD PRIZE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF255FF4)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        SoundManager.playClickSound()
                                                        inputRewardTitle = globalRewardTitle.value
                                                        inputRewardDesc = globalRewardDescription.value
                                                        inputRewardImgUrl = globalRewardImageUrl.value ?: ""
                                                        showRewardModal = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF255FF4), modifier = Modifier.size(18.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        SoundManager.playClickSound()
                                                        globalRewardTitle.value = ""
                                                        globalRewardDescription.value = ""
                                                        globalRewardImageUrl.value = null
                                                        saveRewardToPrefs(context, "", "", null, token ?: "")
                                                        Toast.makeText(context, "🗑️ Reward Prize Removed!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IncorrectRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        if (!globalRewardImageUrl.value.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            AsyncImage(
                                                model = globalRewardImageUrl.value,
                                                contentDescription = "Reward Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = globalRewardTitle.value,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                            fontWeight = FontWeight.Black,
                                            color = if (isDarkRCardAdmin) Color.White else Color(0xFF17181C)
                                        )

                                        if (globalRewardDescription.value.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = globalRewardDescription.value,
                                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.sp),
                                                color = if (isDarkRCardAdmin) Color.White.copy(alpha = 0.95f) else Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No active reward published yet", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // TAB 4: USERS LIST & MANAGEMENT (TABLE FORMAT WITH DELETE ACTION)
            if (selectedTab == 3) {
                var userToDelete by remember { mutableStateOf<com.ilygames.quizapp.data.model.User?>(null) }

                LaunchedEffect(selectedTab) {
                    if (selectedTab == 3) {
                        isLoadingUsers = true
                        try {
                            val res = ApiClient.apiService.getAdminUsers(token ?: "")
                            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                                usersList = res.body()!!
                            } else {
                                usersList = defaultAdminUsers
                            }
                        } catch (_: Exception) {
                            usersList = defaultAdminUsers
                        }
                        isLoadingUsers = false
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDarkUTitleAdmin = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                        Text(
                            text = "Registered Users Directory (${usersList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkUTitleAdmin) Color.White else Color(0xFF17181C)
                        )
                        Button(
                            onClick = {
                                isLoadingUsers = true
                                coroutineScope.launch {
                                    try {
                                        val res = ApiClient.apiService.getAdminUsers(token ?: "")
                                        if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                                            usersList = res.body()!!
                                        } else {
                                            usersList = defaultAdminUsers
                                        }
                                    } catch (_: Exception) {
                                        usersList = defaultAdminUsers
                                    }
                                    isLoadingUsers = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingUsers) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF255FF4))
                        }
                    } else if (usersList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No registered users found", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val validUsers = usersList.filter { !it.name.isNullOrBlank() }.sortedBy { it.name ?: "" }
                            items(validUsers) { user ->
                                val isDarkUCardAdmin = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                                val uCardBgAdmin = if (isDarkUCardAdmin) Color(0xFF1C273A) else Color.White

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(18.dp))
                                        .background(uCardBgAdmin, RoundedCornerShape(18.dp))
                                        .border(
                                            1.5.dp,
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = if (isDarkUCardAdmin) 0.35f else 0.9f),
                                                    Color.Black.copy(alpha = if (isDarkUCardAdmin) 0.5f else 0.08f)
                                                )
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (!user.profileImageUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user.profileImageUrl,
                                                    contentDescription = "Profile",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, if (user.isAdmin == true) Color(0xFF255FF4) else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(
                                                            if (user.isAdmin == true) Color(0xFF255FF4).copy(alpha = 0.15f)
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                            CircleShape
                                                        )
                                                        .border(1.5.dp, if (user.isAdmin == true) Color(0xFF255FF4) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = (user.name?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 18.sp,
                                                        color = if (user.isAdmin == true) Color(0xFF255FF4) else (if (isDarkUCardAdmin) Color.White else Color(0xFF17181C))
                                                    )
                                                }
                                            }

                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = user.name ?: "Unknown",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 15.sp,
                                                        color = if (isDarkUCardAdmin) Color.White else Color(0xFF17181C)
                                                    )
                                                    if (user.isAdmin == true) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF255FF4).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "ADMIN",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFF255FF4),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = user.email ?: "",
                                                    fontSize = 12.sp,
                                                    color = if (isDarkUCardAdmin) Color.White.copy(alpha = 0.7f) else TextMuted
                                                )
                                            }
                                        }

                                        // Delete Icon Button (No Round Background Circle)
                                        IconButton(
                                            onClick = {
                                                SoundManager.playClickSound()
                                                userToDelete = user
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete User",
                                                tint = IncorrectRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Delete User Confirmation Modal (3D SOFT-CLAY CARD & 3D GLOSSY BUTTONS)
                    if (userToDelete != null) {
                        val targetUser = userToDelete!!
                        val isDarkDelUser = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                        val delUserCardBg = if (isDarkDelUser) Color(0xFF1C273A) else Color.White

                        AlertDialog(
                            onDismissRequest = { userToDelete = null },
                            containerColor = delUserCardBg,
                            titleContentColor = if (isDarkDelUser) Color.White else Color(0xFF17181C),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier.border(
                                1.5.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDarkDelUser) 0.35f else 0.9f),
                                        Color.Black.copy(alpha = if (isDarkDelUser) 0.5f else 0.08f)
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
                                            .size(36.dp)
                                            .background(IncorrectRed.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, IncorrectRed, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = IncorrectRed, modifier = Modifier.size(20.dp))
                                    }
                                    Text("Delete User Account?", fontWeight = FontWeight.Black, color = if (isDarkDelUser) Color.White else Color(0xFF17181C), fontSize = 18.sp)
                                }
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to permanently delete user \"${targetUser.name}\" (${targetUser.email ?: ""})? This action cannot be undone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkDelUser) Color.White.copy(alpha = 0.9f) else Color(0xFF334155)
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        SoundManager.playClickSound()
                                        coroutineScope.launch {
                                            try {
                                                val res = ApiClient.apiService.deleteUser(token ?: "", targetUser.id ?: "")
                                                if (res.isSuccessful) {
                                                    usersList = usersList.filter { it.id != targetUser.id }
                                                    Toast.makeText(context, "🗑️ User \"${targetUser.name}\" deleted!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "❌ Delete failed", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                userToDelete = null
                                            }
                                        }
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
                                    Text("Delete User", color = Color.White, fontWeight = FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        SoundManager.playClickSound()
                                        userToDelete = null
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
                                    Text("Cancel", color = Color.White, fontWeight = FontWeight.Black)
                                }
                            }
                        )
                    }
                }
            }
        }

        // BULK QUESTION UPLOAD MODAL (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showBulkUploadModal) {
            val isDarkBulkModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val bulkModalBg = if (isDarkBulkModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = { showBulkUploadModal = false },
                containerColor = bulkModalBg,
                titleContentColor = if (isDarkBulkModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkBulkModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkBulkModal) 0.5f else 0.08f)
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
                            text = "Bulk Question Upload",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkBulkModal) Color.White else Color(0xFF17181C),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                SoundManager.playClickSound()
                                showBulkUploadModal = false
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(bulkModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkBulkModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkBulkModal) 0.1f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkBulkModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Paste formatted text containing questions (e.g. Question 1: ..., A: ..., B: ..., C: ..., D: ..., Correct: B)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        OutlinedTextField(
                            value = bulkCategory,
                            onValueChange = { bulkCategory = it },
                            label = { Text("Questions Category", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold) },
                            placeholder = { Text("e.g. Naruto, General Knowledge", color = TextMuted.copy(alpha = 0.6f)) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkBulkModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF255FF4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = if (isDarkBulkModal) Color.White else Color(0xFF17181C),
                                unfocusedTextColor = if (isDarkBulkModal) Color.White else Color(0xFF17181C),
                                cursorColor = Color(0xFF255FF4)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bulkTextRaw,
                            onValueChange = { bulkTextRaw = it },
                            label = { Text("Paste Formatted Questions Text Here...", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold) },
                            placeholder = { Text("Question 1: ...\nA: ...\nB: ...\nC: ...\nD: ...\nCorrect: B", color = TextMuted.copy(alpha = 0.6f)) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkBulkModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Medium, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF255FF4),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = if (isDarkBulkModal) Color.White else Color(0xFF17181C),
                                unfocusedTextColor = if (isDarkBulkModal) Color.White else Color(0xFF17181C),
                                cursorColor = Color(0xFF255FF4)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (bulkTextRaw.isBlank()) return@Button
                            SoundManager.playClickSound()
                            val parsedQuestions = parseBulkQuestionsText(bulkTextRaw, bulkCategory)
                            if (parsedQuestions.isEmpty()) {
                                Toast.makeText(context, "No valid questions found in text. Check formatting!", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            token?.let { authToken ->
                                coroutineScope.launch {
                                    try {
                                        val res = ApiClient.apiService.bulkUploadQuestions(authToken, parsedQuestions)
                                        if (res.isSuccessful && res.body() != null) {
                                            questionsStateList = res.body()!! + questionsStateList
                                            Toast.makeText(context, "🎉 Saved ${res.body()!!.size} questions to MongoDB!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            questionsStateList = parsedQuestions + questionsStateList
                                            Toast.makeText(context, "🎉 Added ${parsedQuestions.size} questions!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        questionsStateList = parsedQuestions + questionsStateList
                                        Toast.makeText(context, "🎉 Added ${parsedQuestions.size} questions!", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        showBulkUploadModal = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
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
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PARSE & UPLOAD ALL TO DB", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            )
        }

        // QUESTION CREATOR / EDIT MODAL (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showQuestionModal) {
            val isDarkQModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val qModalBg = if (isDarkQModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = { showQuestionModal = false },
                containerColor = qModalBg,
                titleContentColor = if (isDarkQModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkQModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkQModal) 0.5f else 0.08f)
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
                            text = if (editingQuestion == null) "Create Quiz Question" else "Edit Quiz Question",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkQModal) Color.White else Color(0xFF17181C)
                        )
                        IconButton(
                            onClick = { showQuestionModal = false },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(qModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkQModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkQModal) 0.5f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkQModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // FORMAT SELECTOR
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = !isImageQuiz,
                                onClick = { isImageQuiz = false },
                                label = { Text("Text Quiz 📝", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isImageQuiz,
                                onClick = { isImageQuiz = true },
                                label = { Text("Image Quiz 🖼️", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isImageQuiz) {
                            ProfessionalImageDropzone(
                                title = "Question Photo",
                                subtitle = "Upload photo from gallery or enter image URL",
                                currentImageUrl = questionImageUrl,
                                onPickGallery = { questionImagePicker.launch("image/*") },
                                onUrlChange = { questionImageUrl = it },
                                onClearImage = { questionImageUrl = "" }
                            )
                        }

                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            label = { Text(if (isImageQuiz) "Question Text (Optional)" else "Question Text", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text(if (isImageQuiz) "Optional (e.g. Identify this picture)" else "Type question text here...", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Answer Choices", fontWeight = FontWeight.Bold, color = Color(0xFF255FF4), fontSize = 12.sp)
                        dynamicOptions.forEachIndexed { idx, optVal ->
                            val letterLabel = if (idx < 26) ('A' + idx).toString() else (idx + 1).toString()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = optVal,
                                    onValueChange = { newValue -> dynamicOptions[idx] = newValue },
                                    label = { Text("Option $letterLabel", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                    placeholder = { Text("Enter choice $letterLabel", color = TextMuted, fontSize = 11.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    colors = defaultAdminTextFieldColors(),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                if (dynamicOptions.size > 2 && idx >= 2) {
                                    IconButton(
                                        onClick = { dynamicOptions.removeAt(idx) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IncorrectRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { dynamicOptions.add("") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF255FF4)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF255FF4), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Option", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Text("Correct Answer Choice", fontWeight = FontWeight.Bold, color = Color(0xFF255FF4), fontSize = 12.sp)
                        val optionLetters = dynamicOptions.indices.map { idx -> if (idx < 26) ('A' + idx).toString() else (idx + 1).toString() }
                        val chunkedOptions = optionLetters.chunked(5)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunkedOptions.forEach { rowLetters ->
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowLetters.forEach { letter ->
                                        val isSelected = correctAnswer == letter
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .width(48.dp)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF255FF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .border(1.dp, if (isSelected) Color(0xFF255FF4) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                .clickable { correctAnswer = letter },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = letter,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanOptions = dynamicOptions.map { it.trim() }.filter { it.isNotBlank() }
                            val finalQuestionText = questionText.trim()
                            if (!isImageQuiz && finalQuestionText.isBlank() || cleanOptions.size < 2) {
                                Toast.makeText(context, "Please enter question text and at least 2 options!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val optA = cleanOptions.getOrElse(0) { "" }
                            val optB = cleanOptions.getOrElse(1) { "" }
                            val optC = cleanOptions.getOrElse(2) { "" }
                            val optD = cleanOptions.getOrElse(3) { "" }
                            val targetId = editingQuestion?.id

                            val newQ = Question(
                                id = targetId ?: "",
                                question = finalQuestionText,
                                optionA = optA,
                                optionB = optB,
                                optionC = optC,
                                optionD = optD,
                                options = cleanOptions,
                                correctAnswer = correctAnswer,
                                category = "General",
                                difficulty = difficulty,
                                imageUrl = if (isImageQuiz && questionImageUrl.isNotBlank()) questionImageUrl else null
                            )

                            if (editingQuestion != null && !targetId.isNullOrBlank()) {
                                questionsStateList = questionsStateList.map { if (it.id == targetId) newQ else it }
                            } else {
                                questionsStateList = listOf(newQ) + questionsStateList.filter { it.id != newQ.id }
                            }
                            showQuestionModal = false

                            token?.let { authToken ->
                                coroutineScope.launch {
                                    try {
                                        if (editingQuestion != null && !targetId.isNullOrBlank()) {
                                            val updated = ApiClient.apiService.updateQuestion(authToken, targetId, newQ)
                                            if (updated.isSuccessful && updated.body() != null) {
                                                val serverQ = updated.body()!!
                                                questionsStateList = questionsStateList.map { if (it.id == targetId) serverQ else it }
                                            }
                                        } else {
                                            val created = ApiClient.apiService.createQuestion(authToken, newQ)
                                            if (created.isSuccessful && created.body() != null) {
                                                val serverQ = created.body()!!
                                                questionsStateList = listOf(serverQ) + questionsStateList.filter { it.id != serverQ.id }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                            Toast.makeText(context, "🎉 Question Saved to MongoDB!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
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
                        Text("Save Question", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            )
        }

        // PASSAGES MODAL (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showPassageModal) {
            val isDarkPModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val pModalBg = if (isDarkPModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = { showPassageModal = false },
                containerColor = pModalBg,
                titleContentColor = if (isDarkPModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkPModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkPModal) 0.5f else 0.08f)
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
                            text = if (editingPassage != null) "Edit Study Passage" else "Add Study Passage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkPModal) Color.White else Color(0xFF17181C)
                        )
                        IconButton(
                            onClick = { showPassageModal = false },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(pModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkPModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkPModal) 0.5f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkPModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = passageCategory,
                            onValueChange = { passageCategory = it },
                            label = { Text("Passage Category", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. GENERAL KNOWLEDGE or SCIENCE", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkPModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = passageTitle,
                            onValueChange = { passageTitle = it },
                            label = { Text("Passage Title", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. The Wonders of Solar Energy", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkPModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = passageParagraph,
                            onValueChange = { passageParagraph = it },
                            label = { Text("Passage Reading Content", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("Type or paste reading passage paragraph text here...", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkPModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Medium, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (passageTitle.isBlank() || passageParagraph.isBlank()) return@Button
                            val target = editingPassage
                            val newArticle = StudyArticle(
                                id = target?.id ?: java.util.UUID.randomUUID().toString(),
                                title = passageTitle,
                                category = passageCategory,
                                readTime = target?.readTime ?: "2 min read",
                                paragraph = passageParagraph
                            )

                            if (target != null) {
                                val idx = globalPassagesList.indexOf(target)
                                if (idx != -1) {
                                    globalPassagesList[idx] = newArticle
                                }
                            } else {
                                globalPassagesList.add(newArticle)
                            }

                            savePassagesToPrefs(context)
                            showPassageModal = false
                            Toast.makeText(context, "📚 Passage Saved Successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Passage", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // QUIZ SETTINGS MODAL (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showQuizSettingsModal) {
            val isDarkSModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val sModalBg = if (isDarkSModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = { showQuizSettingsModal = false },
                containerColor = sModalBg,
                titleContentColor = if (isDarkSModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkSModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkSModal) 0.5f else 0.08f)
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
                            text = "Admin Quiz Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkSModal) Color.White else Color(0xFF17181C)
                        )
                        IconButton(
                            onClick = { showQuizSettingsModal = false },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(sModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkSModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkSModal) 0.5f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkSModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Question Timer (Seconds)", fontWeight = FontWeight.Bold, color = Color(0xFF255FF4), fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 20, 30, 45).forEach { preset ->
                                val isSelected = tempTimerText == preset.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) Color(0xFF255FF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .clickable { tempTimerText = preset.toString() }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${preset}s", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = tempTimerText,
                            onValueChange = { tempTimerText = it },
                            label = { Text("Custom Seconds per Question", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkSModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Questions Shown per Quiz", fontWeight = FontWeight.Bold, color = Color(0xFF255FF4), fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 20, 30, 50).forEach { preset ->
                                val isSelected = tempLimitText == preset.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) Color(0xFF255FF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .clickable { tempLimitText = preset.toString() }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$preset Qs", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = tempLimitText,
                            onValueChange = { tempLimitText = it },
                            label = { Text("Custom Question Limit", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkSModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                        Text("Reset User Scores", fontWeight = FontWeight.Bold, color = IncorrectRed, fontSize = 12.sp)

                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                showResetScoresConfirmModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset All User Scores to 0", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val limit = tempLimitText.toIntOrNull() ?: 20
                            val timerSec = tempTimerText.toIntOrNull() ?: 20
                            saveQuizSettingsToPrefs(context, limit, timerSec, globalQuizChances.value)
                            showQuizSettingsModal = false
                            Toast.makeText(context, "⚙️ Settings Saved! $limit Qs | ${timerSec}s timer", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF255FF4)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // CONFIRMATION POPUP FOR RESET ALL SCORES
        if (showResetScoresConfirmModal) {
            AlertDialog(
                onDismissRequest = { showResetScoresConfirmModal = false },
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Reset All Scores?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF101828)
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset all user scores to zero? This action will set everyone's score to 0 and clear leaderboard results.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475467)
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
                                showResetScoresConfirmModal = false
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

                        // Red Yes, Reset All Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                showResetScoresConfirmModal = false
                                token?.let { authToken ->
                                    coroutineScope.launch {
                                        try {
                                            val response = ApiClient.apiService.resetScores(authToken)
                                            if (response.isSuccessful) {
                                                quizViewModel?.loadLeaderboard(authToken, true)
                                                quizViewModel?.loadLeaderboard(authToken, false)
                                                Toast.makeText(context, "🔄 All user scores reset to 0!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to reset scores", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
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
                            Text("Yes, Reset All", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                dismissButton = null,
                shape = RoundedCornerShape(26.dp)
            )
        }

        // AI QUIZ GENERATOR MODAL
        if (showAiGeneratorModal) {
            AlertDialog(
                onDismissRequest = { if (!isGeneratingAiQuizzes) showAiGeneratorModal = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                            Text(
                                text = "AI Quiz Generator",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF101828)
                            )
                        }
                        IconButton(onClick = { if (!isGeneratingAiQuizzes) showAiGeneratorModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Type ANY topic prompt below (or pick a preset) to search the web, download images, auto-generate options A,B,C,D, and insert up to 100 questions into your database!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475467),
                            fontSize = 12.sp
                        )

                        Text("1. Type Any Topic / Keyword:", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)
                        OutlinedTextField(
                            value = customAiQuery,
                            onValueChange = { customAiQuery = it },
                            label = { Text("e.g. Marvel Superheroes, Car Logos, Attack on Titan", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF101828), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text("Or Pick a Preset Topic:", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)

                        // 2x2 Grid of Preset Cards
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val row1 = listOf("naruto" to "🍥 Naruto & Anime", "kollywood" to "🎬 Kollywood Stars")
                            val row2 = listOf("cartoons" to "📺 Cartoons", "sports" to "🏏 Sports & Cricket")

                            listOf(row1, row2).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowItems.forEach { (catKey, catLabel) ->
                                        val isSelected = customAiQuery.isBlank() && selectedAiCategory == catKey
                                        Surface(
                                            onClick = {
                                                customAiQuery = ""
                                                selectedAiCategory = catKey
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFFF8F9FA),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) PrimaryGreen else Color(0xFFE4E7EC)),
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(catLabel, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryGreen else Color(0xFF344054), fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        if (isGeneratingAiQuizzes) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                                border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryGreen,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.5.dp
                                    )
                                    Text(
                                        text = "✨ Generating $aiQuestionCount Image Quizzes...",
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Searching web, fetching official HD photos & uploading 16:9 widescreen Cloudinary CDN images...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475467),
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Text("2. Number of Quizzes to Generate:", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 20, 50, 100).forEach { cnt ->
                                val isSelected = aiQuestionCount == cnt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) PrimaryGreen else Color(0xFFF2F4F7))
                                        .clickable(enabled = !isGeneratingAiQuizzes) { aiQuestionCount = cnt }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$cnt Qs", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF344054), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            val prefs = context.getSharedPreferences("quiz_app_prefs", Context.MODE_PRIVATE)
                            val authToken = if (!token.isNullOrBlank()) token else (prefs.getString("auth_token", null) ?: "bypass_auth_token_123")
                            isGeneratingAiQuizzes = true
                            coroutineScope.launch {
                                try {
                                    val req = com.ilygames.quizapp.data.api.AiGenerateQuizRequest(
                                        category = selectedAiCategory,
                                        count = aiQuestionCount,
                                        customQuery = customAiQuery.trim()
                                    )
                                    val res = ApiClient.apiService.aiGenerateCategoryQuiz(authToken, req)
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        showAiGeneratorModal = false
                                        loadExistingQuestions()
                                        Toast.makeText(context, "✨ ${res.body()?.msg ?: "Created Image Quizzes!"}", Toast.LENGTH_LONG).show()
                                    } else {
                                        val errStr = res.errorBody()?.string()
                                        Toast.makeText(context, "Error (${res.code()}): ${res.body()?.msg ?: errStr ?: "Failed to generate AI quizzes"}", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingAiQuizzes = false
                                }
                            }
                        },
                        enabled = !isGeneratingAiQuizzes,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isGeneratingAiQuizzes) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                                Text("Generating $aiQuestionCount Image Quizzes...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate $aiQuestionCount Image Quizzes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            )
        }

        // REWARD PRIZE MODAL (3D SOFT-CLAY CARD & 3D ROYAL BLUE)
        if (showRewardModal) {
            val isDarkRModal = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
            val rModalBg = if (isDarkRModal) Color(0xFF1C273A) else Color.White

            AlertDialog(
                onDismissRequest = { showRewardModal = false },
                containerColor = rModalBg,
                titleContentColor = if (isDarkRModal) Color.White else Color(0xFF17181C),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDarkRModal) 0.35f else 0.9f),
                            Color.Black.copy(alpha = if (isDarkRModal) 0.5f else 0.08f)
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
                            text = if (globalRewardTitle.value.isNotBlank()) "Edit Daily Reward" else "Publish Daily Reward",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkRModal) Color.White else Color(0xFF17181C)
                        )
                        IconButton(
                            onClick = { showRewardModal = false },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(rModalBg, CircleShape)
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkRModal) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkRModal) 0.5f else 0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkRModal) Color.White else Color(0xFF17181C), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfessionalImageDropzone(
                            title = "Upload Reward Prize Photo",
                            subtitle = "Select image from gallery or enter image URL",
                            currentImageUrl = inputRewardImgUrl.ifBlank { globalRewardImageUrl.value },
                            onPickGallery = { rewardImagePicker.launch("image/*") },
                            onUrlChange = { inputRewardImgUrl = it },
                            onClearImage = { inputRewardImgUrl = "" }
                        )

                        OutlinedTextField(
                            value = inputRewardTitle,
                            onValueChange = { inputRewardTitle = it },
                            label = { Text("Prize Name / Title", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. Smart Thermal Water Bottle", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkRModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = inputRewardDesc,
                            onValueChange = { inputRewardDesc = it },
                            label = { Text("Prize Specifications & Details", color = Color(0xFF255FF4), fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. 500ml Stainless Steel with LED Display", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = if (isDarkRModal) Color.White else Color(0xFF17181C), fontWeight = FontWeight.Medium, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputRewardTitle.isNotBlank()) {
                                globalRewardTitle.value = inputRewardTitle
                                globalRewardDescription.value = inputRewardDesc
                                globalRewardImageUrl.value = inputRewardImgUrl.ifBlank { null }
                                saveRewardToPrefs(context, inputRewardTitle, inputRewardDesc, globalRewardImageUrl.value, token ?: "")
                                showRewardModal = false
                                Toast.makeText(context, "🏆 Today's Reward Prize Published!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "⚠️ Please enter a Prize Title!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
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
                        Text("Publish Prize", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            )
        }
    }
}
