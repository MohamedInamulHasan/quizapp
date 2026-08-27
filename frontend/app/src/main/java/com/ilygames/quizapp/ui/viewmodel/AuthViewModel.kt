package com.ilygames.quizapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.model.*
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

    fun authenticate(name: String, mobileNumber: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.authenticate(LoginRequest(name, mobileNumber))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _token.value = authResponse.token
                    _user.value = authResponse.user
                    saveToken(context, authResponse.token)
                    _authState.value = AuthState.Success(authResponse.user)
                } else {
                    _authState.value = AuthState.Error(parseErrorMsg(response.errorBody()?.string(), "Invalid username or mobile number"))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error. Please try again.")
            }
        }
    }

    fun login(name: String, email: String? = null, mobileNumber: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.login(LoginRequest(name, email, mobileNumber))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _token.value = authResponse.token
                    _user.value = authResponse.user
                    saveToken(context, authResponse.token)
                    _authState.value = AuthState.Success(authResponse.user)
                } else {
                    _authState.value = AuthState.Error(parseErrorMsg(response.errorBody()?.string(), "Invalid credentials"))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error. Please try again.")
            }
        }
    }

    fun register(name: String, email: String? = null, mobileNumber: String, context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.register(RegisterRequest(name, email, mobileNumber))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _token.value = authResponse.token
                    _user.value = authResponse.user
                    saveToken(context, authResponse.token)
                    _authState.value = AuthState.Success(authResponse.user)
                } else {
                    _authState.value = AuthState.Error(parseErrorMsg(response.errorBody()?.string(), "Registration failed"))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error. Please try again.")
            }
        }
    }

    fun tryAutoLogin(context: Context) {
        val savedToken = getToken(context)
        if (savedToken != null) {
            _token.value = savedToken
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                try {
                    val response = ApiClient.apiService.getProfile(savedToken)
                    if (response.isSuccessful && response.body() != null) {
                        val userProfile = response.body()!!
                        _user.value = userProfile
                        _authState.value = AuthState.Success(userProfile)
                    } else {
                        clearToken(context)
                        _authState.value = AuthState.Idle
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Error("Network error. Working offline if data cached.")
                }
            }
        }
    }

    fun updateProfileState(name: String? = null, profileImageUrl: String? = null) {
        _user.value = _user.value?.copy(
            name = name ?: _user.value?.name ?: "",
            profileImageUrl = profileImageUrl ?: _user.value?.profileImageUrl
        )
    }

    /** Silently re-fetches user profile from server and updates local state. */
    fun refreshProfile() {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getProfile(currentToken)
                if (response.isSuccessful && response.body() != null) {
                    _user.value = response.body()!!
                }
            } catch (_: Exception) {}
        }
    }

    fun addAdReward(context: Context) {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.addRewards(currentToken, CoinsRewardRequest(100))
                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = ApiClient.apiService.getProfile(currentToken)
                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        _user.value = profileResponse.body()!!
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(context: Context) {
        clearToken(context)
        _token.value = null
        _user.value = null
        _authState.value = AuthState.Idle
    }

    // Parses {"msg":"..."} response body and returns clean message string
    private fun parseErrorMsg(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val msg = JSONObject(raw).optString("msg", fallback)
            if (msg.equals("Invalid Credentials", ignoreCase = true)) {
                "Invalid username or mobile number"
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
