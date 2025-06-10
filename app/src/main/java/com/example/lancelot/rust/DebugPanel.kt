package com.example.lancelot.rust

<<<<<<< HEAD
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
=======
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
>>>>>>> cb2203e70159d386ad1ab6ec7ec89575b55e865a
import com.example.lancelot.execution.ExecutionPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen(onBack: () -> Unit) {
    RustBridge.initLogger()
    val code = """
#include <iostream>
int main(){std::cout<<"hello";return 0;}
    """.trimIndent()
<<<<<<< HEAD
    ExecutionPanel(code = code, language = "cpp", input = "")
=======
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        ExecutionPanel(
            code = code,
            language = "cpp",
            input = "",
            embedded = true,
            modifier = Modifier.padding(padding)
        )
    }
>>>>>>> cb2203e70159d386ad1ab6ec7ec89575b55e865a
}

