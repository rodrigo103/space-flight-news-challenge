package com.example.myandroidapp.data.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Monitoreo reactivo de conectividad.
 *
 * Es una interfaz con factory por plataforma en vez de `expect class`, porque los constructores
 * no coinciden: Android necesita un `Context` y iOS no recibe nada. `expect class` exige que las
 * firmas de los constructores sean iguales en todos los targets.
 */
interface ConnectivityObserver {
    val status: Flow<ConnectivityStatus>
}
