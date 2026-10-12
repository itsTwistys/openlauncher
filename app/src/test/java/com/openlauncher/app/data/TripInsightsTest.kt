package com.openlauncher.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

class TripInsightsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        GregorianCalendar(utc).apply { clear(); set(year, month - 1, day, hour, 0, 0) }.timeInMillis
    private fun trip(start: Long, meters: Double, drive: Double = 600.0, idle: Double = 60.0) =
        TripRecord(startedAtMs = start, endedAtMs = start + 1, distanceMeters = meters, driveSeconds = drive, idleSeconds = idle)

    @Test fun emptyHistoryHasZeroStats() {
        val stats = tripStats(emptyList())
        assertEquals(0, stats.trips)
        assertEquals(0.0, stats.averageMps, 0.0)
        assertEquals(0.0, stats.idleShare, 0.0)
    }

    @Test fun totalsAverageAndLongestTrip() {
        val history = listOf(trip(1, 10_000.0, 1000.0, 0.0), trip(2, 30_000.0, 2000.0, 1000.0))
        val stats = tripStats(history)
        assertEquals(2, stats.trips)
        assertEquals(40_000.0, stats.totalMeters, 0.001)
        assertEquals(40_000.0 / 3000.0, stats.averageMps, 0.001)
        assertEquals(1000.0 / 4000.0, stats.idleShare, 0.001)
        assertEquals(20_000.0, stats.averageMetersPerTrip, 0.001)
        assertEquals(30_000.0, stats.longest?.distanceMeters ?: 0.0, 0.001)
    }

    @Test fun periodFilterUsesStartTime() {
        val history = listOf(trip(100, 1_000.0), trip(200, 2_000.0), trip(300, 4_000.0))
        assertEquals(2, tripStats(history, sinceMs = 200).trips)
        assertEquals(6_000.0, tripStats(history, sinceMs = 200).totalMeters, 0.001)
        assertNotNull(tripStats(history, sinceMs = 0).longest)
    }

    @Test fun weeklyDistanceBucketsByMondayWeek() {
        val now = at(2026, 10, 14, 12) // Wednesday; its week starts Monday 12 October
        val history = listOf(
            trip(at(2026, 10, 13), 1_000.0),   // this week
            trip(at(2026, 10, 12, 0), 500.0),  // Monday 00:00 belongs to this week
            trip(at(2026, 10, 6), 2_000.0),    // previous week
            trip(at(2026, 9, 29), 3_000.0),    // two weeks ago
            trip(at(2026, 9, 27), 4_000.0)     // older than the window
        )
        assertEquals(listOf(3_000.0, 2_000.0, 1_500.0), weeklyDistance(history, now, weeks = 3, zone = utc))
    }

    @Test fun weeklyDistanceOnAMondayStartsTheWeekToday() {
        val now = at(2026, 10, 12, 8)
        val history = listOf(trip(at(2026, 10, 11, 23), 700.0), trip(at(2026, 10, 12, 1), 300.0))
        assertEquals(listOf(700.0, 300.0), weeklyDistance(history, now, weeks = 2, zone = utc))
    }
}
