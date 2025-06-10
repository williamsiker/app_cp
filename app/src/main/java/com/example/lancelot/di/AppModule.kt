package com.example.lancelot.di

import com.example.lancelot.DatabaseProvider
import com.example.lancelot.AppDatabase
import com.example.lancelot.config.ConfigManager
import com.example.lancelot.configpanel.util.ConfigImporter
import com.example.lancelot.configpanel.ConfigurationViewModel
import com.example.lancelot.theme.data.RoomThemeRepository
import com.example.lancelot.theme.domain.ThemeRepository
import com.example.lancelot.execution.data.RustExecutionRepository
import com.example.lancelot.execution.domain.ExecutionRepository
import com.example.lancelot.execution.ExecutionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DatabaseProvider.getDatabase(get()) }
    single<ThemeRepository> { RoomThemeRepository(get<AppDatabase>().themeDAO()) }
    single<ExecutionRepository> { RustExecutionRepository() }
    single { ConfigImporter(get(), get()) }
    single { ConfigManager.getInstance().apply { initialize(get()) } }
    viewModel { ConfigurationViewModel(get(), get()) }
    viewModel { ExecutionViewModel(get()) }
    // BrowserViewModel y EditorViewModel se mantienen si no dependen de Keywords, KeywordGroup o Language
}