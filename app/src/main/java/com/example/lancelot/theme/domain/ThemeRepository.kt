package com.example.lancelot.theme.domain

import com.example.lancelot.CodeThemeEntity

interface ThemeRepository {
    suspend fun insertTheme(theme: CodeThemeEntity): Long
    suspend fun deleteTheme(themeId: Long)
    suspend fun getAllThemes(): List<CodeThemeEntity>
    suspend fun getThemeById(themeId: Long): CodeThemeEntity?
}
