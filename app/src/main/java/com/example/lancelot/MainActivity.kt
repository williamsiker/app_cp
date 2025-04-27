package com.example.lancelot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.lancelot.configpanel.ConfigPanel
import com.example.lancelot.mcpe.components.EditorScaffold
import com.example.lancelot.rust.DebugPanelScreen
import com.example.lancelot.ui.theme.LancelotTheme
import com.example.lancelot.webview.WebViewScreen
import kotlinx.serialization.Serializable
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            KoinContext {
                LancelotTheme {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Home) {
                        composable<Home> {
                            Scaffold(
                                topBar = {
                                    TopBar(onDebugNavigate = {
                                        navController.navigate(DebugPanel)
                                    })
                                }
                            ) { innerPadding ->
                                // Single button to navigate to WebView
                                Button(
                                    onClick = {
                                        navController.navigate(WebViewObj("https://atcoder.jp"))
                                    },
                                    modifier = Modifier
                                        .padding(innerPadding)
                                        .fillMaxWidth()
                                ) {
                                    Text("Open WebView")
                                }
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
