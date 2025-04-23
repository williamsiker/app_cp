package com.example.lancelot.configpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lancelot.AppDatabase
import com.example.lancelot.configpanel.tabs.KeywordsTab
import com.example.lancelot.configpanel.tabs.LanguagesTab
import com.example.lancelot.configpanel.tabs.StylesTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    onPopStackNav: () -> Unit,
    database: AppDatabase
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Languages", "Keywords", "Styles")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration") },
                navigationIcon = {
                    IconButton(onClick = { onPopStackNav() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding -> 
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            when (selectedTab) {
                0 -> LanguagesTab(database.languageDAO())
                1 -> KeywordsTab(database)
                2 -> StylesTab(database.styleDAO())
            }
        }
    }
}