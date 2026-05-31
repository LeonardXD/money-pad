package com.example.moneypad.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class OnboardingUiState(
    val currentStep: Int = 1,
    val selectedGender: String = "",
    val birthMonth: String = "",
    val birthDay: String = "",
    val birthYear: String = "",
    val selectedGenres: List<String> = emptyList(),
    val availableGenres: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
    val user: com.example.moneypad.data.model.User? = null
)

class OnboardingViewModel(private val repository: MoneyPadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getCurrentUser().filterNotNull().collect { user ->
                _uiState.update { state ->
                    if (state.user == null) {
                        val birthParts = user.birthday.split("-")
                        state.copy(
                            user = user,
                            currentStep = user.onboardingStep,
                            selectedGender = user.gender,
                            birthYear = birthParts.getOrNull(0) ?: "",
                            birthMonth = birthParts.getOrNull(1) ?: "",
                            birthDay = birthParts.getOrNull(2) ?: "",
                            selectedGenres = user.preferredGenres.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        )
                    } else {
                        state.copy(user = user)
                    }
                }
            }
        }
        
        viewModelScope.launch {
            repository.getAvailableGenres().collect { genres ->
                _uiState.update { it.copy(availableGenres = genres) }
            }
        }
    }

    fun claimReferralReward() {
        viewModelScope.launch {
            repository.claimReferralReward()
        }
    }

    fun setGender(gender: String) {
        _uiState.update { it.copy(selectedGender = gender, error = null) }
    }

    fun setBirthDate(month: String, day: String, year: String) {
        _uiState.update { it.copy(birthMonth = month, birthDay = day, birthYear = year, error = null) }
    }

    fun toggleGenre(genre: String) {
        _uiState.update { state ->
            val newGenres = if (state.selectedGenres.contains(genre)) {
                state.selectedGenres - genre
            } else {
                if (state.selectedGenres.size < 5) {
                    state.selectedGenres + genre
                } else {
                    state.selectedGenres
                }
            }
            state.copy(selectedGenres = newGenres, error = null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun nextStep() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (state.currentStep) {
                1 -> {
                    if (state.selectedGender.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, error = "Please select a gender") }
                        return@launch
                    }
                    val result = repository.saveOnboardingGender(state.selectedGender)
                    if (result.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, currentStep = 2) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                    }
                }
                2 -> {
                    if (state.birthMonth.isBlank() || state.birthDay.isBlank() || state.birthYear.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, error = "Please enter your full birthday") }
                        return@launch
                    }
                    val formattedMonth = state.birthMonth.padStart(2, '0')
                    val formattedDay = state.birthDay.padStart(2, '0')
                    val birthday = "${state.birthYear}-$formattedMonth-$formattedDay"
                    
                    val result = repository.saveOnboardingBirthday(birthday)
                    if (result.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, currentStep = 3) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                    }
                }
                3 -> {
                    completeOnboarding()
                }
            }
        }
    }
    
    fun skipGenres() {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedGenres = emptyList()) }
            completeOnboarding()
        }
    }

    private suspend fun completeOnboarding() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }
        val result = repository.completeOnboarding(state.selectedGenres)
        if (result.isSuccess) {
            _uiState.update { it.copy(isLoading = false, isComplete = true) }
        } else {
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
        }
    }
}
