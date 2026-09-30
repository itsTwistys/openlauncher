package com.openlauncher.app.data

import android.app.Notification
import android.os.Bundle
import com.google.gson.Gson
import com.openlauncher.app.model.*
import com.openlauncher.app.service.navigationText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherNavigationTest {
    @Test fun weatherUsesCurrentHourAndDoesNotInventExpiredRain() {
        val now = Instant.parse("2026-09-30T14:30:00Z").toEpochMilli()
        val hour = now - 1_800_000
        val w = WeatherState(25.0, 3, 16.09344, updatedAtMs = now, isDay = true,
            timeZoneId = "America/New_York", sunriseMs = hour, sunsetMs = hour + 12*3_600_000,
            hourly = listOf(ForecastHour(hour, 25.0, 29.0, 20, 70), ForecastHour(hour+3_600_000, 26.0, 30.0, 60, 72)))
        assertEquals(29.0, w.currentForecast(now)?.feelsLikeCelsius)
        assertEquals(60, w.nextHourRain(now))
        assertNull(w.nextHourRain(now+3*3_600_000))
        assertNull(w.currentForecast(now+3*3_600_000))
        assertEquals("10 mph", w.windText(false))
        assertEquals("10:00 AM", w.sunTime(w.sunriseMs, true))
        val restored = requireNotNull(WeatherCache.decode(WeatherCache.encode(w), now))
        assertEquals(w.sunsetMs, restored.sunsetMs)
        assertEquals(70, restored.currentForecast(now)?.humidityPercent)
        assertEquals("Unavailable", w.sunTime(null, true))
    }
    @Test fun apiParsesOptionalSunTimesAndHumidity() {
        val response = Gson().fromJson("""{"current_weather":{"temperature":25,"windspeed":8,"weathercode":3,"is_day":1},"hourly":{"time":[100],"relative_humidity_2m":[75]},"daily":{"sunrise":[1000],"sunset":[2000]}}""", OpenMeteoResponse::class.java)
        assertEquals(75, response.hourly?.humidity?.first())
        assertEquals(2000L, response.daily?.sunset?.first())
    }
    @Test fun directionsKeepRoadAndTripDetailsWithoutDuplicateInstructions() {
        val n = Notification().apply { extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, "300 ft")
            putCharSequence(Notification.EXTRA_TEXT, "Turn right onto Main St")
            putCharSequence(Notification.EXTRA_BIG_TEXT, "Turn right onto Main St\n300 ft")
            putCharSequence(Notification.EXTRA_SUB_TEXT, "9 min · 1.9 mi · 6:47 PM")
        } }
        val parsed = navigationText(n)
        assertEquals("300 ft", parsed.title)
        assertEquals("Turn right onto Main St", parsed.instruction)
        assertEquals("9 min · 1.9 mi · 6:47 PM", parsed.trip)
        n.extras.putCharSequence(Notification.EXTRA_BIG_TEXT, "Turn right onto Main St toward Downtown")
        assertEquals("Turn right onto Main St toward Downtown", navigationText(n).instruction)
        assertEquals("", navigationText(Notification().apply { extras = Bundle() }).title)
    }
    @Test fun mapThemeRoundTripsAndRejectsInvalidValues() {
        assertEquals("DARK", SettingsBackup.decode(SettingsBackup.encode(AppSettings(mapTheme="DARK"))).mapTheme)
        assertThrows(IllegalArgumentException::class.java) { SettingsBackup.decode(SettingsBackup.encode(AppSettings(mapTheme="UNKNOWN"))) }
    }
}
