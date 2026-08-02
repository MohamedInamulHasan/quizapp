package com.ilygames.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.AuthState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    AuthScreen(
        authViewModel = authViewModel,
        startOnRegister = false,
        onAuthSuccess = onLoginSuccess
    )
}

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    AuthScreen(
        authViewModel = authViewModel,
        startOnRegister = true,
        onAuthSuccess = onRegisterSuccess
    )
}

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    startOnRegister: Boolean = false,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    var isSignUp by remember { mutableStateOf(startOnRegister) }
    var name by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }
    var mobileError by remember { mutableStateOf("") }

    LaunchedEffect(isSignUp) {
        name = ""
        mobileNumber = ""
        usernameError = ""
        mobileError = ""
        authViewModel.resetAuthState()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onAuthSuccess()
    }

    val inputTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )

    // Outer full screen Box — centers the Card vertically and horizontally
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(PrimaryGreen.copy(alpha = 0.18f), ElectricMint.copy(alpha = 0.1f))
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(1.5.dp, PrimaryGreen.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "QuizApp",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isSignUp) "Create an Account" else "Sign In to Play",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                // Username field (max 12 chars)
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(12)
                        name = filtered
                        usernameError = if (filtered.isNotBlank() && filtered.length < 3)
                            "Invalid username" else ""
                    },
                    label = { Text("Username", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryGreen) },
                    isError = usernameError.isNotBlank(),
                    supportingText = if (usernameError.isNotBlank()) {
                        { Text(usernameError, color = IncorrectRed) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = inputTextStyle,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorBorderColor = IncorrectRed,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = PrimaryGreen,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mobile field (max 10 digits)
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(10)
                        mobileNumber = filtered
                        mobileError = if (filtered.isNotBlank() && filtered.length < 10)
                            "Must be 10 digits" else ""
                    },
                    label = { Text("Mobile Number", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = PrimaryGreen) },
                    isError = mobileError.isNotBlank(),
                    supportingText = if (mobileError.isNotBlank()) {
                        { Text(mobileError, color = IncorrectRed) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = inputTextStyle,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorBorderColor = IncorrectRed,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = PrimaryGreen,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Server error
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = IncorrectRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Primary Button (Sign In or Sign Up)
                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        if (isSignUp) {
                            authViewModel.register(name, mobileNumber, context)
                        } else {
                            authViewModel.login(name, mobileNumber, context)
                        }
                    },
                    enabled = name.length >= 3 && mobileNumber.length == 10
                            && usernameError.isBlank() && mobileError.isBlank()
                            && authState !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(50.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isSignUp) "Sign Up" else "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                // Text link to switch between Sign In and Sign Up
                TextButton(
                    onClick = {
                        SoundManager.playClickSound()
                        isSignUp = !isSignUp
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
