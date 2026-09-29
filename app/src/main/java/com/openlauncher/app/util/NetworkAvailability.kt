package com.openlauncher.app.util

import android.net.NetworkCapabilities

/** Internet usability is independent of Wi-Fi, cellular, Ethernet or VPN transport. */
fun networkCanLoadInternet(caps: NetworkCapabilities?): Boolean = caps != null &&
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
