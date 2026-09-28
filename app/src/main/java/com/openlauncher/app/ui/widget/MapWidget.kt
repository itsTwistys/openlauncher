package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openlauncher.app.util.LocationData
import java.util.Locale
import kotlin.math.abs

/**
 * The visible map loads OpenStreetMap tiles for the device's precise location.
 * The user opts into this network request with the Online Map setting.
 */
@Composable
fun MapWidget(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!onlineEnabled) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Enable online map in Settings", fontSize = 11.sp)
        }
        return
    }
    if (location == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Waiting for GPS location", fontSize = 11.sp)
        }
        return
    }

    var center by remember { mutableStateOf(location.latitude to location.longitude) }
    // Recenter after a meaningful move, avoiding a reload on every GPS fix.
    if (abs(location.latitude - center.first) > 0.005 ||
        abs(location.longitude - center.second) > 0.005) {
        center = location.latitude to location.longitude
    }

    val mapUrl = remember(center) {
        val lat = center.first.coerceIn(-89.99, 89.99)
        val lon = center.second.coerceIn(-179.99, 179.99)
        String.format(
            Locale.US,
            "https://www.openstreetmap.org/export/embed.html?bbox=%f%%2C%f%%2C%f%%2C%f&layer=mapnik&marker=%f%%2C%f",
            lon - 0.015, lat - 0.01, lon + 0.015, lat + 0.01, lat, lon
        )
    }

    if (isEditing) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("MAP • drag or resize", fontSize = 11.sp)
        }
        return
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(it).apply {
                    settings.javaScriptEnabled = true // OpenStreetMap embed uses Leaflet
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.userAgentString += " OpenLauncher/0.0.5"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            // Keep the map within OSM; other links require a tap and open externally.
                            if (request.url.host == "www.openstreetmap.org" &&
                                request.url.scheme == "https") return false
                            if (request.hasGesture() && request.url.scheme == "https") {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                            }
                            return true
                        }
                    }
                    tag = mapUrl
                    loadUrl(mapUrl)
                    webView = this
                }
            },
            update = { view ->
                if (view.tag != mapUrl) {
                    view.tag = mapUrl
                    view.loadUrl(mapUrl)
                }
            }
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0xDD111111))
                .clickable {
                    val uri = Uri.parse(String.format(Locale.US,
                        "geo:%f,%f?q=%f,%f", location.latitude, location.longitude,
                        location.latitude, location.longitude))
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OPEN MAPS", color = Color.White, fontSize = 9.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.OpenInNew, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}
