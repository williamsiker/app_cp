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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BugReport
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
import androidx.compose.ui.graphics.Color
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebViewCompat
import com.example.lancelot.mcpe.components.EditorScaffold
import com.example.lancelot.rust.DebugPanelScreen
import kotlinx.serialization.Serializable
import com.example.lancelot.viewmodel.BrowserViewModel
import org.koin.compose.KoinContext

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
            KoinContext {
                LancelotTheme {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    NavHost(navController = navController, startDestination = Home) {
                        composable<Home> {
                            Scaffold(
                                topBar = {
                                    TopBar(onDebugNavigate = {
                                        navController.navigate(DebugPanel)
                                    })
                                }
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
                            ConfigPanel(onPopStackNav = { navController.popBackStack() })
                        }
                        composable<DebugPanel> {
                            DebugPanelScreen()
                        }
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

@Serializable
object DebugPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onDebugNavigate: () -> Unit = {}
) {
    TopAppBar(
        title = { Text("Lancelot") },
        actions = {
            IconButton(onClick = { /* Acción al presionar + */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar"
                )
            }
            IconButton(onClick = { onDebugNavigate() }) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = "Depurar"
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
