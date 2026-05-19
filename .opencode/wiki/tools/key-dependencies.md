---
tags:
  - wiki/tool
---

# Key Dependencies

> **Last verified:** 2026-05-19 | **Verified by:** [source] — removed DataStore (not used)

Resumen de las dependencias principales del proyecto. Versionadas via version catalog (`gradle/libs.versions.toml`).

| Librería | Versión | Propósito |
|---|---|---|
| Compose BOM | 2024+ | Bill of Materials para Compose |
| Compose Material 3 | vía BOM | Material Design 3 |
| Compose Navigation | vía BOM | Navegación entre screens |
| Hilt | 2.51+ | Dependency Injection |
| Room | 2.6+ | Base de datos local |
| Retrofit | 2.9+ | HTTP client |
| OkHttp | 4.12+ | HTTP transport + logging |
| kotlinx.serialization | 1.6+ | JSON serialization |
| Coil | 2.6+ | Image loading (Compose) |
| Paging 3 | 3.3+ | Paginación |

| Lottie | 6+ | Animaciones |
| LeakCanary | 2.13+ | Memory leak detection (debug) |
| Timber | 5.0+ | Logging |

## Plugins Gradle

| Plugin | Propósito |
|---|---|
| `com.android.application` | Android app module |
| `org.jetbrains.kotlin.android` | Kotlin Android |
| `org.jetbrains.kotlin.plugin.compose` | Compose compiler |
| `org.jetbrains.kotlin.plugin.serialization` | kotlinx.serialization |
| `com.google.dagger.hilt.android` | Hilt DI |
| `com.google.devtools.ksp` | KSP para Room + Hilt |

## Decisiones técnicas

### Coil vs Glide

| Aspecto | Coil | Glide |
|---|---|---|
| Soporte Compose nativo | **Sí** (coil-compose) | Requiere adapters |
| Coroutines | **Nativo** | Adicional |
| OkHttp | **Comparte el cliente** | Propio |
| APK size | **~150KB** | ~500KB |
| Kotlin-first | **Sí** | Java |

**Decisión:** Coil 3 por soporte nativo a Compose, coroutines, y reuso del OkHttpClient del proyecto.

### kotlinx.serialization vs Gson

| Aspecto | kotlinx.serialization | Gson |
|---|---|---|
| Reflection | **No** (compile-time) | Sí (runtime) |
| Rendimiento | **Más rápido** | Más lento |
| Type safety | **Sí** | No (runtime errors) |
| Kotlin multiplatform | **Sí** | No |
| Navigation Compose routes | **Compatible** | No |

**Decisión:** kotlinx.serialization por ser nativo de Kotlin, más rápido, más seguro, y compatible con Navigation Compose type-safe routes.

### StateFlow vs LiveData

| Aspecto | StateFlow | LiveData |
|---|---|---|
| Reactivo | **Sí** | Sí |
| Soporte coroutines | **Nativo** | No |
| Lifecycle-aware | **Sí** (collectAsStateWithLifecycle) | Sí |
| Diseñado para | **Compose** | Views/Fragments |
| Operadores Flow | **Sí** (map, combine, flatMapLatest) | No |

**Decisión:** StateFlow con `collectAsStateWithLifecycle()` por mejor integración con Compose y coroutines.

### TOML Version Catalog vs buildSrc

| Aspecto | TOML | buildSrc |
|---|---|---|
| IDE support | Autocompletado parcial | Autocompletado + go-to-definition |
| Error detection | En runtime | En compilación |
| Velocidad de sync | Instantáneo | buildSrc compila primero (+3s) |
| Custom logic | No soporta | Funciones helper |
| Recomendación oficial | **Sí** (Gradle 7.0+) | Anterior estándar |

**Decisión:** TOML version catalog por ser la recomendación oficial de Gradle. buildSrc tiene ventajas como type-safety, pero para un proyecto de 1 módulo con ~20 dependencias, TOML es más simple y rápido.

## Ver también

- [[tools/testing-strategy]] — Testing dependencies
- [[tools/retrofit-setup]] — Red layer