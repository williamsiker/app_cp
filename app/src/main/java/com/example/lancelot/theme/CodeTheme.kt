package com.example.lancelot.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.example.lancelot.ui.theme.DefaultAppTheme

/**
 * Represents a mapping of highlight tokens to colors.
 */
data class CodeTheme(
    val colors: Map<String, Color>
) {
    fun spanFor(token: String): SpanStyle {
        val color = colors[token] ?: DefaultAppTheme.code.simple.color
        return SpanStyle(color)
    }
}

object ThemeManager {
    private var current: CodeTheme = CodeTheme(emptyMap())

    fun setTheme(theme: CodeTheme) {
        current = theme
    }

    fun styleFor(token: String): SpanStyle = current.spanFor(token)
}
