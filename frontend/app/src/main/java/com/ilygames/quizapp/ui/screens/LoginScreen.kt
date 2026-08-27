package com.ilygames.quizapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    LaunchedEffect(isSignUp) {
        usernameInput = ""
        emailInput = ""
        passwordInput = ""
        authViewModel.resetAuthState()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onAuthSuccess()
    }

    val inputTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Official Quizzy Green Squircle Q Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = PrimaryGreen.copy(alpha = 0.35f))
                        .background(PrimaryGreen, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Q",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Quizzy",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isSignUp) "Create Your Account" else "Sign In to Play",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(2.dp))

                // Field 1: Username / Email
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text(if (isSignUp) "Username" else "Username or Email", color = TextMuted) },
                    leadingIcon = { Icon(if (isSignUp) Icons.Default.Person else Icons.Default.Person, null, tint = PrimaryGreen) },
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

                // Field 2: Email (ONLY in Sign Up mode)
                if (isSignUp) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                }

                // Field 3: Password (in both Sign In & Sign Up modes)
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryGreen) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                Spacer(Modifier.height(2.dp))

                // Primary Button (Sign In or Sign Up)
                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        if (isSignUp) {
                            authViewModel.register(usernameInput, passwordInput, context)
                        } else {
                            authViewModel.login(usernameInput, passwordInput, context)
                        }
                    },
                    enabled = usernameInput.isNotBlank() && passwordInput.isNotBlank()
                            && (!isSignUp || emailInput.isNotBlank())
                            && authState !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(50.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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
