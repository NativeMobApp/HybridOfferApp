package com.example.OfferApp.view.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun PostItem(mainViewModel: MainViewModel, post: Post, onClick: () -> Unit) {
    val isFavorite = mainViewModel.user.favorites.contains(post.id)
    val favoriteColor = if (isFavorite) Color(0xFFFFC107) else Color.Gray

    val score = post.scores.sumOf { it.value }
    val scoreColor = when {
        score > 0 -> Color(0xFF4CAF50) // A nice green color
        score < 0 -> MaterialTheme.colorScheme.error // Use the app's error color for negative scores
        else -> Color.Gray
    }

    val valuation = when {
        score > 10 -> "Ofertón"
        score > 5 -> "Buena oferta"
        score >= -5 -> "Oferta"
        score >= -10 -> "Mala oferta"
        else -> "Estafa"
    }

    val postTime = post.timestamp?.time ?: 0L
    val currentTime = System.currentTimeMillis()
    val diffInMillis = currentTime - postTime
    val isNew = diffInMillis < TimeUnit.HOURS.toMillis(24)

    val cardBackgroundColor = when {
        post.status.equals("vencida", ignoreCase = true) -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        post.status.equals("activa", ignoreCase = true) -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = post.imageUrl.replace("http://", "https://"),
                contentDescription = "Post image",
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = post.status.uppercase(),
                                color = if (post.status.equals("activa", ignoreCase = true)) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = valuation, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            if (isNew) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = post.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$${String.format("%.2f", post.discountPrice)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$${String.format("%.2f", post.price)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        IconButton(onClick = { mainViewModel.toggleFavorite(post.id) }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = favoriteColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
