package com.example.lancelot.theme.data

import com.example.lancelot.CodeThemeEntity
import com.example.lancelot.ThemeDAO
import com.example.lancelot.theme.domain.ThemeRepository

class RoomThemeRepository(private val themeDAO: ThemeDAO) : ThemeRepository {
    override suspend fun insertTheme(theme: CodeThemeEntity): Long = themeDAO.insertTheme(theme)
    override suspend fun deleteTheme(themeId: Long) = themeDAO.deleteTheme(themeId)
    override suspend fun getAllThemes(): List<CodeThemeEntity> = themeDAO.getAllThemes()
    override suspend fun getThemeById(themeId: Long): CodeThemeEntity? = themeDAO.getThemeById(themeId)
}
