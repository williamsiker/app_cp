package com.example.lancelot.configpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    onPopStackNav: () -> Unit,
    viewModel: ConfigurationViewModel = koinViewModel()
) {
    // Memoizar la lista de tabs para evitar recreaciones
    val tabs = remember { listOf("Languages", "Keywords", "Styles") }
    
    // Usar rememberSaveable para mantener el estado a través de recreaciones
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    // Optimizar la recolección del estado
    val state by viewModel.state.collectAsState()
    
    // Derivar estados computados de manera eficiente
    val styles by remember(state.styles) { derivedStateOf { state.styles } }
    val isLoading by remember { derivedStateOf { styles.isEmpty() } }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Configuration") },
                    navigationIcon = {
                        IconButton(
                            onClick = onPopStackNav,
                            modifier = Modifier.semantics { contentDescription = "Navigate back" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { innerPadding -> 
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                val tabRowContent = @Composable {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }

                TabRow(selectedTabIndex = selectedTab, tabs = tabRowContent)

                // Usar lazy loading para el contenido de las pestañas
                when (selectedTab) {
                    2 -> StylesTab(
                        styles = styles,
                        onAddStyle = remember(viewModel) { viewModel::addStyle },
                        onDeleteStyle = remember(viewModel) { viewModel::deleteStyle }
                    )
                    // Otros casos se cargarán solo cuando sean necesarios
                }
            }
        }
    }
}