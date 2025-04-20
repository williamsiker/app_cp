package com.example.lancelot

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.lancelot.ui.theme.LancelotTheme
import kotlinx.serialization.Serializable

data class Platform(
    val name: String,
    val icon: ImageVector,
    val url: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val platforms = listOf(
            Platform(
                name = "AtCoder",
                icon = Icons.Default.Add,
                url = "https://atcoder.jp"
            ),
            Platform(
                name = "Kenkoo",
                icon = Icons.Default.Add,
                url = "https://kenkoooo.com/"
            ),
            Platform(
                name = "Codeforces",
                icon = Icons.Default.Add,
                url = "https://codeforces.com"
            )
        )

        setContent {
            LancelotTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Home) {
                    composable<Home> {
                        Scaffold(
                            topBar = { TopBar() }
                        ) { innerPadding ->
                            PlatformSelector(
                                platforms = platforms,
                                onPlatformSelected = { url ->
                                    navController.navigate(WebViewObj(url))
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                    composable<WebViewObj> {
                        val args = it.toRoute<WebViewObj>()
                        WebViewScreen(
                            args.webUrl,
                            onCodeEditorNavigation = {
                                navController.navigate(CodeBlocks)
                            }
                        )
                    }
                    composable<CodeBlocks> {

                    }
                }
            }
        }
    }
}

@Serializable
object Home

@Serializable
data class WebViewObj(val webUrl: String)

@Serializable
object CodeBlocks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    TopAppBar(
        title = { Text("Lancelot") },
        actions = {
            IconButton(onClick = { /* Acción al presionar + */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar"
                )
            }
        }
    )
}

@Composable
fun PlatformSelector(
    platforms: List<Platform>,
    onPlatformSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        items(platforms) { platform ->
            Text(
                text = platform.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPlatformSelected(platform.url)
                    }
                    .padding(16.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    onCodeEditorNavigation: () -> Unit
) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }
    webView.apply {
        webViewClient = WebViewClient()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        loadUrl(url)
    }
    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack() // Navegar hacia atrás dentro del WebView
        }
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onCodeEditorNavigation()
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Abrir Editor")
            }
        }
    ) { padding ->
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
