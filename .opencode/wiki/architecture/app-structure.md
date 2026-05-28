---
tags:
  - wiki/architecture
---

# App Structure

> **Last verified:** 2026-05-28 | **Verified by:** [source] — extracted `:domain` as standalone Gradle module

Estructura del proyecto `MeliChallenge`. Proyecto **bimódulo** (`:app` + `:domain`) con Jetpack Compose, organizado en 3 capas (domain, data, ui) + di + analytics.

## Module structure

```
MeliChallenge/
├── :domain/                        # Gradle module — pure Kotlin/JVM, KMP-ready
│   └── build.gradle.kts            # kotlin-jvm + kotlinx.serialization + coroutines + paging-common + javax.inject
├── :app/                           # Gradle module — Android app
│   └── build.gradle.kts            # Depends on :domain
│
:domain/src/main/java/com/example/myandroidapp/
└── domain/
    ├── model/
    │   └── Article.kt              # Domain model + ArticleResponse, Author, Socials
    ├── repository/
    │   └── ArticlesRepository.kt   # Repository interface
    └── usecase/
        └── GetArticleUseCase.kt    # Timeout + fetch article by ID (no Timber, no Android deps)

:app/src/main/java/com/example/myandroidapp/
├── MyApplication.kt                # Hilt Application
├── MainActivity.kt                 # Entry point, setContent
├── di/
│   ├── AppModule.kt                # Provider bindings (isDebug)
│   ├── DispatcherModule.kt         # Coroutine dispatchers (IoDispatcher, DefaultDispatcher)
│   ├── NetworkModule.kt            # OkHttp + Retrofit + HttpErrorCallAdapterFactory
│   ├── DatabaseModule.kt           # Room database + DAO
│   ├── RepositoryModule.kt         # Repository bindings
│   └── AnalyticsModule.kt          # AnalyticsHelper binding
├── data/
│   ├── remote/
│   │   ├── ApiService.kt           # Retrofit interface
│   │   ├── ApiException.kt         # HTTP error sealed exception hierarchy
│   │   └── HttpErrorCallAdapter.kt # CallAdapter.Factory
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── ArticleEntity.kt
│   │   ├── ArticleDao.kt
│   │   └── ArticleRemoteMediator.kt
│   ├── repository/
│   │   └── DefaultArticlesRepository.kt  # Repository implementation
│   ├── mappers/
│   │   └── ArticleMappers.kt        # Entity ↔ Domain mapping extensions
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

| Layer | Module | Package | Contents |
|-------|--------|---------|----------|
| Domain | `:domain` | `domain/` | Models (`Article`, `ArticleResponse`), repository interface (`ArticlesRepository`), use cases (`GetArticleUseCase`) — pure Kotlin/JVM, KMP-ready |
| Data | `:app` | `data/` | Implementations: remote (`ApiService`, `ApiException`, `HttpErrorCallAdapter`), local (`Room` entities, DAOs, `RemoteMediator`), repository impl (`DefaultArticlesRepository`), mappers |
| UI | `:app` | `ui/` | Theme, navigation, common (`UiState`), components, feature screens, previews |
| DI | `:app` | `di/` | Hilt modules for each layer |
| Analytics | `:app` | `analytics/` | Interface + Timber + Firebase + Composite pattern |

## Key characteristics

- **Two modules**: `:domain` (pure Kotlin/JVM) and `:app` (Android app)
- `:domain` depends only on `kotlinx-serialization`, `kotlinx-coroutines-core`, `paging-common`, `javax.inject` — no Android, no Timber
- `:app` depends on `:domain` via `implementation(project(":domain"))`
- Hilt automatically injects domain classes (`GetArticleUseCase`, `ArticlesRepository`) into `:app` ViewModels
- Same package names across modules — no import changes needed
- Min SDK 24, Target SDK 36, Compile SDK 36
- Kotlin 17 toolchain (both modules)
- Jetpack Compose + Material 3
- Error handling automático via `CallAdapter.Factory` (no `Response<T>` manual checks)

## Modularization strategy

The `:domain` extraction is the **first step** toward KMP and feature-based modularization:

```
Current (2 modules):  :domain ← :app
Future (KMP ready):   :shared (domain+data+ui KMP) ← :androidApp / :iosApp
Future (feature):     :domain ← :feature:articles ← :app
```

The `:domain` module has **zero Android dependencies**, making it reusable for:
- KMP shared module (Kotlin Multiplatform)
- Unit testing without Android runtime
- Potential reuse in a backend Ktor module

## App lifecycle

- **`MyApplication`**: `@HiltAndroidApp`. Planta `Timber.DebugTree()` solo en debug builds para logging condicional.
- **`MainActivity`**: `@AndroidEntryPoint`. Usa `installSplashScreen()` (AndroidX SplashScreen API) + `enableEdgeToEdge()` para display full-screen.

## Navigation

- **Type-safe routes**: `ArticlesRoute` (data object) y `DetailRoute(articleId: Int)` con kotlinx.serialization.
- **Animaciones**: slide horizontal 300ms via `tween()` + `slideInHorizontally/slideOutHorizontally`.
- **Dual-pane support**: `selectedArticleId` parameter en `MainNavigation` permite navegación programática desde `ResponsiveApp` cuando se selecciona un artículo en tablet.

## Lottie loading

Animación `res/raw/space_loading.json` usada en `ArticlesListScreen` durante el loading inicial. `<Comp>` + `LottieAnimation` para estados `LoadState.Loading`.
