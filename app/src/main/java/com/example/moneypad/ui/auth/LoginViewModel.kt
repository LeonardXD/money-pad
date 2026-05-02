package com.example.moneypad.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val requiresOnboarding: Boolean = false
)

class LoginViewModel(private val repository: MoneyPadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        val trimmedUsername = uiState.value.username.trim()
        val trimmedPassword = uiState.value.password.trim()

        if (trimmedUsername.isBlank() || trimmedPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Fields cannot be empty")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            val result = repository.login(trimmedUsername, trimmedPassword)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isSuccess = true,
                    requiresOnboarding = user?.onboardingCompleted == false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }
}
