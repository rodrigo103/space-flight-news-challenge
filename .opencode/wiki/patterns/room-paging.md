# Room + Paging 3 + RemoteMediator

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Room setup

- `AppDatabase` — Database singleton via Hilt
- `ArticleEntity` — Entidad con campos: `id` (PK), `title`, `authors`, `url`, `image_url`, `news_site`, `summary`, `published_at`
- `ArticleDao` — DAO con:
  - `getAllPaging()` → `PagingSource<Int, ArticleEntity>` — paginación simple
  - `search(searchQuery)` → `PagingSource<Int, ArticleEntity>` — búsqueda con `LIKE`
  - `insertAll(articles: List<ArticleEntity>)` — upsert batch para RemoteMediator
  - `clearAll()` — limpieza antes de recarga

## Paging 3

Usa `PagingSource` de Room + `Flow<PagingData<T>>` expuesto por Repository.

## RemoteMediator

`ArticleRemoteMediator` sincroniza API → Room:

1. Detecta si necesita cargar más datos (scroll al final)
2. Llama a `ApiService.getArticles(limit, offset)`
3. Mapea `Article` → `ArticleEntity`
4. Inserta en Room via `ArticleDao.insertAll()`
5. Room notifica al `PagingSource` que hay nuevos datos
6. `Flow<PagingData<T>>` emite nuevo estado a la UI

### Load type

- `LoadType.REFRESH` — Limpia DB y recarga desde página 0
- `LoadType.PREPEND` — No implementado (API no soporta backward pagination)
- `LoadType.APPEND` — Carga siguiente página usando `offset + limit`

## Flujo completo

```
Screen ← collectAsStateWithLifecycle() ← Flow<PagingData<Article>>
    ↑
ArticlesRepository.getArticles()
    ↑
ArticleRemoteMediator ← → ApiService.getArticles()
    ↑                        ↓
ArticleDao.getAllPaging() ← ArticleDao.insertAll()
    ↑
AppDatabase (Room)
```