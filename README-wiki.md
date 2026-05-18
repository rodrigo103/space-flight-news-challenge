# LLM Wiki — Guía de uso

Este proyecto implementa un **LLM Wiki** (basado en el concepto de [Andrej Karpathy](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f)) para opencode. Es un sistema de conocimiento persistente que opencode consulta y alimenta automáticamente mientras trabaja.

## ¿Qué es?

Un wiki de markdown interconectado en `.opencode/wiki/` que opencode:

- **Lee** antes de responder preguntas sobre el proyecto (arquitectura, procesos, patrones)
- **Escribe** automáticamente cuando descubre conocimiento útil (patrones, gotchas, configuraciones)
- **Mantiene** actualizado verificando fechas y marcando info desactualizada

## ¿Qué problemas resuelve?

- **No empezar de cero cada vez** — opencode recuerda decisiones de arquitectura, patrones del proyecto, procesos CI/CD, gotchas conocidos
- **Documentación que crece sola** — sin tener que sentarse a escribir docs
- **Conocimiento compartido** — el wiki se commitea y todo el equipo lo consulta

## Estructura del wiki

```
.opencode/wiki/
├── raw/                  # Inbox — drop archivos aquí para que opencode los procese
├── index.md              # TOC — opencode lo mantiene automáticamente
├── log.md                # Changelog — registro cronológico de cambios
├── architecture/         # Estructura del app, módulos, DI, data layer
├── processes/            # Build, test, CI/CD, PR workflow
├── patterns/             # MVVM, error handling, Room + Paging
└── tools/                # Retrofit, Hilt, dependencias
```

## Cómo usarlo

### Automáticamente (recomendado)

El wiki funciona solo. Cada vez que opencode resuelve un problema o descubre algo útil, el **plugin nudge** le recuerda (1 de cada 3 sesiones) que evalúe si merece una página wiki. No requiere acción manual.

### Comandos manuales

Usá `/wiki` en opencode para:

| Comando | Qué hace |
|---|---|
| `/wiki ingest` | Procesa archivos en `raw/` y compila páginas wiki |
| `/wiki ingest <tema>` | Busca info del tema en el código y la compila al wiki |
| `/wiki search <query>` | Busca en todas las páginas y sintetiza respuesta |
| `/wiki lint` | Chequea links rotos, páginas huérfanas, info desactualizada |
| `/wiki status` | Muestra estadísticas del wiki |

### Ingest manual

Si encontraste un artículo, documentación externa o notas que quieras incorporar:

1. Copiá el archivo a `.opencode/wiki/raw/`
2. Ejecutá `/wiki ingest`
3. opencode lo procesa, extrae el conocimiento y lo compila en páginas wiki
4. Los archivos en `raw/` se borran automáticamente después de procesarlos

## Claim types

Cada afirmación en el wiki tiene una etiqueta de confianza:

| Tag | Significado | Confianza |
|---|---|---|
| `[source]` | Verificado del código o documentación | Alta |
| `[analysis]` | Conclusión basada en evidencia | Media |
| `[unverified]` | Asumido sin verificar | Baja (requiere revisión) |
| `[gap]` | Desconocido conocido | N/A (marca qué investigar) |

## Para desarrolladores

### Cómo contribuir al wiki

El wiki es parte del repo. Si querés ajustar o agregar conocimiento:

1. Editá o creá archivos `.md` en `.opencode/wiki/`
2. Usá `[[ruta/sin-extension]]` para cross-links
3. Incluí `> **Last verified:** YYYY-MM-DD | **Verified by:**` en cada página
4. Commiteá los cambios

### Cómo funciona el sistema

```
trabajo diario
    ↓
opencode resuelve un problema
    ↓
[plugin wiki-nudge] ¿Esto merece una página wiki?
    ├── Sí → escribe/actualiza .opencode/wiki/
    └── No  → sigue
    ↓
próxima vez que preguntes algo similar
    ↓
opencode consulta el wiki → respuesta más rápida y precisa
```

## Seed inicial

El wiki ya tiene contenido semilla generado del código actual:

| Categoría | Páginas |
|---|---|
| Architecture | app-structure, di-hierarchy, data-layer |
| Processes | build-and-test, pr-workflow |
| Patterns | mvvm-repository, error-handling, room-paging |
| Tools | retrofit-setup, hilt-setup, key-dependencies |

Este seed fue generado manualmente. A partir de ahora, opencode lo mantiene y expande automáticamente.
