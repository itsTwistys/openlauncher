package com.openlauncher.app.data

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.openlauncher.app.service.*
import com.openlauncher.app.service.MediaListenerService.NavigationInfo
import com.openlauncher.app.ui.widget.selectDirections
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DirectionsTest {
    private fun notification(title: String = "300 ft", body: String = "Turn right onto Main St", big: String = "", lines: Array<CharSequence> = emptyArray()) =
        Notification().apply { extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, title)
            putCharSequence(Notification.EXTRA_TEXT, body)
            putCharSequence(Notification.EXTRA_BIG_TEXT, big)
            putCharSequenceArray(Notification.EXTRA_TEXT_LINES, lines)
            putCharSequence(Notification.EXTRA_SUB_TEXT, "9 min · 1.9 mi · 6:47 PM")
        } }

    @Test fun explicitlySharedStepsStaySeparateFromCurrentTurnAndTripSummary() {
        val parsed = navigationText(notification(big = "Turn right onto Main St\nThen turn left onto Oak Ave\nAfter that, continue straight\nETA: 6:47 PM"))
        assertEquals("Turn right onto Main St", parsed.instruction)
        assertEquals(listOf("turn left onto Oak Ave", "continue straight"), parsed.followingSteps)
        assertEquals("9 min · 1.9 mi · 6:47 PM", parsed.trip)
        assertEquals(TurnManeuver.RIGHT, turnCue(parsed.title, parsed.instruction).maneuver)
    }

    @Test fun inlineThenAndNumberedInboxStepsAreSupportedAndDeduplicated() {
        val parsed = navigationText(notification(body = "Turn right onto Main St, then turn left onto Oak Ave",
            lines = arrayOf("1. Turn right onto Main St", "2. turn left onto Oak Ave", "3. Continue straight")))
        assertEquals("Turn right onto Main St", parsed.instruction)
        assertEquals(listOf("turn left onto Oak Ave", "Continue straight"), parsed.followingSteps)
    }

    @Test fun ordinaryProviderNotificationsDoNotPromiseFollowingSteps() {
        val parsed = navigationText(notification(big = "Turn right onto Main St\n300 ft\n9 min · 1.9 mi · 6:47 PM"))
        assertEquals("Turn right onto Main St", parsed.instruction)
        assertTrue(parsed.followingSteps.isEmpty())
        val localized = navigationText(notification(body = "Gira a la derecha"))
        assertEquals("Turn right onto Then Street", navigationText(notification(body = "Turn right onto Then Street")).instruction)
        assertTrue(navigationText(notification(body = "Turn right onto Then Street")).followingSteps.isEmpty())
        assertEquals("Gira a la derecha", localized.instruction)
        assertEquals(TurnManeuver.UNKNOWN, turnCue(localized.title, localized.instruction).maneuver)
    }

    @Test fun noRouteHistoryOrUnorderedTextIsConvertedIntoFutureSteps() {
        val parsed = navigationText(notification(lines = arrayOf("Turn left onto another road", "15 min remaining")))
        assertTrue(parsed.followingSteps.isEmpty())
        val updated = navigationText(notification(body = "Keep right toward Downtown"))
        assertTrue(updated.followingSteps.isEmpty())
        assertEquals(TurnManeuver.SLIGHT_RIGHT, turnCue(updated.title, updated.instruction).maneuver)
        assertEquals(TurnManeuver.RIGHT, turnCue("300 ft", "Turn right · Then turn left").maneuver)
        assertTrue(navigationText(Notification().apply { extras = Bundle() }).followingSteps.isEmpty())
    }

    @Test fun providerSelectionDoesNotSilentlyUseTheOtherAppAndClearsWhenRemoved() {
        val maps = NavigationInfo("com.google.android.apps.maps", "300 ft", "Turn right", null)
        val waze = maps.copy(packageName = "com.waze", title = "1 mi")
        assertEquals(waze, selectDirections(listOf(waze, maps), ""))
        assertEquals(maps, selectDirections(listOf(waze, maps), maps.packageName))
        assertNull(selectDirections(listOf(maps), waze.packageName))
        assertNull(selectDirections(emptyList(), ""))
    }

    @Test fun directionsCanBeAddedResizedBackedUpAndRemovedWithoutChangingDefaults() {
        val defaults = AppSettings(onboardingCompleted = true, navigationPackage = "com.waze", vehicleName = "Jeep")
        assertFalse(defaults.showDirections)
        val added = defaults.withAddedWidget("DIRECTIONS")
        assertTrue(added.showDirections)
        assertTrue(validWidgetLayout(added.widgetLayout))
        val custom = added.copy(widgetLayout = listOf(WidgetConfig("DIRECTIONS", 0, 0, 2, 2), WidgetConfig("CLOCK", 2, 0)))
            .withWidgetVisibility(setOf("DIRECTIONS", "CLOCK"))
        val restored = SettingsBackup.decode(SettingsBackup.encode(custom))
        assertEquals(custom.widgetLayout, restored.widgetLayout)
        assertTrue(restored.showDirections)
        assertEquals("com.waze", restored.navigationPackage)
        assertTrue(restored.onboardingCompleted)
        assertFalse(restored.withWidgetVisibility(setOf("CLOCK")).showDirections)
        assertFalse(restored.withDefaultDashboard().showDirections)
    }

    @Test fun repositoryRetainsDirectionsAndExistingSetupOnReopen() = runBlocking {
        val repo = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
        val original = repo.settingsFlow.first()
        try {
            val settings = AppSettings(onboardingCompleted = true, preferredMediaPackage = "com.spotify.music", navigationPackage = "com.waze")
                .withAddedWidget("DIRECTIONS")
            repo.saveSettings(settings)
            val loaded = SettingsRepository(ApplicationProvider.getApplicationContext<Context>()).settingsFlow.first()
            assertTrue(loaded.showDirections)
            assertEquals(settings.widgetLayout, loaded.widgetLayout)
            assertEquals(settings.preferredMediaPackage, loaded.preferredMediaPackage)
            assertTrue(loaded.onboardingCompleted)
        } finally { repo.saveSettings(original) }
    }
}
