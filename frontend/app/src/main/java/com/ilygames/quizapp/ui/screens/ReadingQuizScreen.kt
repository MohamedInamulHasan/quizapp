package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PassageQuestion(
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String
)

data class PassageStudyUnit(
    val title: String,
    val paragraph: String,
    val questions: List<PassageQuestion>
)

val samplePassageUnits = listOf(
    PassageStudyUnit(
        title = "The James Webb Space Telescope",
        paragraph = "Launched in December 2021, the James Webb Space Telescope (JWST) is the largest and most powerful space observatory ever built. Orbiting 1.5 million kilometers from Earth at Lagrange point 2 (L2), JWST uses infrared astronomy to gaze through dense cosmic dust clouds. Equipped with a giant 6.5-meter gold-coated beryllium mirror and a tennis-court-sized sunshield, it allows astronomers to observe the universe's first galaxies formed over 13.5 billion years ago and analyze exoplanet atmospheres.",
        questions = listOf(
            PassageQuestion(
                question = "Where is the James Webb Space Telescope located in space?",
                optionA = "Earth's Low Orbit",
                optionB = "Lagrange Point 2 (L2)",
                optionC = "Moon's South Pole",
                optionD = "Mars High Orbit",
                correctAnswer = "B"
            ),
            PassageQuestion(
                question = "What material coats JWST's primary 6.5-meter mirror?",
                optionA = "Polished Titanium",
                optionB = "Gold-coated Beryllium",
                optionC = "Solid Silver",
                optionD = "Carbon Fiber",
                correctAnswer = "B"
            ),
            PassageQuestion(
                question = "What primary type of astronomy does JWST use to see through dust?",
                optionA = "Ultraviolet",
                optionB = "X-Ray",
                optionC = "Infrared",
                optionD = "Gamma Ray",
                correctAnswer = "C"
            ),
            PassageQuestion(
                question = "How far back in cosmic time can JWST observe early galaxies?",
                optionA = "4.5 billion years",
                optionB = "8.2 billion years",
                optionC = "Over 13.5 billion years",
                optionD = "500 million years",
                correctAnswer = "C"
            )
        )
    ),
    PassageStudyUnit(
        title = "Artificial Intelligence & Neural Networks",
        paragraph = "Deep learning is a subset of machine learning inspired by the biological neural network of the human brain. Artificial neural networks consist of interconnected layers of nodes: an input layer, hidden layers, and an output layer. During training, a process called backpropagation adjusts the synaptic weights between nodes to minimize error functions. Transformer architectures utilize self-attention mechanisms to process text data in parallel, enabling rapid natural language understanding.",
        questions = listOf(
            PassageQuestion(
                question = "What biological system inspired artificial neural networks?",
                optionA = "DNA double helix",
                optionB = "The human brain",
                optionC = "Plant photosynthesis",
                optionD = "Cardiovascular system",
                correctAnswer = "B"
            ),
            PassageQuestion(
                question = "Which algorithm adjusts node weights to minimize error during AI training?",
                optionA = "Binary Search",
                optionB = "Backpropagation",
                optionC = "Bubble Sort",
                optionD = "Dijkstra Algorithm",
                correctAnswer = "B"
            ),
            PassageQuestion(
                question = "What key mechanism allows Transformer architectures to process text in parallel?",
                optionA = "Self-Attention",
                optionB = "Linear Regression",
                optionC = "Memory Swapping",
                optionD = "Manual Tagging",
                correctAnswer = "A"
            )
        )
    ),
    PassageStudyUnit(
        title = "The Great Pyramid of Giza",
        paragraph = "Constructed around 2560 BCE as a tomb for Pharaoh Khufu, the Great Pyramid of Giza originally stood at 146.6 meters tall and remained the world's tallest structure for over 3,800 years. Built using an estimated 2.3 million limestone and granite blocks weighing 2.5 tons each, the pyramid was aligned to true north with an error margin of less than 1/15th of a degree. Builders used wooden levers, copper chisels, and water-lubricated sledges to move stone blocks across desert sands.",
        questions = listOf(
            PassageQuestion(
                question = "For which Pharaoh was the Great Pyramid constructed?",
                optionA = "Ramses II",
                optionB = "Tutankhamun",
                optionC = "Pharaoh Khufu",
                optionD = "Cleopatra",
                correctAnswer = "C"
            ),
            PassageQuestion(
                question = "Approximately how many blocks were used in construction?",
                optionA = "500,000 blocks",
                optionB = "1.1 million blocks",
                optionC = "2.3 million blocks",
                optionD = "5 million blocks",
                correctAnswer = "C"
            ),
            PassageQuestion(
                question = "How did ancient builders lubricate desert sand to pull massive stone sledges?",
                optionA = "Animal Fat",
                optionB = "Water",
                optionC = "Olive Oil",
                optionD = "Tree Resin",
                correctAnswer = "B"
            )
        )
    )
)

