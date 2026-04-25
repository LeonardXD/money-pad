package com.example.moneypad.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignupUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class SignupViewModel(private val repository: MoneyPadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }

    fun signup() {
        val state = uiState.value
        if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "All fields are required")
            return
        }
        if (!state.username.matches(Regex("^[a-z].*"))) {
            _uiState.value = _uiState.value.copy(error = "Username must start with a lowercase letter")
            return
        }
        if (state.password.length !in 8..16) {
            _uiState.value = _uiState.value.copy(error = "Password must be 8-16 characters long")
            return
        }
        if (!state.password.any { it.isUpperCase() } ||
            !state.password.any { it.isLowerCase() } ||
            !state.password.any { it.isDigit() } ||
            !state.password.any { !it.isLetterOrDigit() }) {
            _uiState.value = _uiState.value.copy(error = "Password must contain uppercase, lowercase, number, and symbol")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            val result = repository.signup(state.username, state.email, state.password)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = result.exceptionOrNull()?.message ?: "Signup failed"
                )
            }
        }
    }
}
