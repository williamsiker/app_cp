package com.example.lancelot.rust

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.lancelot.execution.ExecutionPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen() {
    RustBridge.initLogger()
    val code = """
#include <iostream>
int main(){std::cout<<"hello";return 0;}
    """.trimIndent()
    ExecutionPanel(code = code, language = "cpp", input = "")
}

