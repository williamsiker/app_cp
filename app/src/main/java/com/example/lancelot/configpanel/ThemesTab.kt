package com.example.lancelot.configpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lancelot.CodeThemeEntity
import com.example.lancelot.theme.ThemeManager
import com.example.lancelot.utils.ColorWheel
import com.example.lancelot.utils.toColor
import com.example.lancelot.utils.toHexString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesTab(
    themes: List<CodeThemeEntity>,
    onAddTheme: (String, String) -> Unit,
    onDeleteTheme: (Long) -> Unit,
    onSelectTheme: (CodeThemeEntity) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf<CodeThemeEntity?>(null) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Theme")
            }
        }

        LazyColumn {
            items(themes) { theme ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = { onSelectTheme(theme) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(theme.name, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Row {
                            val json = org.json.JSONObject(theme.colorsJson)
                            json.keys().asSequence().take(5).forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(android.graphics.Color.parseColor(json.getString(key))))
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                        IconButton(onClick = { showDelete = theme }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var themeName by remember { mutableStateOf("") }
        val tokens = remember { listOf("keyword","function","type","string","comment") }
        val colorMap = remember { mutableStateMapOf<String, Color>() }
        tokens.forEach { if (!colorMap.containsKey(it)) colorMap[it] = Color.White }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New Theme") },
            text = {
                Column {
                    OutlinedTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    tokens.forEach { token ->
                        var showPicker by remember { mutableStateOf(false) }
                        Text(token)
                        Button(
                            onClick = { showPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colorMap[token]!!)
                        ) { Text("Pick Color") }
                        Spacer(Modifier.height(8.dp))
                        if (showPicker) {
                            AlertDialog(
                                onDismissRequest = { showPicker = false },
                                title = { Text("Color for $token") },
                                text = {
                                    Box(modifier = Modifier.size(250.dp)) {
                                        ColorWheel(
                                            modifier = Modifier.fillMaxSize(),
                                            initialColor = colorMap[token]!!,
                                            onColorChanged = { hsv -> colorMap[token] = hsv.toColor() }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showPicker = false }) { Text("OK") }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = buildString {
                        append("{")
                        colorMap.entries.joinToString(",") { "\"${it.key}\":\"${it.value.toHexString()}\"" }
                            .let { append(it) }
                        append("}")
                    }
                    onAddTheme(themeName, json)
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }

    showDelete?.let { theme ->
        AlertDialog(
            onDismissRequest = { showDelete = null },
            title = { Text("Delete Theme") },
            text = { Text("Delete ${theme.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTheme(theme.id)
                    showDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = null }) { Text("Cancel") }
            }
        )
    }
}
