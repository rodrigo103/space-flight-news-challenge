package com.example.myandroidapp.util

import timber.log.Timber

actual object AppLogger {
    actual fun d(message: String, tag: String?) {
        if (tag != null) Timber.tag(tag).d(message) else Timber.d(message)
    }

    actual fun e(throwable: Throwable?, message: String, tag: String?) {
        if (tag != null) Timber.tag(tag).e(throwable, message) else Timber.e(throwable, message)
    }
}
