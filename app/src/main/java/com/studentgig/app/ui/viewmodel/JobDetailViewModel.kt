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
    val pendingApplyJobId: Int? = null
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
                        applyMessage = "Gig Applied successfully 🎉",
                        appliedJobIds = _uiState.value.appliedJobIds + id
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        applyMessage = result.message
                    )
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

    fun onLoginSubmit(phone: String, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            when (val result = repository.login(phone, name)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        showLoginSheet = false,
                        loginError = null
                    )
                    // After login, fetch their application state
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
                is NetworkResult.Loading -> {}
            }
        }
    }
}
