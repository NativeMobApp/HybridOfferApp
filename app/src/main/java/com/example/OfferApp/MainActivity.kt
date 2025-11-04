package com.example.OfferApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.OfferApp.data.SessionManager
import com.example.OfferApp.data.repository.AuthRepository
import com.example.OfferApp.navigation.NavGraph
import com.example.OfferApp.navigation.Screen
import com.example.OfferApp.ui.theme.MyApplicationTheme
import com.example.OfferApp.viewmodel.AuthViewModel
import com.example.OfferApp.viewmodel.ThemeViewModel

// ViewModelFactory for creating ViewModels with dependencies
class ViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(AuthRepository(), sessionManager) as T
        }
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    private val authViewModel: AuthViewModel by viewModels { ViewModelFactory(sessionManager) }
    private val themeViewModel: ThemeViewModel by viewModels { ViewModelFactory(sessionManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val useDarkTheme = isDarkMode ?: isSystemInDarkTheme()

            MyApplicationTheme(useDarkTheme = useDarkTheme) {
                val navController = rememberNavController()

                // Observe the login state to determine the start destination
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)

                // Show a loading screen or similar while checking login state
                if (isLoggedIn != null) {
                    val startDestination = if (isLoggedIn as Boolean) Screen.Main.route else Screen.Login.route
                    NavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel, // Pass the ThemeViewModel
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
