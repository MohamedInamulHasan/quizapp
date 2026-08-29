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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var usernameOrEmailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

    val inputTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )

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
                    text = "Welcome Back",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Sign in to continue to Quizzy",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )

                Spacer(Modifier.height(4.dp))

                // Field 1: Username or Email
                OutlinedTextField(
                    value = usernameOrEmailInput,
                    onValueChange = {
                        usernameOrEmailInput = it
                        validationError = null
                        if (authState is AuthState.Error) authViewModel.resetAuthState()
                    },
                    label = { Text("Username or Email", color = labelColor, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryGreen) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = inputTextStyle,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorBorderColor = IncorrectRed,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = labelColor,
                        cursorColor = PrimaryGreen,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Field 2: Password
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        validationError = null
                        if (authState is AuthState.Error) authViewModel.resetAuthState()
                    },
                    label = { Text("Password", color = labelColor, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryGreen) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = labelColor
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = inputTextStyle,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorBorderColor = IncorrectRed,
                        focusedLabelColor = PrimaryGreen,
                        unfocusedLabelColor = labelColor,
                        cursorColor = PrimaryGreen,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Error Message Display (below form fields)
                val activeErrorMsg = validationError ?: (authState as? AuthState.Error)?.message
                AnimatedVisibility(
                    visible = activeErrorMsg != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (activeErrorMsg != null) {
                        Text(
                            text = activeErrorMsg,
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

                // Master Sign In Button
                Button(
                    onClick = {
                        SoundManager.playClickSound()
                        val uInput = usernameOrEmailInput.trim()
                        val pInput = passwordInput.trim()

                        if (uInput.isBlank() || pInput.isBlank()) {
                            validationError = "Please fill in all fields"
                            return@Button
                        }

                        validationError = null
                        authViewModel.login(uInput, pInput, context)
                    },
                    enabled = authState !is AuthState.Loading,
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
                            text = "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Footer Switch to Register
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = labelColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sign Up",
                        color = PrimaryGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playClickSound()
                            authViewModel.resetAuthState()
                            onNavigateToRegister()
                        }
                    )
                }
            }
        }
    }
}
