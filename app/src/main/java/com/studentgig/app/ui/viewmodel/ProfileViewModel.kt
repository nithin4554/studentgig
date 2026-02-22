package com.studentgig.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentgig.app.data.model.UserInfo
import com.studentgig.app.data.repository.JobRepository
import com.studentgig.app.data.repository.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val user: UserInfo? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Edit state
    val isEditing: Boolean = false,
    val editName: String = "",
    val editSkills: List<String> = emptyList(),
    val newSkillText: String = "",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Called every time the screen becomes visible.
     * Re-checks login state and refreshes data if needed.
     */
    fun refresh() {
        val loggedIn = repository.isLoggedIn()
        _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
        if (loggedIn) {
            loadProfile()
        } else {
            _uiState.value = ProfileUiState(isLoggedIn = false)
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getProfile()) {
                is NetworkResult.Success -> {
                    val user = result.data
                    val skills = parseSkills(user.skillsJson)
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false,
                        isLoggedIn = true,
                        editName = user.name,
                        editSkills = skills
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

    fun startEditing() {
        val user = _uiState.value.user ?: return
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editName = user.name,
            editSkills = parseSkills(user.skillsJson),
            newSkillText = ""
        )
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(isEditing = false)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(editName = name)
    }

    fun onNewSkillTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(newSkillText = text)
    }

    fun addSkill() {
        val skill = _uiState.value.newSkillText.trim().lowercase()
        if (skill.isNotBlank() && skill !in _uiState.value.editSkills) {
            _uiState.value = _uiState.value.copy(
                editSkills = _uiState.value.editSkills + skill,
                newSkillText = ""
            )
        }
    }

    fun removeSkill(skill: String) {
        _uiState.value = _uiState.value.copy(
            editSkills = _uiState.value.editSkills - skill
        )
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            val skillsJson = if (state.editSkills.isNotEmpty()) {
                "[${state.editSkills.joinToString(",") { "\"$it\"" }}]"
            } else null

            when (val result = repository.updateProfile(state.editName, skillsJson)) {
                is NetworkResult.Success -> {
                    val user = result.data
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isSaving = false,
                        isEditing = false,
                        successMessage = "Profile updated!",
                        editSkills = parseSkills(user.skillsJson)
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = ProfileUiState(isLoggedIn = false)
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    private fun parseSkills(skillsJson: String?): List<String> {
        if (skillsJson.isNullOrBlank()) return emptyList()
        return try {
            val cleaned = skillsJson.trim()
                .removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
            cleaned
        } catch (_: Exception) {
            emptyList()
        }
    }
}
