package com.example.myandroidapp.data.remote

/**
 * Resultado de una operación de datos.
 *
 * Reemplaza a `kotlin.Result` en la API pública de `:shared` porque `Result` es una value class
 * y **no se exporta al framework de Objective-C**: el tipo de retorno se borra a `id` (`Any?` en
 * Swift), perdiendo incluso el tipo del dato exitoso. Y compila sin warnings, así que el problema
 * recién aparece al escribir el Swift.
 *
 * Al ser una sealed interface, SKIE la convierte en un enum exhaustivo de Swift.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: Throwable) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
): R = when (this) {
    is ApiResult.Success -> onSuccess(data)
    is ApiResult.Failure -> onFailure(error)
}

fun <T> ApiResult<T>.getOrNull(): T? = when (this) {
    is ApiResult.Success -> data
    is ApiResult.Failure -> null
}

internal inline fun <T> apiRunCatching(block: () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
        throw e
    } catch (e: Throwable) {
        ApiResult.Failure(e)
    }
