package com.example.OfferApp.data.repository

import com.example.OfferApp.domain.entities.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.Result

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun registerUser(email: String, password: String, username: String): Result<FirebaseUser?> {
        return try {
            // First, check if username already exists
            val usernameQuery = usersCollection.whereEqualTo("username", username).get().await()
            if (!usernameQuery.isEmpty) {
                throw Exception("El nombre de usuario ya está en uso.")
            }

            // 1. Create user in Authentication
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: throw Exception("Error de registro: no se pudo obtener la información del usuario.")

            // 2. Save user info in Firestore
            val user = User(uid = firebaseUser.uid, username = username, email = email)
            usersCollection.document(firebaseUser.uid).set(user).await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(identifier: String, password: String): Result<FirebaseUser?> {
        return try {
            var email = identifier
            // If identifier is not an email, assume it's a username
            if (!identifier.contains("@")) {
                val query = usersCollection.whereEqualTo("username", identifier).limit(1).get().await()
                if (query.isEmpty) {
                    throw Exception("Nombre de usuario no encontrado.")
                }
                val user = query.documents.first().toObject(User::class.java)
                email = user?.email ?: throw Exception("Error al obtener el email del usuario.")
            }
            
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}