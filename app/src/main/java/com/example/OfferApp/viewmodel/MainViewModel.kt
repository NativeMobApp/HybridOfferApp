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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
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

    var selectedCategory by mutableStateOf("Todos")
        private set

    // <-- CAMBIO: Estado para la pestaña seleccionada (0 para "Todos", 1 para "Siguiendo")
    var selectedFeedTab by mutableStateOf(0)
        private set

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _profileUser = MutableStateFlow<User?>(null)
    val profileUser = _profileUser.asStateFlow()

    private val _profileUserComments = MutableStateFlow<List<Comment>>(emptyList())
    val profileUserComments = _profileUserComments.asStateFlow()

    private val _myComments = MutableStateFlow<List<Comment>>(emptyList())
    val myComments = _myComments.asStateFlow()

    private var commentsJob: Job? = null

    val myPosts: List<Post> by derivedStateOf {
        originalPosts.filter { it.user?.uid == this@MainViewModel.user.uid }
    }

    init {
        viewModelScope.launch {
            postRepository.getPosts().collect { postList ->
                originalPosts = postList
                applyFilters()
            }
        }
        viewModelScope.launch {
            authRepository.getUser(initialUser.uid)?.let { fetchedUser ->
                this@MainViewModel.user = fetchedUser
            }
        }
        viewModelScope.launch {
            getCommentsByUser(initialUser.uid).collect { comments ->
                _myComments.value = comments
            }
        }
    }

    // <-- CAMBIO: Nueva función para cambiar la pestaña del feed
    fun onFeedTabSelected(tabIndex: Int) {
        selectedFeedTab = tabIndex
        applyFilters() // Re-aplicar filtros cada vez que se cambia la pestaña
    }

    fun loadUserProfile(userId: String) {
        if (userId.isBlank()) {
            Log.w("MainViewModel", "loadUserProfile called with blank userId")
            return
        }

        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            try {
                _profileUser.value = null
                _profileUserComments.value = emptyList()

                val profileToLoad = if (userId == user.uid) user else authRepository.getUser(userId)
                _profileUser.value = profileToLoad

                if (profileToLoad != null && userId != user.uid) {
                    getCommentsByUser(userId).collect { comments ->
                        _profileUserComments.value = comments
                    }
                } else if (profileToLoad == null) {
                    Log.w("MainViewModel", "User profile for $userId not found. No comments to load.")
                }

            } catch (e: Throwable) {
                Log.e("MainViewModel", "A critical error occurred in loadUserProfile for userId: $userId", e)
                _profileUser.value = null
                _profileUserComments.value = emptyList()
            }
        }
    }

    fun getCommentsByUser(userId: String): Flow<List<Comment>> {
        return postRepository.getCommentsByUser(userId).catch { e ->
            val errorMessage = e.message ?: "Unknown error"
            if (errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("requires an index")) {
                val firestoreIndexUrl = errorMessage.substringAfter("https://console.firebase.google.com")
                val fullUrl = "https://console.firebase.google.com$firestoreIndexUrl".replace("\\n", "")
                Log.e("MainViewModel",
                    "\n*************************************************************************************************\n" +
                            "** FIRESTORE INDEX REQUIRED **\n" +
                            "** Your query requires a custom index. Please create it in your Firebase console. **\n" +
                            "** Click this link to create it automatically: \n" +
                            "** $fullUrl\n" +
                            "*************************************************************************************************\n"
                )
            } else {
                Log.e("MainViewModel", "Error getting comments for user $userId", e)
            }
            emitAll(MutableStateFlow(emptyList()))
        }
    }

    suspend fun refreshCurrentUser() {
        try {
            authRepository.getUser(user.uid)?.let { user = it }
            applyFilters() // <-- CAMBIO: Refrescar posts al actualizar el usuario (por si se sigue a alguien nuevo)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to refresh current user", e)
        }
    }

    suspend fun followUser(followedUserId: String) {
        if (authRepository.followUser(user.uid, followedUserId).isSuccess) {
            refreshCurrentUser()
            try {
                _profileUser.value = authRepository.getUser(followedUserId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to refresh profile user after follow", e)
            }
        }
    }

    suspend fun unfollowUser(followedUserId: String) {
        if (authRepository.unfollowUser(user.uid, followedUserId).isSuccess) {
            refreshCurrentUser()
            try {
                _profileUser.value = authRepository.getUser(followedUserId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to refresh profile user after unfollow", e)
            }
        }
    }

    suspend fun getUser(userId: String): User? {
        return authRepository.getUser(userId)
    }

    suspend fun getUsers(userIds: List<String>): List<User> {
        return authRepository.getUsers(userIds)
    }

    fun getPostsByUser(userId: String): List<Post> {
        return originalPosts.filter { it.user?.uid == userId }
    }

    fun updateProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            authRepository.updateUserProfileImage(this@MainViewModel.user.uid, imageUri).onSuccess {
                refreshCurrentUser()
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
            val comment = Comment(
                postId = postId,
                userId = this@MainViewModel.user.uid,
                user = this@MainViewModel.user,
                text = text
            )
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
        applyFilters()
    }

    fun filterByCategory(category: String) {
        selectedCategory = category
        applyFilters()
    }

    // <-- CAMBIO: `applyFilters` ahora tiene en cuenta la pestaña seleccionada
    private fun applyFilters() {
        // 1. Determinar la lista base de posts según la pestaña seleccionada
        val basePosts = if (selectedFeedTab == 0) {
            originalPosts // Pestaña "Todos"
        } else {
            // Pestaña "Siguiendo": filtrar posts cuyo autor esté en mi lista de `following`
            originalPosts.filter { post -> user.following.contains(post.user?.uid) }
        }

        // 2. Filtrar por categoría
        val filteredByCategory = if (selectedCategory == "Todos") {
            basePosts
        } else {
            basePosts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        // 3. Filtrar por búsqueda de texto
        posts = if (searchQuery.isBlank()) {
            filteredByCategory
        } else {
            filteredByCategory.filter {
                it.description.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun getPostById(id: String): Post? {
        return originalPosts.find { it.id == id }
    }
}
