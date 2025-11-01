package com.example.OfferApp.view.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Comment
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    mainViewModel: MainViewModel,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onPostClick: (String) -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { mainViewModel.updateProfileImage(it) }
        }
    )

    Scaffold(
        topBar = {
            Header(
                username = mainViewModel.user.username,
                onBackClicked = onBackClicked,
                onSesionClicked = onLogoutClicked,
                onProfileClick = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Profile Info Section ---
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (mainViewModel.user.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = mainViewModel.user.profileImageUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Foto de perfil por defecto",
                        modifier = Modifier
                            .size(120.dp)
                            .clickable { galleryLauncher.launch("image/*") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mainViewModel.user.username,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // --- Tab Section ---
            var selectedTabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("Mis Posts", "Mis Comentarios")

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }

            // --- Content Section ---
            when (selectedTabIndex) {
                0 -> {
                    val myPosts = mainViewModel.myPosts
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(myPosts) { post ->
                            PostItem(mainViewModel = mainViewModel, post = post, onClick = { onPostClick(post.id) })
                        }
                    }
                }
                1 -> {
                    val myComments by mainViewModel.myComments.collectAsState()
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(myComments) { comment ->
                           CommentRowItem(
                               mainViewModel = mainViewModel, // Pass the ViewModel
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

@Composable
private fun CommentRowItem(
    mainViewModel: MainViewModel, // ViewModel is now a parameter
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
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}