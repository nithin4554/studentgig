package com.studentgig.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Domain model: Job listing from the database.
 * match_score is populated only when user is logged in (AI engine).
 * Stage 6: Full real-job-posting fields.
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
    @SerializedName("match_score") val matchScore: Int? = null,  // 0-100 from AI
    @SerializedName("employer_id") val employerId: Int? = null,
    @SerializedName("max_applicants") val maxApplicants: Int = 1,
    // ─── New real-posting fields ──────────────────────────────────
    @SerializedName("company_name") val companyName: String? = null,
    val category: String? = null,
    @SerializedName("job_type") val jobType: String = "one-time",
    val duration: String? = null,
    val status: String = "open",
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("employer_name") val employerName: String? = null,
    @SerializedName("applicant_count") val applicantCount: Int? = null,
    // Phase 1: Scheduling
    @SerializedName("job_date") val jobDate: String? = null,       // "YYYY-MM-DD"
    @SerializedName("start_time") val startTime: String? = null,   // "HH:MM"
    @SerializedName("end_time") val endTime: String? = null,       // "HH:MM"
    val address: String? = null,
    // ─── AI Fields ──────────────────────────────────────────── 
    @SerializedName("ai_score") val aiScore: Int? = null,
    @SerializedName("ai_reason") val aiReason: String? = null,
    @SerializedName("ai_breakdown") val aiBreakdown: Map<String, Int>? = null
)

/**
 * Job creation request — sent by the user when posting a real job.
 */
data class JobCreateRequest(
    val title: String,
    val description: String? = null,
    @SerializedName("pay_amount") val payAmount: Double,
    val location: String,
    @SerializedName("skills_required") val skillsRequired: String? = null,
    @SerializedName("is_urgent") val isUrgent: Boolean = false,
    @SerializedName("company_name") val companyName: String? = null,
    val category: String? = null,
    @SerializedName("job_type") val jobType: String = "one-time",
    val duration: String? = null,
    @SerializedName("max_applicants") val maxApplicants: Int = 1,
    @SerializedName("contact_info") val contactInfo: String? = null,
    // Phase 1: Scheduling
    @SerializedName("job_date") val jobDate: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    val address: String? = null
)

/**
 * Job update request — for editing an existing job.
 */
data class JobUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("pay_amount") val payAmount: Double? = null,
    val location: String? = null,
    @SerializedName("skills_required") val skillsRequired: String? = null,
    @SerializedName("is_urgent") val isUrgent: Boolean? = null,
    @SerializedName("company_name") val companyName: String? = null,
    val category: String? = null,
    @SerializedName("job_type") val jobType: String? = null,
    val duration: String? = null,
    @SerializedName("max_applicants") val maxApplicants: Int? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    val status: String? = null,
    @SerializedName("job_date") val jobDate: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    val address: String? = null
)

/**
 * Categories response from GET /api/jobs/categories
 */
data class CategoriesResponse(
    val categories: List<String>
)

/**
 * Applicant info — employer view of who applied to their job.
 */
data class ApplicantInfo(
    @SerializedName("application_id") val applicationId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_phone") val userPhone: String,
    val status: String,
    @SerializedName("applied_at") val appliedAt: String? = null,
    @SerializedName("user_skills") val userSkills: String? = null,
    @SerializedName("match_score") val matchScore: Int? = null,
    // Phase 4: Trust badges
    @SerializedName("user_rating") val userRating: Double? = 0.0,
    @SerializedName("user_gigs_completed") val userGigsCompleted: Int? = 0,
    @SerializedName("trust_badge") val trustBadge: String? = null
)

/**
 * Schedule conflict check response.
 */
data class ConflictCheckResponse(
    @SerializedName("has_conflict") val hasConflict: Boolean = false,
    @SerializedName("conflicting_job_title") val conflictingJobTitle: String? = null,
    @SerializedName("conflicting_time") val conflictingTime: String? = null,
    val message: String = "No conflict"
)

/**
 * Auth: Password-based login.
 */
data class LoginRequest(
    val phone: String,
    val password: String
)

/**
 * Auth: Registration.
 */
data class RegisterRequest(
    val phone: String,
    val name: String,
    val password: String,
    @SerializedName("security_question") val securityQuestion: String,
    @SerializedName("security_answer") val securityAnswer: String,
    val role: String = "student"
)

/**
 * Auth: Forgot Password Step 1 (Get Question).
 */
data class ResetQuestionRequest(
    val phone: String
)

data class ResetQuestionResponse(
    val question: String
)

/**
 * Auth: Forgot Password Step 2 (Reset).
 */
data class ResetPasswordRequest(
    val phone: String,
    @SerializedName("security_answer") val securityAnswer: String,
    @SerializedName("new_password") val newPassword: String
)

/**
 * Auth: Google One-Tap Sign In.
 */
data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String,
    val role: String = "student"
)

