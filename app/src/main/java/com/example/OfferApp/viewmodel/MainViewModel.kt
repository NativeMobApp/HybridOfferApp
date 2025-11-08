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
import com.example.OfferApp.domain.entities.Score
import com.example.OfferApp.domain.entities.User
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch

class MainViewModel(initialUser: User) : ViewModel() {
    private var currentSortOption: String = "Fecha (más recientes)"
    private var currentStatusFilter: String = "Todas"
    private var currentCategoryFilter: String = "Todos"

    private val postRepository = PostRepository()
    private val authRepository = AuthRepository()

    var user by mutableStateOf(initialUser)
        private set

    var posts by mutableStateOf<List<Post>>(emptyList())
        private set

    private var allPosts by mutableStateOf<List<Post>>(emptyList())

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf("Todos")
        private set

    var selectedFeedTab by mutableStateOf(0)
        private set

    private var lastVisiblePost by mutableStateOf<DocumentSnapshot?>(null)
    var isLoading by mutableStateOf(false)
        private set
    private var allPostsLoaded by mutableStateOf(false)

    var selectedPostId by mutableStateOf<String?>(null)
        private set

    var isDarkTheme by mutableStateOf<Boolean?>(null)
        private set

    val selectedPost by derivedStateOf {
        selectedPostId?.let { id ->
            allPosts.find { it.id == id }
        }
    }

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
        allPosts.filter { it.user?.uid == this@MainViewModel.user.uid }
    }

    val favoritePosts: List<Post> by derivedStateOf {
        allPosts.filter { post -> user.favorites.contains(post.id) }
    }

    init {
        viewModelScope.launch {
            postRepository.expireOldPosts() // Change to expire instead of delete
        }
        refreshPosts() // Initial load
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

    fun onThemeChange(isDark: Boolean?){
        isDarkTheme = isDark
    }

    fun loadMorePosts() {
        if (isLoading || allPostsLoaded) return

        viewModelScope.launch {
            isLoading = true
            try {
                val (newPosts, newLastVisible) = postRepository.getPosts(
                    lastVisiblePost = lastVisiblePost,
                    category = selectedCategory
                )
                if (newPosts.isNotEmpty()) {
                    allPosts = allPosts + newPosts
                    lastVisiblePost = newLastVisible
                    applyFilters()
                } else {
                    allPostsLoaded = true
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading more posts", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshPosts() {
        posts = emptyList()
        allPosts = emptyList()
        lastVisiblePost = null
        allPostsLoaded = false
        loadMorePosts()
    }

    fun loadAllPostsForProfile() {
        if (isLoading) return

        viewModelScope.launch {
            isLoading = true
            val tempAllPosts = mutableListOf<Post>()
            var lastVisible: DocumentSnapshot? = null
            var morePostsExist = true

            try {
                while(morePostsExist) {
                    val (newPosts, newLastVisible) = postRepository.getPosts(
                        lastVisiblePost = lastVisible,
                        category = "Todos"
                    )

                    if (newPosts.isNotEmpty()) {
                        tempAllPosts.addAll(newPosts)
                        lastVisible = newLastVisible
                    } else {
                        morePostsExist = false
                    }
                }
                allPosts = tempAllPosts
                applyFilters()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading all posts for profile", e)
            } finally {
                isLoading = false
                allPostsLoaded = true
            }
        }
    }

    fun selectPost(postId: String?) {
        selectedPostId = postId
        if (postId != null) {
            loadComments(postId)
        }
    }

    fun onFeedTabSelected(tabIndex: Int) {
        selectedFeedTab = tabIndex
        applyFilters()
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
            applyFilters() // Re-apply filters as following list might have changed
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
        return allPosts.filter { it.user?.uid == userId }
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

    suspend fun addPost(description: String, imageUri: Uri, location: String, latitude: Double, longitude: Double, category: String, price: Double, discountPrice: Double, store: String): Result<Unit> {
        val post = Post(
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            category = category,
            price = price,
            discountPrice = discountPrice,
            user = this@MainViewModel.user,
            store = store
        )
        return postRepository.addPost(post, imageUri)
    }

    fun toggleFavorite(postId: String) {
        val isCurrentlyFavorite = user.favorites.contains(postId)

        val updatedFavorites = if (isCurrentlyFavorite) {
            user.favorites - postId
        } else {
            user.favorites + postId
        }
        user = user.copy(favorites = updatedFavorites)

        viewModelScope.launch {
            val result = if (isCurrentlyFavorite) {
                authRepository.removeFavorite(user.uid, postId)
            } else {
                authRepository.addFavorite(user.uid, postId)
            }

            if (result.isFailure) {
                val rolledBackFavorites = if (isCurrentlyFavorite) {
                    user.favorites + postId
                } else {
                    user.favorites - postId
                }
                user = user.copy(favorites = rolledBackFavorites)
                Log.e("MainViewModel", "Failed to toggle favorite status for post $postId")
            }
        }
    }

    fun updatePostScore(postId: String, value: Int) {
        val postIndex = allPosts.indexOfFirst { it.id == postId }
        if (postIndex == -1) return

        val originalPost = allPosts[postIndex]
        if (originalPost.status != "activa") return

        val userId = user.uid
        val newScores = originalPost.scores.toMutableList()
        val existingScore = originalPost.scores.find { it.userId == userId }

        if (existingScore != null) {
            newScores.removeAll { it.userId == userId }
            if (existingScore.value != value) {
                newScores.add(Score(userId, value))
            }
        } else {
            newScores.add(Score(userId, value))
        }

        val totalScore = newScores.sumOf { it.value }
        val newStatus = if (totalScore < -15) "vencida" else originalPost.status

        val updatedPost = originalPost.copy(scores = newScores, status = newStatus)

        allPosts = allPosts.toMutableList().also { it[postIndex] = updatedPost }
        applyFilters()

        viewModelScope.launch {
            try {
                postRepository.updatePostScore(postId, user.uid, value).getOrThrow() // Use getOrThrow to catch exceptions

                // After the repository call, we should refresh the post from the source of truth
                val refreshedPost = postRepository.getPostById(postId)
                if (refreshedPost != null) {
                    val finalPostIndex = allPosts.indexOfFirst { it.id == postId }
                    if (finalPostIndex != -1) {
                        allPosts = allPosts.toMutableList().also { it[finalPostIndex] = refreshedPost }
                        applyFilters()
                    }
                } else {
                    // Post might have been deleted, so remove it from the list
                    allPosts = allPosts.filter { it.id != postId }
                    applyFilters()
                }
            } catch (e: Exception) {
                // If the operation failed, roll back the UI changes
                allPosts = allPosts.toMutableList().also { it[postIndex] = originalPost }
                applyFilters()
                Log.e("MainViewModel", "Failed to update post score, rolled back UI.", e)
            }
        }
    }


    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        applyFilters()
    }

    fun filterByCategory(category: String) {
        selectedCategory = category
        refreshPosts() // Reload from server with the new category filter
    }

    private fun applyFilters() {
        // 1️⃣ Filtramos por feed ("Todos" o "Siguiendo")
        var filteredPosts = if (selectedFeedTab == 1) {
            allPosts.filter { post -> user.following.contains(post.user?.uid) }
        } else {
            allPosts
        }

        // 2️⃣ Aplicamos búsqueda local
        if (searchQuery.isNotBlank()) {
            filteredPosts = filteredPosts.filter {
                it.description.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true)
            }
        }

        // 3️⃣ Aplicamos ordenamientos locales
        filteredPosts = when (currentSortOption) {
            "Puntaje (mayor a menor)" -> filteredPosts.sortedByDescending { it.scores.sumOf { s -> s.value } }
            "Puntaje (menor a mayor)" -> filteredPosts.sortedBy { it.scores.sumOf { s -> s.value } }
            "Precio (mayor a menor)" -> filteredPosts.sortedByDescending { it.price }
            "Precio (menor a mayor)" -> filteredPosts.sortedBy { it.price }
            else -> filteredPosts.sortedByDescending { it.timestamp }
        }

        posts = filteredPosts
    }


    fun getPostById(id: String): Post? {
        return allPosts.find { it.id == id }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(postId)
                // On successful deletion, update the local state to refresh the UI
                allPosts = allPosts.filterNot { it.id == postId }
                applyFilters()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting post $postId", e)
            }
        }
    }

    fun updatePostStatus(postId: String, newStatus: String) {
        val postIndex = allPosts.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val originalPost = allPosts[postIndex]
            val updatedPost = originalPost.copy(status = newStatus)
            allPosts = allPosts.toMutableList().also { it[postIndex] = updatedPost }
            applyFilters()

            viewModelScope.launch {
                val result = postRepository.updatePostStatus(postId, newStatus)
                if (result.isFailure) {
                    allPosts = allPosts.toMutableList().also { it[postIndex] = originalPost }
                    applyFilters()
                }
            }
        }
    }

    fun updatePostDetails(postId: String, description: String, price: Double, discountPrice: Double, category: String, store: String) {
        val postIndex = allPosts.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val originalPost = allPosts[postIndex]
            val updatedPost = originalPost.copy(
                description = description,
                price = price,
                discountPrice = discountPrice,
                category = category,
                store = store
            )
            allPosts = allPosts.toMutableList().also { it[postIndex] = updatedPost }
            applyFilters()

            viewModelScope.launch {
                val result = postRepository.updatePostDetails(postId, description, price, discountPrice, category, store)
                if (result.isFailure) {
                    allPosts = allPosts.toMutableList().also { it[postIndex] = originalPost }
                    applyFilters()
                }
            }
        }
    }
    fun setSortOption(option: String) {
        currentSortOption = option
        viewModelScope.launch { applyFiltersAndSort() }
    }

    fun setStatusFilter(option: String) {
        currentStatusFilter = option
        viewModelScope.launch { applyFiltersAndSort() }
    }

    fun setCategoryFilter(category: String) {
        currentCategoryFilter = category
        viewModelScope.launch { applyFiltersAndSort() }
    }

    suspend fun applyFiltersAndSort() {
        isLoading = true
        val result = postRepository.getFilteredPosts(
            status = currentStatusFilter,
            category = currentCategoryFilter,
            sortOption = currentSortOption
        )
        posts = result
        isLoading = false
    }
}
