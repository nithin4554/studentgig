package com.studentgig.app.data.auth

import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager — centralizes authentication logic.
 *
 * Use this instead of duplicating login/logout calls across ViewModels.
 * Each ViewModel can call [performLogin] / [performGoogleLogin] and then
 * handle post-login behavior (reloading data, etc.) independently.
 */
@Singleton
class AuthManager @Inject constructor(
    private val repository: JobRepository
) {
    // ─── State Queries ──────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
    fun getUserName(): String? = repository.getUserName()
    fun getUserPhone(): String? = repository.getUserPhone()
    fun getUserId(): Int = repository.getUserId()

    // ─── Login ──────────────────────────────────────────────────────────────

    /**
     * Phone-based login. Returns [LoginResult.Success] with user name on success,
     * or [LoginResult.Failure] with error message.
     */
    suspend fun performLogin(phone: String, name: String?): LoginResult {
        return when (val result = repository.login(phone, name)) {
            is NetworkResult.Success -> LoginResult.Success(result.data.user.name)
            is NetworkResult.Error -> LoginResult.Failure(result.message)
            is NetworkResult.Loading -> LoginResult.Failure("Unexpected state")
        }
    }

    /**
     * Google-based login. Returns [LoginResult.Success] with user name on success,
     * or [LoginResult.Failure] with error message.
     */
    suspend fun performGoogleLogin(idToken: String): LoginResult {
        return when (val result = repository.googleLogin(idToken)) {
            is NetworkResult.Success -> LoginResult.Success(result.data.user.name)
            is NetworkResult.Error -> LoginResult.Failure(result.message)
            is NetworkResult.Loading -> LoginResult.Failure("Unexpected state")
        }
    }

    /**
     * Firebase-based login. Returns [LoginResult.Success] with user name on success,
     * or [LoginResult.Failure] with error message.
     */
    suspend fun performFirebaseLogin(idToken: String, name: String? = null): LoginResult {
        return when (val result = repository.firebaseLogin(idToken, name)) {
            is NetworkResult.Success -> LoginResult.Success(result.data.user.name)
            is NetworkResult.Error -> LoginResult.Failure(result.message)
            is NetworkResult.Loading -> LoginResult.Failure("Unexpected state")
        }
    }

    // ─── Logout ─────────────────────────────────────────────────────────────

    fun logout() {
        repository.logout()
    }
}

/**
 * Result of a login attempt. Simple sealed class so ViewModels
 * can handle success/failure without knowing about [NetworkResult].
 */
sealed class LoginResult {
    data class Success(val userName: String) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}
