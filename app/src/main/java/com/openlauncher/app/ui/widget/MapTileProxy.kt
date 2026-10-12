package com.openlauncher.app.ui.widget

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.openlauncher.app.data.MapTileCache
import java.io.ByteArrayInputStream

/**
 * Serve the map's own OpenFreeMap requests through the local cache. Returns null for anything else, and also when
 * neither the network nor the cache has the resource, so the WebView falls back to its normal handling.
 */
internal fun interceptMapRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
    if (!request.method.equals("GET", ignoreCase = true)) return null
    val url = request.url.toString()
    if (!MapTileCache.isCacheableUrl(url)) return null
    val entry = MapTileCache.load(context, url) ?: return null
    // The page is served from another origin, so cross-origin fetches need explicit permission.
    return WebResourceResponse(
        entry.mimeType, null, 200, "OK",
        mapOf("Access-Control-Allow-Origin" to "*"),
        ByteArrayInputStream(entry.body)
    )
}
