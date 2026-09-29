package com.openlauncher.app.data

import org.junit.Assert.*
import org.junit.Test

class DashboardTest {
    @Test fun resizeRelocatesFromRightEdge() {
        val layout = listOf(WidgetConfig("MAP", 2, 0), WidgetConfig("CLOCK", 0, 0))
        val resized = fitWidgetLayout(layout, layout[0].copy(spanX = 2))!!
        assertTrue(validWidgetLayout(resized))
        assertEquals(2, resized.first { it.id == "MAP" }.spanX)
        assertEquals(1, resized.first { it.id == "MAP" }.gridX)
    }
    @Test fun impossibleResizeIsRejected() {
        val layout = defaultWidgetLayout()
        assertNull(fitWidgetLayout(layout, layout[0].copy(spanX = 3, spanY = 2)))
        assertTrue(validWidgetLayout(layout))
    }
    @Test fun movementsNeverOverlap() {
        val layout = defaultWidgetLayout()
        for (widget in layout) for (x in 0 until GRID_COLS) for (y in 0 until GRID_ROWS) {
            assertTrue(validWidgetLayout(computeWidgetMove(layout, widget.id, x, y)))
        }
    }
    @Test fun allResizePresetsAreEitherValidOrRejected() {
        val layout = defaultWidgetLayout()
        for (widget in layout) for (w in 1..GRID_COLS) for (h in 1..GRID_ROWS) {
            val result = fitWidgetLayout(layout, widget.copy(spanX = w, spanY = h))
            if (result != null) {
                assertTrue(validWidgetLayout(result))
                assertEquals(layout.map { it.id }.toSet(), result.map { it.id }.toSet())
            }
        }
    }
    @Test fun backupRoundTripPreservesPreferencesAndRequiresLocationOptIn() {
        val settings = AppSettings(homeDestination = "Home address", use12HourTime = true,
            showClockSeconds = true, onlineMapEnabled = true,
            layoutProfiles = listOf(LayoutProfile("Day", defaultWidgetLayout(), AppSettings().activeWidgetIds().toList())))
        val restored = SettingsBackup.decode(SettingsBackup.encode(settings))
        assertEquals(settings.homeDestination, restored.homeDestination)
        assertEquals(settings.widgetLayout, restored.widgetLayout)
        assertTrue(restored.use12HourTime)
        assertTrue(restored.showClockSeconds)
        assertFalse(restored.onlineMapEnabled)
    }
    @Test(expected = IllegalArgumentException::class)
    fun backupRejectsOverlappingWidgets() {
        val bad = AppSettings(widgetLayout = listOf(WidgetConfig("CLOCK", 0, 0), WidgetConfig("NOW_PLAYING", 0, 0)))
        SettingsBackup.decode(SettingsBackup.encode(bad))
    }
    @Test(expected = IllegalArgumentException::class)
    fun backupRejectsDuplicateWidgetsInProfile() {
        val bad = AppSettings(layoutProfiles = listOf(LayoutProfile("Day",
            listOf(WidgetConfig("CLOCK", 0, 0), WidgetConfig("CLOCK", 1, 0)), listOf("CLOCK"))))
        SettingsBackup.decode(SettingsBackup.encode(bad))
    }
    @Test(expected = IllegalArgumentException::class)
    fun backupRejectsUnsupportedVersion() {
        SettingsBackup.decode("""{"format":"openlauncher-settings","version":99,"settings":{}}""")
    }
}
