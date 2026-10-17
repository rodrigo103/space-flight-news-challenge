---
tags:
  - wiki/process
---

# Build & Test

> **Last verified:** 2026-10-17 | **Verified by:** [source] — Kover 0.9.11 reemplaza a JaCoCo, umbral ratchet y jobs reales del CI

## Build

- `./gradlew assembleDebug` — build debug APK
- `./gradlew assembleRelease` — build release APK (con minification + shrinking, ver ProGuard/R8)
- Usar Gradle wrapper (`./gradlew`), no gradle global

## Static Analysis

- `./gradlew detekt` — Run Detekt static analysis [source]
- `./gradlew detektBaseline` — Regenerar baseline para suprimir issues existentes
- `./gradlew detekt --auto-correct` — Auto-corregir issues corregibles
- Config en `config/detekt/detekt.yml`, baseline en `app/detekt-baseline.xml`
- Compose rules via `io.nlopez.compose.rules:detekt:0.4.2`

## Test

- `./gradlew test` — unit tests de `:app` (debug + release). ⚠️ **No** corre los de `:shared`
- `./gradlew :app:testDebugUnitTest` — tests de ViewModel de `:app` (17 tests)
- `./gradlew :shared:testAndroidHostTest` — `commonTest` de `:shared` en JVM (42 tests)
- `./gradlew :shared:allTests` — ⚠️ requiere runtime de simulador iOS (`xcodebuild -downloadPlatform iOS`)
- `./gradlew connectedAndroidTest` — Instrumentados (Compose UI test + Espresso)

`main` usa JUnit 4 + MockK + kotlinx.coroutines.test en `:app`, y `kotlin.test` + Ktor MockEngine + fakes a mano en `:shared`. Detalle de patrones en [[tools/testing-strategy]].

## Cobertura (Kover)

- **Kover 0.9.11** (`org.jetbrains.kotlinx.kover`) aplicado en `:app` y `:shared`. Reemplazó a JaCoCo: no quedan ni `enableUnitTestCoverage` ni el task `jacocoTestReport` [source]
- El reporte **total** (el que consume SonarCloud y gatea el CI) mide solo la **capa lógica**: packages `com.example.myandroidapp.data` + `com.example.myandroidapp.domain` y clases `*ViewModel*`; excluye generado (`*.R`, `*.BuildConfig`, `*_Impl*`, `*Preview*`, `ComposableSingletons*`) y el package `di` [source]
- **Umbral ratchet** en `app/build.gradle.kts`: `minBound(65)` (la cobertura lógica actual ronda el 70%). Subir, nunca bajar [source]
- `:app` agrega la cobertura de `:shared` vía `kover(project(":shared"))` [source]

| Comando | Qué hace |
|---|---|
| `./gradlew :app:koverVerify` | Aplica el umbral — rompe el build si la cobertura lógica baja de 65 |
| `./gradlew :app:koverXmlReport` | XML JaCoCo-compatible (`app/build/reports/kover/report.xml`) para SonarCloud |
| `./gradlew :app:koverHtmlReport` | HTML del reporte "total" (solo lógica: data + domain + ViewModels) |
| `./gradlew :app:koverHtmlReportDebug` | HTML de `:app` completa (incluye UI Compose) |

## CI Pipeline

El workflow en `.github/workflows/ci.yml` corre 5 jobs en paralelo en cada PR y push a `main` [source]:

| Job | Comando | Propósito |
|---|---|---|
| `detekt` | `./gradlew detekt` | Static analysis |
| `unit-tests` | `./gradlew test :app:koverVerify` | Tests unitarios + umbral de cobertura |
| `build-debug` | `./gradlew assembleDebug` | Build de debug |
| `build-release` | `./gradlew assembleRelease` | Build de release |
| `instrumented-tests` | `./gradlew connectedDebugAndroidTest` (emulador API 34, x86_64) | Tests instrumentados |

El CI usa Ubuntu latest + JDK 17 (Temurin) + Gradle setup action v4.

## SonarCloud

Workflow en `.github/workflows/sonar.yml` que corre en cada PR y push a `main`:

- Corre `./gradlew :app:koverXmlReport sonar` — análisis estático + cobertura
- La property `sonar.coverage.jacoco.xmlReportPaths` en `app/build.gradle.kts` apunta a `app/build/reports/kover/report.xml`: Kover genera XML JaCoCo-compatible, por eso la property conserva el nombre `jacoco` [source]
- Usa `SONAR_TOKEN` de secrets para autenticación
- ⚠️ El token de SonarCloud vive ~90 días: si el job falla con 403 *"scanner bootstrapping failed"*, regenerar el secret [analysis]
- Cachea paquetes de SonarCloud (`~/.sonar/cache`) para acelerar ejecuciones
- `fetch-depth: 0` necesario para blame analysis en Sonar

## Dependencias de test

| Dependencia | Módulo | Uso |
|---|---|---|
| JUnit 4 | `:app` | Test runner |
| MockK | `:app` | Mocking Kotlin (solo JVM) |
| `kotlin.test` | `:shared` | Asserts multiplataforma |
| Ktor MockEngine | `:shared` | HTTP mocking |
| kotlinx.coroutines.test | ambos | Coroutine testing |
| Turbine | ambos | Flow testing (declarado, hoy sin usos) |
| Compose UI Test | `:app/androidTest` | Instrumentados |

`mockwebserver` sigue declarado en `:app/build.gradle.kts` pero sin usos: los tests de HTTP viven en `:shared` (ver [[tools/testing-strategy]]). [source]

## ProGuard / R8

Habilitado en el build type `release` [source]:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

- `isMinifyEnabled` — R8 hace shrinking, ofuscación y optimización del bytecode
- `isShrinkResources` — elimina recursos no referenciados (requiere `isMinifyEnabled = true`)
- Se usa `proguard-android-optimize.txt` (no el default `proguard-android.txt`), que habilita las optimizaciones de R8
- Reglas propias en `app/proguard-rules.pro` (73 líneas)
- El build type `debug` ya **no** configura `enableUnitTestCoverage` (era JaCoCo): la cobertura la maneja Kover sobre los unit tests [source]

Ver [[patterns/secure-coding]] para por qué cada librería con reflection o code generation necesita su keep rule.

## Gradle config clave

- Java 17, Kotlin 17 toolchain
- Compose compiler via `libs.plugins.compose.compiler`
- KSP solo en `:shared` (Room compiler, por target KMP)
- Cobertura con Kover 0.9.11 en `:app` y `:shared`

## CI avanzado (a futuro)

Patrones observados en proyectos de producción:

| Práctica | Descripción |
|---|---|
| **Release train automático** | Workflow schedule que incrementa versión, genera changelog, crea branch y PR |
| **MobSF security scanning** | Escanea el APK en busca de vulnerabilidades mobile |
| **Dependency diff** | Script bash que compara árbol de dependencias antes/después de cambios |
| **Auto-labeler** | Labels automáticos por equipo/área |
| **Gradle Managed Devices** | Dispositivos preconfigurados para consistencia local/CI |

## Ver también

- [[tools/testing-strategy]] — Testing patterns y cobertura
- [[tools/detekt-setup]] — Detekt lint
