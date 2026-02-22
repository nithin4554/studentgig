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
            val response = apiService.searchJobs(query, location, minPay, maxPay, urgentOnly, skip, limit)
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
                NetworkResult.Error("Login failed: ${response.code()}", response.code())
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
                NetworkResult.Error("Google Login failed: ${response.code()}", response.code())
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

    // ─── Applications ───────────────────────────────────────────────────────

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
}
