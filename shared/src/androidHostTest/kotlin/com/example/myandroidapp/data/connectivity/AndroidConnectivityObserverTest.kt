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
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AndroidConnectivityObserverTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowConnectivityManager = shadowOf(connectivityManager)
    }

    private fun observer() = AndroidConnectivityObserver(context)

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

    private fun registeredCallback() = shadowConnectivityManager.networkCallbacks.single()

    @Test
    fun `snapshot is unavailable when there is no active network`() = runTest {
        shadowConnectivityManager.setActiveNetworkInfo(null)

        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    @Test
    fun `snapshot is unavailable when the active network has no internet capability`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = false))

        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    @Test
    fun `snapshot is available when the active network has internet capability`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())
        }
    }

    @Test
    fun `collecting registers a network callback`() = runTest {
        observer().status.test {
            awaitItem()

            assertEquals(1, shadowConnectivityManager.networkCallbacks.size)
        }
    }

    @Test
    fun `status turns available when a network becomes available`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())

            registeredCallback().onAvailable(activeNetwork)

            assertEquals(ConnectivityStatus.Available, awaitItem())
        }
    }

    @Test
    fun `status turns unavailable when the network is lost`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onLost(activeNetwork)

            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

    @Test
    fun `status turns unavailable when the network request is unfulfilled`() = runTest {
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, capabilities(internet = true))

        observer().status.test {
            assertEquals(ConnectivityStatus.Available, awaitItem())

            registeredCallback().onUnavailable()

            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }
    }

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

    @Test
    fun `cancelling collection unregisters the network callback`() = runTest {
        observer().status.test {
            assertEquals(ConnectivityStatus.Unavailable, awaitItem())
        }

        assertTrue(shadowConnectivityManager.networkCallbacks.isEmpty())
    }
}
