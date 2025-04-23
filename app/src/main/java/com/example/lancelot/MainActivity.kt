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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.lancelot.configpanel.ConfigPanel
import com.example.lancelot.ui.theme.LancelotTheme
import kotlinx.serialization.Serializable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.example.lancelot.viewmodel.BrowserViewModel

data class Platform(
    val name: String,
    val icon: ImageVector,
    val url: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
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
                val context = LocalContext.current
                val db = remember { DatabaseProvider.getDatabase(context = context) }
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
                        EditorScaffold(
                            onConfigNavigation = {
                                navController.navigate(ConfigPanel)
                            }
                        )
                    }
                    composable<ConfigPanel> {
                        ConfigPanel(onPopStackNav = {navController.popBackStack()}, db)
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

@Serializable
object ConfigPanel

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
    val webViewModel = viewModel<BrowserViewModel>()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        webViewModel.updateLastUrl(url)
    }

    val context = LocalContext.current
    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browser") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webView?.canGoBack() == true) {
                                webView?.goBack()
                            }
                        },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (webView?.canGoForward() == true) {
                                webView?.goForward()
                            }
                        },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.Default.ArrowForward, "Adelante")
                    }
                    IconButton(
                        onClick = { webViewModel.toggleFab() }
                    ) {
                        Icon(
                            if (webViewModel.showFab) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            "Toggle Editor Button"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = webViewModel.showFab,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(onClick = onCodeEditorNavigation) {
                    Icon(Icons.Default.Edit, contentDescription = "Abrir Editor")
                }
            }
        }
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() ?: false
                            canGoForward = view?.canGoForward() ?: false
                            url?.let { webViewModel.updateLastUrl(it) }
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadUrl(url)
                }.also { webView = it }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
