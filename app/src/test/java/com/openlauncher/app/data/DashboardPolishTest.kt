package com.openlauncher.app.data

import android.content.Context
import android.media.Rating
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.test.core.app.ApplicationProvider
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.components.automaticShortcutIcon
import com.openlauncher.app.util.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardPolishTest {
    private val weather = WeatherState(25.0, 0, 10.0, isDay = true, timeZoneId = "America/New_York")
    @Test fun localTimezoneCorrectsTwelveHourOffsetAndHandlesNoonMidnightDst() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val evening = dashboardCalendar(Instant.parse("2026-09-29T22:38:00Z").toEpochMilli(), "AUTO", weather)
            assertEquals("6:38", clockDigits(evening, true, false)); assertEquals("PM", clockPeriod(evening))
            assertEquals(29, evening.get(java.util.Calendar.DAY_OF_MONTH))
            listOf("2026-09-29T04:00:00Z" to "AM", "2026-09-29T16:00:00Z" to "PM").forEach { (instant, period) ->
                val cal = dashboardCalendar(Instant.parse(instant).toEpochMilli(), "AUTO", weather)
                assertEquals("12:00", clockDigits(cal, true, false)); assertEquals(period, clockPeriod(cal))
            }
            val winter = dashboardCalendar(Instant.parse("2026-12-29T23:38:00Z").toEpochMilli(), "AUTO", weather)
            assertEquals("18:38", clockDigits(winter, false, false))
            assertEquals("Asia/Shanghai", dashboardTimeZone("SYSTEM", weather).id)
            assertEquals("Europe/London", dashboardTimeZone("Europe/London", weather).id)
            assertEquals("Asia/Shanghai", dashboardTimeZone("AUTO", null).id)
            assertEquals("Asia/Shanghai", TimeZone.getDefault().id)
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun timezoneSurvivesBackupAndOlderBackupsUseAuto() {
        assertEquals("America/New_York", SettingsBackup.decode(SettingsBackup.encode(AppSettings(clockTimeZone = "America/New_York"))).clockTimeZone)
        val older = com.google.gson.JsonParser.parseString(SettingsBackup.encode(AppSettings())).asJsonObject
        older.getAsJsonObject("settings").remove("clockTimeZone")
        assertEquals("AUTO", SettingsBackup.decode(older.toString()).clockTimeZone)
    }
    @Test fun gpsQualityPreventsNetworkJumpsButAcceptsRecovery() {
        val fix = LocationData(25.0, -80.0, 0.0, 8f, elapsedRealtimeMs = 100_000)
        assertFalse(acceptLocationFix(fix, 99_000, 5f))
        assertFalse(acceptLocationFix(fix, 101_000, 800f))
        assertTrue(acceptLocationFix(fix, 101_000, 10f))
        assertTrue(acceptLocationFix(fix, 130_000, 800f))
        assertFalse(acceptLocationFix(null, 100, Float.NaN))
        assertTrue(trafficMapUrl(fix, 101_000).contains("center=25.0%2C-80.0"))
        assertFalse(trafficMapUrl(fix, 131_000).contains("center="))
        assertTrue(trafficMapUrl(null, 0).contains("layer=traffic"))
    }
    @Test fun progressInterpolatesOnlyValidPlayingSessions() {
        assertEquals(12_000L, projectedPosition(10_000, 1000, 3000, 1f, true, 60_000))
        assertEquals(10_000L, projectedPosition(10_000, 1000, 3000, 1f, false, 60_000))
        assertEquals(10_000L, projectedPosition(10_000, 0, 3000, 1f, true, 60_000))
        assertEquals(60_000L, projectedPosition(59_000, 1000, 3000, 1f, true, 60_000))
    }
    @Test fun onlyCurrentlyExposedActionsAndRatingStylesAreDispatched() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = MediaSession(context, "polish-test")
        val controller = session.controller
        // Robolectric stores controller state independently from MediaSession state.
        val controllerShadow = shadowOf(controller)
        val transportShadow = shadowOf(controller.transportControls)
        controllerShadow.setRatingType(Rating.RATING_HEART)
        controllerShadow.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_SET_RATING)
            .addCustomAction("SAVE", "Save track", android.R.drawable.btn_star).build())
        try {
            assertFalse(sendExposedMediaAction(controller, "UNKNOWN"))
            assertTrue(sendExposedMediaAction(controller, "SAVE"))
            assertFalse(rateExposedMedia(controller, Rating.newStarRating(Rating.RATING_5_STARS, 5f)))
            assertTrue(rateExposedMedia(controller, Rating.newHeartRating(true)))
            assertEquals("SAVE", transportShadow.customAction)
            assertTrue(transportShadow.rating?.hasHeart() == true)
            controllerShadow.setPlaybackState(PlaybackState.Builder().setActions(0).build())
            assertFalse(sendExposedMediaAction(controller, "SAVE"))
            assertFalse(rateExposedMedia(controller, Rating.newHeartRating(true)))
        } finally { session.release() }
        assertEquals(DefaultShortcutIcon.CHROME, automaticShortcutIcon("com.android.chrome"))
        assertEquals(DefaultShortcutIcon.SPOTIFY, automaticShortcutIcon("com.spotify.music"))
        assertEquals(DefaultShortcutIcon.GOOGLE_MAPS, automaticShortcutIcon("com.google.android.apps.maps"))
        assertEquals(DefaultShortcutIcon.YOUTUBE, automaticShortcutIcon("com.google.android.youtube"))
        assertEquals(DefaultShortcutIcon.YOUTUBE_MUSIC, automaticShortcutIcon("com.google.android.apps.youtube.music"))
        assertEquals(DefaultShortcutIcon.WAZE, automaticShortcutIcon("com.waze"))
        val original = AppSettings(shortcuts = listOf(ShortcutConfig("com.google.android.youtube", "YouTube", customIconOverride = DefaultShortcutIcon.YOUTUBE)))
        assertEquals(original.shortcuts, SettingsBackup.decode(SettingsBackup.encode(original)).shortcuts)
        assertNull(automaticShortcutIcon("other.installed.app"))
    }
}
