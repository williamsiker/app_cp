package com.example.lancelot.mcpe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalFabMenu(
    onFileOpener: () -> Unit,
    onCreateNewFile: () -> Unit,
    flagFileNN: Boolean,
    onSaveFile: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = expanded,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 2 }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = 100)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(durationMillis = 300, delayMillis = 100)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 200)
        ) + shrinkHorizontally(
            targetWidth = { it / 2 }
        )
    ) {
        Row {
            IconButton (
                onClick = { onFileOpener() },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open file")
            }

            IconButton (
                onClick = { onCreateNewFile() },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Limpiar")
            }
            if(flagFileNN) {
                IconButton (
                    onClick = { onSaveFile() }
                ) {
                    Icon(Icons.Default.Save, "Guardar archivo")
                }
            }
        }
    }

    // FAB principal que despliega o cierra el menú
    IconButton(
        onClick = { expanded = !expanded },
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            label = "FAB rotation"
        )
        Icon(
            Icons.Default.Add,
            contentDescription = "Expandir",
            modifier = Modifier.rotate(rotation)
        )
    }
}
