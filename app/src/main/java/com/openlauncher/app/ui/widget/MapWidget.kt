package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.openlauncher.app.data.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openlauncher.app.util.LocationData

/** Local map assets load independently of GPS; position and connectivity are synchronized after resume. */
@Composable
private fun LocationMap(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
              autoZoom: Boolean, headingUp: Boolean, onMapOptions: (Boolean, Boolean) -> Unit,
              softwareRendering: Boolean, onSoftwareRendering: (Boolean) -> Unit,
              navigationAction: String, onOpenNavigation: () -> Unit,
              networkAvailable: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!onlineEnabled || isEditing) {
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            if (!isEditing) TextButton(onClick = onOpenNavigation, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(navigationAction, fontSize = 16.sp)
            }
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
    var optionsOpen by remember { mutableStateOf(false) }
    var previousRendering by remember { mutableStateOf(softwareRendering) }
    val latestLocation by rememberUpdatedState(location)
    val latestNetwork by rememberUpdatedState(networkAvailable)
    fun evaluate(web: WebView, script: String, callback: ((String) -> Unit)? = null) {
        try { web.evaluateJavascript(script) { result -> if (view === web) callback?.invoke(result) } }
        catch (_: RuntimeException) {
            if (view === web) {
                DashboardDiagnostics.engine(MapEngine.EVALUATION_FAILED)
                failure = "Map script evaluation failed. Retrying…"
            }
        }
    }
    fun reload() { ready = false; failure = null; view = null; attempt++ }
    LaunchedEffect(softwareRendering) {
        if (previousRendering != softwareRendering) {
            previousRendering = softwareRendering
            autoRetries = 0
            reload()
        }
    }
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
            else if (ready) view?.let { evaluate(it, "window.resumeMap && window.resumeMap();") }
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
            if (!ready && failure == null) {
                DashboardDiagnostics.engine(MapEngine.TIMED_OUT)
                failure = "Map engine unavailable. Check Android System WebView or tap Reload."
            }
        }
    }
    // Re-send state after page creation, GPS updates and resume. Never inject text or URLs from apps.
    LaunchedEffect(ready, location, networkAvailable, resumed, autoZoom, headingUp) {
        if (ready && resumed) {
            val fix = latestLocation
            view?.let { evaluate(it, "window.setNetworkAvailable($latestNetwork); window.setMapOptions($autoZoom,$headingUp);") }
            if (fix != null && fix.latitude.isFinite() && fix.longitude.isFinite()) {
                val fresh = android.os.SystemClock.elapsedRealtime() - fix.elapsedRealtimeMs < 30_000
                val speed = if (fresh && fix.speedMps.isFinite()) fix.speedMps.coerceAtLeast(0f) else 0f
                val heading = fix.travelBearing?.takeIf { fresh && speed >= 2f && fix.accuracy <= 50f && it.isFinite() }
                view?.let { evaluate(it, "window.updatePosition(${fix.latitude},${fix.longitude},$speed,${heading ?: "null"},${fix.accuracy.takeIf { it.isFinite() } ?: 0f},$fresh);") }
            }
        }
    }
    LaunchedEffect(ready, resumed, attempt) {
        if (ready && resumed) {
            view?.let { evaluate(it, "window.resumeMap();") }
            while (true) {
                now = android.os.SystemClock.elapsedRealtime()
                val current = view
                if (latestLocation?.let { now - it.elapsedRealtimeMs >= 30_000 } != false)
                    current?.let { evaluate(it, "window.clearMotion && window.clearMotion();") }
                if (current != null) evaluate(current, "window.mapStatus ? JSON.stringify(window.mapStatus()) : null") { result ->
                    if (view === current) runCatching {
                        val value = org.json.JSONTokener(result).nextValue() as? String
                        if (value != null) {
                            val status = org.json.JSONObject(value)
                            tileState = status.getString("message")
                            DashboardDiagnostics.tiles(status.getBoolean("started"), status.getBoolean("loading"),
                                status.getInt("totalLoaded"), status.getInt("totalErrors"), status.getInt("totalTimeouts"))
                        }
                    }
                }
                kotlinx.coroutines.delay(3000)
            }
        }
    }
    LaunchedEffect(failure, tileState) {
        DashboardDiagnostics.mapStatus(failure ?: tileState)
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
    val needsGps = !hasPermission || !locationEnabled || location == null || now - location.elapsedRealtimeMs > 30_000
    val status = when {
        failure != null -> failure
        !networkAvailable -> "Offline · waiting for internet"
        needsGps -> gpsText
        !ready -> "Loading map…"
        else -> null // Tile failures remain visible in the map's status overlay.
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpenNavigation, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(navigationAction, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { optionsOpen = true }, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Map options", fontSize = 16.sp)
            }
        }
        status?.let { Text(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 14.sp,
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
        if (optionsOpen) MapOptionsDialog(autoZoom, headingUp, softwareRendering, gpsText,
            failure ?: if (!networkAvailable) "Offline · waiting for internet" else tileState,
            onMapOptions, onSoftwareRendering, onReload = { autoRetries = 0; reload(); optionsOpen = false },
            onDismiss = { optionsOpen = false })
        key(attempt) {
            EmbeddedWebFrame(Modifier.fillMaxWidth().weight(1f), create = {
                DashboardDiagnostics.beginPage()
                var created: WebView? = null
                try { WebView(context).also { created = it }.apply {
                    view = this
                    setBackgroundColor(android.graphics.Color.rgb(230, 232, 230))
                    setLayerType(if (softwareRendering) android.view.View.LAYER_TYPE_SOFTWARE
                        else android.view.View.LAYER_TYPE_NONE, null)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setGeolocationEnabled(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.userAgentString += " OpenLauncher/${com.openlauncher.app.BuildConfig.VERSION_NAME} (+https://github.com/itsTwistys/openlauncher)"
                    val assets = androidx.webkit.WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context)).build()
                    val currentWeb = this
                    // WebView may resume before Compose has assigned its final bounds.
                    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        if (right > left && bottom > top &&
                            (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)) {
                            post { if (view === currentWeb && ready && resumed)
                                evaluate(currentWeb, "window.resizeMap && window.resizeMap();") }
                        }
                    }
                    webChromeClient = MapChromeDiagnostics(DashboardDiagnostics) { view === currentWeb }
                    webViewClient = object : MapWebDiagnostics(DashboardDiagnostics, { view === it }, {
                        failure = "Map page failed to load. Retrying…"
                    }) {
                        override fun shouldInterceptRequest(web: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? =
                            assets.shouldInterceptRequest(request.url)
                        override fun onPageFinished(web: WebView, url: String) {
                            if (view !== web || url == "about:blank") return
                            evaluate(web, "typeof window.updatePosition === 'function' && typeof window.mapStatus === 'function'") { result ->
                                if (view === web) {
                                    ready = result == "true"
                                    DashboardDiagnostics.engine(if (ready) MapEngine.READY else MapEngine.SCRIPT_FAILED)
                                    failure = if (ready) null else "Map script did not initialize. Retrying…"
                                }
                            }
                        }
                        override fun onRenderProcessGone(web: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                            (web.parent as? android.view.ViewGroup)?.removeView(web)
                            web.destroy()
                            if (view === web) {
                                DashboardDiagnostics.engine(MapEngine.RENDERER_GONE)
                                view = null; ready = false; failure = "Map renderer stopped. Recovering…"
                            }
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
                } } catch (_: RuntimeException) {
                    view = null
                    runCatching { created?.destroy() }
                    DashboardDiagnostics.engine(MapEngine.CREATE_FAILED)
                    failure = "Map WebView could not initialize. Retrying…"
                    null
                }
            }, onRelease = { released -> if (view === released) view = null })
        }
    }
}

@Composable
fun MapWidget(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
              autoZoom: Boolean = true, headingUp: Boolean = false,
              onMapOptions: (Boolean, Boolean) -> Unit = { _, _ -> },
              softwareRendering: Boolean = true, onSoftwareRendering: (Boolean) -> Unit = {},
              networkAvailable: Boolean = true, navigationPackage: String = "", modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val directions by com.openlauncher.app.service.MediaListenerService.navigation.collectAsState()
    val connected by com.openlauncher.app.service.MediaListenerService.isConnected.collectAsState()
    val navigation = directions.firstOrNull { navigationPackage.isBlank() || it.packageName == navigationPackage }
    Column(modifier) {
        if (!isEditing && navigation != null) {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(navigation.title, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 24.sp,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (navigation.details.isNotBlank()) Text(navigation.details,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontSize = 16.sp,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
        LocationMap(location, isEditing, onlineEnabled, autoZoom, headingUp, onMapOptions,
            softwareRendering, onSoftwareRendering, if (connected) "Open navigation" else "Enable directions", onOpenNavigation = {
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
            }, networkAvailable = networkAvailable, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
internal fun MapOptionsDialog(autoZoom: Boolean, headingUp: Boolean, softwareRendering: Boolean,
    gpsStatus: String, mapStatus: String, onMapOptions: (Boolean, Boolean) -> Unit,
    onSoftwareRendering: (Boolean) -> Unit, onReload: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Map options") }, text = {
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapOptionSwitch("Auto zoom", autoZoom) { onMapOptions(it, headingUp) }
            MapOptionSwitch("Heading up", headingUp) { onMapOptions(autoZoom, it) }
            HorizontalDivider()
            MapOptionSwitch("Compatibility rendering", softwareRendering, onSoftwareRendering)
            Text("If tiles load but the map looks blank, try changing this. The map reloads automatically.", fontSize = 14.sp)
            Text(gpsStatus, fontSize = 16.sp)
            Text(mapStatus, fontSize = 14.sp)
            OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Reload map", fontSize = 16.sp)
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) {
        Text("Done", fontSize = 16.sp)
    } })
}

@Composable
private fun MapOptionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked, onCheckedChange, modifier = Modifier.semantics { contentDescription = label })
    }
}
