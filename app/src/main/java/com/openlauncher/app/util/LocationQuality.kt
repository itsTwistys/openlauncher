package com.openlauncher.app.util

/** Prevent a recent accurate GPS fix from jumping to a much poorer Wi-Fi/network fix. */
fun acceptLocationFix(previous: LocationData?, timestamp: Long, accuracy: Float): Boolean {
    if (!accuracy.isFinite() || accuracy < 0 || timestamp < 0) return false
    if (previous == null) return true
    if (timestamp < previous.elapsedRealtimeMs) return false
    val recent = timestamp - previous.elapsedRealtimeMs < 10_000
    return !recent || accuracy <= maxOf(50f, previous.accuracy * 2f)
}
