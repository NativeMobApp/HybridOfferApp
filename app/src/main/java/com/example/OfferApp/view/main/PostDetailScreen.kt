package com.example.OfferApp.view.main

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Comment
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PostDetailScreen(
    mainViewModel: MainViewModel,
    post: Post,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onProfileClick: (String) -> Unit // Modified: Now accepts a user ID
) {
    LaunchedEffect(post.id) {
        mainViewModel.loadComments(post.id)
    }

    Scaffold(
        topBar = {
            Header(
                username = mainViewModel.user.username,
                query = mainViewModel.searchQuery,
                onQueryChange = { mainViewModel.onSearchQueryChange(it) },
                onBackClicked = onBackClicked,
                onSesionClicked = onLogoutClicked,
                onProfileClick = { onProfileClick(mainViewModel.user.uid) } // Navigate to current user's profile
            )
        }
    ) { paddingValues ->
        PostDetailContent(
            mainViewModel = mainViewModel,
            post = post,
            modifier = Modifier.padding(paddingValues),
            onProfileClick = onProfileClick // Pass the navigation action
        )
    }
}

@Composable
fun PostDetailContent(
    mainViewModel: MainViewModel,
    post: Post,
    modifier: Modifier = Modifier,
    onProfileClick: (String) -> Unit
) {
    val comments by mainViewModel.comments.collectAsState()
    var newCommentText by remember { mutableStateOf("") }
    val currentUserIsAuthor = mainViewModel.user.uid == post.user?.uid

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PostInfoSection(mainViewModel, post, onProfileClick)

        Divider(modifier = Modifier.padding(top = 16.dp))
        Text(
            "Comentarios",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (comments.isEmpty()) {
                Text(
                    "Aún no hay comentarios. ¡Sé el primero!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                comments.forEach { comment ->
                    CommentItem(comment, onProfileClick)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!currentUserIsAuthor) {
            AddCommentSection(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                onSend = {
                    mainViewModel.addComment(post.id, newCommentText)
                    newCommentText = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PostInfoSection(
    mainViewModel: MainViewModel,
    post: Post,
    onProfileClick: (String) -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
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

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = post.imageUrl.replace("http://", "https://"),
            contentDescription = "Post image",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = post.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$${post.price}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (post.category.isNotBlank()) {
            Text(text = "Categoría: ${post.category}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(text = "Ubicación: ${post.location}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Latitud: ${post.latitude}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Longitud: ${post.longitude}", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { post.user?.uid?.let { onProfileClick(it) } }
        ) {
            Text(
                text = "Publicado por: ",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = post.user?.username ?: "Usuario desconocido",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        post.timestamp?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "El: ${sdf.format(it)}", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { mainViewModel.updatePostScore(post.id, 1) }, enabled = !currentUserIsAuthor) {
                Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = likeColor)
            }
            Text(
                text = "$score",
                color = scoreColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { mainViewModel.updatePostScore(post.id, -1) }, enabled = !currentUserIsAuthor) {
                Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", tint = dislikeColor)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val shareText = """
                    ¡Mira esta oferta en OfferApp!
                    
                    ${post.description}
                    Precio: $${post.price}
                    Ubicación: ${post.location}
                    
                    ${post.imageUrl}
                    
                    ¡Descárgate OfferApp y no te pierdas ninguna oferta!
                """.trimIndent()

                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }) {
                Icon(Icons.Default.Share, contentDescription = "Compartir")
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment, onProfileClick: (String) -> Unit) {
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.user?.username ?: "Anónimo",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { comment.user?.uid?.let { onProfileClick(it) } }
            )
            Spacer(modifier = Modifier.width(8.dp))
            comment.timestamp?.let {
                Text(text = sdf.format(it), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = comment.text)
    }
}

@Composable
private fun AddCommentSection(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Añadir un comentario...") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSend, enabled = value.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = "Enviar comentario")
        }
    }
}