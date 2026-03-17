package com.studentgig.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.Job
import com.studentgig.app.data.model.JobCreateRequest
import com.studentgig.app.data.model.*
import com.studentgig.app.data.auth.AuthManager
import com.studentgig.app.data.auth.LoginResult
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostJobUiState(
    val currentStep: Int = 0,           // 0-2 (3 steps)
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val payAmount: String = "",
    val location: String = "",
    val skillsRequired: String = "",     // comma-separated input
    val isUrgent: Boolean = false,
    val companyName: String = "",
    val jobType: String = "one-time",
    val duration: String = "",
    val maxApplicants: String = "1",
    val contactInfo: String = "",
    // Phase 1: Scheduling
    val jobDate: String = "",           // "YYYY-MM-DD" or empty = flexible
    val startTime: String = "",         // "HH:MM" 24h or empty
    val endTime: String = "",           // "HH:MM" 24h or empty
    val address: String = "",           // Full address
    // UI state
    val isPosting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val postedJob: Job? = null,
    val isLoggedIn: Boolean = false,
    // AI Features
    val isGeneratingDescription: Boolean = false,
    val aiDescriptionData: AIGenerateDescriptionResponse? = null,
    val aiDescriptionError: String? = null,
    val isEstimatingPay: Boolean = false,
    val aiPayData: AIPayEstimateResponse? = null,
    val aiPayError: String? = null,
    // Lookups
    val categories: List<String> = listOf(
        "Tutoring", "Delivery", "Events", "Tech", "Content Creation",
        "Design", "Marketing", "Data Entry", "Photography", "Volunteering",
        "Writing", "Translation", "Hospitality", "Fitness", "Other"
    ),
    val jobTypes: List<String> = listOf("one-time", "part-time", "recurring"),
    // Validation errors
    val titleError: String? = null,
    val payError: String? = null,
    val locationError: String? = null,
    val categoryError: String? = null,
    // Login sheet
    val showLoginSheet: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
)

