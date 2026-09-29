package com.openlauncher.app.data

import com.openlauncher.app.util.SessionSnapshotCache
import org.junit.Assert.*
import org.junit.Test

class DashboardCompletionTest {
    @Test fun recoveryIsBoundedAndPreservesAppearance() {
        var s = AppSettings(vehicleName = "My vehicle", weatherBackground = true, launcherBrightness = 0.6f)
        val layouts = (1..3).flatMap { w -> (1..2).flatMap { h ->
            (0..3-w).flatMap { x -> (0..2-h).map { y -> WidgetConfig("MAP", x, y, w, h) } }
        } }
        layouts.forEachIndexed { i, widget ->
            val before = s
            s = s.copy(widgetLayout = listOf(widget)).withWidgetVisibility(setOf("MAP"))
                .rememberLayoutBefore(before, i.toLong())
        }
        assertEquals(8, s.layoutHistory.size)
        assertEquals("My vehicle", s.vehicleName)
        assertTrue(s.weatherBackground)
        val noChange = s.rememberLayoutBefore(s, 999)
        assertEquals(s.layoutHistory, noChange.layoutHistory)
    }
    @Test fun addingToFullDefaultOnlyChangesDraftAndProducesNoOverlap() {
        val original = AppSettings()
        val draft = original.withAddedWidget("TRIP_TRACKER")
        assertFalse(original.showTripTracker)
        assertTrue(draft.showTripTracker)
        assertTrue(validWidgetLayout(draft.widgetLayout))
        assertEquals(4, draft.widgetLayout.size)
    }
    @Test fun emptyLayoutCanBeRecovered() {
        val empty = AppSettings().copy(widgetLayout = emptyList()).withWidgetVisibility(emptySet())
        val s = empty.withDefaultDashboard().rememberLayoutBefore(empty, 1)
        assertTrue(s.layoutHistory.first().layout.isEmpty())
    }
    @Test fun tripRejectsSleepStaleAndInaccurateFixes() {
        val trip = TripRecord(startedAtMs = 1, running = true)
        assertEquals(trip, advanceTrip(trip, 20f, 30_000, 5f, 1.0))
        assertEquals(trip, advanceTrip(trip, 20f, 0, 100f, 1.0))
        assertEquals(trip, advanceTrip(trip, 20f, 0, 5f, 60.0))
        assertEquals(trip, advanceTrip(trip, Float.NaN, 0, 5f, 1.0))
        val moving = advanceTrip(trip, 20f, 0, 5f, 2.0)
        assertEquals(40.0, moving.distanceMeters, 0.001)
        assertEquals(20.0, moving.averageMps, 0.001)
        val log = completeTrip(TripLog(moving), 3000)
        assertEquals(1, log.history.size)
        assertFalse(log.history.first().running)
        assertEquals(TripRecord(), log.current)
        assertTrue(tripsCsv(log.history).contains("40.0,2.0,0.0,20.0"))
    }
    @Test fun updateComparisonUsesNumbersAndRequiresAssets() {
        assertTrue(newerVersion("Open Launcher 0.0.12-preview", "0.0.9-preview"))
        assertFalse(newerVersion("0.0.12-preview", "0.0.12-preview"))
        assertFalse(newerVersion("preview-abcd", "0.0.12"))
        val json = """[{"name":"0.0.13-preview","draft":false,"html_url":"https://github.com/itsTwistys/openlauncher/releases/tag/preview-test","assets":[{"name":"openlauncher-preview.apk","browser_download_url":"https://github.com/itsTwistys/openlauncher/releases/download/preview-test/openlauncher-preview.apk"},{"name":"SHA256SUMS.txt"}]}]"""
        assertEquals(1, parseReleases(json).size)
        assertTrue(parseReleases(json.replace("SHA256SUMS.txt", "other.txt")).isEmpty())
        assertTrue(parseReleases(json.replace("https://github.com/", "https://example.com/")).isEmpty())
    }
    @Test fun sessionSnapshotsDeduplicatePerTokenAndInvalidateArtwork() {
        val cache = SessionSnapshotCache<String, String, Any>()
        val a = cache.value("session-a", "title|uri|playing|art") { Any() }
        assertSame(a, cache.value("session-a", "title|uri|playing|art") { Any() })
        assertNotSame(a, cache.value("session-b", "title|uri|playing|art") { Any() })
        assertNotSame(a, cache.value("session-a", "title|uri|paused|art") { Any() })
        val changed = cache.value("session-a", "title|uri|playing|art", force = true) { Any() }
        assertNotSame(a, changed)
        cache.retain(emptySet())
        assertNotSame(changed, cache.value("session-a", "title|uri|playing|art") { Any() })
    }
    @Test fun backupRoundTripKeepsNewPreferences() {
        val s = AppSettings(weatherBackground = true, launcherBrightness = 0.7f,
            layoutHistory = listOf(LayoutProfile("1", defaultWidgetLayout(), listOf("MAP", "CLOCK", "NOW_PLAYING"))))
        val decoded = SettingsBackup.decode(SettingsBackup.encode(s))
        assertTrue(decoded.weatherBackground)
        assertEquals(0.7f, decoded.launcherBrightness)
        assertEquals(s.layoutHistory, decoded.layoutHistory)
    }
}
