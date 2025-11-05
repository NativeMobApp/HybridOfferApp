package com.example.OfferApp.view.forgotpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.OfferApp.R
import com.example.OfferApp.view.components.TemporaryMessageCard
import com.example.OfferApp.viewmodel.AuthViewModel
import com.example.OfferApp.viewmodel.AuthState

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAuthState()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {

                if (state is AuthState.PasswordResetSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Correo Enviado",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = (state as AuthState.PasswordResetSuccess).message,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                                Text("Volver a inicio de sesión")
                            }
                        }
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.offerapplogo),
                        contentDescription = "OfferApp Logo",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )

                    Spacer(Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "Recuperar Contraseña",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Correo electrónico") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = state !is AuthState.Loading
                            )

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.resetPassword(email) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state !is AuthState.Loading && email.isNotBlank()
                            ) {
                                Text("Enviar correo")
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(onClick = onNavigateToLogin, enabled = state !is AuthState.Loading) {
                        Text("Volver a inicio de sesión")
                    }

                    Spacer(Modifier.height(16.dp))

                    if (state is AuthState.Loading) {
                        CircularProgressIndicator()
                    }

                    if (state is AuthState.Error) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            TemporaryMessageCard(
                                message = (state as AuthState.Error).message,
                                backgroundColor = Color(0xFFFFA726), // Naranja suave
                                onDismiss = { viewModel.resetAuthState() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
