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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*

data class PassageStudyUnit(
    val title: String,
    val paragraph: String
)

@Composable
fun ReadingQuizScreen(
    onBack: () -> Unit
) {
    // Only load active passages created in Admin Studio / Database
    val activeUnits = globalPassagesList.map { article ->
        PassageStudyUnit(
            title = article.title,
            paragraph = article.paragraph
        )
    }

    val isDarkScreen = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
    val screenHeaderBg = if (isDarkScreen) Color(0xFF1C273A) else Color.White

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
            // Header Bar (3D Back button on left, 3D Royal Blue Book Icon & Title centered)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(42.dp)
                        .shadow(4.dp, CircleShape)
                        .background(screenHeaderBg, CircleShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkScreen) 0.35f else 0.9f),
                                    Color.Black.copy(alpha = if (isDarkScreen) 0.5f else 0.1f)
                                )
                            ),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkScreen) Color.White else Color(0xFF17181C)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF255FF4),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Passage Study",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkScreen) Color.White else Color(0xFF17181C)
                    )
                }
            }

            if (activeUnits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Empty",
                            tint = Color(0xFF255FF4),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "No Study Passages Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkScreen) Color.White else Color(0xFF17181C)
                        )
                        Text(
                            text = "Passages created in Admin Studio will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 3D Soft-Clay Passage Cards List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(activeUnits) { unit ->
                        val isDarkUnitCard = com.ilygames.quizapp.ui.theme.ThemeState.isDarkMode
                        val unitCardBg = if (isDarkUnitCard) Color(0xFF1C273A) else Color.White

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .background(unitCardBg, RoundedCornerShape(24.dp))
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isDarkUnitCard) 0.35f else 0.9f),
                                            Color.Black.copy(alpha = if (isDarkUnitCard) 0.5f else 0.08f)
                                        )
                                    ),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(22.dp)
                        ) {
                            Column {
                                Text(
                                    text = unit.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkUnitCard) Color.White else Color(0xFF17181C)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = formatMarkdownText(unit.paragraph),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 15.sp),
                                    color = if (isDarkUnitCard) Color.White.copy(alpha = 0.95f) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