data class FirebaseLoginRequest(
    @SerializedName("idToken") val idToken: String,
    val name: String? = "Student",
    val role: String = "student"
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
    val role: String,
    @SerializedName("total_earned") val totalEarned: Double? = 0.0,
    @SerializedName("gigs_completed") val gigsCompleted: Int? = 0,
    val rating: Double? = 0.0,
    // Phase 4: Trust badge
    @SerializedName("trust_badge") val trustBadge: String? = null
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
 * Enriched application response — includes job details + lifecycle timestamps.
 */
data class ApplicationDetailResponse(
    val id: Int,
    @SerializedName("job_id") val jobId: Int,
    @SerializedName("user_id") val userId: Int,
    val status: String,
    @SerializedName("applied_at") val appliedAt: String?,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("employer_note") val employerNote: String? = null,
    val rating: Int? = null,
    // Phase 2: Two-Sided Confirmation timestamps
    @SerializedName("checked_in_at") val checkedInAt: String? = null,
    @SerializedName("work_done_at") val workDoneAt: String? = null,
    @SerializedName("confirmed_at") val confirmedAt: String? = null,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("job_description") val jobDescription: String? = null,
    @SerializedName("job_pay_amount") val jobPayAmount: Double,
    @SerializedName("job_location") val jobLocation: String,
    @SerializedName("job_is_urgent") val jobIsUrgent: Boolean = false,
    @SerializedName("job_employer_id") val jobEmployerId: Int? = null,
    // Phase 1: Schedule
    @SerializedName("job_date") val jobDate: String? = null,
    @SerializedName("job_start_time") val jobStartTime: String? = null,
    @SerializedName("job_end_time") val jobEndTime: String? = null
)

/**
 * Status update request for application lifecycle.
 */
data class ApplicationStatusUpdate(
    val status: String,
    val note: String? = null
)

/**
 * Earnings summary for the student.
 */
data class EarningsResponse(
    @SerializedName("total_earned") val totalEarned: Double = 0.0,
    @SerializedName("pending_payment") val pendingPayment: Double = 0.0,
    @SerializedName("gigs_completed") val gigsCompleted: Int = 0,
    @SerializedName("gigs_in_progress") val gigsInProgress: Int = 0,
    @SerializedName("recent_payments") val recentPayments: List<PaymentRecord> = emptyList()
)

data class PaymentRecord(
    val id: Int,
    @SerializedName("application_id") val applicationId: Int,
    val amount: Double,
    @SerializedName("from_user_id") val fromUserId: Int,
    @SerializedName("to_user_id") val toUserId: Int,
    val status: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("released_at") val releasedAt: String? = null,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("employer_name") val employerName: String
)

/**
 * Simulation response (dev only).
 */
data class SimulationResponse(
    val message: String,
    @SerializedName("accepted_count") val acceptedCount: Int
)

/**
 * Health check response from GET /
 */
data class ServerStatus(
    val status: String,
    val service: String,
    val version: String
)

/**
 * Generic message response.
 */
data class MessageResponse(
    val message: String
)

/**
 * Rating creation request.
 */
data class RatingCreate(
    @SerializedName("rated_id") val ratedUserId: Int,
    val score: Int,
    val review: String? = null
)

/**
 * Rating response from backend.
 */
data class RatingResponse(
    val id: Int,
    @SerializedName("application_id") val applicationId: Int,
    @SerializedName("rater_id") val raterId: Int,
    @SerializedName("rated_id") val ratedId: Int,
    val score: Int,
    val review: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

/**
 * Phase 6: In-App Notification.
 */
data class NotificationItem(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val title: String,
    val message: String,
    val type: String, // application_accepted, check_in, work_done, payment, etc.
    @SerializedName("related_job_id") val relatedJobId: Int? = null,
    @SerializedName("related_application_id") val relatedApplicationId: Int? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

/**
 * Phase 6: Unread notification count for badge.
 */
data class UnreadCountResponse(
    @SerializedName("unread_count") val unreadCount: Int = 0
)

// ═══════════════════════════════════════════════════════════════════════════════════
//  AI MODELS — Intelligence Layer
// ═══════════════════════════════════════════════════════════════════════════════════

/**
 * Job with AI ranking metadata — used for Smart Feed (GET /api/ai/feed).
 * Extends normal Job fields with ai_score, ai_reason, ai_breakdown.
 */
data class AIJobResponse(
    val id: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("pay_amount") val payAmount: Double,
    val location: String,
    @SerializedName("skills_required") val skillsRequired: String?,
    @SerializedName("is_urgent") val isUrgent: Boolean,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("match_score") val matchScore: Int? = null,
    @SerializedName("employer_id") val employerId: Int? = null,
    @SerializedName("max_applicants") val maxApplicants: Int = 1,
    @SerializedName("company_name") val companyName: String? = null,
    val category: String? = null,
    @SerializedName("job_type") val jobType: String = "one-time",
    val duration: String? = null,
    val status: String = "open",
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("employer_name") val employerName: String? = null,
    @SerializedName("applicant_count") val applicantCount: Int? = null,
    @SerializedName("job_date") val jobDate: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    val address: String? = null,
    // ─── AI Fields ────────────────────────────────────────────
    @SerializedName("ai_score") val aiScore: Int? = null,
    @SerializedName("ai_reason") val aiReason: String? = null,
    @SerializedName("ai_breakdown") val aiBreakdown: Map<String, Int>? = null
)

/**
 * Individual skill recommendation from AI.
 */
data class AISkillRecommendation(
    val skill: String,
    @SerializedName("demand_count") val demandCount: Int,
    @SerializedName("new_matches") val newMatches: Int,
    val categories: List<String> = emptyList(),
    val reason: String
)

/**
 * GET /api/ai/skill-recommendations response.
 */
data class AISkillRecommendationsResponse(
    val recommendations: List<AISkillRecommendation> = emptyList(),
    @SerializedName("current_skills") val currentSkills: List<String> = emptyList(),
    @SerializedName("total_open_jobs") val totalOpenJobs: Int = 0
)

/**
 * Applicant with AI ranking — employer view (GET /api/ai/applicants/{job_id}).
 */
data class AIApplicantResponse(
    @SerializedName("application_id") val applicationId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_phone") val userPhone: String,
    val status: String,
    @SerializedName("applied_at") val appliedAt: String? = null,
    @SerializedName("user_skills") val userSkills: String? = null,
    @SerializedName("match_score") val matchScore: Int? = null,
    @SerializedName("user_rating") val userRating: Double? = 0.0,
    @SerializedName("user_gigs_completed") val userGigsCompleted: Int? = 0,
    @SerializedName("trust_badge") val trustBadge: String? = null,
    // ─── AI Fields ────────────────────────────────────────────
    @SerializedName("ai_rank_score") val aiRankScore: Int? = null,
    @SerializedName("ai_badges") val aiBadges: List<String> = emptyList(),
    @SerializedName("ai_breakdown") val aiBreakdown: Map<String, Int>? = null
)

/**
 * POST /api/ai/estimate-pay request.
 */
data class AIPayEstimateRequest(
    val category: String? = null,
    val location: String? = null,
    val duration: String? = null,
    @SerializedName("job_type") val jobType: String = "one-time"
)

/**
 * AI Pay estimation result.
 */
data class AIPayEstimateResponse(
    @SerializedName("min_pay") val minPay: Double? = null,
    @SerializedName("avg_pay") val avgPay: Double? = null,
    @SerializedName("max_pay") val maxPay: Double? = null,
    @SerializedName("sample_size") val sampleSize: Int = 0,
    val confidence: String = "low",
    val reasoning: String = ""
)

/**
 * POST /api/ai/generate-description request.
 */
data class AIGenerateDescriptionRequest(
    val title: String,
    val category: String? = null,
    @SerializedName("rough_notes") val roughNotes: String? = null,
    val location: String? = null,
    val duration: String? = null
)

/**
 * AI-generated job description response.
 */
data class AIGenerateDescriptionResponse(
    val description: String,
    @SerializedName("suggested_skills") val suggestedSkills: String? = null,
    @SerializedName("suggested_category") val suggestedCategory: String? = null,
    @SerializedName("suggested_pay_min") val suggestedPayMin: Double? = null,
    @SerializedName("suggested_pay_max") val suggestedPayMax: Double? = null,
    @SerializedName("ai_generated") val aiGenerated: Boolean = false
)

/**
 * POST /api/ai/smart-search request.
 */
data class AISmartSearchRequest(
    val query: String
)

/**
 * AI Smart Search response — parsed filters + job results.
 */
data class AISmartSearchResponse(
    @SerializedName("search_text") val searchText: String? = null,
    val location: String? = null,
    @SerializedName("min_pay") val minPay: Double? = null,
    @SerializedName("max_pay") val maxPay: Double? = null,
    val category: String? = null,
    @SerializedName("urgent_only") val urgentOnly: Boolean = false,
    val interpretation: String = "",
    @SerializedName("ai_parsed") val aiParsed: Boolean = false,
    val jobs: List<Job> = emptyList()
)

/**
 * POST /api/ai/generate-application-note request.
 */
data class AIApplicationNoteRequest(
    @SerializedName("job_id") val jobId: Int
)

/**
 * AI-generated application cover note response.
 */
data class AIApplicationNoteResponse(
    val note: String,
    @SerializedName("ai_generated") val aiGenerated: Boolean = false
)

/**
 * GET /api/ai/match-explanation/{job_id} — detailed match breakdown.
 */
data class AIMatchExplanationResponse(
    val score: Int = 0,
    @SerializedName("matched_skills") val matchedSkills: List<String> = emptyList(),
    @SerializedName("missing_skills") val missingSkills: List<String> = emptyList(),
    @SerializedName("extra_skills") val extraSkills: List<String> = emptyList(),
    val explanation: String = ""
)

/**
 * GET /api/ai/earnings-insights — AI analysis of earning patterns.
 */
data class AIEarningsInsightsResponse(
    val insights: List<String> = emptyList(),
    @SerializedName("best_category") val bestCategory: String? = null,
    @SerializedName("projected_monthly") val projectedMonthly: Int = 0,
    val tips: List<String> = emptyList()
)
