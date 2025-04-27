package com.example.lancelot.webview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewModel : ViewModel() {
    private val _lastUrl = MutableStateFlow<String?>(null)
    val lastUrl = _lastUrl.asStateFlow()

    var showFab by mutableStateOf(true)
        private set

    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set

    fun updateLastUrl(url: String) {
        viewModelScope.launch {
            _lastUrl.emit(url)
        }
    }

    fun toggleFab() {
        showFab = !showFab
    }

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        this.canGoBack = canGoBack
        this.canGoForward = canGoForward
    }
}