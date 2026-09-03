package com.ilygames.quizapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ilygames.quizapp.ui.theme.ThemeState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager

// ── CHARACTER DATA MODEL ─────────────────────────────────────────────────
enum class Gender { MALE, FEMALE }
enum class EyeColor { BLUE, GREEN, BROWN, BLACK }
enum class HairColor { BLACK, BROWN, BLONDE, RED, GREY }
enum class HairStyle { SHORT, LONG, BALD }
enum class SkinTone { FAIR, MEDIUM, DARK }
enum class Accessory { GLASSES, HAT, EARRINGS, JEWELS, NONE }
enum class FacialHair { BEARD, MUSTACHE, NONE }

data class GuessWhoCharacter(
    val id: String,
    val name: String,
    val gender: Gender,
    val eyeColor: EyeColor,
    val hairColor: HairColor,
    val hairStyle: HairStyle,
    val skinTone: SkinTone,
    val accessory: Accessory,
    val facialHair: FacialHair,
    val avatarBgColor: Color,
    val isEliminated: Boolean = false
)

// 30 Unique Character Roster
val ALL_CHARACTERS = listOf(
    GuessWhoCharacter("1", "Lucy", Gender.FEMALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.LONG, SkinTone.FAIR, Accessory.HAT, FacialHair.NONE, Color(0xFF8B5CF6)),
    GuessWhoCharacter("2", "Tom", Gender.MALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.BALD, SkinTone.FAIR, Accessory.GLASSES, FacialHair.MUSTACHE, Color(0xFF3B82F6)),
    GuessWhoCharacter("3", "Sara", Gender.FEMALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.LONG, SkinTone.FAIR, Accessory.GLASSES, FacialHair.NONE, Color(0xFFEC4899)),
    GuessWhoCharacter("4", "Emma", Gender.FEMALE, EyeColor.BROWN, HairColor.GREY, HairStyle.LONG, SkinTone.MEDIUM, Accessory.NONE, FacialHair.NONE, Color(0xFF10B981)),
    GuessWhoCharacter("5", "Henry", Gender.MALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.NONE, FacialHair.BEARD, Color(0xFFF59E0B)),

    GuessWhoCharacter("6", "Rose", Gender.FEMALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.LONG, SkinTone.FAIR, Accessory.HAT, FacialHair.NONE, Color(0xFF6366F1)),
    GuessWhoCharacter("7", "Jeff", Gender.MALE, EyeColor.BLACK, HairColor.BLONDE, HairStyle.SHORT, SkinTone.FAIR, Accessory.GLASSES, FacialHair.BEARD, Color(0xFF14B8A6)),
    GuessWhoCharacter("8", "Lia", Gender.FEMALE, EyeColor.BROWN, HairColor.GREY, HairStyle.LONG, SkinTone.FAIR, Accessory.EARRINGS, FacialHair.NONE, Color(0xFFF43F5E)),
    GuessWhoCharacter("9", "Rob", Gender.MALE, EyeColor.BLACK, HairColor.BLACK, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.HAT, FacialHair.BEARD, Color(0xFF06B6D4)),
    GuessWhoCharacter("10", "Aria", Gender.FEMALE, EyeColor.BROWN, HairColor.GREY, HairStyle.LONG, SkinTone.FAIR, Accessory.GLASSES, FacialHair.NONE, Color(0xFFA855F7)),

    GuessWhoCharacter("11", "Bob", Gender.MALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.NONE, FacialHair.NONE, Color(0xFFEF4444)),
    GuessWhoCharacter("12", "Bella", Gender.FEMALE, EyeColor.GREEN, HairColor.RED, HairStyle.LONG, SkinTone.FAIR, Accessory.NONE, FacialHair.NONE, Color(0xFF3B82F6)),
    GuessWhoCharacter("13", "Sophia", Gender.FEMALE, EyeColor.BROWN, HairColor.BLONDE, HairStyle.LONG, SkinTone.FAIR, Accessory.EARRINGS, FacialHair.NONE, Color(0xFFF59E0B)),
    GuessWhoCharacter("14", "Jack", Gender.MALE, EyeColor.GREEN, HairColor.GREY, HairStyle.SHORT, SkinTone.FAIR, Accessory.HAT, FacialHair.BEARD, Color(0xFF8B5CF6)),
    GuessWhoCharacter("15", "Jose", Gender.MALE, EyeColor.GREEN, HairColor.BLACK, HairStyle.BALD, SkinTone.DARK, Accessory.NONE, FacialHair.BEARD, Color(0xFF10B981)),

    GuessWhoCharacter("16", "Theo", Gender.MALE, EyeColor.BLUE, HairColor.BLONDE, HairStyle.SHORT, SkinTone.FAIR, Accessory.NONE, FacialHair.NONE, Color(0xFF6366F1)),
    GuessWhoCharacter("17", "Annie", Gender.FEMALE, EyeColor.BROWN, HairColor.RED, HairStyle.LONG, SkinTone.FAIR, Accessory.EARRINGS, FacialHair.NONE, Color(0xFFEC4899)),
    GuessWhoCharacter("18", "Paul", Gender.MALE, EyeColor.BROWN, HairColor.RED, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.NONE, FacialHair.BEARD, Color(0xFFF43F5E)),
    GuessWhoCharacter("19", "Olivia", Gender.FEMALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.LONG, SkinTone.DARK, Accessory.EARRINGS, FacialHair.NONE, Color(0xFF06B6D4)),
    GuessWhoCharacter("20", "Chloe", Gender.FEMALE, EyeColor.GREEN, HairColor.BROWN, HairStyle.LONG, SkinTone.FAIR, Accessory.EARRINGS, FacialHair.NONE, Color(0xFFA855F7)),

    GuessWhoCharacter("21", "Nora", Gender.FEMALE, EyeColor.BLACK, HairColor.BROWN, HairStyle.LONG, SkinTone.DARK, Accessory.GLASSES, FacialHair.NONE, Color(0xFFEF4444)),
    GuessWhoCharacter("22", "James", Gender.MALE, EyeColor.BROWN, HairColor.BROWN, HairStyle.SHORT, SkinTone.FAIR, Accessory.GLASSES, FacialHair.NONE, Color(0xFF3B82F6)),
    GuessWhoCharacter("23", "Bill", Gender.MALE, EyeColor.BLUE, HairColor.BLONDE, HairStyle.SHORT, SkinTone.FAIR, Accessory.NONE, FacialHair.NONE, Color(0xFF10B981)),
    GuessWhoCharacter("24", "Julia", Gender.FEMALE, EyeColor.BROWN, HairColor.GREY, HairStyle.LONG, SkinTone.DARK, Accessory.NONE, FacialHair.NONE, Color(0xFFF59E0B)),
    GuessWhoCharacter("25", "Naomi", Gender.FEMALE, EyeColor.BLACK, HairColor.BLACK, HairStyle.LONG, SkinTone.FAIR, Accessory.EARRINGS, FacialHair.NONE, Color(0xFF8B5CF6)),

    GuessWhoCharacter("26", "Ryan", Gender.MALE, EyeColor.BLUE, HairColor.BROWN, HairStyle.SHORT, SkinTone.FAIR, Accessory.GLASSES, FacialHair.MUSTACHE, Color(0xFF6366F1)),
    GuessWhoCharacter("27", "David", Gender.MALE, EyeColor.BLACK, HairColor.BLACK, HairStyle.SHORT, SkinTone.DARK, Accessory.NONE, FacialHair.MUSTACHE, Color(0xFF14B8A6)),
    GuessWhoCharacter("28", "Mila", Gender.FEMALE, EyeColor.BROWN, HairColor.BLONDE, HairStyle.LONG, SkinTone.DARK, Accessory.GLASSES, FacialHair.NONE, Color(0xFFEC4899)),
    GuessWhoCharacter("29", "John", Gender.MALE, EyeColor.BLUE, HairColor.GREY, HairStyle.SHORT, SkinTone.DARK, Accessory.HAT, FacialHair.BEARD, Color(0xFF06B6D4)),
    GuessWhoCharacter("30", "Ben", Gender.MALE, EyeColor.BLACK, HairColor.BLACK, HairStyle.BALD, SkinTone.DARK, Accessory.GLASSES, FacialHair.NONE, Color(0xFFF59E0B))
)

