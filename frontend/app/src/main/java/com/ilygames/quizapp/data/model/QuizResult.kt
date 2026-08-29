package com.ilygames.quizapp.data.model

import com.google.gson.annotations.SerializedName

data class QuizSubmissionRequest(
    val score: Int,
    val timeTaken: Int
)

data class QuizSubmissionResponse(
    val msg: String,
    val coinsEarned: Int,
    val newCoinsBalance: Int,
    val totalScore: Int,
    val todayScore: Int
)

data class UserSummary(
    val id: String?,
    val name: String,
    val email: String
)

data class DailyWinnerResponse(
    val winner: UserSummary?,
    val score: Int,
    val timeTaken: Int,
    val date: String,
    val status: String?,
    val msg: String?
)

data class ImageUploadResponse(
    val imageUrl: String?,
    val url: String?,
    val filename: String?
)
