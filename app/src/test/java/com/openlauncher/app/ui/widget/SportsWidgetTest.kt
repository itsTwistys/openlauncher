package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.openlauncher.app.data.*
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w600dp-h360dp-land-mdpi")
class SportsWidgetTest {
    @get:Rule val rule = createComposeRule()
    @Test fun savedLiveScoresAreExplicitlyOfflineAndNeverClaimCurrentLive() {
        val cache = File(RuntimeEnvironment.getApplication().filesDir, "sports-cache").apply { mkdirs() }
        File(cache, "NFL-LIVE.json").writeText(Gson().toJson(SportsSnapshot(SportsLeague.NFL, SportsView.LIVE,
            listOf(SportsEvent("fixture", "Miami at Buffalo", System.currentTimeMillis(), "in", "Q4 2:10",
                listOf(SportsCompetitor("Miami Dolphins", "MIA", "0"), SportsCompetitor("Buffalo Bills", "BUF", "21")))),
            System.currentTimeMillis(), "ESPN")))
        rule.setContent {
            OpenLauncherTheme {
                SportsWidget(AppSettings(), null, false, false, {}, Modifier.size(280.dp, 300.dp))
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("MIA").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Offline · saved scores").assertExists()
        rule.onNodeWithText("Saved · Q4 2:10").assertExists()
        rule.onNodeWithText("0").assertExists()
        rule.onNodeWithText("LIVE · Q4 2:10").assertDoesNotExist()
        rule.onNodeWithText("Results").assertExists()
        rule.onNodeWithText("Upcoming").assertExists()
    }
}
