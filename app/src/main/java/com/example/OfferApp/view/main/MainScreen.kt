package com.example.OfferApp.view.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onPostClick: (String) -> Unit,
    onLogoutClicked: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val categories = listOf(
        "Todos", "Alimentos", "Tecnología", "Moda", "Deportes", "Construcción",
        "Animales", "Electrodomésticos", "Servicios", "Educación",
        "Juguetes", "Vehículos", "Otros"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD32F2F))
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Categorías",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider(color = Color.LightGray)

                    categories.forEach { category ->
                        Text(
                            text = category,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    mainViewModel.filterByCategory(category)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    ) { 
        Scaffold(
            topBar = {
                Header(
                    username = mainViewModel.user.username,
                    query = mainViewModel.searchQuery,
                    onQueryChange = { mainViewModel.onSearchQueryChange(it) },
                    onSesionClicked = onLogoutClicked,
                    onProfileClick = onNavigateToProfile,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = onNavigateToMap,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Show map")
                    }
                    FloatingActionButton(onClick = onNavigateToCreatePost) {
                        Icon(Icons.Default.Add, contentDescription = "Add post")
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            if (isLandscape) {
                Row(modifier = Modifier.padding(paddingValues)) {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn {
                            items(mainViewModel.posts) { post ->
                                PostItem(mainViewModel = mainViewModel, post = post, onClick = { selectedPost = post })
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        selectedPost?.let { post ->
                            PostDetailContent(
                                mainViewModel = mainViewModel,
                                post = post
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(paddingValues)) {
                    items(mainViewModel.posts) { post ->
                        PostItem(mainViewModel = mainViewModel, post = post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(mainViewModel: MainViewModel, post: Post, onClick: () -> Unit) {
    val currentUserIsAuthor = mainViewModel.user.uid == post.user?.uid
    val score = post.scores.sumOf { it.value }
    val scoreColor = when {
        score > 0 -> Color.Green
        score < 0 -> Color.Red
        else -> LocalContentColor.current
    }

    val userVote = post.scores.find { it.userId == mainViewModel.user.uid }?.value
    val likeColor = if (userVote == 1) Color.Green else LocalContentColor.current
    val dislikeColor = if (userVote == -1) Color.Red else LocalContentColor.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.imageUrl.replace("http://", "https://"),
                contentDescription = "Post image",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${post.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                 Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ubicación: ${post.location}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Publicado por: ${post.user?.username ?: "Usuario desconocido"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { mainViewModel.updatePostScore(post.id, 1) }, enabled = !currentUserIsAuthor) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = likeColor)
                }
                Text(text = "$score", color = scoreColor, fontWeight = FontWeight.Bold)
                IconButton(onClick = { mainViewModel.updatePostScore(post.id, -1) }, enabled = !currentUserIsAuthor) {
                    Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", tint = dislikeColor)
                }
            }
        }
    }
}

@Composable
fun DrawerItem(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    )
}
