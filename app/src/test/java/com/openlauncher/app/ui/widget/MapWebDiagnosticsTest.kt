package com.openlauncher.app.ui.widget

import android.net.Uri
import android.webkit.*
import com.openlauncher.app.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MapWebDiagnosticsTest {
    private fun request(url: String, main: Boolean = false) = object : WebResourceRequest {
        override fun getUrl() = Uri.parse(url)
        override fun isForMainFrame() = main
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders() = emptyMap<String, String>()
    }
    @Test fun callbacksKeepCodesAndCategoriesWithoutUrlsOrMessages() {
        val d = MapDiagnostics { 123 }
        val web = WebView(RuntimeEnvironment.getApplication())
        var active = true
        var pageFailures = 0
        val client = MapWebDiagnostics(d, { active && it === web }, { pageFailures++ })
        val privateUrl = "https://tile.openstreetmap.org/17/123/456.png?lat=25.761&token=secret"
        val tile = request(privateUrl)
        val http = WebResourceResponse("image/png", null, 403, "Forbidden", emptyMap(), null)
        client.onReceivedHttpError(web, tile, http)
        assertEquals(MapFailureEvidence(MapFailure.HTTP, MapResource.TILE, 403, 123), d.map.value.lastFailure)
        assertEquals(0, pageFailures)
        val error = TestWebResourceError(WebViewClient.ERROR_HOST_LOOKUP, privateUrl)
        client.onReceivedError(web, request("https://appassets.androidplatform.net/assets/map/map.js"), error)
        assertEquals(MapResource.LOCAL_ASSET, d.map.value.lastFailure?.resource)
        assertEquals(WebViewClient.ERROR_HOST_LOOKUP, d.map.value.lastFailure?.code)
        client.onReceivedError(web, request(privateUrl, true), error)
        assertEquals(1, pageFailures)
        assertEquals(MapResource.PAGE, d.map.value.lastFailure?.resource)
        val chrome = MapChromeDiagnostics(d) { active }
        assertTrue(chrome.onConsoleMessage(ConsoleMessage(privateUrl, privateUrl, 1, ConsoleMessage.MessageLevel.ERROR)))
        chrome.onConsoleMessage(ConsoleMessage(privateUrl, privateUrl, 2, ConsoleMessage.MessageLevel.WARNING))
        chrome.onConsoleMessage(ConsoleMessage(privateUrl, privateUrl, 3, ConsoleMessage.MessageLevel.LOG))
        assertEquals(1, d.map.value.failures[MapFailure.CONSOLE])
        assertEquals(1, d.map.value.consoleWarnings)
        assertEquals(403, d.map.value.lastByKind[MapFailure.HTTP]?.code)
        assertEquals(MapResource.TILE, d.map.value.lastByKind[MapFailure.HTTP]?.resource)
        val before = d.map.value
        active = false
        client.onReceivedHttpError(web, tile, http)
        client.onReceivedError(web, tile, error)
        chrome.onConsoleMessage(ConsoleMessage(privateUrl, privateUrl, 4, ConsoleMessage.MessageLevel.ERROR))
        assertEquals(before, d.map.value)
        val export = d.map.value.toString() + d.map.value.evidenceLines().joinToString()
        listOf("https", "17/123/456", "25.761", "secret").forEach { assertFalse(export.contains(it)) }
        web.destroy()
    }
    @Test fun classificationUsesExactHosts() {
        assertEquals(MapResource.TILE, mapResource(Uri.parse("https://tile.openstreetmap.org/1/2/3.png")))
        assertEquals(MapResource.OTHER, mapResource(Uri.parse("https://tile.openstreetmap.org.evil.invalid/1/2/3.png")))
        assertEquals(MapResource.TILE, mapResource(Uri.parse("https://tiles.openfreemap.org/planet/17/123/456.pbf")))
        assertEquals(MapResource.OTHER, mapResource(null))
        assertEquals(MapResource.PAGE, mapResource(null, true))
    }
}
