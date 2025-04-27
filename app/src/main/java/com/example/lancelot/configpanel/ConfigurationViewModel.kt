package com.example.lancelot.configpanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigurationState(
    val styles: List<Styles> = emptyList(),
    val selectedStyle: Styles? = null
)

class ConfigurationViewModel(private val database: AppDatabase) : ViewModel() {
    private val _state = MutableStateFlow(ConfigurationState())
    val state: StateFlow<ConfigurationState> = _state.asStateFlow()

    init {
        loadStyles()
    }

    private fun loadStyles() {
        viewModelScope.launch {
            val styles = database.styleDAO().getAllStyles()
            _state.value = _state.value.copy(styles = styles)
        }
    }

    fun setSelectedStyle(style: Styles?) {
        _state.value = _state.value.copy(selectedStyle = style)
    }

    fun addStyle(name: String, color: String, sizeFont: Int, isBold: Boolean, isItalic: Boolean, isUnderline: Boolean) {
        viewModelScope.launch {
            database.styleDAO().insertStyle(
                Styles(
                    name = name,
                    color = color,
                    sizeFont = sizeFont,
                    isBold = isBold,
                    isItalic = isItalic,
                    isUnderline = isUnderline
                )
            )
            loadStyles()
        }
    }

    fun deleteStyle(styleId: Long) {
        viewModelScope.launch {
            database.styleDAO().deleteStyle(styleId)
            loadStyles()
            if (_state.value.selectedStyle?.id == styleId) {
                setSelectedStyle(null)
            }
        }
    }
}