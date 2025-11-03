@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.OfferApp.view.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import com.example.OfferApp.domain.entities.Comment
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PostDetailScreen(
    mainViewModel: MainViewModel,
    post: Post,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    LaunchedEffect(post.id) {
        mainViewModel.loadComments(post.id)
    }

    Scaffold(
        topBar = {
            Header(
                username = mainViewModel.user.username,
                title = post.description,
                onBackClicked = onBackClicked,
                onProfileClick = { onProfileClick(mainViewModel.user.uid) },
                onSesionClicked = onLogoutClicked
            )
        }
    ) { paddingValues ->
        PostDetailContent(
            mainViewModel = mainViewModel,
            post = post,
            modifier = Modifier.padding(paddingValues),
            onProfileClick = onProfileClick,
            onBackClicked = onBackClicked
        )
    }
}

@Composable
fun PostDetailContent(
    mainViewModel: MainViewModel,
    post: Post,
    modifier: Modifier = Modifier,
    onProfileClick: (String) -> Unit,
    onBackClicked: () -> Unit
) {
    val comments by mainViewModel.comments.collectAsState()
    var newCommentText by remember { mutableStateOf("") }
    val currentUserIsAuthor = mainViewModel.user.uid == post.user?.uid
    var showDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Foto", "Mapa")
    val isMapTouched = remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este post? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = {
                    mainViewModel.deletePost(post.id)
                    showDialog = false
                    onBackClicked() // Go back after deleting
                }) { Text("Eliminar") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title) }
                )
            }
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState, enabled = !isMapTouched.value)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .zIndex(-1f)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        AsyncImage(
                            model = post.imageUrl.replace("http://", "https://"),
                            contentDescription = "Post image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    1 -> {
                        PostMapView(post = post, isMapTouched = isMapTouched)
                    }
                }
            }

            // Content Section
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                PostInfoSection(mainViewModel, post, onProfileClick) { showDialog = true }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Comments Section
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
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        comments.forEach { comment ->
                            CommentItem(comment, onProfileClick)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Add Comment text field is always at the bottom
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
    }
}

@Composable
@Suppress("DEPRECATION")
private fun PostMapView(post: Post, isMapTouched: MutableState<Boolean>) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            val geoPoint = GeoPoint(post.latitude, post.longitude)
            controller.setCenter(geoPoint)

            val marker = Marker(this)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = post.description
            marker.snippet = post.location
            overlays.add(marker)
            marker.showInfoWindow()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isMapTouched.value = true // Disable parent scrolling
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        isMapTouched.value = false // Re-enable parent scrolling
                    }
                }
            },
        factory = { mapView },
        update = {
            it.controller.setCenter(GeoPoint(post.latitude, post.longitude))
            it.invalidate()
        }
    )
}

@Composable
private fun PostInfoSection(
    mainViewModel: MainViewModel,
    post: Post,
    onProfileClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val currentUserIsAuthor = mainViewModel.user.uid == post.user?.uid
    val score = post.scores.sumOf { it.value }
    val scoreColor = when {
        score > 0 -> Color(0xFF4CAF50)
        score < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
    val userVote = post.scores.find { it.userId == mainViewModel.user.uid }?.value
    val isFavorite = mainViewModel.user.favorites.contains(post.id)
    val favoriteColor = if (isFavorite) Color(0xFFFFC107) else Color.Gray

    Column(modifier = Modifier.padding(16.dp)) {
        // Author Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { post.user?.uid?.let { onProfileClick(it) } },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.user?.profileImageUrl?.replace("http://", "https://"),
                contentDescription = "Author profile image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = post.user?.username ?: "Usuario desconocido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                post.timestamp?.let {
                    Text(
                        text = "Publicado el ${sdf.format(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        HorizontalDivider()

        // Post Details
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column (Description, Category, Location)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(icon = Icons.Default.Category, text = post.category)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(icon = Icons.Default.LocationOn, text = post.location)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column (Price and Score)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(100.dp)
            ) {
                Text(
                    text = "$${String.format("%.2f", post.price)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$score",
                        color = scoreColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Star, contentDescription = "Score",
                        tint = scoreColor, modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vote Buttons
            OutlinedButton(onClick = { mainViewModel.updatePostScore(post.id, 1) }, enabled = !currentUserIsAuthor) {
                Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = if (userVote == 1) Color(0xFF4CAF50) else Color.Gray)
            }
            OutlinedButton(onClick = { mainViewModel.updatePostScore(post.id, -1) }, enabled = !currentUserIsAuthor) {
                Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", tint = if (userVote == -1) MaterialTheme.colorScheme.error else Color.Gray)
            }

            // Favorite and Share
            OutlinedButton(onClick = { mainViewModel.toggleFavorite(post.id) }) {
                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = favoriteColor)
            }

            OutlinedButton(onClick = {
                val shareText = "¡Mira esta oferta en OfferApp!\n\n${post.description} por solo $${post.price}\n\n${post.imageUrl}"
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Compartir")
            }

            // Delete Button
            if (currentUserIsAuthor) {
                OutlinedButton(onClick = onDeleteClick, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar post")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CommentItem(comment: Comment, onProfileClick: (String) -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = comment.user?.profileImageUrl?.replace("http://", "https://"),
                contentDescription = "Comment author profile image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .clickable { comment.user?.uid?.let { onProfileClick(it) } },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user?.username ?: "Anónimo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { comment.user?.uid?.let { onProfileClick(it) } }
                    )
                }
                comment.timestamp?.let {
                    Text(
                        text = sdf.format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AddCommentSection(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Añadir un comentario...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar comentario", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
