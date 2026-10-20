package com.openlauncher.app.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.openlauncher.app.service.MediaListenerService.NavigationInfo
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h600dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DirectionsWidgetTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val maps = NavigationInfo("com.google.android.apps.maps", "300 ft", "Turn right onto Main St", null,
        "9 min · 1.9 mi · 6:47 PM", followingSteps = listOf("Turn left onto Oak Ave", "Continue straight for 1 mi"))

    private fun screenshot(name: String) = compose.runOnIdle {
        val view = compose.activity.window.decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        val file = File("build/outputs/ui-checks/$name.png")
        file.parentFile.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun bigArrowCurrentTurnFollowingStepsAndExpandRemainVisible() {
        var info by mutableStateOf(maps)
        var expanded = false
        var opened = false
        compose.setContent { OpenLauncherTheme(textScale = 1.2f) { Surface(Modifier.size(580.dp, 500.dp)) {
            DirectionsCard(info, true, onExpand = { expanded = true }, onOpen = { opened = true }, modifier = Modifier.fillMaxSize())
        } } }
        compose.onNodeWithText("300 ft").assertIsDisplayed()
        compose.onNodeWithText("Turn right onto Main St").assertIsDisplayed()
        compose.onNodeWithText("Turn left onto Oak Ave").assertIsDisplayed()
        compose.onNodeWithContentDescription("RIGHT").assertIsDisplayed()
        assertTrue(compose.onNodeWithContentDescription("RIGHT").fetchSemanticsNode().boundsInRoot.width >= 120f)
        screenshot("directions-wide-dark")
        compose.onNodeWithContentDescription("Expand DIRECTIONS").performClick()
        compose.onNodeWithContentDescription("Open navigation").performClick()
        compose.runOnIdle { assertTrue(expanded); assertTrue(opened); info = maps.copy(title = "100 ft", details = "Turn left onto Oak Ave", followingSteps = emptyList()) }
        compose.onNodeWithText("100 ft").assertIsDisplayed()
        compose.onNodeWithContentDescription("LEFT").assertIsDisplayed()
        compose.onNodeWithText("Continue straight for 1 mi").assertDoesNotExist()
    }

    @Test fun compactLightCardKeepsNextTurnVisibleAtLargerTextAndStepsCanScroll() {
        compose.setContent { OpenLauncherTheme(textScale = 1.4f, isDayMode = true) { Surface(Modifier.size(320.dp, 230.dp)) {
            DirectionsCard(maps.copy(packageName = "com.waze"), true, onExpand = {}, modifier = Modifier.fillMaxSize())
        } } }
        compose.onNodeWithText("Waze").assertIsDisplayed()
        compose.onNodeWithText("300 ft").assertIsDisplayed()
        compose.onNodeWithText("Turn right onto Main St").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand DIRECTIONS").assertIsDisplayed()
        screenshot("directions-compact-light")
        compose.onNodeWithText("Continue straight for 1 mi").performScrollTo().assertIsDisplayed()
    }

    @Test fun smallestCellUsesFullWidthForInstructionInsteadOfSqueezingBesideArrow() {
        compose.setContent { OpenLauncherTheme(textScale = 1.4f) { Surface(Modifier.size(177.dp, 171.dp)) {
            DirectionsCard(maps, true, onExpand = {}, modifier = Modifier.fillMaxSize())
        } } }
        compose.onNodeWithText("300 ft").assertIsDisplayed()
        val instruction = compose.onNodeWithText("Turn right onto Main St")
        instruction.assertIsDisplayed()
        assertTrue(instruction.fetchSemanticsNode().boundsInRoot.width > 140f)
        compose.onNodeWithContentDescription("Expand DIRECTIONS").assertIsDisplayed()
        screenshot("directions-smallest-cell")
    }

    @Test fun providerSymbolsGetContrastingBackgroundsInEitherTheme() {
        val white = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            val arrow = android.graphics.Path().apply {
                moveTo(3f, 8f); lineTo(13f, 8f); lineTo(13f, 3f); lineTo(22f, 12f)
                lineTo(13f, 21f); lineTo(13f, 16f); lineTo(3f, 16f); close()
            }
            Canvas(this).drawPath(arrow, android.graphics.Paint().apply { color = android.graphics.Color.WHITE })
        }
        val black = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
        assertEquals(androidx.compose.ui.graphics.Color(0xFF242830), symbolBackground(white))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFE7EBEF), symbolBackground(black))
        compose.setContent { OpenLauncherTheme { Surface(Modifier.size(320.dp, 480.dp)) {
            DirectionsCard(maps.copy(maneuverIcon = white), true, modifier = Modifier.fillMaxSize())
        } } }
        compose.onNodeWithContentDescription("Navigation symbol supplied by navigation app").assertIsDisplayed()
        screenshot("directions-provider-symbol")
    }

    @Test fun routeEndAndDisconnectionRemoveOldCuesAndExplainSetup() {
        var info by mutableStateOf<NavigationInfo?>(maps)
        var connected by mutableStateOf(true)
        compose.setContent { OpenLauncherTheme { Surface(Modifier.size(320.dp, 480.dp)) {
            DirectionsCard(info, connected, preferredPackage = "com.waze", modifier = Modifier.fillMaxSize())
        } } }
        compose.runOnIdle { connected = false }
        compose.onNodeWithText("300 ft").assertDoesNotExist()
        compose.onNodeWithText("Directions disconnected").assertIsDisplayed()
        compose.onNodeWithContentDescription("Notification access").assertIsDisplayed()
        compose.runOnIdle { connected = true; info = null }
        compose.onNodeWithText("No active route").assertIsDisplayed()
        compose.onNodeWithText("Start navigation in Waze on this device, then return Home.").assertIsDisplayed()
        compose.runOnIdle { info = maps.copy(details = "Left Bank Road", maneuverIcon = null, followingSteps = emptyList()) }
        compose.onNodeWithContentDescription("Maneuver unavailable").assertIsDisplayed()
        compose.onNodeWithText("Following steps are not shared by this app yet.").performScrollTo().assertIsDisplayed()
    }
}
