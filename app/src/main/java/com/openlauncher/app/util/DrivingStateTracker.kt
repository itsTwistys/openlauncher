package com.openlauncher.app.util

enum class DrivingState { UNKNOWN, DRIVING, PARKED }

/**
 * Debounced driving/parked classification from GPS speed.
 *
 * Moving only counts after [driveHoldMs] above [driveKmh], and parking only after [parkedHoldMs] below [parkedKmh],
 * so a traffic light or a short stop never flips the layout. Speeds between the two thresholds keep the current state.
 * This only reports state; it never blocks anything.
 */
class DrivingStateTracker(
    private val driveKmh: Float = 10f,
    private val driveHoldMs: Long = 5_000L,
    private val parkedKmh: Float = 3f,
    private val parkedHoldMs: Long = 5 * 60_000L
) {
    var state: DrivingState = DrivingState.UNKNOWN
        private set
    private var aboveSince = -1L
    private var belowSince = -1L

    fun update(speedKmh: Float, nowMs: Long): DrivingState {
        if (!speedKmh.isFinite() || speedKmh < 0f) return state
        if (speedKmh >= driveKmh) {
            belowSince = -1L
            if (aboveSince < 0L) aboveSince = nowMs
            if (state != DrivingState.DRIVING && nowMs - aboveSince >= driveHoldMs) state = DrivingState.DRIVING
        } else {
            aboveSince = -1L
            if (speedKmh <= parkedKmh) {
                if (belowSince < 0L) belowSince = nowMs
                if (state != DrivingState.PARKED && nowMs - belowSince >= parkedHoldMs) state = DrivingState.PARKED
            } else {
                belowSince = -1L
            }
        }
        return state
    }
}

/** Saved layout name that matches a driving state, or null when no switch applies. */
fun layoutProfileFor(state: DrivingState): String? = when (state) {
    DrivingState.DRIVING -> "Driving"
    DrivingState.PARKED -> "Parked"
    DrivingState.UNKNOWN -> null
}
