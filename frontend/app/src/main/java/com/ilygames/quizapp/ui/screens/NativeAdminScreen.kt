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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedLabelColor = PrimaryGreen,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    cursorColor = PrimaryGreen
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
                    CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(24.dp))
                    Text("Uploading image...", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        } else if (!currentImageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                AsyncImage(
                    model = currentImageUrl,
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickGallery,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
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
                border = BorderStroke(1.dp, PrimaryGreen),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showUrlInput) "Hide URL" else "Paste URL", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (showUrlInput) {
            OutlinedTextField(
                value = if (currentImageUrl == "uploading...") "" else (currentImageUrl ?: ""),
                onValueChange = onUrlChange,
                label = { Text("Direct Image URL", color = PrimaryGreen, fontSize = 11.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                colors = defaultAdminTextFieldColors(),
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

        val questionMatch = Regex("(?s)^(.*?)(?=[\\*]*\\bA[:\\)])").find(cleanBlock)
        val qText = questionMatch?.groupValues?.get(1)?.trim()
            ?.replace(Regex("^[*\\s:]+"), "")
            ?.replace(Regex("[*\\s:]+$"), "")?.trim() ?: ""

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
                    imageUrl = null
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
                // Use admin/questions: returns ALL questions with imageUrl, sorted newest first
                val resp = ApiClient.apiService.getAdminQuestions(token ?: "")
                if (resp.isSuccessful && resp.body() != null) {
                    questionsStateList = resp.body()!!
                }
            } catch (e: Exception) {
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
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(PrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Admin Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen
                    )
                }

                // Main Settings Icon for Quiz Question Limit Configuration
                IconButton(
                    onClick = {
                        SoundManager.playClickSound()
                        tempLimitText = globalQuizQuestionLimit.value.toString()
                        tempTimerText = globalQuizTimerSeconds.value.toString()
                        tempChances = globalQuizChances.value
                        showQuizSettingsModal = true
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quiz Settings",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Tab Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Questions", "Passages", "Rewards", "Users").forEachIndexed { idx, label ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryGreen else Color.Transparent)
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
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
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
                        // Create Single Question Button
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Bulk Upload Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                bulkTextRaw = ""
                                showBulkUploadModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .border(1.dp, PrimaryGreen, RoundedCornerShape(14.dp))
                                .weight(1.2f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Bulk", tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Bulk Upload", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Delete All Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                token?.let { authToken ->
                                    coroutineScope.launch {
                                        try {
                                            ApiClient.apiService.deleteAllQuestions(authToken)
                                            questionsStateList = emptyList()
                                            Toast.makeText(context, "🗑️ All Questions Wiped from DB!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            questionsStateList = emptyList()
                                            Toast.makeText(context, "Cleared questions!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Wipe", tint = IncorrectRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Wipe All", color = IncorrectRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
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
                                                Text(
                                                    text = q.category.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = PrimaryGreen
                                                )
                                                if (!q.imageUrl.isNullOrBlank()) {
                                                    Surface(
                                                        color = PrimaryGreen.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "🖼️ IMAGE QUIZ",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = PrimaryGreen,
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
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                                }

                                                // Delete Button
                                                IconButton(
                                                    onClick = {
                                                        SoundManager.playClickSound()
                                                        val targetId = q.id
                                                        val targetQuestionText = q.question
                                                        questionsStateList = questionsStateList.filter { item ->
                                                            if (!targetId.isNullOrBlank()) item.id != targetId else item.question != targetQuestionText
                                                        }
                                                        if (!targetId.isNullOrBlank()) {
                                                            token?.let { authToken ->
                                                                coroutineScope.launch {
                                                                    try {
                                                                        ApiClient.apiService.deleteQuestion(authToken, targetId)
                                                                        Toast.makeText(context, "🗑️ Question Deleted from DB!", Toast.LENGTH_SHORT).show()
                                                                    } catch (e: Exception) {
                                                                        Toast.makeText(context, "Question removed!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Question removed!", Toast.LENGTH_SHORT).show()
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
                                            AsyncImage(
                                                model = q.imageUrl,
                                                contentDescription = "Question Image Preview",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        Text(
                                            text = q.question,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        val opts = q.getOptionsList()
                                        val letterLabels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
                                        val optionsText = opts.mapIndexed { idx, opt -> "${letterLabels.getOrElse(idx) { (idx + 1).toString() }}: $opt" }.joinToString(" | ")

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "$optionsText\nCorrect Option: ${q.correctAnswer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: PASSAGES MANAGER
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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Passage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(globalPassagesList) { article ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
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
                                            color = PrimaryGreen
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
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
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
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = article.paragraph,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.sp),
                                        color = TextMuted,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: REWARDS MANAGER (Matching Questions & Passages Layout)
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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Publish", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRewardPublished) "Edit Daily Prize" else "Publish New Reward", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isRewardPublished) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
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
                                                color = PrimaryGreen
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
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        SoundManager.playClickSound()
                                                        inputRewardTitle = ""
                                                        inputRewardDesc = ""
                                                        inputRewardImgUrl = ""
                                                        globalRewardTitle.value = ""
                                                        globalRewardDescription.value = ""
                                                        globalRewardImageUrl.value = null
                                                        saveRewardToPrefs(context, "", "", null, token ?: "")
                                                        Toast.makeText(context, "🗑️ Reward Prize Deleted!", Toast.LENGTH_SHORT).show()
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (globalRewardDescription.value.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = globalRewardDescription.value,
                                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.sp),
                                                color = TextMuted
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
                        Text(
                            text = "Registered Users Directory (${usersList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingUsers) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
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
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
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
                                                        .border(1.5.dp, if (user.isAdmin == true) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(
                                                            if (user.isAdmin == true) PrimaryGreen.copy(alpha = 0.15f)
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                            CircleShape
                                                        )
                                                        .border(1.5.dp, if (user.isAdmin == true) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = (user.name?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 18.sp,
                                                        color = if (user.isAdmin == true) PrimaryGreen else MaterialTheme.colorScheme.onSurface
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
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (user.isAdmin == true) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = PrimaryGreen.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "ADMIN",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = PrimaryGreen,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = user.email ?: "",
                                                    fontSize = 12.sp,
                                                    color = TextMuted
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

                    // Delete User Confirmation Modal
                    if (userToDelete != null) {
                        val targetUser = userToDelete!!
                        AlertDialog(
                            onDismissRequest = { userToDelete = null },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(26.dp),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = IncorrectRed)
                                    Text("Delete User Account?", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to permanently delete user \"${targetUser.name}\" (${targetUser.email ?: ""})? This action cannot be undone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
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
                                    colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { userToDelete = null }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        }

        // BULK QUESTION UPLOAD MODAL
        if (showBulkUploadModal) {
            AlertDialog(
                onDismissRequest = { showBulkUploadModal = false },
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
                            text = "Bulk Question Text Upload",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { showBulkUploadModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
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
                            label = { Text("Questions Category", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bulkTextRaw,
                            onValueChange = { bulkTextRaw = it },
                            label = { Text("Paste Formatted Questions Text Here...", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            colors = defaultAdminTextFieldColors(),
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
                                            Toast.makeText(context, "🎉 Uploaded ${res.body()!!.size} questions to DB!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            questionsStateList = parsedQuestions + questionsStateList
                                            Toast.makeText(context, "🎉 Added ${parsedQuestions.size} questions!", Toast.LENGTH_SHORT).show()
                                        }
                                        showBulkUploadModal = false
                                        loadExistingQuestions()
                                    } catch (e: Exception) {
                                        questionsStateList = parsedQuestions + questionsStateList
                                        Toast.makeText(context, "🎉 Added ${parsedQuestions.size} questions!", Toast.LENGTH_SHORT).show()
                                        showBulkUploadModal = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PARSE & UPLOAD ALL TO DB", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            )
        }

        // QUESTION CREATOR / EDIT MODAL (NORMAL COMPACT FORM)
        if (showQuestionModal) {
            AlertDialog(
                onDismissRequest = { showQuestionModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingQuestion != null) "Edit Question" else "Create Question",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showQuestionModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
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
                            label = { Text(if (isImageQuiz) "Question Text (Optional)" else "Question Text", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text(if (isImageQuiz) "Optional (e.g. Identify this picture)" else "Type question text here...", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Answer Choices", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)
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
                                    label = { Text("Option $letterLabel", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
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
                            border = BorderStroke(1.dp, PrimaryGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Option", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Text("Correct Answer Choice", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)
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
                                                .background(if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .border(1.dp, if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
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

                            showQuestionModal = false

                            token?.let { authToken ->
                                coroutineScope.launch {
                                    try {
                                        if (editingQuestion != null && !targetId.isNullOrBlank()) {
                                            val updated = ApiClient.apiService.updateQuestion(authToken, targetId, newQ)
                                            if (updated.isSuccessful && updated.body() != null) {
                                                val serverQ = updated.body()!!
                                                questionsStateList = questionsStateList.map { if (it.id == targetId) serverQ else it }
                                            } else {
                                                loadExistingQuestions()
                                            }
                                        } else {
                                            val created = ApiClient.apiService.createQuestion(authToken, newQ)
                                            if (created.isSuccessful && created.body() != null) {
                                                val serverQ = created.body()!!
                                                questionsStateList = listOf(serverQ) + questionsStateList.filter { it.id != serverQ.id }
                                            } else {
                                                loadExistingQuestions()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        loadExistingQuestions()
                                    }
                                }
                            }
                            Toast.makeText(context, "🎉 Question Saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Question", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // PASSAGES MODAL (NORMAL COMPACT FORM)
        if (showPassageModal) {
            AlertDialog(
                onDismissRequest = { showPassageModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingPassage != null) "Edit Study Passage" else "Add Study Passage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showPassageModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
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
                            label = { Text("Passage Category", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. GENERAL KNOWLEDGE or SCIENCE", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = passageTitle,
                            onValueChange = { passageTitle = it },
                            label = { Text("Passage Title", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. The Wonders of Solar Energy", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = passageParagraph,
                            onValueChange = { passageParagraph = it },
                            label = { Text("Passage Reading Content", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("Type or paste reading passage paragraph text here...", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 13.sp),
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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Passage", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // QUIZ SETTINGS MODAL (NORMAL COMPACT FORM)
        if (showQuizSettingsModal) {
            AlertDialog(
                onDismissRequest = { showQuizSettingsModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Quiz Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showQuizSettingsModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
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
                        Text("Question Timer (Seconds)", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 20, 30, 45).forEach { preset ->
                                val isSelected = tempTimerText == preset.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
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
                            label = { Text("Custom Seconds per Question", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Questions Shown per Quiz", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 20, 30, 50).forEach { preset ->
                                val isSelected = tempLimitText == preset.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
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
                            label = { Text("Custom Question Limit", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
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
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Reset All Scores?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset all user scores to zero? This action will set everyone's score to 0 and clear leaderboard results.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            showResetScoresConfirmModal = false
                            token?.let { authToken ->
                                coroutineScope.launch {
                                    try {
                                        val response = ApiClient.apiService.resetScores(authToken)
                                        if (response.isSuccessful) {
                                            quizViewModel.loadLeaderboard(authToken, forceRefresh = true)
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
                        colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Yes, Reset All", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            showResetScoresConfirmModal = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0))
                    ) {
                        Text("Cancel", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // REWARD PRIZE MODAL (NORMAL COMPACT FORM)
        if (showRewardModal) {
            AlertDialog(
                onDismissRequest = { showRewardModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (globalRewardTitle.value.isNotBlank()) "Edit Daily Reward" else "Publish Daily Reward",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showRewardModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
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
                            label = { Text("Prize Name / Title", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. Smart Thermal Water Bottle", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = inputRewardDesc,
                            onValueChange = { inputRewardDesc = it },
                            label = { Text("Prize Specifications & Details", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            placeholder = { Text("e.g. 500ml Stainless Steel with LED Display", color = TextMuted, fontSize = 12.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 13.sp),
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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Publish Prize", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