@Composable
fun GuessWhoScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    // Match States
    var currentRound by remember { mutableIntStateOf(1) }
    var p1Wins by remember { mutableIntStateOf(0) }
    var p2Wins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }

    // Game Phases
    var gamePhase by remember { mutableStateOf("SELECTION") } // "SELECTION" or "PLAYING"
    var p1SecretCharacter by remember { mutableStateOf<GuessWhoCharacter?>(null) }
    var p2SecretCharacter by remember { mutableStateOf<GuessWhoCharacter?>(null) }
    var selectedCharacterForChoice by remember { mutableStateOf<GuessWhoCharacter?>(null) }

    // Roster Board State
    var boardCharacters by remember { mutableStateOf(ALL_CHARACTERS) }

    // Filter Dialog Modals
    var activeTraitModal by remember { mutableStateOf<String?>(null) } // "GENDER", "EYE_COLOR", "HAIR", "HAIR_COLOR", "SKIN_TONE", "ACCESSORY", "FACIAL_HAIR"
    var showWinnerDialog by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf("") }
    var winningPlayer by remember { mutableIntStateOf(1) }
    var rewardEarned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    // Question Filter State
    var lastAskedQuestion by remember { mutableStateOf<String?>(null) }
    var lastQuestionAnswer by remember { mutableStateOf<Boolean?>(null) }

    // Auto-select secret character for Player 2 (AI/Opponent)
    fun setupNewRound() {
        gamePhase = "SELECTION"
        p1SecretCharacter = null
        p2SecretCharacter = ALL_CHARACTERS.random()
        selectedCharacterForChoice = null
        boardCharacters = ALL_CHARACTERS.map { it.copy(isEliminated = false) }
        lastAskedQuestion = null
        lastQuestionAnswer = null
    }

    LaunchedEffect(currentRound) {
        setupNewRound()
    }

    fun resetFullMatch() {
        currentRound = 1
        p1Wins = 0
        p2Wins = 0
        draws = 0
        isPlayer1Turn = true
        showWinnerDialog = false
        rewardEarned = false
        setupNewRound()
        SoundManager.playRetrySound()
    }

    // Question Filter Eliminator
    fun applyQuestionFilter(questionText: String, matchingCondition: (GuessWhoCharacter) -> Boolean) {
        val targetSecret = if (isPlayer1Turn) p2SecretCharacter!! else p1SecretCharacter!!
        val isYes = matchingCondition(targetSecret)

        SoundManager.playPopSound()

        lastAskedQuestion = questionText
        lastQuestionAnswer = isYes

        // Eliminate non-matching characters
        boardCharacters = boardCharacters.map { char ->
            val matchesFilter = matchingCondition(char)
            if (isYes) {
                // If answer is YES, eliminate characters that DON'T match!
                char.copy(isEliminated = char.isEliminated || !matchesFilter)
            } else {
                // If answer is NO, eliminate characters that DO match!
                char.copy(isEliminated = char.isEliminated || matchesFilter)
            }
        }

        activeTraitModal = null

        // Check if only 1 character remains
        val remaining = boardCharacters.filter { !it.isEliminated }
        if (remaining.size == 1) {
            val lastChar = remaining.first()
            if (lastChar.id == targetSecret.id) {
                // Victory!
                if (isPlayer1Turn) p1Wins++ else p2Wins++
                winnerMessage = if (isPlayer1Turn) "🎉 Player 1 Guessed Correctly! It's ${targetSecret.name}!" else "🎉 Player 2 Wins!"
                winningPlayer = if (isPlayer1Turn) 1 else 2
                showWinnerDialog = true
            }
        }
    }

    // Direct Guess Character
    fun makeDirectGuess(guessedChar: GuessWhoCharacter) {
        val targetSecret = if (isPlayer1Turn) p2SecretCharacter!! else p1SecretCharacter!!
        if (guessedChar.id == targetSecret.id) {
            SoundManager.playCorrectSound()
            if (isPlayer1Turn) p1Wins++ else p2Wins++
            winnerMessage = if (isPlayer1Turn) "🎉 Player 1 Guessed Correctly! It's ${targetSecret.name}!" else "🎉 Player 2 Wins!"
            winningPlayer = if (isPlayer1Turn) 1 else 2
            showWinnerDialog = true
        } else {
            SoundManager.playWrongSound()
            Toast.makeText(context, "❌ Wrong Guess! It is not ${guessedChar.name}!", Toast.LENGTH_SHORT).show()
            // Switch Turn
            isPlayer1Turn = !isPlayer1Turn
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFA8B73))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP HEADER BAR ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 3D Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.5f else 0.9f),
                                    Color.Black.copy(alpha = if (isDark) 0.6f else 0.15f)
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(20.dp))
                }

                // Title
                Text(
                    text = "Guess Who?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )

                // 3D Retry Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF2C3E55), Color(0xFF1A2636))
                                else listOf(Color.White, Color(0xFFE2E8F0))
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.5f else 0.9f),
                                    Color.Black.copy(alpha = if (isDark) 0.6f else 0.15f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            SoundManager.playRetrySound()
                            resetFullMatch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Match", tint = textColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── PLAYER CARDS ROW: PLAYER 1 (LEFT) & PLAYER 2 (RIGHT) ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 Card (Attached to Left Edge)
                Box(
                    modifier = Modifier
                        .offset(x = (-3).dp)
                        .shadow(8.dp, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isPlayer1Turn) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "PLAYER 1", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "$p1Wins", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (isPlayer1Turn) Color(0xFFDC2626) else Color(0xFF334155))
                        }
                    }
                }

                // Player 2 Card (Attached to Right Edge)
                Box(
                    modifier = Modifier
                        .offset(x = 3.dp)
                        .shadow(8.dp, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(
                            Brush.verticalGradient(
                                if (!isPlayer1Turn) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "PLAYER 2", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "$p2Wins", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (!isPlayer1Turn) Color(0xFF1D4ED8) else Color(0xFF334155))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── PHASE 1: CHARACTER SELECTION SCREEN ("Choose your character!") ──
            if (gamePhase == "SELECTION") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose your character!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // 5 Columns x 6 Rows Grid Container (Matching Screenshot 1 & 2)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(12.dp, RoundedCornerShape(24.dp))
                            .background(Color(0xFF9E654E), RoundedCornerShape(24.dp))
                            .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(ALL_CHARACTERS) { char ->
                                val isSelected = selectedCharacterForChoice?.id == char.id
                                GuessWhoCharacterCardItem(
                                    character = char,
                                    isSelected = isSelected,
                                    onClick = {
                                        SoundManager.playPopSound()
                                        selectedCharacterForChoice = char
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // CONFIRM BUTTON (Matching Screenshot 2)
                    Button(
                        onClick = {
                            if (selectedCharacterForChoice != null) {
                                SoundManager.playCorrectSound()
                                p1SecretCharacter = selectedCharacterForChoice
                                gamePhase = "PLAYING"
                            } else {
                                Toast.makeText(context, "Please select your secret character first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
                        enabled = selectedCharacterForChoice != null,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (selectedCharacterForChoice != null) listOf(Color(0xFF10B981), Color(0xFF059669))
                                    else listOf(Color(0xFF9CA3AF), Color(0xFF6B7280))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                    ) {
                        Text("CONFIRM", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else {
                // ── PHASE 2: DEDUCTION BOARD & QUESTION RESULT BANNER (Matching Screenshot 6) ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 5x6 Roster Grid
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(12.dp, RoundedCornerShape(24.dp))
                            .background(Color(0xFF9E654E), RoundedCornerShape(24.dp))
                            .border(2.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(boardCharacters) { char ->
                                GuessWhoCharacterCardItem(
                                    character = char,
                                    isSelected = false,
                                    onClick = {
                                        if (!char.isEliminated) {
                                            makeDirectGuess(char)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── WHITE QUESTION & RESULT BANNER (Exact Match to Screenshot 6) ──
                    if (lastAskedQuestion != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(22.dp))
                                .background(Color.White, RoundedCornerShape(22.dp))
                                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                .padding(vertical = 14.dp, horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lastAskedQuestion!!,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF17181C),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (lastQuestionAnswer == true) "👤✅ Yes!" else "👤❌ No!",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (lastQuestionAnswer == true) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // FOOTER HELP TEXT (Exact Match to Screenshot 6)
                        Text(
                            text = "HELP: Tap the cards to remove candidates that do not match the question's trait.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        // BOTTOM TRAIT FILTER BUTTONS ROW (Exact match to Screenshots 3, 4, 5)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: GENDER, EYE COLOR, HAIR, HAIR COLOR
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TraitCategoryButton(label = "GENDER", icon = "👫", modifier = Modifier.weight(1f)) { activeTraitModal = "GENDER" }
                                TraitCategoryButton(label = "EYE COLOR", icon = "👁️", modifier = Modifier.weight(1.1f)) { activeTraitModal = "EYE_COLOR" }
                                TraitCategoryButton(label = "HAIR", icon = "💇", modifier = Modifier.weight(0.9f)) { activeTraitModal = "HAIR" }
                                TraitCategoryButton(label = "HAIR COLOR", icon = "🎨", modifier = Modifier.weight(1.1f)) { activeTraitModal = "HAIR_COLOR" }
                            }

                            // Row 2: SKIN TONE, ACCESSORIES, FACIAL HAIR
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TraitCategoryButton(label = "SKIN TONE", icon = "🏽", modifier = Modifier.weight(1f)) { activeTraitModal = "SKIN_TONE" }
                                TraitCategoryButton(label = "ACCESSORIES", icon = "👓", modifier = Modifier.weight(1.2f)) { activeTraitModal = "ACCESSORY" }
                                TraitCategoryButton(label = "FACIAL HAIR", icon = "👨", modifier = Modifier.weight(1.2f)) { activeTraitModal = "FACIAL_HAIR" }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // ── TRAIT SELECTION MODAL DIALOGS (Matching Screenshot 4 & 5) ─────────────
        if (activeTraitModal != null) {
            Dialog(
                onDismissRequest = { activeTraitModal = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .shadow(16.dp, RoundedCornerShape(28.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color.White, RoundedCornerShape(28.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Top Bar Header with Close X (Matching Screenshot 4 & 5)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.size(24.dp))
                                Text(
                                    text = activeTraitModal!!.replace("_", " "),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                                IconButton(
                                    onClick = { activeTraitModal = null },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            when (activeTraitModal) {
                                "GENDER" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        TraitOptionCard("MALE", "👨") { applyQuestionFilter("Is the person male?") { it.gender == Gender.MALE } }
                                        TraitOptionCard("FEMALE", "👩") { applyQuestionFilter("Is the person female?") { it.gender == Gender.FEMALE } }
                                    }
                                }
                                "EYE_COLOR" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            TraitOptionCard("BLUE", "🟦") { applyQuestionFilter("Does the person have blue eyes?") { it.eyeColor == EyeColor.BLUE } }
                                            TraitOptionCard("GREEN", "🟩") { applyQuestionFilter("Does the person have green eyes?") { it.eyeColor == EyeColor.GREEN } }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            TraitOptionCard("BROWN", "🟫") { applyQuestionFilter("Does the person have brown eyes?") { it.eyeColor == EyeColor.BROWN } }
                                            TraitOptionCard("BLACK", "⬛") { applyQuestionFilter("Does the person have black eyes?") { it.eyeColor == EyeColor.BLACK } }
                                        }
                                    }
                                }
                                "HAIR" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        TraitOptionCard("SHORT", "👨‍🦱") { applyQuestionFilter("Does the person have short hair?") { it.hairStyle == HairStyle.SHORT } }
                                        TraitOptionCard("LONG", "👩‍🦰") { applyQuestionFilter("Does the person have long hair?") { it.hairStyle == HairStyle.LONG } }
                                        TraitOptionCard("BALD", "👨‍🦲") { applyQuestionFilter("Is the person bald?") { it.hairStyle == HairStyle.BALD } }
                                    }
                                }
                                "HAIR_COLOR" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            TraitOptionCard("BROWN", "🟫") { applyQuestionFilter("Does the person have brown hair?") { it.hairColor == HairColor.BROWN } }
                                            TraitOptionCard("BLACK", "⬛") { applyQuestionFilter("Does the person have black hair?") { it.hairColor == HairColor.BLACK } }
                                            TraitOptionCard("BLONDE", "🟨") { applyQuestionFilter("Does the person have blonde hair?") { it.hairColor == HairColor.BLONDE } }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            TraitOptionCard("GRAY", "👵") { applyQuestionFilter("Does the person have gray hair?") { it.hairColor == HairColor.GREY } }
                                            TraitOptionCard("RED", "🟥") { applyQuestionFilter("Does the person have red hair?") { it.hairColor == HairColor.RED } }
                                        }
                                    }
                                }
                                "SKIN_TONE" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        TraitOptionCard("FAIR", "🏻") { applyQuestionFilter("Does the person have fair skin?") { it.skinTone == SkinTone.FAIR } }
                                        TraitOptionCard("MEDIUM", "🏽") { applyQuestionFilter("Does the person have medium skin?") { it.skinTone == SkinTone.MEDIUM } }
                                        TraitOptionCard("DARK", "🏿") { applyQuestionFilter("Does the person have dark skin?") { it.skinTone == SkinTone.DARK } }
                                    }
                                }
                                "ACCESSORY" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            TraitOptionCard("NONE", "🚫") { applyQuestionFilter("Does the person have no accessories?") { it.accessory == Accessory.NONE } }
                                            TraitOptionCard("GLASSES", "👓") { applyQuestionFilter("Is the person wearing glasses?") { it.accessory == Accessory.GLASSES } }
                                            TraitOptionCard("HAT", "🧢") { applyQuestionFilter("Is the person wearing a hat?") { it.accessory == Accessory.HAT } }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            TraitOptionCard("EARRINGS", "💎") { applyQuestionFilter("Is the person wearing earrings?") { it.accessory == Accessory.EARRINGS || it.accessory == Accessory.JEWELS } }
                                            TraitOptionCard("JEWELS", "💍") { applyQuestionFilter("Is the person wearing jewels?") { it.accessory == Accessory.JEWELS || it.accessory == Accessory.EARRINGS } }
                                        }
                                    }
                                }
                                "FACIAL_HAIR" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        TraitOptionCard("NONE", "🚫") { applyQuestionFilter("Does the person have no facial hair?") { it.facialHair == FacialHair.NONE } }
                                        TraitOptionCard("MUSTACHE", "👨‍🦰") { applyQuestionFilter("Does the person have a mustache?") { it.facialHair == FacialHair.MUSTACHE } }
                                        TraitOptionCard("BEARD", "🧔") { applyQuestionFilter("Does the person have a beard?") { it.facialHair == FacialHair.BEARD } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3D MATCH VICTORY REWARD MODAL ────────────────────────────────
        if (showWinnerDialog) {
            val p1Won = winningPlayer == 1
            val p2Won = winningPlayer == 2

            LaunchedEffect(Unit) {
                SoundManager.playCorrectSound()
            }

            var isCardVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isCardVisible = true }

            val cardScale by animateFloatAsState(
                targetValue = if (isCardVisible) 1.0f else 0.35f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "CardSpringEntrance"
            )

            val cardAlpha by animateFloatAsState(
                targetValue = if (isCardVisible) 1.0f else 0.0f,
                animationSpec = tween(350),
                label = "CardAlphaEntrance"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "TrophyPulse")
            val trophyScale by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                label = "TrophyScale"
            )

            Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .scale(cardScale)
                            .graphicsLayer { alpha = cardAlpha }
                            .shadow(24.dp, RoundedCornerShape(32.dp))
                            .background(
                                Brush.verticalGradient(if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A)) else listOf(Color.White, Color(0xFFF1F5F9))),
                                RoundedCornerShape(32.dp)
                            )
                            .border(
                                2.5.dp,
                                Brush.verticalGradient(listOf(Color.White.copy(alpha = if (isDark) 0.8f else 0.95f), Color.White.copy(alpha = if (isDark) 0.2f else 0.4f))),
                                RoundedCornerShape(32.dp)
                            )
                            .padding(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(trophyScale)
                                    .shadow(12.dp, CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            if (p1Won) listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                            else if (p2Won) listOf(Color(0xFF255FF4), Color(0xFF1D4ED8))
                                            else listOf(Color(0xFF64748B), Color(0xFF334155))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏆", fontSize = 44.sp)
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = winnerMessage,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .shadow(10.dp, RoundedCornerShape(18.dp))
                                        .background(Brush.verticalGradient(listOf(Color(0xFF386DF5), Color(0xFF255FF4), Color(0xFF0B46DA))), RoundedCornerShape(18.dp))
                                        .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.3f))), RoundedCornerShape(18.dp))
                                        .clickable {
                                            SoundManager.playClickSound()
                                            setupNewRound()
                                            showWinnerDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PLAY AGAIN", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .shadow(10.dp, RoundedCornerShape(18.dp))
                                        .background(Brush.verticalGradient(if (isDark) listOf(Color(0xFF475569), Color(0xFF334155)) else listOf(Color(0xFF64748B), Color(0xFF475569))), RoundedCornerShape(18.dp))
                                        .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.3f))), RoundedCornerShape(18.dp))
                                        .clickable {
                                            SoundManager.playClickSound()
                                            onBack()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("HOME", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── CHARACTER CARD COMPONENT ─────────────────────────────────────────────
@Composable
fun GuessWhoCharacterCardItem(
    character: GuessWhoCharacter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .graphicsLayer { alpha = if (character.isEliminated) 0.35f else 1.0f }
            .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(12.dp))
            .background(
                if (character.isEliminated) Color(0xFF334155) else Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                if (isSelected) 3.dp else 1.dp,
                if (isSelected) Color(0xFF10B981) else Color.White.copy(alpha = 0.8f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !character.isEliminated, onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Stylized Avatar Box / Custom Uploaded Image
            val context = LocalContext.current
            val drawableName = "avatar_${character.name.lowercase()}"
            val imageResId = remember(character.name) {
                context.resources.getIdentifier(drawableName, "drawable", context.packageName)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                    .background(character.avatarBgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0) {
                    // Uploaded Avatar Image Found!
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback Vector Person Representation
                    val hairColorVal = when (character.hairColor) {
                        HairColor.BLACK -> Color(0xFF17181C)
                        HairColor.BROWN -> Color(0xFF78350F)
                        HairColor.BLONDE -> Color(0xFFF59E0B)
                        HairColor.RED -> Color(0xFFDC2626)
                        HairColor.GREY -> Color(0xFF9CA3AF)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    when (character.skinTone) {
                                        SkinTone.FAIR -> Color(0xFFFDE68A)
                                        SkinTone.MEDIUM -> Color(0xFFD97706)
                                        SkinTone.DARK -> Color(0xFF78350F)
                                    },
                                    CircleShape
                                )
                                .border(1.5.dp, hairColorVal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (character.accessory == Accessory.GLASSES) "👓"
                                else if (character.accessory == Accessory.HAT) "🧢"
                                else if (character.facialHair != FacialHair.NONE) "🧔"
                                else "👤",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Name Ribbon (Matching Screenshot 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(bottomStart = 11.dp, bottomEnd = 11.dp))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

// ── TRAIT CATEGORY BUTTON COMPONENT (Matching Screenshot 3) ───────────────
@Composable
fun TraitCategoryButton(
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 14.sp)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
        }
    }
}

// ── TRAIT OPTION CARD COMPONENT (Matching Screenshot 4 & 5) ───────────────
@Composable
fun TraitOptionCard(
    label: String,
    icon: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 90.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(Color(0xFFF1F5F9), RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFF94A3B8), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
        }
    }
}
