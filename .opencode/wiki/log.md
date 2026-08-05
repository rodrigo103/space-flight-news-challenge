# Changelog del Wiki

## 2026-08-04 (3)

- **Fase 1 de [[patterns/kmp-migration]] completada** — `:domain` (Kotlin/JVM) reemplazado por `:shared` (KMP)
  - `shared/build.gradle.kts` con `kotlin-multiplatform` + `com.android.kotlin.multiplatform.library` (plugin recomendado con AGP 9) y 4 targets; los iOS producen `Shared.framework`
  - Fuentes movidas con `git mv` (historial preservado); `:domain` eliminado
  - `javax.inject` eliminado de `GetArticleUseCase` (no compila para Native) + puente `@Provides` en `AppModule` hasta la Fase 4
  - `paging-common` pasa a `api(...)`: `ArticlesRepository` expone `Flow<PagingData<Article>>` en su API pública
  - Verificado: `:app:assembleDebug` ✅, `linkDebugFrameworkIosSimulatorArm64` ✅, 40 tests ✅, detekt ✅
  - **Confirmación empírica del problema de `kotlin.Result`**: en el header ObjC generado, `Result<Article>` se borra a `id` (`Any?` en Swift) — se pierde también `Article`, y compila sin warnings. Documentado en la página
  - Deuda registrada: detekt no está aplicado a `:shared`
  - Actualizado [[architecture/app-structure]] — estructura de módulos, tabla de capas, características y sección de modularización

## 2026-08-04 (2)

- Creada [[patterns/kmp-migration]] — plan de migración a Kotlin Multiplatform
  - **Arquitectura**: `:shared` (domain + data) con 4 targets; UI nativa por plataforma (Compose en Android, SwiftUI en iOS); la presentación no se comparte
  - **Room KMP en vez de SQLDelight** — Room 2.8.4 soporta KMP con `BundledSQLiteDriver` + `@ConstructedBy` + KSP por target. DAOs, entidades y RemoteMediator sobreviven. Corrige la suposición de la "Phase 3" de [[architecture/app-structure]]
  - **Tres problemas de interop Kotlin↔Swift documentados**: `kotlin.Result` no se exporta (es value class) → sealed `ApiResult`; `PagingData` no es consumible desde SwiftUI → iOS usa `getArticles(limit, offset)`; `Flow`/`suspend` → SKIE
  - **Bloqueadores concretos** — tabla archivo por archivo: `javax.inject` en `:domain`, excepciones `java.net.*` en `ArticleRemoteMediator`, Timber en el repositorio, Retrofit/OkHttp, `ConnectivityManager`, Firebase
  - 8 fases con criterio de verificación, riesgos y estado del entorno (Xcode 26.6 instalado, sin runtimes de simulador)
  - Decisiones del usuario registradas: lógica compartida + SwiftUI nativa, Koin en todo el proyecto, tests migrados a `commonTest` con fakes
  - [[index]] actualizado con link a la nueva página

- Fase 0 del plan KMP ejecutada (verificación de versiones, sin cambios de código)
  - **SKIE 0.10.14 soporta Kotlin 2.0.0 → 2.4.10**; el proyecto está en 2.3.20 → sin bloqueo. El riesgo principal del plan queda despejado
  - Lección registrada en la página: el índice de ctx7 devolvía "hasta Kotlin 2.3.10" (desactualizado) y habría hecho abortar el plan. La página en vivo dice 2.4.10 — verificar versiones contra la fuente en vivo, no contra índices cacheados
  - Entorno verificado: Xcode 26.6 + SDK iOS 26.5 instalados y licencia aceptada, pero **sin runtimes de simulador** (falta `xcodebuild -downloadPlatform iOS`)

## 2026-08-04

- Wiki audit: contradicciones entre wiki y código, verificadas una por una contra el source
  - **Binding de analytics corregido** — [[architecture/di-hierarchy]] y [[tools/hilt-setup]] decían que `AnalyticsModule` bindea `AnalyticsHelper → TimberAnalyticsHelper`. El código bindea `CompositeAnalyticsHelper(listOf(firebase, timber))`, como ya documentaba [[patterns/analytics-composite]]
  - **ProGuard/R8 corregido** — [[processes/build-and-test]] decía "minification deshabilitado". El release tiene `isMinifyEnabled = true`, `isShrinkResources = true` y `proguard-android-optimize.txt`. Sección reescrita con el bloque real
  - **`allowBackup` corregido** — [[patterns/secure-coding]] marcaba ⚠️ `true` para este proyecto; el manifest dice `false` desde el commit `d16e678`
  - **Rename del proyecto** — [[architecture/app-structure]] decía `MeliChallenge`; `rootProject.name` es `SpaceFlightNews` desde `63964a0`. Agregada nota de que los paquetes siguen siendo `com.example.myandroidapp`
  - **Links rotos** — `[[tool/retrofit-setup]]` → [[tools/retrofit-setup]] en [[patterns/secure-coding]]; link relativo `../mvi-lite` en [[patterns/mvvm-vs-mvi]] reemplazado por referencia a las ramas git `mvi-lite` / `mvi-full` (ambas existen local y en `origin`)
  - **Formato** — [[tools/retrofit-setup]] no tenía título ni header `Last verified`; tabla de [[tools/key-dependencies]] estaba partida en dos por una línea en blanco

