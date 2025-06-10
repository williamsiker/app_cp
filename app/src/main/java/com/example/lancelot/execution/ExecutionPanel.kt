package com.example.lancelot.execution

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionPanel(
    code: String,
    language: String,
    input: String,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val viewModel: ExecutionViewModel = koinViewModel()
    val state = viewModel.state

    val content: @Composable (Modifier) -> Unit = { inner ->
        Column(modifier = inner.padding(16.dp)) {
            when (state) {
                ExecutionState.Idle -> {
                    Button(onClick = { viewModel.run(code, language, input) }) {
                        Text("Run")
                    }
                }
                ExecutionState.Loading -> {
                    CircularProgressIndicator()
                }
                is ExecutionState.Success -> {
                    Text(state.result.output)
                    state.result.compileOutput?.let {
                        Text(it)
                    }
                }
                is ExecutionState.Error -> {
                    Text("Error: ${state.message}")
                }
            }
        }
    }

    if (embedded) {
        content(modifier)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Run") })
            },
            modifier = modifier
        ) { padding ->
            content(Modifier.padding(padding))
        }
    }
}
