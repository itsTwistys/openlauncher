package com.openlauncher.app.ui

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.openlauncher.app.data.*
import com.openlauncher.app.ui.screen.DashboardEditor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h600dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelDiscardsDraftAndEditorFitsLandscape() {
        var applied: LayoutProfile? = null
        var dismissed = false
        compose.setContent { MaterialTheme(colorScheme = darkColorScheme()) {
            DashboardEditor(AppSettings(), { applied = it }, { dismissed = true })
        } }
        compose.onNodeWithText("Apply").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertNull(applied); assertTrue(dismissed) }
    }

    @Test fun resizingAppliesValidLayoutAndCapturesEditor() {
        var applied: LayoutProfile? = null
        compose.setContent { MaterialTheme(colorScheme = darkColorScheme()) {
            DashboardEditor(AppSettings(), { applied = it }, {})
        } }
        // Clock is the initial selection. Select the Map from the visible picker.
        compose.onAllNodesWithText("MAP").onLast().performClick()
        compose.onNodeWithText("2 × 1").performClick()
        val image = compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        val file = File("build/outputs/ui-checks/dashboard-editor-landscape.png")
        file.parentFile.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            val saved = requireNotNull(applied)
            assertTrue(validWidgetLayout(saved.layout))
            assertEquals(1, saved.layout.first { it.id == "MAP" }.spanY)
        }
    }

    @Test @Config(qualifiers = "w600dp-h360dp-land-mdpi")
    fun controlsRemainVisibleOnSmallHeadUnit() {
        compose.setContent { MaterialTheme(colorScheme = darkColorScheme()) {
            DashboardEditor(AppSettings(), {}, {})
        } }
        compose.onNodeWithText("Apply").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Recovery").assertIsDisplayed().performClick()
        compose.onNodeWithText("Previous layouts appear here after your next change.").assertExists()
    }
}
