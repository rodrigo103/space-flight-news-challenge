package com.example.myandroidapp.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Infraestructura de test (no es un doble): JUnit [TestWatcher] que reemplaza `Dispatchers.Main`
 * por un [StandardTestDispatcher] antes de cada test y lo restaura al terminar.
 *
 * Permite controlar el virtual time del `viewModelScope` (`advanceUntilIdle`, `runCurrent`) con
 * tests deterministas y sin dispatchers reales (nota 28, §2.1).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val testDispatcher = StandardTestDispatcher()

    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}
