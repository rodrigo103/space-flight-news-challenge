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

### Current state (incremental, 2 modules)

```
:domain  ←  :app
```

This is the **minimal viable modularization** that demonstrates separation of concerns at the Gradle level. `:domain` is pure Kotlin/JVM — it can be reused in a KMP shared module, backend Ktor service, or unit-tested without Android runtime.

### Phase 2 — Feature-based (3+ modules)

Split `:app` by feature when the app grows beyond ~3 screens:

```
:domain          ←  :feature:articles  ←  :app
                  ←  :feature:search
                  ←  :feature:settings
                  ←  :core:ui           (design system, shared composables)
                  ←  :core:network      (Retrofit, OkHttp)
                  ←  :core:database     (Room)
                  ←  :core:navigation   (Routes, navigator)
```

Each `:feature:*` module contains its own **domain + data + presentation** layers, making features self-contained and independently testable. `:core:*` modules hold cross-cutting infrastructure. `:app` becomes the shell — just `MainActivity`, `Application`, and DI wiring.

### Phase 3 — KMP feature-based

The endgame for multi-platform. Convert the feature modules to Kotlin Multiplatform:

```
:shared (KMP)
 ├── :core
 │    ├── :model           — Article.kt, DTOs (commonMain, zero deps)
 │    ├── :network         — Ktor HttpClient + ApiService interface
 │    ├── :database        — SQLDelight .sq files + DAOs
 │    └── :ui              — Design system (Compose Multiplatform)
 │
 ├── :feature:articles
 │    ├── :domain          — ArticlesRepository interface, GetArticlesUseCase
 │    ├── :data            — Repository impl (Ktor + SQLDelight)
 │    └── :ui              — ArticlesListScreen, ArticleDetailScreen (Compose MP)
 │
 └── :feature:search
      └── ...

:androidApp   ← MainActivity, platform DI, Firebase, SplashScreen
:iosApp       ← SwiftUI entry point, platform DI
```

**Key differences between current (Android-pure) and KMP feature-based:**

| Concept | Current | Feature-based KMP |
|---|---|---|
| DI | Hilt (Android-only) | Koin (KMP) or kotlin-inject |
| HTTP | Retrofit + OkHttp | Ktor Client |
| DB | Room | SQLDelight |
| ViewModels | `androidx.lifecycle.ViewModel` | Plain class + `StateFlow` (StateHolder pattern) |
| Navigation | Jetpack Navigation Compose | Custom navigator or Voyager |
| Images | Coil | Coil MP or Kamel |
| Analytics | Firebase | `expect/actual` (Firebase on Android, custom on iOS) |

### Migration path

```
1. Extract :domain (current)
2. Extract :core:model (:domain becomes split into model + repository interfaces)
3. Introduce Ktor Client as alternative to Retrofit (feature-flagged)
4. Extract :feature:articles (still Android-only, but self-contained)
5. Convert :feature:articles to KMP (Ktor + SQLDelight + Compose MP)
6. Add :iosApp entry point
7. Repeat for remaining features
```

The first step (extract `:domain`) is the hardest because it creates the Gradle module boundary. Every subsequent extraction follows the same pattern and gets easier.

## App lifecycle

- **`MyApplication`**: `@HiltAndroidApp`. Planta `Timber.DebugTree()` solo en debug builds para logging condicional.
- **`MainActivity`**: `@AndroidEntryPoint`. Usa `installSplashScreen()` (AndroidX SplashScreen API) + `enableEdgeToEdge()` para display full-screen.

## Navigation

- **Type-safe routes**: `ArticlesRoute` (data object) y `DetailRoute(articleId: Int)` con kotlinx.serialization.
- **Animaciones**: slide horizontal 300ms via `tween()` + `slideInHorizontally/slideOutHorizontally`.
- **Dual-pane support**: `selectedArticleId` parameter en `MainNavigation` permite navegación programática desde `ResponsiveApp` cuando se selecciona un artículo en tablet.

## Lottie loading

Animación `res/raw/space_loading.json` usada en `ArticlesListScreen` durante el loading inicial. `<Comp>` + `LottieAnimation` para estados `LoadState.Loading`.
