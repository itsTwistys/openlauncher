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

/** Local map assets load independently of GPS; position and connectivity are synchronized after resume. */
@Composable
private fun LocationMap(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
              autoZoom: Boolean, headingUp: Boolean, onMapOptions: (Boolean, Boolean) -> Unit,
              networkAvailable: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!onlineEnabled || isEditing) {
        Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(if (isEditing) "Map · drag to move" else "Enable Settings → Online Map → Show Embedded Map",
                fontSize = 16.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
        }
        return
    }
    val owner = LocalLifecycleOwner.current
    var ready by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var autoRetries by remember { mutableIntStateOf(0) }
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var view by remember { mutableStateOf<WebView?>(null) }
    var tileState by remember { mutableStateOf("Loading map engine…") }
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    val latestLocation by rememberUpdatedState(location)
    val latestNetwork by rememberUpdatedState(networkAvailable)
    fun reload() { ready = false; failure = null; view = null; attempt++ }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { resumed = true; autoRetries = 0 }
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(view, resumed) {
        if (resumed) view?.onResume() else view?.onPause()
    }
    LaunchedEffect(networkAvailable, resumed) {
        if (networkAvailable && resumed) {
            autoRetries = 0
            if (failure != null) reload()
            else if (ready) view?.evaluateJavascript("window.resumeMap && window.resumeMap();", null)
        }
    }
    LaunchedEffect(failure, resumed) {
        if (failure != null && resumed && autoRetries < 2) {
            kotlinx.coroutines.delay(2000L * (autoRetries + 1))
            autoRetries++
            reload()
        }
    }
    LaunchedEffect(attempt, resumed) {
        if (resumed) {
            kotlinx.coroutines.delay(12_000)
            if (!ready) failure = "Map engine unavailable. Check Android System WebView or tap Reload."
        }
    }
    // Re-send state after page creation, GPS updates and resume. Never inject text or URLs from apps.
    LaunchedEffect(ready, location, networkAvailable, resumed, autoZoom, headingUp) {
        if (ready && resumed) {
            val fix = latestLocation
            view?.evaluateJavascript("window.setNetworkAvailable($latestNetwork); window.setMapOptions($autoZoom,$headingUp);", null)
            if (fix != null && fix.latitude.isFinite() && fix.longitude.isFinite()) {
                val fresh = android.os.SystemClock.elapsedRealtime() - fix.elapsedRealtimeMs < 30_000
                val speed = if (fresh && fix.speedMps.isFinite()) fix.speedMps.coerceAtLeast(0f) else 0f
                val heading = fix.travelBearing?.takeIf { fresh && speed >= 2f && fix.accuracy <= 50f && it.isFinite() }
                view?.evaluateJavascript("window.updatePosition(${fix.latitude},${fix.longitude},$speed,${heading ?: "null"});", null)
            }
        }
    }
    LaunchedEffect(ready, resumed, attempt) {
        if (ready && resumed) {
            view?.evaluateJavascript("window.resumeMap();", null)
            while (true) {
                now = android.os.SystemClock.elapsedRealtime()
                val current = view
                if (latestLocation?.let { now - it.elapsedRealtimeMs >= 30_000 } != false)
                    current?.evaluateJavascript("window.clearMotion && window.clearMotion();", null)
                current?.evaluateJavascript("window.mapStatus ? JSON.stringify(window.mapStatus()) : null") { result ->
                    if (view === current) runCatching {
                        val value = org.json.JSONTokener(result).nextValue() as? String
                        if (value != null) tileState = org.json.JSONObject(value).getString("message")
                    }
                }
                kotlinx.coroutines.delay(3000)
            }
        }
    }
    LaunchedEffect(failure, tileState) {
        com.openlauncher.app.data.DashboardDiagnostics.mapStatus(failure ?: tileState)
    }
    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context,
        android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val locationEnabled = androidx.core.location.LocationManagerCompat.isLocationEnabled(
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager)
    val gpsText = when {
        !hasPermission -> "Location permission needed"
        !locationEnabled -> "Device location is off"
        location == null -> "Waiting for GPS signal"
        now - location.elapsedRealtimeMs > 30_000 -> "GPS stale · showing last location"
        else -> "GPS ±${location.accuracy.toInt()} m"
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(gpsText, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                Text(if (!networkAvailable) "Offline · reconnecting automatically" else tileState,
                    fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
            }
            TextButton(onClick = { autoRetries = 0; reload() }) { Text("Reload", fontSize = 14.sp) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = autoZoom, onClick = { onMapOptions(!autoZoom, headingUp) },
                label = { Text(if (autoZoom) "Auto zoom on" else "Auto zoom off", fontSize = 14.sp) })
            FilterChip(selected = headingUp, onClick = { onMapOptions(autoZoom, !headingUp) },
                label = { Text(if (headingUp) "Heading up" else "North up", fontSize = 14.sp) })
        }
        if (failure != null) Text(failure!!, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
        key(attempt) {
            EmbeddedWebFrame(Modifier.fillMaxWidth().weight(1f), create = {
                WebView(context).apply {
                    view = this
                    setBackgroundColor(android.graphics.Color.rgb(230, 232, 230))
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setGeolocationEnabled(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.userAgentString += " OpenLauncher/0.0.12 (+https://github.com/itsTwistys/openlauncher)"
                    val assets = androidx.webkit.WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context)).build()
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(web: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? =
                            assets.shouldInterceptRequest(request.url)
                        override fun onPageFinished(web: WebView, url: String) {
                            if (view !== web || url == "about:blank") return
                            web.evaluateJavascript("typeof window.updatePosition === 'function'") { result ->
                                if (view === web) {
                                    ready = result == "true"
                                    failure = if (ready) null else "Map script did not initialize. Retrying…"
                                }
                            }
                        }
                        override fun onReceivedError(web: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                            if (request.isForMainFrame && view === web) failure = "Map engine failed: ${error.description}"
                        }
                        override fun onRenderProcessGone(web: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                            (web.parent as? android.view.ViewGroup)?.removeView(web)
                            web.destroy()
                            if (view === web) { view = null; ready = false; failure = "Map renderer stopped. Recovering…" }
                            return true
                        }
                        override fun shouldOverrideUrlLoading(web: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            if (request.hasGesture() && request.url.scheme == "https")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                            return true
                        }
                    }
                    loadUrl("https://appassets.androidplatform.net/assets/map/map.html")
                }
            })
        }
    }
}

@Composable
fun MapWidget(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
              autoZoom: Boolean = true, headingUp: Boolean = false,
              onMapOptions: (Boolean, Boolean) -> Unit = { _, _ -> },
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
                        Text(if (navigation.packageName == "com.waze") "Waze navigation" else "Google Maps navigation", fontSize = 14.sp)
                        Text(navigation.title, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 20.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (navigation.details.isNotBlank()) Text(navigation.details, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontSize = 16.sp,
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    } else {
                        Text(if (connected) "Start Google Maps or Waze navigation on this device for directions here."
                            else "Enable Notification Access for directions from Google Maps or Waze.", fontSize = 14.sp, maxLines = 2)
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
                    }) { Text(if (!connected) "Enable access" else "Open navigation", fontSize = 14.sp) }
                }
            }
        }
        LocationMap(location, isEditing, onlineEnabled, autoZoom, headingUp, onMapOptions, networkAvailable, Modifier.fillMaxWidth().weight(1f))
    }
}
