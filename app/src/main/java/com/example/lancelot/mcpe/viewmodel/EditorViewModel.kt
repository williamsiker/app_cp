package com.example.lancelot.mcpe.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.mcpe.model.TextState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val openFiles: List<CodeFile> = emptyList(),
    val currentFile: CodeFile? = null,
    val selectedIndex: Int = 0
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private fun updateState(block: (EditorState) -> EditorState) {
        _state.update(block)
    }

    fun setCurrentFile(file: CodeFile) {
        updateState { currentState ->
            val updatedFiles = currentState.openFiles.toMutableList()
            // Verificar si el archivo ya existe
            val existingIndex = updatedFiles.indexOfFirst { 
                it.name == file.name && it.uri == file.uri 
            }
            if (existingIndex >= 0) {
                // Actualizar archivo existente
                updatedFiles[existingIndex] = file
                currentState.copy(
                    openFiles = updatedFiles,
                    currentFile = file,
                    selectedIndex = existingIndex
                )
            } else {
                updatedFiles.add(file)
                currentState.copy(
                    openFiles = updatedFiles,
                    currentFile = file,
                    selectedIndex = updatedFiles.size - 1
                )
            }
        }
    }

    fun updateFile(file: CodeFile) {
        updateState { currentState ->
            val updatedFiles = currentState.openFiles.toMutableList()
            val index = updatedFiles.indexOfFirst { it.name == file.name }
            
            if (index >= 0) {
                updatedFiles[index] = file
                currentState.copy(
                    currentFile = if (currentState.currentFile?.name == file.name) file else currentState.currentFile,
                    openFiles = updatedFiles
                )
            } else {
                currentState
            }
        }
    }

    fun closeFile(file: CodeFile) {
        updateState { currentState ->
            val updatedFiles = currentState.openFiles.toMutableList()
            val removedIndex = updatedFiles.indexOfFirst { it.name == file.name }

            if (removedIndex >= 0) {
                updatedFiles.removeAt(removedIndex)

                val newFiles = if (updatedFiles.isEmpty()) {
                    listOf(CodeFile(name = "untitled", isUnsaved = true))
                } else updatedFiles

                // Calcular nuevo índice seguro
                val newIndex = when {
                    currentState.selectedIndex < removedIndex -> currentState.selectedIndex
                    currentState.selectedIndex == removedIndex -> (removedIndex - 1).coerceAtLeast(0)
                    else -> (currentState.selectedIndex - 1).coerceAtLeast(0)
                }.coerceIn(0, newFiles.size - 1)
                currentState.copy(
                    openFiles = newFiles,
                    currentFile = newFiles.getOrNull(newIndex),
                    selectedIndex = newIndex
                )
            } else {
                currentState
            }
        }
    }
    fun selectFile(index: Int) {
        updateState { currentState ->
            val safeIndex = index.coerceIn(0, currentState.openFiles.size - 1)
            currentState.copy(
                currentFile = currentState.openFiles.getOrNull(safeIndex),
                selectedIndex = safeIndex
            )
        }
    }
}