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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lancelot.mcpe.viewmodel.EditorViewModel
import com.example.lancelot.mcpe.viewmodel.CodeFile
import com.example.lancelot.mcpe.model.TextState
import com.example.lancelot.rust.RustBridge
import com.example.lancelot.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

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

    fun getFileName(uri: Uri): String {
        uri.lastPathSegment?.let { fullPath ->
            return File(fullPath).name
        }
        return "untitled"
    }
    
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val content = stream.bufferedReader().use { it.readText() }
                val fileName = getFileName(uri)
                viewModel.setCurrentFile(
                    CodeFile(
                        name = fileName,
                        content = TextState(content),
                        uri = uri
                    )
                )
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(currentFile?.content?.text?.toByteArray() ?: ByteArray(0))
                currentFile?.let { file ->
                    viewModel.updateFile(file.copy(
                        uri = uri,
                        name = getFileName(uri),
                        isUnsaved = false,
                        mimeType = context.contentResolver.getType(uri) ?: FileUtils.getMimeType(getFileName(uri))
                    ))
                }
            }
        }
    }

    // --- INICIO CAMBIO: TabRow para archivos abiertos ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFile?.name ?: "untitled") },
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
                            onClick = { openFileLauncher.launch(arrayOf("text/*")) }
                        ) {
                            Icon(Icons.Default.FolderOpen, "Abrir archivo")
                        }
                        SmallFloatingActionButton(
                            onClick = { showNewFileDialog = true }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, "Nuevo archivo")
                        }
                        if (currentFile != null) {
                            SmallFloatingActionButton(
                                onClick = {
                                    currentFile.uri?.let { uri ->
                                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                                            stream.write(currentFile.content.text.toByteArray())
                                            viewModel.updateFile(currentFile.copy(isUnsaved = false))
                                        }
                                    } ?: run {
                                        saveFileLauncher.launch(currentFile.name)
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
        Box(modifier = Modifier.padding(innerPadding)) {
            if (openFiles.isNotEmpty()) {
                val selectedTabIndex = openFiles.indexOfFirst { it.name == currentFile?.name }.takeIf { it >= 0 } ?: 0
                Column {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp
                    ) {
                        openFiles.forEachIndexed { index, file ->
                            Tab(
                                selected = index == selectedTabIndex,
                                onClick = { viewModel.setCurrentFile(file) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(file.name + if (file.isUnsaved) "*" else "")
                                        IconButton(
                                            onClick = { viewModel.closeFile(file) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cerrar archivo", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                    if (openFiles.getOrNull(selectedTabIndex) != null) {
                        val file = openFiles[selectedTabIndex]
                        EditorTextField(
                            textState = file.content,
                            scrollState = rememberScrollState(),
                            onScroll = { delta -> scope.launch { scrollState.scrollBy(delta) } },
                            onTextChanged = { newContent ->
                                viewModel.updateFile(file.copy(
                                    content = newContent,
                                    isUnsaved = true
                                ))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            "No hay archivo abierto",
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                Text(
                    "No hay archivo abierto",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
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
                                isUnsaved = true,
                                mimeType = FileUtils.getMimeType(newFileName)
                            )
                            viewModel.setCurrentFile(newFile)
                            saveFileLauncher.launch(newFileName)
                            showNewFileDialog = false
                            newFileName = ""
                        }
                    }
                ) {
                    Text("Crear")
                }
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