package com.studentgig.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.Job
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.studentgig.app.data.model.AIJobResponse
import com.studentgig.app.data.model.AIEarningsInsightsResponse

data class HomeUiState(
    val isServerOnline: Boolean = false,
    val isCheckingServer: Boolean = true,
    val jobs: List<Job> = emptyList(),
    val isLoadingJobs: Boolean = false,
    val errorMessage: String? = null,
    // AI Mode
    val feedMode: String = "latest", // "latest" or "foryou"
    val aiInsights: AIEarningsInsightsResponse? = null,
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

    /**
     * Re-check auth state when screen becomes visible.
     * If login state changed (user logged in/out from another tab),
     * refresh data accordingly.
     */
    fun refreshAuthState() {
        val wasLoggedIn = _uiState.value.isLoggedIn
        val nowLoggedIn = repository.isLoggedIn()
        if (wasLoggedIn != nowLoggedIn) {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = nowLoggedIn,
                userName = repository.getUserName()
            )
            if (nowLoggedIn) {
                // User logged in from another tab — load applied jobs + insights
                viewModelScope.launch { loadAppliedJobIds() }
                loadInsights()
            } else {
                // User logged out from another tab — clear user-specific data
                _uiState.value = _uiState.value.copy(
                    appliedJobIds = emptySet(),
                    aiInsights = null,
                    feedMode = "latest"
                )
            }
        }
    }

    fun checkServerAndLoadJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingServer = true,
                isLoadingJobs = true,
                errorMessage = null
            )

            // Skip separate health check — try loading jobs directly.
            // If jobs load, server is obviously online. Much faster!
            when (val result = repository.getJobs(skip = 0, limit = 10)) {
                is NetworkResult.Success -> {
                    val data = result.data
                    Log.d("HomeViewModel", "Jobs loaded instantly — ${data.size} items")
                    _uiState.value = _uiState.value.copy(
                        isServerOnline = true,
                        isCheckingServer = false,
                        jobs = data,
                        isLoadingJobs = false,
                        errorMessage = null,
                        isLastPage = data.size < 10
                    )
                    // Fetch applied jobs in background (non-blocking)
                    if (repository.isLoggedIn()) {
                        launch { loadAppliedJobIds() }
                        loadInsights()
                    }
                }
                is NetworkResult.Error -> {
                    // One quick retry after 1 second
                    Log.w("HomeViewModel", "First load failed, retrying: ${result.message}")
                    delay(1000L)
                    when (val retry = repository.getJobs(skip = 0, limit = 10)) {
                        is NetworkResult.Success -> {
                            val data = retry.data
                            _uiState.value = _uiState.value.copy(
                                isServerOnline = true,
                                isCheckingServer = false,
                                jobs = data,
                                isLoadingJobs = false,
                                errorMessage = null,
                                isLastPage = data.size < 10
                            )
                            if (repository.isLoggedIn()) {
                                launch { loadAppliedJobIds() }
                                loadInsights()
                            }
                        }
                        is NetworkResult.Error -> {
                            Log.e("HomeViewModel", "Server offline: ${retry.message}")
                            _uiState.value = _uiState.value.copy(
                                isServerOnline = false,
                                isCheckingServer = false,
                                isLoadingJobs = false,
                                errorMessage = retry.message
                            )
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadInsights() {
        viewModelScope.launch {
            when (val result = repository.getEarningsInsights()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(aiInsights = result.data)
                }
                else -> { /* Ignore errors for insights */ }
            }
        }
    }

    fun setFeedMode(mode: String) {
        if (_uiState.value.feedMode == mode) return
        _uiState.value = _uiState.value.copy(feedMode = mode, jobs = emptyList())
        if (mode == "foryou" && _uiState.value.isLoggedIn) {
            loadAIFeed()
        } else {
            checkServerAndLoadJobs()
        }
    }

    private fun loadAIFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingJobs = true,
                errorMessage = null,
                page = 0,
                isLastPage = true // AI feed doesn't paginate initially
            )

            when (val result = repository.getAIFeed()) {
                is NetworkResult.Success -> {
                    val jobs = result.data.map { it.toJob() }
                    _uiState.value = _uiState.value.copy(
                        jobs = jobs,
                        isLoadingJobs = false
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingJobs = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun AIJobResponse.toJob(): Job {
        return Job(
            id = id, title = title, description = description, payAmount = payAmount,
            location = location, skillsRequired = skillsRequired, isUrgent = isUrgent,
            createdAt = createdAt, matchScore = matchScore, employerId = employerId,
            maxApplicants = maxApplicants, companyName = companyName, category = category,
            jobType = jobType, duration = duration, status = status, contactInfo = contactInfo,
            employerName = employerName, applicantCount = applicantCount, jobDate = jobDate,
            startTime = startTime, endTime = endTime, address = address,
            aiScore = aiScore, aiReason = aiReason, aiBreakdown = aiBreakdown
        )
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
                    if (result.code == 401) {
                        // Token expired — clear stale token & show login sheet
                        repository.logout()
                        _uiState.value = _uiState.value.copy(
                            isApplying = false,
                            isLoggedIn = false,
                            showLoginSheet = true,
                            pendingApplyJobId = jobId,
                            applyMessage = null
                        )
                    } else {
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
                    val pendingJob = _uiState.value.pendingApplyJobId
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userName = result.data.user.name,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null,
                        pendingApplyJobId = null
                    )
                    // Reload jobs + applied IDs in parallel (fast!)
                    launch { loadJobs() }
                    launch { loadAppliedJobIds() }
                    // Apply to pending job if any
                    if (pendingJob != null) {
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
                    val pendingJob = _uiState.value.pendingApplyJobId
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userName = result.data.user.name,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null,
                        pendingApplyJobId = null
                    )
                    // Reload jobs + applied IDs in parallel (fast!)
                    launch { loadJobs() }
                    launch { loadAppliedJobIds() }
                    if (pendingJob != null) {
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

    fun onFirebaseLogin(idToken: String, name: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)

            when (val result = repository.firebaseLogin(idToken, name)) {
                is NetworkResult.Success -> {
                    val pendingJob = _uiState.value.pendingApplyJobId
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userName = result.data.user.name,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null,
                        pendingApplyJobId = null
                    )
                    // Reload jobs + applied IDs in parallel (fast!)
                    launch { loadJobs() }
                    launch { loadAppliedJobIds() }
                    if (pendingJob != null) {
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
        // Full state reset — prevent stale data from previous user
        _uiState.value = HomeUiState(
            isServerOnline = _uiState.value.isServerOnline,
            isCheckingServer = false,
            jobs = _uiState.value.jobs,  // Keep job list (public data)
            isLoggedIn = false,
            userName = null,
            appliedJobIds = emptySet()
        )
        // Reload jobs to clear match scores
        _uiState.value = _uiState.value.copy(feedMode = "latest")
        checkServerAndLoadJobs()
    }

    fun retry() {
        if (_uiState.value.feedMode == "foryou") {
            loadAIFeed()
        } else {
            checkServerAndLoadJobs()
        }
    }
}
