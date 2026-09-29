package com.openlauncher.app.util

import android.net.NetworkCapabilities

/** Internet usability is independent of Wi-Fi, cellular, Ethernet or VPN transport. */
@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.M)
fun networkCanLoadInternet(caps: NetworkCapabilities?): Boolean = caps != null &&
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
