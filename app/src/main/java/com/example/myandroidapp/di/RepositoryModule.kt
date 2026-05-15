package com.example.myandroidapp.di

import com.example.myandroidapp.data.ArticlesRepository
import com.example.myandroidapp.data.DefaultArticlesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindArticlesRepository(impl: DefaultArticlesRepository): ArticlesRepository
}
