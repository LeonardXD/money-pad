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
        if (v.isNotBlank()) {
            usernameJob = viewModelScope.launch {
                val taken = repository.isUsernameTaken(v)
                update { copy(isUsernameTaken = taken) }
            }
        }
    }

    fun onEmailChange(v: String) {
        update { copy(email = v, isEmailTaken = false) }
        emailJob?.cancel()
        if (v.isNotBlank()) {
            emailJob = viewModelScope.launch {
                val taken = repository.isEmailTaken(v)
                update { copy(isEmailTaken = taken) }
            }
        }
    }

    fun onPasswordChange(v: String) = update { copy(password = v) }
    fun onConfirmPasswordChange(v: String) = update { copy(confirmPassword = v) }
    fun onReferrerUsernameChange(v: String) = update { copy(referrerUsername = v) }

    fun signup() {
        val s = uiState.value
        when {
            s.username.isBlank() || s.email.isBlank() || s.password.isBlank() ->
                return update { copy(error = "All required fields must be filled") }
            !s.username.matches(Regex("^[a-z].*")) ->
                return update { copy(error = "Username must start with a lowercase letter") }
            s.password.length !in 8..16 ->
                return update { copy(error = "Password must be 8–16 characters") }
            !s.password.any { it.isUpperCase() } || !s.password.any { it.isLowerCase() } ||
                    !s.password.any { it.isDigit() } || !s.password.any { !it.isLetterOrDigit() } ->
                return update { copy(error = "Password needs uppercase, lowercase, number & symbol") }
            s.password != s.confirmPassword ->
                return update { copy(error = "Passwords do not match") }
        }

        update { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.signup(s.username, s.email, s.password, s.referrerUsername.trim())
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