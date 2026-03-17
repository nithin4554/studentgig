package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.Job
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.studentgig.app.data.model.AIJobResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val location: String = "",
    val urgentOnly: Boolean = false,
    val minPay: Double? = null,
    val maxPay: Double? = null,
    val dateFilter: String = "", // "today", "tomorrow", "this_week", or "" for any
    val results: List<Job> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    // AI Search
    val useAiSearch: Boolean = false,
    val aiInterpretation: String? = null,
    val aiDidParse: Boolean = false,
    // Pagination tracking
    val page: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    // Apply state
    val showLoginSheet: Boolean = false,
    val pendingApplyJobId: Int? = null,
    val isApplying: Boolean = false,
    val applyMessage: String? = null,
    val appliedJobIds: Set<Int> = emptySet(),
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: CoroutineJob? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        debounceSearch()
    }

    fun onLocationChanged(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun onUrgentToggle(urgent: Boolean) {
        _uiState.value = _uiState.value.copy(urgentOnly = urgent)
        search()
    }

    fun onPayRangeChanged(min: Double?, max: Double?) {
        _uiState.value = _uiState.value.copy(minPay = min, maxPay = max)
    }

    fun onDateFilterChanged(filter: String) {
        _uiState.value = _uiState.value.copy(dateFilter = filter)
        search()
    }

    fun onAiToggle(useAi: Boolean) {
        _uiState.value = _uiState.value.copy(useAiSearch = useAi)
        if (_uiState.value.query.isNotBlank()) search()
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250) // Quick but still prevents spam
            search()
        }
    }

    fun search() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(
                isSearching = true, 
                errorMessage = null,
                page = 0,
                isLastPage = false
            )

            val q = state.query.ifBlank { null }
            val loc = state.location.ifBlank { null }

            if (state.useAiSearch && q != null) {
                // ─── AI SMART SEARCH ───
                when (val result = repository.smartSearch(q)) {
                    is NetworkResult.Success -> {
                        val aiData = result.data
                        val jobs = filterByDate(aiData.jobs, _uiState.value.dateFilter)
                        _uiState.value = _uiState.value.copy(
                            results = jobs,
                            isSearching = false,
                            hasSearched = true,
                            isLastPage = true, // AI search returns top matches, no pagination
                            aiInterpretation = aiData.interpretation,
                            aiDidParse = aiData.aiParsed
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            errorMessage = result.message,
                            hasSearched = true
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } else {
                // ─── STANDARD SEARCH ───
                _uiState.value = _uiState.value.copy(aiInterpretation = null, aiDidParse = false)
                when (val result = repository.searchJobs(
                    query = q,
                    location = loc,
                    minPay = state.minPay,
                    maxPay = state.maxPay,
                    urgentOnly = state.urgentOnly,
                    skip = 0,
                    limit = 10
                )) {
                    is NetworkResult.Success -> {
                        val data = filterByDate(result.data, _uiState.value.dateFilter)
                        _uiState.value = _uiState.value.copy(
                            results = data,
                            isSearching = false,
                            hasSearched = true,
                            isLastPage = result.data.size < 10
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            errorMessage = result.message,
                            hasSearched = true
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            }
        }
    }

    fun loadMoreSearch() {
        val state = _uiState.value
        if (state.isSearching || state.isLoadingMore || state.isLastPage || !state.hasSearched) return
        if (state.useAiSearch) return // AI search doesn't paginate

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.page + 1
            val skip = nextPage * 10
            
            val q = state.query.ifBlank { null }
            val loc = state.location.ifBlank { null }

            when (val result = repository.searchJobs(
                query = q,
                location = loc,
                minPay = state.minPay,
                maxPay = state.maxPay,
                urgentOnly = state.urgentOnly,
                skip = skip,
                limit = 10
            )) {
                is NetworkResult.Success -> {
                    val newData = result.data
                    _uiState.value = _uiState.value.copy(
                        results = state.results + newData,
                        isLoadingMore = false,
                        page = nextPage,
                        isLastPage = newData.size < 10
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // ─── Apply / Login flow (same pattern as HomeViewModel) ─────────────

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
                        applyMessage = "\u2705 Applied successfully!",
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
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onLoginSubmit(phone: String, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = repository.login(phone, name)) {
                is NetworkResult.Success -> {
                    val pendingJob = _uiState.value.pendingApplyJobId
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null,
                        pendingApplyJobId = null
                    )
                    // Fire in parallel for speed
                    launch { fetchAppliedJobs() }
                    launch { search() }
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
                is NetworkResult.Loading -> {}
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
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null,
                        pendingApplyJobId = null
                    )
                    launch { fetchAppliedJobs() }
                    launch { search() }
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
                is NetworkResult.Loading -> {}
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
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null,
                        pendingApplyJobId = null
                    )
                    launch { fetchAppliedJobs() }
                    launch { search() }
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
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun fetchAppliedJobs() {
        viewModelScope.launch {
            when (val result = repository.getMyApplications()) {
                is NetworkResult.Success -> {
                    val ids = result.data.map { it.jobId }.toSet()
                    _uiState.value = _uiState.value.copy(appliedJobIds = ids)
                }
                else -> {}
            }
        }
    }

    fun dismissLoginSheet() {
        _uiState.value = _uiState.value.copy(
            showLoginSheet = false, pendingApplyJobId = null, loginError = null
        )
    }

    fun dismissApplyMessage() {
        _uiState.value = _uiState.value.copy(applyMessage = null)
    }

    /**
     * Phase 1/5: Client-side date filter for search results.
     * Compares job's jobDate (YYYY-MM-DD) against today/tomorrow/this_week.
     */
    private fun filterByDate(jobs: List<Job>, filter: String): List<Job> {
        if (filter.isBlank()) return jobs
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return jobs.filter { job ->
            val dateStr = job.jobDate ?: return@filter false
            try {
                val jobDate = LocalDate.parse(dateStr, fmt)
                when (filter) {
                    "today" -> jobDate == today
                    "tomorrow" -> jobDate == today.plusDays(1)
                    "this_week" -> !jobDate.isBefore(today) && !jobDate.isAfter(today.plusDays(6))
                    else -> true
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
