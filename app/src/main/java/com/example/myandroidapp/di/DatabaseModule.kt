package com.example.myandroidapp.di

import android.content.Context
import com.example.myandroidapp.data.local.AppDatabase
import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.local.createDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        createDatabase(context)

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticleDao = database.articleDao()
}
