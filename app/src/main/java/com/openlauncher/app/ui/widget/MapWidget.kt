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
fun MapWidget(location: LocationData?, isEditing: Boolean, onlineEnabled: Boolean,
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
    val latestLocation by rememberUpdatedState(location)
    var ready by remember { mutableStateOf(false) }
    val view = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setGeolocationEnabled(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString += " OpenLauncher/0.0.6 (+https://github.com/itsTwistys/openlauncher)"
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) { ready = true }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.hasGesture() && request.url.scheme == "https") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    }
                    return true
                }
            }
            val html = context.assets.open("map/map.html").bufferedReader().use { it.readText() }
                .replace("/*LEAFLET_CSS*/", context.assets.open("map/leaflet.css").bufferedReader().use { it.readText() })
                .replace("/*LEAFLET_JS*/", context.assets.open("map/leaflet.js").bufferedReader().use { it.readText() })
            loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
        }
    }
    DisposableEffect(view, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) view.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) view.onPause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); view.stopLoading(); view.destroy() }
    }
    LaunchedEffect(ready, location.latitude, location.longitude) {
        if (ready && location.latitude.isFinite() && location.longitude.isFinite()) {
            view.evaluateJavascript("window.updatePosition(${location.latitude},${location.longitude});", null)
        }
    }
    LaunchedEffect(ready, networkAvailable) {
        if (ready) view.evaluateJavascript("window.setNetworkAvailable($networkAvailable);", null)
    }
    Box(modifier) {
        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        FilledTonalButton(onClick = {
            val pos = latestLocation ?: return@FilledTonalButton
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:${pos.latitude},${pos.longitude}?q=${pos.latitude},${pos.longitude}"))) }
        }, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) { Text("Open maps", fontSize = 10.sp) }
    }
}
