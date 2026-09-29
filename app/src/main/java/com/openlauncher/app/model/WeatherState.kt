package com.openlauncher.app.model

data class WeatherState(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val windspeedKmh: Double,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val highCelsius: Double? = null,
    val lowCelsius: Double? = null,
    val hourly: List<ForecastHour> = emptyList(),
    val utcOffsetSeconds: Long = 0,
    val isDay: Boolean
) {
    // roundToInt, not toInt — truncation displayed 20.9° as 20°
    fun temperatureDisplay(metric: Boolean): String =
        if (metric) "${Math.round(temperatureCelsius)}°C"
        else "${Math.round(celsiusToFahrenheit(temperatureCelsius))}°F"

    val conditionLabel: String get() = wmoCodeToLabel(weatherCode)
    val conditionIcon: String get() = wmoCodeToEmoji(weatherCode, isDay)
}

private fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0

private fun wmoCodeToLabel(code: Int): String = when (code) {
    0 -> "Clear"
    1, 2, 3 -> "Cloudy"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Showers"
    95 -> "Thunderstorm"
    96, 99 -> "Hail"
    else -> "Unknown"
}

private fun wmoCodeToEmoji(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1, 2 -> if (isDay) "⛅" else "🌤"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
    71, 73, 75 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}

data class ForecastHour(val timeMs: Long, val celsius: Double?, val feelsLikeCelsius: Double?, val rainPercent: Int?)
fun temperatureText(value: Double?, metric: Boolean): String = value?.let {
    "${Math.round(if (metric) it else it * 9 / 5 + 32)}°${if (metric) "C" else "F"}"
} ?: "Unavailable"
