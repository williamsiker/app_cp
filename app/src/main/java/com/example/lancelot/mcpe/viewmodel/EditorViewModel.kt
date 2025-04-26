package com.example.lancelot.mcpe.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.mcpe.model.TextState
import com.example.lancelot.utils.FileUtils // Assuming you have this utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/***
 *  @property name e.g "main.rs"
 *  @property uri is the path to the file on the device
 *  @property mimeType is the mime type for avoid automatic.rename file-extension
*/
data class CodeFile(
    val name: String,
    val content: TextState = TextState(""),
    val uri: Uri? = null,
    val isUnsaved: Boolean = false,
    val mimeType: String = "text/plain"
)

data class EditorState(
    val openFiles: List<CodeFile> = listOf(CodeFile(name = "untitled", isUnsaved = true)), // Start with one untitled file
    val currentFile: CodeFile? = openFiles.firstOrNull(),
    val selectedIndex: Int = 0
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // Mutex para garantizar operaciones atómicas en el estado
    private val stateMutex = Mutex()

    private suspend fun updateStateSync(block: (EditorState) -> EditorState) {
        stateMutex.withLock {
            _state.update { currentState ->
                val updatedState = block(currentState)
                val safeIndex = ensureValidIndex(updatedState.openFiles, updatedState.selectedIndex)
                // Asegurar que el currentFile siempre corresponda al índice seleccionado
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
                    listOf(CodeFile(name = "untitled", isUnsaved = true))
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
                val content = readFileContent(uri, contentResolver) // Suspending function on IO dispatcher
                val fileName = getFileNameFromUri(uri, contentResolver)
                val mimeType = contentResolver.getType(uri) ?: FileUtils.getMimeType(fileName)

                // Update state on the main thread
                updateStateSync { currentState ->
                    val existingIndex = currentState.openFiles.indexOfFirst { it.uri == uri }
                    val newFile = CodeFile(
                        name = fileName,
                        content = TextState(content), // Create new TextState
                        uri = uri,
                        isUnsaved = false, // Freshly loaded = saved state
                        mimeType = mimeType
                    )
                    if (existingIndex >= 0) {
                        // Replace existing file
                        currentState.copy(
                            openFiles = currentState.openFiles.toMutableList().apply { this[existingIndex] = newFile },
                            selectedIndex = existingIndex
                        )
                    } else {
                        // Add new file
                        val newFiles = currentState.openFiles + newFile
                        currentState.copy(
                            openFiles = newFiles,
                            selectedIndex = newFiles.size - 1
                        )
                    }
                }
            } catch (e: IOException) {
                // Handle exceptions (e.g., show a toast or update state with error)
                println("Error opening file: $e")
                // You might want to update the UI state to show an error message
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
                writeFileContent(targetUri, fileToSave.content.text, contentResolver) // Suspending function on IO dispatcher
                val newName = getFileNameFromUri(targetUri, contentResolver)
                val newMimeType = contentResolver.getType(targetUri) ?: FileUtils.getMimeType(newName)

                // Update state on the main thread
                updateStateSync { currentState ->
                    val index = currentState.openFiles.indexOfFirst { it.uri == fileToSave.uri || (it.uri == null && it.name == fileToSave.name) }
                    if (index >= 0) {
                        val updatedFile = currentState.openFiles[index].copy(
                            uri = targetUri, // Update URI
                            name = newName, // Update name
                            isUnsaved = false, // Mark as saved
                            mimeType = newMimeType // Update mime type
                        )
                        currentState.copy(
                            openFiles = currentState.openFiles.toMutableList().apply { this[index] = updatedFile },
                            // Keep the same index selected
                            selectedIndex = index
                        )
                    } else {
                        // Should not happen if called correctly, but handle defensively
                        currentState
                    }
                }
            } catch (e: IOException) {
                // Handle exceptions
                println("Error saving file: $e")
                // Update UI state with error
            }
        }
    }

    /**
     * Saves the currently selected file. If it has a URI, overwrites it.
     * If not (new/untitled file), triggers the onSaveAsNeeded callback.
     * Runs file writing on Dispatchers.IO if overwriting.
     */
    fun saveCurrentFile(contentResolver: ContentResolver, onSaveAsNeeded: (suggestedName: String) -> Unit) {
        val fileToSave = state.value.currentFile ?: return // No file selected

        if (fileToSave.uri != null) {
            // Existing file, overwrite it on IO thread
            viewModelScope.launch {
                try {
                    writeFileContent(fileToSave.uri, fileToSave.content.text, contentResolver)
                    // Update state on main thread
                    updateStateSync { currentState ->
                         val index = currentState.openFiles.indexOfFirst { it.uri == fileToSave.uri }
                         if (index >= 0) {
                             val savedFile = currentState.openFiles[index].copy(isUnsaved = false)
                             currentState.copy(
                                 openFiles = currentState.openFiles.toMutableList().apply { this[index] = savedFile }
                             )
                         } else { currentState }
                    }
                } catch (e: IOException) {
                    println("Error saving file: $e")
                    // Update UI state with error
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

     suspend fun updateFileContent(file: CodeFile, newContent: TextState) {
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
}