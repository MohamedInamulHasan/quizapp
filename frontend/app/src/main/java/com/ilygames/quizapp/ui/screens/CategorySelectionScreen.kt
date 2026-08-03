package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilygames.quizapp.data.model.QuizCategory
import com.ilygames.quizapp.ui.viewmodel.InfiniteQuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(
    token: String,
    onCategorySelected: (categoryId: Int, categoryName: String, difficulty: String) -> Unit,
    onAiTopicSelected: (topic: String, difficulty: String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: InfiniteQuizViewModel = viewModel()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedDifficulty by remember { mutableStateOf("medium") }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiTopic by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadCategories(token) }

    if (showAiDialog) {
        AiTopicDialog(
            topic = aiTopic,
            onTopicChange = { aiTopic = it },
            onConfirm = {
                if (aiTopic.isNotBlank()) {
                    showAiDialog = false
                    onAiTopicSelected(aiTopic.trim(), selectedDifficulty)
                }
            },
            onDismiss = { showAiDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        // Decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0x1A6C63FF), radius = 300f, center = Offset(size.width * 0.1f, size.height * 0.15f))
            drawCircle(color = Color(0x1AFF6B6B), radius = 200f, center = Offset(size.width * 0.9f, size.height * 0.85f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Explore Quiz",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pick a topic & play!",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            // Difficulty selector
            Text(
                "Difficulty",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("easy" to "😊 Easy", "medium" to "🔥 Medium", "hard" to "💀 Hard").forEach { (key, label) ->
                    val selected = selectedDifficulty == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = if (selected) 0.dp else 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDifficulty = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Choose Category",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6C63FF))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 2000.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = {
                                if (category.id == 0) {
                                    showAiDialog = true
                                } else {
                                    onCategorySelected(category.id, category.name, selectedDifficulty)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: QuizCategory, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_${category.id}")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale_${category.id}"
    )

    val isAi = category.id == 0
    val cardColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (e: Exception) {
        Color(0xFF6C63FF)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .then(if (isAi) Modifier.scale(scale) else Modifier),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isAi)
                        Brush.linearGradient(colors = listOf(Color(0xFFFF4757), Color(0xFFFF6B81)))
                    else
                        Brush.linearGradient(
                            colors = listOf(cardColor.copy(alpha = 0.9f), cardColor.copy(alpha = 0.6f))
                        )
                )
        ) {
            if (isAi) {
                // Sparkle effect for AI card
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..5) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = (20 + i * 15).toFloat(),
                            center = Offset(size.width * 0.8f, size.height * 0.2f)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 36.sp
                )
                Column {
                    Text(
                        text = category.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAi) {
                        Text(
                            "Powered by Gemini",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiTopicDialog(
    topic: String,
    onTopicChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1B3A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🤖", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Quiz Generator", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Type any topic and AI will create 10 unique questions for you!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = topic,
                    onValueChange = onTopicChange,
                    label = { Text("Topic", color = Color.White.copy(alpha = 0.6f)) },
                    placeholder = { Text("e.g. Indian Cricket, Marvel Movies...", color = Color.White.copy(0.3f), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(0.3f)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Quick suggestions
                Text("Quick picks:", color = Color.White.copy(0.5f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("IPL Cricket", "Harry Potter", "Bollywood").forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onTopicChange(suggestion) },
                            label = { Text(suggestion, fontSize = 11.sp, color = Color(0xFF6C63FF)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF6C63FF).copy(alpha = 0.15f)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = Color(0xFF6C63FF).copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                enabled = topic.isNotBlank()
            ) {
                Text("Generate Quiz ✨", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(0.6f))
            }
        }
    )
}
