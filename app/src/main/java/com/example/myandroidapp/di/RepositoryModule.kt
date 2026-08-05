package com.example.myandroidapp.di

import com.example.myandroidapp.data.local.ArticleDao
import com.example.myandroidapp.data.remote.ApiService
import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.domain.repository.ArticlesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * `DefaultArticlesRepository` vive en `:shared` y ya no lleva `@Inject constructor`
     * (javax.inject no compila para Native), así que no se puede usar `@Binds`.
     * Se construye explícitamente hasta que la Fase 4 reemplace Hilt por Koin.
     */
    @Provides
    @Singleton
    fun provideArticlesRepository(
        apiService: ApiService,
        articleDao: ArticleDao,
    ): ArticlesRepository = DefaultArticlesRepository(apiService, articleDao)
}
