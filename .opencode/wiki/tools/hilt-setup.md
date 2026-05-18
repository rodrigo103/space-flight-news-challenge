# Hilt Setup

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Entries

- `MyApplication` → `@HiltAndroidApp`
- `MainActivity` → `@AndroidEntryPoint`

## Módulos de DI

| Módulo | Bindings clave |
|---|---|
| `AppModule` | `Context`, `DataStore<Preferences>` |
| `NetworkModule` | `OkHttpClient`, `Retrofit`, `ApiService` |
| `DatabaseModule` | `AppDatabase`, `ArticleDao` |
| `RepositoryModule` | `ArticlesRepository` |
| `DataStoreModule` | `AppPreferences` |
| `DispatcherModule` | `CoroutineDispatchers` |
| `AnalyticsModule` | `AnalyticsHelper` → `TimberAnalyticsHelper` |

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

## Entry Point para acceso sin ViewModel

`RepositoryEntryPoint` en `ui/` package es un `@HiltEntryPoint` que permite acceder a `ArticlesRepository` desde contexts donde no hay inyección automática (ej: `ArticleDetailScreen` usa `hiltViewModel` en el Route, pero hay un entry point como fallback).