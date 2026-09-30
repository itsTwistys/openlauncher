package com.openlauncher.app.model

import com.openlauncher.app.util.dashboardTimeZone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun WeatherState.currentForecast(nowMs: Long): ForecastHour? = hourly.lastOrNull {
    nowMs - it.timeMs in 0 until 3_600_000L
}
fun WeatherState.nextHourRain(nowMs: Long): Int? = hourly.firstOrNull {
    it.timeMs - nowMs in 0..3_600_000L
}?.rainPercent
fun WeatherState.sunTime(epoch: Long?, twelveHour: Boolean): String = epoch?.takeIf { it > 0 }?.let {
    SimpleDateFormat(if (twelveHour) "h:mm a" else "HH:mm", Locale.getDefault()).apply {
        timeZone = dashboardTimeZone("AUTO", this@sunTime)
    }.format(Date(it))
} ?: "Unavailable"
fun WeatherState.windText(metric: Boolean): String = "${Math.round(if (metric) windspeedKmh else windspeedKmh / 1.609344)} ${if (metric) "km/h" else "mph"}"
