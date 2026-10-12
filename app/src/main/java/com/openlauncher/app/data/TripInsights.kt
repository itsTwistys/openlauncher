package com.openlauncher.app.data

import java.util.Calendar
import java.util.TimeZone

/** Aggregates over saved trips. Only the totals each trip already stores are used; no route is kept or reconstructed. */
data class TripStats(
    val trips: Int = 0,
    val totalMeters: Double = 0.0,
    val driveSeconds: Double = 0.0,
    val idleSeconds: Double = 0.0,
    val longest: TripRecord? = null
) {
    val averageMps: Double get() = if (driveSeconds > 0) totalMeters / driveSeconds else 0.0
    /** Share of recorded time spent stopped, 0..1. */
    val idleShare: Double get() = (driveSeconds + idleSeconds).let { if (it > 0) idleSeconds / it else 0.0 }
    val averageMetersPerTrip: Double get() = if (trips > 0) totalMeters / trips else 0.0
}

/** Statistics for trips that started at or after [sinceMs] (0 = all saved trips). */
fun tripStats(history: List<TripRecord>, sinceMs: Long = 0L): TripStats {
    val chosen = history.filter { it.startedAtMs >= sinceMs && it.distanceMeters.isFinite() }
    return TripStats(
        trips = chosen.size,
        totalMeters = chosen.sumOf { it.distanceMeters },
        driveSeconds = chosen.sumOf { it.driveSeconds },
        idleSeconds = chosen.sumOf { it.idleSeconds },
        longest = chosen.maxByOrNull { it.distanceMeters }
    )
}

/**
 * Distance per calendar week for the last [weeks] weeks, oldest first, newest (the current week) last.
 * Weeks start on Monday in [zone].
 */
fun weeklyDistance(history: List<TripRecord>, nowMs: Long, weeks: Int = 8, zone: TimeZone = TimeZone.getDefault()): List<Double> {
    require(weeks in 1..52)
    val start = Calendar.getInstance(zone).apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        // Step back to Monday of the current week.
        val offset = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -offset)
    }
    val boundaries = (0..weeks).map { index ->
        (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7 * (index - (weeks - 1))) }.timeInMillis
    }
    return (0 until weeks).map { index ->
        val from = boundaries[index]
        val to = if (index == weeks - 1) Long.MAX_VALUE else boundaries[index + 1]
        history.filter { it.startedAtMs in from until to && it.distanceMeters.isFinite() }.sumOf { it.distanceMeters }
    }
}
