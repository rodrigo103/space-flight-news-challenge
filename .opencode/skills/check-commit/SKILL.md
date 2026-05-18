---
name: check-commit
description: "Run detekt + unit tests, then commit with a structured message. Usage: /check-commit"
user-invocable: true
allowed-tools: Bash, Read, Glob, Write, Edit
---

# Check Commit

Gatekeeper antes de commitear: corre detekt, luego tests, y si todo pasa genera el commit.

## Procedure

### Step 1 — Mostrar resumen de cambios

```bash
git diff --stat
git status --short
```

### Step 2 — Correr detekt

```bash
./gradlew detekt
```

- Si **falla** → mostrar los issues, preguntar si quiere arreglarlos o forzar el commit igual (`--no-verify`).
- Si pasa → continuar.

### Step 3 — Correr tests

```bash
./gradlew test
```

- Si **falla** → mostrar los tests fallidos, preguntar si quiere arreglarlos o forzar.
- Si pasa → continuar.

### Step 4 — Generar el commit

Analizar los cambios con `git diff` y preguntar:

1. **Tipo**: feat / fix / refactor / chore / docs / test / perf
2. **Descripción corta** (imperativo, inglés, < 72 chars)
3. **Cuerpo opcional** (bullet points de cambios)

### Step 5 — Ejecutar

```bash
git add <files>
git commit -m "<type>: <description>"
```

Si hay cuerpo:
```bash
git commit -m "<type>: <description>" -m "<body>"
```

Mostrar resultado del commit al final.
