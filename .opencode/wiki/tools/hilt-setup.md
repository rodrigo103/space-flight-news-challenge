---
tags:
  - wiki/tool
---

# Hilt Setup

> **Last verified:** 2026-08-05 | **Verified by:** [source] — módulos recableados contra `:shared`

> **Nota:** las clases de `:shared` ya no llevan `@Inject constructor` (`javax.inject` no compila
> para Native), así que `NetworkModule`, `DatabaseModule` y `RepositoryModule` las construyen con
> `@Provides` explícitos en vez de `@Binds`. Es temporal: la Fase 4 de [[patterns/kmp-migration]]
> reemplaza Hilt por Koin.

## Entries

- `MyApplication` → `@HiltAndroidApp`
- `MainActivity` → `@AndroidEntryPoint`

## Módulos de DI

| Módulo | Bindings clave |
|---|---|
| `AppModule` | `isDebug` named binding |
| `NetworkModule` | `OkHttpClient`, `Retrofit`, `ApiService` |
| `DatabaseModule` | `AppDatabase`, `ArticleDao` |
| `RepositoryModule` | `ArticlesRepository` |
| `DispatcherModule` | `CoroutineDispatchers` |
| `AnalyticsModule` | `AnalyticsHelper` → `CompositeAnalyticsHelper(listOf(firebase, timber))` |

## Plugins Gradle

```
id("com.google.dagger.hilt.android")
id("com.google.devtools.ksp")
```

## Dependencias

```
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)
```