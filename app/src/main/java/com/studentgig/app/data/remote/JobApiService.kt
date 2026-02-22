package com.studentgig.app.data.remote

import com.studentgig.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service — HTTP contract with FastAPI backend.
 * Auth header is attached automatically by AuthInterceptor in NetworkModule.
 */
interface JobApiService {

    @GET("/")
    suspend fun checkServerStatus(): Response<ServerStatus>

    // ─── Jobs ───────────────────────────────────────────────────────────────

    @GET("/api/jobs")
    suspend fun getJobs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
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
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<Job>>

    @POST("/api/jobs")
    suspend fun createJob(@Body job: Job): Response<Job>

    // ─── Auth ───────────────────────────────────────────────────────────────

    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("/api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<TokenResponse>

    // ─── Profile ────────────────────────────────────────────────────────────

    @GET("/api/profile")
    suspend fun getProfile(): Response<UserInfo>

    @PUT("/api/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<UserInfo>

    // ─── Applications ───────────────────────────────────────────────────────

    @POST("/api/apply")
    suspend fun applyToJob(@Body request: ApplicationRequest): Response<ApplicationResponse>

    @GET("/api/my-applications")
    suspend fun getMyApplications(): Response<List<ApplicationDetailResponse>>
}
