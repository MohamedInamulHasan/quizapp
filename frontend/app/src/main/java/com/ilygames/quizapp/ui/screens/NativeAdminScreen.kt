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
var globalRewardTitle = mutableStateOf("Smart Temperature Water Bottle")
var globalRewardDescription = mutableStateOf("500ml Insulated Stainless Steel Smart Thermal Bottle with LED Temperature Display.")
var globalRewardImageUrl = mutableStateOf<String?>(null)
var globalQuizQuestionLimit = mutableStateOf(20)
var globalQuizTimerSeconds = mutableStateOf(20)
private var isDataLoadedFromPrefs = false

@Composable
fun defaultAdminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = PrimaryGreen.copy(alpha = 0.6f),
    focusedLabelColor = Color(0xFF0F7B52),
    unfocusedLabelColor = Color(0xFF0F7B52),
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    focusedPlaceholderColor = Color(0xFF475569),
    unfocusedPlaceholderColor = Color(0xFF475569),
    cursorColor = PrimaryGreen,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

fun loadPersistedAdminData(context: Context) {
    if (isDataLoadedFromPrefs) return
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)

    // Load Quiz Question Limit & Timer Seconds
    val savedLimit = prefs.getInt("quiz_questions_limit_key", 20)
    val savedTimer = prefs.getInt("quiz_timer_seconds_key", 20)
    globalQuizQuestionLimit.value = savedLimit
    globalQuizTimerSeconds.value = savedTimer

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
        globalPassagesList.addAll(studyArticlesList)
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

    isDataLoadedFromPrefs = true
}

fun saveQuizSettingsToPrefs(context: Context, limit: Int, timerSeconds: Int) {
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("quiz_questions_limit_key", limit)
        .putInt("quiz_timer_seconds_key", timerSeconds)
        .apply()
    globalQuizQuestionLimit.value = limit
    globalQuizTimerSeconds.value = timerSeconds
}

fun savePassagesToPrefs(context: Context) {
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(globalPassagesList.toList())
    prefs.edit().putString("saved_passages_key", json).apply()
}

