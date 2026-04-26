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
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(StoryViewModel::class.java) ->
                StoryViewModel(repository) as T
            modelClass.isAssignableFrom(EarningsViewModel::class.java) ->
                EarningsViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(repository) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(repository) as T
            modelClass.isAssignableFrom(SignupViewModel::class.java) ->
                SignupViewModel(repository) as T
            modelClass.isAssignableFrom(ThemeViewModel::class.java) ->
                ThemeViewModel() as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}