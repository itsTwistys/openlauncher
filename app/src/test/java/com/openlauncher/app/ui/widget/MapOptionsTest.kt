package com.openlauncher.app.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import com.openlauncher.app.data.AppFont
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
class MapOptionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun optionsScrollAndSettingsRemainIndependentOnSmallHeadUnit() {
        var zoom by mutableStateOf(true)
        var heading by mutableStateOf(false)
        var compatibility by mutableStateOf(true)
        var reloaded = false
        var dismissed = false
        compose.setContent { OpenLauncherTheme(appFont = AppFont.SYSTEM, textScale = 1.4f) {
            MapOptionsDialog(zoom, heading, compatibility, "GPS ±10 m", "Map ready",
                { z, h -> zoom = z; heading = h }, { compatibility = it }, { reloaded = true }, { dismissed = true })
        } }
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNodeWithContentDescription("Auto zoom").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Heading up").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Compatibility rendering").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(zoom); assertTrue(heading); assertFalse(compatibility) }
        compose.onNodeWithText("Reload map").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(reloaded) }
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle {
            assertTrue(dismissed)
            val view = requireNotNull(ShadowDialog.getLatestDialog().window).decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val file = File("build/outputs/ui-checks/map-options-small-landscape.png")
            file.parentFile.mkdirs()
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
