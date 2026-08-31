package com.ilygames.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.utils.SoundManager

data class StudyArticle(
    val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val paragraph: String
)

val studyArticlesList = listOf(
    StudyArticle(
        id = "1",
        title = "The James Webb Space Telescope (JWST)",
        category = "SPACE & ASTRONOMY",
        readTime = "2 min read",
        paragraph = "Launched in December 2021, the James Webb Space Telescope (JWST) is the largest and most powerful space observatory ever built. Orbiting 1.5 million kilometers from Earth at Lagrange point 2 (L2), JWST uses infrared astronomy to gaze through dense cosmic dust clouds."
    ),
    StudyArticle(
        id = "2",
        title = "Artificial Intelligence & Neural Networks",
        category = "MODERN TECHNOLOGY",
        readTime = "2 min read",
        paragraph = "Deep learning is a subset of machine learning inspired by the biological neural network of the human brain. Artificial neural networks consist of interconnected layers of nodes."
    )
)

// Helper to format **markdown bold** text into AnnotatedString without displaying asterisks
fun formatMarkdownText(text: String): AnnotatedString {
    val parts = text.split("**")
    return buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = Color(0xFF255FF4))) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}

@Composable
fun StudyPassagesScreen(
    onBack: () -> Unit,
    onStartDaily20Quiz: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        loadPersistedAdminData(context)
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
            // 1. REDESIGNED TOP HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "PASSAGE STUDY HUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Study Passages",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        onStartDaily20Quiz()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Quiz",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Take Quiz",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            // Passages Reading List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(globalPassagesList) { article ->
                    val isDarkArt = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                    val artCardBg = if (isDarkArt) Color(0xFF1C273A) else Color.White

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(22.dp))
                            .background(artCardBg, RoundedCornerShape(22.dp))
                            .border(
                                1.5.dp,
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDarkArt) 0.35f else 0.9f),
                                        Color.Black.copy(alpha = if (isDarkArt) 0.5f else 0.08f)
                                    )
                                ),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                                fontWeight = FontWeight.Black,
                                color = if (isDarkArt) Color.White else Color(0xFF17181C)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatMarkdownText(article.paragraph),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontSize = 14.sp),
                                color = if (isDarkArt) Color.White.copy(alpha = 0.9f) else Color(0xFF2C3E6B)
                            )
                        }
                    }
                }
            }
        }
    }
}
