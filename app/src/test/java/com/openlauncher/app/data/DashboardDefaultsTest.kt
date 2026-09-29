package com.openlauncher.app.data

import org.junit.Assert.*
import org.junit.Test

class DashboardDefaultsTest {
    @Test fun mapIsLargestAndMediaAndClockRemainVisible() {
        val settings = AppSettings()
        assertEquals(setOf("MAP", "NOW_PLAYING", "CLOCK"), settings.activeWidgetIds())
        assertTrue(validWidgetLayout(settings.widgetLayout))
        assertEquals(4, settings.widgetLayout.first { it.id == "MAP" }.let { it.spanX * it.spanY })
        assertFalse(settings.onlineMapEnabled)
    }
    @Test fun restorePreservesPersonalSettingsAndSavedProfiles() {
        val profile = LayoutProfile("Day", defaultWidgetLayout(), listOf("CLOCK", "MAP", "NOW_PLAYING"))
        val settings = AppSettings(vehicleName = "My car", homeDestination = "Home", onlineMapEnabled = true,
            activeLayoutProfile = "Day", autoDayNightProfiles = true, layoutProfiles = listOf(profile))
        val restored = settings.withDefaultDashboard()
        assertEquals(settings.homeDestination, restored.homeDestination)
        assertEquals(settings.vehicleName, restored.vehicleName)
        assertEquals(settings.layoutProfiles, restored.layoutProfiles)
        assertTrue(restored.onlineMapEnabled)
        assertFalse(restored.autoDayNightProfiles)
        assertEquals("", restored.activeLayoutProfile)
    }
    @Test fun weatherOnlyMigratesToCombinedClockInSamePlace() {
        val weather = WidgetConfig("WEATHER", 1, 1)
        val settings = AppSettings(showClock = false, showWeather = true, widgetLayout = listOf(weather),
            layoutProfiles = listOf(LayoutProfile("Day", listOf(weather), listOf("WEATHER"))))
        val result = settings.withMergedClockWeather()
        assertTrue(result.showClock)
        assertFalse(result.showWeather)
        assertEquals(listOf(weather.copy(id = "CLOCK")), result.widgetLayout)
        assertEquals(listOf("CLOCK"), result.layoutProfiles.single().enabledIds)
        assertEquals(result, result.withMergedClockWeather())
    }
    @Test fun clockPositionWinsWhenBothWereVisible() {
        val clock = WidgetConfig("CLOCK", 2, 1)
        val settings = AppSettings(showWeather = true,
            widgetLayout = listOf(clock, WidgetConfig("WEATHER", 0, 0)))
        assertEquals(listOf(clock), settings.withMergedClockWeather().widgetLayout)
    }
}
