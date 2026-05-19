# Changelog del Wiki

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
- Updated [[processes/pr-workflow]] — Added PR template, CODEOWNERS, CI checks section
- Updated [[index]] — Added detekt-setup link

## 2026-05-17

- Setup inicial del wiki
- Seed pages creadas para architecture, processes, patterns, tools
- Added [[patterns/attributes-actions]] — Attributes/Actions pattern + previews + analytics en Actions