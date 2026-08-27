package com.ilygames.quizapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ilygames.quizapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _user = MutableStateFlow<User>(defaultAdminUser)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Success(defaultAdminUser))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _token = MutableStateFlow<String?>("bypass_auth_token_123")
    val token: StateFlow<String?> = _token.asStateFlow()

    fun resetAuthState() {
        _authState.value = AuthState.Success(_user.value)
    }

    fun tryAutoLogin(context: Context? = null) {
        _authState.value = AuthState.Success(_user.value)
    }

    fun refreshProfile(context: Context? = null) {
        _authState.value = AuthState.Success(_user.value)
    }

    fun updateProfileState(name: String? = null, profileImageUrl: String? = null) {
        val currentUser = _user.value
        val updatedUser = currentUser.copy(
            name = name ?: currentUser.name,
            profileImageUrl = profileImageUrl ?: currentUser.profileImageUrl
        )
        _user.value = updatedUser
        _authState.value = AuthState.Success(updatedUser)
    }

    fun addAdReward(context: Context? = null, coinsToAdd: Int = 10) {
        val currentUser = _user.value
        val updatedUser = currentUser.copy(coins = currentUser.coins + coinsToAdd)
        _user.value = updatedUser
        _authState.value = AuthState.Success(updatedUser)
    }

    fun logout(context: Context? = null) {
        _user.value = defaultAdminUser
        _authState.value = AuthState.Success(defaultAdminUser)
    }
}
