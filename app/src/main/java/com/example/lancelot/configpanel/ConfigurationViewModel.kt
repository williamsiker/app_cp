package com.example.lancelot.configpanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.AppDatabase
import com.example.lancelot.Styles
import com.example.lancelot.CodeThemeEntity
import com.example.lancelot.theme.CodeTheme
import com.example.lancelot.theme.ThemeManager
import com.example.lancelot.theme.domain.ThemeRepository
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigurationState(
    val styles: List<Styles> = emptyList(),
    val selectedStyle: Styles? = null,
    val themes: List<CodeThemeEntity> = emptyList(),
    val selectedTheme: CodeThemeEntity? = null
)

class ConfigurationViewModel(
    private val database: AppDatabase,
    private val themeRepository: ThemeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConfigurationState())
    val state: StateFlow<ConfigurationState> = _state.asStateFlow()

    init {
        loadStyles()
        loadThemes()
    }

    private fun loadStyles() {
        viewModelScope.launch {
            val styles = database.styleDAO().getAllStyles()
            _state.value = _state.value.copy(styles = styles)
        }
    }

    private fun loadThemes() {
        viewModelScope.launch {
            val themes = themeRepository.getAllThemes()
            _state.value = _state.value.copy(themes = themes)
            if (_state.value.selectedTheme == null && themes.isNotEmpty()) {
                setSelectedTheme(themes.first())
            }
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

    fun addTheme(name: String, colorsJson: String) {
        viewModelScope.launch {
            themeRepository.insertTheme(CodeThemeEntity(name = name, colorsJson = colorsJson))
            loadThemes()
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

    fun deleteTheme(themeId: Long) {
        viewModelScope.launch {
            themeRepository.deleteTheme(themeId)
            loadThemes()
            if (_state.value.selectedTheme?.id == themeId) {
                setSelectedTheme(null)
            }
        }
    }

    fun setSelectedTheme(theme: CodeThemeEntity?) {
        _state.value = _state.value.copy(selectedTheme = theme)
        theme?.let {
            val map = try {
                val json = org.json.JSONObject(it.colorsJson)
                json.keys().asSequence().associateWith { key -> Color(android.graphics.Color.parseColor(json.getString(key))) }
            } catch (e: Exception) { emptyMap<String, Color>() }
            ThemeManager.setTheme(CodeTheme(map))
        }
    }
}