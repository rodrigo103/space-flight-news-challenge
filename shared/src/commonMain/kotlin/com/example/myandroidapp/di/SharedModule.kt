package com.example.myandroidapp.di

import com.example.myandroidapp.data.local.AppDatabase
import com.example.myandroidapp.data.remote.createApiService
import com.example.myandroidapp.data.repository.DefaultArticlesRepository
import com.example.myandroidapp.domain.repository.ArticlesRepository
import com.example.myandroidapp.domain.usecase.GetArticleUseCase
import org.koin.dsl.module

/**
 * Grafo de la capa data, compartido por Android e iOS.
 *
 * El flag de logging entra por parámetro en vez de por qualifier: es un solo valor y así el
 * módulo no depende de que el consumidor registre un `named()` con el nombre correcto.
 *
 * `AppDatabase` **no** se declara acá porque su construcción necesita input de plataforma
 * (`Context` en Android, ruta de `NSFileManager` en iOS). Cada app la registra y este módulo
 * la consume con `get()`.
 */
fun sharedModule(enableHttpLogging: Boolean = false) = module {
    single { createApiService(enableLogging = enableHttpLogging) }
    single { get<AppDatabase>().articleDao() }
    single<ArticlesRepository> { DefaultArticlesRepository(get(), get()) }
    factory { GetArticleUseCase(get()) }
}
