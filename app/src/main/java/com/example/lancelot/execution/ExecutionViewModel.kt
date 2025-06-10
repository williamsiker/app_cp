package com.example.lancelot.execution

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.execution.domain.ExecutionRepository
import com.example.lancelot.execution.model.ExecutionResult
import kotlinx.coroutines.launch

sealed class ExecutionState {
    object Idle : ExecutionState()
    object Loading : ExecutionState()
    data class Success(val result: ExecutionResult) : ExecutionState()
    data class Error(val message: String) : ExecutionState()
}

class ExecutionViewModel(private val repository: ExecutionRepository) : ViewModel() {
    var state: ExecutionState by mutableStateOf(ExecutionState.Idle)
        private set

    fun run(code: String, language: String, input: String) {
        viewModelScope.launch {
            state = ExecutionState.Loading
            state = try {
                val result = repository.execute(code, language, input)
                ExecutionState.Success(result)
            } catch (e: Exception) {
                ExecutionState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
