package com.example.OfferApp.data.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.OfferApp.data.firebase.FirebaseAuthErrorHandler
import com.example.OfferApp.domain.entities.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.Result
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.OfferApp.data.firebase.FirebaseAuthService

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authService: FirebaseAuthService = FirebaseAuthService()
) {

    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun registerUser(email: String, password: String, username: String): Result<FirebaseUser?> {
        return try {
            val usernameQuery = usersCollection.whereEqualTo("username", username).get().await()
            if (!usernameQuery.isEmpty) {
                throw Exception("El nombre de usuario ya está en uso.")
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: throw Exception("Error de registro: no se pudo obtener la información del usuario.")

            val user = User(uid = firebaseUser.uid, username = username, email = email)
            usersCollection.document(firebaseUser.uid).set(user).await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            val message = FirebaseAuthErrorHandler.getErrorMessage(e)
            Result.failure(Exception(message))
        }
    }

    suspend fun loginUser(identifier: String, password: String): Result<FirebaseUser?> {
        return try {
            var email = identifier
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
            val message = FirebaseAuthErrorHandler.getErrorMessage(e)
            Result.failure(Exception(message))
        }
    }

    suspend fun getUser(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUsers(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        return try {
            val querySnapshot = usersCollection.whereIn("uid", userIds).get().await()
            querySnapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"].toString()
                        if (continuation.isActive) {
                            continuation.resume(imageUrl)
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception(error.description))
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        }
    }

    suspend fun updateUserProfileImage(uid: String, imageUri: Uri): Result<String> {
        return try {
            val imageUrl = uploadImageToCloudinary(imageUri)
            usersCollection.document(uid).update("profileImageUrl", imageUrl).await()
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
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

    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val followerRef = usersCollection.document(followerId)
            val followingRef = usersCollection.document(followingId)

            firestore.runTransaction { transaction ->
                transaction.update(followerRef, "following", FieldValue.arrayUnion(followingId))
                transaction.update(followingRef, "followers", FieldValue.arrayUnion(followerId))
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val followerRef = usersCollection.document(followerId)
            val followingRef = usersCollection.document(followingId)

            firestore.runTransaction { transaction ->
                transaction.update(followerRef, "following", FieldValue.arrayRemove(followingId))
                transaction.update(followingRef, "followers", FieldValue.arrayRemove(followerId))
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavorite(userId: String, postId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("favorites", FieldValue.arrayUnion(postId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateFCMToken(userId: String, token: String): Result<Unit> {
        // Redirige la llamada al servicio
        return authService.updateFCMToken(userId, token)
    }
    suspend fun removeFavorite(userId: String, postId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("favorites", FieldValue.arrayRemove(postId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}