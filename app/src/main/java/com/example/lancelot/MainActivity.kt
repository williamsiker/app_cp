package com.example.lancelot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.lancelot.configpanel.ConfigPanel
import com.example.lancelot.rust.DebugPanelScreen
import com.example.lancelot.ui.theme.LancelotTheme
import com.example.lancelot.webview.WebEditorPagerScreen
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
                            WebEditorPagerScreen(
                                onConfigNavigation = { navController.navigate(ConfigPanel) },
                                onDebugNavigation = { navController.navigate(DebugPanel) }
                            )
                        }
                        composable<ConfigPanel> { ConfigPanel(onPopStackNav = { navController.popBackStack() }) }
                        composable<DebugPanel> { DebugPanelScreen() }
                    }
                }
            }
        }
    }
}

@Serializable
object Home

@Serializable
object ConfigPanel

@Serializable
object DebugPanel
