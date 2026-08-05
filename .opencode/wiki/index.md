# Wiki del Proyecto

> **Last verified:** 2026-08-04 | **Verified by:** Wiki audit — contradicciones corregidas contra el código

Base de conocimiento persistente del proyecto, mantenida por opencode.

## Architecture

- [[architecture/app-structure]] — Estructura general del módulo `:app`
- [[architecture/di-hierarchy]] — Jerarquía de módulos de Hilt DI
- [[architecture/data-layer]] — Data layer: API, Room
- [[architecture/clean-architecture-guide]] — Clean Architecture: 3 capas, regla de dependencia, use cases
- [[architecture/responsive-layout]] — Layout adaptativo phone/tablet con DualPaneScreen

## Processes

- [[processes/build-and-test]] — Cómo compilar y testear
- [[processes/tech-defense-guide]] — Guía de defensa técnica para entrevistas

## Patterns

- [[patterns/attributes-actions]] — Attributes/Actions pattern + previews
- [[patterns/mvvm-repository]] — MVVM + Repository pattern
- [[patterns/mvvm-vs-mvi]] — Comparativa MVVM vs MVI en Android
- [[patterns/mvi-migration]] — Plan de migración MVVM → MVI (completo + Lite)
- [[patterns/kmp-migration]] — Plan de migración a Kotlin Multiplatform (lógica compartida + SwiftUI)
- [[patterns/error-handling]] — Manejo de errores con Result + UiState + CallAdapter
- [[patterns/room-paging]] — Room + Paging 3 + RemoteMediator
- [[patterns/search-strategy]] — Búsqueda local vs API: decisión arquitectónica
- [[patterns/secure-coding]] — Secure coding: AES/CBC, SHA-256, TLS, ofuscación, almacenamiento encriptado
- [[patterns/settings-composable]] — Settings + Composable pattern
- [[patterns/analytics-composite]] — Analytics con Composite Pattern (Firebase + Timber)

## Tools

- [[tools/retrofit-setup]] — ⚠️ obsoleta: Retrofit fue reemplazado por Ktor
- [[tools/detekt-setup]] — Detekt lint + Compose rules
- [[tools/koin-setup]] — Koin DI: módulos, equivalencias con Hilt, y el costo de perder validación en compilación
- [[tools/hilt-setup]] — ⚠️ obsoleta: Hilt fue reemplazado por Koin
- [[tools/key-dependencies]] — Resumen de dependencias clave + decisiones técnicas
- [[tools/testing-strategy]] — Testing patterns: MockK, MockWebServer, Turbine, Semantic Properties

## Meta

- [[log]] — Changelog del wiki