- Documentado el feature de delete del detalle (commit `1cdfb26`, sin registrar en el wiki hasta ahora)
  - Actualizado [[patterns/attributes-actions]] — nueva sección "Navegación disparada desde el estado": `onDelete` en `ArticleDetailActions`, flag `navigateBack` en `ArticleDetailState`, `LaunchedEffect` en el Route
  - **`onDeleteArticle()` no borra nada** — no toca Room ni la API, solo loguea `article_deleted` y marca `navigateBack = true`. Documentado explícitamente para que no se lea como una operación de datos
  - Documentado el tradeoff del flag en el estado (no se resetea tras consumirse) vs `Channel<SideEffect>` — ver [[patterns/mvvm-vs-mvi]]
  - Actualizado [[patterns/analytics-composite]] — agregado `article_deleted` a la tabla de eventos

## 2026-05-29

- Creada [[patterns/mvvm-vs-mvi]] — comparativa exhaustiva MVVM vs MVI en Android
  - Tablas comparativas: estructura, flujo de datos, entry points, estado, eventos, side effects
  - Ejemplos de código reales del proyecto (misma funcionalidad en ambos patrones)
  - Diagramas de flujo visuales lado a lado
  - MVI Lite como punto medio pragmático
  - FAQ: Paging 3 + MVI, librerías, inmutabilidad
  - Guía de cuándo usar cada patrón según tamaño y criticidad del proyecto
  - [[index]] actualizado con link a la nueva página

- Creada [[patterns/secure-coding]] — guía completa de secure coding basada en análisis de repos Ualá
  - **AES/CBC/PKCS7** — encriptación del PIN en AndroidKeyStore con `setUserAuthenticationRequired`
  - **SHA-256** — RSA-OAEP-SHA256 para datos al backend + hashing de deviceId (migrado de SHA-1)
  - **TLS / Network Security Config** — solo system CAs en release, TLS 1.2+ para Auth0
  - **ProGuard/R8** — ofuscación en release con 188 keep rules (SQLCipher, Retrofit, kotlinx.serialization, OkHttp)
  - **EncryptedSharedPrefs + SQLCipher** — triple capa: Keystore → EncryptedSharedPrefs (AES-256-GCM) → SQLCipher (AES-256 SQLite)
  - **Root Detection** — RootBeer en onboarding, bloquea biometría si detecta root
  - **BiometricPrompt + CryptoObject** — `BIOMETRIC_STRONG` + binding criptográfico vía TEE
  - Incluye explicaciones de cada tradeoff (CBC vs GCM, SHA-256 vs SHA-1, SIV vs GCM para keys)
  - [[index]] actualizado con link a la nueva página

- Search strategy: migrado de Option B (Room-only) a stale-while-revalidate (híbrido A+B)
  - **`ArticleRemoteMediator`** — acepta `searchQuery: String?`, lo pasa a `apiService.getArticles(search = ...)`
  - **`DefaultArticlesRepository.getArticlesPaged`** — eliminada rama `remoteMediator = null`; siempre se crea `ArticleRemoteMediator` con el query
  - **`ArticleDao.searchPagingSource`** — extendido a buscar en `title` y `summary`
  - Resultado: Room emite stale instantáneo, RemoteMediator refresca desde API en paralelo, offline fallback automático
  - Actualizado [[patterns/search-strategy]] — nueva arquitectura, flujo de datos, tabla de archivos clave

- Creada [[patterns/mvi-migration]] — plan de migración MVVM → MVI, archivo por archivo
  - **MVI completo** — `sealed interface Event`, `StateFlow<ScreenState>` unificado, `Channel<SideEffect>`, `fun onEvent(Event)` como único entry point
  - **MVI Lite** (recomendado) — solo side effects canalizados, mantiene Attributes/Actions y MutableStateFlow separados
  - Cubre los 10 archivos del feature list + detail + DualPaneScreen + ResponsiveApp
  - Incluye antes/después de cada archivo, tabla comparativa de las 3 opciones, beneficios, riesgos y recomendación por tamaño de proyecto
  - [[index]] actualizado con link a la nueva página

