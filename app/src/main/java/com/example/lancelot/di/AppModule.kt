package com.example.lancelot.di

import android.content.Context
import com.example.lancelot.DatabaseProvider
import com.example.lancelot.configpanel.util.ConfigImporter
import com.example.lancelot.configpanel.viewmodel.ConfigurationViewModel
import com.example.lancelot.mcpe.viewmodel.EditorViewModel
import com.example.lancelot.viewmodel.BrowserViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DatabaseProvider.getDatabase(get()) }
    single { ConfigImporter(get(), get()) }
    viewModel { ConfigurationViewModel(get()) }
    // BrowserViewModel y EditorViewModel se mantienen si no dependen de Keywords, KeywordGroup o Language
}