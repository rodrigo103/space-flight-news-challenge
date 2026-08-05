package com.example.myandroidapp.util

import timber.log.Timber

actual object AppLogger {
    actual fun d(message: String) {
        Timber.d(message)
    }

    actual fun e(throwable: Throwable?, message: String) {
        Timber.e(throwable, message)
    }
}
