package com.example.OfferApp.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.OfferApp.data.repository.AuthRepository
import com.example.OfferApp.data.repository.PostRepository
import com.example.OfferApp.domain.entities.Comment
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.domain.entities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(initialUser: User) : ViewModel() {
    private val postRepository = PostRepository()
    private val authRepository = AuthRepository()

    var user by mutableStateOf(initialUser)
        private set

    var posts by mutableStateOf<List<Post>>(emptyList())
        private set

    private var originalPosts by mutableStateOf<List<Post>>(emptyList())

    var searchQuery by mutableStateOf("")
        private set

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _myComments = MutableStateFlow<List<Comment>>(emptyList())
    val myComments = _myComments.asStateFlow()

    val myPosts: List<Post> by derivedStateOf {
        originalPosts.filter { it.user?.uid == this@MainViewModel.user.uid }
    }

    init {
        viewModelScope.launch {
            postRepository.getPosts().collect { postList ->
                originalPosts = postList
                onSearchQueryChange(searchQuery)
            }
        }
        viewModelScope.launch {
            authRepository.getUser(initialUser.uid)?.let { fetchedUser ->
                this@MainViewModel.user = fetchedUser
            }
        }
        viewModelScope.launch {
            postRepository.getCommentsByUser(initialUser.uid)
                .catch { e ->
                    // This will prevent the crash and log the error.
                    // The error message from Firestore will contain a link to create the index.
                    Log.e("MainViewModel", "Error fetching user comments: ${e.message}")
                }
                .collect {
                _myComments.value = it
            }
        }
    }

    fun updateProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            authRepository.updateUserProfileImage(this@MainViewModel.user.uid, imageUri).onSuccess {
                authRepository.getUser(this@MainViewModel.user.uid)?.let { updatedUser ->
                    this@MainViewModel.user = updatedUser
                }
            }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getCommentsForPost(postId).collect {
                _comments.value = it
            }
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            val comment = Comment(postId = postId, user = this@MainViewModel.user, text = text)
            postRepository.addCommentToPost(postId, comment)
        }
    }

    suspend fun addPost(description: String, imageUri: Uri, location: String, latitude: Double, longitude: Double, category: String, price: Double): Result<Unit> {
        val post = Post(
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            category = category,
            price = price,
            user = this@MainViewModel.user
        )
        return postRepository.addPost(post, imageUri)
    }

    fun updatePostScore(postId: String, value: Int) {
        viewModelScope.launch {
            postRepository.updatePostScore(postId, this@MainViewModel.user.uid, value)
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        posts = if (newQuery.isBlank()) {
            originalPosts
        } else {
            originalPosts.filter {
                it.description.contains(newQuery, ignoreCase = true) ||
                it.location.contains(newQuery, ignoreCase = true)
            }
        }
    }

    fun filterByCategory(category: String) {
        posts = if (category == "Todos") {
            originalPosts
        } else {
            originalPosts.filter { it.category.equals(category, ignoreCase = true) }
        }
    }

    fun getPostById(id: String): Post? {
        return originalPosts.find { it.id == id }
    }
}