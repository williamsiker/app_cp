package com.example.lancelot.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContent(
    url: String,
    onCodeEditorNavigation: () -> Unit,
    modifier: Modifier = Modifier,
    showFab: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val webViewModel = viewModel<ViewModel>()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        webViewModel.updateLastUrl(url)
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            view?.let {
                                webViewModel.updateNavigationState(
                                    it.canGoBack(),
                                    it.canGoForward()
                                )
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            view?.let {
                                webViewModel.updateNavigationState(
                                    it.canGoBack(),
                                    it.canGoForward()
                                )
                            }
                            url?.let { webViewModel.updateLastUrl(it) }
                        }
                    }

                    loadUrl(url)
                }.also { webView = it }
            },
            modifier = Modifier
                .fillMaxSize()
        )
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }
        if (showFab && webViewModel.showFab) {
            FloatingActionButton(
                onClick = onCodeEditorNavigation,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Abrir Editor")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    onCodeEditorNavigation: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val webViewModel = viewModel<ViewModel>()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        webViewModel.updateLastUrl(url)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browser") },
                navigationIcon = {
                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = webViewModel.canGoBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Atrás",
                            tint = if (webViewModel.canGoBack) Color.Unspecified else Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { webView?.goForward() },
                        enabled = webViewModel.canGoForward
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
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, webView?.url)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share URL"))
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in Browser") },
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url))
                                context.startActivity(intent)
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        WebViewContent(
            url = url,
            onCodeEditorNavigation = onCodeEditorNavigation,
            modifier = Modifier.padding(padding),
            showFab = true
        )
    }
}
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                view?.let {
                                    webViewModel.updateNavigationState(
                                        it.canGoBack(),
                                        it.canGoForward()
                                    )
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                view?.let {
                                    webViewModel.updateNavigationState(
                                        it.canGoBack(),
                                        it.canGoForward()
                                    )
                                }
                                url?.let { webViewModel.updateLastUrl(it) }
                            }
                        }

                        loadUrl(url)
                    }.also { webView = it }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
        }
    }
}