## 2026-05-28

- Modularización: extraído `:domain` como módulo Gradle independiente (Kotlin puro/JVM)
  - **Nuevo módulo `:domain`** — `kotlin-jvm` + `kotlinx-serialization` + `coroutines-core` + `paging-common` + `javax.inject`
  - **Movidos**: `domain/model/Article.kt`, `domain/repository/ArticlesRepository.kt`, `domain/usecase/GetArticleUseCase.kt`
  - **Limpiado** `GetArticleUseCase` — removida dependencia de `Timber` (no es pure-Kotlin)
  - **Eliminado** directorio `domain/` de `:app`
  - **Agregado** `implementation(project(":domain"))` en `:app/build.gradle.kts`
  - **TOML actualizado** — nuevas entries: plugin `kotlin-jvm`, libraries `kotlinx-coroutines-core`, `javax-inject`
  - **Root build.gradle.kts** — registrado `kotlin-jvm` con `apply false`
  - `assembleDebug`, `test`, `detekt` verificados — todo verde
  - Actualizado [[architecture/app-structure]] — estructura bimódulo, tabla de capas con columna Module, nueva sección "Modularization strategy"

## 2026-05-19 (9)

- Reorganización arquitectónica del proyecto en 3 capas (domain/data/ui)
  - **Creado `domain/`** — capa de dominio pura:
    - `domain/model/Article.kt` — modelo de dominio (`Article`, `ArticleResponse`, `Author`, `Socials`)
    - `domain/repository/ArticlesRepository.kt` — interfaz del repositorio
    - `domain/usecase/GetArticleUseCase.kt` — use case con timeout + cache fallback
  - **Reorganizado `data/`** — solo implementaciones:
    - `data/remote/` — `ApiService`, `ApiException`, `HttpErrorCallAdapter`
    - `data/local/` — Room entities, DAOs, RemoteMediator (sin cambios)
    - `data/repository/DefaultArticlesRepository.kt` — implementación del repositorio
    - `data/mappers/ArticleMappers.kt` — extensiones de mapeo Entity ↔ Domain (extraídas de `ArticleEntity`)
  - **Reorganizado `ui/`**:
    - `ui/navigation/` — `Routes`, `Navigation`, `ResponsiveApp`, `DualPaneScreen`
    - `ui/common/UiState.kt` — estado genérico Loading/Success/Error
    - `ui/components/OfflineBanner.kt` — sin cambios
    - `ui/articles/`, `ui/preview/`, `ui/theme/` — sin cambios
  - **Eliminados directorios vacíos**: `data/usecase/`
  - Actualizado [[architecture/app-structure]] — nueva estructura de paquetes + tabla de capas
  - Actualizado [[architecture/data-layer]] — nuevos paths `data/remote/`, `data/mappers/`, separación interfaz/impl

## 2026-05-19 (8)

- Documentada estructura de la Spaceflight News API: schema idéntico en list/detail, sin campo `body`/`content`
  - Actualizado [[architecture/data-layer]] — nota sobre API agregadora, schema idéntico, decisión de mantener fetch del detalle
  - Actualizado [[tools/retrofit-setup]] — nueva sección "Estructura de la API"
  - Actualizado [[patterns/mvvm-repository]] — nota sobre redundancia del fetch individual y por qué se mantiene

## 2026-05-19 (7)

- Offline detection + graceful degradation
  - Created `data/connectivity/ConnectivityStatus` (enum: Available, Unavailable)
  - Created `data/connectivity/ConnectivityObserver` — reactive monitoring via `ConnectivityManager.NetworkCallback` + `callbackFlow`
  - Created `ui/components/OfflineBanner` — animated slide-in/out banner with `SignalWifiOff` icon
  - Updated `MainActivity` — injects `ConnectivityObserver`, passes `Flow<ConnectivityStatus>` to `ResponsiveApp`
  - Updated `ui/ResponsiveApp` — overlays `OfflineBanner` on top of entire app when offline
  - Updated `ArticleRemoteMediator` — network exceptions (`UnknownHostException`, `ConnectException`, `SocketTimeoutException`) return `Success` instead of `Error`, keeping cached Paging data visible
  - Updated `ArticlesListScreen` — full-screen error only when `itemCount == 0`; otherwise shows cached list + Snackbar
  - Added `ArticlesRepository.getCachedArticle(id)` — Room fallback for detail screen
  - Updated `GetArticleUseCase` — 3-tier strategy: API → Room cache → timeout error
  - Updated `ArticleDetailScreen` + `DualPaneScreen` — added retry button on error state
  - Added `ACCESS_NETWORK_STATE` permission to `AndroidManifest.xml`
  - Added `offline_banner` string resource
  - Updated [[architecture/app-structure]] — added `connectivity/` and `components/` directories
  - Updated [[architecture/responsive-layout]] — documented ConnectivityObserver + OfflineBanner integration
  - Updated [[patterns/room-paging]] — documented offline-first RemoteMediator error handling
  - Updated [[patterns/mvvm-repository]] — documented GetArticleUseCase cache fallback

