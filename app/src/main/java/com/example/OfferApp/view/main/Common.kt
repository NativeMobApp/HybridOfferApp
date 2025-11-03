package com.example.OfferApp.view.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.viewmodel.MainViewModel

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

    val isFavorite = mainViewModel.user.favorites.contains(post.id)
    val favoriteColor = if (isFavorite) Color.Yellow else LocalContentColor.current

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
            IconButton(onClick = { mainViewModel.toggleFavorite(post.id) }) {
                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = favoriteColor)
            }
        }
    }
}
