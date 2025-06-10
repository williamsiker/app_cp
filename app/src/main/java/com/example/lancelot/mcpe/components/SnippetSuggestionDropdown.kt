package com.example.lancelot.mcpe.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lancelot.snippets.Snippet

@Composable
fun SnippetSuggestionDropdown(
    suggestions: List<Snippet>,
    onSuggestionSelected: (Snippet) -> Unit
) {
    if (suggestions.isEmpty()) return

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column {
            suggestions.take(5).forEach { snippet ->
                Text(
                    text = "${snippet.prefix} - ${snippet.description}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionSelected(snippet) }
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
