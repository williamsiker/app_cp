package com.example.lancelot

import android.app.Application
import com.example.lancelot.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LancelotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@LancelotApp)
            modules(appModule)
        }
    }
}