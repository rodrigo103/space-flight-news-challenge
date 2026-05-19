---
tags:
  - wiki/architecture
---

# App Structure

> **Last verified:** 2026-05-19 | **Verified by:** [source] — removed `DataStoreModule`, `AppPreferences`, `preferences/` directory

Estructura del módulo `:app` en el proyecto `MeliChallenge`. App monomódulo con Jetpack Compose.

## Package structure

```
com.example.myandroidapp/
├── MyApplication.kt              # Hilt Application
├── MainActivity.kt               # Entry point, setContent
├── Routes.kt                     # Route sealed class definitions
├── Navigation.kt                 # NavHost + NavController setup
├── di/
│   ├── AppModule.kt              # Provider bindings (isDebug)
│   ├── DispatcherModule.kt       # Coroutine dispatchers
│   ├── NetworkModule.kt          # OkHttp + Retrofit + HttpErrorCallAdapterFactory
│   ├── RepositoryModule.kt       # Repository bindings
│   └── AnalyticsModule.kt        # AnalyticsHelper binding
├── data/
│   ├── Article.kt                # Domain model + API response
│   ├── ApiService.kt             # Retrofit interface
│   ├── ApiException.kt           # HTTP error sealed exception hierarchy
│   ├── HttpErrorCallAdapter.kt   # CallAdapter.Factory
│   ├── ArticlesRepository.kt     # Repository (API + Room)
│   ├── usecase/
│   │   └── GetArticleUseCase.kt  # Timeout + fetch article by ID
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── ArticleEntity.kt
│   │   ├── ArticleDao.kt
│   │   └── ArticleRemoteMediator.kt
│   ├── ui/
│   ├── DualPaneScreen.kt         # Adaptive layout (list + detail, tablet)
│   ├── ResponsiveApp.kt          # Window size-based routing (phone vs tablet)
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
│   │       ├── ArticleDetailViewModel.kt        # Phone detail (SavedStateHandle)
│   │       └── ArticleDetailPaneViewModel.kt  # Tablet detail (dynamic articleId)
│   └── preview/
├── analytics/
│   ├── AnalyticsHelper.kt
│   └── TimberAnalyticsHelper.kt
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
- Error handling automático via `CallAdapter.Factory` (no `Response<T>` manual checks)
