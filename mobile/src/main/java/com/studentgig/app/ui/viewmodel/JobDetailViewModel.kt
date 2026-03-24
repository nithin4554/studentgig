package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import com.studentgig.app.data.model.AIMatchExplanationResponse

data class JobDetailUiState(
    val job: Job? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val appliedJobIds: Set<Int> = emptySet(),
    val applyMessage: String? = null,
    val isApplying: Boolean = false,
    val showLoginSheet: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val pendingApplyJobId: Int? = null,
    
    // AI Features
    val matchExplanation: AIMatchExplanationResponse? = null,
    val isLoadingMatch: Boolean = false,
    val generatedNote: String? = null,
    val isGeneratingNote: Boolean = false
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val repository: JobRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    private val jobId: Int = checkNotNull(savedStateHandle["jobId"]) { "jobId is required" }

    init {
        loadJobDetails()
        if (repository.isLoggedIn()) {
            loadAppliedJobIds()
        }
    }

    private fun loadJobDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.getJob(jobId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        job = result.data,
                        isLoading = false,
                        errorMessage = null
                    )
                    if (repository.isLoggedIn()) {
                        loadMatchExplanation()
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadAppliedJobIds() {
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

    fun onApplyClicked(id: Int) {
        if (!repository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                showLoginSheet = true,
                pendingApplyJobId = id
            )
            return
        }
        applyToJob(id)
    }

    private fun applyToJob(id: Int) {
        if (_uiState.value.isApplying) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, applyMessage = null)
            
            when (val result = repository.applyToJob(id)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        applyMessage = "Job Applied successfully 🎉",
                        appliedJobIds = _uiState.value.appliedJobIds + id
                    )
                }
                is NetworkResult.Error -> {
                    if (result.code == 401) {
                        // Token expired — clear stale token & show login sheet
                        repository.logout()
                        _uiState.value = _uiState.value.copy(
                            isApplying = false,
                            showLoginSheet = true,
                            pendingApplyJobId = id,
                            applyMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isApplying = false,
                            applyMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun dismissApplyMessage() {
        _uiState.value = _uiState.value.copy(applyMessage = null)
    }

    fun dismissLoginSheet() {
        _uiState.value = _uiState.value.copy(
            showLoginSheet = false,
            loginError = null,
            pendingApplyJobId = null
        )
    }




    private fun loadMatchExplanation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMatch = true)
            when (val result = repository.getMatchExplanation(jobId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMatch = false,
                        matchExplanation = result.data
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoadingMatch = false)
                }
            }
        }
    }

    fun generateApplicationNote() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingNote = true)
            when (val result = repository.generateApplicationNote(jobId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingNote = false,
                        generatedNote = result.data.note
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isGeneratingNote = false)
                }
            }
        }
    }
}
