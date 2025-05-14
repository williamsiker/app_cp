package com.example.lancelot.mcpe

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.rust.RustBridge
import com.example.lancelot.rust.Token
import com.example.lancelot.utils.FileUtils // Assuming you have this utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/***
 *  @property name e.g "main.rs"
 *  @property uri is the path to the file on the device
 *  @property mimeType is the mime type for avoid automatic.rename file-extension
 *  @property isUnsaved is true if the file has unsaved changes
 *  @property treePointer is the pointer to the rust-library AST
 *  @property tokens maintains the tokens to highlight in the editor
 *  @see RustBridge for native implementation of syntax-highlight and code-navigation?
 */
data class CodeFile(
    val name: String,
    val content: EditorTextFieldState,
    val uri: Uri? = null,
    val isUnsaved: Boolean = false,
    val mimeType: String = "text/plain"
//    var treePointer: Long = 0L,
//    var tokens: ArrayList<Token> = ArrayList()
)
//{
//    fun update(code: String, languageName: String) {
//        this.treePointer = RustBridge.parseIncremental(code, languageName, treePointer)
//        val tokensRaw  = RustBridge.tokenizeCode(code, languageName, treePointer)
//        tokens.clear()
//        tokens.addAll(tokensRaw)
//    }
//}

data class EditorState(
    val openFiles: List<CodeFile> = listOf(CodeFile(name = "untitled", content = EditorTextFieldState(TextState("")), isUnsaved = true)),
    val currentFile: CodeFile? = openFiles.firstOrNull(),
    val selectedIndex: Int = 0
)

/***
 * @property stateMutex allows you not no block the viewModel scope
 */
