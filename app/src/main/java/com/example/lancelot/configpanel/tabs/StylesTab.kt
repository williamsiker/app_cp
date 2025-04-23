package com.example.lancelot.configpanel.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lancelot.*
import com.example.lancelot.configpanel.util.ConfigImporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylesTab(
    styles: List<Styles>,
    onAddStyle: (String, String, Int, Boolean, Boolean, Boolean) -> Unit,
    onDeleteStyle: (Long) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Styles?>(null) }
    var newStyleName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var showColorPicker by remember { mutableStateOf(false) }
    var newStyleSize by remember { mutableStateOf("12") }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val configImporter = remember { ConfigImporter(context, DatabaseProvider.getDatabase(context)) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                configImporter.importStyles(uri, snackbarHostState)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { importLauncher.launch("application/json") }
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Styles")
                }
                
                Button(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Style")
                }
            }

            LazyColumn {
                items(styles) { style ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(android.graphics.Color.parseColor(style.color)))
                                    )
                                    Text(
                                        text = "Size: ${style.sizeFont}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            IconButton(onClick = { showDeleteDialog = style }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete style",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Style") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newStyleName,
                        onValueChange = { newStyleName = it },
                        label = { Text("Style Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedColor
                        )
                    ) {
                        Text(
                            "Select Color",
                            color = if (selectedColor.luminance() > 0.5f) Color.Black else Color.White
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newStyleSize,
                        onValueChange = { newStyleSize = it.filter { it.isDigit() } },
                        label = { Text("Font Size") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isBold,
                            onCheckedChange = { isBold = it }
                        )
                        Text("Bold")
                        Spacer(Modifier.width(8.dp))
                        Checkbox(
                            checked = isItalic,
                            onCheckedChange = { isItalic = it }
                        )
                        Text("Italic")
                        Spacer(Modifier.width(8.dp))
                        Checkbox(
                            checked = isUnderline,
                            onCheckedChange = { isUnderline = it }
                        )
                        Text("Underline")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newStyleName.isNotBlank() && newStyleSize.isNotBlank()) {
                            onAddStyle(
                                newStyleName,
                                selectedColor.toHexString(),
                                newStyleSize.toIntOrNull() ?: 12,
                                isBold,
                                isItalic,
                                isUnderline
                            )
                            showAddDialog = false
                            newStyleName = ""
                            selectedColor = Color.White
                            newStyleSize = "12"
                            isBold = false
                            isItalic = false
                            isUnderline = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Select Color") },
            text = {
                Box(modifier = Modifier.size(300.dp)) {
                    ColorWheel(
                        modifier = Modifier.fillMaxSize(),
                        initialColor = selectedColor,
                        onColorChanged = { hsvColor ->
                            selectedColor = hsvColor.toColor()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("OK")
                }
            }
        )
    }

    showDeleteDialog?.let { style ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Style") },
            text = { 
                Text("Are you sure you want to delete '${style.name}'?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStyle(style.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun Color.toHexString(): String {
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return String.format("#%02x%02x%02x", red, green, blue)
}