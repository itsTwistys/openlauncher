package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.openlauncher.app.service.MediaListenerService.NavigationInfo
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w800dp-h480dp-land-mdpi")
class NavigationBannerTest {
    @get:Rule val compose = createComposeRule()
    @Test fun compactTurnRemainsStableAndOpensDetailsOnTap() {
        var info by mutableStateOf(NavigationInfo("com.waze", "300 ft", "Turn right", null, "9 min · 1.9 mi"))
        var tapped = false
        compose.setContent { OpenLauncherTheme(textScale = 1.2f) { Box(Modifier.width(320.dp)) {
            NavigationBanner(info, false, { tapped = true })
        } } }
        val banner = compose.onNodeWithContentDescription("Navigation directions")
        val before = banner.fetchSemanticsNode().boundsInRoot.height
        assertTrue("The turn strip uses less height than the former 132dp banner", before < 108f)
        compose.onNodeWithText("300 ft").assertIsDisplayed()
        compose.onNodeWithContentDescription("RIGHT", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("9 min · 1.9 mi").assertDoesNotExist()
        compose.runOnIdle { info = info.copy(details="Turn left onto a much longer street name toward the city center") }
        compose.onNodeWithContentDescription("LEFT", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(before, banner.fetchSemanticsNode().boundsInRoot.height)
        banner.performClick()
        assertTrue(tapped)
        compose.runOnIdle { info = info.copy(details="SW 57th Ave / Coral Gables Blvd") }
        compose.onNodeWithContentDescription("Maneuver unavailable", useUnmergedTree = true).assertIsDisplayed()
    }
}
