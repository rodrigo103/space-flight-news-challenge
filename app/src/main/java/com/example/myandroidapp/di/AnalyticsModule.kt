package com.example.myandroidapp.di

import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.analytics.TimberAnalyticsHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsHelper(impl: TimberAnalyticsHelper): AnalyticsHelper
}
