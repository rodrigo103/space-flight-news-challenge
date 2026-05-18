# Build & Test

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Build

- `./gradlew assembleDebug` — build debug APK
- `./gradlew assembleRelease` — build release APK (minification disabled por ahora)
- Usar Gradle wrapper (`./gradlew`), no gradle global

## Test

- `./gradlew test` — Unit tests (JUnit 5 + MockK + Turbine + kotlinx.coroutines.test)
- `./gradlew connectedAndroidTest` — Instrumenteded tests (Compose UI test + Espresso)

## Dependencias de test

| Dependencia | Uso |
|---|---|
| JUnit 5 | Test runner |
| MockK | Mocking Kotlin |
| kotlinx.coroutines.test | Coroutine testing |
| Turbine | Flow testing |
| MockWebServer | HTTP mocking |
| Compose UI Test | Screenshot/compose testing |

## ProGuard / R8

Minification deshabilitado en release por ahora. ProGuard rules file: `app/proguard-rules.pro`.

## Gradle config clave

- Java 17, Kotlin 17 toolchain
- Compose compiler via `libs.plugins.compose.compiler`
- KSP para Room + Hilt compilers