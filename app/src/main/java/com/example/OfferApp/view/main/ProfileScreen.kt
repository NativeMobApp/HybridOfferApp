package com.example.OfferApp.view.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Comment
import com.example.OfferApp.domain.entities.User
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    mainViewModel: MainViewModel,
    userId: String,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { mainViewModel.updateProfileImage(it) }
        }
    )

    // Load the user profile information when the screen is first displayed or the userId changes.
    LaunchedEffect(userId) {
        mainViewModel.loadUserProfile(userId)
    }

    val profileUser by mainViewModel.profileUser.collectAsState()
    // Use the appropriate comment stream based on whether it is "My Profile" or not.
    val myComments by mainViewModel.myComments.collectAsState()
    val otherUserComments by mainViewModel.profileUserComments.collectAsState()


    Scaffold(
        topBar = {
            Header(
                username = mainViewModel.user.username,
                onBackClicked = onBackClicked,
                onSesionClicked = onLogoutClicked,
                onProfileClick = { onProfileClick(mainViewModel.user.uid) }
            )
        }
    ) { paddingValues ->
        profileUser?.let { user ->
            val isMyProfile = mainViewModel.user.uid == user.uid

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                // Profile Info & Stats Section
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (user.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .clickable { if (isMyProfile) galleryLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Foto de perfil por defecto",
                            modifier = Modifier
                                .size(120.dp)
                                .clickable { if (isMyProfile) galleryLauncher.launch("image/*") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumn(count = mainViewModel.getPostsByUser(user.uid).size, title = "Posts")
                        StatColumn(count = user.followers.size, title = "Seguidores")
                        StatColumn(count = user.following.size, title = "Seguidos")
                    }

                    // Follow/Unfollow Button
                    if (!isMyProfile) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val isFollowing = mainViewModel.user.following.contains(user.uid)
                        Button(onClick = {
                            scope.launch {
                                if (isFollowing) {
                                    mainViewModel.unfollowUser(user.uid)
                                } else {
                                    mainViewModel.followUser(user.uid)
                                }
                            }
                        }) {
                            Text(if (isFollowing) "Dejar de seguir" else "Seguir")
                        }
                    }
                }

                // Tab Section
                var selectedTabIndex by remember { mutableStateOf(0) }
                val tabs = if(isMyProfile) listOf("Mis Posts", "Mis Comentarios") else listOf("Posts", "Comentarios")

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) }
                        )
                    }
                }

                // Content Section
                when (selectedTabIndex) {
                    0 -> {
                        val userPosts = mainViewModel.getPostsByUser(user.uid)
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(userPosts) { post ->
                                PostItem(mainViewModel = mainViewModel, post = post, onClick = { onPostClick(post.id) })
                            }
                        }
                    }
                    1 -> {
                        // ** THIS IS THE KEY CHANGE **
                        // Show `myComments` for the current user and `otherUserComments` for others.
                        val commentsToShow = if (isMyProfile) myComments else otherUserComments
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(commentsToShow) { comment ->
                                CommentRowItem(
                                    mainViewModel = mainViewModel,
                                    comment = comment,
                                    onClick = { onPostClick(comment.postId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatColumn(count: Int, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}

@Composable
private fun CommentRowItem(
    mainViewModel: MainViewModel,
    comment: Comment,
    onClick: () -> Unit
) {
    val postTitle = mainViewModel.getPostById(comment.postId)?.description ?: "Post no encontrado"
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Comentaste en: $postTitle",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = comment.text, maxLines = 2)
        comment.timestamp?.let {
            Text(
                text = sdf.format(it),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
