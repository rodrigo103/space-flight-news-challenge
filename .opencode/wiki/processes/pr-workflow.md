# PR Workflow

> **Last verified:** 2026-05-17 | **Verified by:** [analysis]

## Branch naming

`<tipo>/<descripcion-corta>` — ej: `feat/search-articles`, `fix/crash-on-empty-list`

## Commit format

```
<tipo>: <descripción>

Opcional: cuerpo explicativo
```

Tipos: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`.

## PR template

```markdown
## Descripción
_Breve resumen del cambio_

## Cambios
- _Lista de cambios principales_

## Testing
- [ ] Unit tests
- [ ] Manual testing

## Screenshots
_Si aplica_
```

## Review

- PR mínimo de 1 approve
- Review checklist:
  - Compila en CI
  - Tests pasan
  - Sin magic numbers
  - Null safety chequeada
  - Composición sin recomposiciones innecesarias
  - Manejo de errores consistente

## Merge

- Squash merge
- Mensaje de commit consolidado = título del PR
- Eliminar branch post-merge