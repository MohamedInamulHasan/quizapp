package com.ilygames.quizapp.data.model

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("_id") val id: String? = null,
    val question: String,
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val options: List<String>? = null,
    val correctAnswer: String, // "A", "B", "C", "D", "E", "F"
    val category: String = "General",
    val difficulty: String = "easy",
    val imageUrl: String? = null
) {
    fun getOptionsList(): List<String> {
        if (!options.isNullOrEmpty()) {
            return options.filter { it.isNotBlank() }
        }
        return listOf(optionA, optionB, optionC, optionD).filter { it.isNotBlank() }
    }
}
