package com.ilygames.quizapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ilygames.quizapp.data.api.ApiClient
import com.ilygames.quizapp.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*

sealed class QuizState {
    object Idle : QuizState()
    object Loading : QuizState()
    data class Active(
        val questions: List<Question>,
        val currentQuestionIndex: Int,
        val answersSubmitted: Int,
        val score: Int,
        val startTime: Long
    ) : QuizState()
    data class Complete(val score: Int, val timeTaken: Int, val coinsEarned: Int) : QuizState()
    data class Error(val message: String) : QuizState()
}

class QuizViewModel : ViewModel() {

    private val gson = Gson()
    private var currentToken: String = ""

    // Regular Quiz State
    private val _quizState = MutableStateFlow<QuizState>(QuizState.Idle)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    // Flag tracked when user clicks Home button from completed quiz screen
    private var _justCompletedQuiz = false
    fun markQuizJustCompleted() {
        _justCompletedQuiz = true
    }
    fun consumeQuizJustCompleted(): Boolean {
        val result = _justCompletedQuiz
        _justCompletedQuiz = false
        return result
    }

    // Timer State (counts down configured seconds for each question)
    private val _timerState = MutableStateFlow(com.ilygames.quizapp.ui.screens.globalQuizTimerSeconds.value)
    val timerState: StateFlow<Int> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    // Daily/Weekly Leaderboards
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    // Daily Winner info
    private val _dailyWinner = MutableStateFlow<DailyWinnerResponse?>(null)
    val dailyWinner: StateFlow<DailyWinnerResponse?> = _dailyWinner.asStateFlow()

    // ==========================================
    // LIVE QUIZ (REAL-TIME WEBSOCKETS) STATE
    // ==========================================
    private var webSocket: WebSocket? = null
    private var realtimeSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient()

    // Callback invoked when any user's score is pushed by the server
    var onScoreUpdated: (() -> Unit)? = null

    private val _liveQuizStatus = MutableStateFlow("idle") // idle, waiting, playing, ended
    val liveQuizStatus: StateFlow<String> = _liveQuizStatus.asStateFlow()

    private val _liveQuizTimer = MutableStateFlow(0)
    val liveQuizTimer: StateFlow<Int> = _liveQuizTimer.asStateFlow()

    private val _liveQuizParticipantCount = MutableStateFlow(0)
    val liveQuizParticipantCount: StateFlow<Int> = _liveQuizParticipantCount.asStateFlow()

    private val _liveQuizQuestion = MutableStateFlow<Question?>(null)
    val liveQuizQuestion: StateFlow<Question?> = _liveQuizQuestion.asStateFlow()

    private val _liveQuizQuestionIndex = MutableStateFlow(0)
    val liveQuizQuestionIndex: StateFlow<Int> = _liveQuizQuestionIndex.asStateFlow()

    private val _liveQuizTotalQuestions = MutableStateFlow(0)
    val liveQuizTotalQuestions: StateFlow<Int> = _liveQuizTotalQuestions.asStateFlow()

    private val _liveQuizCorrectAnswer = MutableStateFlow<String?>(null) // "A", "B", "C", "D"
    val liveQuizCorrectAnswer: StateFlow<String?> = _liveQuizCorrectAnswer.asStateFlow()

    private val _liveQuizSelectedAnswer = MutableStateFlow<String?>(null)
    val liveQuizSelectedAnswer: StateFlow<String?> = _liveQuizSelectedAnswer.asStateFlow()

    private val _liveQuizScore = MutableStateFlow(0)
    val liveQuizScore: StateFlow<Int> = _liveQuizScore.asStateFlow()

    private val _liveQuizStandings = MutableStateFlow<List<String>>(emptyList())
    val liveQuizStandings: StateFlow<List<String>> = _liveQuizStandings.asStateFlow()

    private val _liveQuizLogs = MutableStateFlow<List<String>>(listOf("System Initialized."))
    val liveQuizLogs: StateFlow<List<String>> = _liveQuizLogs.asStateFlow()

    // ==========================================
    // REGULAR QUIZ LOGIC
    // ==========================================

