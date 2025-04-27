package com.example.lancelot.mcpe.components

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lancelot.mcpe.CodeFile
import com.example.lancelot.mcpe.EditorViewModel
import com.example.lancelot.mcpe.TextState
import com.example.lancelot.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScaffold(
    viewModel: EditorViewModel = viewModel(),
    onConfigNavigation: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var fabExpanded by remember { mutableStateOf(false) }
    
    val editorState by viewModel.state.collectAsState()
    val currentFile = editorState.currentFile
    val openFiles = editorState.openFiles
    val scrollState = rememberScrollState()
    val windowInsets = WindowInsets.ime
    val density = LocalDensity.current
    val imeVisible by remember { derivedStateOf { windowInsets.getBottom(density) > 0 } }
    val lastSelectionRef = remember { mutableStateOf<TextRange?>(null) }
    val configuration = LocalConfiguration.current

    var isOnSnippet = remember {mutableStateOf(false)}

    LaunchedEffect(imeVisible, currentFile?.content?.selection) {
        currentFile?.content?.let { textState ->
            if (imeVisible && textState.selection != TextRange.Zero && textState.selection != lastSelectionRef.value) {
                lastSelectionRef.value = textState.selection
                textState.textLayoutResult?.let { layoutResult ->
                    val selectionLine = layoutResult.getLineForOffset(textState.selection.start)
                    val lineHeight = textState.lineHeight
                    val visibleLines = with(density) {
                        (configuration.screenHeightDp.dp.toPx() - windowInsets.getBottom(density)) / lineHeight
                    }
                    
                    val targetScrollPosition = (selectionLine * lineHeight) - (visibleLines * lineHeight / 2)
                    scope.launch {
                        scrollState.scrollTo(targetScrollPosition.toInt().coerceAtLeast(0))
                    }
                }
            }
        }
    }

    // --- Move I/O Launchers Logic to ViewModel Calls ---
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Delegate loading to ViewModel on IO thread
            viewModel.loadAndOpenFile(it, context.contentResolver)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument(currentFile?.mimeType ?: "text/plain") // Use correct mime type
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            currentFile?.let { fileToSave ->
                // Delegate saving to ViewModel on IO thread
                viewModel.saveFileToUri(fileToSave, targetUri, context.contentResolver)
            }
        }
    }

    // --- INICIO CAMBIO: TabRow para archivos abiertos ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Display name of the *actually* selected file
                    Text(
                        "Editor",  // Changed to a generic title since tabs will be moved
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                 },
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = { onConfigNavigation() }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Configuración")
                        }
                        HorizontalFabMenu(
                            onFileOpener = {openFileLauncher.launch(arrayOf("*/*"))},
                            onCreateNewFile = {showNewFileDialog = true},
                            flagFileNN = currentFile == null,
                            onSaveFile = {
                                viewModel.saveCurrentFile(context.contentResolver) { suggestedName ->
                                    // This lambda is called if "Save As" is needed
                                    saveFileLauncher.launch(suggestedName)
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Estado local para el índice seleccionado para evitar condiciones de carrera
        val currentSelectedIndex = editorState.selectedIndex
        
        // Validar el índice antes de la composición
        val safeIndex = remember(openFiles.size, currentSelectedIndex) {
            currentSelectedIndex.coerceIn(0, maxOf(0, openFiles.size - 1))
        }

        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            if (openFiles.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom tab layout
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp), // Altura reducida
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val stableFiles = remember(openFiles) { openFiles.toList() }
                            stableFiles.forEachIndexed { index, file ->
                                key(file.uri ?: file.name) {
                                    Surface(
                                        modifier = Modifier
                                            .widthIn(min = 100.dp, max = 160.dp)
                                            .height(24.dp),
                                        onClick = { 
                                            if (index != safeIndex) {
                                                scope.launch {
                                                    viewModel.selectFile(index)
                                                }
                                            }
                                        },
                                        selected = index == safeIndex,
                                        color = if (index == safeIndex) 
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = file.name + if (file.isUnsaved) "*" else "",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (stableFiles.size > 1 || file.uri != null) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close ${file.name}",
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            scope.launch {
                                                                viewModel.closeFile(index)
                                                            }
                                                        },
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Editor actual
                    editorState.currentFile?.let { file ->
                        EditorTextField(
                            textState = file.content,
                            scrollState = scrollState,
                            onScroll = { delta -> scope.launch { scrollState.scrollBy(delta) } },
                            onTextChanged = { newContent ->
                                scope.launch {
                                    viewModel.updateFileContent(file, newContent)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No files open.")
                }
            }
        }
    }
    // --- FIN CAMBIO: TabRow para archivos abiertos ---

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Nuevo Archivo") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Nombre del archivo con extensión (ej: main.cpp)") },
                        singleLine = true
                    )
                    Text(
                        "Asegúrate de incluir la extensión del archivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val newFile = CodeFile(
                                name = newFileName,
                                isUnsaved = true, // It's unsaved until first save
                                mimeType = FileUtils.getMimeType(newFileName),
                                content = TextState("") // Start with empty content
                            )
                            // Open the new file in the editor state
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    viewModel.openFile(newFile)
                                }
                            }
                            // Immediately trigger "Save As" for the new file
                            saveFileLauncher.launch(newFileName)
                            showNewFileDialog = false
                            newFileName = ""
                        }
                    }
                ) { Text("Crear y Guardar Como...") } // Clarify button action
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showNewFileDialog = false
                        newFileName = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showBottomSheet && currentFile != null) {
        CodeBlockBottomSheet(
            onBlockSelected = { block ->
                // TODO: Implementar la inserción del bloque en el editor
                showBottomSheet = false
            },
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
fun NavigateSnippetActions(
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit
) {
    IconButton(onClick = { onNavigateBack() }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Regresar"
        )
    }
    IconButton(onClick = { onNavigateForward() }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Avanzar"
        )
    }
}