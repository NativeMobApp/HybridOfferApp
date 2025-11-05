package com.example.OfferApp.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val message = FirebaseAuthErrorHandler.getErrorMessage(e)
            Result.failure(Exception(message))
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<Unit> {
        return try {
            // Validar username único
            if (isUsernameTaken(username)) {
                return Result.failure(Exception("El nombre de usuario ya está en uso."))
            }

            // Crear usuario en FirebaseAuth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No se pudo crear el usuario."))

            // Guardar en Firestore
            val user = mapOf(
                "uid" to uid,
                "email" to email,
                "username" to username
            )
            firestore.collection("users").document(uid).set(user).await()

            Result.success(Unit)
        } catch (e: Exception) {
            val message = FirebaseAuthErrorHandler.getErrorMessage(e)
            Result.failure(Exception(message))
        }
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        val snapshot = firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        return !snapshot.isEmpty
    }


    fun logout() {
        auth.signOut()

    }

    fun currentUser() = auth.currentUser
}
