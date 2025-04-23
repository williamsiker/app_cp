package com.example.lancelot

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lancelot.viewmodel.BrowserViewModel

data class Platform(
    val name: String,
    val icon: ImageVector,
    val url: String,
)

@OptIn(ExperimentalMaterial3Api::class)
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

    fun updateNavigationState(view: WebView?) {
        canGoBack = view?.canGoBack() ?: false
        canGoForward = view?.canGoForward() ?: false
        println("canGoBack: $canGoBack, canGoForward: $canGoForward")
    }

    LaunchedEffect(Unit) {
        webViewModel.updateLastUrl(url)
    }

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
                    IconButton(onClick = { webView?.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Atrás",
                            tint = if (canGoBack) Color.Unspecified else Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (webView?.canGoForward() == true) { // Double check to avoid crash
                                webView?.goForward()
                            } else {
                                // Handle cases where forward navigation is not possible (optional)
                            }
                        },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Adelante")
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
        }, floatingActionButton = {
            if (webViewModel.showFab) {

                FloatingActionButton(onClick = onCodeEditorNavigation) {
                    Icon(Icons.Default.Edit, contentDescription = "Abrir Editor")
                }
            }
        }
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            updateNavigationState(view)
                            url?.let { webViewModel.updateLastUrl(it) }
                        }
                    }

                    updateNavigationState(this)
                    // Listen for changes in navigation history
                    setOnKeyListener(android.view.View.OnKeyListener { _, keyCode, event ->
                        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP && canGoBack) {
                            goBack()
                            return@OnKeyListener true
                        }
                        false
                    })

                    loadUrl(url)
                }.also { webView = it }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}