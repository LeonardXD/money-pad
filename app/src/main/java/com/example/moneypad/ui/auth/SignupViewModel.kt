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
    val referrerUsername: String = "",
    val isUsernameTaken: Boolean = false,
    val isEmailTaken: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class SignupViewModel(private val repository: MoneyPadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private var usernameJob: kotlinx.coroutines.Job? = null
    private var emailJob: kotlinx.coroutines.Job? = null

    fun onUsernameChange(v: String) {
        update { copy(username = v, isUsernameTaken = false) }
        usernameJob?.cancel()
        val trimmed = v.trim()
        if (trimmed.isNotBlank()) {
            usernameJob = viewModelScope.launch {
                val taken = repository.isUsernameTaken(trimmed)
                update { copy(isUsernameTaken = taken) }
            }
        }
    }

    fun onEmailChange(v: String) {
        update { copy(email = v, isEmailTaken = false) }
        emailJob?.cancel()
        val trimmed = v.trim()
        if (trimmed.isNotBlank()) {
            emailJob = viewModelScope.launch {
                val taken = repository.isEmailTaken(trimmed)
                update { copy(isEmailTaken = taken) }
            }
        }
    }

    fun onPasswordChange(v: String) = update { copy(password = v) }
    fun onConfirmPasswordChange(v: String) = update { copy(confirmPassword = v) }
    fun onReferrerUsernameChange(v: String) = update { copy(referrerUsername = v) }

    fun signup() {
        val s = uiState.value
        val trimmedUsername = s.username.trim()
        val trimmedEmail = s.email.trim()
        val trimmedPassword = s.password.trim()
        val trimmedConfirm = s.confirmPassword.trim()

        when {
            trimmedUsername.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank() ->
                return update { copy(error = "All required fields must be filled") }
            !trimmedUsername.matches(Regex("^[a-z].*")) ->
                return update { copy(error = "Username must start with a lowercase letter") }
            trimmedPassword.length !in 8..16 ->
                return update { copy(error = "Password must be 8–16 characters") }
            !trimmedPassword.any { it.isUpperCase() } || !trimmedPassword.any { it.isLowerCase() } ||
                    !trimmedPassword.any { it.isDigit() } || !trimmedPassword.any { !it.isLetterOrDigit() } ->
                return update { copy(error = "Password needs uppercase, lowercase, number & symbol") }
            trimmedPassword != trimmedConfirm ->
                return update { copy(error = "Passwords do not match") }
        }

        update { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.signup(trimmedUsername, trimmedEmail, trimmedPassword, s.referrerUsername.trim())
            if (result.isSuccess) {
                update { copy(isLoading = false, isSuccess = true) }
            } else {
                update { copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Signup failed") }
            }
        }
    }

    private fun update(block: SignupUiState.() -> SignupUiState) {
        _uiState.value = _uiState.value.block()
    }
}