package com.example.OfferApp.view.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.OfferApp.view.header.Header
import com.example.OfferApp.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    mainViewModel: MainViewModel,
    onPostCreated: () -> Unit,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onProfileClick: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var originalPrice by remember { mutableStateOf("") }
    var finalPrice by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var lastEditedField by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Alimentos", "Tecnología", "Moda", "Deportes", "Construcción",
        "Animales", "Electrodomésticos", "Servicios", "Educación",
        "Juguetes", "Vehículos", "Otros"
    )
    var product by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val promotionTypes = listOf(
        "2x1", "3x1", "3x2", "25% OFF", "30% OFF", "50% OFF", "Liquidación", "Otros"
    )
    var selectedPromotionType by remember { mutableStateOf("") }
    var expandedPromotion by remember { mutableStateOf(false) }
    var otherPromotion by remember { mutableStateOf("") }
    val finalPromotionType = if (selectedPromotionType == "Otros") otherPromotion else selectedPromotionType

    val autoDescription by remember(product, brand, finalPromotionType) {
        derivedStateOf {
            val promotionDetail = if (finalPromotionType.isNotBlank()) "$finalPromotionType" else ""
            val productText = if (product.isNotBlank()) product else ""
            val brandText = if (brand.isNotBlank()) "de $brand" else ""

            if (finalPromotionType.isNotBlank() || product.isNotBlank() || brand.isNotBlank()) {
                "$promotionDetail en $productText $brandText".trim().replace(Regex("\\s+"), " ")
            } else {
                ""
            }
        }
    }
    description = autoDescription

    LaunchedEffect(originalPrice, selectedPromotionType) {
        if (lastEditedField == "original" && selectedPromotionType != "Liquidación" && selectedPromotionType != "Otros") {
            val priceDouble = originalPrice.toDoubleOrNull()
            if (priceDouble != null) {
                val newFinalPrice = when (selectedPromotionType) {
                    "2x1" -> priceDouble / 2
                    "3x1" -> priceDouble / 3
                    "3x2" -> priceDouble * 2 / 3
                    "25% OFF" -> priceDouble * 0.75
                    "30% OFF" -> priceDouble * 0.70
                    "50% OFF" -> priceDouble * 0.50
                    else -> null
                }
                finalPrice = newFinalPrice?.let { "%.2f".format(it) } ?: ""
            } else {
                finalPrice = ""
            }
        }
    }

    LaunchedEffect(finalPrice, selectedPromotionType) {
        if (lastEditedField == "final" && selectedPromotionType != "Liquidación" && selectedPromotionType != "Otros") {
            val finalPriceDouble = finalPrice.toDoubleOrNull()
            if (finalPriceDouble != null) {
                val newOriginalPrice = when (selectedPromotionType) {
                    "2x1" -> finalPriceDouble * 2
                    "3x1" -> finalPriceDouble * 3
                    "3x2" -> finalPriceDouble * 3 / 2
                    "25% OFF" -> finalPriceDouble / 0.75
                    "30% OFF" -> finalPriceDouble / 0.70
                    "50% OFF" -> finalPriceDouble / 0.50
                    else -> null
                }
                originalPrice = newOriginalPrice?.let { "%.2f".format(it) } ?: ""
            } else {
                originalPrice = ""
            }
        }
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 && longitude != 0.0) {
            location = getCityName(context, latitude, longitude) ?: "Ubicación Desconocida"
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                getCurrentLocation(context) { lat, long ->
                    latitude = lat
                    longitude = long
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation(context) { lat, long ->
                latitude = lat
                longitude = long
            }
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (showCamera) {
        CameraView(onImageCaptured = {
            imageUri = it
            showCamera = false
        })
    } else {
        Scaffold(
            topBar = {
                Header(
                    username = mainViewModel.user.username,
                    title = "Crear Post",
                    onProfileClick = onProfileClick,
                    onSesionClicked = onLogoutClicked,
                    onBackClicked = onBackClicked
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedPromotion,
                        onExpandedChange = { expandedPromotion = !expandedPromotion }
                    ) {
                        OutlinedTextField(
                            value = selectedPromotionType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Promoción") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPromotion) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPromotion,
                            onDismissRequest = { expandedPromotion = false }
                        ) {
                            promotionTypes.forEach { promotion ->
                                DropdownMenuItem(
                                    text = { Text(promotion) },
                                    onClick = {
                                        selectedPromotionType = promotion
                                        expandedPromotion = false
                                        // Recalculate based on the last edited field, except for Liquidación and Otros
                                        if (promotion != "Liquidación" && promotion != "Otros") {
                                            if (lastEditedField == "final" && finalPrice.isNotBlank()) {
                                                val finalPriceDouble = finalPrice.toDoubleOrNull()
                                                if (finalPriceDouble != null) {
                                                    val newOriginalPrice = when (promotion) {
                                                        "2x1" -> finalPriceDouble * 2
                                                        "3x1" -> finalPriceDouble * 3
                                                        "3x2" -> finalPriceDouble * 3 / 2
                                                        "25% OFF" -> finalPriceDouble / 0.75
                                                        "30% OFF" -> finalPriceDouble / 0.70
                                                        "50% OFF" -> finalPriceDouble / 0.50
                                                        else -> null
                                                    }
                                                    originalPrice = newOriginalPrice?.let { "%.2f".format(it) } ?: ""
                                                }
                                            } else if (originalPrice.isNotBlank()) {
                                                val priceDouble = originalPrice.toDoubleOrNull()
                                                if (priceDouble != null) {
                                                    val newFinalPrice = when (promotion) {
                                                        "2x1" -> priceDouble / 2
                                                        "3x1" -> priceDouble / 3
                                                        "3x2" -> priceDouble * 2 / 3
                                                        "25% OFF" -> priceDouble * 0.75
                                                        "30% OFF" -> priceDouble * 0.70
                                                        "50% OFF" -> priceDouble * 0.50
                                                        else -> null
                                                    }
                                                    finalPrice = newFinalPrice?.let { "%.2f".format(it) } ?: ""
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedPromotionType == "Otros") {
                        OutlinedTextField(
                            value = otherPromotion,
                            onValueChange = { otherPromotion = it },
                            label = { Text("Especifique la Promoción") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = product,
                        onValueChange = { product = it },
                        label = { Text("Producto") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = store,
                        onValueChange = { store = it },
                        label = { Text("Comercio (Tienda)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = originalPrice,
                        onValueChange = {
                            originalPrice = it
                            lastEditedField = "original"
                        },
                        label = { Text("Precio Original") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = finalPrice,
                        onValueChange = {
                            finalPrice = it
                            lastEditedField = "final"
                        },
                        label = { Text("Precio Final (con descuento)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    imageUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it),
                            contentDescription = "Imagen de la publicación",
                            modifier = Modifier.size(150.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Button(onClick = { showCamera = true }, enabled = !isLoading) {
                        Text("Tomar foto")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            imageUri?.let { uri ->
                                scope.launch {
                                    isLoading = true
                                    val finalStore = if (store.isNotBlank()) store else "desconocido"
                                    val result = mainViewModel.addPost(
                                        description = description,
                                        imageUri = uri,
                                        location = location,
                                        latitude = latitude,
                                        longitude = longitude,
                                        category = selectedCategory,
                                        price = originalPrice.toDoubleOrNull() ?: 0.0,
                                        discountPrice = finalPrice.toDoubleOrNull() ?: 0.0,
                                        store = finalStore
                                    )
                                    if (result.isSuccess) {
                                        onPostCreated()
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        enabled = imageUri != null &&
                                selectedCategory.isNotBlank() &&
                                originalPrice.isNotBlank() &&
                                finalPrice.isNotBlank() &&
                                (finalPrice.toDoubleOrNull() ?: 0.0) < (originalPrice.toDoubleOrNull() ?: Double.MAX_VALUE) &&
                                !isLoading
                    ) {
                        Text("Guardar")
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun getCurrentLocation(context: Context, callback: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callback(location.latitude, location.longitude)
            }
        }
    } catch (e: SecurityException) {
        // Handle exception
    }
}

private fun getCityName(context: Context, latitude: Double, longitude: Double): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        addresses?.firstOrNull()?.let { address ->
            address.subLocality ?: address.locality ?: address.subAdminArea ?: address.adminArea
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}