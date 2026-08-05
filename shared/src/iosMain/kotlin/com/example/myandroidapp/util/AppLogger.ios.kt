package com.example.myandroidapp.util

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun d(message: String, tag: String?) {
        NSLog("%s%s", tag?.let { "[$it] " } ?: "", message)
    }

    actual fun e(throwable: Throwable?, message: String, tag: String?) {
        NSLog("%s%s | %s", tag?.let { "[$it] " } ?: "", message, throwable?.message ?: "")
    }
}
