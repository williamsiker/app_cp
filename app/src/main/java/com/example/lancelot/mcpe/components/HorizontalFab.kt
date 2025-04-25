package com.example.lancelot.mcpe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalFabMenu() {
    var expanded by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = expanded) {
        Row {
            IconButton (
                onClick = { /* Acción 1 */ },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Ejecutar")
            }

            IconButton (
                onClick = { /* Acción 2 */ },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar")
            }
        }
    }

    // FAB principal que despliega o cierra el menú
    IconButton(
        onClick = { expanded = !expanded },
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
            contentDescription = "Menú"
        )
    }
}
