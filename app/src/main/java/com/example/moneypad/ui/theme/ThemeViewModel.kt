package com.example.moneypad.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple in-memory ThemeViewModel (no Application needed).
 * The ViewModelFactory creates this without a repository.
 * Theme is in-memory per session — toggling persists until the process dies.
 * For true cross-session persistence, swap _isDarkTheme initialisation with
 * a SharedPreferences read (requires passing context or using AndroidViewModel).
 */
class ThemeViewModel : ViewModel() {
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setDark(dark: Boolean) {
        _isDarkTheme.value = dark
    }
}