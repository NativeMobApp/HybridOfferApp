package com.example.OfferApp.view.main

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.OfferApp.R
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("RememberReturnType")
@Composable
fun MapScreen(
    mainViewModel: MainViewModel,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Configuración de OSMdroid (solo se hace una vez)
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        topBar = {
            Header(
                username = mainViewModel.user.username,
                title = "Mapa de Ofertas",
                onProfileClick = onProfileClick,
                onSesionClicked = onLogoutClicked,
                onBackClicked = onBackClicked
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val mapView = MapView(ctx)
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    mapView.setBuiltInZoomControls(true)
                    mapView.setMultiTouchControls(true)

                    val customIconDrawable = ContextCompat.getDrawable(
                        ctx,
                        R.drawable.outline_map_pin_heart_24 //
                    )
                    val posts = mainViewModel.posts

                    val BUENOS_AIRES_CENTER = GeoPoint(-34.6037, -58.3816)

                    val initialCenter = BUENOS_AIRES_CENTER

                    mapView.controller.setZoom(12.0)
                    mapView.controller.setCenter(initialCenter)

                    // Añadir marcadores para cada post
                    posts.forEach { post ->
                        val point = GeoPoint(post.latitude, post.longitude)
                        val marker = Marker(mapView)
                        marker.position = point
                        if (customIconDrawable != null) {
                            marker.icon = customIconDrawable

                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        } else {
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }

                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = post.description.take(30) + "..." // Título del popup
                        marker.snippet = "Lat: ${post.latitude}, Lon: ${post.longitude}"
                        mapView.overlays.add(marker)

                    }

                    mapView.invalidate()
                    mapView
                },

                update = { mapView ->

                }
            )
        }
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mapView = (context as? androidx.activity.ComponentActivity)?.findViewById<MapView>(mapViewId)
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private const val mapViewId = 1001