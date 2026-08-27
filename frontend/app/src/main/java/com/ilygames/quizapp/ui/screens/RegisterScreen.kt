package com.ilygames.quizapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilygames.quizapp.ui.theme.*
import com.ilygames.quizapp.ui.viewmodel.AuthState
import com.ilygames.quizapp.ui.viewmodel.AuthViewModel
import com.ilygames.quizapp.utils.SoundManager

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
        }
    }

    val inputTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    )

    val isGmailValid = emailInput.trim().endsWith("@gmail.com", ignoreCase = true) &&
            emailInput.trim().length >= 11

    val isPasswordValid = passwordInput.length >= 6
    val isUsernameValid = usernameInput.trim().length >= 3
    val isPasswordMatch = passwordInput == confirmPasswordInput

    val isSubmitEnabled = isUsernameValid &&
            isGmailValid &&
            isPasswordValid &&
            isPasswordMatch &&
            authState !is AuthState.Loading

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // App Brand Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = PrimaryGreen.copy(alpha = 0.35f))
                        .background(PrimaryGreen, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Q",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Join Quizzy to play & earn rewards",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextMuted
                )

                Spacer(Modifier.height(2.dp))

                // Field 1: Username
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username (min 3 chars)", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryGreen) },
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

                // Field 2: Gmail Address
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email (@gmail.com)", color = TextMuted) },
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

                // Field 3: Password
                val showPasswordError = passwordInput.isNotBlank() && !isPasswordValid
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password (min 6 chars)", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryGreen) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = TextMuted
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else SoftPasswordTransformation(),
                    isError = showPasswordError,
                    supportingText = if (showPasswordError) {
                        { Text("Password must be at least 6 characters", color = IncorrectRed, fontSize = 12.sp) }
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

                // Field 4: Confirm Password
                val showConfirmError = confirmPasswordInput.isNotBlank() && !isPasswordMatch
                OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = { confirmPasswordInput = it },
                    label = { Text("Confirm Password", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryGreen) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else SoftPasswordTransformation(),
                    isError = showConfirmError,
                    supportingText = if (showConfirmError) {
                        { Text("Passwords do not match", color = IncorrectRed, fontSize = 12.sp) }
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

                // Live Error Message Display
                AnimatedVisibility(
                    visible = authState is AuthState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = IncorrectRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                // Master Sign Up Button
                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        authViewModel.register(usernameInput.trim(), emailInput.trim(), passwordInput, context)
                    },
                    enabled = isSubmitEnabled,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        disabledContainerColor = PrimaryGreen.copy(alpha = 0.35f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Footer Switch to Sign In
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sign In",
                        color = PrimaryGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playClickSound()
                            authViewModel.resetAuthState()
                            onNavigateToLogin()
                        }
                    )
                }
            }
        }
    }
}
