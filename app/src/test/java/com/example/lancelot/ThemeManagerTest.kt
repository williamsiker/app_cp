package com.example.lancelot

import androidx.compose.ui.graphics.Color
import com.example.lancelot.theme.CodeTheme
import com.example.lancelot.theme.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {
    @Test
    fun styleForTokenUsesCurrentTheme() {
        val theme = CodeTheme(mapOf("keyword" to Color.Red))
        ThemeManager.setTheme(theme)
        val style = ThemeManager.styleFor("keyword")
        assertEquals(Color.Red, style.color)
    }
}
