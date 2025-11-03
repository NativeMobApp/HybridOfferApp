package com.example.OfferApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.OfferApp.data.repository.AuthRepository
import com.example.OfferApp.domain.entities.User
import com.example.OfferApp.navigation.NavGraph
import com.example.OfferApp.ui.theme.MyApplicationTheme
import com.example.OfferApp.viewmodel.AuthViewModel
import com.example.OfferApp.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authViewModel: AuthViewModel by viewModels()

        setContent {
            // We need access to the MainViewModel here, but it's created inside NavGraph.
            // We pass the authViewModel to the NavGraph, and the NavGraph is responsible
            // for creating the MainViewModel once the user is authenticated.
            // The NavGraph will then internally use MyApplicationTheme to apply the correct theme.

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                NavGraph(navController = navController, authViewModel = authViewModel)
            }
        }
    }
}
