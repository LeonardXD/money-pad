package com.example.moneypad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.ui.auth.LoginViewModel
import com.example.moneypad.ui.auth.SignupViewModel
import com.example.moneypad.ui.screens.EarningsViewModel
import com.example.moneypad.ui.screens.ProfileViewModel
import com.example.moneypad.ui.screens.StoryViewModel
import com.example.moneypad.ui.theme.ThemeViewModel

class ViewModelFactory(private val repository: MoneyPadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(EarningsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EarningsViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignupViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
