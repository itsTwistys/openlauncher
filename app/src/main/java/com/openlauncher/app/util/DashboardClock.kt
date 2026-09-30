package com.openlauncher.app.util

import com.openlauncher.app.model.WeatherState
import java.util.Calendar
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone

/** Changes display only. Never writes the head unit's clock or time-zone settings. */
fun dashboardTimeZone(choice: String, weather: WeatherState?, device: TimeZone = TimeZone.getDefault()): TimeZone {
    if (choice == "SYSTEM") return device
    if (choice != "AUTO" && choice in TimeZone.getAvailableIDs()) return TimeZone.getTimeZone(choice)
    val local = weather?.timeZoneId
    if (local != null && local in TimeZone.getAvailableIDs()) return TimeZone.getTimeZone(local)
    // Backward-compatible cached forecasts already contain a location-derived UTC offset.
    return weather?.utcOffsetSeconds?.takeIf { it in -43200..50400 }?.let {
        SimpleTimeZone((it * 1000).toInt(), "Local")
    } ?: device
}
fun dashboardCalendar(nowMs: Long, choice: String, weather: WeatherState?): Calendar =
    Calendar.getInstance(dashboardTimeZone(choice, weather)).apply { timeInMillis = nowMs }
fun clockDigits(cal: Calendar, twelveHour: Boolean, seconds: Boolean): String = String.format(Locale.getDefault(),
    if (twelveHour) "%d:%02d" else "%02d:%02d",
    if (twelveHour) (cal.get(Calendar.HOUR_OF_DAY) + 11) % 12 + 1 else cal.get(Calendar.HOUR_OF_DAY),
    cal.get(Calendar.MINUTE)) + if (seconds) String.format(Locale.getDefault(), ":%02d", cal.get(Calendar.SECOND)) else ""
fun clockPeriod(cal: Calendar): String = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
