package com.example.OfferApp.view.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.OfferApp.navigation.Screen
import com.example.OfferApp.viewmodel.AuthViewModel
import com.example.OfferApp.viewmodel.AuthState

@Composable
fun LogInScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

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

        TextButton(onClick = { navController.navigate(Screen.Register.route) }, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta")
        }

        TextButton(onClick = { navController.navigate(Screen.ForgotPassword.route) }, modifier = Modifier.fillMaxWidth()) {
            Text("Olvidé mi contraseña")
        }

        Spacer(Modifier.height(8.dp))

        if (state is AuthState.Loading) {
            CircularProgressIndicator()
        }
        if (state is AuthState.Error) {
            Text("Error: ${(state as AuthState.Error).message}")
        }
    }
}
