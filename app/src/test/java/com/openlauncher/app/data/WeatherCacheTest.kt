package com.openlauncher.app.data

import com.openlauncher.app.model.ForecastHour
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.model.freshnessLabel
import org.junit.Assert.*
import org.junit.Test

class WeatherCacheTest {
    private val now = 1_800_000_000_000L
    private val weather = WeatherState(21.5, 3, 12.0, updatedAtMs = now - 600_000, isDay = true,
        hourly = listOf(ForecastHour(now + 3_600_000, 22.0, 23.0, 30)))

    @Test fun roundTripPreservesForecastAndOriginalUpdateTime() {
        assertEquals(weather, WeatherCache.decode(WeatherCache.encode(weather), now))
        val json = WeatherCache.encode(weather)
        assertFalse(json.contains("latitude"))
        assertFalse(json.contains("longitude"))
    }
    @Test fun corruptIncompleteExpiredAndFutureCachesAreIgnored() {
        assertNull(WeatherCache.decode("{", now))
        assertNull(WeatherCache.decode("{\"version\":1,\"weather\":{}}", now))
        assertNull(WeatherCache.decode(WeatherCache.encode(weather.copy(updatedAtMs = now - WeatherCache.MAX_AGE_MS - 1)), now))
        assertNull(WeatherCache.decode(WeatherCache.encode(weather.copy(updatedAtMs = now + 600_000)), now))
        assertNull(WeatherCache.decode(" ".repeat(64_001), now))
    }
    @Test fun invalidConditionsDoNotReachTheDashboard() {
        assertNull(WeatherCache.decode(WeatherCache.encode(weather.copy(temperatureCelsius = 1000.0)), now))
        assertNull(WeatherCache.decode(WeatherCache.encode(weather.copy(windspeedKmh = -1.0)), now))
    }
    @Test fun offlineAgeStaleExpiredAndClockChangesAreExplicit() {
        assertEquals("Offline · updated 10m ago", weather.freshnessLabel(now, false))
        assertEquals("Updated 35m ago · stale", weather.copy(updatedAtMs = now - 35 * 60_000).freshnessLabel(now, true))
        assertEquals("Offline · updated 2h ago · stale", weather.copy(updatedAtMs = now - 2 * 3_600_000).freshnessLabel(now, false))
        assertEquals("Forecast expired · reconnect to refresh", weather.copy(updatedAtMs = now - WeatherCache.MAX_AGE_MS).freshnessLabel(now, false))
        assertEquals("Update time unavailable", weather.copy(updatedAtMs = now + 60_000).freshnessLabel(now, true))
    }
    @Test fun renderingPreferenceRoundTripsAndOlderBackupsKeepCompatibilityMode() {
        val settings = AppSettings(mapSoftwareRendering = false)
        assertFalse(SettingsBackup.decode(SettingsBackup.encode(settings)).mapSoftwareRendering)
        assertTrue(SettingsBackup.decode("{\"format\":\"openlauncher-settings\",\"version\":1,\"settings\":{}}").mapSoftwareRendering)
    }
}
