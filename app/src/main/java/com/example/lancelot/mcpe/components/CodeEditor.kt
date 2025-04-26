package com.example.lancelot.mcpe.components

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lancelot.mcpe.viewmodel.EditorViewModel
import com.example.lancelot.mcpe.viewmodel.CodeFile
import com.example.lancelot.mcpe.model.TextState
import com.example.lancelot.rust.RustBridge
import com.example.lancelot.utils.FileUtils
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Import withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

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
        contract = ActivityResultContracts.CreateDocument(currentFile?.mimeType ?: "text/plain") // Use correct mime type
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
                        HorizontalFabMenu()
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                // Use the launcher which now calls the ViewModel
                                openFileLauncher.launch(arrayOf("*/*")) // Allow any file type initially
                            }
                        ) {
                            Icon(Icons.Default.FolderOpen, "Abrir archivo")
                        }
                        SmallFloatingActionButton(
                            onClick = { showNewFileDialog = true }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, "Nuevo archivo")
                        }
                        // Check if the *selected* file can be saved
                        if (currentFile != null) {
                            SmallFloatingActionButton(
                                onClick = {
                                    // Delegate saving to ViewModel
                                    viewModel.saveCurrentFile(context.contentResolver) { suggestedName ->
                                        // This lambda is called if "Save As" is needed
                                        saveFileLauncher.launch(suggestedName)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Save, "Guardar archivo")
                            }
                            SmallFloatingActionButton(
                                onClick = { showBottomSheet = true }
                            ) {
                                Icon(Icons.Default.Code, "Insertar bloque de código")
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded }
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (fabExpanded) 45f else 0f,
                        label = "FAB rotation"
                    )
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Expandir",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        }
    ) { innerPadding ->
        // Estado local para el índice seleccionado para evitar condiciones de carrera
        val openFiles = editorState.openFiles
        val currentSelectedIndex = editorState.selectedIndex
        
        // Validar el índice antes de la composición
        val safeIndex = remember(openFiles.size, currentSelectedIndex) {
            currentSelectedIndex.coerceIn(0, maxOf(0, openFiles.size - 1))
        }

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (openFiles.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(
                        selectedTabIndex = safeIndex,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty() && safeIndex < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[safeIndex])
                                )
                            }
                        }
                    ) {
                        // Asegurarnos de que la lista no cambie durante la composición
                        val stableFiles = remember(openFiles) { openFiles.toList() }
                        stableFiles.forEachIndexed { index, file ->
                            key(file.uri ?: file.name) {
                                Tab(
                                    selected = index == safeIndex,
                                    onClick = { 
                                        if (index != safeIndex) {
                                            scope.launch {
                                                viewModel.selectFile(index)
                                            }
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = file.name + if (file.isUnsaved) "*" else "",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    icon = {
                                        if (stableFiles.size > 1 || file.uri != null) {
                                            IconButton(
                                                onClick = { 
                                                    scope.launch {
                                                        viewModel.closeFile(index)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close ${file.name}",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                )
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
                    modifier = Modifier.fillMaxSize().padding(16.dp),
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