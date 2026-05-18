package com.example.myandroidapp.data

import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import java.io.IOException

fun <T> retrofit2.Response<T>.extractBody(): T {
    if (isSuccessful) return body()!!
    val errorBody = errorBody()?.takeIf { it.contentLength() > 0 }
    val message = buildErrorMessage(errorBody)
    throw when (code()) {
        BAD_REQUEST -> ApiException.BadRequest(message)
        UNAUTHORIZED -> ApiException.Unauthorized(message)
        NOT_FOUND -> ApiException.NotFound(message)
        CONFLICT -> ApiException.Conflict(message)
        in SERVER_ERROR_MIN..SERVER_ERROR_MAX -> ApiException.ServerError(code(), message)
        else -> IOException(message)
    }
}

private fun buildErrorMessage(errorBody: ResponseBody?): String {
    val details = errorBody?.let {
        try { it.string() } catch (e: CancellationException) { throw e } catch (_: IOException) { null }
    } ?: ""
    return details.ifBlank { "Unknown error" }
}

private const val BAD_REQUEST = 400
private const val UNAUTHORIZED = 401
private const val NOT_FOUND = 404
private const val CONFLICT = 409
private const val SERVER_ERROR_MIN = 500
private const val SERVER_ERROR_MAX = 599
