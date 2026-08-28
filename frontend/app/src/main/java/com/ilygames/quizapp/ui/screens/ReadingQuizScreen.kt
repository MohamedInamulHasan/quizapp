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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*

data class PassageStudyUnit(
    val title: String,
    val paragraph: String
)

val samplePassageUnits = listOf(
    PassageStudyUnit(
        title = "The James Webb Space Telescope",
        paragraph = "Launched in December 2021, the James Webb Space Telescope (JWST) is the largest and most powerful space observatory ever built. Orbiting 1.5 million kilometers from Earth at Lagrange point 2 (L2), JWST uses infrared astronomy to gaze through dense cosmic dust clouds. Equipped with a giant 6.5-meter gold-coated beryllium mirror and a tennis-court-sized sunshield, it allows astronomers to observe the universe's first galaxies formed over 13.5 billion years ago and analyze exoplanet atmospheres."
    ),
    PassageStudyUnit(
        title = "Artificial Intelligence & Neural Networks",
        paragraph = "Deep learning is a subset of machine learning inspired by the biological neural network of the human brain. Artificial neural networks consist of interconnected layers of nodes: an input layer, hidden layers, and an output layer. During training, a process called backpropagation adjusts the synaptic weights between nodes to minimize error functions. Transformer architectures utilize self-attention mechanisms to process text data in parallel, enabling rapid natural language understanding."
    ),
    PassageStudyUnit(
        title = "The Great Pyramid of Giza",
        paragraph = "Constructed around 2560 BCE as a tomb for Pharaoh Khufu, the Great Pyramid of Giza originally stood at 146.6 meters tall and remained the world's tallest structure for over 3,800 years. Built using an estimated 2.3 million limestone and granite blocks weighing 2.5 tons each, the pyramid was aligned to true north with an error margin of less than 1/15th of a degree. Builders used wooden levers, copper chisels, and water-lubricated sledges to move stone blocks across desert sands."
    )
)

@Composable
fun ReadingQuizScreen(
    onBack: () -> Unit
) {
    // Dynamically load active passages from NativeAdminScreen's globalPassagesList if populated
    val activeUnits = if (globalPassagesList.isNotEmpty()) {
        globalPassagesList.map { article ->
            PassageStudyUnit(
                title = article.title,
                paragraph = article.paragraph
            )
        }
    } else {
        samplePassageUnits
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
            // Header Bar (Back button on left, Passage Study Title centered)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean Passage Cards List (Title and Passage content only)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(activeUnits) { unit ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp)
                        ) {
                            Text(
                                text = unit.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = unit.paragraph,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
                            )
                        }
                    }
                }
            }
        }
    }
}
