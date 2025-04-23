package com.example.lancelot.configpanel.tabs

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
import androidx.compose.ui.unit.dp
import com.example.lancelot.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagesTab(languageDAO: LanguageDAO) {
    var languages by remember { mutableStateOf(emptyList<Languages>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Languages?>(null) }
    var newLanguageName by remember { mutableStateOf("") }
    var newLanguageDescription by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        languages = languageDAO.getAllLanguages()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Add Language Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Language")
        }

        // Languages List
        LazyColumn {
            items(languages) { language ->
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
                                text = language.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            language.description?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = language }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete language",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Language") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newLanguageName,
                        onValueChange = { newLanguageName = it },
                        label = { Text("Language Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newLanguageDescription,
                        onValueChange = { newLanguageDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newLanguageName.isNotBlank()) {
                            scope.launch {
                                languageDAO.insertLanguages(
                                    Languages(
                                        name = newLanguageName,
                                        description = newLanguageDescription.takeIf { it.isNotBlank() }
                                    )
                                )
                                languages = languageDAO.getAllLanguages()
                                showAddDialog = false
                                newLanguageName = ""
                                newLanguageDescription = ""
                            }
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

    // Delete Confirmation Dialog
    showDeleteDialog?.let { language ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Language") },
            text = { 
                Text("Are you sure you want to delete '${language.name}'? This will also delete all associated keywords.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            languageDAO.deleteLanguage(language.id)
                            languages = languageDAO.getAllLanguages()
                            showDeleteDialog = null
                        }
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