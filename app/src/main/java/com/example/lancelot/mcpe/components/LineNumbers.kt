package com.example.lancelot.mcpe.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import com.example.lancelot.mcpe.EditorTextFieldState
import com.example.lancelot.mcpe.utils.Paddings
import com.example.lancelot.ui.theme.DefaultAppTheme

@Composable
internal fun LineNumbers(
    textFieldState: EditorTextFieldState,
    scrollState: ScrollState,
    horizontalScrollState: ScrollState,
    onWidthChange: (Int) -> Unit,
    density: Density
) {
    var maxWidth by remember { mutableIntStateOf(0) }
    val lineCount = textFieldState.lineCount

    LaunchedEffect(lineCount) {
        val newMaxWidth = with(density) {
            (lineCount.toString().length * DefaultAppTheme.typography.code.fontSize.toPx() * 0.6f).toInt() +
                    Paddings.lineNumbersHorizontalPaddingSum.toPx().toInt()
        }
        if (newMaxWidth != maxWidth) {
            maxWidth = newMaxWidth
            onWidthChange(maxWidth)
        }
    }

    Box(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .fillMaxHeight()
            .padding(Paddings.lineNumbersPadding)
    ) {
        Text(
            text = (1..lineCount).joinToString("\n"),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            ),
            color = DefaultAppTheme.colors.indicatorColor
        )
    }
}