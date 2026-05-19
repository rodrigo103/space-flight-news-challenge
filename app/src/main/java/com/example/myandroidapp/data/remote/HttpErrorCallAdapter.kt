package com.example.myandroidapp.data.remote

import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Suppress("MatchingDeclarationName")
class HttpErrorCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null
        val successType = getParameterUpperBound(0, returnType as ParameterizedType)
        return HttpErrorCallAdapter<Any>(successType)
    }
}

private class HttpErrorCallAdapter<T>(
    private val successType: Type,
) : CallAdapter<T, Call<T>> {

    override fun responseType(): Type = successType

    override fun adapt(call: Call<T>): Call<T> = HttpErrorCall(call)
}

private class HttpErrorCall<T>(
    private val delegate: Call<T>,
) : Call<T> by delegate {

    override fun enqueue(callback: Callback<T>) {
        delegate.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    callback.onResponse(this@HttpErrorCall, response)
                } else {
                    callback.onFailure(this@HttpErrorCall, response.toApiException())
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback.onFailure(this@HttpErrorCall, t)
            }
        })
    }

    override fun clone(): Call<T> = HttpErrorCall(delegate.clone())

    private fun Response<T>.toApiException(): Throwable {
        val errorBody = errorBody()?.takeIf { it.contentLength() > 0 }
        val message = buildErrorMessage(this, errorBody)
        return when (val code = code()) {
            BAD_REQUEST -> ApiException.BadRequest(message)
            UNAUTHORIZED -> ApiException.Unauthorized(message)
            NOT_FOUND -> ApiException.NotFound(message)
            CONFLICT -> ApiException.Conflict(message)
            in SERVER_ERROR_MIN..SERVER_ERROR_MAX -> ApiException.ServerError(code, message)
            else -> IOException(message)
        }
    }

    private fun buildErrorMessage(response: Response<*>, errorBody: ResponseBody?): String {
        val details = errorBody?.let {
            try { it.string() } catch (e: CancellationException) { throw e } catch (_: IOException) { null }
        } ?: response.message()
        return "HTTP ${response.code()}: $details"
    }

    private companion object {
        private const val BAD_REQUEST = 400
        private const val UNAUTHORIZED = 401
        private const val NOT_FOUND = 404
        private const val CONFLICT = 409
        private const val SERVER_ERROR_MIN = 500
        private const val SERVER_ERROR_MAX = 599
    }
}
