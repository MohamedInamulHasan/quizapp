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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
        onAuthSuccess = onLoginSuccess,
        onNavigateToOther = onNavigateToRegister
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
        onAuthSuccess = onRegisterSuccess,
        onNavigateToOther = onNavigateToLogin
    )
}

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    startOnRegister: Boolean = false,
    onAuthSuccess: () -> Unit,
    onNavigateToOther: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    var isSignUp by remember { mutableStateOf(startOnRegister) }
    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isSignUp) {
        usernameInput = ""
        emailInput = ""
        passwordInput = ""
        isPasswordVisible = false
        authViewModel.resetAuthState()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess()
        }
    }

    val inputTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    )

    // Form Field Validations
    val isGmailValid = emailInput.trim().endsWith("@gmail.com", ignoreCase = true) &&
            emailInput.trim().length >= 11 &&
            emailInput.trim().substringBefore("@gmail.com", "").isNotBlank()

    val isPasswordValid = passwordInput.length >= 6
    val isUsernameValid = usernameInput.trim().length >= 3

    // Master Submit Button Enabled Rule
    val isSubmitEnabled = if (isSignUp) {
        isUsernameValid && isGmailValid && isPasswordValid && authState !is AuthState.Loading
    } else {
        usernameInput.trim().isNotBlank() && passwordInput.isNotBlank() && authState !is AuthState.Loading
    }

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Brand Logo
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

                // Sign In / Sign Up Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (!isSignUp) PrimaryGreen else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isSignUp) {
                                    SoundManager.playClickSound()
                                    isSignUp = false
                                    onNavigateToOther?.invoke()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (!isSignUp) Color.White else TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isSignUp) PrimaryGreen else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSignUp) {
                                    SoundManager.playClickSound()
                                    isSignUp = true
                                    onNavigateToOther?.invoke()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign Up",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSignUp) Color.White else TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Field 1: Username (or Username/Email in Sign In)
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text(if (isSignUp) "Username" else "Username or Email", color = TextMuted) },
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

                // Field 2: Email (Sign Up only)
                if (isSignUp) {
                    val showEmailError = emailInput.isNotBlank() && !isGmailValid
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryGreen) },
                        isError = showEmailError,
                        supportingText = if (showEmailError) {
                            { Text("Must be a valid @gmail.com email address", color = IncorrectRed, fontSize = 12.sp) }
                        } else null,
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

                // Field 3: Password (with Eye Toggle Icon)
                val showPasswordError = isSignUp && passwordInput.isNotBlank() && !isPasswordValid
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", color = TextMuted) },
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
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = showPasswordError,
                    supportingText = if (showPasswordError) {
                        { Text("Password must be at least 6 characters", color = IncorrectRed, fontSize = 12.sp) }
                    } else null,
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

                // Server / Backend Error Display
                AnimatedVisibility(
                    visible = authState is AuthState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = IncorrectRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        if (isSignUp) {
                            authViewModel.register(usernameInput.trim(), emailInput.trim(), passwordInput, context)
                        } else {
                            authViewModel.login(usernameInput.trim(), passwordInput, context)
                        }
                    },
                    enabled = isSubmitEnabled,
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

                // Bottom Switch Toggle Link
                TextButton(
                    onClick = {
                        SoundManager.playClickSound()
                        isSignUp = !isSignUp
                        onNavigateToOther?.invoke()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
