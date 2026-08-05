package com.example.myandroidapp.di

import android.content.pm.ApplicationInfo
import com.example.myandroidapp.analytics.AnalyticsHelper
import com.example.myandroidapp.analytics.CompositeAnalyticsHelper
import com.example.myandroidapp.analytics.FirebaseAnalyticsHelper
import com.example.myandroidapp.analytics.LoggingAnalyticsHelper
import com.example.myandroidapp.data.connectivity.AndroidConnectivityObserver
import com.example.myandroidapp.data.connectivity.ConnectivityObserver
import com.example.myandroidapp.data.local.createDatabase
import com.example.myandroidapp.ui.articles.detail.ArticleDetailPaneViewModel
import com.example.myandroidapp.ui.articles.detail.ArticleDetailViewModel
import com.example.myandroidapp.ui.articles.list.ArticlesListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Grafo específico de Android. La capa data la aporta `sharedModule()`.
 */
val appModule = module {
    // AppDatabase se registra acá porque su construcción necesita Context; `sharedModule`
    // la consume con get().
    single { createDatabase(androidContext()) }

    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }

    single<AnalyticsHelper> {
        CompositeAnalyticsHelper(
            listOf(
                FirebaseAnalyticsHelper(androidContext()),
                LoggingAnalyticsHelper(),
            ),
        )
    }

    viewModel { ArticlesListViewModel(get(), get()) }
    viewModel { ArticleDetailPaneViewModel(get(), get()) }
    // SavedStateHandle lo inyecta Koin automáticamente al estar en el constructor.
    viewModel { ArticleDetailViewModel(get(), get(), get()) }
}

fun isDebugBuild(applicationInfo: ApplicationInfo): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
