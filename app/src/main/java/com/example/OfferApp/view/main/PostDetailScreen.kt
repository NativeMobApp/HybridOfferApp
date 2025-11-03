package com.example.OfferApp.view.main

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
                query = mainViewModel.searchQuery,
                onQueryChange = { mainViewModel.onSearchQueryChange(it) },
                onBackClicked = onBackClicked,
                onSesionClicked = onLogoutClicked,
                onProfileClick = { onProfileClick(mainViewModel.user.uid) },
                onLogoClick = onBackClicked
            )
        }
    ) { paddingValues ->
        PostDetailContent(
            mainViewModel = mainViewModel,
            post = post,
            modifier = Modifier.padding(paddingValues),
            onProfileClick = onProfileClick
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
                }) { Text("Eliminar") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
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

            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                PostInfoSection(mainViewModel, post, onProfileClick) { showDialog = true }

                HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
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
            }
        }

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
    val isFavorite = mainViewModel.user.favorites.contains(post.id)
    val favoriteColor = if (isFavorite) Color.Yellow else LocalContentColor.current

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            IconButton(onClick = { mainViewModel.toggleFavorite(post.id) }) {
                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = favoriteColor)
            }
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

            if (currentUserIsAuthor) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar post", tint = Color.Red)
                }
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
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar comentario")
        }
    }
}