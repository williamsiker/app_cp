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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
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

    // Efecto para manejar el scroll cuando aparece el teclado
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
                        isUnsaved = false
                    ))
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Archivos Abiertos",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn {
                    items(openFiles) { file ->
                        ListItem(
                            headlineContent = { 
                                Text(file.name + if (file.isUnsaved) "*" else "") 
                            },
                            modifier = Modifier.clickable {
                                viewModel.setCurrentFile(file)
                                scope.launch { drawerState.close() }
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.closeFile(file) }) {
                                    Icon(Icons.Default.Close, "Cerrar archivo")
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentFile?.name ?: "Sin archivo") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onConfigNavigation() }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Configuración")
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
                                            // Guardar en el archivo existente
                                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                                stream.write(currentFile.content.text.toByteArray())
                                                viewModel.updateFile(currentFile.copy(isUnsaved = false))
                                            }
                                        } ?: run {
                                            // Si no tiene URI, abrir diálogo para guardar como
                                            saveFileLauncher.launch(currentFile.name, FileUtils.getMimeType(currentFile.name))
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
                currentFile?.let { file ->
                    EditorTextField(
                        textState = file.content,
                        scrollState = rememberScrollState(),
                        onScroll = { delta -> scope.launch { scrollState.scrollBy(delta) } },
                        onTextChanged = { newContent ->
                            viewModel.updateFile(file.copy(
                                content = newContent,
                                isUnsaved = true
                            ))
                        }
                    )
                } ?: Text(
                    "No hay archivo abierto",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }

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
                                isUnsaved = true
                            )
                            viewModel.setCurrentFile(newFile)
                            saveFileLauncher.launch(newFileName, FileUtils.getMimeType(newFileName))
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