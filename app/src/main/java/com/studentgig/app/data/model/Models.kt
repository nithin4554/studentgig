package com.studentgig.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Domain model: Job listing from the database.
 * match_score is populated only when user is logged in (AI engine).
 */
data class Job(
    val id: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("pay_amount") val payAmount: Double,
    val location: String,
    @SerializedName("skills_required") val skillsRequired: String?,
    @SerializedName("is_urgent") val isUrgent: Boolean,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("match_score") val matchScore: Int? = null  // 0-100 from AI
)

/**
 * Auth: Phone-based login (OTP-ready, mocked for MVP).
 */
data class LoginRequest(
    val phone: String,
    val name: String? = "Student"
)

/**
 * Auth: Google One-Tap Sign In.
 */
data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserInfo
)

data class UserInfo(
    val id: Int,
    val phone: String,
    val name: String,
    @SerializedName("skills_json") val skillsJson: String?,
    val role: String
)

/**
 * Profile update request.
 */
data class ProfileUpdateRequest(
    val name: String? = null,
    @SerializedName("skills_json") val skillsJson: String? = null
)

/**
 * Application: Submit job application (requires JWT).
 */
data class ApplicationRequest(
    @SerializedName("job_id") val jobId: Int
)

data class ApplicationResponse(
    val id: Int,
    @SerializedName("job_id") val jobId: Int,
    @SerializedName("user_id") val userId: Int,
    val status: String,
    @SerializedName("applied_at") val appliedAt: String?
)

/**
 * Enriched application response — includes job details.
 */
data class ApplicationDetailResponse(
    val id: Int,
    @SerializedName("job_id") val jobId: Int,
    @SerializedName("user_id") val userId: Int,
    val status: String,
    @SerializedName("applied_at") val appliedAt: String?,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("job_pay_amount") val jobPayAmount: Double,
    @SerializedName("job_location") val jobLocation: String,
    @SerializedName("job_is_urgent") val jobIsUrgent: Boolean = false
)

/**
 * Health check response from GET /
 */
data class ServerStatus(
    val status: String,
    val service: String,
    val version: String
)
