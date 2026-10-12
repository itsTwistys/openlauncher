package com.openlauncher.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AgendaEvent(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    val location: String = ""
)

private const val DAY_MS = 86_400_000L

/** Whole days since the epoch in [zone], rounding toward the past so times before 1970 still bucket correctly. */
private fun dayNumber(ms: Long, zone: TimeZone): Long {
    val local = ms + zone.getOffset(ms)
    val days = local / DAY_MS
    return if (local % DAY_MS < 0) days - 1 else days
}

/** Short, glanceable start time: Now, All day, 14:30, Tomorrow 09:00 or Fri 09:00. */
fun agendaTimeLabel(
    event: AgendaEvent,
    nowMs: Long,
    use12Hour: Boolean,
    zone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault()
): String {
    if (event.allDay) return "All day"
    if (event.startMs <= nowMs && event.endMs > nowMs) return "Now"
    val clock = SimpleDateFormat(if (use12Hour) "h:mm a" else "HH:mm", locale).apply { timeZone = zone }.format(Date(event.startMs))
    return when (val ahead = dayNumber(event.startMs, zone) - dayNumber(nowMs, zone)) {
        0L -> clock
        1L -> "Tomorrow $clock"
        else -> if (ahead in 2L..6L)
            SimpleDateFormat("EEE", locale).apply { timeZone = zone }.format(Date(event.startMs)) + " " + clock
        else clock
    }
}
