package com.ilygames.quizapp.data.model

data class User(
    val id: String?,
    val name: String?,
    val email: String? = null,
    val coins: Int? = 0,
    val totalScore: Int? = 0,
    val todayScore: Int? = 0,
    val highScore: Int? = 0,
    val isAdmin: Boolean? = false,
    val profileImageUrl: String? = null
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val mobileNumber: String = password
)

data class LoginRequest(
    val credential: String? = null,
    val name: String? = null,
    val email: String? = null,
    val password: String,
    val mobileNumber: String = password
)

data class CoinsRewardRequest(
    val coinsToAdd: Int
)

data class CoinsRewardResponse(
    val coins: Int,
    val msg: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val profileImageUrl: String? = null
)
