package com.example.myandroidapp

import android.app.Application
import com.example.myandroidapp.di.appModule
import com.example.myandroidapp.di.isDebugBuild
import com.example.myandroidapp.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val isDebug = isDebugBuild(applicationInfo)
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            if (isDebug) androidLogger(Level.ERROR)
            androidContext(this@MyApplication)
            modules(sharedModule(enableHttpLogging = isDebug), appModule)
        }
    }
}