## 2026-05-19 (6)

- Wiki audit: recorrido completo de código → wiki
  - **Fixes:**
    - Fixed duplicate YAML frontmatter in [[tools/detekt-setup]] and [[patterns/attributes-actions]]
    - Removed stale DataStore dependency from [[tools/key-dependencies]]
    - Fixed broken link `[[processes/pr-workflow]]` (page never existed) → [[processes/build-and-test]]
    - Fixed `[[architecture/app-structure]]` indentation (ui/ incorrectly nested under data/)
  - **New pages:**
    - [[patterns/analytics-composite]] — Analytics architecture: interface + Timber + Firebase + Composite
    - [[architecture/responsive-layout]] — Responsive layout: boxWithConstraints, 840dp breakpoint, DualPaneScreen, pane ViewModel
  - **Updated [[architecture/app-structure]]** — Added MyApplication lifecycle, SplashScreen API, edge-to-edge, navigation animations, Lottie loading, Settings files, UiState.kt, corrected directory tree
  - Updated [[index]] — Added new pages, corrected "Preferences" reference

## 2026-05-19 (5)

- Updated [[architecture/app-structure]] — Renombrado proyecto de `MyAndroidApp` a `MeliChallenge`

## 2026-05-19 (4)

- Updated [[patterns/attributes-actions]] — Documentada limitación de Paging 3 en Compose Preview (`AndroidUiDispatcher.Main`), removidos previews de datos/vacío/búsqueda que no funcionaban, actualizado conteo a 3 previews (loading + cards).

## 2026-05-19 (3)

- Updated [[processes/build-and-test]] — SonarCloud movido de "CI avanzado (a futuro)" a sección propia con descripción del workflow real
- Updated [[patterns/mvvm-repository]] — "Cache optimista" reemplazado por "Stale-while-revalidate via Paging 3", descripción del patrón real implementado con RemoteMediator + Room. Eliminados duplicados.

## 2026-05-19 (2)

- Batch wiki update: ingested content from deleted `.md` files into wiki pages
  - **Created 5 new pages:**
    - [[architecture/clean-architecture-guide]] — Clean Architecture 3 capas, regla de dependencia, use cases
    - [[patterns/search-strategy]] — Búsqueda local vs API: decisión arquitectónica, combine+flatMapLatest
    - [[patterns/settings-composable]] — Settings+Composable pattern, color interfaces, typography system
    - [[processes/tech-defense-guide]] — Guía de defensa técnica: Hilt, Navigation, testing, errores
    - [[tools/testing-strategy]] — Testing: MockK, MockWebServer, Turbine, semantic properties, fakes
  - **Updated 8 existing pages:**
    - [[patterns/attributes-actions]] — Expanded analytics section, removed external references
    - [[patterns/error-handling]] — Added timeout pattern, retry pattern, error hierarchy, ver también
    - [[patterns/room-paging]] — Added indices, chunked inserts, cache check, combine+flatMapLatest
    - [[patterns/mvvm-repository]] — Added use cases, cache optimista, operator fun invoke
    - [[architecture/di-hierarchy]] — Added ViewModel multibinding, multi-environment factory, scopes
    - [[tools/retrofit-setup]] — Added logging interceptor condicional, ver también links
    - [[tools/key-dependencies]] — Added decision tables: Coil/Glide, kotlinx/Gson, StateFlow/LiveData, TOML/buildSrc
    - [[processes/build-and-test]] — Added CI avanzado patterns (MobSF, release train, GMD)
  - **Updated [[index]]** — Cleaned duplicates, added new pages, removed stale descriptions
- Deleted source files from working tree: 17 `.md` files removed (analisis-*, decision-*, defensa-*, patron-*, plan-*, ranking-*)

## 2026-05-19

