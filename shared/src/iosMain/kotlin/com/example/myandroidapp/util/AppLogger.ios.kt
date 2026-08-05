package com.example.myandroidapp.util

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun d(message: String) {
        NSLog("%s", message)
    }

    actual fun e(throwable: Throwable?, message: String) {
        NSLog("%s | %s", message, throwable?.message ?: "")
    }
}
