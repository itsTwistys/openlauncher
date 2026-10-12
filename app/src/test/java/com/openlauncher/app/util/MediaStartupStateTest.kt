package com.openlauncher.app.util

import android.media.Rating
import android.os.Bundle
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.SettingsBackup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaStartupStateTest {
    @Test fun appStateWinsAndUnknownActionsStayUnknown() {
        assertFalse(mediaActionActive("Like", "heart_filled", rating = Rating.newHeartRating(false))!!)
        assertTrue(mediaActionActive("Like", "heart_outline", rating = Rating.newHeartRating(true))!!)
        assertTrue(mediaActionActive("Like", extras = Bundle().apply { putBoolean("selected", true) })!!)
        assertFalse(mediaActionActive("Save", "save_inactive")!!)
        assertTrue(mediaActionActive("Remove from library")!!)
        assertNull(mediaActionActive("Shuffle"))
        assertNull(mediaActionActive("Open queue", extras = Bundle().apply { putBoolean("selected", true) }))
        assertFalse(ratingActive(Rating.newThumbRating(false), Rating.RATING_THUMB_UP_DOWN)!!)
        assertNull(ratingActive(null, Rating.RATING_HEART))
    }
    @Test fun startupNeverCreatesAReturnToHomeLoopAndLongSleepRearmsIt() {
        val gate = MediaStartupGate()
        assertTrue(gate.take(0))
        gate.stopped(1000)
        assertFalse(gate.take(2000))
        assertFalse(gate.take(3000))
        gate.stopped(4000)
        assertTrue(gate.take(65000))
        assertFalse(gate.take(65001))
        assertEquals("com.spotify.music", musicStartupPackage("", "com.spotify.music"))
        assertEquals("com.example.player", musicStartupPackage("com.example.player", "com.spotify.music"))
        assertEquals("", musicStartupPackage("@radio", "com.spotify.music"))
    }
    @Test fun savedStartupPreferencesPreserveExistingSetup() {
        val original = AppSettings(lastMediaPackage = "com.spotify.music", autoLaunchMedia = false)
        val decoded = SettingsBackup.decode(SettingsBackup.encode(original))
        assertEquals(original.lastMediaPackage, decoded.lastMediaPackage)
        assertFalse(decoded.autoLaunchMedia)
        assertEquals(original.shortcuts, decoded.shortcuts)
        assertEquals(original.widgetLayout, decoded.widgetLayout)
    }
}
