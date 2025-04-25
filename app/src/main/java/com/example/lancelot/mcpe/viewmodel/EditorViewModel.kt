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
    val openFiles: MutableList<CodeFile> = mutableListOf(),
    val currentFile: CodeFile? = null,
    val selectedIndex: Int = 0
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private fun updateState(block: (EditorState) -> EditorState) {
        viewModelScope.launch {
            _state.update(block)
        }
    }

    fun setCurrentFile(file: CodeFile) {
        updateState { currentState ->
            val updatedFiles = currentState.openFiles.toMutableList()
            val existingFileIndex = updatedFiles.indexOfFirst { it.name == file.name }
            
            when {
                updatedFiles.isEmpty() -> {
                    updatedFiles.add(file)
                    currentState.copy(
                        currentFile = file,
                        openFiles = updatedFiles,
                        selectedIndex = 0
                    )
                }
                existingFileIndex >= 0 -> {
                    updatedFiles[existingFileIndex] = file
                    currentState.copy(
                        currentFile = file,
                        openFiles = updatedFiles,
                        selectedIndex = existingFileIndex
                    )
                }
                else -> {
                    updatedFiles.add(file)
                    currentState.copy(
                        currentFile = file,
                        openFiles = updatedFiles,
                        selectedIndex = updatedFiles.size - 1
                    )
                }
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
            val indexToRemove = updatedFiles.indexOfFirst { it.name == file.name }
            
            if (indexToRemove >= 0) {
                updatedFiles.removeAt(indexToRemove)
                
                val newFiles = if (updatedFiles.isEmpty()) {
                    mutableListOf(CodeFile(name = "untitled", isUnsaved = true))
                } else updatedFiles
                
                val newSelectedIndex = when {
                    currentState.selectedIndex >= newFiles.size -> newFiles.size - 1
                    currentState.selectedIndex > indexToRemove -> currentState.selectedIndex - 1
                    else -> currentState.selectedIndex
                }.coerceIn(0, newFiles.size - 1)
                
                currentState.copy(
                    currentFile = if (currentState.currentFile?.name == file.name) 
                        newFiles.getOrNull(newSelectedIndex)
                    else 
                        currentState.currentFile,
                    openFiles = newFiles,
                    selectedIndex = newSelectedIndex
                )
            } else {
                currentState
            }
        }
    }

    fun selectFile(index: Int) {
        updateState { currentState ->
            if (index >= 0 && index < currentState.openFiles.size) {
                currentState.copy(
                    currentFile = currentState.openFiles[index],
                    selectedIndex = index
                )
            } else {
                currentState
            }
        }
    }
}