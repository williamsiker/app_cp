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
    single { ConfigImporter(get(), get()) }  // Changed this line to use the context from Koin
    viewModel { ConfigurationViewModel(get()) }
    viewModel { BrowserViewModel() }
    viewModel { EditorViewModel() }
}