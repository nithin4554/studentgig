package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.ApplicationDetailResponse
import com.studentgig.app.data.model.ApplicationStatusUpdate
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.studentgig.app.data.model.AIApplicantResponse

data class EmployerUiState(
    val myJobs: List<com.studentgig.app.data.model.Job> = emptyList(),
    val applications: List<ApplicationDetailResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    // Action states
    val actionLoading: Int? = null,
    val actionMessage: String? = null,
    val actionSuccess: Boolean = false,
    // Filter
    val selectedFilter: String = "all",
    
    // AI Rankings (ApplicationId -> AIApplicantResponse)
    val aiRankings: Map<Int, AIApplicantResponse> = emptyMap(),
    val isLoadingRanks: Set<Int> = emptySet() // Job IDs loading ranks
)

@HiltViewModel
class EmployerViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployerUiState())
    val uiState: StateFlow<EmployerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun getUserId(): Int = repository.getUserId()

    fun refresh() {
        val loggedIn = repository.isLoggedIn()
        _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
        if (loggedIn) {
            loadEmployerApplications()
        } else {
            _uiState.value = _uiState.value.copy(applications = emptyList())
        }
    }

    fun loadEmployerApplications() {
        if (!repository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                applications = emptyList(),
                isLoggedIn = false
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val jobsResult = repository.getMyJobs()
            val myJobs = (jobsResult as? NetworkResult.Success)?.data ?: emptyList()
            
            when (val result = repository.getEmployerApplications()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        myJobs = myJobs,
                        applications = result.data,
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    if (result.code == 401) {
                        repository.logout()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            myJobs = emptyList(),
                            applications = emptyList(),
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            myJobs = myJobs,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // ─── Employer Actions ────────────────────────────────────────────────────

    /** Accept an application */
    fun acceptApplication(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.updateApplicationStatus(appId, ApplicationStatusUpdate("accepted"))) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = "🤝 Application accepted!",
                        actionSuccess = true
                    )
                    loadEmployerApplications()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = result.message,
                        actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /** Reject an application */
    fun rejectApplication(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.updateApplicationStatus(appId, ApplicationStatusUpdate("rejected"))) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = "Application rejected",
                        actionSuccess = true
                    )
                    loadEmployerApplications()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = result.message,
                        actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /** Phase 2: Confirm student arrived → start work */
    fun confirmArrival(appId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = appId, actionMessage = null)
            when (val result = repository.startWork(appId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = "🚀 Arrival confirmed! Work started.",
                        actionSuccess = true
                    )
                    loadEmployerApplications()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = result.message,
                        actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /** Phase 2: Confirm work quality → completion */
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
                    loadEmployerApplications()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = result.message,
                        actionSuccess = false
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun dismissActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredApplications(): List<ApplicationDetailResponse> {
        val state = _uiState.value
        return if (state.selectedFilter == "all") {
            state.applications
        } else {
            state.applications.filter { it.status.lowercase() == state.selectedFilter }
        }
    }

    // ─── AI Intelligence ─────────────────────────────────────────────────────

    fun loadAIRankingsForJob(jobId: Int) {
        if (_uiState.value.isLoadingRanks.contains(jobId)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingRanks = _uiState.value.isLoadingRanks + jobId
            )
            when (val result = repository.getAIRankedApplicants(jobId)) {
                is NetworkResult.Success -> {
                    val newRanks = result.data.associateBy { it.applicationId }
                    _uiState.value = _uiState.value.copy(
                        aiRankings = _uiState.value.aiRankings + newRanks,
                        isLoadingRanks = _uiState.value.isLoadingRanks - jobId
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingRanks = _uiState.value.isLoadingRanks - jobId
                    )
                }
            }
        }
    }
}
