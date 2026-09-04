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
import kotlinx.coroutines.delay

// ── CHARACTER DATA MODEL ─────────────────────────────────────────────────
enum class Gender { MALE, FEMALE }
enum class EyeColor { BLUE, GREEN, BROWN, BLACK }
enum class HairColor { BLACK, BROWN, BLONDE, RED, GREY }
enum class HairStyle { SHORT, LONG, BALD }
enum class SkinTone { FAIR, MEDIUM, DARK }
enum class Accessory { GLASSES, HAT, EARRINGS, JEWELS, NECKLACE, NONE }
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

    // Character Carousel Selection State for Phase 1
    var selectedCharacterIndex by remember { mutableIntStateOf(0) }

    var p1SecretCharacter by remember { mutableStateOf<GuessWhoCharacter?>(null) }
    var p2SecretCharacter by remember { mutableStateOf<GuessWhoCharacter?>(null) }

    // Roster Board State
    var boardCharacters by remember { mutableStateOf(ALL_CHARACTERS) }

    // Filter Dialog Modals
    var activeTraitModal by remember { mutableStateOf<String?>(null) }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf("") }
    var winningPlayer by remember { mutableIntStateOf(1) }

    // Question Filter State
    var lastAskedQuestion by remember { mutableStateOf<String?>(null) }
    var lastQuestionAnswer by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        SoundManager.playRetrySound()
    }

    // Auto-setup for new round
    fun setupNewRound() {
        gamePhase = "SELECTION"
        selectionStep = "P1_INTRO"
        isPlayer1Turn = true
        actionTakenThisTurn = false
        selectedCharacterIndex = 0
        p1SecretCharacter = null
        p2SecretCharacter = null
        boardCharacters = ALL_CHARACTERS.map { it.copy(isEliminated = false) }
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
            SoundManager.playPopSound()
            delay(450)
            countdownText = "2"
            SoundManager.playPopSound()
            delay(450)
            countdownText = "1"
            SoundManager.playPopSound()
            delay(450)
            countdownText = "START!"
            SoundManager.playCorrectSound()
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

    // Toggle Card Flip (Tap card to flip face down/up)
    fun toggleCardFlip(clickedChar: GuessWhoCharacter) {
        SoundManager.playPopSound()
        actionTakenThisTurn = true

        val updatedList = boardCharacters.map { char ->
            if (char.id == clickedChar.id) {
                char.copy(isEliminated = !char.isEliminated)
            } else {
                char
            }
        }
        boardCharacters = updatedList

        // Check if only 1 card remains face-up!
        val remainingFaceUp = updatedList.filter { !it.isEliminated }
        if (remainingFaceUp.size == 1) {
            val lastRemainingChar = remainingFaceUp.first()
            val targetSecret = if (isPlayer1Turn) p2SecretCharacter!! else p1SecretCharacter!!

            if (lastRemainingChar.id == targetSecret.id) {
                SoundManager.playCorrectSound()
                if (isPlayer1Turn) p1Wins++ else p2Wins++
                winnerMessage = if (isPlayer1Turn) "🎉 Player 1 Guessed Correctly! It's ${targetSecret.name}!" else "🎉 Player 2 Wins! It's ${targetSecret.name}!"
                winningPlayer = if (isPlayer1Turn) 1 else 2
                showWinnerDialog = true
            }
        }
    }

    // Question Filter Eliminator
    fun applyQuestionFilter(questionText: String, matchingCondition: (GuessWhoCharacter) -> Boolean) {
        val targetSecret = if (isPlayer1Turn) p2SecretCharacter!! else p1SecretCharacter!!
        val isYes = matchingCondition(targetSecret)

        SoundManager.playPopSound()
        actionTakenThisTurn = true

        lastAskedQuestion = questionText
        lastQuestionAnswer = isYes

        boardCharacters = boardCharacters.map { char ->
            val matchesFilter = matchingCondition(char)
            if (isYes) {
                char.copy(isEliminated = char.isEliminated || !matchesFilter)
            } else {
                char.copy(isEliminated = char.isEliminated || matchesFilter)
            }
        }

        activeTraitModal = null

        val remaining = boardCharacters.filter { !it.isEliminated }
        if (remaining.size == 1) {
            val lastChar = remaining.first()
            if (lastChar.id == targetSecret.id) {
                if (isPlayer1Turn) p1Wins++ else p2Wins++
                winnerMessage = if (isPlayer1Turn) "🎉 Player 1 Guessed Correctly! It's ${targetSecret.name}!" else "🎉 Player 2 Wins!"
                winningPlayer = if (isPlayer1Turn) 1 else 2
                showWinnerDialog = true
            }
        }
    }

    // Determine Background Color
    val currentBgColor = when {
        gamePhase == "SELECTION" -> Color(0xFF0D1424) // Deep Dark Slate Theme
        gamePhase == "PLAYING" && isPlayer1Turn -> Color(0xFF1D4ED8) // Player 1 Turn Theme (Blue)
        else -> Color(0xFFB91C1C) // Player 2 Turn Theme (Red)
    }

    // ── STEP 1 INTRO SCREEN & STEP 3 PASS SCREEN OVERLAYS ──
    if (gamePhase == "SELECTION" && selectionStep == "P1_INTRO") {
        PassMessageScreen(
            bgColor = Color(0xFF0F2B5C), // Dark Blue Theme matching user request
            messageText = "Hide the screen\nfrom your friend\nand choose your\ncharacter",
            onOkClick = {
                SoundManager.playClickSound()
                selectionStep = "P1"
            }
        )
        return
    }

    if (gamePhase == "SELECTION" && selectionStep == "P2_PASS") {
        PassMessageScreen(
            bgColor = Color(0xFFDC2626), // Red Theme matching user request
            messageText = "Pass the screen\nto your friend\nand don't look",
            onOkClick = {
                SoundManager.playClickSound()
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
                // White 3D Back Button with BLACK Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .clickable {
                            SoundManager.playClickSound()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title Banner
                Text(
                    text = if (gamePhase == "PLAYING") (if (isPlayer1Turn) "PLAYER 1 TURN" else "PLAYER 2 TURN") else "Guess Who?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                // White 3D Retry Button with BLACK Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .clickable {
                            SoundManager.playRetrySound()
                            resetFullMatch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset Match",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GET READY!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = countdownText,
                                fontSize = if (countdownText.length > 2) 48.sp else 110.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
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
                        // Centered Blue Box (PLAYER 1) or Red Box (PLAYER 2)
                        Box(
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(14.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (selectionStep == "P1") listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        else listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                                .padding(horizontal = 34.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectionStep == "P1") "PLAYER 1" else "PLAYER 2",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Big text: "choose your character"
                        Text(
                            text = "choose your character",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // Single Big 3D Character Card flanked by Left & Right 3D Arrows (Matching screenshot)
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
                                        SoundManager.playPopSound()
                                        selectedCharacterIndex = (selectedCharacterIndex - 1 + ALL_CHARACTERS.size) % ALL_CHARACTERS.size
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

                            // SINGLE BIG 3D CHARACTER CARD (White Border & Top Name Ribbon)
                            SingleBig3DCharacterCard(character = currentChar)

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
                                        SoundManager.playPopSound()
                                        selectedCharacterIndex = (selectedCharacterIndex + 1) % ALL_CHARACTERS.size
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

                        // Rectangular Rounded-Corner Green CONFIRM Button directly below image
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(50.dp)
                                .shadow(10.dp, RoundedCornerShape(14.dp))
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                        ) {
                            Text("CONFIRM", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                // ── PHASE 2: DEDUCTION BOARD & PASS TURN CONTROL ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 5x6 Roster Grid (Square Cards, filling sides, non-scrollable)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(boardCharacters) { char ->
                            GuessWhoCharacterCardItem(
                                character = char,
                                isSelected = false,
                                onClick = {
                                    toggleCardFlip(char)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── PASS CHANCE BUTTON TO SWITCH TURN ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp)
                            .shadow(10.dp, RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (isPlayer1Turn) listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                    else listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
                                ),
                                RoundedCornerShape(18.dp)
                            )
                            .border(1.5.dp, Color.White, RoundedCornerShape(18.dp))
                            .clickable {
                                SoundManager.playClickSound()
                                isPlayer1Turn = !isPlayer1Turn
                                actionTakenThisTurn = false
                                Toast.makeText(
                                    context,
                                    if (isPlayer1Turn) "🎮 Player 1's Turn!" else "🎮 Player 2's Turn!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isPlayer1Turn) "PASS CHANCE TO PLAYER 2" else "PASS CHANCE TO PLAYER 1",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Pass Turn", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // BOTTOM TRAIT FILTER BUTTONS ROW
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: GENDER, EYE COLOR, HAIR, HAIR COLOR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TraitCategoryButton(label = "GENDER", icon = "👫", modifier = Modifier.weight(1f)) { activeTraitModal = "GENDER" }
                            TraitCategoryButton(label = "EYE COLOR", icon = "👁️", modifier = Modifier.weight(1.1f)) { activeTraitModal = "EYE_COLOR" }
                            TraitCategoryButton(label = "HAIR", icon = "💇", modifier = Modifier.weight(0.9f)) { activeTraitModal = "HAIR" }
                            TraitCategoryButton(label = "HAIR COLOR", icon = "🎨", modifier = Modifier.weight(1.1f)) { activeTraitModal = "HAIR_COLOR" }
                        }

                        // Row 2: SKIN TONE, ACCESSORIES, FACIAL HAIR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TraitCategoryButton(label = "SKIN TONE", icon = "🏽", modifier = Modifier.weight(1f)) { activeTraitModal = "SKIN_TONE" }
                            TraitCategoryButton(label = "ACCESSORIES", icon = "👓", modifier = Modifier.weight(1.2f)) { activeTraitModal = "ACCESSORY" }
                            TraitCategoryButton(label = "FACIAL HAIR", icon = "👨", modifier = Modifier.weight(1.2f)) { activeTraitModal = "FACIAL_HAIR" }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // ── TRAIT SELECTION MODAL DIALOGS ─────────────
        if (activeTraitModal != null) {
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
                            .fillMaxWidth(0.92f)
                            .shadow(20.dp, RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                            .border(2.5.dp, Color.White, RoundedCornerShape(24.dp))
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

                                // 3D Red Floating Close Button
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
                                        .border(2.dp, Color.White, CircleShape)
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

                            Spacer(modifier = Modifier.height(20.dp))

                            when (activeTraitModal) {
                                "GENDER" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TraitOptionCard("MALE", icon = "👨", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Is the person male?") { it.gender == Gender.MALE }
                                        }
                                        TraitOptionCard("FEMALE", icon = "👩", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Is the person female?") { it.gender == Gender.FEMALE }
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
                                            TraitOptionCard("BLUE", colorSwatch = Color(0xFF2563EB), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have blue eyes?") { it.eyeColor == EyeColor.BLUE }
                                            }
                                            TraitOptionCard("GREEN", colorSwatch = Color(0xFF16A34A), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have green eyes?") { it.eyeColor == EyeColor.GREEN }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("BROWN", colorSwatch = Color(0xFF92400E), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have brown eyes?") { it.eyeColor == EyeColor.BROWN }
                                            }
                                            TraitOptionCard("BLACK", colorSwatch = Color(0xFF1F2937), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have black eyes?") { it.eyeColor == EyeColor.BLACK }
                                            }
                                        }
                                    }
                                }
                                "HAIR" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TraitOptionCard("SHORT", icon = "👨‍🦱", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have short hair?") { it.hairStyle == HairStyle.SHORT }
                                        }
                                        TraitOptionCard("LONG", icon = "👩‍🦰", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have long hair?") { it.hairStyle == HairStyle.LONG }
                                        }
                                        TraitOptionCard("BALD", icon = "👨‍🦲", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Is the person bald?") { it.hairStyle == HairStyle.BALD }
                                        }
                                    }
                                }
                                "HAIR_COLOR" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("BROWN", colorSwatch = Color(0xFF92400E), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have brown hair?") { it.hairColor == HairColor.BROWN }
                                            }
                                            TraitOptionCard("BLACK", colorSwatch = Color(0xFF1F2937), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have black hair?") { it.hairColor == HairColor.BLACK }
                                            }
                                            TraitOptionCard("BLONDE", colorSwatch = Color(0xFFF59E0B), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have blonde hair?") { it.hairColor == HairColor.BLONDE }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("GRAY", icon = "👵", modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have gray hair?") { it.hairColor == HairColor.GREY }
                                            }
                                            TraitOptionCard("RED", colorSwatch = Color(0xFFDC2626), modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have red hair?") { it.hairColor == HairColor.RED }
                                            }
                                        }
                                    }
                                }
                                "SKIN_TONE" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TraitOptionCard("FAIR", colorSwatch = Color(0xFFFDE68A), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have fair skin?") { it.skinTone == SkinTone.FAIR }
                                        }
                                        TraitOptionCard("MEDIUM", colorSwatch = Color(0xFFD97706), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have medium skin?") { it.skinTone == SkinTone.MEDIUM }
                                        }
                                        TraitOptionCard("DARK", colorSwatch = Color(0xFF78350F), modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have dark skin?") { it.skinTone == SkinTone.DARK }
                                        }
                                    }
                                }
                                "ACCESSORY" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("NONE", icon = "🚫", modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Does the person have no accessories?") { it.accessory == Accessory.NONE }
                                            }
                                            TraitOptionCard("GLASSES", icon = "👓", modifier = Modifier.weight(1.1f)) {
                                                applyQuestionFilter("Is the person wearing glasses?") { it.accessory == Accessory.GLASSES }
                                            }
                                            TraitOptionCard("HAT", icon = "🧢", modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing a hat?") { it.accessory == Accessory.HAT }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            TraitOptionCard("EARRINGS", icon = "💎", modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing earrings?") { it.accessory == Accessory.EARRINGS || it.accessory == Accessory.JEWELS }
                                            }
                                            TraitOptionCard("JEWELS", icon = "💍", modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing jewels?") { it.accessory == Accessory.JEWELS || it.accessory == Accessory.EARRINGS || it.accessory == Accessory.NECKLACE }
                                            }
                                            TraitOptionCard("NECKLACE", icon = "📿", modifier = Modifier.weight(1f)) {
                                                applyQuestionFilter("Is the person wearing a necklace?") { it.accessory == Accessory.NECKLACE || it.accessory == Accessory.JEWELS }
                                            }
                                        }
                                    }
                                }
                                "FACIAL_HAIR" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TraitOptionCard("NONE", icon = "🚫", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have no facial hair?") { it.facialHair == FacialHair.NONE }
                                        }
                                        TraitOptionCard("MUSTACHE", icon = "👨‍🦰", modifier = Modifier.weight(1.1f)) {
                                            applyQuestionFilter("Does the person have a mustache?") { it.facialHair == FacialHair.MUSTACHE }
                                        }
                                        TraitOptionCard("BEARD", icon = "🧔", modifier = Modifier.weight(1f)) {
                                            applyQuestionFilter("Does the person have a beard?") { it.facialHair == FacialHair.BEARD }
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

            // Bottom White Card with Green OK Button (Matching screenshots 1 & 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .shadow(12.dp, RoundedCornerShape(28.dp))
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onOkClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Text("OK", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

// ── SINGLE BIG 3D CHARACTER CARD FOR PHASE 1 SELECTION ───────────────────
@Composable
fun SingleBig3DCharacterCard(character: GuessWhoCharacter) {
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
                3.5.dp,
                Color.White,
                RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Big Character Name Ribbon AT THE TOP (Above Image)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Main Big Avatar Image Container BELOW Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
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
        }
    }
}

