package com.openlauncher.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.openlauncher.app.data.*
import com.openlauncher.app.ui.components.ColorPickerDialog
import com.openlauncher.app.ui.components.ConfirmDialog
import com.openlauncher.app.ui.screen.SettingsScreen
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w600dp-h360dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun displayControlsWrapAndChangesPreserveDashboardAndConnectedSettings() {
        val original = AppSettings(onlineMapEnabled = true, preferredMediaPackage = "com.spotify.music",
            homeDestination = "Saved home", showSports = true)
        var settings by mutableStateOf(original)
        compose.setContent { OpenLauncherTheme(accent = Color(0xFF2979FF), appFont = AppFont.SYSTEM, textScale = 1.4f) {
            SettingsScreen(settings, Color(0xFF2979FF), { settings = it(settings) }, {})
        } }
        compose.onNodeWithText("Display").performClick()
        compose.onNodeWithText("Light").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(DayNightMode.LIGHT, settings.dayNightMode)
            assertEquals(original.widgetLayout, settings.widgetLayout)
            assertEquals(original.shortcuts, settings.shortcuts)
            assertEquals(original.preferredMediaPackage, settings.preferredMediaPackage)
            assertEquals(original.homeDestination, settings.homeDestination)
            assertTrue(settings.onlineMapEnabled)
            assertTrue(settings.showSports)
        }
        compose.onNodeWithText("Connected").assertIsDisplayed().performClick()
        compose.onNodeWithText("Show Embedded Map").assertIsDisplayed()
        compose.onNodeWithText("Appearance").assertDoesNotExist()
    }

    @Test fun lightColorPickerScrollsAndKeepsActionsVisible() {
        var selected: Color? = null
        var dismissed = false
        compose.setContent { OpenLauncherTheme(isDayMode = true, appFont = AppFont.SYSTEM, textScale = 1.4f) {
            ColorPickerDialog("Accent color", Color(0xFF2979FF), { selected = it }, { dismissed = true })
        } }
        compose.onNodeWithText("Apply").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Brightness").performScrollTo().assertIsDisplayed()
        capture("settings-color-light-small.png")
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { assertEquals(Color(0xFF2979FF), selected); assertTrue(dismissed) }
    }

    @Test fun darkConfirmationUsesThemeAndCancelDoesNotReset() {
        var reset = false
        var dismissed = false
        compose.setContent { OpenLauncherTheme(appFont = AppFont.SOURCE_CODE_PRO, textScale = 1.4f) {
            ConfirmDialog("Reset settings", "Your saved layout and preferences will be replaced.", "Reset",
                { reset = true }, { dismissed = true })
        } }
        compose.onNodeWithText("Reset").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        capture("settings-confirm-dark-small.png")
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(reset); assertTrue(dismissed) }
    }

    private fun capture(name: String) = compose.runOnIdle {
        val view = requireNotNull(ShadowDialog.getLatestDialog().window).decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        val file = File("build/outputs/ui-checks/$name")
        file.parentFile.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
