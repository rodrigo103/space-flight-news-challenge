---
tags:
  - wiki/tool
---

# Detekt Setup

> **Last verified:** 2026-05-19 | **Verified by:** [source]

## Plugin Gradle

```kotlin
// libs.versions.toml
detekt = "1.23.8"
detektComposeRules = "0.4.2"

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.detekt)
}

detekt {
  config.setFrom(files("../config/detekt/detekt.yml"))
  baseline = file("detekt-baseline.xml")
  buildUponDefaultConfig = true
  allRules = false
}
```

## Config file

`config/detekt/detekt.yml` — custom ruleset, ~410 lines con reglas para:

| Categoría | Reglas clave |
|---|---|
| `complexity` | CognitiveComplexMethod (15), LongMethod (60), TooManyFunctions, LargeClass (600) |
| `coroutines` | InjectDispatcher, RedundantSuspendModifier, SleepInsteadOfDelay |
| `empty-blocks` | Todas las empty block checks |
| `exceptions` | PrintStackTrace, SwallowedException, TooGenericExceptionCaught/Thrown |
| `naming` | PackageNaming, ClassNaming, FunctionNaming, VariableNaming |
| `performance` | ArrayPrimitive, CouldBeSequence, UnnecessaryTemporaryInstantiation |
| `potential-bugs` | UnsafeCallOnNullableType, UnsafeCast, HasPlatformType, IgnoredReturnValue |
| `style` | MagicNumber, MaxLineLength (120), ReturnCount (3), UnusedImports, VarCouldBeVal, WildcardImport |
| `Compose` | ModifierMissing, ModifierReused, ComposableNaming, ViewModelInjection, RememberMissing, PreviewPublic |

## Compose rules

Usa `io.nlopez.compose.rules:detekt:0.4.2` (sucesor de `com.twitter.compose.rules`). Ruleset ID: `Compose`.

```kotlin
detektPlugins("io.nlopez.compose.rules:detekt:${libs.versions.detektComposeRules.get()}")
```

Reglas de Compose activas:
- `MutableParameters`, `ViewModelForwarding`, `ViewModelInjection`
- `ModifierMissing`, `ModifierReused`, `ModifierWithoutDefault`, `ModifierComposable`
- `ComposableNaming`, `PreviewNaming`, `PreviewPublic`
- `RememberMissing`, `CompositionLocalNaming`
- `UnstableCollections`, `MultipleContentEmitters`

## Baseline

`app/detekt-baseline.xml` suprime ~35 issues existentes para que solo se reporten issues nuevos en CI. Generado con:

```bash
./gradlew detektBaseline
```

Baseline se usa en conjunto con `maxIssues: 50` para evitar que el build falle por issues existentes.

## CI integration

Workflow en `.github/workflows/ci.yml` — job `detekt` corre en cada PR a `main`:

```yaml
- name: Run Detekt
  run: ./gradlew detekt
```

## Comandos

```bash
./gradlew detekt           # Run analysis
./gradlew detektBaseline   # Regenerar baseline
./gradlew detekt --auto-correct  # Auto-corregir issues corregibles
```

## Ver también

- [[processes/build-and-test]] — Comandos de build y test, CI pipeline
- [[tools/key-dependencies]] — Versiones de dependencias
