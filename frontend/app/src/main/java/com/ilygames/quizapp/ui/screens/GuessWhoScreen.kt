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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlinx.coroutines.delay

import com.ilygames.quizapp.utils.SoundManager

// ── Bouncy Clickable Modifier for 3D Pop/Press Scale Animation Effect ──────
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BouncyClickScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// ── CHARACTER DATA MODEL ─────────────────────────────────────────────────
enum class Gender { MALE, FEMALE }
enum class EyeColor { BLUE, GREEN, BROWN, BLACK }
enum class HairColor { BLACK, BROWN, BLONDE, RED, GREY }
enum class HairStyle { SHORT, LONG, BALD }
enum class SkinTone { FAIR, MEDIUM, DARK }
enum class Accessory { GLASSES, HAT, EARRINGS, JEWELS, NECKLACE, NONE }
enum class FacialHair { BEARD, MUSTACHE, NONE }
enum class TraitStatus { NONE, TICK, CROSS }

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
    GuessWhoCharacter("1", "Lucy", Gender.FEMALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.JEWELS, FacialHair.NONE, Color(0xFF8B5CF6)),
    GuessWhoCharacter("2", "Tom", Gender.MALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.BALD, SkinTone.MEDIUM, Accessory.GLASSES, FacialHair.MUSTACHE, Color(0xFF3B82F6)),
    GuessWhoCharacter("3", "Sara", Gender.FEMALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.LONG, SkinTone.FAIR, Accessory.JEWELS, FacialHair.NONE, Color(0xFFEC4899)),
    GuessWhoCharacter("4", "Emma", Gender.FEMALE, EyeColor.BLACK, HairColor.GREY, HairStyle.LONG, SkinTone.DARK, Accessory.NONE, FacialHair.NONE, Color(0xFF10B981)),
    GuessWhoCharacter("5", "Henry", Gender.MALE, EyeColor.BLACK, HairColor.BROWN, HairStyle.SHORT, SkinTone.DARK, Accessory.NONE, FacialHair.BEARD, Color(0xFFF59E0B)),

    GuessWhoCharacter("6", "Rose", Gender.FEMALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.SHORT, SkinTone.FAIR, Accessory.HAT, FacialHair.NONE, Color(0xFF6366F1)),
    GuessWhoCharacter("7", "Jeff", Gender.MALE, EyeColor.BLACK, HairColor.BLONDE, HairStyle.LONG, SkinTone.FAIR, Accessory.GLASSES, FacialHair.BEARD, Color(0xFF14B8A6)),
    GuessWhoCharacter("8", "Lia", Gender.FEMALE, EyeColor.BLACK, HairColor.GREY, HairStyle.LONG, SkinTone.MEDIUM, Accessory.JEWELS, FacialHair.NONE, Color(0xFFF43F5E)),
    GuessWhoCharacter("9", "Rob", Gender.MALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.HAT, FacialHair.BEARD, Color(0xFF06B6D4)),
    GuessWhoCharacter("10", "Aria", Gender.FEMALE, EyeColor.BLACK, HairColor.GREY, HairStyle.SHORT, SkinTone.FAIR, Accessory.JEWELS, FacialHair.NONE, Color(0xFFA855F7)),

    GuessWhoCharacter("11", "Bob", Gender.MALE, EyeColor.BROWN, HairColor.BROWN, HairStyle.SHORT, SkinTone.DARK, Accessory.NONE, FacialHair.NONE, Color(0xFFEF4444)),
    GuessWhoCharacter("12", "Bella", Gender.FEMALE, EyeColor.GREEN, HairColor.RED, HairStyle.SHORT, SkinTone.FAIR, Accessory.NONE, FacialHair.NONE, Color(0xFF3B82F6)),
    GuessWhoCharacter("13", "Sophia", Gender.FEMALE, EyeColor.BLACK, HairColor.BLONDE, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.JEWELS, FacialHair.NONE, Color(0xFFF59E0B)),
    GuessWhoCharacter("14", "Jack", Gender.MALE, EyeColor.GREEN, HairColor.GREY, HairStyle.SHORT, SkinTone.FAIR, Accessory.HAT, FacialHair.BEARD, Color(0xFF8B5CF6)),
    GuessWhoCharacter("15", "Jose", Gender.MALE, EyeColor.GREEN, HairColor.BLACK, HairStyle.BALD, SkinTone.DARK, Accessory.NONE, FacialHair.BEARD, Color(0xFF10B981)),

    GuessWhoCharacter("16", "Theo", Gender.MALE, EyeColor.BLUE, HairColor.BLONDE, HairStyle.SHORT, SkinTone.MEDIUM, Accessory.NONE, FacialHair.NONE, Color(0xFF6366F1)),
    GuessWhoCharacter("17", "Annie", Gender.FEMALE, EyeColor.BROWN, HairColor.RED, HairStyle.LONG, SkinTone.FAIR, Accessory.JEWELS, FacialHair.NONE, Color(0xFFEC4899)),
    GuessWhoCharacter("18", "Paul", Gender.MALE, EyeColor.GREEN, HairColor.RED, HairStyle.LONG, SkinTone.MEDIUM, Accessory.NONE, FacialHair.BEARD, Color(0xFFF43F5E)),
    GuessWhoCharacter("19", "Olivia", Gender.FEMALE, EyeColor.GREEN, HairColor.BLONDE, HairStyle.LONG, SkinTone.DARK, Accessory.JEWELS, FacialHair.NONE, Color(0xFF06B6D4)),
    GuessWhoCharacter("20", "Chloe", Gender.FEMALE, EyeColor.GREEN, HairColor.BROWN, HairStyle.LONG, SkinTone.MEDIUM, Accessory.JEWELS, FacialHair.NONE, Color(0xFFA855F7)),

    GuessWhoCharacter("21", "Nora", Gender.FEMALE, EyeColor.BLACK, HairColor.RED, HairStyle.SHORT, SkinTone.DARK, Accessory.GLASSES, FacialHair.NONE, Color(0xFFEF4444)),
    GuessWhoCharacter("22", "James", Gender.MALE, EyeColor.BROWN, HairColor.RED, HairStyle.SHORT, SkinTone.FAIR, Accessory.GLASSES, FacialHair.NONE, Color(0xFF3B82F6)),
    GuessWhoCharacter("23", "Bill", Gender.MALE, EyeColor.BROWN, HairColor.BLONDE, HairStyle.SHORT, SkinTone.FAIR, Accessory.NONE, FacialHair.NONE, Color(0xFF10B981)),
    GuessWhoCharacter("24", "Julia", Gender.FEMALE, EyeColor.BLACK, HairColor.GREY, HairStyle.SHORT, SkinTone.DARK, Accessory.JEWELS, FacialHair.NONE, Color(0xFFF59E0B)),
    GuessWhoCharacter("25", "Naomi", Gender.FEMALE, EyeColor.BLACK, HairColor.BLACK, HairStyle.LONG, SkinTone.FAIR, Accessory.JEWELS, FacialHair.NONE, Color(0xFF8B5CF6)),

    GuessWhoCharacter("26", "Ryan", Gender.MALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.SHORT, SkinTone.FAIR, Accessory.GLASSES, FacialHair.MUSTACHE, Color(0xFF6366F1)),
    GuessWhoCharacter("27", "David", Gender.MALE, EyeColor.BROWN, HairColor.BLACK, HairStyle.LONG, SkinTone.DARK, Accessory.NONE, FacialHair.MUSTACHE, Color(0xFF14B8A6)),
    GuessWhoCharacter("28", "Mila", Gender.FEMALE, EyeColor.BROWN, HairColor.BLONDE, HairStyle.LONG, SkinTone.DARK, Accessory.GLASSES, FacialHair.NONE, Color(0xFFEC4899)),
    GuessWhoCharacter("29", "John", Gender.MALE, EyeColor.BLUE, HairColor.GREY, HairStyle.SHORT, SkinTone.DARK, Accessory.HAT, FacialHair.BEARD, Color(0xFF06B6D4)),
    GuessWhoCharacter("30", "Ben", Gender.MALE, EyeColor.BLUE, HairColor.BLACK, HairStyle.BALD, SkinTone.DARK, Accessory.GLASSES, FacialHair.NONE, Color(0xFFF59E0B))
)

