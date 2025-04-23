package com.example.lancelot.configpanel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lancelot.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigurationState(
    val languages: List<Languages> = emptyList(),
    val keywords: List<Keywords> = emptyList(),
    val styles: List<Styles> = emptyList(),
    val groups: List<KeywordGroup> = emptyList(),
    val selectedLanguage: Languages? = null,
    val selectedGroup: KeywordGroup? = null,
    val selectedStyle: Styles? = null
)

class ConfigurationViewModel(private val database: AppDatabase) : ViewModel() {
    private val _state = MutableStateFlow(ConfigurationState())
    val state: StateFlow<ConfigurationState> = _state.asStateFlow()

    init {
        loadLanguages()
        loadStyles()
    }

    private fun loadLanguages() {
        viewModelScope.launch {
            val languages = database.languageDAO().getAllLanguages()
            _state.value = _state.value.copy(languages = languages)
        }
    }

    private fun loadKeywordsForLanguage(languageId: Long) {
        viewModelScope.launch {
            val keywords = database.keywordDAO().getAllKeywordsForLanguage(languageId)
            val groups = database.keywordGroupDAO().getAllGroupsForLanguage(languageId)
            _state.value = _state.value.copy(
                keywords = keywords,
                groups = groups
            )
        }
    }

    private fun loadStyles() {
        viewModelScope.launch {
            val styles = database.styleDAO().getAllStyles()
            _state.value = _state.value.copy(styles = styles)
        }
    }

    fun setSelectedLanguage(language: Languages?) {
        _state.value = _state.value.copy(selectedLanguage = language)
        language?.let { loadKeywordsForLanguage(it.id) }
    }

    fun setSelectedGroup(group: KeywordGroup?) {
        _state.value = _state.value.copy(selectedGroup = group)
    }

    fun setSelectedStyle(style: Styles?) {
        _state.value = _state.value.copy(selectedStyle = style)
    }

    fun addLanguage(name: String, description: String?, fileExtensions: String) {
        viewModelScope.launch {
            val language = Languages(
                name = name, 
                description = description,
                fileExtensions = fileExtensions
            )
            database.languageDAO().insertLanguages(language)
            loadLanguages()
        }
    }

    fun deleteLanguage(languageId: Long) {
        viewModelScope.launch {
            database.languageDAO().deleteLanguage(languageId)
            loadLanguages()
            if (_state.value.selectedLanguage?.id == languageId) {
                setSelectedLanguage(null)
            }
        }
    }

    fun addKeyword(keyword: String, languageId: Long, groupId: Long?, styleId: Long?) {
        viewModelScope.launch {
            database.keywordDAO().insertKeyword(
                Keywords(
                    keyword = keyword,
                    languageId = languageId,
                    groupId = groupId,
                    styleId = styleId
                )
            )
            loadKeywordsForLanguage(languageId)
        }
    }

    fun deleteKeyword(keywordId: Long) {
        viewModelScope.launch {
            database.keywordDAO().deleteKeyword(keywordId)
            _state.value.selectedLanguage?.let { loadKeywordsForLanguage(it.id) }
        }
    }

    fun addGroup(name: String, languageId: Long, styleId: Long?) {
        viewModelScope.launch {
            database.keywordGroupDAO().insertGroup(
                KeywordGroup(
                    name = name,
                    languageId = languageId,
                    styleId = styleId
                )
            )
            loadKeywordsForLanguage(languageId)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            database.keywordGroupDAO().deleteGroup(groupId)
            _state.value.selectedLanguage?.let { loadKeywordsForLanguage(it.id) }
            if (_state.value.selectedGroup?.id == groupId) {
                setSelectedGroup(null)
            }
        }
    }

    fun updateGroupStyle(groupId: Long, styleId: Long?) {
        viewModelScope.launch {
            database.keywordGroupDAO().updateGroupStyle(groupId, styleId)
            database.keywordDAO().updateGroupKeywordsStyle(groupId, styleId)
            _state.value.selectedLanguage?.let { loadKeywordsForLanguage(it.id) }
        }
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