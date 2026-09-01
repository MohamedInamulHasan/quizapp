package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.api.CustomAiQuizRequest
import com.ilygames.quizapp.data.model.Question
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.ui.viewmodel.QuizViewModel
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.launch

@Composable
fun AiQuizStudioScreen(
    token: String,
    quizViewModel: QuizViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = ThemeState.isDarkMode

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    var aiPromptInput by remember { mutableStateOf("") }
    var selectedQuestionCount by remember { mutableIntStateOf(10) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedQuestions by remember { mutableStateOf<List<Question>?>(null) }
    var generatedPromptTitle by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Top Header with 3D Glossy Back Button ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Soft-Clay Glossy Circle Icon Button
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

                Spacer(modifier = Modifier.width(14.dp))

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
                    Column {
                        Text(
                            text = "AI Quiz Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            text = "Create & Practice Custom Quizzes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Main Card Container ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "ENTER TOPIC OR PROMPT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF255FF4),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prompt Input Search Bar (White Text & Multi-Line Wrapping up to 4 lines)
                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it },
                        placeholder = { Text("e.g. Sports, History, Cinema, Science, Technology...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)) },
                        singleLine = false,
                        maxLines = 4,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF386DF5)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF386DF5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Topic Suggestion Chips (Sports, History, Cinema, Science, Technology, Geography)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf("Sports ⚽", "History 🏛️", "Cinema 🎬", "Science 🔬", "Technology 💻", "Geography 🌍")) { tag ->
                            val cleanTag = tag.split(" ")[0]
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF255FF4).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFF386DF5).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        SoundManager.playClickSound()
                                        aiPromptInput = cleanTag
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = tag, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Select Number of Questions
                    Text(
                        text = "SELECT NUMBER OF QUESTIONS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF255FF4),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(10, 20, 30, 40, 50).forEach { countOpt ->
                            val isSelected = selectedQuestionCount == countOpt
                            val pillBg = if (isSelected) Color(0xFF255FF4) else (if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                            val pillText = if (isSelected) Color.White else (if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 3.dp)
                                    .height(42.dp)
                                    .shadow(if (isSelected) 6.dp else 0.dp, RoundedCornerShape(14.dp))
                                    .background(pillBg, RoundedCornerShape(14.dp))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        RoundedCornerShape(14.dp)
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
                                    fontSize = 14.sp,
                                    color = pillText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ⚡ GENERATE QUIZ WITH AI Button
                    Button(
                        onClick = {
                            SoundManager.playClickSound()
                            if (aiPromptInput.trim().isBlank()) {
                                Toast.makeText(context, "Please enter a topic or prompt first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val activeToken = if (token.isNotBlank()) token else authViewModel.getToken(context) ?: "default_token"
                            isGenerating = true
                            generatedQuestions = null
                            coroutineScope.launch {
                                try {
                                    val response = ApiClient.apiService.generateCustomAiQuiz(
                                        activeToken,
                                        CustomAiQuizRequest(
                                            prompt = aiPromptInput.trim(),
                                            count = selectedQuestionCount
                                        )
                                    )
                                    isGenerating = false
                                    if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                                        val body = response.body()!!
                                        if (!body.questions.isNullOrEmpty()) {
                                            generatedPromptTitle = body.prompt ?: aiPromptInput.trim()
                                            generatedQuestions = body.questions
                                            SoundManager.playCorrectSound()
                                            Toast.makeText(context, "🎯 ${body.questions.size} Questions Generated!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No questions generated. Try another topic!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to generate AI quiz. Please try again!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isGenerating = false
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Text("⚡ GENERATE QUIZ WITH AI", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // ── Generated 3D Soft-Clay Quiz Card ─────────────────────────────
            if (generatedQuestions != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(26.dp))
                        .background(cardBg, RoundedCornerShape(26.dp))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.35f else 0.95f),
                                    Color.Black.copy(alpha = if (isDark) 0.5f else 0.08f)
                                )
                            ),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Clean White Topic Name with Multi-line Wrapping
                        Text(
                            text = generatedPromptTitle.ifBlank { "Custom Quiz" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3D Compact White Play Button
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                val qList = generatedQuestions!!
                                aiPromptInput = ""
                                generatedQuestions = null
                                quizViewModel.startCustomAiQuiz(qList)
                                onStartQuiz()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            modifier = Modifier
                                .width(180.dp)
                                .height(46.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .background(Color.White, RoundedCornerShape(24.dp))
                                .border(
                                    1.5.dp,
                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFCBD5E1))),
                                    RoundedCornerShape(24.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF255FF4), modifier = Modifier.size(20.dp))
                                Text("PLAY QUIZ", color = Color(0xFF255FF4), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
