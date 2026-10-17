package com.example.myandroidapp.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * Covers [AndroidConnectivityObserver] against Robolectric's [ShadowConnectivityManager]: the
 * flow snapshots connectivity at collection time, follows the [ConnectivityManager.NetworkCallback]
 * events, deduplicates repeated states and unregisters the callback when the collector is
 * cancelled. Robolectric's default active network is a mobile network without the INTERNET
 * capability, so the baseline status is [ConnectivityStatus.Unavailable].
 *
 * Runs in the `androidHostTest` source set of `:shared` (KMP host tests on the JVM). SDK is pinned
 * because Robolectric's SDK 36 requires Java 21 and this project targets Java 17.
 *
 * Dobles usados:
 * - `shadowConnectivityManager` ([ShadowConnectivityManager] de Robolectric) → FAKE del framework:
 *   implementación simplificada pero funcional de [ConnectivityManager].
 * - Los setters (`setActiveNetworkInfo`, `setNetworkCapabilities`) → uso STUB: dejan estado
 *   enlatado que consume el SUT al snapshotear.
 * - `capabilities(internet)` vía [ShadowNetworkCapabilities] → STUB: respuesta fija a
 *   `hasCapability(NET_CAPABILITY_INTERNET)`.
 * - Leer `shadowConnectivityManager.networkCallbacks` → uso SPY: registra las interacciones
 *   (callbacks registrados/desregistrados) para assertearlas.
 * - Robolectric y Turbine → infraestructura de test, no son dobles.
 *
 * Estilos (state/behavioral):
 * - STATE: los 3 snapshots iniciales, los 4 tests de eventos del callback y `repeated states…`.
 * - BEHAVIORAL: `collecting registers a network callback` y `cancelling collection unregisters…`.
 *
 * Entorno: source set `androidHostTest` de KMP (JVM), con `@Config(sdk = [34])`.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AndroidConnectivityObserverTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    // FAKE del framework — el shadow de Robolectric es una implementación simplificada pero
    // funcional de ConnectivityManager: guarda estado real y dispara callbacks de verdad.
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowConnectivityManager = shadowOf(connectivityManager)
    }

    private fun observer() = AndroidConnectivityObserver(context)

    // STUB — NetworkCapabilities enlatado: respuesta fija a `hasCapability(NET_CAPABILITY_INTERNET)`.
    private fun capabilities(internet: Boolean): NetworkCapabilities =
        ShadowNetworkCapabilities.newInstance().also { capabilities ->
            if (internet) {
                shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

    private val activeNetwork
        get() = requireNotNull(connectivityManager.activeNetwork) {
            "Robolectric always reports an active network unless the test clears it"
        }

    // Uso SPY — `networkCallbacks` registra las interacciones (los callbacks que registró el SUT)
    // para dispararles eventos y assertear la registración/desregistración.
    private fun registeredCallback() = shadowConnectivityManager.networkCallbacks.single()

    // STATE — aserción sobre el primer snapshot del flow (Unavailable): importa el estado
    // emitido. El "sin red" se enlata con un setter del shadow (uso STUB).
    @Test
    fun `snapshot is unavailable when there is no active network`() = runTest {
        // Uso STUB — `setActiveNetworkInfo`/`setNetworkCapabilities` dejan estado fijo que el
        // SUT consume al snapshotear.
        shadowConnectivityManager.setActiveNetworkInfo(null)

        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — mismo formato: capabilities sin INTERNET (STUB) ⇒ el snapshot emite Unavailable.
    @Test
    fun `snapshot is unavailable when the active network has no internet capability`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = false))

        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — el snapshot inicial respeta las capabilities con INTERNET (STUB) ⇒ Available.
    @Test
    fun `snapshot is available when the active network has internet capability`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())
        }
    }

    // BEHAVIORAL — verifica la interacción con el framework: al colectar, el SUT registra
    // exactamente un NetworkCallback (leído del shadow, uso SPY).
    @Test
    fun `collecting registers a network callback`() = runTest {
        observer().status.test {
            awaitItem()

            assertEquals(1, shadowConnectivityManager.networkCallbacks.size)
        }
    }

    // STATE — transición del flow: Unavailable → Available al disparar `onAvailable` sobre el
    // callback registrado.
    @Test
    fun `status turns available when a network becomes available`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())

            registeredCallback().onAvailable(activeNetwork)

            assertEquals(ConnectivityStatus.Available, awaitItem())
        }
    }

    // STATE — transición Available → Unavailable disparando `onLost` sobre el callback.
    @Test
    fun `status turns unavailable when the network is lost`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onLost(activeNetwork)

            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — transición a Unavailable cuando el request se marca `onUnavailable`.
    @Test
    fun `status turns unavailable when the network request is unfulfilled`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onUnavailable()

            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — el flow sigue los cambios de capability: Available ↔ Unavailable según
    // `onCapabilitiesChanged` (las capabilities las provee el STUB `capabilities()`).
    @Test
    fun `status follows capability changes reported for the network`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())

            registeredCallback().onCapabilitiesChanged(activeNetwork, capabilities(internet = true))
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onCapabilitiesChanged(activeNetwork, capabilities(internet = false))
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — contrato de emisiones del flow (`distinctUntilChanged`): eventos repetidos no
    // re-emiten (`expectNoEvents`) y solo se emite al cambiar de estado.
    @Test
    fun `repeated states are not re-emitted`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            val callback = registeredCallback()
            callback.onAvailable(activeNetwork)
            callback.onCapabilitiesChanged(activeNetwork, capabilities(internet = true))
            expectNoEvents()

            callback.onLost(activeNetwork)
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())

            callback.onLost(activeNetwork)
            callback.onCapabilitiesChanged(activeNetwork, capabilities(internet = false))
            expectNoEvents()
        }
    }

    // BEHAVIORAL — verificación de la interacción de cleanup: al cancelar la colección, el SUT
    // desregistra el callback (el shadow queda sin callbacks — lectura del SPY).
    @Test
    fun `cancelling collection unregisters the network callback`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }

        assertTrue(shadowConnectivityManager.networkCallbacks.isEmpty())
    }
}
