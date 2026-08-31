package com.ilygames.quizapp.data.api

import com.ilygames.quizapp.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/authenticate")
    suspend fun authenticate(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/guest")
    suspend fun guestLogin(): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getProfile(
        @Header("x-auth-token") token: String
    ): Response<User>

    @POST("auth/rewards")
    suspend fun addRewards(
        @Header("x-auth-token") token: String,
        @Body request: CoinsRewardRequest
    ): Response<CoinsRewardResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("x-auth-token") token: String,
        @Body request: UpdateProfileRequest
    ): Response<User>


    @GET("quiz/questions")
    suspend fun getQuestions(
        @Header("x-auth-token") token: String,
        @Query("limit") limit: Int = 100
    ): Response<List<Question>>

    @GET("quiz/daily-challenge")
    suspend fun getDailyChallenge(
        @Header("x-auth-token") token: String
    ): Response<List<Question>>

    @POST("quiz/submit")
    suspend fun submitQuiz(
        @Header("x-auth-token") token: String,
        @Body request: QuizSubmissionRequest
    ): Response<QuizSubmissionResponse>

    @GET("quiz/winner")
    suspend fun getDailyWinner(
        @Header("x-auth-token") token: String
    ): Response<DailyWinnerResponse>

    @GET("leaderboard/daily")
    suspend fun getDailyLeaderboard(
        @Header("x-auth-token") token: String
    ): Response<List<LeaderboardEntry>>

    @GET("leaderboard/weekly")
    suspend fun getWeeklyLeaderboard(
        @Header("x-auth-token") token: String
    ): Response<List<LeaderboardEntry>>

    @POST("admin/questions")
    suspend fun createQuestion(
        @Header("x-auth-token") token: String,
        @Body question: Question
    ): Response<Question>

    // Returns ALL questions (not just random 20) — for admin panel
    @GET("admin/questions")
    suspend fun getAdminQuestions(
        @Header("x-auth-token") token: String
    ): Response<List<Question>>

    @POST("admin/questions/bulk")
    suspend fun bulkUploadQuestions(
        @Header("x-auth-token") token: String,
        @Body questions: List<Question>
    ): Response<List<Question>>

    @PUT("admin/questions/{id}")
    suspend fun updateQuestion(
        @Header("x-auth-token") token: String,
        @Path("id") id: String,
        @Body question: Question
    ): Response<Question>

    @DELETE("admin/questions/{id}")
    suspend fun deleteQuestion(
        @Header("x-auth-token") token: String,
        @Path("id") id: String
    ): Response<Unit>

    @DELETE("admin/questions/all/clear")
    suspend fun deleteAllQuestions(
        @Header("x-auth-token") token: String
    ): Response<Unit>

    @POST("admin/seed")
    suspend fun seedQuestions(
        @Header("x-auth-token") token: String
    ): Response<Unit>

    @GET("admin/users")
    suspend fun getAdminUsers(
        @Header("x-auth-token") token: String
    ): Response<List<User>>

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(
        @Header("x-auth-token") token: String,
        @Path("id") id: String
    ): Response<Unit>

    // Upload an image to the server → returns { imageUrl: "http://...", filename: "..." }
    @Multipart
    @POST("admin/upload-image")
    suspend fun uploadImage(
        @Header("x-auth-token") token: String,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>

    @DELETE("auth/profile-image")
    suspend fun deleteProfileImage(
        @Header("x-auth-token") token: String
    ): Response<User>

    @POST("admin/reward")
    suspend fun publishReward(
        @Header("x-auth-token") token: String,
        @Body reward: RewardSyncRequest
    ): Response<Unit>

    @GET("admin/reward")
    suspend fun getReward(): Response<RewardSyncResponse>

    @POST("admin/reset-scores")
    suspend fun resetScores(
        @Header("x-auth-token") token: String
    ): Response<Unit>

    @POST("admin/ai-generate-category-quiz")
    suspend fun aiGenerateCategoryQuiz(
        @Header("x-auth-token") token: String,
        @Body request: AiGenerateQuizRequest
    ): Response<AiGenerateQuizResponse>
}

data class RewardSyncRequest(val title: String, val description: String, val imageUrl: String?)
data class RewardSyncResponse(val title: String, val description: String, val imageUrl: String?)
data class AiGenerateQuizRequest(val category: String, val count: Int = 5, val customQuery: String = "")
data class AiGenerateQuizResponse(val success: Boolean, val count: Int, val msg: String?)
