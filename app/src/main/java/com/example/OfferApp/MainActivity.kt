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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log // <--- ¡Añadir esta!
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging // <--- ¡Añadir esta!

// ViewModelFactory para crear ViewModels con dependencias
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

    // DECLARACIÓN Y REGISTRO DEL LAUNCHER (SOLUCIÓN DEL ERROR)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
        } else {
            // Permiso denegado
        }
    }


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
                    val startDestination =
                        if (isLoggedIn as Boolean) Screen.Main.route else Screen.Login.route
                    NavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel, // Pass the ThemeViewModel
                        startDestination = startDestination
                    )
                }
            }
        }
        // Llamada para solicitar el permiso y obtener el token
        askNotificationPermission()
    }


    private fun askNotificationPermission() {
        // Esto solo es necesario para API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permiso ya concedido. Las notificaciones funcionarán.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Opcional: Mostrar UI educativa, pero el código de abajo pide el permiso directamente.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Pedir el permiso directamente
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}