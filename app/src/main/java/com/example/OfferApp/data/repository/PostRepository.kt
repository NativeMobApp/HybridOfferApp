package com.example.OfferApp.data.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.OfferApp.domain.entities.Comment
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.domain.entities.Score
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["url"].toString()
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

    suspend fun addPost(post: Post, imageUri: Uri): Result<Unit> {
        return try {
            val imageUrl = uploadImageToCloudinary(imageUri)
            post.imageUrl = imageUrl
            postsCollection.add(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePostScore(postId: String, userId: String, value: Int): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val post = transaction.get(postRef).toObject(Post::class.java)
                    ?: throw Exception("Post not found")

                if (post.status != "activa") { // Do not allow voting on non-active posts
                    throw Exception("Post is not active, cannot change score.")
                }

                val existingScoreIndex = post.scores.indexOfFirst { it.userId == userId }
                val newScores = post.scores.toMutableList()

                if (existingScoreIndex != -1) {
                    if (newScores[existingScoreIndex].value == value) {
                        newScores.removeAt(existingScoreIndex) // User removes their vote
                    } else {
                        // User changes their vote
                        newScores[existingScoreIndex] = newScores[existingScoreIndex].copy(value = value)
                    }
                } else {
                    newScores.add(Score(userId, value)) // New vote
                }
                transaction.update(postRef, "scores", newScores)

                val totalScore = newScores.sumOf { it.value }
                if (totalScore < -15) {
                    transaction.update(postRef, "status", "vencida")
                }

                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCommentToPost(postId: String, comment: Comment): Result<Unit> {
        return try {
            postsCollection.document(postId).collection("comments").add(comment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        return callbackFlow {
            val listener = postsCollection.document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        close(e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val comments = snapshot.toObjects(Comment::class.java)
                        trySend(comments).isSuccess
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    fun getCommentsByUser(userId: String): Flow<List<Comment>> {
        return callbackFlow {
            val listener = firestore.collectionGroup("comments")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        close(e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val comments = snapshot.toObjects(Comment::class.java)
                        trySend(comments).isSuccess
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun getPosts(
        lastVisiblePost: DocumentSnapshot? = null,
        category: String? = null
    ): Pair<List<Post>, DocumentSnapshot?> {
        val limit = 10L
        var query: Query = postsCollection

        if (category != null && category != "Todos") {
            query = query.whereEqualTo("category", category)
        }

        query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)

        val finalQuery = if (lastVisiblePost != null) {
            query.startAfter(lastVisiblePost)
        } else {
            query
        }

        val snapshot = finalQuery.get().await()
        val posts = snapshot.toObjects(Post::class.java)
        val newLastVisible = snapshot.documents.lastOrNull()

        return Pair(posts, newLastVisible)
    }

    suspend fun getPostById(postId: String): Post? {
        return try {
            postsCollection.document(postId).get().await().toObject(Post::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // First, delete all comments in the subcollection
            val commentsQuery = postsCollection.document(postId).collection("comments").get().await()
            for (document in commentsQuery.documents) {
                document.reference.delete().await()
            }

            // Then, delete the post itself
            postsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun expireOldPosts(): Result<Unit> {
        return try {
            val thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000L
            val cutoffDate = Date(System.currentTimeMillis() - thirtyDaysInMillis)

            val querySnapshot = postsCollection
                .whereLessThan("timestamp", cutoffDate)
                .whereEqualTo("status", "activa")
                .get()
                .await()

            val batch = firestore.batch()
            for (document in querySnapshot.documents) {
                val postRef = postsCollection.document(document.id)
                batch.update(postRef, "status", "vencida")
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePostStatus(postId: String, newStatus: String): Result<Unit> {
        return try {
            postsCollection.document(postId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePostDetails(postId: String, description: String, price: Double, discountPrice: Double, category: String, store: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "description" to description,
                "price" to price,
                "discountPrice" to discountPrice,
                "category" to category,
                "store" to store
            )
            postsCollection.document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // 🔹 Nuevo método para aplicar filtros y ordenamientos
    suspend fun getFilteredPosts(
        status: String = "Todas",
        category: String = "Todos",
        sortOption: String = "Fecha (más recientes)"
    ): List<Post> {
        var query: Query = postsCollection

        if (status != "Todas") query = query.whereEqualTo("status", status.lowercase())
        if (category != "Todos") query = query.whereEqualTo("category", category)

        // 🔸 Firestore sólo permite ordenar por campos simples
        query = when (sortOption) {
            "Precio (menor a mayor)" -> query.orderBy("price", Query.Direction.ASCENDING)
            "Precio (mayor a menor)" -> query.orderBy("price", Query.Direction.DESCENDING)
            "Fecha (más recientes)" -> query.orderBy("timestamp", Query.Direction.DESCENDING)
            else -> query.orderBy("timestamp", Query.Direction.DESCENDING)
        }

        val snapshot = query.get().await()
        var posts = snapshot.toObjects(Post::class.java)

        // 🔸 Ordenar por puntaje si fue seleccionado (client-side)
        if (sortOption == "Puntaje") {
            posts = posts.sortedByDescending { it.scores.sumOf { score -> score.value } }
        }

        return posts
    }

}
