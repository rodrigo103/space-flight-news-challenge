---
tags:
  - wiki/architecture
---

# App Structure

> **Last verified:** 2026-05-19 | **Verified by:** [source] — reorganized into domain/data/ui layers, extracted mappers package

Estructura del módulo `:app` en el proyecto `MeliChallenge`. App monomódulo con Jetpack Compose, organizada en 3 capas (domain, data, ui) + di + analytics.

## Package structure

```
com.example.myandroidapp/
├── MyApplication.kt              # Hilt Application
├── MainActivity.kt               # Entry point, setContent
├── di/
│   ├── AppModule.kt              # Provider bindings (isDebug)
│   ├── DispatcherModule.kt       # Coroutine dispatchers (IoDispatcher, DefaultDispatcher)
│   ├── NetworkModule.kt          # OkHttp + Retrofit + HttpErrorCallAdapterFactory
│   ├── DatabaseModule.kt         # Room database + DAO
│   ├── RepositoryModule.kt       # Repository bindings
│   └── AnalyticsModule.kt        # AnalyticsHelper binding
├── domain/
│   ├── model/
│   │   └── Article.kt            # Domain model + ArticleResponse, Author, Socials
│   ├── repository/
│   │   └── ArticlesRepository.kt # Repository interface
│   └── usecase/
│       └── GetArticleUseCase.kt  # Timeout + fetch article by ID
├── data/
│   ├── remote/
│   │   ├── ApiService.kt         # Retrofit interface
│   │   ├── ApiException.kt       # HTTP error sealed exception hierarchy
│   │   └── HttpErrorCallAdapter.kt # CallAdapter.Factory
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── ArticleEntity.kt
│   │   ├── ArticleDao.kt
│   │   └── ArticleRemoteMediator.kt
│   ├── repository/
│   │   └── DefaultArticlesRepository.kt  # Repository implementation
│   ├── mappers/
│   │   └── ArticleMappers.kt      # Entity ↔ Domain mapping extensions
│   └── connectivity/
│       ├── ConnectivityStatus.kt    # Available / Unavailable enum
│       └── ConnectivityObserver.kt  # Reactive network monitoring (callbackFlow)
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   ├── Routes.kt              # Route sealed class definitions
│   │   ├── Navigation.kt          # NavHost + NavController setup
│   │   ├── ResponsiveApp.kt       # Window size-based routing (phone vs tablet)
│   │   └── DualPaneScreen.kt      # Adaptive layout (list + detail, tablet)
│   ├── common/
│   │   └── UiState.kt             # Loading / Success<T> / Error
│   ├── components/
│   │   └── OfflineBanner.kt       # Animated offline banner (slide in/out)
│   ├── articles/
│   │   ├── list/
│   │   │   ├── ArticlesListScreen.kt
│   │   │   ├── ArticlesListScreenRoute.kt
│   │   │   ├── ArticlesListScreenState.kt
│   │   │   ├── ArticleCardSettings.kt
│   │   │   └── ArticlesListViewModel.kt
│   │   └── detail/
│   │       ├── ArticleDetailScreen.kt
│   │       ├── ArticleDetailScreenRoute.kt
│   │       ├── ArticleDetailScreenState.kt
│   │       ├── ArticleDetailContentSettings.kt
│   │       ├── ArticleDetailViewModel.kt        # Phone detail (SavedStateHandle)
│   │       └── ArticleDetailPaneViewModel.kt    # Tablet detail (dynamic articleId)
│   └── preview/
│       ├── ArticlesListScreenPreviews.kt
│       └── ArticleDetailScreenPreviews.kt
└── analytics/
    ├── AnalyticsHelper.kt           # Interface
    ├── TimberAnalyticsHelper.kt     # Logcat logging
    ├── FirebaseAnalyticsHelper.kt   # Firebase events
    └── CompositeAnalyticsHelper.kt  # Composite pattern (multi-dispatch)
```

## Layer separation

| Layer | Package | Contents |
|-------|---------|----------|
| Domain | `domain/` | Models (`Article`, `ArticleResponse`), repository interface (`ArticlesRepository`), use cases (`GetArticleUseCase`) |
| Data | `data/` | Implementations: remote (`ApiService`, `ApiException`, `HttpErrorCallAdapter`), local (`Room` entities, DAOs, `RemoteMediator`), repository impl (`DefaultArticlesRepository`), mappers |
| UI | `ui/` | Theme, navigation, common (`UiState`), components, feature screens, previews |
| DI | `di/` | Hilt modules for each layer |
| Analytics | `analytics/` | Interface + Timber + Firebase + Composite pattern |

## Key characteristics

- Single module `:app`
- Min SDK 24, Target SDK 36, Compile SDK 36
- Kotlin 17 toolchain
- 100% Kotlin (no Java)
- Jetpack Compose + Material 3
- Error handling automático via `CallAdapter.Factory` (no `Response<T>` manual checks)

## App lifecycle

- **`MyApplication`**: `@HiltAndroidApp`. Planta `Timber.DebugTree()` solo en debug builds para logging condicional.
- **`MainActivity`**: `@AndroidEntryPoint`. Usa `installSplashScreen()` (AndroidX SplashScreen API) + `enableEdgeToEdge()` para display full-screen.

## Navigation

- **Type-safe routes**: `ArticlesRoute` (data object) y `DetailRoute(articleId: Int)` con kotlinx.serialization.
- **Animaciones**: slide horizontal 300ms via `tween()` + `slideInHorizontally/slideOutHorizontally`.
- **Dual-pane support**: `selectedArticleId` parameter en `MainNavigation` permite navegación programática desde `ResponsiveApp` cuando se selecciona un artículo en tablet.

## Lottie loading

Animación `res/raw/space_loading.json` usada en `ArticlesListScreen` durante el loading inicial. `<Comp>` + `LottieAnimation` para estados `LoadState.Loading`.
