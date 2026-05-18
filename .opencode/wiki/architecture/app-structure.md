# App Structure

> **Last verified:** 2026-05-17 | **Verified by:** [source]

Estructura del módulo `:app` en el proyecto `MyAndroidApp`. App monomódulo con Jetpack Compose.

## Package structure

```
com.example.myandroidapp/
├── MyApplication.kt              # Hilt Application
├── MainActivity.kt               # Entry point, setContent
├── Routes.kt                     # Route sealed class definitions
├── Navigation.kt                 # NavHost + NavController setup
├── di/
│   ├── AppModule.kt              # Provider bindings (Context, DataStore)
│   ├── DatabaseModule.kt         # Room database + DAOs
│   ├── DataStoreModule.kt        # DataStore preferences provider
│   ├── DispatcherModule.kt       # Coroutine dispatchers
│   ├── NetworkModule.kt          # OkHttp + Retrofit
│   ├── RepositoryModule.kt       # Repository bindings
│   └── AnalyticsModule.kt        # AnalyticsHelper binding
├── data/
│   ├── Article.kt                # Domain model + API response
│   ├── ApiService.kt             # Retrofit interface (articles)
│   ├── ApiException.kt           # HTTP error exception type
│   ├── ResponseExt.kt            # Extension to handle Response
│   ├── HttpErrorCallAdapter.kt   # Custom CallAdapter for HTTP errors
│   ├── ArticlesRepository.kt     # Repository (API + Room)
│   ├── local/
│   │   ├── AppDatabase.kt        # Room database definition
│   │   ├── ArticleEntity.kt      # Room entity
│   │   ├── ArticleDao.kt         # Room DAO with Paging
│   │   └── ArticleRemoteMediator.kt  # Paging RemoteMediator for API + Room sync
│   └── preferences/
│       └── AppPreferences.kt     # DataStore preferences wrapper
├── ui/
│   ├── DualPaneScreen.kt         # Adaptive layout (list + detail)
│   ├── ResponsiveApp.kt          # Window size-based routing
│   ├── RepositoryEntryPoint.kt   # Hilt entry point for Repository
│   ├── articles/
│   │   ├── list/
│   │   │   ├── ArticlesListScreen.kt
│   │   │   ├── ArticlesListScreenRoute.kt
│   │   │   ├── ArticlesListScreenState.kt
│   │   │   └── ArticlesListViewModel.kt
│   │   └── detail/
│   │       ├── ArticleDetailScreen.kt
│   │       ├── ArticleDetailScreenRoute.kt
│   │       ├── ArticleDetailScreenState.kt
│   │       └── ArticleDetailViewModel.kt
│   └── preview/                  # Compose previews
├── analytics/
│   ├── AnalyticsHelper.kt        # Analytics interface
│   └── TimberAnalyticsHelper.kt  # Timber implementation
└── theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Key characteristics

- Single module `:app`
- Min SDK 24, Target SDK 36, Compile SDK 36
- Kotlin 17 toolchain
- 100% Kotlin (no Java)
- Jetpack Compose + Material 3