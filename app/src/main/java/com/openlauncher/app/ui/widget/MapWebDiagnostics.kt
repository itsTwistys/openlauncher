package com.openlauncher.app.ui.widget

import android.net.Uri
import android.net.http.SslError
import android.webkit.*
import com.openlauncher.app.data.*

/** Classify transiently, never retain the URL (tile paths encode the viewed location). */
internal fun mapResource(uri: Uri?, mainFrame: Boolean = false): MapResource = when {
    mainFrame -> MapResource.PAGE
    uri?.host == "appassets.androidplatform.net" -> MapResource.LOCAL_ASSET
    uri?.host in setOf("tile.openstreetmap.org", "tiles.openfreemap.org") -> MapResource.TILE
    else -> MapResource.OTHER
}

internal class MapChromeDiagnostics(
    private val diagnostics: MapDiagnostics,
    private val active: () -> Boolean
) : WebChromeClient() {
    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
        if (active()) when (message.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> diagnostics.failure(MapFailure.CONSOLE)
            ConsoleMessage.MessageLevel.WARNING -> diagnostics.consoleWarning()
            else -> Unit
        }
        // Consume without forwarding message/source ID/line to logs or the diagnostics export.
        return true
    }
}

internal open class MapWebDiagnostics(
    private val diagnostics: MapDiagnostics,
    private val active: (WebView) -> Boolean,
    private val mainFrameFailed: () -> Unit
) : WebViewClient() {
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.M)
    override fun onReceivedError(web: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (!active(web)) return
        diagnostics.failure(MapFailure.NETWORK, mapResource(request.url, request.isForMainFrame), error.errorCode)
        if (request.isForMainFrame) mainFrameFailed()
    }
    @Suppress("DEPRECATION")
    override fun onReceivedError(web: WebView, code: Int, description: String?, failingUrl: String?) {
        // API 21/22 only provide this main-frame callback. Avoid double counting on API 23+.
        if (android.os.Build.VERSION.SDK_INT < 23 && active(web)) {
            diagnostics.failure(MapFailure.NETWORK, MapResource.PAGE, code)
            mainFrameFailed()
        }
    }
    override fun onReceivedHttpError(web: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        if (!active(web)) return
        diagnostics.failure(MapFailure.HTTP, mapResource(request.url, request.isForMainFrame), response.statusCode)
        if (request.isForMainFrame) mainFrameFailed()
    }
    override fun onReceivedSslError(web: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        if (active(web)) diagnostics.failure(MapFailure.SSL, mapResource(Uri.parse(error.url)), error.primaryError)
    }
}
