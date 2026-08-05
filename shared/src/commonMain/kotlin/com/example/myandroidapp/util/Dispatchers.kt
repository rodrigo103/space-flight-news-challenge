package com.example.myandroidapp.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * `Dispatchers.IO` es `internal` en `commonMain`, así que no se puede usar directamente en código
 * compartido: cada plataforma expone el suyo.
 */
expect val ioDispatcher: CoroutineDispatcher
