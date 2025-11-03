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

                val existingScoreIndex = post.scores.indexOfFirst { it.userId == userId }
                val newScores = post.scores.toMutableList()

                if (existingScoreIndex != -1) {
                    if (newScores[existingScoreIndex].value == value) {
                        newScores.removeAt(existingScoreIndex)
                    } else {
                        newScores[existingScoreIndex] = newScores[existingScoreIndex].copy(value = value)
                    }
                } else {
                    newScores.add(Score(userId, value))
                }
                transaction.update(postRef, "scores", newScores)
            }.await()

            val updatedPost = postsCollection.document(postId).get().await().toObject(Post::class.java)
            if (updatedPost != null) {
                val totalScore = updatedPost.scores.sumOf { it.value }
                if (totalScore < -15) {
                    deletePost(postId)
                }
            }

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
                .whereEqualTo("userId", userId) // Querying by the new simple field
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

    suspend fun getPosts(lastVisiblePost: DocumentSnapshot? = null): Pair<List<Post>, DocumentSnapshot?> {
        val limit = 3L
        val query = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

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

    suspend fun deleteExpiredPosts(): Result<Unit> {
        return try {
            val thirtyDaysInMillis = 30 * 24 * 60 * 60 * 1000L
            val cutoffDate = Date(System.currentTimeMillis() - thirtyDaysInMillis)

            val querySnapshot = postsCollection
                .whereLessThan("timestamp", cutoffDate)
                .get()
                .await()

            for (document in querySnapshot.documents) {
                deletePost(document.id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