// ── CHARACTER CARD COMPONENT (SQUARE ASPECT RATIO 1:1 FOR PHASE 2) ───────
@Composable
fun GuessWhoCharacterCardItem(
    character: GuessWhoCharacter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val flipRotation by animateFloatAsState(
        targetValue = if (character.isEliminated) 180f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "CardFlipRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = flipRotation
                cameraDistance = 12f * density
            }
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(
                if (isSelected) 3.5.dp else 0.dp,
                if (isSelected) Color(0xFF10B981) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (flipRotation > 90f) {
            // Flipped Card Back Side (Clean Flat Minimal Design)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(
                        Color(0xFF334155),
                        RoundedCornerShape(12.dp)
                    )
            )
        } else {
            // Front Face-Up Side
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
                                    .size(30.dp)
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
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Name Ribbon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = character.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ── TRAIT CATEGORY BUTTON COMPONENT ───────────────────────────────────────
@Composable
fun TraitCategoryButton(
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
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
            Text(text = icon, fontSize = 13.sp)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
        }
    }
}

// ── TRAIT OPTION CARD COMPONENT (EQUAL WEIGHT & FLEXIBLE ICON/SWATCH) ─────
@Composable
fun TraitOptionCard(
    label: String,
    icon: String? = null,
    colorSwatch: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
            .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            if (colorSwatch != null) {
                // 3D Color Swatch Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(colorSwatch, RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                )
            } else if (icon != null) {
                Text(text = icon, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
        }
    }
}
