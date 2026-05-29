package com.example.myandroidapp.di

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Named("isDebug")
    fun provideIsDebug(app: Application): Boolean {
        return app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
