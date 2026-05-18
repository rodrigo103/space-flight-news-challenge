# Key Dependencies

> **Last verified:** 2026-05-17 | **Verified by:** [source]

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
| DataStore | 1.1+ | Preferences |
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