@Composable
fun GuessWhoScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeState.isDarkMode

    // Match & Phase States
    var currentRound by remember { mutableIntStateOf(1) }
    var p1Wins by remember { mutableIntStateOf(0) }
    var p2Wins by remember { mutableIntStateOf(0) }
    var isPlayer1Turn by remember { mutableStateOf(true) }
    var actionTakenThisTurn by remember { mutableStateOf(false) }

    // Game Phases & Selection Steps
    var gamePhase by remember { mutableStateOf("SELECTION") } // "SELECTION" or "PLAYING"
    var selectionStep by remember { mutableStateOf("P1_INTRO") } // "P1_INTRO", "P1", "P2_PASS", "P2", "COUNTDOWN", "DONE"
    var countdownText by remember { mutableStateOf("3") }

    // Character Carousel Selection State for Phase 1 & Phase 2
    var selectedCharacterIndex by remember { mutableIntStateOf(0) }
    var isCharacterSelected by remember { mutableStateOf(false) }
    var activeBoardCharacterIndex by remember { mutableIntStateOf(0) }

    var p1SecretCharacter by remember { mutableStateOf<GuessWhoCharacter?>(null) }
    var p2SecretCharacter by remember { mutableStateOf<GuessWhoCharacter?>(null) }

    // Roster Board States (Separate for Player 1 and Player 2!)
    var p1BoardCharacters by remember { mutableStateOf(ALL_CHARACTERS) }
    var p2BoardCharacters by remember { mutableStateOf(ALL_CHARACTERS) }
    val boardCharacters = if (isPlayer1Turn) p1BoardCharacters else p2BoardCharacters

    // Filter Dialog Modals
    var activeTraitModal by remember { mutableStateOf<String?>(null) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf("") }
    var winningPlayer by remember { mutableIntStateOf(1) }

    // Question Filter State
    var lastAskedQuestion by remember { mutableStateOf<String?>(null) }
    var lastQuestionAnswer by remember { mutableStateOf<Boolean?>(null) }
    var cardsTappedThisTurn by remember { mutableStateOf(false) }
    var hasAskedQuestionThisTurn by remember { mutableStateOf(false) }
    var showAskQuestionWarning by remember { mutableStateOf(false) }
    var warningAnimTrigger by remember { mutableIntStateOf(0) }
    var showHelpCard by remember { mutableStateOf(false) }
    var selectedGuessCharacterId by remember { mutableStateOf<String?>(null) }

    // Map of tested traits per player: key = "CATEGORY:OPTION" -> value = true (Yes) / false (No)
    var p1TestedTraits by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var p2TestedTraits by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // Category Keys Definitions
    val genderKeys = remember { listOf("GENDER:MALE", "GENDER:FEMALE") }
    val eyeColorKeys = remember { listOf("EYE_COLOR:BLUE", "EYE_COLOR:GREEN", "EYE_COLOR:BROWN", "EYE_COLOR:BLACK") }
    val hairKeys = remember { listOf("HAIR:SHORT", "HAIR:LONG", "HAIR:BALD") }
    val hairColorKeys = remember { listOf("HAIR_COLOR:BLACK", "HAIR_COLOR:BROWN", "HAIR_COLOR:BLONDE", "HAIR_COLOR:GRAY", "HAIR_COLOR:RED") }
    val skinToneKeys = remember { listOf("SKIN_TONE:FAIR", "SKIN_TONE:MEDIUM", "SKIN_TONE:DARK") }
    val accessoryKeys = remember { listOf("ACCESSORY:NONE", "ACCESSORY:GLASSES", "ACCESSORY:HAT", "ACCESSORY:JEWELS") }
    val facialHairKeys = remember { listOf("FACIAL_HAIR:NONE", "FACIAL_HAIR:MUSTACHE", "FACIAL_HAIR:BEARD") }

    fun getOptionStatus(optionKey: String, categoryKeys: List<String>): TraitStatus {
        val currentTested = if (isPlayer1Turn) p1TestedTraits else p2TestedTraits
        val testedVal = currentTested[optionKey]
        if (testedVal == true) return TraitStatus.TICK
        if (testedVal == false) return TraitStatus.CROSS

        // Check if all OTHER keys in this category were tested FALSE (cross) -> auto-tick remaining 1 option
        val otherKeys = categoryKeys.filter { it != optionKey }
        if (otherKeys.isNotEmpty() && otherKeys.all { currentTested[it] == false }) {
            return TraitStatus.TICK
        }

        // Check if ANY key in this category was tested TRUE (tick) -> auto-cross all other options
        val anyTrue = categoryKeys.any { currentTested[it] == true }
        if (anyTrue) {
            return TraitStatus.CROSS
        }

        return TraitStatus.NONE
    }

    fun isCategoryResolved(categoryKeys: List<String>): Boolean {
        return categoryKeys.all { getOptionStatus(it, categoryKeys) != TraitStatus.NONE }
    }

    LaunchedEffect(Unit) {
        SoundManager.init(context)
        SoundManager.playRetrySound()
    }

    LaunchedEffect(lastAskedQuestion) {
        if (lastAskedQuestion != null) {
            SoundManager.playClickSound()
        }
    }

    // Auto-setup for new round
    fun setupNewRound() {
        gamePhase = "SELECTION"
        selectionStep = "P1_INTRO"
        isPlayer1Turn = true
        actionTakenThisTurn = false
        cardsTappedThisTurn = false
        hasAskedQuestionThisTurn = false
        showAskQuestionWarning = false
        selectedGuessCharacterId = null
        selectedCharacterIndex = 0
        isCharacterSelected = false
        activeBoardCharacterIndex = 0
        p1SecretCharacter = null
        p2SecretCharacter = null
        p1TestedTraits = emptyMap()
        p2TestedTraits = emptyMap()
        p1BoardCharacters = ALL_CHARACTERS.map { it.copy(isEliminated = false) }
        p2BoardCharacters = ALL_CHARACTERS.map { it.copy(isEliminated = false) }
        lastAskedQuestion = null
        lastQuestionAnswer = null
    }

    LaunchedEffect(currentRound) {
        setupNewRound()
    }

    // Countdown animation handler (Faster snappy countdown)
    LaunchedEffect(selectionStep) {
        if (selectionStep == "COUNTDOWN") {
            countdownText = "3"
            delay(450)
            countdownText = "2"
            delay(450)
            countdownText = "1"
            delay(450)
            countdownText = "START!"
            delay(400)
            selectionStep = "DONE"
            gamePhase = "PLAYING"
        }
    }

    fun resetFullMatch() {
        currentRound = 1
        p1Wins = 0
        p2Wins = 0
        isPlayer1Turn = true
        showWinnerDialog = false
        setupNewRound()
        SoundManager.playRetrySound()
    }

    // Toggle Card Flip (Tap card to flip face down/up in Black & White)
    fun toggleCardFlip(clickedChar: GuessWhoCharacter) {
        if (!hasAskedQuestionThisTurn) {
            showAskQuestionWarning = true
            warningAnimTrigger++
            return
        }
        showAskQuestionWarning = false
        SoundManager.playClickSound()
        actionTakenThisTurn = true
        cardsTappedThisTurn = true

        val currentList = if (isPlayer1Turn) p1BoardCharacters else p2BoardCharacters
        val updatedList = currentList.map { char ->
            if (char.id == clickedChar.id) {
                char.copy(isEliminated = !char.isEliminated)
            } else {
                char
            }
        }

        if (isPlayer1Turn) {
            p1BoardCharacters = updatedList
        } else {
            p2BoardCharacters = updatedList
        }

        // Check if only 1 card remains face-up!
        val remainingFaceUp = updatedList.filter { !it.isEliminated }
        if (remainingFaceUp.size == 1) {
            val lastRemainingChar = remainingFaceUp.first()
            selectedGuessCharacterId = lastRemainingChar.id
            activeBoardCharacterIndex = 0
        } else {
            selectedGuessCharacterId = null
        }
    }

    // Question Filter Eliminator (With Trait Status Recording & Manual Card Elimination)
    fun applyQuestionFilter(
        questionText: String,
        traitKey: String,
        categoryKeys: List<String>,
        matchingCondition: (GuessWhoCharacter) -> Boolean
    ) {
        val targetSecret = if (isPlayer1Turn) p2SecretCharacter!! else p1SecretCharacter!!
        val isYes = matchingCondition(targetSecret)

        SoundManager.playClickSound()
        actionTakenThisTurn = true
        cardsTappedThisTurn = false
        hasAskedQuestionThisTurn = true
        showAskQuestionWarning = false
        selectedGuessCharacterId = null

        lastAskedQuestion = questionText
        lastQuestionAnswer = isYes
        showHelpCard = true

        // Update tested traits for current active player
        if (isPlayer1Turn) {
            val updated = p1TestedTraits.toMutableMap()
            updated[traitKey] = isYes
            if (isYes) {
                categoryKeys.filter { it != traitKey }.forEach { updated[it] = false }
            }
            p1TestedTraits = updated
        } else {
            val updated = p2TestedTraits.toMutableMap()
            updated[traitKey] = isYes
            if (isYes) {
                categoryKeys.filter { it != traitKey }.forEach { updated[it] = false }
            }
            p2TestedTraits = updated
        }

        // Keep boardCharacters unchanged so player manually taps cards to eliminate/hide them!
        activeTraitModal = null
    }

    // Determine Background Color
    val currentBgColor = when {
        gamePhase == "SELECTION" && selectionStep == "P2" -> Color(0xFF5C0F1B) // Dark Red BG for Player 2 Selection
        gamePhase == "SELECTION" -> Color(0xFF0D1B36) // Dark Blue BG for Player 1 Selection
        gamePhase == "PLAYING" && isPlayer1Turn -> Color(0xFF0D1B36) // Player 1 Dark Blue Theme
        else -> Color(0xFF5C0F1B) // Player 2 Dark Red Theme
    }

    // ── STEP 1 INTRO SCREEN & STEP 3 PASS SCREEN OVERLAYS ──
    if (gamePhase == "SELECTION" && selectionStep == "P1_INTRO") {
        PassMessageScreen(
            bgColor = Color(0xFF0D1B36), // Dark Blue Theme
            messageText = "Hide the screen\nfrom your friend\nand Choose Your\nCharacter",
            onOkClick = {
                SoundManager.playClickSound()
                isCharacterSelected = false
                selectionStep = "P1"
            }
        )
        return
    }

    if (gamePhase == "SELECTION" && selectionStep == "P2_PASS") {
        PassMessageScreen(
            bgColor = Color(0xFF5C0F1B), // Dark Red Theme
            messageText = "Pass the screen\nto your friend\nand don't look",
            onOkClick = {
                SoundManager.playClickSound()
                isCharacterSelected = false
                selectionStep = "P2"
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP HEADER BAR (Black Icons inside White 3D Circular Buttons) ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 3D Metallic Glassmorphism Back Button (Matching Memory Match Page)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF2C3E55), Color(0xFF1A2636))),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.6f)
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
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title Banner (Plain Text PLAYER 1 / PLAYER 2 without box)
                Text(
                    text = when {
                        gamePhase == "SELECTION" && selectionStep == "P1" -> "PLAYER 1"
                        gamePhase == "SELECTION" && selectionStep == "P2" -> "PLAYER 2"
                        gamePhase == "PLAYING" && isPlayer1Turn -> "PLAYER 1"
                        gamePhase == "PLAYING" && !isPlayer1Turn -> "PLAYER 2"
                        else -> "Guess Who?"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                // 3D Metallic Glassmorphism Retry Button (Matching Memory Match Page)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF2C3E55), Color(0xFF1A2636))),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.6f)
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
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset Match",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── PHASE 1: SINGLE BIG 3D CARD CHARACTER SELECTION (P1 -> P2 -> Countdown) ──
            if (gamePhase == "SELECTION") {
                if (selectionStep == "COUNTDOWN") {
                    // Dark Overlay with Big White Number 1 2 3 Countdown
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdownText,
                            fontSize = if (countdownText.length > 2) 48.sp else 110.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // P1 (Blue) or P2 (Red) Selection Step matching exact screenshot layout
                    val currentChar = ALL_CHARACTERS[selectedCharacterIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Big text: "select your character"
                        Text(
                            text = "select your character",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // Single Big 3D Character Card flanked by Left & Right 3D Arrows (Centered in screen)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 3D LEFT ARROW BUTTON (◀)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .shadow(10.dp, CircleShape)
                                    .background(
                                        Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        selectedCharacterIndex = (selectedCharacterIndex - 1 + ALL_CHARACTERS.size) % ALL_CHARACTERS.size
                                        isCharacterSelected = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Character",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // SINGLE BIG 3D CHARACTER CARD (Green Border & Name Box when selected)
                            SingleBig3DCharacterCard(
                                character = currentChar,
                                isSelected = isCharacterSelected,
                                onClick = {
                                    SoundManager.playClickSound()
                                    isCharacterSelected = !isCharacterSelected
                                }
                            )

                            // 3D RIGHT ARROW BUTTON (▶)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .shadow(10.dp, CircleShape)
                                    .background(
                                        Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        selectedCharacterIndex = (selectedCharacterIndex + 1) % ALL_CHARACTERS.size
                                        isCharacterSelected = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Character",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Rectangular Rounded-Corner 3D CONFIRM Button directly below image (Grey when unselected, White when selected, Bouncy Pop-Up Effect & Tap Sound)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(50.dp)
                                .shadow(if (isCharacterSelected) 10.dp else 2.dp, RoundedCornerShape(14.dp))
                                .background(
                                    if (isCharacterSelected)
                                        Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0)))
                                    else
                                        Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF475569))),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    if (isCharacterSelected) 2.dp else 0.dp,
                                    if (isCharacterSelected) Color.White else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .bouncyClickable(enabled = isCharacterSelected) {
                                    if (!isCharacterSelected) return@bouncyClickable
                                    SoundManager.playClickSound()
                                    isCharacterSelected = false
                                    if (selectionStep == "P1") {
                                        p1SecretCharacter = currentChar
                                        selectedCharacterIndex = 0
                                        selectionStep = "P2_PASS"
                                    } else {
                                        p2SecretCharacter = currentChar
                                        selectedCharacterIndex = 0
                                        selectionStep = "COUNTDOWN"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "CONFIRM",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isCharacterSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                // ── PHASE 2: DEDUCTION BOARD (SINGLE BIG CARD WITH LEFT/RIGHT ARROWS) ──
                val currentDeductionChar = boardCharacters[activeBoardCharacterIndex]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rectangular Rounded-Corner Help/Warning Card right above image (Fixed height 54.dp, Bouncy Pop-Up Effect, Bigger Size & Text)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val isVisible = showAskQuestionWarning || (showHelpCard && lastAskedQuestion != null)
                        val warningScaleAnim = remember { Animatable(1f) }
                        LaunchedEffect(warningAnimTrigger) {
                            if (warningAnimTrigger > 0) {
                                warningScaleAnim.snapTo(0.5f)
                                warningScaleAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }

                        if (isVisible) {
                            Box(
                                modifier = Modifier
                                    .scale(warningScaleAnim.value)
                                    .shadow(8.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        if (isPlayer1Turn) Color(0xFF1D4ED8) else Color(0xFFB91C1C),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (showAskQuestionWarning) "Press a button below to ask a question!" else "HELP: Tap image to hide or unhide candidate",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Single Big Character Card flanked by Left & Right 3D Arrow Buttons to Skip Candidates (Always visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 3D LEFT ARROW BUTTON (◀)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(10.dp, CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                                    CircleShape
                                )
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    activeBoardCharacterIndex = (activeBoardCharacterIndex - 1 + boardCharacters.size) % boardCharacters.size
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous Candidate",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // SINGLE BIG 3D DEDUCTION CARD (Tap to Select for Winner Guess or Hide/Unhide)
                        SingleBigDeductionCharacterCard(
                            character = currentDeductionChar,
                            isSelected = (selectedGuessCharacterId == currentDeductionChar.id),
                            onClick = {
                                showHelpCard = false
                                val remainingFaceUp = boardCharacters.filter { !it.isEliminated }
                                val isLastFaceUpCard = !currentDeductionChar.isEliminated &&
                                        remainingFaceUp.size == 1 &&
                                        remainingFaceUp.first().id == currentDeductionChar.id

                                if (isLastFaceUpCard) {
                                    SoundManager.playClickSound()
                                    if (selectedGuessCharacterId == currentDeductionChar.id) {
                                        selectedGuessCharacterId = null
                                    } else {
                                        selectedGuessCharacterId = currentDeductionChar.id
                                    }
                                } else {
                                    toggleCardFlip(currentDeductionChar)
                                }
                            }
                        )

                        // 3D RIGHT ARROW BUTTON (▶)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(10.dp, CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                                    CircleShape
                                )
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    selectedGuessCharacterId = null
                                    activeBoardCharacterIndex = (activeBoardCharacterIndex + 1) % boardCharacters.size
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Candidate",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // BELOW CHARACTER CARD SECTION:
                    // If a candidate is chosen for winner guess: Everything clears (question box, 7 category buttons, pass button hidden)!
                    // Show White 3D CONFIRM Button directly below the image!
                    if (selectedGuessCharacterId == currentDeductionChar.id) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    SoundManager.playClickSound()
                                    val targetSecret = if (isPlayer1Turn) p2SecretCharacter!! else p1SecretCharacter!!
                                    val isCorrect = currentDeductionChar.id == targetSecret.id

                                    if (isCorrect) {
                                        SoundManager.playCorrectSound()
                                        if (isPlayer1Turn) p1Wins++ else p2Wins++
                                        winnerMessage = if (isPlayer1Turn) "🎉 Player 1 Guessed Correctly! It's ${targetSecret.name}!" else "🎉 Player 2 Guessed Correctly! It's ${targetSecret.name}!"
                                        winningPlayer = if (isPlayer1Turn) 1 else 2
                                    } else {
                                        SoundManager.playWrongSound()
                                        if (isPlayer1Turn) p2Wins++ else p1Wins++
                                        winnerMessage = if (isPlayer1Turn) "❌ Wrong Guess! Player 2 Wins! It was ${targetSecret.name}." else "❌ Wrong Guess! Player 1 Wins! It was ${targetSecret.name}."
                                        winningPlayer = if (isPlayer1Turn) 2 else 1
                                    }
                                    showWinnerDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(50.dp)
                                    .shadow(10.dp, RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                            ) {
                                Text(
                                    "CONFIRM",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    selectedGuessCharacterId = null
                                    toggleCardFlip(currentDeductionChar)
                                }
                            ) {
                                Text("Hide Candidate", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (lastAskedQuestion != null) {
                        val questionBannerScale = remember { Animatable(0.3f) }
                        LaunchedEffect(lastAskedQuestion) {
                            questionBannerScale.snapTo(0.3f)
                            questionBannerScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .scale(questionBannerScale.value)
                                .fillMaxWidth(0.92f)
                                .shadow(10.dp, RoundedCornerShape(22.dp))
                                .background(Color.White, RoundedCornerShape(22.dp))
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lastAskedQuestion!!,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (lastQuestionAnswer == true) "👤 Yes!" else "❌ No!",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (lastQuestionAnswer == true) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    } else {
                        // BOTTOM TRAIT FILTER BUTTONS ROW (4 Above, 3 Below - ALL 7 CARDS EXACT SAME SIZE)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: 4 Cards (GENDER, EYE COLOR, HAIR, HAIR COLOR)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TraitCategoryButton(label = "GENDER", icon = "👫", isResolved = isCategoryResolved(genderKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "GENDER" }
                                TraitCategoryButton(label = "EYE COLOR", icon = "👁️", isResolved = isCategoryResolved(eyeColorKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "EYE_COLOR" }
                                TraitCategoryButton(label = "HAIR", icon = "💇", isResolved = isCategoryResolved(hairKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "HAIR" }
                                TraitCategoryButton(label = "HAIR COLOR", icon = "🎨", isResolved = isCategoryResolved(hairColorKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "HAIR_COLOR" }
                            }

                            // Row 2: 3 Cards (SKIN TONE, ACCESSORIES, FACIAL HAIR) - Centered 75% width (SAME SIZE!)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(0.75f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TraitCategoryButton(label = "SKIN TONE", icon = "🏽", isResolved = isCategoryResolved(skinToneKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "SKIN_TONE" }
                                    TraitCategoryButton(label = "ACCESSORIES", icon = "👓", isResolved = isCategoryResolved(accessoryKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "ACCESSORY" }
                                    TraitCategoryButton(label = "FACIAL HAIR", icon = "👨", isResolved = isCategoryResolved(facialHairKeys), modifier = Modifier.weight(1f)) { activeTraitModal = "FACIAL_HAIR" }
                                }
                            }
                        }
                    }

                    // SMALL 3D PASS BUTTON CONTAINER (Fixed height 46.dp so image position NEVER shifts when PASS appears/disappears)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cardsTappedThisTurn && selectedGuessCharacterId == null) {
                            Button(
                                onClick = {
                                    SoundManager.playClickSound()
                                    if (isPlayer1Turn) {
                                        p1BoardCharacters = p1BoardCharacters.sortedBy { it.isEliminated }
                                    } else {
                                        p2BoardCharacters = p2BoardCharacters.sortedBy { it.isEliminated }
                                    }
                                    isPlayer1Turn = !isPlayer1Turn
                                    actionTakenThisTurn = false
                                    cardsTappedThisTurn = false
                                    hasAskedQuestionThisTurn = false
                                    showAskQuestionWarning = false
                                    showHelpCard = false
                                    selectedGuessCharacterId = null
                                    lastAskedQuestion = null
                                    lastQuestionAnswer = null
                                    activeBoardCharacterIndex = 0
                                    Toast.makeText(
                                        context,
                                        if (isPlayer1Turn) "🎮 Player 1's Turn!" else "🎮 Player 2's Turn!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(42.dp)
                                    .shadow(8.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Text("PASS", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

            }
        }

        // ── TRAIT SELECTION MODAL DIALOGS ─────────────
        if (activeTraitModal != null) {
            val modalWidthFraction = when (activeTraitModal) {
                "GENDER" -> 0.70f
                "EYE_COLOR" -> 0.78f
                "HAIR", "SKIN_TONE", "FACIAL_HAIR" -> 0.85f
                else -> 0.88f
            }

            Dialog(
                onDismissRequest = { activeTraitModal = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(modalWidthFraction)
                            .shadow(20.dp, RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                            .padding(top = 16.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Title & 3D Floating Red Close Button (X) Header
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeTraitModal!!.replace("_", " "),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                // 3D Red Floating Close Button (Without White Outline)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 6.dp, y = (-8).dp)
                                        .size(38.dp)
                                        .shadow(8.dp, CircleShape)
                                        .background(
                                            Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626))),
                                            CircleShape
                                        )
                                        .clickable {
                                            SoundManager.playClickSound()
                                            activeTraitModal = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            when (activeTraitModal) {
                                "GENDER" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TraitOptionCard("MALE", icon = "👨", status = getOptionStatus("GENDER:MALE", genderKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Is the person male?", "GENDER:MALE", genderKeys) { it.gender == Gender.MALE }
                                        }
                                        TraitOptionCard("FEMALE", icon = "👩", status = getOptionStatus("GENDER:FEMALE", genderKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Is the person female?", "GENDER:FEMALE", genderKeys) { it.gender == Gender.FEMALE }
                                        }
                                    }
                                }
                                "EYE_COLOR" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("BLUE", colorSwatch = Color(0xFF2563EB), status = getOptionStatus("EYE_COLOR:BLUE", eyeColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have blue eyes?", "EYE_COLOR:BLUE", eyeColorKeys) { it.eyeColor == EyeColor.BLUE }
                                            }
                                            TraitOptionCard("GREEN", colorSwatch = Color(0xFF16A34A), status = getOptionStatus("EYE_COLOR:GREEN", eyeColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have green eyes?", "EYE_COLOR:GREEN", eyeColorKeys) { it.eyeColor == EyeColor.GREEN }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("BROWN", colorSwatch = Color(0xFF92400E), status = getOptionStatus("EYE_COLOR:BROWN", eyeColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have brown eyes?", "EYE_COLOR:BROWN", eyeColorKeys) { it.eyeColor == EyeColor.BROWN }
                                            }
                                            TraitOptionCard("BLACK", colorSwatch = Color(0xFF1F2937), status = getOptionStatus("EYE_COLOR:BLACK", eyeColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have black eyes?", "EYE_COLOR:BLACK", eyeColorKeys) { it.eyeColor == EyeColor.BLACK }
                                            }
                                        }
                                    }
                                }
                                "HAIR" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TraitOptionCard("SHORT", icon = "👨‍🦱", status = getOptionStatus("HAIR:SHORT", hairKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have short hair?", "HAIR:SHORT", hairKeys) { it.hairStyle == HairStyle.SHORT }
                                        }
                                        TraitOptionCard("LONG", icon = "👩‍🦰", status = getOptionStatus("HAIR:LONG", hairKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have long hair?", "HAIR:LONG", hairKeys) { it.hairStyle == HairStyle.LONG }
                                        }
                                        TraitOptionCard("BALD", icon = "👨‍🦲", status = getOptionStatus("HAIR:BALD", hairKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Is the person bald?", "HAIR:BALD", hairKeys) { it.hairStyle == HairStyle.BALD }
                                        }
                                    }
                                }
                                "HAIR_COLOR" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TraitOptionCard("BROWN", colorSwatch = Color(0xFF92400E), status = getOptionStatus("HAIR_COLOR:BROWN", hairColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have brown hair?", "HAIR_COLOR:BROWN", hairColorKeys) { it.hairColor == HairColor.BROWN }
                                            }
                                            TraitOptionCard("BLACK", colorSwatch = Color(0xFF1F2937), status = getOptionStatus("HAIR_COLOR:BLACK", hairColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have black hair?", "HAIR_COLOR:BLACK", hairColorKeys) { it.hairColor == HairColor.BLACK }
                                            }
                                            TraitOptionCard("BLONDE", colorSwatch = Color(0xFFF59E0B), status = getOptionStatus("HAIR_COLOR:BLONDE", hairColorKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have blonde hair?", "HAIR_COLOR:BLONDE", hairColorKeys) { it.hairColor == HairColor.BLONDE }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(0.66f),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                TraitOptionCard("GRAY", colorSwatch = Color(0xFF9CA3AF), status = getOptionStatus("HAIR_COLOR:GRAY", hairColorKeys), modifier = Modifier.weight(1f)) {
                                                    applyQuestionFilter("Does the person have gray hair?", "HAIR_COLOR:GRAY", hairColorKeys) { it.hairColor == HairColor.GREY }
                                                }
                                                TraitOptionCard("RED", colorSwatch = Color(0xFFDC2626), status = getOptionStatus("HAIR_COLOR:RED", hairColorKeys), modifier = Modifier.weight(1f)) {
                                                    applyQuestionFilter("Does the person have red hair?", "HAIR_COLOR:RED", hairColorKeys) { it.hairColor == HairColor.RED }
                                                }
                                            }
                                        }
                                    }
                                }
                                "SKIN_TONE" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TraitOptionCard("FAIR", colorSwatch = Color(0xFFFDE68A), status = getOptionStatus("SKIN_TONE:FAIR", skinToneKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have fair skin?", "SKIN_TONE:FAIR", skinToneKeys) { it.skinTone == SkinTone.FAIR }
                                        }
                                        TraitOptionCard("MEDIUM", colorSwatch = Color(0xFFD97706), status = getOptionStatus("SKIN_TONE:MEDIUM", skinToneKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have medium skin?", "SKIN_TONE:MEDIUM", skinToneKeys) { it.skinTone == SkinTone.MEDIUM }
                                        }
                                        TraitOptionCard("DARK", colorSwatch = Color(0xFF78350F), status = getOptionStatus("SKIN_TONE:DARK", skinToneKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have dark skin?", "SKIN_TONE:DARK", skinToneKeys) { it.skinTone == SkinTone.DARK }
                                        }
                                    }
                                }
                                "ACCESSORY" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TraitOptionCard("NONE", icon = "🚫", status = getOptionStatus("ACCESSORY:NONE", accessoryKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have no accessories?", "ACCESSORY:NONE", accessoryKeys) { it.accessory == Accessory.NONE }
                                            }
                                            TraitOptionCard("GLASSES", icon = "👓", status = getOptionStatus("ACCESSORY:GLASSES", accessoryKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing glasses?", "ACCESSORY:GLASSES", accessoryKeys) { it.accessory == Accessory.GLASSES }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TraitOptionCard("HAT", icon = "🧢", status = getOptionStatus("ACCESSORY:HAT", accessoryKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing a hat?", "ACCESSORY:HAT", accessoryKeys) { it.accessory == Accessory.HAT }
                                            }
                                            TraitOptionCard("JEWELS", icon = "💍", status = getOptionStatus("ACCESSORY:JEWELS", accessoryKeys), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing jewels?", "ACCESSORY:JEWELS", accessoryKeys) { it.accessory == Accessory.JEWELS }
                                            }
                                        }
                                    }
                                }
                                "FACIAL_HAIR" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TraitOptionCard("NONE", icon = "🚫", status = getOptionStatus("FACIAL_HAIR:NONE", facialHairKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have no facial hair?", "FACIAL_HAIR:NONE", facialHairKeys) { it.facialHair == FacialHair.NONE }
                                        }
                                        TraitOptionCard("MUSTACHE", icon = "🥸", status = getOptionStatus("FACIAL_HAIR:MUSTACHE", facialHairKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have a mustache?", "FACIAL_HAIR:MUSTACHE", facialHairKeys) { it.facialHair == FacialHair.MUSTACHE }
                                        }
                                        TraitOptionCard("BEARD", icon = "🧔", status = getOptionStatus("FACIAL_HAIR:BEARD", facialHairKeys), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have a beard?", "FACIAL_HAIR:BEARD", facialHairKeys) { it.facialHair == FacialHair.BEARD }
                                        }
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
                                color = if (isDark) Color.White else Color(0xFF0F172A),
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

// ── PASS MESSAGE SCREEN COMPONENT (MATCHING SCREENSHOTS 1 & 2) ─────────────
@Composable
fun PassMessageScreen(
    bgColor: Color,
    messageText: String,
    onOkClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Center Big White Message Text (Matching screenshots 1 & 2)
            Text(
                text = messageText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 42.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Small Green 3D OK Button directly below text (With bouncy 3D pop effect)
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp)
                    .shadow(10.dp, RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                        RoundedCornerShape(22.dp)
                    )
                    .bouncyClickable(onClick = onOkClick),
                contentAlignment = Alignment.Center
            ) {
                Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ── SINGLE BIG 3D CHARACTER CARD FOR PHASE 1 SELECTION ───────────────────
@Composable
fun SingleBig3DCharacterCard(
    character: GuessWhoCharacter,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val rawName = character.name.lowercase()
    val imageResId = remember(character.name) {
        val nameVariant = if (rawName == "sara") "sora" else rawName
        var id = context.resources.getIdentifier(rawName, "drawable", context.packageName)
        if (id == 0) id = context.resources.getIdentifier("avatar_$rawName", "drawable", context.packageName)
        if (id == 0) id = context.resources.getIdentifier(nameVariant, "drawable", context.packageName)
        if (id == 0) id = context.resources.getIdentifier("avatar_$nameVariant", "drawable", context.packageName)
        id
    }

    Box(
        modifier = Modifier
            .width(230.dp)
            .height(310.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                if (isSelected) Color(0xFF10B981) else Color.White,
                RoundedCornerShape(24.dp)
            )
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Big Avatar Image Container AT THE TOP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(character.avatarBgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                                .size(90.dp)
                                .background(
                                    when (character.skinTone) {
                                        SkinTone.FAIR -> Color(0xFFFDE68A)
                                        SkinTone.MEDIUM -> Color(0xFFD97706)
                                        SkinTone.DARK -> Color(0xFF78350F)
                                    },
                                    CircleShape
                                )
                                .border(3.dp, hairColorVal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (character.accessory == Accessory.GLASSES) "👓"
                                else if (character.accessory == Accessory.HAT) "🧢"
                                else if (character.facialHair != FacialHair.NONE) "🧔"
                                else "👤",
                                fontSize = 48.sp
                            )
                        }
                    }
                }
            }

            // Horizontal White Line above name pallet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(if (isSelected) Color(0xFF10B981) else Color.White)
            )

            // Big Character Name Ribbon AT THE BOTTOM (Below Image)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B),
                        RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

// ── SINGLE BIG DEDUCTION CHARACTER CARD FOR PHASE 2 ────────────────────
@Composable
fun SingleBigDeductionCharacterCard(
    character: GuessWhoCharacter,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val rawName = character.name.lowercase()
    val imageResId = remember(character.name) {
        val nameVariant = if (rawName == "sara") "sora" else rawName
        var id = context.resources.getIdentifier(rawName, "drawable", context.packageName)
        if (id == 0) id = context.resources.getIdentifier("avatar_$rawName", "drawable", context.packageName)
        if (id == 0) id = context.resources.getIdentifier(nameVariant, "drawable", context.packageName)
        if (id == 0) id = context.resources.getIdentifier("avatar_$nameVariant", "drawable", context.packageName)
        id
    }
    val isHidden = character.isEliminated

    Box(
        modifier = Modifier
            .width(230.dp)
            .height(310.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(if (isHidden) Color(0xFF1E293B) else Color.White, RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                if (isSelected) Color(0xFF10B981) else if (isHidden) Color(0xFF64748B) else Color.White,
                RoundedCornerShape(24.dp)
            )
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Big Avatar Image Container AT THE TOP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        if (isHidden) Color(0xFF0F172A)
                        else character.avatarBgColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (isHidden) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (isHidden) 0.45f else 1.0f }
                    )
                } else {
                    val hairColorVal = when (character.hairColor) {
                        HairColor.BLACK -> Color(0xFF17181C)
                        HairColor.BROWN -> Color(0xFF78350F)
                        HairColor.BLONDE -> Color(0xFFF59E0B)
                        HairColor.RED -> Color(0xFFDC2626)
                        HairColor.GREY -> Color(0xFF9CA3AF)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { alpha = if (isHidden) 0.45f else 1.0f }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    if (isHidden) Color(0xFF64748B) else when (character.skinTone) {
                                        SkinTone.FAIR -> Color(0xFFFDE68A)
                                        SkinTone.MEDIUM -> Color(0xFFD97706)
                                        SkinTone.DARK -> Color(0xFF78350F)
                                    },
                                    CircleShape
                                )
                                .border(3.dp, if (isHidden) Color.DarkGray else hairColorVal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (character.accessory == Accessory.GLASSES) "👓"
                                else if (character.accessory == Accessory.HAT) "🧢"
                                else if (character.facialHair != FacialHair.NONE) "🧔"
                                else "👤",
                                fontSize = 48.sp
                            )
                        }
                    }
                }

                // Subtle dark overlay if eliminated
                if (isHidden) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }

            // Horizontal Line above name pallet (Green when selected, Grey when hidden, White normal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        if (isSelected) Color(0xFF10B981)
                        else if (isHidden) Color(0xFF64748B)
                        else Color.White
                    )
            )

            // Big Character Name Ribbon AT THE BOTTOM (Below Image with X symbol next to name if hidden, Green when selected)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0xFF10B981)
                        else if (isHidden) Color(0xFF0F172A) else Color(0xFF1E293B),
                        RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else if (isHidden) Color(0xFF94A3B8) else Color.White
                )
            }
        }
    }
}

@Composable
fun GuessWhoCharacterCardItem(
    character: GuessWhoCharacter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isHidden = character.isEliminated

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(12.dp))
            .background(
                if (isHidden) Color(0xFF1E293B) else Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                if (isSelected) 3.5.dp else 0.dp,
                if (isSelected) Color(0xFF10B981) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar Box / Custom Uploaded Image
            val context = LocalContext.current
            val rawName = character.name.lowercase()
            val imageResId = remember(character.name) {
                val nameVariant = if (rawName == "sara") "sora" else rawName
                var id = context.resources.getIdentifier(rawName, "drawable", context.packageName)
                if (id == 0) id = context.resources.getIdentifier("avatar_$rawName", "drawable", context.packageName)
                if (id == 0) id = context.resources.getIdentifier(nameVariant, "drawable", context.packageName)
                if (id == 0) id = context.resources.getIdentifier("avatar_$nameVariant", "drawable", context.packageName)
                id
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(
                        if (isHidden) Color(0xFF0F172A)
                        else character.avatarBgColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (isHidden) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (isHidden) 0.45f else 1.0f }
                    )
                } else {
                    val hairColorVal = when (character.hairColor) {
                        HairColor.BLACK -> Color(0xFF17181C)
                        HairColor.BROWN -> Color(0xFF78350F)
                        HairColor.BLONDE -> Color(0xFFF59E0B)
                        HairColor.RED -> Color(0xFFDC2626)
                        HairColor.GREY -> Color(0xFF9CA3AF)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { alpha = if (isHidden) 0.45f else 1.0f }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    if (isHidden) Color(0xFF64748B) else when (character.skinTone) {
                                        SkinTone.FAIR -> Color(0xFFFDE68A)
                                        SkinTone.MEDIUM -> Color(0xFFD97706)
                                        SkinTone.DARK -> Color(0xFF78350F)
                                    },
                                    CircleShape
                                )
                                .border(1.5.dp, if (isHidden) Color.DarkGray else hairColorVal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (character.accessory == Accessory.GLASSES) "👓"
                                else if (character.accessory == Accessory.HAT) "🧢"
                                else if (character.facialHair != FacialHair.NONE) "🧔"
                                else "👤",
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Dark cross overlay if hidden
                if (isHidden) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Eliminated",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Name Ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHidden) Color(0xFF0F172A) else Color(0xFF1E293B),
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isHidden) Color(0xFF94A3B8) else Color.White
                )
            }
        }
    }
}

// ── TRAIT CATEGORY BUTTON COMPONENT (3D WHITE, NO OUTLINE, DARK WHEN RESOLVED) ───────
@Composable
fun TraitCategoryButton(
    label: String,
    icon: String,
    isResolved: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(
                if (isResolved) Color(0xFF334155) else Color.White,
                RoundedCornerShape(16.dp)
            )
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isResolved) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── TRAIT OPTION CARD COMPONENT (3D SMALL SQUARE CARDS WITH INSIDE CORNER BADGE) ─────
@Composable
fun TraitOptionCard(
    label: String,
    icon: String? = null,
    colorSwatch: Color? = null,
    status: TraitStatus = TraitStatus.NONE,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isCross = status == TraitStatus.CROSS
    val isTick = status == TraitStatus.TICK
    val isResolved = isCross || isTick

    Box(
        modifier = modifier
            .height(76.dp)
            .shadow(if (isResolved) 2.dp else 6.dp, RoundedCornerShape(16.dp))
            .background(
                if (isResolved) Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF334155)))
                else Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))),
                RoundedCornerShape(16.dp)
            )
            .border(
                if (isTick) 2.5.dp else 1.5.dp,
                if (isTick) Color(0xFF10B981) else if (isCross) Color(0xFF64748B) else Color(0xFFCBD5E1),
                RoundedCornerShape(16.dp)
            )
            .bouncyClickable(enabled = !isResolved, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            if (colorSwatch != null) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .shadow(3.dp, RoundedCornerShape(8.dp))
                        .background(
                            if (isResolved) colorSwatch.copy(alpha = 0.4f) else colorSwatch,
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                )
            } else if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier.graphicsLayer { alpha = if (isResolved) 0.5f else 1.0f }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isResolved) Color.White.copy(alpha = 0.75f) else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // Inside Corner Badge with Bold Tick or Wrong Icon (Placed safely inside card bounds!)
        if (isTick) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(20.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(1.5.dp, Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Correct",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(13.dp)
                )
            }
        } else if (isCross) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(20.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(1.5.dp, Color(0xFFEF4444), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Incorrect",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

