package com.ilygames.quizapp.ui.screens

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.utils.SoundManager

data class PassageStudyUnit(
    val title: String,
    val category: String = "General Knowledge",
    val paragraph: String,
    val keyTakeaways: List<String> = emptyList()
)

val samplePassageUnits = listOf(
    PassageStudyUnit(
        title = "The James Webb Space Telescope",
        category = "Space & Astronomy",
        paragraph = "Launched in December 2021, the James Webb Space Telescope (JWST) is the largest and most powerful space observatory ever built. Orbiting 1.5 million kilometers from Earth at Lagrange point 2 (L2), JWST uses infrared astronomy to gaze through dense cosmic dust clouds. Equipped with a giant 6.5-meter gold-coated beryllium mirror and a tennis-court-sized sunshield, it allows astronomers to observe the universe's first galaxies formed over 13.5 billion years ago and analyze exoplanet atmospheres.",
        keyTakeaways = listOf(
            "Orbiting at Lagrange Point 2 (1.5M km from Earth)",
            "6.5-meter primary mirror coated in gold-coated beryllium",
            "Observes galaxies formed over 13.5 billion years ago"
        )
    ),
    PassageStudyUnit(
        title = "Artificial Intelligence & Neural Networks",
        category = "Technology & Science",
        paragraph = "Deep learning is a subset of machine learning inspired by the biological neural network of the human brain. Artificial neural networks consist of interconnected layers of nodes: an input layer, hidden layers, and an output layer. During training, a process called backpropagation adjusts the synaptic weights between nodes to minimize error functions. Transformer architectures utilize self-attention mechanisms to process text data in parallel, enabling rapid natural language understanding.",
        keyTakeaways = listOf(
            "Inspired by biological human brain neural structures",
            "Backpropagation adjusts node weights to reduce training error",
            "Transformer models use self-attention for parallel text processing"
        )
    ),
    PassageStudyUnit(
        title = "The Great Pyramid of Giza",
        category = "World History",
        paragraph = "Constructed around 2560 BCE as a tomb for Pharaoh Khufu, the Great Pyramid of Giza originally stood at 146.6 meters tall and remained the world's tallest structure for over 3,800 years. Built using an estimated 2.3 million limestone and granite blocks weighing 2.5 tons each, the pyramid was aligned to true north with an error margin of less than 1/15th of a degree. Builders used wooden levers, copper chisels, and water-lubricated sledges to move stone blocks across desert sands.",
        keyTakeaways = listOf(
            "Built around 2560 BCE as tomb for Pharaoh Khufu",
            "Constructed with over 2.3 million limestone & granite blocks",
            "Aligned precisely to true north within 1/15th of a degree"
        )
    )
)

@Composable
fun ReadingQuizScreen(
    onBack: () -> Unit
) {
    var unitIndex by remember { mutableStateOf(0) }

    // Dynamically load active passages from NativeAdminScreen's globalPassagesList if populated
    val activeUnits = if (globalPassagesList.isNotEmpty()) {
        globalPassagesList.map { article ->
            PassageStudyUnit(
                title = article.title,
                category = "Study Passage",
                paragraph = article.paragraph,
                keyTakeaways = emptyList()
            )
        }
    } else {
        samplePassageUnits
    }

    val currentUnit = activeUnits[unitIndex % activeUnits.size]

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
                    Icon(Icons.Default.MenuBook, contentDescription = "Reading", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Passage Study",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen
                    )
                }

                Text(
                    text = "Passage ${(unitIndex % activeUnits.size) + 1}/${activeUnits.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Tag Card
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentUnit.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen,
                        letterSpacing = 1.sp
                    )
                }

                // Passage Article Card
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {
                        Text(
                            text = currentUnit.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = currentUnit.paragraph,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
                        )
                    }
                }

                // Key Takeaways Card (if available)
                if (currentUnit.keyTakeaways.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryGreen.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "KEY STUDY POINTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen,
                                letterSpacing = 1.sp
                            )

                            currentUnit.keyTakeaways.forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("•", fontWeight = FontWeight.Black, color = PrimaryGreen, fontSize = 16.sp)
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Passage Navigation Controls (Previous & Next Passage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Previous Passage Button
                OutlinedButton(
                    onClick = {
                        SoundManager.playClickSound()
                        if (unitIndex > 0) {
                            unitIndex--
                        } else {
                            unitIndex = activeUnits.size - 1
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous", tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous", fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }

                // Next Passage Button
                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        unitIndex++
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Next Passage", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.White)
                }
            }
        }
    }
}
