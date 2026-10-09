package com.openlauncher.app.util

import android.location.Location
import android.location.LocationManager

private fun usableGps(fix: Location) = fix.provider == LocationManager.GPS_PROVIDER &&
    fix.hasAccuracy() && fix.accuracy.isFinite() && fix.accuracy in 0f..50f

/** Infer motion only beyond both accuracy envelopes, even when hardware supplies bearing. */
internal fun inferredTravelSpeed(previous: Location?, current: Location): Float? {
    if (previous == null || !usableGps(previous) || !usableGps(current)) return null
    val age = (current.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000
    if (age !in 1..15_000) return null
    val distance = previous.distanceTo(current)
    if (distance < maxOf(8f, previous.accuracy + current.accuracy)) return null
    return (distance / (age / 1000f)).takeIf { it >= 2f }
}

/** GPS course only: never substitute the dashboard's magnetic sensor orientation. */
internal fun travelHeading(previous: Location?, current: Location): Float? {
    if (!usableGps(current)) return null
    if (current.hasBearing() && current.bearing.isFinite()) return (current.bearing % 360f + 360f) % 360f
    if (inferredTravelSpeed(previous, current) == null) return null
    return (previous!!.bearingTo(current) % 360f + 360f) % 360f
}
