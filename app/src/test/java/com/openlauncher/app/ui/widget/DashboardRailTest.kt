package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.Column
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
        compose.setContent { OpenLauncherTheme { Column { DashboardRailControls(true, false, true) { action = it } } } }
        compose.onNodeWithContentDescription("Dashboard controls", substring=true).performClick()
        assertEquals("controls", action)
        compose.onNodeWithContentDescription("Edit Dashboard").performClick()
        assertEquals("edit", action)
        compose.onNodeWithContentDescription("Dashboard layouts").performClick()
        assertEquals("layouts", action)
        assertEquals(SidebarPosition.TOP, SettingsBackup.decode(SettingsBackup.encode(AppSettings(sidebarPosition=SidebarPosition.TOP))).sidebarPosition)
        assertEquals(SidebarPosition.LEFT, SettingsBackup.decode(SettingsBackup.encode(AppSettings())).sidebarPosition)
    }
}
