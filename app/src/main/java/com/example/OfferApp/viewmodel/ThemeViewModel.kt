package com.example.OfferApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.OfferApp.data.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val sessionManager: SessionManager) : ViewModel() {

    // Expose the isDarkMode flow as a StateFlow
    val isDarkMode = sessionManager.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Function to change the theme
    fun setTheme(isDarkMode: Boolean) {
        viewModelScope.launch {
            sessionManager.saveTheme(isDarkMode)
        }
    }
}
