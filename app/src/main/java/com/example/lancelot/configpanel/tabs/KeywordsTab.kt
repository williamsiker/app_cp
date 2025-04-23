package com.example.lancelot.configpanel.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lancelot.*
import com.example.lancelot.configpanel.util.ConfigImporter
import com.example.lancelot.configpanel.viewmodel.ConfigurationState
import org.koin.compose.getKoin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { Text(contentDescription) },
        state = rememberTooltipState()
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordsTab(
    state: ConfigurationState,
    onLanguageSelect: (Languages?) -> Unit,
    onAddKeyword: (String, Long, Long?, Long?) -> Unit,
    onDeleteKeyword: (Long) -> Unit,
    onAddGroup: (String, Long, Long?) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onUpdateGroupStyle: (Long, Long?) -> Unit,
    onGroupSelect: (KeywordGroup?) -> Unit,
    onStyleSelect: (Styles?) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf<KeywordGroup?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Keywords?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf<KeywordGroup?>(null) }
    var newKeyword by remember { mutableStateOf("") }
    var newGroupName by remember { mutableStateOf("") }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }
    var isStyleDropdownExpanded by remember { mutableStateOf(false) }

    val configImporter = getKoin().get<ConfigImporter>()
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (state.selectedLanguage != null) {
                scope.launch {
                    configImporter.importKeywords(uri, state.selectedLanguage.id, snackbarHostState)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Please select a language first")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Language Selector
            ExposedDropdownMenuBox(
                expanded = isLanguageDropdownExpanded,
                onExpandedChange = { isLanguageDropdownExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.selectedLanguage?.name ?: "Select Language",
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isLanguageDropdownExpanded,
                    onDismissRequest = { isLanguageDropdownExpanded = false }
                ) {
                    state.languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.name) },
                            onClick = {
                                onLanguageSelect(language)
                                onGroupSelect(null)
                                isLanguageDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            state.selectedLanguage?.let { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Keyword Groups",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionIconButton(
                            onClick = { importLauncher.launch("application/json") },
                            icon = Icons.Default.Upload,
                            contentDescription = "Import Keywords"
                        )
                        ActionIconButton(
                            onClick = { showAddGroupDialog = true },
                            icon = Icons.Default.CreateNewFolder,
                            contentDescription = "New Group"
                        )
                    }
                }

                // Groups List with Keywords
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.groups) { group ->
                        val isSelected = group == state.selectedGroup
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            onClick = { onGroupSelect(if (isSelected) null else group) }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Group Header
                                ListItem(
                                    headlineContent = { Text(group.name) },
                                    supportingContent = { 
                                        group.styleId?.let { styleId ->
                                            state.styles.find { it.id == styleId }?.let { style ->
                                                Text("Style: ${style.name}")
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = { showStyleDialog = group }) {
                                                Icon(
                                                    Icons.Default.Style,
                                                    contentDescription = "Change style"
                                                )
                                            }
                                            IconButton(onClick = { showDeleteGroupDialog = group }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete group"
                                                )
                                            }
                                            IconButton(onClick = { onGroupSelect(if (isSelected) null else group) }) {
                                                Icon(
                                                    if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = if (isSelected) "Collapse" else "Expand"
                                                )
                                            }
                                        }
                                    }
                                )

                                // Animated Keywords Section
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    ) {
                                        // Keywords List
                                        state.keywords.filter { it.groupId == group.id }.forEach { keyword ->
                                            ListItem(
                                                headlineContent = { Text(keyword.keyword) },
                                                trailingContent = {
                                                    IconButton(onClick = { showDeleteDialog = keyword }) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Delete keyword"
                                                        )
                                                    }
                                                }
                                            )
                                        }

                                        // Add Keyword Button
                                        ActionIconButton(
                                            onClick = { showAddDialog = true },
                                            icon = Icons.Default.Add,
                                            contentDescription = "Add Keyword",
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Style Selection Dialog
    showStyleDialog?.let { group ->
        AlertDialog(
            onDismissRequest = { 
                showStyleDialog = null
                onStyleSelect(null)
            },
            title = { Text("Change Group Style") },
            text = {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = isStyleDropdownExpanded,
                        onExpandedChange = { isStyleDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.selectedStyle?.name ?: "Select Style",
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isStyleDropdownExpanded,
                            onDismissRequest = { isStyleDropdownExpanded = false }
                        ) {
                            state.styles.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.name) },
                                    onClick = {
                                        onStyleSelect(style)
                                        isStyleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateGroupStyle(group.id, state.selectedStyle?.id)
                        showStyleDialog = null
                        onStyleSelect(null)
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showStyleDialog = null
                    onStyleSelect(null)
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Group Dialog
    if (showAddGroupDialog && state.selectedLanguage != null) {
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("New Keyword Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = isStyleDropdownExpanded,
                        onExpandedChange = { isStyleDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.selectedStyle?.name ?: "Select Style (Optional)",
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isStyleDropdownExpanded,
                            onDismissRequest = { isStyleDropdownExpanded = false }
                        ) {
                            state.styles.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.name) },
                                    onClick = {
                                        onStyleSelect(style)
                                        isStyleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onAddGroup(
                                newGroupName,
                                state.selectedLanguage.id,
                                state.selectedStyle?.id
                            )
                            showAddGroupDialog = false
                            newGroupName = ""
                            onStyleSelect(null)
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Keyword Dialog
    if (showAddDialog && state.selectedLanguage != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Keyword") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        label = { Text("Keyword") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = newKeyword.isBlank()
                    )
                    if (newKeyword.isBlank()) {
                        Text(
                            text = "Keyword is required",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newKeyword.isNotBlank()) {
                            onAddKeyword(
                                newKeyword,
                                state.selectedLanguage.id,
                                state.selectedGroup?.id,
                                state.selectedStyle?.id
                            )
                            showAddDialog = false
                            newKeyword = ""
                            onStyleSelect(null)
                        }
                    },
                    enabled = newKeyword.isNotBlank()
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

    // Delete Keyword Dialog
    showDeleteDialog?.let { keyword ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Keyword") },
            text = { 
                Text("Are you sure you want to delete '${keyword.keyword}'?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteKeyword(keyword.id)
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

    // Delete Group Dialog
    showDeleteGroupDialog?.let { group ->
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            title = { Text("Delete Group") },
            text = { 
                Text("Are you sure you want to delete the group '${group.name}'? This will also delete all keywords in this group.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGroup(group.id)
                        showDeleteGroupDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}