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
    @Test fun changingTurnDoesNotResizeBannerAndTripRemainsVisible() {
        var info by mutableStateOf(NavigationInfo("com.waze", "300 ft", "Turn right", null, "9 min · 1.9 mi"))
        compose.setContent { OpenLauncherTheme(textScale = 1.2f) { Box(Modifier.width(320.dp)) {
            NavigationBanner(info, false, {})
        } } }
        val before = compose.onNodeWithContentDescription("Navigation directions").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithText("Waze directions").assertIsDisplayed()
        compose.onNodeWithText("9 min · 1.9 mi").assertIsDisplayed()
        compose.runOnIdle { info = info.copy(details="Turn right onto a much longer street name toward the city center") }
        compose.onNodeWithText("9 min · 1.9 mi").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithContentDescription("Navigation directions").fetchSemanticsNode().boundsInRoot.height)
    }
}