class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // Mutex para garantizar operaciones atómicas en el estado (thread-safe?)
    private val stateMutex = Mutex()

    private suspend fun updateStateSync(block: (EditorState) -> EditorState) {
        stateMutex.withLock {
            _state.update { currentState ->
                val updatedState = block(currentState)
                val safeIndex = ensureValidIndex(updatedState.openFiles, updatedState.selectedIndex)
                val safeCurrentFile = if (safeIndex >= 0) updatedState.openFiles.getOrNull(safeIndex) else null
                updatedState.copy(
                    selectedIndex = safeIndex,
                    currentFile = safeCurrentFile
                )
            }
        }
    }

    private fun ensureValidIndex(files: List<CodeFile>, index: Int): Int {
        return when {
            files.isEmpty() -> -1
            index < 0 -> 0
            index >= files.size -> (files.size - 1).coerceAtLeast(0)
            else -> index
        }
    }

    fun selectFile(index: Int) {
        viewModelScope.launch {
            updateStateSync { currentState ->
                currentState.copy(selectedIndex = index)
            }
        }
    }

    fun closeFile(indexToClose: Int) {
        viewModelScope.launch {
            updateStateSync { currentState ->
                if (currentState.openFiles.size <= 1 && currentState.openFiles.firstOrNull()?.uri == null) {
                    return@updateStateSync currentState
                }

                val safeIndexToClose = indexToClose.coerceIn(0, currentState.openFiles.size - 1)
                val updatedFiles = currentState.openFiles.toMutableList().apply {
                    removeAt(safeIndexToClose)
                }

                val finalFiles = if (updatedFiles.isEmpty()) {
                    listOf(CodeFile(name = "untitled", content = EditorTextFieldState(TextState("")), isUnsaved = true))
                } else updatedFiles

                val newSelectedIndex = when {
                    currentState.selectedIndex < safeIndexToClose -> currentState.selectedIndex
                    currentState.selectedIndex == safeIndexToClose -> (safeIndexToClose - 1).coerceAtLeast(0)
                    else -> (currentState.selectedIndex - 1).coerceAtLeast(0)
                }

                currentState.copy(
                    openFiles = finalFiles,
                    selectedIndex = newSelectedIndex.coerceIn(0, finalFiles.size - 1)
                )
            }
        }
    }

    // --- File Operations (with IO Dispatcher) ---

    /**
     * Loads content from URI and opens/updates the file in the editor state.
     * Runs file reading on Dispatchers.IO.
     */
    fun loadAndOpenFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val content = readFileContent(uri, contentResolver)
                val fileName = getFileNameFromUri(uri, contentResolver)
                val mimeType = contentResolver.getType(uri) ?: FileUtils.getMimeType(fileName)

                updateStateSync { currentState ->
                    val existingIndex = currentState.openFiles.indexOfFirst { it.uri == uri }
                    val newFile = CodeFile(
                        name = fileName,
                        content = EditorTextFieldState(TextState(content)),
                        uri = uri,
                        isUnsaved = false,
                        mimeType = mimeType
                    )
                    if (existingIndex >= 0) {
                        currentState.copy(
                            openFiles = currentState.openFiles.toMutableList().apply { this[existingIndex] = newFile },
                            selectedIndex = existingIndex
                        )
                    } else {
                        val newFiles = currentState.openFiles + newFile
                        currentState.copy(
                            openFiles = newFiles,
                            selectedIndex = newFiles.size - 1
                        )
                    }
                }
            } catch (e: IOException) {
                println("Error opening file: $e")
            }
        }
    }

    /**
     * Saves the given file's content to the target URI.
     * Runs file writing on Dispatchers.IO.
     */
    fun saveFileToUri(fileToSave: CodeFile, targetUri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                writeFileContent(targetUri, fileToSave.content.textState.text, contentResolver)
                val newName = getFileNameFromUri(targetUri, contentResolver)
                val newMimeType = contentResolver.getType(targetUri) ?: FileUtils.getMimeType(newName)

                updateStateSync { currentState ->
                    val index = currentState.openFiles.indexOfFirst { it.uri == fileToSave.uri || (it.uri == null && it.name == fileToSave.name) }
                    if (index >= 0) {
                        val updatedFile = currentState.openFiles[index].copy(
                            uri = targetUri,
                            name = newName,
                            isUnsaved = false,
                            mimeType = newMimeType
                        )
                        currentState.copy(
                            openFiles = currentState.openFiles.toMutableList().apply { this[index] = updatedFile }
                        )
                    } else {
                        currentState
                    }
                }
            } catch (e: IOException) {
                println("Error saving file: $e")
            }
        }
    }

    /**
     * Saves the currently selected file. If it has a URI, overwrites it.
     * If not (new/untitled file), triggers the onSaveAsNeeded callback.
     * Runs file writing on Dispatchers.IO if overwriting.
     */
    fun saveCurrentFile(contentResolver: ContentResolver, onSaveAsNeeded: (suggestedName: String) -> Unit) {
        val fileToSave = state.value.currentFile ?: return

        if (fileToSave.uri != null) {
            // Existing file, overwrite it on IO thread
            viewModelScope.launch {
                try {
                    writeFileContent(fileToSave.uri, fileToSave.content.textState.text, contentResolver)
                    // Update state on main thread
                    updateStateSync { currentState ->
                        val index = currentState.openFiles.indexOfFirst { it.uri == fileToSave.uri }
                        if (index >= 0) {
                            val savedFile = currentState.openFiles[index].copy(isUnsaved = false)
                            currentState.copy(
                                openFiles = currentState.openFiles.toMutableList().apply { this[index] = savedFile }
                            )
                        } else {
                            currentState
                        }
                    }
                } catch (e: IOException) {
                    println("Error saving file: $e")
                }
            }
        } else {
            // New/Untitled file, trigger "Save As" flow
            onSaveAsNeeded(fileToSave.name)
        }
    }

    // --- Helper functions for file I/O (run on IO dispatcher) ---

    private suspend fun readFileContent(uri: Uri, contentResolver: ContentResolver): String = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: throw FileNotFoundException("Failed to open input stream for URI: $uri")
    }

    private suspend fun writeFileContent(uri: Uri, content: String, contentResolver: ContentResolver) = withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                // Use 'wt' mode for truncation before writing (like standard save)
                // If you need 'wa' (append), adjust accordingly.
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            } ?: throw IOException("Failed to open output stream for URI: $uri")
        } catch (e: Exception) {
            throw IOException("Error writing to URI $uri: ${e.message}", e)
        }
    }

    // Helper to get a filename (you might have a better version in FileUtils)
    private fun getFileNameFromUri(uri: Uri, contentResolver: ContentResolver): String {
        // Try to get display name from ContentResolver first
        var name = "untitled"
        val cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        // Fallback if cursor fails or name is empty
        if (name == "untitled" || name.isBlank()) {
            name = uri.lastPathSegment ?: "untitled"
            // Basic cleanup if it's a path segment
            val lastSlash = name.lastIndexOf('/')
            if (lastSlash != -1) name = name.substring(lastSlash + 1)
        }
        return name
    }


    // --- Existing ViewModel functions (openFile, closeFile, selectFile, updateFileContent) ---
    // Ensure these primarily manipulate state and don't do direct I/O

    /**
     * Opens a file in the editor state (doesn't load content here).
     * Used for creating new files or potentially switching to already loaded ones.
     */
    suspend fun openFile(file: CodeFile) { // Renamed from setCurrentFile in previous examples
        updateStateSync { currentState ->
            val existingIndex = currentState.openFiles.indexOfFirst {
                if (it.uri != null && file.uri != null) it.uri == file.uri else it.name == file.name && it.uri == null // Match untitled by name
            }

            if (existingIndex >= 0) {
                currentState.copy(
                    openFiles = currentState.openFiles.toMutableList().apply { this[existingIndex] = file },
                    selectedIndex = existingIndex
                )
            } else {
                val newFiles = currentState.openFiles + file
                currentState.copy(
                    openFiles = newFiles,
                    selectedIndex = newFiles.size - 1
                )
            }
        }
    }

    // closeFile, selectFile, updateFileContent remain largely the same as the previous version,
    // focusing only on state manipulation.

    suspend fun updateFileContent(file: CodeFile, newContent: EditorTextFieldState) {
        updateStateSync { currentState ->
            // Find based on URI if available, otherwise name for untitled
            val index = currentState.openFiles.indexOfFirst {
                if (it.uri != null && file.uri != null) it.uri == file.uri else it.name == file.name && it.uri == null
            }
            if (index >= 0) {
                // Avoid unnecessary state update if content hasn't changed
                if (currentState.openFiles[index].content == newContent && !currentState.openFiles[index].isUnsaved) {
                    return@updateStateSync currentState
                }
                val updatedFile = currentState.openFiles[index].copy(
                    content = newContent,
                    isUnsaved = true // Mark unsaved on any content change
                )
                currentState.copy(
                    openFiles = currentState.openFiles.toMutableList().apply { this[index] = updatedFile }
                )
            } else {
                currentState // File not found
            }
        }
    }

    // Deprecate or remove the generic updateFile if no longer needed
    // fun updateFile(file: CodeFile) { ... }

//    suspend fun closeFile(indexToClose: Int) {
//        updateStateSync { currentState ->
//            if (currentState.openFiles.size > indexToClose) {
//                // Clean up the file being closed
//                currentState.openFiles[indexToClose].content.cleanup()
//
//                val safeIndexToClose = indexToClose.coerceIn(0, currentState.openFiles.size - 1)
//                val updatedFiles = currentState.openFiles.toMutableList().apply {
//                    removeAt(safeIndexToClose)
//                }
//
//                val finalFiles = if (updatedFiles.isEmpty()) {
//                    listOf(CodeFile(name = "untitled", content = EditorTextFieldState(TextState("")), isUnsaved = true))
//                } else updatedFiles
//
//                // ...rest of existing code...
//            } else currentState
//        }
//    }
//
//    suspend fun selectFile(index: Int) {
//        updateStateSync { currentState ->
//            // Clean up the previous file's tree
//            currentState.currentFile?.content?.cleanup()
//
//            currentState.copy(
//                selectedIndex = index.coerceIn(0, maxOf(0, currentState.openFiles.size - 1))
//            )
//        }
//    }
}