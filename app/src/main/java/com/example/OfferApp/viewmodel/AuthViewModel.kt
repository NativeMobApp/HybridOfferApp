package com.example.OfferApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.OfferApp.data.repository.AuthRepository
import com.example.OfferApp.domain.entities.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    // Success state now holds the full User object
    data class Success(val user: User) : AuthState()
    data class PasswordResetSuccess(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repository.registerUser(email, password, username)
            _state.value = result.fold(
                onSuccess = { firebaseUser ->
                    val uid = firebaseUser?.uid ?: return@fold AuthState.Error("Error de registro.")
                    // After registering, fetch the full user profile from Firestore
                    val user = repository.getUser(uid)
                    if (user != null) {
                        AuthState.Success(user)
                    } else {
                        AuthState.Error("No se pudo cargar el perfil del usuario.")
                    }
                },
                onFailure = { AuthState.Error(it.message ?: "Error al registrar") }
            )
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repository.loginUser(identifier, password)
            _state.value = result.fold(
                onSuccess = { firebaseUser ->
                     val uid = firebaseUser?.uid ?: return@fold AuthState.Error("Error de inicio de sesión.")
                    // After logging in, fetch the full user profile from Firestore
                    val user = repository.getUser(uid)
                    if (user != null) {
                        AuthState.Success(user)
                    } else {
                        AuthState.Error("No se pudo cargar el perfil del usuario.")
                    }
                },
                onFailure = { AuthState.Error(it.message ?: "Error al iniciar sesión") }
            )
        }
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val result = repository.resetPassword(email)
            _state.value = result.fold(
                onSuccess = { AuthState.PasswordResetSuccess("Se envió un mail a $email") },
                onFailure = { AuthState.Error(it.message ?: "Error al enviar mail") }
            )
        }
    }
    fun logout() {
        repository.logout()
        _state.value = AuthState.Idle
    }
}