package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.Job
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isServerOnline: Boolean = false,
    val isCheckingServer: Boolean = true,
    val jobs: List<Job> = emptyList(),
    val isLoadingJobs: Boolean = false,
    val errorMessage: String? = null,
    // Pagination tracking
    val page: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    // Auth state
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val showLoginSheet: Boolean = false,
    val pendingApplyJobId: Int? = null,
    // Apply state
    val applyMessage: String? = null,
    val isApplying: Boolean = false,
    val appliedJobIds: Set<Int> = emptySet(), // Track which jobs user applied to
    // Login state
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = repository.isLoggedIn(),
            userName = repository.getUserName()
        )
        checkServerAndLoadJobs()
    }

    fun checkServerAndLoadJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingServer = true, errorMessage = null)

            when (val statusResult = repository.checkServerStatus()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isServerOnline = true,
                        isCheckingServer = false
                    )
                    loadJobs()
                    // Also fetch applied jobs if logged in
                    if (repository.isLoggedIn()) {
                        loadAppliedJobIds()
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isServerOnline = false,
                        isCheckingServer = false,
                        errorMessage = statusResult.message
                    )
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }

    private suspend fun loadJobs() {
        _uiState.value = _uiState.value.copy(
            isLoadingJobs = true,
            page = 0,
            isLastPage = false
        )

        when (val result = repository.getJobs(skip = 0, limit = 10)) {
            is NetworkResult.Success -> {
                val data = result.data
                _uiState.value = _uiState.value.copy(
                    jobs = data,
                    isLoadingJobs = false,
                    errorMessage = null,
                    isLastPage = data.size < 10
                )
            }
            is NetworkResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoadingJobs = false,
                    errorMessage = result.message
                )
            }
            is NetworkResult.Loading -> { /* no-op */ }
        }
    }

    fun loadMoreJobs() {
        val state = _uiState.value
        if (state.isLoadingJobs || state.isLoadingMore || state.isLastPage) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.page + 1
            val skip = nextPage * 10

            when (val result = repository.getJobs(skip = skip, limit = 10)) {
                is NetworkResult.Success -> {
                    val newData = result.data
                    _uiState.value = _uiState.value.copy(
                        jobs = state.jobs + newData,
                        isLoadingMore = false,
                        page = nextPage,
                        isLastPage = newData.size < 10
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        // Optionally show an error toast here
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Fetches the list of job IDs the user has already applied to.
     */
    private suspend fun loadAppliedJobIds() {
        when (val result = repository.getMyApplications()) {
            is NetworkResult.Success -> {
                val ids = result.data.map { it.jobId }.toSet()
                _uiState.value = _uiState.value.copy(appliedJobIds = ids)
            }
            is NetworkResult.Error -> { /* silently fail — non-critical */ }
            is NetworkResult.Loading -> {}
        }
    }

    // ─── GATEKEEPER: Apply button logic ─────────────────────────────────────

    fun onApplyClicked(jobId: Int) {
        // Prevent double-apply
        if (jobId in _uiState.value.appliedJobIds) return

        if (repository.isLoggedIn()) {
            applyToJob(jobId)
        } else {
            _uiState.value = _uiState.value.copy(
                showLoginSheet = true,
                pendingApplyJobId = jobId
            )
        }
    }

    private fun applyToJob(jobId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, applyMessage = null)

            when (val result = repository.applyToJob(jobId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        applyMessage = "✅ Applied successfully!",
                        appliedJobIds = _uiState.value.appliedJobIds + jobId
                    )
                }
                is NetworkResult.Error -> {
                    // If the error says "already applied", mark it as applied
                    val alreadyApplied = result.message?.contains("already", ignoreCase = true) == true
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        applyMessage = result.message,
                        appliedJobIds = if (alreadyApplied)
                            _uiState.value.appliedJobIds + jobId
                        else _uiState.value.appliedJobIds
                    )
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }

    // ─── LOGIN from bottom sheet ────────────────────────────────────────────

    fun onLoginSubmit(phone: String, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)

            when (val result = repository.login(phone, name)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userName = result.data.user.name,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null
                    )
                    // Reload jobs to get match scores
                    loadJobs()
                    // Fetch applied jobs
                    loadAppliedJobIds()
                    // Apply to pending job if any
                    val pendingJob = _uiState.value.pendingApplyJobId
                    if (pendingJob != null) {
                        _uiState.value = _uiState.value.copy(pendingApplyJobId = null)
                        applyToJob(pendingJob)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
                is NetworkResult.Loading -> { /* no-op */ }
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
                        userName = result.data.user.name,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null
                    )
                    loadJobs()
                    loadAppliedJobIds()
                    val pendingJob = _uiState.value.pendingApplyJobId
                    if (pendingJob != null) {
                        _uiState.value = _uiState.value.copy(pendingApplyJobId = null)
                        applyToJob(pendingJob)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
                is NetworkResult.Loading -> { /* no-op */ }
            }
        }
    }

    fun dismissLoginSheet() {
        _uiState.value = _uiState.value.copy(
            showLoginSheet = false,
            pendingApplyJobId = null,
            loginError = null
        )
    }

    fun dismissApplyMessage() {
        _uiState.value = _uiState.value.copy(applyMessage = null)
    }

    fun logout() {
        repository.logout()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            userName = null,
            appliedJobIds = emptySet()
        )
        // Reload jobs to clear match scores
        viewModelScope.launch { loadJobs() }
    }

    fun retry() {
        checkServerAndLoadJobs()
    }
}
