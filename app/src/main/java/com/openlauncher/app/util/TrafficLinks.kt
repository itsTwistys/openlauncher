package com.openlauncher.app.util

fun trafficMapUrl(location: LocationData?, nowElapsedMs: Long): String {
    val fix = location?.takeIf { nowElapsedMs - it.elapsedRealtimeMs in 0..30_000 &&
        it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
    val center = fix?.let { "&center=${it.latitude}%2C${it.longitude}&zoom=13" }.orEmpty()
    return "https://www.google.com/maps/@?api=1&map_action=map&basemap=roadmap&layer=traffic$center"
}
