package com.example.OfferApp.view.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onLogoutClicked: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val categories = listOf(
        "Todos", "Alimentos", "Tecnología", "Moda", "Deportes", "Construcción",
        "Animales", "Electrodomésticos", "Servicios", "Educación",
        "Juguetes", "Vehículos", "Otros"
    )
    val themeOptions = listOf("Claro", "Oscuro", "Automático")

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Categorías",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LazyColumn {
                        items(categories) { category ->
                            val isSelected = mainViewModel.selectedCategory == category
                            Text(
                                text = category,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mainViewModel.filterByCategory(category)
                                        scope.launch { drawerState.close() }
                                    }
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        item {
                            HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Tema",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(themeOptions) { theme ->
                            val isSelected = when (theme) {
                                "Claro" -> mainViewModel.isDarkTheme == false
                                "Oscuro" -> mainViewModel.isDarkTheme == true
                                "Automático" -> mainViewModel.isDarkTheme == null
                                else -> false
                            }

                            Text(
                                text = theme,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (theme) {
                                            "Claro" -> mainViewModel.onThemeChange(false)
                                            "Oscuro" -> mainViewModel.onThemeChange(true)
                                            "Automático" -> mainViewModel.onThemeChange(null)
                                        }
                                        scope.launch { drawerState.close() }
                                    }
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        gesturesEnabled = !(isLandscape && mainViewModel.selectedPost != null)
    ) {
        Scaffold(
            topBar = {
                Header(
                    username = mainViewModel.user.username,
                    query = mainViewModel.searchQuery,
                    onQueryChange = { mainViewModel.onSearchQueryChange(it) },
                    onProfileClick = { onNavigateToProfile(mainViewModel.user.uid) },
                    onSesionClicked = onLogoutClicked,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onLogoClick = {
                        mainViewModel.onSearchQueryChange("")
                        mainViewModel.filterByCategory("Todos")
                    }
                )
            },
            floatingActionButton = {
                Column {
                    FloatingActionButton(
                        onClick = onNavigateToCreatePost,
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Crear Post", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    FloatingActionButton(
                        onClick = onNavigateToMap,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Ver en mapa", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                val tabTitles = listOf("Todos", "Siguiendo")
                TabRow(selectedTabIndex = mainViewModel.selectedFeedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = mainViewModel.selectedFeedTab == index,
                            onClick = { mainViewModel.onFeedTabSelected(index) },
                            text = { Text(text = title) }
                        )
                    }
                }

                if (isLandscape) {
                    LandscapeLayout(mainViewModel, onNavigateToProfile)
                } else {
                    PortraitLayout(mainViewModel, onPostClick)
                }
            }
        }
    }
}

@Composable
fun PortraitLayout(mainViewModel: MainViewModel, onPostClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        items(mainViewModel.posts) { post ->
            PostItem(mainViewModel = mainViewModel, post = post, onClick = { onPostClick(post.id) })
        }
        if (mainViewModel.isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Cargando posts...",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= mainViewModel.posts.size - 1) {
                    mainViewModel.loadMorePosts()
                }
            }
    }
}

@Composable
fun LandscapeLayout(mainViewModel: MainViewModel, onProfileClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f),
            state = listState
        ) {
            items(mainViewModel.posts) { post ->
                val modifier = if (mainViewModel.selectedPostId == post.id) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                }
                Box(modifier = modifier) {
                    PostItem(
                        mainViewModel = mainViewModel,
                        post = post,
                        onClick = { mainViewModel.selectPost(post.id) }
                    )
                }
            }
            if (mainViewModel.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Cargando posts...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f),
            contentAlignment = Alignment.Center
        ) {
            val selectedPost = mainViewModel.selectedPost
            if (selectedPost != null) {
                PostDetailContent(
                    mainViewModel = mainViewModel,
                    post = selectedPost,
                    onProfileClick = onProfileClick,
                    onBackClicked = { mainViewModel.selectPost("") }
                )
            } else {
                Text("Selecciona un post para ver su detalle", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= mainViewModel.posts.size - 1) {
                    mainViewModel.loadMorePosts()
                }
            }
    }
}