@HiltViewModel
class PostJobViewModel @Inject constructor(
    private val repository: JobRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostJobUiState())
    val uiState: StateFlow<PostJobUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(isLoggedIn = authManager.isLoggedIn())
    }

    // ─── Field Setters ─────────────────────────────────────────────────────

    fun setTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value, titleError = null)
    }

    fun setDescription(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun setCategory(value: String) {
        _uiState.value = _uiState.value.copy(category = value, categoryError = null)
    }

    fun setPayAmount(value: String) {
        _uiState.value = _uiState.value.copy(payAmount = value, payError = null)
    }

    fun setLocation(value: String) {
        _uiState.value = _uiState.value.copy(location = value, locationError = null)
    }

    fun setSkillsRequired(value: String) {
        _uiState.value = _uiState.value.copy(skillsRequired = value)
    }

    fun setIsUrgent(value: Boolean) {
        _uiState.value = _uiState.value.copy(isUrgent = value)
    }

    fun setCompanyName(value: String) {
        _uiState.value = _uiState.value.copy(companyName = value)
    }

    fun setJobType(value: String) {
        _uiState.value = _uiState.value.copy(jobType = value)
    }

    fun setDuration(value: String) {
        _uiState.value = _uiState.value.copy(duration = value)
    }

    fun setMaxApplicants(value: String) {
        _uiState.value = _uiState.value.copy(maxApplicants = value)
    }

    fun setContactInfo(value: String) {
        _uiState.value = _uiState.value.copy(contactInfo = value)
    }

    fun setJobDate(value: String) {
        _uiState.value = _uiState.value.copy(jobDate = value)
    }

    fun setStartTime(value: String) {
        _uiState.value = _uiState.value.copy(startTime = value)
    }

    fun setEndTime(value: String) {
        _uiState.value = _uiState.value.copy(endTime = value)
    }

    fun setAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    // ─── Step Navigation ───────────────────────────────────────────────────

    fun nextStep(): Boolean {
        val state = _uiState.value
        // Validate current step
        when (state.currentStep) {
            0 -> {
                var hasError = false
                if (state.title.isBlank()) {
                    _uiState.value = state.copy(titleError = "Job title is required")
                    hasError = true
                }
                if (state.category.isBlank()) {
                    _uiState.value = _uiState.value.copy(categoryError = "Select a category")
                    hasError = true
                }
                if (state.location.isBlank()) {
                    _uiState.value = _uiState.value.copy(locationError = "Select or type a location")
                    hasError = true
                }
                if (hasError) return false
            }
            1 -> {
                val pay = state.payAmount.toDoubleOrNull()
                if (pay == null || pay <= 0) {
                    _uiState.value = state.copy(payError = "Select or enter a pay amount")
                    return false
                }
            }
        }

        if (state.currentStep < 2) {
            _uiState.value = _uiState.value.copy(currentStep = state.currentStep + 1)
        }
        return true
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.currentStep > 0) {
            _uiState.value = state.copy(currentStep = state.currentStep - 1)
        }
    }

    // ─── Post Job ──────────────────────────────────────────────────────────

    fun postJob() {
        val state = _uiState.value

        // Final validation
        val pay = state.payAmount.toDoubleOrNull()
        if (state.title.isBlank() || pay == null || pay <= 0 || state.location.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in all required fields")
            return
        }
        if (pay > 1_000_000) {
            _uiState.value = state.copy(payError = "Pay cannot exceed ₹10,00,000")
            return
        }

        // Convert comma-separated skills to JSON array string
        val skillsList = state.skillsRequired
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        val skillsJson = if (skillsList.isNotEmpty()) {
            "[${skillsList.joinToString(",") { "\"$it\"" }}]"
        } else null

        val request = JobCreateRequest(
            title = state.title.trim(),
            description = state.description.trim().ifBlank { null },
            payAmount = pay,
            location = state.location.trim(),
            skillsRequired = skillsJson,
            isUrgent = state.isUrgent,
            companyName = state.companyName.trim().ifBlank { null },
            category = state.category.ifBlank { null },
            jobType = state.jobType,
            duration = state.duration.trim().ifBlank { null },
            maxApplicants = state.maxApplicants.toIntOrNull() ?: 1,
            contactInfo = state.contactInfo.trim().ifBlank { null },
            // Phase 1: Scheduling
            jobDate = state.jobDate.trim().ifBlank { null },
            startTime = state.startTime.trim().ifBlank { null },
            endTime = state.endTime.trim().ifBlank { null },
            address = state.address.trim().ifBlank { null }
        )

        _uiState.value = state.copy(isPosting = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = repository.createJob(request)) {
                is NetworkResult.Success -> {
                    Log.d("PostJob", "Job posted successfully: ${result.data.id}")
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        isSuccess = true,
                        postedJob = result.data
                    )
                }
                is NetworkResult.Error -> {
                    Log.e("PostJob", "Failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetForm() {
        _uiState.value = PostJobUiState(isLoggedIn = repository.isLoggedIn())
    }

    // ─── Login ─────────────────────────────────────────────────────────────

    fun showLoginSheet() {
        _uiState.value = _uiState.value.copy(showLoginSheet = true, loginError = null)
    }

    fun dismissLoginSheet() {
        _uiState.value = _uiState.value.copy(showLoginSheet = false, loginError = null)
    }

    fun onLoginSubmit(phone: String, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = authManager.performLogin(phone, name)) {
                is LoginResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoggingIn = false, showLoginSheet = false, isLoggedIn = true
                )
                is LoginResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoggingIn = false, loginError = result.message
                )
            }
        }
    }

    fun onGoogleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = authManager.performGoogleLogin(idToken)) {
                is LoginResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoggingIn = false, showLoginSheet = false, isLoggedIn = true
                )
                is LoginResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoggingIn = false, loginError = result.message
                )
            }
        }
    }

    fun onFirebaseLogin(idToken: String, name: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = authManager.performFirebaseLogin(idToken, name)) {
                is LoginResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoggingIn = false, showLoginSheet = false, isLoggedIn = true
                )
                is LoginResult.Failure -> _uiState.value = _uiState.value.copy(
                    isLoggingIn = false, loginError = result.message
                )
            }
        }
    }

    // ─── AI Intelligence Features ──────────────────────────────────────────

    fun generateDescription() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(aiDescriptionError = "Please enter a job title first")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingDescription = true,
                aiDescriptionError = null,
                aiDescriptionData = null
            )

            val request = AIGenerateDescriptionRequest(
                title = state.title.trim(),
                category = state.category.ifBlank { null },
                roughNotes = state.description.ifBlank { null },
                location = state.location.ifBlank { null },
                duration = state.duration.ifBlank { null }
            )

            when (val result = repository.generateDescription(request)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingDescription = false,
                        aiDescriptionData = result.data,
                        // Automatically apply the generated description
                        description = result.data.description
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingDescription = false,
                        aiDescriptionError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun applySuggestedSkillsAndCategory() {
        val aiData = _uiState.value.aiDescriptionData ?: return
        var state = _uiState.value
        if (!aiData.suggestedCategory.isNullOrBlank() && state.category.isBlank()) {
            state = state.copy(category = aiData.suggestedCategory, categoryError = null)
        }
        if (!aiData.suggestedSkills.isNullOrBlank() && state.skillsRequired.isBlank()) {
            state = state.copy(skillsRequired = aiData.suggestedSkills)
        }
        _uiState.value = state
    }

    fun dismissAiDescriptionData() {
        _uiState.value = _uiState.value.copy(aiDescriptionData = null, aiDescriptionError = null)
    }

    fun estimatePay() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isEstimatingPay = true,
                aiPayError = null,
                aiPayData = null
            )

            val request = AIPayEstimateRequest(
                category = state.category.ifBlank { null },
                location = state.location.ifBlank { null },
                duration = state.duration.ifBlank { null },
                jobType = state.jobType
            )

            when (val result = repository.estimatePay(request)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isEstimatingPay = false,
                        aiPayData = result.data
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isEstimatingPay = false,
                        aiPayError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun applyEstimatedPay() {
        val aiData = _uiState.value.aiPayData ?: return
        if (aiData.avgPay != null) {
            _uiState.value = _uiState.value.copy(
                payAmount = aiData.avgPay.toInt().toString(),
                payError = null
            )
        }
    }

    fun dismissAiPayData() {
        _uiState.value = _uiState.value.copy(aiPayData = null, aiPayError = null)
    }
}
