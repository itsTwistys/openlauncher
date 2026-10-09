package com.openlauncher.app.util

import android.location.Location
import android.location.LocationManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TravelHeadingTest {
    private fun fix(lat: Double, lon: Double, time: Long, accuracyMeters: Float = 3f) =
        Location(LocationManager.GPS_PROVIDER).apply {
            latitude = lat; longitude = lon; elapsedRealtimeNanos = time * 1_000_000
            accuracy = accuracyMeters
        }
    @Test fun northIsValidAndPositionFallbackWorksWithoutSpeedOrBearing() {
        val a = fix(25.0, -80.0, 1000)
        val b = fix(25.0, -79.9998, 6000)
        assertEquals(90f, travelHeading(a, b)!!, 1f)
        b.bearing = 0f
        assertEquals(0f, travelHeading(a, b)!!, 0f)
    }
    @Test fun hardwareBearingCannotTurnJitterIntoSpeed() {
        val a = fix(25.0, -80.0, 1000)
        val jitter = fix(25.000001, -80.0, 2000).apply { bearing = 90f }
        assertNotNull(travelHeading(a, jitter))
        assertNull(inferredTravelSpeed(a, jitter))
        assertNull(inferredTravelSpeed(fix(25.0, -80.0, 1000, 100f), jitter))
        assertNotNull(inferredTravelSpeed(a, fix(25.0, -79.9998, 6000)))
    }
    @Test fun jitterStaleAndNetworkFixesCannotSetCourse() {
        val a = fix(25.0, -80.0, 1000)
        assertNull(travelHeading(a, fix(25.000001, -80.0, 2000)))
        assertNull(travelHeading(a, fix(25.001, -80.0, 20000)))
        val network = fix(25.001, -80.0, 2000).apply { provider = LocationManager.NETWORK_PROVIDER; bearing = 90f }
        assertNull(travelHeading(a, network))
        assertNull(travelHeading(a, fix(25.001, -80.0, 2000, 100f)))
    }
}
