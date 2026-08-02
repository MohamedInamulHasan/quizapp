package com.ilygames.quizapp.data.model

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("_id") val id: String? = null,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val category: String,
    val difficulty: String,
    val imageUrl: String? = null
)
