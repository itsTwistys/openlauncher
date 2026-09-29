package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openlauncher.app.util.LocationData

/** Loads bundled map code once, then moves the marker without reloading the page. */
@Composable
private fun LocationMap(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
              networkAvailable: Boolean = true, modifier: Modifier = Modifier) {
    if (!onlineEnabled || location == null || isEditing) {
        Box(modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(when {
                !onlineEnabled -> "Enable online map in Settings"
                isEditing -> "MAP · drag or resize"
                else -> "Waiting for GPS. Check location permission and signal."
            }, fontSize = 12.sp)
        }
        return
    }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var ready by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var view by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(view, owner) {
        val current = view
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) current?.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) current?.onPause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(attempt) {
        kotlinx.coroutines.delay(10_000)
        if (!ready) failure = "Map engine did not start. Try Reload; check Android System WebView if it persists."
    }
    LaunchedEffect(ready, location.latitude, location.longitude) {
        if (ready && location.latitude.isFinite() && location.longitude.isFinite()) {
            view?.evaluateJavascript("window.updatePosition(${location.latitude},${location.longitude});", null)
        }
    }
    LaunchedEffect(ready, networkAvailable) {
        if (ready) view?.evaluateJavascript("window.setNetworkAvailable($networkAvailable);", null)
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (!networkAvailable) "Map offline" else if (ready) "Live GPS map" else "Loading map…",
                fontSize = 10.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
            TextButton(onClick = { ready = false; failure = null; view = null; attempt++ }) {
                Text("Reload", fontSize = 11.sp)
            }
        }
        if (failure != null) {
            Text(failure!!, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
        } else {
            key(attempt) {
                EmbeddedWebFrame(Modifier.fillMaxWidth().weight(1f), create = {
                    WebView(context).apply {
                        view = this
                        setBackgroundColor(android.graphics.Color.rgb(230, 232, 230))
                        // Maps need no video surface; software rendering avoids vendor GPU compositing issues.
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setGeolocationEnabled(false)
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.userAgentString += " OpenLauncher/0.0.8 (+https://github.com/itsTwistys/openlauncher)"
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(web: WebView, url: String) {
                                web.evaluateJavascript("typeof window.updatePosition === 'function'") { result ->
                                    if (view === web) {
                                        ready = result == "true"
                                        if (!ready) failure = "Map script failed to initialize. Reload or update Android System WebView."
                                    }
                                }
                            }
                            override fun onReceivedError(web: WebView, request: WebResourceRequest,
                                error: android.webkit.WebResourceError) {
                                if (request.isForMainFrame && view === web)
                                    failure = "Map could not load: ${error.description}"
                            }
                            override fun shouldOverrideUrlLoading(web: WebView, request: WebResourceRequest): Boolean {
                                if (!request.isForMainFrame) return false
                                if (request.hasGesture() && request.url.scheme == "https")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                                return true
                            }
                        }
                        val html = context.assets.open("map/map.html").bufferedReader().use { it.readText() }
                            .replace("/*LEAFLET_CSS*/", context.assets.open("map/leaflet.css").bufferedReader().use { it.readText() })
                            .replace("/*LEAFLET_JS*/", context.assets.open("map/leaflet.js").bufferedReader().use { it.readText() })
                        loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
                    }
                })
            }
        }
    }
}


@Composable
fun MapWidget(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
              networkAvailable: Boolean = true, navigationPackage: String = "", modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val directions by com.openlauncher.app.service.MediaListenerService.navigation.collectAsState()
    val connected by com.openlauncher.app.service.MediaListenerService.isConnected.collectAsState()
    val navigation = directions.firstOrNull { navigationPackage.isBlank() || it.packageName == navigationPackage }
    Column(modifier) {
        if (!isEditing) {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    if (navigation != null) {
                        Text(if (navigation.packageName == "com.waze") "Waze navigation" else "Google Maps navigation", fontSize = 10.sp)
                        Text(navigation.title, fontSize = 14.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (navigation.details.isNotBlank()) Text(navigation.details, fontSize = 11.sp,
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    } else {
                        Text(if (connected) "Start Google Maps or Waze navigation on this device for directions here."
                            else "Enable Notification Access for directions from Google Maps or Waze.", fontSize = 11.sp, maxLines = 2)
                    }
                    TextButton(contentPadding = PaddingValues(horizontal = 4.dp), onClick = {
                        if (!connected) {
                            runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                        } else {
                            val opened = navigation?.openIntent?.let { runCatching { it.send() }.isSuccess } ?: false
                            if (!opened) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
                                if (navigationPackage.isNotBlank()) intent.setPackage(navigationPackage)
                                if (runCatching { context.startActivity(intent) }.isFailure)
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps"))) }
                            }
                        }
                    }) { Text(if (!connected) "Enable access" else "Open navigation", fontSize = 11.sp) }
                }
            }
        }
        LocationMap(location, isEditing, onlineEnabled, networkAvailable, Modifier.fillMaxWidth().weight(1f))
    }
}