- Removed `data/preferences/AppPreferences` and `di/DataStoreModule` — no se consumía (`lastOpenedArticleId` sin lectores, `isDarkMode` sin uso)
  - Updated [[architecture/app-structure]] — removed `DataStoreModule` and `preferences/` directory
  - Updated [[tools/hilt-setup]] — removed `DataStoreModule` row, updated `AppModule` bindings
  - Updated [[architecture/di-hierarchy]] — removed `DataStoreModule` row and dependency, updated `AppModule` bindings
  - Updated [[architecture/data-layer]] — removed DataStore Preferences section
  - Removed `datastore.preferences` dependency from `build.gradle.kts`

## 2026-05-18 (2)

- Refactor DualPaneScreen + introducción de `GetArticleUseCase`
  - Created `data/usecase/GetArticleUseCase` — pure domain use case: timeout + fetch article by ID
  - Created `ui/articles/detail/ArticleDetailPaneViewModel` — ViewModel para tablet detail pane (dynamic articleId)
  - Updated `DualPaneScreen` — eliminado `repository` param, usa `ArticlesListViewModel` + `ArticleDetailPaneViewModel` como default params, estado de selección local
  - Updated `ArticleDetailViewModel` — ahora usa `GetArticleUseCase` en vez de `repository.getArticle` directo
  - Updated `ArticlesListViewModel` — eliminado `selectedArticleId` / `onArticleSelected` (solo se usaba en tablet)
  - Deleted `ui/RepositoryEntryPoint` — ya no necesario
  - Updated `ui/ResponsiveApp` — eliminado `EntryPointAccessors`
  - Updated [[architecture/app-structure]] — removed `RepositoryEntryPoint`, added `usecase/` and `ArticleDetailPaneViewModel`
  - Updated [[architecture/di-hierarchy]] — removed `RepositoryEntryPoint`
  - Updated [[tools/hilt-setup]] — removed EntryPoint section
  - Eliminados todos los `@Suppress("ViewModelInjection")` del proyecto (2)

- Refactor DualPaneScreen + introducción de `GetArticleUseCase`
  - Created `data/usecase/GetArticleUseCase` — pure domain use case: timeout + fetch article by ID
  - Created `ui/articles/detail/ArticleDetailPaneViewModel` — ViewModel para tablet detail pane (dynamic articleId)
  - Updated `DualPaneScreen` — eliminado `repository` param, usa `ArticlesListViewModel` + `ArticleDetailPaneViewModel` como default params, estado de selección local
  - Updated `ArticleDetailViewModel` — ahora usa `GetArticleUseCase` en vez de `repository.getArticle` directo
  - Updated `ArticlesListViewModel` — eliminado `selectedArticleId` / `onArticleSelected` (solo se usaba en tablet)
  - Deleted `ui/RepositoryEntryPoint` — ya no necesario
  - Updated `ui/ResponsiveApp` — eliminado `EntryPointAccessors`
  - Updated [[architecture/app-structure]] — removed `RepositoryEntryPoint`, added `usecase/` and `ArticleDetailPaneViewModel`
  - Updated [[architecture/di-hierarchy]] — removed `RepositoryEntryPoint`
  - Updated [[tools/hilt-setup]] — removed EntryPoint section
  - Eliminados todos los `@Suppress("ViewModelInjection")` del proyecto

- Migración de `Response<T>` + `extractBody()` a `CallAdapter.Factory` automático
  - Updated [[tools/retrofit-setup]] — Apis devuelven dominio directo, `HttpErrorCallAdapterFactory` activado
  - Updated [[architecture/data-layer]] — `ResponseExt.kt` eliminado, APIs sin `Response<T>`
  - Updated [[patterns/error-handling]] — Nuevo flujo CallAdapter → ApiException → Result
  - Updated [[architecture/app-structure]] — `ResponseExt.kt` removido, `NetworkModule` usa `CallAdapterFactory`
  - Deleted `ResponseExt.kt` — reemplazado por `HttpErrorCallAdapter.kt` activo
- Updated `check-commit` skill — new step 4: wiki review antes del commit
- Updated `AGENTS.md` — reminder para revisar wiki después de cambios arquitectónicos / API / DI / error handling

## 2026-05-17 (2)

- Added [[tools/detekt-setup]] — Detekt static analysis, config, Compose rules, baseline, CI
- Updated [[processes/build-and-test]] — Added detekt commands, CI pipeline table
- Updated [[processes/build-and-test]] — Added detekt commands, CI pipeline table
- Updated [[index]] — Added detekt-setup link

## 2026-05-17

- Setup inicial del wiki
- Seed pages creadas para architecture, processes, patterns, tools
- Added [[patterns/attributes-actions]] — Attributes/Actions pattern + previews + analytics en Actions