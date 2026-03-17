package com.studentgig.app.data.remote

import com.studentgig.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service — HTTP contract with FastAPI backend.
 * Auth header is attached automatically by AuthInterceptor in NetworkModule.
 *
 * Stage 6: Real Job Posting + Full application lifecycle + earnings.
 */
interface JobApiService {

    @GET("/")
    suspend fun checkServerStatus(): Response<ServerStatus>

    // ─── Jobs ───────────────────────────────────────────────────────────────

    @GET("/api/jobs")
    suspend fun getJobs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("category") category: String? = null
    ): Response<List<Job>>

    @GET("/api/jobs/{job_id}")
    suspend fun getJob(@Path("job_id") jobId: Int): Response<Job>

    @GET("/api/jobs/search")
    suspend fun searchJobs(
        @Query("q") query: String? = null,
        @Query("location") location: String? = null,
        @Query("min_pay") minPay: Double? = null,
        @Query("max_pay") maxPay: Double? = null,
        @Query("urgent_only") urgentOnly: Boolean = false,
        @Query("category") category: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<Job>>

    @GET("/api/jobs/categories")
    suspend fun getCategories(): Response<CategoriesResponse>

    @POST("/api/jobs")
    suspend fun createJob(@Body job: JobCreateRequest): Response<Job>

    @GET("/api/my-jobs")
    suspend fun getMyJobs(): Response<List<Job>>

    @PUT("/api/jobs/{job_id}")
    suspend fun updateJob(
        @Path("job_id") jobId: Int,
        @Body update: JobUpdateRequest
    ): Response<Job>

    @DELETE("/api/jobs/{job_id}")
    suspend fun deleteJob(@Path("job_id") jobId: Int): Response<MessageResponse>

    @GET("/api/jobs/{job_id}/applicants")
    suspend fun getJobApplicants(@Path("job_id") jobId: Int): Response<List<ApplicantInfo>>

    @GET("/api/jobs/{job_id}/check-conflict")
    suspend fun checkScheduleConflict(@Path("job_id") jobId: Int): Response<ConflictCheckResponse>

    // ─── Auth ───────────────────────────────────────────────────────────────

    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("/api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<TokenResponse>

    @POST("/api/auth/firebase")
    suspend fun firebaseLogin(@Body request: FirebaseLoginRequest): Response<TokenResponse>

    // ─── Profile ────────────────────────────────────────────────────────────

    @GET("/api/profile")
    suspend fun getProfile(): Response<UserInfo>

    @PUT("/api/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<UserInfo>

    // ─── Applications — Full Lifecycle ───────────────────────────────────────

    @POST("/api/apply")
    suspend fun applyToJob(@Body request: ApplicationRequest): Response<ApplicationResponse>

    @GET("/api/my-applications")
    suspend fun getMyApplications(): Response<List<ApplicationDetailResponse>>

    @PUT("/api/applications/{app_id}/status")
    suspend fun updateApplicationStatus(
        @Path("app_id") appId: Int,
        @Body update: ApplicationStatusUpdate
    ): Response<ApplicationDetailResponse>

    // Phase 2: Two-Sided Confirmation
    @POST("/api/applications/{app_id}/check-in")
    suspend fun checkIn(
        @Path("app_id") appId: Int,
        @Body body: String = ""
    ): Response<ApplicationDetailResponse>

    @POST("/api/applications/{app_id}/start-work")
    suspend fun startWork(
        @Path("app_id") appId: Int,
        @Body body: String = ""
    ): Response<ApplicationDetailResponse>

    @POST("/api/applications/{app_id}/complete")
    suspend fun completeWork(
        @Path("app_id") appId: Int,
        @Body body: String = ""
    ): Response<ApplicationDetailResponse>

    @POST("/api/applications/{app_id}/confirm")
    suspend fun confirmCompletion(
        @Path("app_id") appId: Int,
        @Body body: String = ""
    ): Response<ApplicationDetailResponse>

    @POST("/api/applications/{app_id}/confirm-payment")
    suspend fun confirmPayment(
        @Path("app_id") appId: Int,
        @Body body: String = ""
    ): Response<ApplicationDetailResponse>

    // Employer: Get all applications for their jobs
    @GET("/api/employer/applications")
    suspend fun getEmployerApplications(): Response<List<ApplicationDetailResponse>>

    // ─── Earnings ───────────────────────────────────────────────────────────

    @GET("/api/earnings")
    suspend fun getEarnings(): Response<EarningsResponse>

    // Phase 4: Ratings
    @POST("/api/applications/{app_id}/rate")
    suspend fun rateApplication(
        @Path("app_id") appId: Int,
        @Body request: RatingCreate
    ): Response<RatingResponse>

    @GET("/api/users/{user_id}/ratings")
    suspend fun getUserRatings(
        @Path("user_id") userId: Int
    ): Response<List<RatingResponse>>

    // ─── Phase 6: Notifications ─────────────────────────────────────────────

    @GET("/api/notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int = 20,
        @Query("skip") skip: Int = 0
    ): Response<List<NotificationItem>>

    @GET("/api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @POST("/api/notifications/mark-read")
    suspend fun markNotificationsRead(@Body body: String = ""): Response<MessageResponse>
    
    // ─── Simulation (Dev Only) ──────────────────────────────────────────────

    @POST("/api/simulate/accept-all")
    suspend fun simulateAcceptAll(@Body body: String = ""): Response<SimulationResponse>

    // ─── AI Intelligence Layer ──────────────────────────────────────────────

    /** 🤖 AI Smart Feed — personalized job ranking */
    @GET("/api/ai/feed")
    suspend fun getAIFeed(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<AIJobResponse>>

    /** 🎯 AI Skill Recommendations — suggests skills to learn */
    @GET("/api/ai/skill-recommendations")
    suspend fun getAISkillRecommendations(): Response<AISkillRecommendationsResponse>

    /** 📊 AI Applicant Ranking — ranked applicants for employers */
    @GET("/api/ai/applicants/{job_id}")
    suspend fun getAIRankedApplicants(
        @Path("job_id") jobId: Int
    ): Response<List<AIApplicantResponse>>

    /** 💰 AI Pay Estimator — suggests fair pay */
    @POST("/api/ai/estimate-pay")
    suspend fun estimatePay(
        @Body request: AIPayEstimateRequest
    ): Response<AIPayEstimateResponse>

    /** 💬 AI Job Description Generator — Gemini-powered */
    @POST("/api/ai/generate-description")
    suspend fun generateDescription(
        @Body request: AIGenerateDescriptionRequest
    ): Response<AIGenerateDescriptionResponse>

    /** 🔍 AI Smart Search — NLP natural language search */
    @POST("/api/ai/smart-search")
    suspend fun smartSearch(
        @Body request: AISmartSearchRequest
    ): Response<AISmartSearchResponse>

    /** 🤝 AI Application Note Generator */
    @POST("/api/ai/generate-application-note")
    suspend fun generateApplicationNote(
        @Body request: AIApplicationNoteRequest
    ): Response<AIApplicationNoteResponse>

    /** 🎯 AI Match Explanation — detailed match breakdown */
    @GET("/api/ai/match-explanation/{job_id}")
    suspend fun getMatchExplanation(
        @Path("job_id") jobId: Int
    ): Response<AIMatchExplanationResponse>

    /** 📈 AI Earnings Insights — analysis & predictions */
    @GET("/api/ai/earnings-insights")
    suspend fun getEarningsInsights(): Response<AIEarningsInsightsResponse>
}

