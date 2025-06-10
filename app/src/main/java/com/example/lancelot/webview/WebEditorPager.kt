package com.example.lancelot.webview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
<<<<<<< HEAD
=======
import androidx.compose.foundation.layout.fillMaxSize
>>>>>>> cb2203e70159d386ad1ab6ec7ec89575b55e865a
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lancelot.mcpe.components.EditorScaffold
<<<<<<< HEAD
=======
import com.example.lancelot.webview.WebViewContent
>>>>>>> cb2203e70159d386ad1ab6ec7ec89575b55e865a
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WebEditorPagerScreen(
    startUrl: String = "https://atcoder.jp",
    onConfigNavigation: () -> Unit,
    onDebugNavigation: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0)
    val browserViewModel: ViewModel = viewModel()
    val lastUrl = browserViewModel.lastUrl.collectAsState().value ?: startUrl
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lancelot") },
                actions = {
                    IconButton(onClick = onDebugNavigation) {
                        Icon(Icons.Outlined.BugReport, contentDescription = "Debug")
                    }
                    IconButton(onClick = onConfigNavigation) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Config")
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            pageCount = 2,
            state = pagerState,
            modifier = Modifier.padding(padding)
        ) { page ->
            when (page) {
<<<<<<< HEAD
                0 -> WebViewScreen(
                    url = lastUrl,
                    onCodeEditorNavigation = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
                else -> EditorScaffold(onConfigNavigation = onConfigNavigation)
=======
                0 -> WebViewContent(
                    url = lastUrl,
                    onCodeEditorNavigation = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    modifier = Modifier.fillMaxSize(),
                    showFab = true
                )
                else -> EditorScaffold(
                    onConfigNavigation = onConfigNavigation,
                    embedded = true,
                    modifier = Modifier.fillMaxSize()
                )
>>>>>>> cb2203e70159d386ad1ab6ec7ec89575b55e865a
            }
        }
    }
}
