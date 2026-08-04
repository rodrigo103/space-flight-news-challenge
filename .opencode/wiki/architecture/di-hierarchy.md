---
tags:
  - wiki/architecture
---

# DI Hierarchy

> **Last verified:** 2026-08-04 | **Verified by:** [source] — corregido binding de `AnalyticsModule` (era `TimberAnalyticsHelper`, es `CompositeAnalyticsHelper`)

Jerarquía de módulos de Hilt DI. Todos los módulos están en `com.example.myandroidapp.di`.

RepositoryEntryPoint eliminado — ya no se pasa repository a composables.

## Módulos

| Módulo | Scope | Bindings |
|---|---|---|
| `AppModule` | Singleton | `isDebug` named binding |
| `NetworkModule` | Singleton | `OkHttpClient`, `Retrofit`, `ApiService` |
| `DatabaseModule` | Singleton | `AppDatabase`, `ArticleDao` |
| `RepositoryModule` | Singleton | `ArticlesRepository` |
| `DispatcherModule` | Singleton | `CoroutineDispatchers` provider |
| `AnalyticsModule` | Singleton | `AnalyticsHelper` → `CompositeAnalyticsHelper(listOf(firebase, timber))` |

## Dependencies entre módulos

```
AppModule (isDebug)
├── NetworkModule (OkHttp, Retrofit, ApiService)
├── DatabaseModule (Room, DAOs)
│   └── DispatcherModule (Dispatchers)
└── RepositoryModule (ArticlesRepository)
    ├── NetworkModule
    ├── DatabaseModule
    └── DispatcherModule
```

## Entry points

- `MainActivity` — `@AndroidEntryPoint`
- `MyApplication` — `@HiltAndroidApp`

## ViewModel multibinding

A escala, se puede usar `@IntoMap` + `@ViewModelKey` para registrar ViewModels automáticamente:

```kotlin
@Module
abstract class ViewModelModule {
    @Binds @IntoMap @ViewModelKey(ArticlesViewModel::class)
    abstract fun bindArticlesViewModel(viewModel: ArticlesViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(DetailViewModel::class)
    abstract fun bindDetailViewModel(viewModel: DetailViewModel): ViewModel
}
```

## Multi-environment Factory Pattern

Para apps que necesitan comportamientos distintos según país/entorno:

```
interface EnvironmentFactory {
    fun provideApiClient(): RetrofitApiClient
    fun provideAuthInterceptor(): Interceptor
}

class EnvironmentFactoryProd : EnvironmentFactory { /* API real */ }
class EnvironmentFactoryDemo : EnvironmentFactory { /* mock API */ }
```

Cada entorno tiene su propia autenticación, endpoints y reglas de negocio. El Abstract Factory pattern permite switchear todo sin `if/else`.

## Scopes custom

Para componentes con ciclo de vida propio (ej: sesión de usuario):

```kotlin
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class SessionScope
```

## Ver también

- [[tools/hilt-setup]] — Detalles de los módulos Hilt
- [[architecture/clean-architecture-guide]] — Clean Architecture