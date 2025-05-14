package com.example.lancelot.di

import com.example.lancelot.DatabaseProvider
import com.example.lancelot.config.ConfigManager
import com.example.lancelot.configpanel.util.ConfigImporter
import com.example.lancelot.configpanel.ConfigurationViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DatabaseProvider.getDatabase(get()) }
    single { ConfigImporter(get(), get()) }
    single { ConfigManager.getInstance().apply { initialize(get()) } }
    viewModel { ConfigurationViewModel(get()) }
    // BrowserViewModel y EditorViewModel se mantienen si no dependen de Keywords, KeywordGroup o Language
}