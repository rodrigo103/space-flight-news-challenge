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
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * Covers [ConnectivityObserver] against Robolectric's [ShadowConnectivityManager]: the flow
 * snapshots connectivity at collection time, follows the [ConnectivityManager.NetworkCallback]
 * events, deduplicates repeated states and unregisters the callback when the collector is
 * cancelled. Robolectric's default active network is a mobile network without the INTERNET
 * capability, so the baseline status is [ConnectivityStatus.Unavailable].
 *
 * **Dobles usados:**
 * - [shadowConnectivityManager] (Robolectric) → FAKE del framework: implementación simplificada
 *   pero funcional del ConnectivityManager. Sus setters (`setActiveNetworkInfo`,
 *   `setNetworkCapabilities`) son uso STUB: enlatan el estado que consume el SUT; leer
 *   `networkCallbacks` es uso SPY: registra las interacciones para assertearlas.
 * - `capabilities(internet)` (vía [ShadowNetworkCapabilities]) → STUB: respuesta fija al
 *   `hasCapability` que consulta el SUT.
 * - NO dobles: Robolectric es entorno de ejecución JVM y Turbine un colector de flows —
 *   infraestructura de test. El SUT ([ConnectivityObserver]) es real.
 *
 * **Estilos (state/behavioral):** mayoría STATE — se assertean las emisiones del flow ante cada
 * estímulo; BEHAVIORAL en el registro/desregistro del callback (la interacción que importa).
 */
// Robolectric = entorno de ejecución JVM (no un doble): corre el framework Android real sin device.
@RunWith(RobolectricTestRunner::class)
class ConnectivityObserverTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // DOBLE — FAKE del framework: el shadow es una implementación simplificada pero
        // funcional (registra callbacks y expone estado). Los setters de más abajo son uso
        // STUB sobre el fake; `networkCallbacks` es uso SPY.
        shadowConnectivityManager = shadowOf(connectivityManager)
    }

    private fun observer() = ConnectivityObserver(context)

    // STUB — objeto prearmado con una respuesta fija (con/sin INTERNET) que el SUT consulta vía
    // `hasCapability`; el test no verifica cómo se usó, solo lo enlata como estado de entrada.
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

    // Uso SPY del fake: `networkCallbacks` registra las interacciones del SUT con el manager;
    // el test las lee para assertear el registro (y para disparar eventos desde el test).
    private fun registeredCallback() = shadowConnectivityManager.networkCallbacks.single()

    // STATE — el setter del shadow enlata el estado de entrada (uso STUB) y se assertea la
    // primera emisión del flow: Unavailable. Snapshot al momento de colectar.
    @Test
    fun `snapshot is unavailable when there is no active network`() = runTest {
        shadowConnectivityManager.setActiveNetworkInfo(null)

        // Infraestructura (no doble): Turbine colecta el flow para consumir emisiones una a una.
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — STUB de capabilities sin INTERNET; la aserción es la emisión resultante del flow,
    // no cómo el observer consultó al ConnectivityManager.
    @Test
    fun `snapshot is unavailable when the active network has no internet capability`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = false))

        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — mismo snapshot con capabilities(internet = true): al colectar, el flow debe emitir
    // Available. Assert sobre la emisión, no sobre las llamadas al framework.
    @Test
    fun `snapshot is available when the active network has internet capability`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())
        }
    }

    // BEHAVIORAL — verifica la interacción del SUT con el framework: al colectar debe quedar
    // registrado exactamente 1 NetworkCallback. La lectura de `networkCallbacks` es uso SPY del
    // fake (el shadow registra las invocaciones del SUT).
    @Test
    fun `collecting registers a network callback`() = runTest {
        observer().status.test {
            awaitItem()

            assertEquals(1, shadowConnectivityManager.networkCallbacks.size)
        }
    }

    // STATE — el callback del shadow (estímulo del framework) dispara onAvailable y se assertea
    // la emisión Available del flow: la interacción estimula, la verificación es de estado.
    @Test
    fun `status turns available when a network becomes available`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())

            registeredCallback().onAvailable(activeNetwork)

            assertEquals(ConnectivityStatus.Available, awaitItem())
        }
    }

    // STATE — arranca en Available por el STUB de capabilities y el estímulo onLost produce la
    // emisión Unavailable: verificación de estado.
    @Test
    fun `status turns unavailable when the network is lost`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onLost(activeNetwork)

            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — el estímulo es onUnavailable y se assertea la emisión resultante (Unavailable);
    // sin chequeos de interacción.
    @Test
    fun `status turns unavailable when the network request is unfulfilled`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onUnavailable()

            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    // STATE — secuencia de capabilities (STUB) reportadas por el callback: Available →
    // Unavailable. Se assertea cada emisión del flow, no las llamadas al manager.
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

    // STATE — verifica el contrato de estado (deduplicación, distinctUntilChanged): estímulos
    // repetidos no re-emiten (`expectNoEvents`) y un cambio real sí emite. Sigue siendo state
    // testing aunque use varios callbacks como estímulo.
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

    // BEHAVIORAL — verifica la interacción de cleanup: al cancelar la colecta, la lista de
    // `networkCallbacks` (uso SPY del fake) queda vacía: el SUT desregistró su callback.
    @Test
    fun `cancelling collection unregisters the network callback`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }

        assertTrue(shadowConnectivityManager.networkCallbacks.isEmpty())
    }
}
