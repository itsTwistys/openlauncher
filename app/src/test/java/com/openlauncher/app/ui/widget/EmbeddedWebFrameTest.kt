package com.openlauncher.app.ui.widget

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h600dp-land-mdpi")
class EmbeddedWebFrameTest {
    @get:Rule val compose = createComposeRule()
    private class TrackedWebView(context: Context) : WebView(context) {
        var destructions = 0
        override fun destroy() { destructions++; super.destroy() }
    }

    @Test fun resizingKeepsWebViewAndReloadReleasesOldInstanceExactlyOnce() {
        var expanded by mutableStateOf(false)
        var generation by mutableIntStateOf(0)
        var visible by mutableStateOf(true)
        val created = mutableListOf<TrackedWebView>()
        var releases = 0
        compose.setContent {
            val context = LocalContext.current
            if (visible) Box(Modifier.size(if (expanded) 800.dp else 400.dp, 240.dp)) {
                key(generation) {
                    EmbeddedWebFrame(Modifier.fillMaxSize(), create = {
                        TrackedWebView(context).also { created.add(it) }
                    }, onRelease = { releases++ })
                }
            }
        }
        compose.runOnIdle { assertEquals(400, created.single().width); expanded = true }
        compose.runOnIdle {
            assertEquals(800, created.single().width)
            assertEquals(0, releases)
            generation++
        }
        compose.runOnIdle {
            assertEquals(2, created.size)
            assertEquals(1, created.first().destructions)
            assertEquals(1, releases)
            assertEquals(800, created.last().width)
            visible = false
        }
        compose.runOnIdle {
            assertEquals(2, releases)
            assertEquals(1, created.last().destructions)
        }
    }
}
