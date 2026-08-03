package com.ilygames.quizapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.model.QuizCategory
import com.ilygames.quizapp.data.model.TriviaQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InfiniteQuizViewModel : ViewModel() {

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            QuizCategory(9,  "General Knowledge", "🌍", "#6C63FF"),
            QuizCategory(23, "History",           "🏛️", "#FF6B6B"),
            QuizCategory(21, "Sports",            "⚽", "#4ECDC4"),
            QuizCategory(11, "Movies",            "🎬", "#FF8E53"),
            QuizCategory(32, "Cartoons",          "🎨", "#A8E6CF"),
            QuizCategory(12, "Music",             "🎵", "#FF6B9D"),
            QuizCategory(17, "Science",           "🔬", "#45B7D1"),
            QuizCategory(22, "Geography",         "🗺️", "#96CEB4"),
            QuizCategory(15, "Video Games",       "🎮", "#FFEAA7"),
            QuizCategory(18, "Technology",        "💻", "#DDA0DD"),
            QuizCategory(14, "Television",        "📺", "#98FB98"),
            QuizCategory(26, "Celebrities",       "⭐", "#FFD700"),
            QuizCategory(0,  "AI Custom Topic",   "🤖", "#FF4757")
        )
    }

    private val _categories = MutableStateFlow<List<QuizCategory>>(DEFAULT_CATEGORIES)
    val categories: StateFlow<List<QuizCategory>> = _categories

    private val _questions = MutableStateFlow<List<TriviaQuestion>>(emptyList())
    val questions: StateFlow<List<TriviaQuestion>> = _questions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadCategories(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getCategories(token)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    _categories.value = response.body()!!
                }
            } catch (e: Exception) {
                // Keep default categories if network fails
            }
        }
    }

    fun loadTriviaQuestions(token: String, categoryId: Int, difficulty: String = "medium") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _questions.value = emptyList()
            try {
                val response = ApiClient.apiService.getTriviaQuestions(
                    token = token,
                    category = categoryId,
                    amount = 10,
                    difficulty = difficulty
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (!body.isNullOrEmpty()) {
                        _questions.value = body
                    } else {
                        _error.value = "No questions found for this category. Try a different difficulty."
                    }
                } else {
                    _error.value = "Failed to load questions. Please try again."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateAIQuestions(token: String, topic: String, difficulty: String = "medium") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _questions.value = emptyList()
            try {
                val response = ApiClient.apiService.generateAIQuestions(
                    token = token,
                    topic = topic,
                    amount = 10,
                    difficulty = difficulty
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (!body.isNullOrEmpty()) {
                        _questions.value = body
                    } else {
                        _error.value = "AI could not generate questions. Try a different topic."
                    }
                } else {
                    val errMsg = response.errorBody()?.string() ?: "Unknown error"
                    _error.value = when {
                        errMsg.contains("not configured") -> "AI quiz needs Gemini API key setup."
                        else -> "AI generation failed. Try again."
                    }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
