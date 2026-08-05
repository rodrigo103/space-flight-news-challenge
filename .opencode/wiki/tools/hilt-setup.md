---
tags:
  - wiki/tool
---

# Hilt Setup

> ⚠️ **OBSOLETA desde 2026-08-05.** Hilt fue reemplazado por **Koin** en la Fase 4 de
> [[patterns/kmp-migration]], porque Hilt es Dagger sobre Android y no compila para Kotlin/Native.
> Esta página se conserva como registro histórico. Para la DI actual ver [[tools/koin-setup]],
> que incluye una tabla de equivalencias Hilt → Koin.

> **Last verified:** 2026-08-05 | **Verified by:** [source] — configuración histórica, ya no en uso

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