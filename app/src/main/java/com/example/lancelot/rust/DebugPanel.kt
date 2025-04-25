package com.example.lancelot.rust

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
    val tokens = RustBridge.tokenizeCode(code, "cpp")
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = RustBridge.executeCode(code, "cpp", "android") )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(tokens) { token ->
                DbgTokens(token, code)
            }
        }
    }
}

@Composable
fun DbgTokens(token: Token, code: String) {
    val snippet = try {
        code.substring(token.startByte, token.endByte)
    } catch (e: Exception) {
        "Error: ${e.message}"
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Text(
            text = "Kind: ${token.kind} | NodeType: ${token.nodeType}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Snippet: \"$snippet\"",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Positions: [${token.positions.joinToString(", ")}]",
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider()
    }
}

