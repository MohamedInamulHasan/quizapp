package com.ilygames.quizapp.data.model

data class LeaderboardEntry(
    val rank: Int,
    val id: String,
    val name: String,
    val score: Int,
    val coins: Int,
    val profileImageUrl: String? = null
)
