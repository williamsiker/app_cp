package com.example.lancelot.configpanel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lancelot.configpanel.tabs.KeywordsTab
import com.example.lancelot.configpanel.tabs.LanguagesTab
import com.example.lancelot.configpanel.tabs.StylesTab
import com.example.lancelot.configpanel.viewmodel.ConfigurationViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    onPopStackNav: () -> Unit,
    viewModel: ConfigurationViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Languages", "Keywords", "Styles")
    val state by viewModel.state.collectAsState()

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
                0 -> LanguagesTab(
                    languages = state.languages,
                    onAddLanguage = viewModel::addLanguage,
                    onDeleteLanguage = viewModel::deleteLanguage
                )
                1 -> KeywordsTab(
                    state = state,
                    onLanguageSelect = viewModel::setSelectedLanguage,
                    onAddKeyword = viewModel::addKeyword,
                    onDeleteKeyword = viewModel::deleteKeyword,
                    onAddGroup = viewModel::addGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                    onUpdateGroupStyle = viewModel::updateGroupStyle,
                    onGroupSelect = viewModel::setSelectedGroup,
                    onStyleSelect = viewModel::setSelectedStyle
                )
                2 -> StylesTab(
                    styles = state.styles,
                    onAddStyle = viewModel::addStyle,
                    onDeleteStyle = viewModel::deleteStyle
                )
            }
        }
    }
}