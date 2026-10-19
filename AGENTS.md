# Proyecto Android — Space Flight News App

App Android con Jetpack Compose, MVVM + Repository pattern, Hilt DI, Room, Retrofit, Navigation Compose, Paging 3.

## Ramas del repo

El repo mantiene dos líneas vivas en paralelo:

| Rama | Rol | Base |
|---|---|---|
| `main` | Troncal KMP: `:app` (Android) + `:shared` (KMP, Koin) | — |
| `feat/ios-app` | WIP app iOS (SKIE + `iosApp/`) sobre `main` | `main` |
| `android-native` | Línea Android nativo puro: `:app` + `:domain`, Hilt, sin KMP | tag `pre-kmp` (aa5080a) |

Reglas:

- **Nunca merge entre líneas** (divergen en módulos y DI): los cambios compartidos se portan con `git cherry-pick -x <sha>`, adaptando si tocan data layer o DI.
- **Desarrollo dual:** toda feature nueva se implementa en ambas líneas; en el mensaje de commit referenciá el espejo (`espejo de android-native@abc1234`).
- Para que los portes apliquen limpios, mantené los mismos paths en la capa UI (`app/src/main/java/.../ui/...` coincide en ambas líneas).

## Build & Test

- `./gradlew assembleDebug` — build de debug
- `./gradlew test` — tests unitarios
- `./gradlew connectedAndroidTest` — tests instrumentados
- Usar `./gradlew` wrapper, no gradle global.

## Arquitectura

- MVVM con `ViewModel` + `StateFlow<UiState>`.
- `UiState` es una `sealed interface` con `Loading`, `Success<T>`, `Error`.
- Repository devuelve valores y **lanza excepciones** en error (sin `Result` ni `ApiResult`); las VMs mapean a `UiState.Error` con try/catch.
- Hilt DI con módulos por capa: `AppModule`, `NetworkModule`, `DatabaseModule`, `RepositoryModule`, etc.
- Paging 3 con `RemoteMediator` para paginación desde API + Room.

## Convenciones

- Kotlin con `jvmToolchain(17)`.
- Serialización con `kotlinx.serialization`, no Gson.
- Room + coroutines, sin RxJava.
- Analitycs via `AnalyticsHelper` interfaz + `TimberAnalyticsHelper` impl.

---

# LLM Wiki

El proyecto tiene un wiki persistente en `.opencode/wiki/` que opencode mantiene incrementalmente.

## Cuándo leer el wiki — ¡OBLIGATORIO!

**Siempre, sin excepción, leé la wiki ANTES de responder cualquier consulta.**
Esto aplica incluso para tareas de código — consultá la página relevante antes de editar.

Categorías cubiertas:
- Arquitectura del proyecto (módulos, DI, capas)
- Procesos (build, test, PR workflow)
- Patrones (MVVM, error handling, Room + Paging)
- Herramientas (Retrofit, Hilt, Coil)

Hacer `/wiki search <tema>` o leer directamente en `.opencode/wiki/`.

## Cuándo escribir al wiki

Después de resolver un problema no trivial, compilar conocimiento al wiki si se trata de:
- Un proceso completo (build, deploy, release)
- Arquitectura o estructura del proyecto
- Un patrón reusable o gotcha descubierto
- Configuración de herramientas

**IMPORTANTE**: Después de cambios significativos (arquitectura, API contracts, DI, error handling, nuevas dependencias), **revisá si las páginas del wiki necesitan actualizarse**. Buscá archivos mencionados en el wiki (`grep` de nombres de archivo en `.opencode/wiki/`). Si encontrás referencias a código que cambió o se eliminó, actualizá la página y agregá entrada en `log.md`.

El skill `check-commit` incluye un paso de wiki review antes del commit.

Hacer `/wiki ingest` para procesar archivos en `raw/` o `/wiki ingest <topic>` para compilar conocimiento desde el codebase.

## Formato de páginas

Toda página debe tener:

```markdown
# Título

> **Last verified:** YYYY-MM-DD | **Verified by:**

Contenido con claim types...
```

### Claim types

| Tag | Uso |
|-----|-----|
| `[source]` | Verificado del código o docs |
| `[analysis]` | Conclusión de evidencia |
| `[unverified]` | Suposición sin chequear |
| `[gap]` | Desconocido conocido |

### Cross-links

Usar `[[ruta/sin-extension]]` para enlazar páginas.

### Lint

`/wiki lint` chequea: links rotos, páginas huérfanas, info stale (>30 días), `[unverified]` abundantes.

## Estructura del wiki

```
.opencode/wiki/
├── raw/              # Inbox — drop files aquí
├── index.md          # TOC auto-mantenido
├── log.md            # Changelog cronológico
├── architecture/     # Estructura del app, DI, data layer
├── processes/        # Build, test, PR workflow
├── patterns/         # MVVM, error handling, Room+Paging
└── tools/            # Retrofit, Hilt, dependencias clave
```