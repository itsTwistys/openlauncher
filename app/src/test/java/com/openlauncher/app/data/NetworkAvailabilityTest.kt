package com.openlauncher.app.data

import android.net.NetworkCapabilities
import com.openlauncher.app.util.networkCanLoadInternet
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkAvailabilityTest {
    private fun caps(transport: Int, validated: Boolean) = NetworkCapabilities().apply {
        addTransportType(transport)
        addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (validated) addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    @Test fun validatedEthernetAndVpnAreUsable() {
        assertTrue(networkCanLoadInternet(caps(NetworkCapabilities.TRANSPORT_ETHERNET, true)))
        assertTrue(networkCanLoadInternet(caps(NetworkCapabilities.TRANSPORT_VPN, true)))
    }
    @Test fun wifiCaptivePortalAndDisconnectedNetworksAreOffline() {
        assertFalse(networkCanLoadInternet(caps(NetworkCapabilities.TRANSPORT_WIFI, false)))
        assertFalse(networkCanLoadInternet(null))
        assertTrue(networkCanLoadInternet(caps(NetworkCapabilities.TRANSPORT_WIFI, true)))
    }
}