@Composable
fun ReadingQuizScreen(
    onBack: () -> Unit
) {
    var unitIndex by remember { mutableStateOf(0) }
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val currentUnit = samplePassageUnits[unitIndex % samplePassageUnits.size]
    val currentQ = currentUnit.questions[questionIndex % currentUnit.questions.size]

    // Keyed selectedOption directly to question & unit index
    var selectedOption by remember(unitIndex, questionIndex) { mutableStateOf<String?>(null) }

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
                    text = "$score pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Passage Study Paragraph Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "PASSAGE STUDY TEXT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = PrimaryGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentUnit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentUnit.paragraph,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                // Keyed Question Deck
                key(unitIndex, questionIndex) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Question Box
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentQ.question,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Options Stack (550ms showcase delay!)
                        CleanHighlightOptionRow(
                            text = currentQ.optionA,
                            label = "A",
                            optionKey = "A",
                            selectedOption = selectedOption,
                            correctAnswer = currentQ.correctAnswer,
                            onClick = {
                                if (selectedOption == null) {
                                    selectedOption = "A"
                                    if ("A" == currentQ.correctAnswer) {
                                        SoundManager.playCorrectSound()
                                        score += 10
                                    } else {
                                        SoundManager.playWrongSound()
                                    }
                                    
                                    coroutineScope.launch {
                                        delay(550)
                                        if (questionIndex + 1 < currentUnit.questions.size) {
                                            questionIndex++
                                        } else {
                                            unitIndex++
                                            questionIndex = 0
                                        }
                                    }
                                }
                            }
                        )

                        CleanHighlightOptionRow(
                            text = currentQ.optionB,
                            label = "B",
                            optionKey = "B",
                            selectedOption = selectedOption,
                            correctAnswer = currentQ.correctAnswer,
                            onClick = {
                                if (selectedOption == null) {
                                    selectedOption = "B"
                                    if ("B" == currentQ.correctAnswer) {
                                        SoundManager.playCorrectSound()
                                        score += 10
                                    } else {
                                        SoundManager.playWrongSound()
                                    }
                                    
                                    coroutineScope.launch {
                                        delay(550)
                                        if (questionIndex + 1 < currentUnit.questions.size) {
                                            questionIndex++
                                        } else {
                                            unitIndex++
                                            questionIndex = 0
                                        }
                                    }
                                }
                            }
                        )

                        CleanHighlightOptionRow(
                            text = currentQ.optionC,
                            label = "C",
                            optionKey = "C",
                            selectedOption = selectedOption,
                            correctAnswer = currentQ.correctAnswer,
                            onClick = {
                                if (selectedOption == null) {
                                    selectedOption = "C"
                                    if ("C" == currentQ.correctAnswer) {
                                        SoundManager.playCorrectSound()
                                        score += 10
                                    } else {
                                        SoundManager.playWrongSound()
                                    }
                                    
                                    coroutineScope.launch {
                                        delay(550)
                                        if (questionIndex + 1 < currentUnit.questions.size) {
                                            questionIndex++
                                        } else {
                                            unitIndex++
                                            questionIndex = 0
                                        }
                                    }
                                }
                            }
                        )

                        CleanHighlightOptionRow(
                            text = currentQ.optionD,
                            label = "D",
                            optionKey = "D",
                            selectedOption = selectedOption,
                            correctAnswer = currentQ.correctAnswer,
                            onClick = {
                                if (selectedOption == null) {
                                    selectedOption = "D"
                                    if ("D" == currentQ.correctAnswer) {
                                        SoundManager.playCorrectSound()
                                        score += 10
                                    } else {
                                        SoundManager.playWrongSound()
                                    }
                                    
                                    coroutineScope.launch {
                                        delay(550)
                                        if (questionIndex + 1 < currentUnit.questions.size) {
                                            questionIndex++
                                        } else {
                                            unitIndex++
                                            questionIndex = 0
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
