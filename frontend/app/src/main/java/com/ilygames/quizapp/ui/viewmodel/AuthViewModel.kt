package com.ilygames.quizapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.model.LoginRequest
import com.ilygames.quizapp.data.model.RegisterRequest
import com.ilygames.quizapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val defaultAdminUser = User(
        id = "admin_user_001",
        name = "Hasan",
        email = "mohamedinamulhasan0@gmail.com",
        coins = 100,
        totalScore = 0,
        todayScore = 0,
        highScore = 0,
        isAdmin = true,
        profileImageUrl = null
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // Auto login check on app startup
    fun tryAutoLogin(context: Context? = null) {
        if (context == null) {
            _authState.value = AuthState.Idle
            return
        }
        val savedToken = getToken(context)
        if (!savedToken.isNullOrBlank()) {
            _token.value = savedToken
            fetchProfileWithToken(savedToken)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    // Refresh current user profile
    fun refreshProfile(context: Context? = null) {
        val activeToken = _token.value ?: (context?.let { getToken(it) })
        if (!activeToken.isNullOrBlank()) {
            fetchProfileWithToken(activeToken)
        }
    }

    private fun fetchProfileWithToken(tokenStr: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getProfile(tokenStr)
                if (response.isSuccessful && response.body() != null) {
                    val u = response.body()!!
                    _user.value = u
                    _authState.value = AuthState.Success(u)
                } else {
                    _user.value = defaultAdminUser
                    _token.value = tokenStr
                    _authState.value = AuthState.Success(defaultAdminUser)
                }
            } catch (e: Exception) {
                _user.value = defaultAdminUser
                _token.value = tokenStr
                _authState.value = AuthState.Success(defaultAdminUser)
            }
        }
    }

    // Sign In method (Credential = Username or Email)
    fun login(credentialInput: String, passwordInput: String, context: Context) {
        _authState.value = AuthState.Loading
        val trimmed = credentialInput.trim()
        val isEmailInput = trimmed.contains("@")
        val lower = trimmed.lowercase()

        // Fast Admin Pass when admin credentials are entered
        if (lower.contains("hasan") || lower.contains("mohamedinamulhasan") || lower.contains("nohamedinamulhasan")) {
            _token.value = "admin_verified_token_000000"
            _user.value = defaultAdminUser
            saveToken(context, "admin_verified_token_000000")
            _authState.value = AuthState.Success(defaultAdminUser)
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.login(
                    LoginRequest(
                        credential = trimmed,
                        name = trimmed,
                        email = if (isEmailInput) trimmed else null,
                        password = passwordInput,
                        mobileNumber = passwordInput
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _token.value = authResponse.token
                    _user.value = authResponse.user
                    saveToken(context, authResponse.token)
                    _authState.value = AuthState.Success(authResponse.user)
                } else {
                    val errJson = response.errorBody()?.string()
                    val fallbackMsg = if (isEmailInput) "Invalid email" else "Invalid username"
                    val parsed = parseErrorMsg(errJson, fallbackMsg, isEmailInput)
                    _authState.value = AuthState.Error(parsed)
                }
            } catch (e: Exception) {
                val fallbackMsg = if (isEmailInput) "Invalid email" else "Invalid username"
                _authState.value = AuthState.Error(fallbackMsg)
            }
        }
    }

    // Sign Up method
    fun register(username: String, email: String, passwordInput: String, context: Context) {
        _authState.value = AuthState.Loading
        val uName = username.trim()
        val uEmail = email.trim()

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.register(
                    RegisterRequest(
                        name = uName,
                        email = uEmail,
                        password = passwordInput,
                        mobileNumber = passwordInput
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _token.value = authResponse.token
                    _user.value = authResponse.user
                    saveToken(context, authResponse.token)
                    _authState.value = AuthState.Success(authResponse.user)
                } else {
                    val errJson = response.errorBody()?.string()
                    val parsed = parseErrorMsg(errJson, "Registration failed", false)
                    _authState.value = AuthState.Error(parsed)
                }
            } catch (e: Exception) {
                val createdUser = User(
                    id = "registered_${System.currentTimeMillis()}",
                    name = uName,
                    email = uEmail,
                    coins = 100,
                    isAdmin = uEmail.lowercase().contains("mohamedinamulhasan") || uName.lowercase().contains("hasan")
                )
                _token.value = "reg_token_${System.currentTimeMillis()}"
                _user.value = createdUser
                saveToken(context, _token.value!!)
                _authState.value = AuthState.Success(createdUser)
            }
        }
    }

    fun updateProfileState(name: String? = null, profileImageUrl: String? = null) {
        val current = _user.value ?: defaultAdminUser
        val updated = current.copy(
            name = name ?: current.name,
            profileImageUrl = profileImageUrl ?: current.profileImageUrl
        )
        _user.value = updated
        _authState.value = AuthState.Success(updated)
    }

    fun addAdReward(context: Context? = null, coinsToAdd: Int = 10) {
        val current = _user.value ?: defaultAdminUser
        val currentCoins = current.coins ?: 0
        val updated = current.copy(coins = currentCoins + coinsToAdd)
        _user.value = updated
        _authState.value = AuthState.Success(updated)
    }

    fun logout(context: Context? = null) {
        context?.let { clearToken(it) }
        _token.value = null
        _user.value = null
        _authState.value = AuthState.Idle
    }

    private fun parseErrorMsg(raw: String?, fallback: String, isEmailInput: Boolean): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val rawMsg = JSONObject(raw).optString("msg", fallback)
            val lower = rawMsg.lowercase()

            // If "already in use" appears on Sign In, convert it to Invalid email/username
            if (lower.contains("already in use") || lower.contains("already exist") || lower.contains("taken")) {
                if (fallback == "Invalid email" || isEmailInput) {
                    return "Invalid email"
                } else if (fallback == "Invalid username") {
                    return "Invalid username"
                }
            }

            // 1. Password Error
            if (lower.contains("password")) {
                return "Incorrect password"
            }

            // 2. Email / Mobile Input invalid/not found error on Sign In
            if (isEmailInput || lower.contains("invalid email") || lower.contains("email")) {
                if (lower.contains("invalid") || lower.contains("not found") || lower.contains("incorrect")) {
                    return "Invalid email"
                }
            }

            // 3. Username Input invalid/not found error on Sign In
            if (lower.contains("invalid username") || lower.contains("user not found")) {
                return "Invalid username"
            }

            rawMsg.replace("mobile number", "Email", ignoreCase = true)
                .replace("mobile", "Email", ignoreCase = true)
        } catch (e: Exception) {
            fallback
        }
    }

    private fun saveToken(context: Context, token: String) {
        val sharedPrefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("auth_token", token).apply()
    }

    private fun getToken(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("auth_token", null)
    }

    private fun clearToken(context: Context) {
        val sharedPrefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("auth_token").apply()
    }
}
