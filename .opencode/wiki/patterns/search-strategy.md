---
tags:
  - wiki/pattern
---

# Search Strategy

> **Last verified:** 2026-05-29 | **Verified by:** [analysis]

## Contexto

La API de Space Flight News soporta búsqueda textual via `?search=mars`. El proyecto tiene Room como caché con Paging 3 y `RemoteMediator`.

## El problema

Al implementar búsqueda contra la API nos enfrentamos a:

1. **RemoteMediator se recrea en cada búsqueda** — al usar `flatMapLatest` sobre el query, se crea un nuevo `Pager` con un nuevo `RemoteMediator` por cada cambio.
2. **CancellationException** — `flatMapLatest` cancela el flow anterior, lanzando `CancellationException`. Si no se relanza, se traduce en un snackbar de error.
3. **Errores transitorios** — combinación de timeouts, cancelaciones y errores de red que producen experiencias inconsistentes.

## Las dos opciones

### Opción A: Búsqueda contra la API

```
Usuario escribe → debounce 300ms → Pager con RemoteMediator + searchQuery → API ?search= → Room
```

**Pros:** Resultados completos, siempre fresco.
**Contras:** HTTP request por cada búsqueda, RemoteMediator se recrea, no funciona offline, experiencia de "carga" en cada búsqueda.

### Opción B: Búsqueda local contra Room

```
Usuario escribe → Pager SIN RemoteMediator → Room PagingSource con WHERE/LIKE → instantáneo
```

**Pros:** Sin HTTP requests, sin problemas de cancelación, funciona offline, instantáneo.
**Contras:** Solo busca entre artículos ya cacheados en Room.

## Decisión: Stale-While-Revalidate (híbrido A + B)

**Siempre se usa RemoteMediator con `searchQuery`. Room emite resultados stale instantáneos mientras la API refresca en paralelo.**

### Arquitectura resultante

```
Normal (sin búsqueda):
  Pager(remoteMediator = ArticleRemoteMediator(searchQuery = null), pagingSource = articleDao.pagingSource())
    → RemoteMediator llama API sin ?search=, guarda en Room
    → PagingSource lee de Room

Búsqueda (con query):
  Pager(remoteMediator = ArticleRemoteMediator(searchQuery = "mars"), pagingSource = articleDao.searchPagingSource("mars"))
    → searchPagingSource emite resultados stale de Room instantáneamente
    → RemoteMediator llama API con ?search=mars en paralelo, guarda en Room
    → Room invalida PagingSource → re-emite datos frescos
```

### Flujo de datos

```
Usuario typea "mars"
  → _searchQuery.value = "mars"
  → debounce 300ms → flatMapLatest → repository.getArticlesPaged("mars")
  → Pager(remoteMediator = ArticleRemoteMediator(searchQuery = "mars"), pagingSource = dao.searchPagingSource("mars"))
  → 1️⃣ Room emite local: SELECT * FROM articles WHERE title LIKE '%mars%' OR summary LIKE '%mars%'
  → 2️⃣ RemoteMediator.load() → GET /articles?search=mars → insertAll() → Room invalida → re-emite
  → Flow<PagingData<Article>> → collectAsLazyPagingItems()
```

### Ventajas del enfoque

1. **Resultados instantáneos** — Room emite datos cacheados mientras la API carga
2. **Resultados completos** — la API trae artículos que no están en Room
3. **Funciona offline** — si la red falla, RemoteMediator devuelve `Success(false)` y el usuario se queda con los resultados de Room
4. **Sin problemas de cancelación** — `CancellationException` se relanza correctamente en RemoteMediator
5. **Stale-while-revalidate es nativo de Paging 3** — PagingSource emite stale, RemoteMediator refresca, Room notifica cambios automáticamente

### Archivos clave

| Archivo | Rol |
|---------|-----|
| `ArticleRemoteMediator.kt` | Constructor acepta `searchQuery: String?`, lo pasa a `apiService.getArticles(search = ...)` |
| `DefaultArticlesRepository.kt:53` | Siempre crea `ArticleRemoteMediator` con `searchQuery?.ifBlank { null }` |
| `ArticleDao.kt:21` | `searchPagingSource` busca en `title` y `summary` con `LIKE` |

## Ver también

- [[patterns/room-paging]] — Room + Paging 3 + RemoteMediator
- [[patterns/mvvm-repository]] — MVVM + Repository