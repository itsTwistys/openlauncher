package com.openlauncher.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class UpgradeSettingsTest {
    @Test fun reopeningRepositoryPreservesSetupAndLaterEditsDoNotResetIt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)
        val original = repo.settingsFlow.first()
        try {
            val saved = AppSettings(onboardingCompleted = true, vehicleName = "My Jeep",
                preferredMediaPackage = "com.spotify.music", navigationPackage = "com.waze",
                clockTimeZone = "America/New_York", use12HourTime = true, mapTheme = "DARK",
                onlineMapEnabled = true, mapHeadingUp = true, wallpaperUri = "content://example/wallpaper",
                appFont = AppFont.SOURCE_CODE_PRO, accentColor = 0xff33aa88.toInt(),
                shortcuts = listOf(ShortcutConfig("com.spotify.music", "Spotify")),
                homeDestination = "Saved home", workDestination = "Saved work",
                widgetLayout = listOf(WidgetConfig("NOW_PLAYING", 0, 0, 2, 2),
                    WidgetConfig("CLOCK", 2, 0), WidgetConfig("MAP", 2, 1)))
            repo.saveSettings(saved)
            val reopened = SettingsRepository(context)
            val loaded = reopened.settingsFlow.first()
            assertTrue(loaded.onboardingCompleted)
            assertEquals(saved.widgetLayout, loaded.widgetLayout)
            assertEquals(saved.shortcuts, loaded.shortcuts)
            assertEquals(saved.wallpaperUri, loaded.wallpaperUri)
            assertEquals(saved.preferredMediaPackage, loaded.preferredMediaPackage)
            assertEquals(saved.navigationPackage, loaded.navigationPackage)
            assertEquals(saved.clockTimeZone, loaded.clockTimeZone)
            assertEquals(saved.mapTheme, loaded.mapTheme)
            assertEquals(saved.appFont, loaded.appFont)
            assertEquals(saved.accentColor, loaded.accentColor)
            assertEquals(saved.homeDestination, loaded.homeDestination)
            assertTrue(loaded.onlineMapEnabled)
            reopened.updateSettings { it.copy(showClockSeconds = true) }
            val edited = reopened.settingsFlow.first()
            assertTrue(edited.onboardingCompleted)
            assertEquals(saved.shortcuts, edited.shortcuts)
            assertEquals(saved.widgetLayout, edited.widgetLayout)
            assertEquals(saved.wallpaperUri, edited.wallpaperUri)
        } finally { repo.saveSettings(original) }
    }
}
