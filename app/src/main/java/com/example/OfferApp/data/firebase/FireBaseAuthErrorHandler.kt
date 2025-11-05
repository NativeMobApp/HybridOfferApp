package com.example.OfferApp.data.firebase

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object FirebaseAuthErrorHandler {
    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException ->
                "El usuario no existe o fue eliminado."
            is FirebaseAuthInvalidCredentialsException ->
                "El usuario y/o la contraseña no son correctos"
            is FirebaseAuthUserCollisionException ->
                "Ya existe una cuenta con ese correo."
            is FirebaseAuthWeakPasswordException ->
                "La contraseña es demasiado débil."
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo."
                    "ERROR_USER_DISABLED" -> "La cuenta ha sido deshabilitada."
                    else -> "Error al procesar la autenticación. Intenta nuevamente."
                }
            }
            else -> "Error al procesar la autenticación. Intenta nuevamente."
        }
    }
}
