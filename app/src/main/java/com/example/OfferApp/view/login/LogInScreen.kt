package com.example.OfferApp.view.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.OfferApp.domain.entities.User
import com.example.OfferApp.viewmodel.AuthViewModel
import com.example.OfferApp.viewmodel.AuthState

@Composable
fun LogInScreen(
    viewModel: AuthViewModel,
    onSuccess: (User) -> Unit, // Now passes the full User object
    onRegisterClick: () -> Unit,
    onForgotClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email o Nombre de usuario") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login(identifier, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }

        TextButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta")
        }

        TextButton(onClick = onForgotClick, modifier = Modifier.fillMaxWidth()) {
            Text("Olvidé mi contraseña")
        }

        Spacer(Modifier.height(8.dp))

        when (val S = state) {
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Success -> onSuccess(S.user) // Pass the full User object
            is AuthState.Error -> Text("Error: ${S.message}")
            else -> {}
        }
    }
}