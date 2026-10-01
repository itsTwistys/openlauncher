package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openlauncher.app.ui.components.Sidebar
import com.openlauncher.app.model.NavDestination
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.openlauncher.app.ui.components.DashboardRailControls
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import com.openlauncher.app.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w800dp-h480dp-land-mdpi")
class DashboardRailTest {
    @get:Rule val compose = createComposeRule()
    @Test fun relocatedActionsRemainAccessibleAndTopPositionRoundTrips() {
        var action = ""
        compose.setContent { OpenLauncherTheme { Column {
            DashboardRailControls(true, false, true, { action = it }, internetValidated = false)
        } } }
        compose.onNodeWithText("Edit Dashboard").assertDoesNotExist()
        compose.onNodeWithContentDescription("Dashboard controls").performClick()
        compose.onNodeWithText("Wi-Fi connected").assertIsDisplayed()
        compose.onNodeWithText("No verified internet").assertIsDisplayed()
        compose.onNodeWithText("Wi-Fi settings").assertIsDisplayed()
        compose.onNodeWithText("Edit Dashboard").performClick()
        assertEquals("edit", action)
        compose.onNodeWithText("Wi-Fi settings").assertDoesNotExist()
        compose.onNodeWithContentDescription("Dashboard controls").performClick()
        compose.onNodeWithText("Dashboard tools").performClick()
        assertEquals("controls", action)
        compose.onNodeWithContentDescription("Dashboard controls").performClick()
        compose.onNodeWithText("Saved layouts").performClick()
        assertEquals("layouts", action)
        assertEquals(SidebarPosition.TOP, SettingsBackup.decode(SettingsBackup.encode(AppSettings(sidebarPosition=SidebarPosition.TOP))).sidebarPosition)
        assertEquals(SidebarPosition.LEFT, SettingsBackup.decode(SettingsBackup.encode(AppSettings())).sidebarPosition)
    }
    @Test fun horizontalRailKeepsShortcutsOnTheSavedSide() {
        var right by mutableStateOf(true)
        compose.setContent { OpenLauncherTheme { Box(Modifier.size(800.dp, 56.dp)) {
            Sidebar(NavDestination.HOME,
                AppSettings(bottomBarShortcutsRight = right, shortcuts = listOf(ShortcutConfig("com.spotify.music", "Spotify"))),
                installedIconFor = { null }, onNavigate = {}, onShortcutClick = {}, onShortcutLongPress = {},
                onShortcutRemove = {}, onShortcutSetIcon = { _, _ -> }, onReorder = { _, _ -> }, isHorizontal = true)
        } } }
        fun x(label: String) = compose.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot.left
        assertTrue(x("Home") < x("Spotify"))
        compose.runOnIdle { right = false }
        assertTrue(x("Spotify") < x("Home"))
    }

}
