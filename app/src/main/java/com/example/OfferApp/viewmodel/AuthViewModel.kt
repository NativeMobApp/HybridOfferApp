package com.example.OfferApp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.OfferApp.data.SessionManager
import com.example.OfferApp.data.repository.AuthRepository
import com.example.OfferApp.domain.entities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    data class PasswordResetSuccess(val message: String) : AuthState() // Added this state
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()

    val isLoggedIn = sessionManager.isLoggedInFlow

    init {
        viewModelScope.launch {
            if (sessionManager.isLoggedInFlow.first()) {
                val user = repository.currentUser?.uid?.let { repository.getUser(it) }
                if (user != null) {
                    _state.value = AuthState.Success(user)
                } else {
                    sessionManager.clearSession()
                }
            }
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            repository.loginUser(identifier, password).onSuccess { firebaseUser ->
                val user = repository.getUser(firebaseUser!!.uid)
                if (user != null) {
                    sessionManager.saveSessionState(true)
                    _state.value = AuthState.Success(user)
                } else {
                    _state.value = AuthState.Error("No se pudo cargar el perfil del usuario.")
                }
            }.onFailure {
                _state.value = AuthState.Error(it.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            repository.registerUser(email, password, username).onSuccess { firebaseUser ->
                val user = repository.getUser(firebaseUser!!.uid)
                if (user != null) {
                    sessionManager.saveSessionState(true)
                    _state.value = AuthState.Success(user)
                } else {
                    _state.value = AuthState.Error("No se pudo cargar el perfil del usuario.")
                }
            }.onFailure {
                _state.value = AuthState.Error(it.message ?: "Error al registrar")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            repository.resetPassword(email).onSuccess {
                _state.value = AuthState.PasswordResetSuccess("Se ha enviado un correo para restablecer tu contraseña.")
            }.onFailure {
                _state.value = AuthState.Error(it.message ?: "Error al restablecer la contraseña")
            }
        }
    }

    fun resetAuthState() {
        _state.value = AuthState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            sessionManager.clearSession()
            _state.value = AuthState.Idle
        }
    }
    fun setUiError(message: String) {
        _state.value = AuthState.Error(message)
    }
}
