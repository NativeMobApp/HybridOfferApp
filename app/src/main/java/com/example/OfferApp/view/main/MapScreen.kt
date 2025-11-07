package com.example.OfferApp.view.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.OfferApp.R
import com.example.OfferApp.domain.entities.Post
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("RememberReturnType")
@Composable
fun MapScreen(
    mainViewModel: MainViewModel,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onProfileClick: () -> Unit,
    onPostClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember { mutableStateOf<MapView?>(null) }
    val locationOverlay = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    val hasLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission.value = isGranted
        }
    )

    remember(context) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    LaunchedEffect(mainViewModel.posts) {
        mapView.value?.invalidate()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission.value) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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
                    MapView(ctx).also {
                        it.setTileSource(TileSourceFactory.MAPNIK)
                        it.setMultiTouchControls(true)
                        it.setBuiltInZoomControls(true)
                        // Set initial view to Buenos Aires
                        it.controller.setCenter(GeoPoint(-34.6037, -58.3816))
                        it.controller.setZoom(12.0)
                        mapView.value = it
                    }
                },
                update = { view ->
                    view.overlays.removeIf { it !is MyLocationNewOverlay }

                    val customIconDrawable = ContextCompat.getDrawable(view.context, R.drawable.outline_map_pin_heart_24)

                    mainViewModel.posts.forEach { post ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(post.latitude, post.longitude)
                            icon = customIconDrawable
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = post.description
                            snippet = post.location
                            relatedObject = post
                            infoWindow = CustomInfoWindow(view, onPostClicked)
                        }
                        view.overlays.add(marker)
                    }

                    val locationManager = view.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (hasLocationPermission.value && (isGpsEnabled || isNetworkEnabled)) {
                        if (locationOverlay.value == null) {
                            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(view.context), view)
                            myLocationOverlay.enableMyLocation()
                            myLocationOverlay.runOnFirstFix {
                                view.post {
                                    view.controller.animateTo(myLocationOverlay.myLocation)
                                    view.controller.setZoom(15.0)
                                }
                            }
                            view.overlays.add(0, myLocationOverlay)
                            locationOverlay.value = myLocationOverlay
                        }
                    } else {
                        locationOverlay.value?.let {
                            it.disableMyLocation()
                            view.overlays.remove(it)
                            locationOverlay.value = null
                        }
                    }

                    view.invalidate()
                }
            )
        }
    }

    DisposableEffect(lifecycleOwner, mapView.value) {
        val currentMapView = mapView.value
        val currentObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    currentMapView?.onResume()
                    if (hasLocationPermission.value) locationOverlay.value?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    currentMapView?.onPause()
                    locationOverlay.value?.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(currentObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(currentObserver)
            currentMapView?.onPause()
            locationOverlay.value?.disableMyLocation()
            currentMapView?.onDetach()
        }
    }
}

class CustomInfoWindow(mapView: MapView, private val onPostClicked: (String) -> Unit) : InfoWindow(R.layout.info_window, mapView) {
    override fun onOpen(item: Any?) {
        val marker = item as? Marker
        val post = marker?.relatedObject as? Post

        if (marker != null && post != null) {
            val titleView = mView.findViewById<TextView>(R.id.info_window_title)
            val snippetView = mView.findViewById<TextView>(R.id.info_window_snippet)

            titleView.text = marker.title
            snippetView.text = marker.snippet

            mView.setOnClickListener {
                onPostClicked(post.id)
                close()
            }
        } else {
            close()
        }
    }

    override fun onClose() {
        // Clean up resources if needed
    }
}