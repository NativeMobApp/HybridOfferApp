package com.example.OfferApp.view.header

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.OfferApp.R

@Composable
fun Header(
    onSesionClicked: () -> Unit,
    onProfileClick: () -> Unit,
    username: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    onBackClicked: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    onLogoClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconTint = MaterialTheme.colorScheme.onPrimary

            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menú", tint = iconTint)
                }
            } else if (onBackClicked != null) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = iconTint)
                }
            }

            if (onLogoClick != null) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onLogoClick)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_percent_discount_24),
                        contentDescription = "Logo de OfferApp",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            if (query != null && onQueryChange != null) {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1f)
                )
            } else if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Perfil / Cerrar Sesión",
                        tint = iconTint
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Hola, $username", fontWeight = FontWeight.Bold) },
                        enabled = false,
                        onClick = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar productos...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface
        ),
        textStyle = MaterialTheme.typography.bodyLarge,

    )
}