    fun startQuiz(token: String) {
        if (token.isNotBlank()) currentToken = token
        _quizState.value = QuizState.Loading
        viewModelScope.launch {
            try {
                val limit = com.ilygames.quizapp.ui.screens.globalQuizQuestionLimit.value
                val response = ApiClient.apiService.getQuestions(currentToken, limit = limit)
                if (response.isSuccessful && response.body() != null) {
                    val allQuestions = response.body()!!
                    val uniqueQuestions = allQuestions.distinctBy { it.id ?: it.question }
                    val randomizedQuestions = uniqueQuestions.shuffled().take(limit)

                    _quizState.value = QuizState.Active(
                        questions = randomizedQuestions,
                        currentQuestionIndex = 0,
                        answersSubmitted = 0,
                        score = 0,
                        startTime = System.currentTimeMillis()
                    )
                    startTimer()
                } else {
                    _quizState.value = QuizState.Error("Failed to load questions.")
                }
            } catch (e: Exception) {
                _quizState.value = QuizState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun startGkPractice(token: String) {
        if (token.isNotBlank()) currentToken = token
        _quizState.value = QuizState.Loading
        viewModelScope.launch {
            try {
                val limit = com.ilygames.quizapp.ui.screens.globalQuizQuestionLimit.value
                val response = ApiClient.apiService.getQuestions(currentToken, limit = limit)
                if (response.isSuccessful && response.body() != null) {
                    val allQuestions = response.body()!!
                    val uniqueQuestions = allQuestions.distinctBy { it.id ?: it.question }
                    val randomizedQuestions = uniqueQuestions.shuffled().take(limit)
                    _quizState.value = QuizState.Active(
                        questions = randomizedQuestions,
                        currentQuestionIndex = 0,
                        answersSubmitted = 0,
                        score = 0,
                        startTime = System.currentTimeMillis()
                    )
                    startTimer()
                } else {
                    _quizState.value = QuizState.Error("Failed to load GK questions.")
                }
            } catch (e: Exception) {
                _quizState.value = QuizState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timerState.value = com.ilygames.quizapp.ui.screens.globalQuizTimerSeconds.value
        timerJob = viewModelScope.launch {
            while (_timerState.value > 0) {
                delay(1000)
                _timerState.value -= 1
            }
            // Timer expired, auto submit empty answer using stored currentToken
            submitAnswer(token = currentToken, option = "", isTimeout = true)
        }
    }

    fun submitAnswer(token: String, option: String, isTimeout: Boolean = false) {
        val currentState = _quizState.value
        if (currentState !is QuizState.Active) return

        timerJob?.cancel()
        if (token.isNotBlank()) currentToken = token
        
        val question = currentState.questions[currentState.currentQuestionIndex]
        val isCorrect = !isTimeout && option == question.correctAnswer
        
        val points = if (isCorrect) {
            // Speed bonus: up to 10 extra points based on time remaining
            10 + (_timerState.value / 2)
        } else {
            0
        }

        val newScore = currentState.score + points
        val nextIndex = currentState.currentQuestionIndex + 1

        if (nextIndex >= currentState.questions.size) {
            // Quiz Complete
            val timeTaken = ((System.currentTimeMillis() - currentState.startTime) / 1000).toInt()
            submitResultsToBackend(currentToken, newScore, timeTaken)
        } else {
            // Load next question
            _quizState.value = currentState.copy(
                currentQuestionIndex = nextIndex,
                score = newScore
            )
            startTimer()
        }
    }

    var isPracticeMode = false

    fun startCustomAiQuiz(questions: List<Question>) {
        isPracticeMode = true
        _quizState.value = QuizState.Active(
            questions = questions,
            currentQuestionIndex = 0,
            answersSubmitted = 0,
            score = 0,
            startTime = System.currentTimeMillis()
        )
        startTimer()
    }

    private fun submitResultsToBackend(token: String, score: Int, timeTaken: Int) {
        // Instant transition to Complete screen without loading screen
        _quizState.value = QuizState.Complete(
            score = score,
            timeTaken = timeTaken,
            coinsEarned = if (isPracticeMode) 0 else score
        )
        if (isPracticeMode) {
            // Casual / Practice mode: No score submission to backend or leaderboard!
            return
        }
        viewModelScope.launch {
            val activeToken = if (token.isNotBlank()) token else currentToken
            try {
                if (activeToken.isNotBlank()) {
                    val response = ApiClient.apiService.submitQuiz(activeToken, QuizSubmissionRequest(score, timeTaken))
                    if (response.isSuccessful && response.body() != null) {
                        val coins = response.body()!!.coinsEarned
                        _quizState.value = QuizState.Complete(
                            score = score,
                            timeTaken = timeTaken,
                            coinsEarned = coins
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun resetQuiz() {
        isPracticeMode = false
        _quizState.value = QuizState.Idle
    }

    // ==========================================
    // REAL-TIME PUSH SOCKET (always-on)
    // ==========================================

    fun connectRealtime(token: String, isDaily: Boolean = true) {
        if (realtimeSocket != null) return
        val request = Request.Builder().url(com.ilygames.quizapp.data.api.ApiClient.WS_URL).build()
        realtimeSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(gson.toJson(com.google.gson.JsonObject().apply {
                    addProperty("type", "auth")
                    addProperty("token", token)
                }))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                viewModelScope.launch {
                    try {
                        val json = gson.fromJson(text, com.google.gson.JsonObject::class.java)
                        if (json.get("type")?.asString == "score-updated") {
                            loadLeaderboard(token, isDaily)
                            onScoreUpdated?.invoke()
                        }
                    } catch (_: Exception) {}
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                realtimeSocket = null
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000)
                    connectRealtime(token, isDaily)
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                realtimeSocket = null
            }
        })
    }

    fun disconnectRealtime() {
        realtimeSocket?.close(1000, "Disconnect")
        realtimeSocket = null
    }

    // ==========================================
    // LEADERBOARD & WINNERS
    // ==========================================

    fun loadLeaderboard(token: String, isDaily: Boolean) {
        viewModelScope.launch {
            try {
                val response = if (isDaily) {
                    ApiClient.apiService.getDailyLeaderboard(token)
                } else {
                    ApiClient.apiService.getWeeklyLeaderboard(token)
                }
                if (response.isSuccessful && response.body() != null) {
                    _leaderboard.value = response.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadDailyWinner(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getDailyWinner(token)
                if (response.isSuccessful && response.body() != null) {
                    _dailyWinner.value = response.body()!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // REAL-TIME LIVE QUIZ WEBSOCKET LOGIC
    // ==========================================

    fun joinLiveQuiz(token: String) {
        val request = Request.Builder()
            .url("ws://127.0.0.1:3000")
            .build()

        _liveQuizLogs.value = listOf("Connecting to Live Server...")
        _liveQuizScore.value = 0
        _liveQuizSelectedAnswer.value = null
        _liveQuizCorrectAnswer.value = null

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                addLog("Connected to WebSocket Server.")
                val authMsg = JsonObject().apply {
                    addProperty("type", "auth")
                    addProperty("token", token)
                }
                webSocket.send(gson.toJson(authMsg))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                viewModelScope.launch {
                    try {
                        val json = gson.fromJson(text, JsonObject::class.java)
                        val type = json.get("type").asString

                        when (type) {
                            "auth-success" -> {
                                addLog("Authenticated as ${json.get("name").asString}.")
                                val joinMsg = JsonObject().apply {
                                    addProperty("type", "join-live-quiz")
                                }
                                webSocket.send(gson.toJson(joinMsg))
                            }
                            "state-sync" -> {
                                _liveQuizStatus.value = json.get("status").asString
                                _liveQuizTimer.value = json.get("countdown").asInt
                                addLog("Synced state: ${_liveQuizStatus.value}")
                            }
                            "joined-quiz" -> {
                                addLog("Successfully joined the live quiz queue!")
                            }
                            "participant-count" -> {
                                _liveQuizParticipantCount.value = json.get("count").asInt
                            }
                            "live-quiz-waiting" -> {
                                _liveQuizStatus.value = "waiting"
                                _liveQuizTimer.value = json.get("countdown").asInt
                                addLog("Live quiz waiting room active. Join now!")
                            }
                            "waiting-countdown" -> {
                                _liveQuizTimer.value = json.get("countdown").asInt
                            }
                            "next-question" -> {
                                _liveQuizStatus.value = "playing"
                                _liveQuizSelectedAnswer.value = null
                                _liveQuizCorrectAnswer.value = null
                                
                                val questionIndex = json.get("questionIndex").asInt
                                val total = json.get("totalQuestions").asInt
                                _liveQuizQuestionIndex.value = questionIndex
                                _liveQuizTotalQuestions.value = total
                                _liveQuizTimer.value = json.get("duration").asInt

                                val qJson = json.getAsJsonObject("question")
                                val optionsList = mutableListOf<String>()
                                if (qJson.has("options") && qJson.get("options").isJsonArray) {
                                    qJson.getAsJsonArray("options").forEach { optionsList.add(it.asString) }
                                }
                                val optA = if (qJson.has("optionA")) qJson.get("optionA").asString else ""
                                val optB = if (qJson.has("optionB")) qJson.get("optionB").asString else ""
                                val optC = if (qJson.has("optionC")) qJson.get("optionC").asString else ""
                                val optD = if (qJson.has("optionD")) qJson.get("optionD").asString else ""

                                val q = Question(
                                    id = if (qJson.has("id")) qJson.get("id").asString else null,
                                    question = qJson.get("question").asString,
                                    optionA = optA,
                                    optionB = optB,
                                    optionC = optC,
                                    optionD = optD,
                                    options = if (optionsList.isNotEmpty()) optionsList else null,
                                    correctAnswer = "",
                                    category = if (qJson.has("category")) qJson.get("category").asString else "General",
                                    difficulty = if (qJson.has("difficulty")) qJson.get("difficulty").asString else "easy"
                                )
                                _liveQuizQuestion.value = q
                                addLog("Question ${questionIndex + 1}: ${q.question}")
                            }
                            "question-countdown" -> {
                                _liveQuizTimer.value = json.get("countdown").asInt
                            }
                            "answer-ack" -> {
                                val isCorrect = json.get("isCorrect").asBoolean
                                val correct = json.get("correctAnswer").asString
                                _liveQuizCorrectAnswer.value = correct
                                _liveQuizScore.value = json.get("currentScore").asInt
                                addLog(if (isCorrect) "Correct! +points" else "Incorrect! Correct option was $correct")
                            }
                            "question-finished" -> {
                                val correct = json.get("correctAnswer").asString
                                _liveQuizCorrectAnswer.value = correct
                                addLog("Time's up! The correct answer was: $correct")
                            }
                            "live-quiz-ended" -> {
                                _liveQuizStatus.value = "ended"
                                _liveQuizQuestion.value = null
                                
                                val standingsArr = json.getAsJsonArray("standings")
                                val list = mutableListOf<String>()
                                standingsArr.forEachIndexed { index, element ->
                                    val obj = element.asJsonObject
                                    list.add("${index + 1}. ${obj.get("name").asString} - ${obj.get("score").asInt} pts")
                                }
                                _liveQuizStandings.value = list
                                addLog("Live quiz finished! Review standings.")
                            }
                            "live-quiz-reset" -> {
                                _liveQuizStatus.value = "idle"
                                _liveQuizQuestion.value = null
                                _liveQuizStandings.value = emptyList()
                                addLog("Lobby reset. Ready for the next event.")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                addLog("Error: ${t.localizedMessage ?: "Connection failure"}")
            }
        })
    }

    fun submitLiveAnswer(answer: String) {
        val ws = webSocket ?: return
        if (_liveQuizSelectedAnswer.value != null) return
        
        _liveQuizSelectedAnswer.value = answer

        val answerMsg = JsonObject().apply {
            addProperty("type", "submit-answer")
            addProperty("answer", answer)
            addProperty("timeRemaining", _liveQuizTimer.value)
        }
        ws.send(gson.toJson(answerMsg))
    }

    fun leaveLiveQuiz() {
        webSocket?.close(1000, "Left lobby")
        webSocket = null
        _liveQuizStatus.value = "idle"
        _liveQuizQuestion.value = null
    }

    private fun addLog(message: String) {
        val current = _liveQuizLogs.value.toMutableList()
        current.add("[${System.currentTimeMillis()}] $message")
        if (current.size > 20) {
            current.removeAt(0)
        }
        _liveQuizLogs.value = current
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        webSocket?.close(1000, "ViewModel cleared")
        realtimeSocket?.close(1000, "ViewModel cleared")
    }
}
