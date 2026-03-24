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
    suspend fun performLogin(phone: String, password: String): LoginResult {
        return when (val result = repository.login(phone, password)) {
            is NetworkResult.Success -> LoginResult.Success(result.data.user.name)
            is NetworkResult.Error -> LoginResult.Failure(result.message)
            is NetworkResult.Loading -> LoginResult.Failure("Unexpected state")
        }
    }
    suspend fun performRegister(phone: String, name: String, password: String, secQuestion: String, secAnswer: String, role: String): LoginResult {
        return when (val result = repository.register(phone, name, password, secQuestion, secAnswer, role)) {
            is NetworkResult.Success -> LoginResult.Success(result.data.user.name)
            is NetworkResult.Error -> LoginResult.Failure(result.message)
            is NetworkResult.Loading -> LoginResult.Failure("Unexpected state")
        }
    }

    suspend fun performGetResetQuestion(phone: String): ResetQuestionResult {
        return when (val result = repository.getResetQuestion(phone)) {
            is NetworkResult.Success -> ResetQuestionResult.Success(result.data.question)
            is NetworkResult.Error -> ResetQuestionResult.Failure(result.message)
            is NetworkResult.Loading -> ResetQuestionResult.Failure("Unexpected state")
        }
    }

    suspend fun performResetPassword(phone: String, secAnswer: String, newPassword: String): BasicResult {
        return when (val result = repository.resetPassword(phone, secAnswer, newPassword)) {
            is NetworkResult.Success -> BasicResult.Success
            is NetworkResult.Error -> BasicResult.Failure(result.message)
            is NetworkResult.Loading -> BasicResult.Failure("Unexpected state")
        }
    }

    /**
     * Google-based login. Returns [LoginResult.Success] with user name on success,
     * or [LoginResult.Failure] with error message.
     */
    suspend fun performGoogleLogin(idToken: String, role: String): LoginResult {
        return when (val result = repository.googleLogin(idToken, role)) {
            is NetworkResult.Success -> LoginResult.Success(result.data.user.name)
            is NetworkResult.Error -> LoginResult.Failure(result.message)
            is NetworkResult.Loading -> LoginResult.Failure("Unexpected state")
        }
    }

    /**
     * Firebase-based login. Returns [LoginResult.Success] with user name on success,
     * or [LoginResult.Failure] with error message.
     */
    suspend fun performFirebaseLogin(idToken: String, name: String? = null, role: String): LoginResult {
        return when (val result = repository.firebaseLogin(idToken, name, role)) {
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

sealed class ResetQuestionResult {
    data class Success(val question: String) : ResetQuestionResult()
    data class Failure(val message: String) : ResetQuestionResult()
}

sealed class BasicResult {
    data object Success : BasicResult()
    data class Failure(val message: String) : BasicResult()
}
