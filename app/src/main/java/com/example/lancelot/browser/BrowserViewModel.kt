package com.example.lancelot.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowserViewModel : ViewModel() {
    private val _lastUrl = MutableStateFlow<String?>(null)
    val lastUrl = _lastUrl.asStateFlow()

    var showFab by mutableStateOf(true)
        private set

    fun updateLastUrl(url: String) {
        viewModelScope.launch {
            _lastUrl.emit(url)
        }
    }

    fun toggleFab() {
        showFab = !showFab
    }
}