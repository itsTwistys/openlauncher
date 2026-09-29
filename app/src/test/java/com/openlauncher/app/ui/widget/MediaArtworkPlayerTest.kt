package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h600dp-land-mdpi")
class MediaArtworkPlayerTest {
    @get:Rule val compose = createComposeRule()
    private val state = NowPlayingState("A long track title with enough words to wrap", "Artist name", null, isPlaying = true, controller = null)
    @Test fun compactCardKeepsTitleAndTransportVisible() {
        compose.setContent { OpenLauncherTheme { Box(Modifier.size(280.dp, 200.dp)) {
            MediaArtworkPlayer(state, Color.White, false, true, {}, {}, {}, {})
        } } }
        compose.onNodeWithText(state.title).assertIsDisplayed()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next track").assertIsDisplayed()
        compose.onNodeWithText(" Open player").assertDoesNotExist()
    }
    @Test fun tallCardUsesFullHeightAndShowsAdditionalControls() {
        compose.setContent { OpenLauncherTheme { Box(Modifier.size(320.dp, 480.dp)) {
            MediaArtworkPlayer(state, Color.White, false, true, {}, {}, {}, {})
        } } }
        compose.onNodeWithText(state.title).assertIsDisplayed()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
        compose.onNodeWithText(" Open player").assertIsDisplayed()
    }
}
