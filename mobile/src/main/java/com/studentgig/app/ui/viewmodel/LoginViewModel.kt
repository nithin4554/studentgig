package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.auth.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val mode: String = "login", // "login", "register", "forgot"
    val forgotStep: Int = 1, // 1 (get question), 2 (submit answer and new password)
    
    val phone: String = "",
    val name: String = "",
    val password: String = "",
    val secQuestion: String = "",
    val secAnswer: String = "",
    val newPassword: String = "",
    
    val fetchedQuestion: String = "",
    val role: String = "student",

    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val successMsg: String? = null,
    val isLoggedIn: Boolean = false,

    // Track if a field has been interacted with (touched)
    val touchedFields: Set<String> = emptySet()
) {
    // Computed validation errors
    val nameError: String? get() {
        if (!touchedFields.contains("name")) return null
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name is required"
        if (trimmed.length < 3) return "Name must be at least 3 characters"
        val dummies = listOf("abc", "xyz", "test", "demo", "asdf", "qwerty", "admin", "user", "name", "hello")
        if (dummies.contains(trimmed.lowercase())) return "Please enter your real name"
        if (trimmed.length >= 4 && trimmed.lowercase().replace(" ", "").toSet().size <= 1) return "Name cannot be all the same character"
        if (!trimmed.all { it.isLetter() || it.isWhitespace() }) return "Only letters and spaces allowed"
        return null
    }

    val phoneError: String? get() {
        if (!touchedFields.contains("phone")) return null
        if (phone.isEmpty()) return "Phone number is required"
        if (phone.length < 10) return "${10 - phone.length} more digit${if (10 - phone.length > 1) "s" else ""} needed"
        if (!phone.matches(Regex("^[6-9].*"))) return "Indian mobile numbers start with 6, 7, 8, or 9"
        if (phone.toSet().size <= 2) return "Too many repeated digits"
        if ("0123456789".contains(phone) || "9876543210".contains(phone)) return "Sequential patterns are not allowed"
        return null
    }

    val passwordError: String? get() {
        if (!touchedFields.contains("password")) return null
        if (password.isEmpty()) return "Password is required"
        if (mode == "register" && password.length < 6) return "${6 - password.length} more character${if (6 - password.length > 1) "s" else ""} needed"
        if (mode == "login" && password.length < 4) return "Password must be at least 4 characters"
        return null
    }

    val secQuestionError: String? get() {
        if (!touchedFields.contains("secQuestion")) return null
        if (secQuestion.trim().isEmpty()) return "Security question is required"
        if (secQuestion.trim().length < 5) return "Must be at least 5 characters"
        return null
    }

    val secAnswerError: String? get() {
        if (!touchedFields.contains("secAnswer")) return null
        if (secAnswer.trim().isEmpty()) return "Answer is required"
        if (secAnswer.trim().length < 3) return "Must be at least 3 characters"
        return null
    }
    
    val newPasswordError: String? get() {
        if (!touchedFields.contains("newPassword")) return null
        if (newPassword.isEmpty()) return "New password is required"
        if (newPassword.length < 6) return "${6 - newPassword.length} more character${if (6 - newPassword.length > 1) "s" else ""} needed"
        return null
    }

    val isRegisterFormValid: Boolean get() = nameError == null && phoneError == null && passwordError == null && secQuestionError == null && secAnswerError == null && phone.isNotEmpty() && name.isNotEmpty() && password.isNotEmpty() && secQuestion.isNotEmpty() && secAnswer.isNotEmpty()
    val isLoginFormValid: Boolean get() = phoneError == null && passwordError == null && phone.isNotEmpty() && password.isNotEmpty()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateField(field: String, value: String) {
        val s = _uiState.value
        val newState = when(field) {
            "mode" -> s.copy(mode = value, errorMsg = null, successMsg = null, fetchedQuestion = "", forgotStep = 1, touchedFields = emptySet(), phone = "", password = "")
            "role" -> s.copy(role = value, errorMsg = null, successMsg = null)
            "phone" -> s.copy(phone = value.filter { it.isDigit() }.take(10), errorMsg = null, successMsg = null)
            "name" -> s.copy(name = value.filter { it.isLetter() || it.isWhitespace() }.take(50), errorMsg = null, successMsg = null)
            "password" -> s.copy(password = value.take(50), errorMsg = null, successMsg = null)
            "secQuestion" -> s.copy(secQuestion = value, errorMsg = null, successMsg = null)
            "secAnswer" -> s.copy(secAnswer = value.take(50), errorMsg = null, successMsg = null)
            "newPassword" -> s.copy(newPassword = value.take(50), errorMsg = null, successMsg = null)
            else -> s
        }

        _uiState.value = newState
    }

    fun markTouched(field: String) {
        val s = _uiState.value
        _uiState.value = s.copy(touchedFields = s.touchedFields + field)
    }

    fun markAllTouchedLogin() {
        val s = _uiState.value
        _uiState.value = s.copy(touchedFields = s.touchedFields + listOf("phone", "password"))
    }

    fun markAllTouchedRegister() {
        val s = _uiState.value
        _uiState.value = s.copy(touchedFields = s.touchedFields + listOf("phone", "password", "name", "secQuestion", "secAnswer"))
    }
    
    fun markAllTouchedForgotStep2() {
        val s = _uiState.value
        _uiState.value = s.copy(touchedFields = s.touchedFields + listOf("secAnswer", "newPassword"))
    }
    
    fun markPhoneTouched() {
        val s = _uiState.value
        _uiState.value = s.copy(touchedFields = s.touchedFields + "phone")
    }

    fun performLogin(onSuccess: () -> Unit) {
        markAllTouchedLogin()
        val s = _uiState.value
        if (!s.isLoginFormValid) {
            _uiState.value = s.copy(errorMsg = "Please fix the highlighted errors")
            return
        }
        
        _uiState.value = s.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch {
            when (val result = authManager.performLogin(s.phone, s.password)) {
                is LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = result.message)
                }
            }
        }
    }

    fun performRegister(onSuccess: () -> Unit) {
        markAllTouchedRegister()
        val s = _uiState.value
        if (!s.isRegisterFormValid) {
             _uiState.value = s.copy(errorMsg = "Please fix the highlighted errors")
            return
        }
        
        _uiState.value = s.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch {
            when (val result = authManager.performRegister(s.phone, s.name, s.password, s.secQuestion, s.secAnswer, s.role)) {
                is LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = result.message)
                }
            }
        }
    }

    fun performGetResetQuestion() {
        markPhoneTouched()
        val s = _uiState.value
        if (s.phoneError != null || s.phone.isEmpty()) {
            _uiState.value = s.copy(errorMsg = "Enter a valid 10-digit phone number")
            return
        }
        
        _uiState.value = s.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch {
            when (val result = authManager.performGetResetQuestion(s.phone)) {
                is ResetQuestionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        fetchedQuestion = result.question,
                        forgotStep = 2,
                        errorMsg = null
                    )
                }
                is ResetQuestionResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = result.message)
                }
            }
        }
    }

    fun performResetPassword(onSuccessGoLogin: () -> Unit) {
        markAllTouchedForgotStep2()
        val s = _uiState.value
        if (s.secAnswerError != null || s.newPasswordError != null || s.secAnswer.isEmpty() || s.newPassword.isEmpty()) {
            _uiState.value = s.copy(errorMsg = "Please fix the highlighted errors")
            return
        }
        
        _uiState.value = s.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch {
            when (val result = authManager.performResetPassword(s.phone, s.secAnswer, s.newPassword)) {
                is BasicResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        mode = "login",
                        isLoading = false,
                        successMsg = "Password reset! Please login.",
                        errorMsg = null,
                        fetchedQuestion = "",
                        forgotStep = 1,
                        touchedFields = emptySet(),
                        password = ""
                    )
                    onSuccessGoLogin()
                }
                is BasicResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = result.message)
                }
            }
        }
    }

    fun performFirebaseLogin(idToken: String, name: String?, onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch {
            when (val result = authManager.performFirebaseLogin(idToken, name, _uiState.value.role)) {
                is LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = result.message)
                }
            }
        }
    }

    fun performGoogleLogin(idToken: String, onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
        viewModelScope.launch {
            when (val result = authManager.performGoogleLogin(idToken, _uiState.value.role)) {
                is LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = result.message)
                }
            }
        }
    }
}
