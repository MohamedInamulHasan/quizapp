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
    fun tryAutoLogin(context: Context) {
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
                }
            } catch (e: Exception) {
                // Silent catch for background profile refresh
            }
        }
    }

    // Sign In method
    fun login(credential: String, passwordInput: String, context: Context) {
        _authState.value = AuthState.Loading
        val trimmed = credential.trim()
        val isEmailInput = trimmed.contains("@")

        val nameParam = if (!isEmailInput) trimmed else trimmed
        val emailParam = if (isEmailInput) trimmed else null

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.login(
                    LoginRequest(name = nameParam, email = emailParam, password = passwordInput)
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
                _authState.value = AuthState.Error("Network error. Please check connection.")
            }
        }
    }

    // Sign Up method
    fun register(username: String, email: String, passwordInput: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.register(
                    RegisterRequest(name = username.trim(), email = email.trim(), password = passwordInput)
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
                _authState.value = AuthState.Error("Network error. Please check connection.")
            }
        }
    }

    fun updateProfileState(name: String? = null, profileImageUrl: String? = null) {
        val currentUser = _user.value ?: return
        _user.value = currentUser.copy(
            name = name ?: currentUser.name,
            profileImageUrl = profileImageUrl ?: currentUser.profileImageUrl
        )
    }

    fun addAdReward(context: Context, coinsToAdd: Int = 10) {
        viewModelScope.launch {
            try {
                val tokenStr = _token.value ?: getToken(context) ?: return@launch
                val response = ApiClient.apiService.addRewards(
                    tokenStr,
                    com.ilygames.quizapp.data.model.CoinsRewardRequest(coinsToAdd)
                )
                if (response.isSuccessful) {
                    refreshProfile(context)
                }
            } catch (e: Exception) {
                // Ignore silent ad reward error
            }
        }
    }

    fun logout(context: Context) {
        clearToken(context)
        _token.value = null
        _user.value = null
        _authState.value = AuthState.Idle
    }

    // Parses {"msg":"..."} response body and applies client-side safety overrides
    private fun parseErrorMsg(raw: String?, fallback: String, isEmailInput: Boolean): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val msg = JSONObject(raw).optString("msg", fallback)
            if (isEmailInput && msg.equals("Invalid username", ignoreCase = true)) {
                "Invalid email"
            } else if (msg.contains("mobile", ignoreCase = true)) {
                "Invalid credentials"
            } else {
                msg
            }
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
