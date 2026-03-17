package com.studentgig.app.data.repository

import com.studentgig.app.data.local.TokenManager
import com.studentgig.app.data.model.*
import com.studentgig.app.data.remote.JobApiService
import javax.inject.Inject
import javax.inject.Singleton

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}

@Singleton
class JobRepository @Inject constructor(
    private val apiService: JobApiService,
    private val tokenManager: TokenManager
) {
    // ─── Server Health ──────────────────────────────────────────────────────

    suspend fun checkServerStatus(): NetworkResult<ServerStatus> {
        return try {
            val response = apiService.checkServerStatus()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Server responded with error: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Start the Python backend on port 8000")
        } catch (e: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out — Is the server running?")
        } catch (e: java.io.IOException) {
            NetworkResult.Error("Network error — Check ADB connection and backend")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // ─── Jobs ───────────────────────────────────────────────────────────────

    suspend fun getJobs(skip: Int = 0, limit: Int = 50): NetworkResult<List<Job>> {
        return try {
            val response = apiService.getJobs(skip, limit)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load jobs: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Start the Python backend on port 8000")
        } catch (e: java.net.SocketTimeoutException) {
            NetworkResult.Error("Connection timed out — Is the server running?")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getJob(jobId: Int): NetworkResult<Job> {
        return try {
            val response = apiService.getJob(jobId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load job: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun searchJobs(
        query: String? = null,
        location: String? = null,
        minPay: Double? = null,
        maxPay: Double? = null,
        urgentOnly: Boolean = false,
        skip: Int = 0,
        limit: Int = 50
    ): NetworkResult<List<Job>> {
        return try {
            val response = apiService.searchJobs(query, location, minPay, maxPay, urgentOnly, null, skip, limit)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Search failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot search")
        } catch (e: Exception) {
            NetworkResult.Error("Search error: ${e.localizedMessage}")
        }
    }

    // ─── Job Posting (Real Jobs) ────────────────────────────────────────────

    suspend fun createJob(request: JobCreateRequest): NetworkResult<Job> {
        return try {
            val response = apiService.createJob(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else if (response.code() == 401) {
                NetworkResult.Error("Login required to post a job", 401)
            } else {
                NetworkResult.Error("Failed to create job: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot post job")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getMyJobs(): NetworkResult<List<Job>> {
        return try {
            val response = apiService.getMyJobs()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load your jobs: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun updateJob(jobId: Int, update: JobUpdateRequest): NetworkResult<Job> {
        return try {
            val response = apiService.updateJob(jobId, update)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else if (response.code() == 403) {
                NetworkResult.Error("Not authorized to edit this job", 403)
            } else {
                NetworkResult.Error("Update failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun deleteJob(jobId: Int): NetworkResult<MessageResponse> {
        return try {
            val response = apiService.deleteJob(jobId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else if (response.code() == 403) {
                NetworkResult.Error("Not authorized to delete this job", 403)
            } else {
                NetworkResult.Error("Delete failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }


    suspend fun getJobApplicants(jobId: Int): NetworkResult<List<ApplicantInfo>> {
        return try {
            val response = apiService.getJobApplicants(jobId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load applicants: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getCategories(): NetworkResult<List<String>> {
        return try {
            val response = apiService.getCategories()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!.categories)
            } else {
                NetworkResult.Error("Failed to load categories: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun checkScheduleConflict(jobId: Int): NetworkResult<ConflictCheckResponse> {
        return try {
            val response = apiService.checkScheduleConflict(jobId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Conflict check failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // ─── Auth ───────────────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
    fun getUserName(): String? = tokenManager.getUserName()
    fun getUserPhone(): String? = tokenManager.getUserPhone()
    fun getUserId(): Int = tokenManager.getUserId()

    suspend fun login(phone: String, name: String? = null): NetworkResult<TokenResponse> {
        return try {
            val response = apiService.login(LoginRequest(phone, name ?: "Student"))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Save token and user info
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveUser(body.user.id, body.user.name, body.user.phone)
                if (body.user.skillsJson != null) {
                    tokenManager.saveSkills(body.user.skillsJson)
                }
                NetworkResult.Success(body)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val detail = try {
                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                    json.get("detail")?.asString ?: "Login failed"
                } catch (_: Exception) { "Login failed (${response.code()})" }
                NetworkResult.Error(detail, response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot login")
        } catch (e: Exception) {
            NetworkResult.Error("Login error: ${e.localizedMessage}")
        }
    }

    suspend fun googleLogin(idToken: String): NetworkResult<TokenResponse> {
        return try {
            val response = apiService.googleLogin(GoogleLoginRequest(idToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveUser(body.user.id, body.user.name, body.user.phone)
                if (body.user.skillsJson != null) {
                    tokenManager.saveSkills(body.user.skillsJson)
                }
                NetworkResult.Success(body)
            } else {
                // Parse error body for the actual detail message
                val errorBody = response.errorBody()?.string() ?: ""
                val detail = try {
                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                    json.get("detail")?.asString ?: "Unknown error"
                } catch (_: Exception) { errorBody.take(200) }
                NetworkResult.Error("Google Login failed: $detail", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot login")
        } catch (e: Exception) {
            NetworkResult.Error("Login error: ${e.localizedMessage}")
        }
    }

    suspend fun firebaseLogin(idToken: String, name: String? = null): NetworkResult<TokenResponse> {
        return try {
            val response = apiService.firebaseLogin(FirebaseLoginRequest(idToken, name))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveUser(body.user.id, body.user.name, body.user.phone)
                if (body.user.skillsJson != null) {
                    tokenManager.saveSkills(body.user.skillsJson)
                }
                NetworkResult.Success(body)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val detail = try {
                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                    json.get("detail")?.asString ?: "Unknown error"
                } catch (_: Exception) { errorBody.take(200) }
                NetworkResult.Error("Firebase Login failed: $detail", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot login")
        } catch (e: Exception) {
            NetworkResult.Error("Login error: ${e.localizedMessage}")
        }
    }

    fun logout() {
        tokenManager.logout()
    }

    // ─── Profile ────────────────────────────────────────────────────────────

    suspend fun getProfile(): NetworkResult<UserInfo> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                tokenManager.saveUser(user.id, user.name, user.phone)
                if (user.skillsJson != null) {
                    tokenManager.saveSkills(user.skillsJson)
                }
                NetworkResult.Success(user)
            } else {
                NetworkResult.Error("Failed to load profile: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Profile error: ${e.localizedMessage}")
        }
    }

    suspend fun updateProfile(name: String?, skillsJson: String?): NetworkResult<UserInfo> {
        return try {
            val response = apiService.updateProfile(ProfileUpdateRequest(name, skillsJson))
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                tokenManager.saveUser(user.id, user.name, user.phone)
                if (user.skillsJson != null) {
                    tokenManager.saveSkills(user.skillsJson)
                }
                NetworkResult.Success(user)
            } else {
                NetworkResult.Error("Update failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot update profile")
        } catch (e: Exception) {
            NetworkResult.Error("Update error: ${e.localizedMessage}")
        }
    }

    // ─── Applications — Full Lifecycle ──────────────────────────────────────

    suspend fun applyToJob(jobId: Int): NetworkResult<ApplicationResponse> {
        return try {
            val response = apiService.applyToJob(ApplicationRequest(jobId))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else if (response.code() == 401) {
                NetworkResult.Error("Login required", 401)
            } else if (response.code() == 409) {
                NetworkResult.Error("Already applied to this job", 409)
            } else {
                NetworkResult.Error("Application failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline — Cannot apply")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getMyApplications(): NetworkResult<List<ApplicationDetailResponse>> {
        return try {
            val response = apiService.getMyApplications()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load applications: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // Employer: update application status (accept/reject)
    suspend fun updateApplicationStatus(appId: Int, update: ApplicationStatusUpdate): NetworkResult<ApplicationDetailResponse> {
        return try {
            val response = apiService.updateApplicationStatus(appId, update)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot update status: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun startWork(appId: Int): NetworkResult<ApplicationDetailResponse> {
        return try {
            val response = apiService.startWork(appId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot start work: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun completeWork(appId: Int): NetworkResult<ApplicationDetailResponse> {
        return try {
            val response = apiService.completeWork(appId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot complete: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // Phase 2: Student check-in
    suspend fun checkIn(appId: Int): NetworkResult<ApplicationDetailResponse> {
        return try {
            val response = apiService.checkIn(appId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot check in: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // Phase 2: Employer confirms completion
    suspend fun confirmCompletion(appId: Int): NetworkResult<ApplicationDetailResponse> {
        return try {
            val response = apiService.confirmCompletion(appId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot confirm: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // Phase 2: Get employer applications (all applications for jobs posted by current user)
    suspend fun getEmployerApplications(): NetworkResult<List<ApplicationDetailResponse>> {
        return try {
            val response = apiService.getEmployerApplications()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load employer applications: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun confirmPayment(appId: Int): NetworkResult<ApplicationDetailResponse> {
        return try {
            val response = apiService.confirmPayment(appId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot confirm payment: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // ─── Earnings ───────────────────────────────────────────────────────────

    suspend fun getEarnings(): NetworkResult<EarningsResponse> {
        return try {
            val response = apiService.getEarnings()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load earnings: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // Phase 4: Ratings ───────────────────────────────────────────────────────

    suspend fun rateApplication(appId: Int, request: RatingCreate): NetworkResult<RatingResponse> {
        return try {
            val response = apiService.rateApplication(appId, request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Failed"
                NetworkResult.Error("Cannot submit rating: $errorBody", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getUserRatings(userId: Int): NetworkResult<List<RatingResponse>> {
        return try {
            val response = apiService.getUserRatings(userId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load ratings: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // ─── Phase 6: Notifications ─────────────────────────────────────────────

    suspend fun getNotifications(limit: Int = 20, skip: Int = 0): NetworkResult<List<NotificationItem>> {
        return try {
            val response = apiService.getNotifications(limit, skip)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to load notifications: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun getUnreadCount(): NetworkResult<UnreadCountResponse> {
        return try {
            val response = apiService.getUnreadCount()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to get count", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun markNotificationsRead(): NetworkResult<MessageResponse> {
        return try {
            val response = apiService.markNotificationsRead()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Failed to mark read: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    // ─── Simulation (Dev Only) ──────────────────────────────────────────────

    suspend fun simulateAcceptAll(): NetworkResult<SimulationResponse> {
        return try {
            val response = apiService.simulateAcceptAll()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Simulation failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Simulation error: ${e.localizedMessage}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AI INTELLIGENCE LAYER
    // ═══════════════════════════════════════════════════════════════════════════

    /** 🤖 AI Smart Feed — personalized job ranking */
    suspend fun getAIFeed(skip: Int = 0, limit: Int = 50): NetworkResult<List<AIJobResponse>> {
        return try {
            val response = apiService.getAIFeed(skip, limit)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("AI Feed failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("AI Feed error: ${e.localizedMessage}")
        }
    }

    /** 🎯 AI Skill Recommendations */
    suspend fun getAISkillRecommendations(): NetworkResult<AISkillRecommendationsResponse> {
        return try {
            val response = apiService.getAISkillRecommendations()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Skill recommendations failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Skill recommendations error: ${e.localizedMessage}")
        }
    }

    /** 📊 AI Applicant Ranking */
    suspend fun getAIRankedApplicants(jobId: Int): NetworkResult<List<AIApplicantResponse>> {
        return try {
            val response = apiService.getAIRankedApplicants(jobId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("AI ranking failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("AI ranking error: ${e.localizedMessage}")
        }
    }

    /** 💰 AI Pay Estimator */
    suspend fun estimatePay(request: AIPayEstimateRequest): NetworkResult<AIPayEstimateResponse> {
        return try {
            val response = apiService.estimatePay(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Pay estimation failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Pay estimation error: ${e.localizedMessage}")
        }
    }

    /** 💬 AI Job Description Generator */
    suspend fun generateDescription(request: AIGenerateDescriptionRequest): NetworkResult<AIGenerateDescriptionResponse> {
        return try {
            val response = apiService.generateDescription(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("AI generation failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("AI generation error: ${e.localizedMessage}")
        }
    }

    /** 🔍 AI Smart Search */
    suspend fun smartSearch(query: String): NetworkResult<AISmartSearchResponse> {
        return try {
            val response = apiService.smartSearch(AISmartSearchRequest(query))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Smart search failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Smart search error: ${e.localizedMessage}")
        }
    }

    /** 🤝 AI Application Note Generator */
    suspend fun generateApplicationNote(jobId: Int): NetworkResult<AIApplicationNoteResponse> {
        return try {
            val response = apiService.generateApplicationNote(AIApplicationNoteRequest(jobId))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Note generation failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Note generation error: ${e.localizedMessage}")
        }
    }

    /** 🎯 AI Match Explanation */
    suspend fun getMatchExplanation(jobId: Int): NetworkResult<AIMatchExplanationResponse> {
        return try {
            val response = apiService.getMatchExplanation(jobId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Match explanation failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Match explanation error: ${e.localizedMessage}")
        }
    }

    /** 📈 AI Earnings Insights */
    suspend fun getEarningsInsights(): NetworkResult<AIEarningsInsightsResponse> {
        return try {
            val response = apiService.getEarningsInsights()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Earnings insights failed: ${response.code()}", response.code())
            }
        } catch (e: java.net.ConnectException) {
            NetworkResult.Error("Server Offline")
        } catch (e: Exception) {
            NetworkResult.Error("Earnings insights error: ${e.localizedMessage}")
        }
    }
}

