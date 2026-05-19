package com.example.myandroidapp.di

import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.domain.repository.ArticlesRepository
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
