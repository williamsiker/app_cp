package com.example.lancelot.rust

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen() {
    RustBridge.initLogger()
    // Llamada a parseCode solo una vez
    val code = """
#include <iostream>
class MyClass {
public:
    void myFunction(std::string system) {
        std::cout << "Hello" << system  << "from a rust library" << std::endl;
    };
};

int main() {
    std::string s; std::cin>>s;
    MyClass obj;
    obj.myFunction(s);
    return 0;
}
            """.trimIndent()

    var resultText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            RustBridge.ktFuture(code,"cpp", "android")
                .thenAccept{
                    resultText = it
                    isLoading = false
                }
                .exceptionally { throwable ->
                    resultText = "Error: ${throwable.message}"
                    isLoading = false
                    Log.e("RustBridge", "Error executing code asynchronously", throwable)
                    null
                }
        } catch (e: Exception) {
            resultText = "Error: ${e.message}"
            isLoading = false
            Log.e("RustBridge", "Error executing code asynchronously", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isLoading) {
                        Column {
                            CircularProgressIndicator()
                        }
                    } else {
                        Text(resultText ?: "No result")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {

        }
    }
}

