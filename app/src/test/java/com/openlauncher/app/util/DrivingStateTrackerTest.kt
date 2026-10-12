package com.openlauncher.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DrivingStateTrackerTest {
    private fun tracker() = DrivingStateTracker(driveKmh = 10f, driveHoldMs = 5_000, parkedKmh = 3f, parkedHoldMs = 300_000)

    @Test fun startsUnknownAndNeedsSustainedSpeedToDrive() {
        val t = tracker()
        assertEquals(DrivingState.UNKNOWN, t.state)
        assertEquals(DrivingState.UNKNOWN, t.update(40f, 0))
        assertEquals(DrivingState.UNKNOWN, t.update(40f, 4_999))
        assertEquals(DrivingState.DRIVING, t.update(40f, 5_000))
    }

    @Test fun aShortBurstOfSpeedDoesNotStartDriving() {
        val t = tracker()
        t.update(30f, 0)
        t.update(0f, 2_000)
        t.update(30f, 3_000)
        assertEquals(DrivingState.UNKNOWN, t.update(30f, 7_000))
        assertEquals(DrivingState.DRIVING, t.update(30f, 8_000))
    }

    @Test fun trafficLightsNeverReturnToParked() {
        val t = tracker()
        t.update(50f, 0); t.update(50f, 6_000)
        assertEquals(DrivingState.DRIVING, t.state)
        // A two minute red light, then moving again.
        for (s in 7..126) t.update(0f, s * 1_000L)
        assertEquals(DrivingState.DRIVING, t.state)
        t.update(30f, 127_000)
        assertEquals(DrivingState.DRIVING, t.state)
    }

    @Test fun parkingNeedsAFullStationaryDelay() {
        val t = tracker()
        t.update(50f, 0); t.update(50f, 6_000)
        t.update(0f, 10_000)
        assertEquals(DrivingState.DRIVING, t.update(0f, 309_999))
        assertEquals(DrivingState.PARKED, t.update(0f, 310_000))
    }

    @Test fun creepingBetweenThresholdsKeepsStateAndResetsTheParkTimer() {
        val t = tracker()
        t.update(50f, 0); t.update(50f, 6_000)
        t.update(0f, 10_000)
        t.update(5f, 200_000) // creeping resets the stationary timer
        assertEquals(DrivingState.DRIVING, t.update(0f, 300_000))
        assertEquals(DrivingState.DRIVING, t.update(0f, 599_999))
        assertEquals(DrivingState.PARKED, t.update(0f, 600_000))
    }

    @Test fun invalidSpeedsAreIgnored() {
        val t = tracker()
        assertEquals(DrivingState.UNKNOWN, t.update(Float.NaN, 0))
        assertEquals(DrivingState.UNKNOWN, t.update(-1f, 10_000))
        assertEquals(DrivingState.UNKNOWN, t.update(Float.POSITIVE_INFINITY, 20_000))
    }

    @Test fun profilesMapToSavedLayoutNames() {
        assertEquals("Driving", layoutProfileFor(DrivingState.DRIVING))
        assertEquals("Parked", layoutProfileFor(DrivingState.PARKED))
        assertEquals(null, layoutProfileFor(DrivingState.UNKNOWN))
    }
}
