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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextLayoutResult
import com.example.lancelot.mcpe.model.TextState
import com.example.lancelot.mcpe.state.EditorTextFieldState
import com.example.lancelot.ui.theme.AppTheme

@Composable
internal fun EditorTextField(
    modifier: Modifier = Modifier,
    textState: TextState,
    scrollState: ScrollState,
    onScroll: (Float) -> Unit = {},
    onTextChanged: (TextState) -> Unit = {},
    onLineNumbersWidthChange: (Int) -> Unit = {}
) {
    val textFieldState = remember(textState) { EditorTextFieldState(textState) }
    val horizontalScroll = rememberScrollState()
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize()) {
        LineNumbers(
            textState = textState,
            textFieldState = textFieldState,
            scrollState = scrollState,
            horizontalScrollState = horizontalScroll,
            onWidthChange = onLineNumbersWidthChange,
            density = density
        )

        CompositionLocalProvider(LocalTextSelectionColors provides AppTheme.colors.selectionColors) {
            BasicTextField(
                value = textFieldState.textFieldValue,
                onValueChange = { value: TextFieldValue ->
                    textFieldState.onTextFieldValueChange(value)
                    onTextChanged(textState)
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