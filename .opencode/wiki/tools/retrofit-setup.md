---
tags:
  - wiki/tool
---
## Config

- Base URL: `https://api.spaceflightnewsapi.net/v4`
- Converter: `kotlinx.serialization` (vía `Json { ignoreUnknownKeys = true }`)
- `OkHttpClient` con logging interceptor en debug
- `HttpErrorCallAdapterFactory` — custom `CallAdapter.Factory` que intercepta respuestas HTTP ≠ 2xx y las convierte en excepciones tipadas antes de que lleguen al caller [source]

## ApiService

```kotlin
interface ApiService {
    @GET("articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
    ): ArticleResponse

    @GET("articles/{id}/")
    suspend fun getArticle(@Path("id") id: Int): Article
}
```

Los endpoints devuelven dominio directo. No se usa `Response<T>` — el `CallAdapter` maneja errores automáticamente.

## Estructura de la API

La Spaceflight News API es un **agregador** de noticias, no hostea contenido. Ambos endpoints (`GET /articles/` y `GET /articles/{id}/`) devuelven el **mismo schema de artículo**. No existe campo `body` ni `content` — solo `summary` (resumen corto) y `url` (link al artículo original). [source]

## HTTP Error handling

`HttpErrorCallAdapterFactory` es un `CallAdapter.Factory` registrado en `NetworkModule`. Intercepta `Call.enqueue()`: si la response es 2xx la deja pasar, si no, parsea el error y tira una `ApiException` tipada:

| HTTP status | Exception |
|---|---|
| 400 | `ApiException.BadRequest` |
| 401 | `ApiException.Unauthorized` |
| 404 | `ApiException.NotFound` |
| 409 | `ApiException.Conflict` |
| 500..599 | `ApiException.ServerError(code, message)` |
| Otro | `IOException` |

El error se propaga al Repository, que lo captura con `Result.runCatching {}`.

## Dependencias

- `com.squareup.retrofit2:retrofit`
- `com.squareup.retrofit2:converter-kotlinx-serialization`
- `com.squareup.okhttp3:okhttp`
- `com.squareup.okhttp3:logging-interceptor`

## Module DI

`NetworkModule` (Singleton):
1. Provee `OkHttpClient` con logging interceptor
2. Provee `Retrofit` con base URL + kotlinx serialization converter + `HttpErrorCallAdapterFactory()`
3. Provee `ApiService` implementado por Retrofit

## Logging interceptor condicional

```kotlin
@Provides @Singleton
fun provideOkHttpClient(@Named("isDebug") isDebug: Boolean): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
        level = if (isDebug) BODY else BASIC
    }
    return OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("origin", "android")
                .build()
            chain.proceed(request)
        }
        .build()
}
```

En debug se loguean bodies completos. En release solo headers para no exponer datos sensibles.

## Dual CallAdapter strategy

Para soportar tanto coroutines como RxJava2:

```kotlin
val retrofit = Retrofit.Builder()
    .addCallAdapterFactory(HttpErrorCallAdapterFactory())
    .addConverterFactory(kotlinxSerializationConverter(json))
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .build()
```

## Ver también

- [[patterns/error-handling]] — Error handling + ApiException hierarchy
- [[tools/testing-strategy]] — Testing con MockWebServer
