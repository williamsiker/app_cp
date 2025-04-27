package com.example.lancelot.mcpe.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextLayoutResult
import com.example.lancelot.mcpe.TextState
import com.example.lancelot.mcpe.EditorTextFieldState
import com.example.lancelot.ui.theme.DefaultAppTheme

@Composable
internal fun EditorTextField(
    modifier: Modifier = Modifier,
    textFieldState: EditorTextFieldState,
    scrollState: ScrollState,
    onScroll: (Float) -> Unit = {},
    onTextChanged: (EditorTextFieldState) -> Unit = {},
    onLineNumbersWidthChange: (Int) -> Unit = {}
) {
    val horizontalScroll = rememberScrollState()
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize()) {
        LineNumbers(
            textFieldState = textFieldState,
            scrollState = scrollState,
            horizontalScrollState = horizontalScroll,
            onWidthChange = onLineNumbersWidthChange,
            density = density
        )

        CompositionLocalProvider(LocalTextSelectionColors provides DefaultAppTheme.colors.selectionColors) {
            BasicTextField(
                value = textFieldState.textFieldValue,
                onValueChange = { value: TextFieldValue ->
                    textFieldState.onTextFieldValueChange(value)
                    onTextChanged(textFieldState)
                },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .horizontalScroll(horizontalScroll),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                onTextLayout = { layoutResult: TextLayoutResult ->
                    textFieldState.onTextLayoutChange(layoutResult)
                }
            )
        }
    }
}