fun saveRewardToPrefs(context: Context, title: String, desc: String, imgUrl: String? = globalRewardImageUrl.value) {
    val prefs = context.getSharedPreferences("admin_app_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("saved_reward_title_key", title)
        .putString("saved_reward_desc_key", desc)
        .putString("saved_reward_img_key", imgUrl ?: "")
        .apply()
    globalRewardTitle.value = title
    globalRewardDescription.value = desc
    globalRewardImageUrl.value = imgUrl
}

// Professional Reusable Image Dropzone Placeholder
@Composable
fun ProfessionalImageDropzone(
    title: String = "Upload Question Image",
    subtitle: String = "Tap to pick photo from gallery or enter direct URL",
    currentImageUrl: String?,
    onPickGallery: () -> Unit,
    onUrlChange: (String) -> Unit,
    onClearImage: () -> Unit
) {
    var showUrlInput by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.5.dp, Brush.linearGradient(colors = listOf(PrimaryGreen, EmeraldGlow))),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentImageUrl == "uploading...") {
                // Show upload progress spinner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryGreen.copy(alpha = 0.08f))
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(36.dp))
                        Text("Uploading to server...", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!currentImageUrl.isNullOrBlank()) {
                // Live Image Preview Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = currentImageUrl,
                        contentDescription = "Image Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Right Action Badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onPickGallery() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Change", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = IncorrectRed.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onClearImage() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                // Dropzone Upload Placeholder Cues
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Action Row: Gallery Upload vs URL Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Gallery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showUrlInput = !showUrlInput },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrimaryGreen),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = "URL", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showUrlInput) "Hide URL" else "Paste URL", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (showUrlInput) {
                OutlinedTextField(
                    value = currentImageUrl ?: "",
                    onValueChange = onUrlChange,
                    label = { Text("Direct Image URL (HTTP/HTTPS)") },
                    placeholder = { Text("https://example.com/image.jpg") },
                    singleLine = true,
                    colors = defaultAdminTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
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

    // Users State
    var usersList by remember { mutableStateOf<List<com.ilygames.quizapp.data.model.User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }

    // Quiz Question Limit & Timing Settings Modal State
    var showQuizSettingsModal by remember { mutableStateOf(false) }
    var tempLimitText by remember { mutableStateOf(globalQuizQuestionLimit.value.toString()) }
    var tempTimerText by remember { mutableStateOf(globalQuizTimerSeconds.value.toString()) }

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
    var categoryText by remember { mutableStateOf("Passage Study") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
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
    var inputRewardTitle by remember { mutableStateOf(globalRewardTitle.value) }
    var inputRewardDesc by remember { mutableStateOf(globalRewardDescription.value) }
    var inputRewardImgUrl by remember { mutableStateOf(globalRewardImageUrl.value ?: "") }

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
                val url = uploadImageToServer(pickedUri, "quiz_img")
                questionImageUrl = url ?: ""
                if (url == null) Toast.makeText(context, "❌ Upload failed. Check server connection.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "✅ Image uploaded to server!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val rewardImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            coroutineScope.launch {
                val url = uploadImageToServer(pickedUri, "reward_img")
                if (url != null) {
                    inputRewardImgUrl = url
                    globalRewardImageUrl.value = url
                    saveRewardToPrefs(context, inputRewardTitle, inputRewardDesc, url)
                    Toast.makeText(context, "🖼️ Reward Image Saved to Server!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "❌ Upload failed. Check server connection.", Toast.LENGTH_SHORT).show()
                }
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
                                isImageQuiz = false
                                questionImageUrl = ""
                                optionA = ""
                                optionB = ""
                                optionC = ""
                                optionD = ""
                                correctAnswer = "A"
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
                                                        categoryText = q.category
                                                        optionA = q.optionA
                                                        optionB = q.optionB
                                                        optionC = q.optionC
                                                        optionD = q.optionD
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

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "A: ${q.optionA} | B: ${q.optionB}\nC: ${q.optionC} | D: ${q.optionD}\nCorrect Option: ${q.correctAnswer}",
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

            // TAB 3: REWARDS MANAGER (REBUILT FROM SCRATCH WITH HIGH CONTRAST & HYPER DESIGN)
            if (selectedTab == 2) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = "Reward", tint = PrimaryGreen)
                                    Text(
                                        text = "DAILY REWARD MANAGER",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        inputRewardTitle = ""
                                        inputRewardDesc = ""
                                        inputRewardImgUrl = ""
                                        globalRewardTitle.value = "No Active Reward Today"
                                        globalRewardDescription.value = "Check back tomorrow for the next physical prize!"
                                        globalRewardImageUrl.value = null
                                        saveRewardToPrefs(context, globalRewardTitle.value, globalRewardDescription.value, null)
                                        Toast.makeText(context, "🗑️ Reward Reset!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Wipe Reward", tint = IncorrectRed)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                            // Professional Image Upload Dropzone
                            ProfessionalImageDropzone(
                                title = "Upload Today's Reward Prize Image",
                                subtitle = "Pick prize photo from device gallery or paste direct image URL",
                                currentImageUrl = inputRewardImgUrl.ifBlank { globalRewardImageUrl.value },
                                onPickGallery = { rewardImagePicker.launch("image/*") },
                                onUrlChange = {
                                    inputRewardImgUrl = it
                                    globalRewardImageUrl.value = it
                                },
                                onClearImage = {
                                    inputRewardImgUrl = ""
                                    globalRewardImageUrl.value = null
                                }
                            )

                            // Prize Title Input Field (High Contrast Deep Black Text in Light Mode)
                            OutlinedTextField(
                                value = inputRewardTitle,
                                onValueChange = { inputRewardTitle = it },
                                label = { Text("Prize Name / Title", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                                placeholder = { Text("e.g. Smart Thermal Water Bottle", color = TextMuted) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                colors = defaultAdminTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Prize Description & Specs Input Field
                            OutlinedTextField(
                                value = inputRewardDesc,
                                onValueChange = { inputRewardDesc = it },
                                label = { Text("Prize Specifications & Value", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                                placeholder = { Text("e.g. 500ml Insulated Stainless Steel with LED Temp Display", color = TextMuted) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                colors = defaultAdminTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(14.dp)
                            )

                            Button(
                                onClick = {
                                    if (inputRewardTitle.isNotBlank()) {
                                        globalRewardTitle.value = inputRewardTitle
                                        globalRewardDescription.value = inputRewardDesc
                                        globalRewardImageUrl.value = inputRewardImgUrl.ifBlank { null }
                                        saveRewardToPrefs(context, inputRewardTitle, inputRewardDesc, globalRewardImageUrl.value)
                                        Toast.makeText(context, "🏆 Today's Reward Prize Published!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "⚠️ Please enter a Prize Title!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("💾 PUBLISH TODAY'S REWARD PRIZE", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // TAB 4: USERS LIST
            if (selectedTab == 3) {
                LaunchedEffect(Unit) {
                    isLoadingUsers = true
                    try {
                        val res = ApiClient.apiService.getAdminUsers(token ?: "")
                        if (res.isSuccessful) usersList = res.body() ?: emptyList()
                    } catch (_: Exception) {}
                    isLoadingUsers = false
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Users (${usersList.size})",
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
                                        if (res.isSuccessful) usersList = res.body() ?: emptyList()
                                    } catch (_: Exception) {}
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

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingUsers) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    } else if (usersList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No users found", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(usersList.filter { !it.mobileNumber.isNullOrBlank() }.sortedBy { it.name ?: "" }) { user ->
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
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar circle
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

                                        Column(modifier = Modifier.weight(1f)) {
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
                                                text = user.mobileNumber ?: "",
                                                fontSize = 12.sp,
                                                color = TextMuted
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
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bulkTextRaw,
                            onValueChange = { bulkTextRaw = it },
                            label = { Text("Paste Formatted Questions Text Here...", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontSize = 14.sp),
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

        // SINGLE QUESTION MODAL (SUPPORTING PROFESSIONAL IMAGE QUIZ DROPZONE & HIGH CONTRAST TEXT)
        if (showQuestionModal) {
            AlertDialog(
                onDismissRequest = { showQuestionModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(28.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingQuestion != null) "Edit Question" else "Create Question",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
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
                        // QUIZ TYPE SELECTOR
                        Text("Select Quiz Type:", fontWeight = FontWeight.Black, color = PrimaryGreen, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = !isImageQuiz,
                                onClick = { isImageQuiz = false },
                                label = { Text("Word / Text Quiz", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isImageQuiz,
                                onClick = { isImageQuiz = true },
                                label = { Text("Image Quiz 🖼️", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isImageQuiz) {
                            ProfessionalImageDropzone(
                                title = "Question Image Upload",
                                subtitle = "Pick photo from gallery or enter direct image URL",
                                currentImageUrl = questionImageUrl,
                                onPickGallery = { questionImagePicker.launch("image/*") },
                                onUrlChange = { questionImageUrl = it },
                                onClearImage = { questionImageUrl = "" }
                            )
                        }

                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            label = { Text("Question Text", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = categoryText,
                            onValueChange = { categoryText = it },
                            label = { Text("Category", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = optionA,
                            onValueChange = { optionA = it },
                            label = { Text("Option A", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = optionB,
                            onValueChange = { optionB = it },
                            label = { Text("Option B", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = optionC,
                            onValueChange = { optionC = it },
                            label = { Text("Option C", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = optionD,
                            onValueChange = { optionD = it },
                            label = { Text("Option D", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text("Correct Answer Option:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("A", "B", "C", "D").forEach { opt ->
                                FilterChip(
                                    selected = correctAnswer == opt,
                                    onClick = { correctAnswer = opt },
                                    label = { Text("Opt $opt", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = PrimaryGreen
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (questionText.isBlank() || optionA.isBlank() || optionB.isBlank()) return@Button
                            val targetId = editingQuestion?.id
                            val newQ = Question(
                                id = targetId,
                                question = questionText,
                                optionA = optionA,
                                optionB = optionB,
                                optionC = optionC,
                                optionD = optionD,
                                correctAnswer = correctAnswer,
                                category = categoryText,
                                difficulty = difficulty,
                                imageUrl = if (isImageQuiz && questionImageUrl.isNotBlank()) questionImageUrl else null
                            )

                            // Always put saved/updated question at the TOP of the visible list
                            if (editingQuestion != null && targetId != null) {
                                // Remove old entry and push updated one to top
                                questionsStateList = listOf(newQ) + questionsStateList.filter { it.id != targetId }
                            } else {
                                // New question goes to top
                                questionsStateList = listOf(newQ) + questionsStateList
                            }

                            token?.let { authToken ->
                                coroutineScope.launch {
                                    try {
                                        if (editingQuestion != null && !targetId.isNullOrBlank()) {
                                            val updated = ApiClient.apiService.updateQuestion(authToken, targetId, newQ)
                                            // If server returns the updated question, use it (it'll have correct _id)
                                            if (updated.isSuccessful && updated.body() != null) {
                                                val serverQ = updated.body()!!
                                                questionsStateList = listOf(serverQ) + questionsStateList.filter { it.id != targetId }
                                            }
                                        } else {
                                            val created = ApiClient.apiService.createQuestion(authToken, newQ)
                                            // If server returns created question with real _id, put it at top
                                            if (created.isSuccessful && created.body() != null) {
                                                val serverQ = created.body()!!
                                                questionsStateList = listOf(serverQ) + questionsStateList.filter { it.id == null }
                                            }
                                        }
                                        // NOTE: No reload — edited question is already at top
                                    } catch (e: Exception) {}
                                }
                            }

                            showQuestionModal = false
                            Toast.makeText(context, "🎉 Question Saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save Question", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // STUDY PASSAGE MODAL (CREATE / EDIT PASSAGE)
        if (showPassageModal) {
            AlertDialog(
                onDismissRequest = { showPassageModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(28.dp),
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = passageCategory,
                            onValueChange = { passageCategory = it },
                            label = { Text("Passage Category", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = passageTitle,
                            onValueChange = { passageTitle = it },
                            label = { Text("Passage Title", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = passageParagraph,
                            onValueChange = { passageParagraph = it },
                            label = { Text("Passage Reading Content", color = PrimaryGreen, fontWeight = FontWeight.Bold) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(12.dp)
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save Passage", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ADMIN QUIZ TIMING & QUESTION LIMIT SETTINGS MODAL
        if (showQuizSettingsModal) {
            AlertDialog(
                onDismissRequest = { showQuizSettingsModal = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = PrimaryGreen
                            )
                            Text(
                                text = "Admin Quiz Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section 1: Timer per Question
                        Text(
                            text = "⏱️ QUESTION TIMER (SECONDS)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = PrimaryGreen,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 15, 20, 30, 45).forEach { preset ->
                                val isSelected = tempTimerText == preset.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            brush = if (isSelected) {
                                                Brush.linearGradient(colors = listOf(PrimaryGreen, EmeraldGlow))
                                            } else {
                                                Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricMint else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(50.dp)
                                        )
                                        .clickable { tempTimerText = preset.toString() }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${preset}s",
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tempTimerText,
                            onValueChange = { tempTimerText = it },
                            label = { Text("Custom Seconds per Question (e.g. 15, 30)", color = Color(0xFF0F7B52), fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        // Section 2: Questions Count per Quiz
                        Text(
                            text = "❓ QUESTIONS SHOWN PER QUIZ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = PrimaryGreen,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 15, 20, 30, 50).forEach { preset ->
                                val isSelected = tempLimitText == preset.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            brush = if (isSelected) {
                                                Brush.linearGradient(colors = listOf(PrimaryGreen, EmeraldGlow))
                                            } else {
                                                Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricMint else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(50.dp)
                                        )
                                        .clickable { tempLimitText = preset.toString() }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$preset Qs",
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tempLimitText,
                            onValueChange = { tempLimitText = it },
                            label = { Text("Custom Question Limit (e.g. 10, 20, 50)", color = Color(0xFF0F7B52), fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            colors = defaultAdminTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val limit = tempLimitText.toIntOrNull() ?: 20
                            val timerSec = tempTimerText.toIntOrNull() ?: 20
                            saveQuizSettingsToPrefs(context, limit, timerSec)
                            showQuizSettingsModal = false
                            Toast.makeText(context, "⚙️ Settings Saved! $limit Qs | ${timerSec}s timer", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save All Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
