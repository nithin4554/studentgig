package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.ApplicationDetailResponse
import com.studentgig.app.data.model.ApplicationStatusUpdate
import com.studentgig.app.data.model.EarningsResponse
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ActivityRole { STUDENT, POSTER }

data class ApplicationsUiState(
    // Student data
    val applications: List<ApplicationDetailResponse> = emptyList(),
    // Employer data
    val employerApplications: List<ApplicationDetailResponse> = emptyList(),
    // Common state
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    // Earnings
    val earnings: EarningsResponse? = null,
    val isLoadingEarnings: Boolean = false,
    // Action states
    val actionLoading: Int? = null,
    val actionMessage: String? = null,
    val actionSuccess: Boolean = false,
    // Filter
    val selectedFilter: String = "all",
    // Role-aware Activity tab
    val activeRole: ActivityRole = ActivityRole.STUDENT,
    val hasStudentData: Boolean = false,
    val hasPosterData: Boolean = false,
    // Login from Activity tab
    val showLoginSheet: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null
)

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationsUiState())
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun getUserId(): Int = repository.getUserId()

    /**
     * Called every time the screen becomes visible.
     * Loads BOTH student and employer data in parallel to detect roles.
     */
    fun refresh() {
        val loggedIn = repository.isLoggedIn()
        _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
        if (loggedIn) {
            loadAllData()
        } else {
            _uiState.value = _uiState.value.copy(
                applications = emptyList(),
                employerApplications = emptyList(),
                earnings = null,
                hasStudentData = false,
                hasPosterData = false
            )
        }
    }

    /**
     * Load student apps, employer apps, and earnings in parallel.
     * This is the key to role detection — we check which data exists.
     */
    private fun loadAllData() {
        if (!repository.isLoggedIn()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Fire all requests in parallel
            val studentAppsDeferred = async { repository.getMyApplications() }
            val employerAppsDeferred = async { repository.getEmployerApplications() }
            val earningsDeferred = async { repository.getEarnings() }

            // Collect results
            val studentResult = studentAppsDeferred.await()
            val employerResult = employerAppsDeferred.await()
            val earningsResult = earningsDeferred.await()

            // Process student applications
            val studentApps = when (studentResult) {
                is NetworkResult.Success -> studentResult.data
                is NetworkResult.Error -> {
                    if (studentResult.code == 401) {
                        repository.logout()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, isLoggedIn = false,
                            applications = emptyList(), errorMessage = null
                        )
                        return@launch
                    }
                    emptyList()
                }
                is NetworkResult.Loading -> emptyList()
            }

            // Process employer applications
            val empApps = when (employerResult) {
                is NetworkResult.Success -> employerResult.data
                is NetworkResult.Error -> emptyList()
                is NetworkResult.Loading -> emptyList()
            }

            // Process earnings
            val earnings = when (earningsResult) {
                is NetworkResult.Success -> earningsResult.data
                else -> null
            }

            // Detect roles based on data
            val hasStudent = studentApps.isNotEmpty()
            val hasPoster = empApps.isNotEmpty()

            // Auto-select role: if user has only poster data, show poster view
            val currentRole = _uiState.value.activeRole
            val autoRole = when {
                hasStudent && hasPoster -> currentRole  // Keep user's choice
                hasPoster && !hasStudent -> ActivityRole.POSTER
                else -> ActivityRole.STUDENT  // Default to student
            }

            _uiState.value = _uiState.value.copy(
                applications = studentApps,
                employerApplications = empApps,
                isLoading = false,
                earnings = earnings,
                hasStudentData = hasStudent,
                hasPosterData = hasPoster,
                activeRole = autoRole
            )
        }
    }

    fun loadApplications() {
        if (!repository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false, applications = emptyList(), isLoggedIn = false
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getMyApplications()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        applications = result.data,
                        isLoading = false,
                        hasStudentData = result.data.isNotEmpty()
                    )
                }
                is NetworkResult.Error -> {
                    if (result.code == 401) {
                        repository.logout()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, isLoggedIn = false,
                            applications = emptyList(), errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadEmployerApplications() {
        if (!repository.isLoggedIn()) return
        viewModelScope.launch {
            when (val result = repository.getEmployerApplications()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        employerApplications = result.data,
                        hasPosterData = result.data.isNotEmpty()
                    )
                }
                is NetworkResult.Error -> {}
                is NetworkResult.Loading -> {}
            }
        }
    }

    // ─── Role Toggle ─────────────────────────────────────────────────────────

    fun setActiveRole(role: ActivityRole) {
        _uiState.value = _uiState.value.copy(activeRole = role, selectedFilter = "all")
    }

    // ─── Student Lifecycle Actions (Phase 2) ─────────────────────────────────

    fun checkIn(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.checkIn(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "📍 Checked in! On your way.",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun startWork(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.startWork(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "🚀 Arrival confirmed! Work started.",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun completeWork(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.completeWork(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = "✅ Work marked done! Awaiting employer confirmation.",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun confirmCompletion(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.confirmCompletion(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = "✅ Work confirmed! Payment authorized.",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun confirmPayment(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.confirmPayment(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "💰 Payment confirmed! Earnings updated.",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // ─── Employer Actions ────────────────────────────────────────────────────

    fun acceptApplication(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.updateApplicationStatus(appId, ApplicationStatusUpdate("accepted"))) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "🤝 Application accepted!",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun rejectApplication(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.updateApplicationStatus(appId, ApplicationStatusUpdate("rejected"))) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "Application rejected",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun confirmArrival(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.startWork(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "🚀 Arrival confirmed! Work started.",
                        actionSuccess = true
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // ─── Phase 4: Ratings ────────────────────────────────────────────────────

    fun rateApplication(appId: Int, ratedUserId: Int, score: Int, review: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            val request = com.studentgig.app.data.model.RatingCreate(ratedUserId, score, review)
            when (val result = repository.rateApplication(appId, request)) {
                is com.studentgig.app.data.repository.NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = "⭐ Rating submitted successfully!",
                        actionSuccess = true
                    )
                    // Optionally, reload data to reflect changes (e.g. if we show ratings later)
                    loadAllData()
                }
                is com.studentgig.app.data.repository.NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null, actionMessage = result.message, actionSuccess = false
                    )
                }
                is com.studentgig.app.data.repository.NetworkResult.Loading -> {}
            }
        }
    }

    // ─── Common ──────────────────────────────────────────────────────────────

    fun dismissActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    // ─── Login from Activity tab ────────────────────────────────────────────────

    fun showLogin() {
        _uiState.value = _uiState.value.copy(showLoginSheet = true, loginError = null)
    }

    fun dismissLoginSheet() {
        _uiState.value = _uiState.value.copy(showLoginSheet = false, loginError = null)
    }

    fun onLoginSubmit(phone: String, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = repository.login(phone, name)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onGoogleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = repository.googleLogin(idToken)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onFirebaseLogin(idToken: String, name: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = repository.firebaseLogin(idToken, name)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null
                    )
                    loadAllData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun getFilteredApplications(): List<ApplicationDetailResponse> {
        val state = _uiState.value
        val apps = if (state.activeRole == ActivityRole.STUDENT)
            state.applications else state.employerApplications
        return if (state.selectedFilter == "all") apps
        else apps.filter { it.status.lowercase() == state.selectedFilter }
    }
}
