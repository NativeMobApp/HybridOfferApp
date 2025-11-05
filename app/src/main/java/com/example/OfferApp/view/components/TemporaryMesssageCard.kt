package com.example.OfferApp.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TemporaryMessageCard(
    message: String,
    backgroundColor: Color = Color(0xFFFFA726), // Naranja suave (Amber 600)
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier // 👈 agregado para mayor flexibilidad
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(message) {
        delay(3000) // visible 3 segundos
        visible = false
        onDismiss()
    }

    AnimatedVisibility(visible = visible) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier // 👈 ahora usa el modifier que le pases desde afuera
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
