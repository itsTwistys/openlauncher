package com.openlauncher.app.util

import android.location.Location

/** GPS course only: never substitute the dashboard's magnetic sensor orientation. */
internal fun travelHeading(previous: Location?, current: Location): Float? {
    if (current.provider != android.location.LocationManager.GPS_PROVIDER ||
        !current.hasAccuracy() || !current.accuracy.isFinite() || current.accuracy > 50f) return null
    if (current.hasBearing() && current.bearing.isFinite()) return (current.bearing % 360f + 360f) % 360f
    if (previous == null || previous.provider != current.provider || !previous.hasAccuracy() ||
        !previous.accuracy.isFinite() || previous.accuracy > 50f) return null
    val age = (current.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000
    if (age !in 1..15_000) return null
    val distance = previous.distanceTo(current)
    // Two fixes must move farther than their accuracy envelopes to exclude stationary jitter.
    if (distance < maxOf(8f, previous.accuracy + current.accuracy) || distance / (age / 1000f) < 2f) return null
    return (previous.bearingTo(current) % 360f + 360f) % 360f
}
