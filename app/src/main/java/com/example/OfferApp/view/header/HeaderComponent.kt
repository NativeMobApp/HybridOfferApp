package com.example.OfferApp.view.header

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.OfferApp.R

@Composable
fun Header(
    onSesionClicked: () -> Unit,
    onProfileClick: () -> Unit,
    username: String,
    modifier: Modifier = Modifier,
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    onBackClicked: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFFD32F2F),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                }
            } else if (onBackClicked != null) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            }
            
            if (query != null && onQueryChange != null) {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Perfil / Cerrar Sesión",
                        tint = Color.White
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Hola, $username") },
                        enabled = false, 
                        onClick = {}
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Ver Perfil") },
                        onClick = {
                            onProfileClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión") },
                        onClick = {
                            onSesionClicked()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxHeight()) {
                    Text("Buscar productos...", color = Color.Gray)
                }
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 18.sp, color = Color.Black)
        )
    }
}