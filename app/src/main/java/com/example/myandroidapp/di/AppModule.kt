package com.example.myandroidapp.di

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.myandroidapp.domain.repository.ArticlesRepository
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Named("isDebug")
    fun provideIsDebug(app: Application): Boolean {
        return app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    /**
     * `GetArticleUseCase` vive en `:shared` (commonMain), donde `javax.inject` no está
     * disponible porque no compila para Kotlin/Native. Hasta que la Fase 4 reemplace
     * Hilt por Koin, se construye explícitamente acá.
     */
    @Provides
    @Singleton
    fun provideGetArticleUseCase(repository: ArticlesRepository): GetArticleUseCase =
        GetArticleUseCase(repository)
}
