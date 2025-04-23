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

data class CodeFile(
    val name: String,
    val content: TextState = TextState(""),
    val uri: Uri? = null,
    val isUnsaved: Boolean = false,
    val mimeType: String = "text/plain"
)

data class EditorState(
    val openFiles: List<CodeFile> = emptyList(),
    val currentFile: CodeFile? = null
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun setCurrentFile(file: CodeFile) {
        viewModelScope.launch {
            _state.update { currentState ->
                val existingFileIndex = currentState.openFiles.indexOfFirst { it.name == file.name }
                val updatedFiles = if (existingFileIndex >= 0) {
                    currentState.openFiles.toMutableList().also { files ->
                        files[existingFileIndex] = file
                    }
                } else {
                    currentState.openFiles + file
                }
                currentState.copy(
                    currentFile = file,
                    openFiles = updatedFiles
                )
            }
        }
    }

    fun updateFile(file: CodeFile) {
        viewModelScope.launch {
            _state.update { currentState ->
                val updatedFiles = currentState.openFiles.map { 
                    if (it.name == file.name) file else it 
                }
                currentState.copy(
                    currentFile = if (currentState.currentFile?.name == file.name) file else currentState.currentFile,
                    openFiles = updatedFiles
                )
            }
        }
    }

    fun closeFile(file: CodeFile) {
        viewModelScope.launch {
            _state.update { currentState ->
                val updatedFiles = currentState.openFiles.filterNot { it.name == file.name }
                currentState.copy(
                    currentFile = if (currentState.currentFile?.name == file.name) 
                        updatedFiles.firstOrNull() 
                    else 
                        currentState.currentFile,
                    openFiles = updatedFiles
                )
            }
        }
    }
}