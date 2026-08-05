package com.example.myandroidapp.di

import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.remote.createApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiService(@Named("isDebug") isDebug: Boolean): ApiService =
        createApiService(enableLogging = isDebug)
}
