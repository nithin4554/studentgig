package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.NotificationItem
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun refresh() {
        val loggedIn = repository.isLoggedIn()
        if (loggedIn) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
            loadNotifications()
            fetchUnreadCount()
        } else {
            // Full reset — clear stale data from previous user
            _uiState.value = NotificationsUiState(isLoggedIn = false)
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getNotifications()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        notifications = result.data,
                        isLoading = false
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

    fun fetchUnreadCount() {
        viewModelScope.launch {
            when (val result = repository.getUnreadCount()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        unreadCount = result.data.unreadCount
                    )
                }
                else -> {}
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (repository.markNotificationsRead()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        unreadCount = 0,
                        notifications = _uiState.value.notifications.map {
                            it.copy(isRead = true)
                        }
                    )
                }
                else -> {}
            }
        }
    }
}
