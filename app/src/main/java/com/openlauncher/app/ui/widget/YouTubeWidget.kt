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
import com.openlauncher.app.util.youtubeVideoId

@Composable
fun YouTubeWidget(url: String, isEditing: Boolean, moving: Boolean,
                  networkAvailable: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val id = remember(url) { youtubeVideoId(url) }
    var parkedPlayback by remember(id) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) parkedPlayback = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(moving, isEditing, networkAvailable) {
        if (moving || isEditing || !networkAvailable) parkedPlayback = false
    }
    BoxWithConstraints(modifier) {
        // The official player requires at least a 200 x 200 CSS-pixel viewport.
        val fits = maxWidth >= 200.dp && maxHeight >= 248.dp
        if (id == null || isEditing || moving || !networkAvailable || !fits || !parkedPlayback) {
            Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(when {
                    isEditing -> "YOUTUBE · drag or resize"
                    id == null -> "Add a video or live-stream link in Settings → YouTube."
                    moving -> "Video paused while moving."
                    !networkAvailable -> "Connect to WiFi to play YouTube."
                    !fits -> "Enlarge this widget to show the YouTube player."
                    else -> "YouTube video / live stream · parked use only"
                }, fontSize = 12.sp)
                if (id != null && !isEditing && !moving && networkAvailable && fits)
                    TextButton(onClick = { parkedPlayback = true }) { Text("I’m parked · Load player") }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.height(48.dp)) {
                    TextButton(onClick = { retry++ }) { Text("Reload", fontSize = 11.sp) }
                    TextButton(onClick = {
                        parkedPlayback = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id"))) }
                    }) { Text("Open YouTube", fontSize = 11.sp) }
                    TextButton(onClick = { parkedPlayback = false }) { Text("Stop", fontSize = 11.sp) }
                }
                key(id, retry) { YouTubePlayer(id, Modifier.fillMaxWidth().weight(1f)) }
            }
        }
    }
}

@Composable
private fun YouTubePlayer(id: String, modifier: Modifier) {
    val context = LocalContext.current
    val view = remember(id, context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setGeolocationEnabled(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webChromeClient = android.webkit.WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    if (request.hasGesture() && request.url.scheme == "https")
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    return true
                }
            }
            val origin = "https://${context.packageName}"
            val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>html,body,iframe{margin:0;width:100%;height:100%;border:0;background:black;overflow:hidden}</style>
                </head><body><iframe title="YouTube video player" src="https://www.youtube.com/embed/$id?playsinline=1&amp;autoplay=0&amp;fs=0&amp;origin=$origin" allow="encrypted-media; picture-in-picture" referrerpolicy="strict-origin-when-cross-origin"></iframe></body></html>"""
            loadDataWithBaseURL("$origin/", html, "text/html", "UTF-8", null)
        }
    }
    DisposableEffect(view) {
        onDispose {
            view.onPause()
            view.stopLoading()
            view.loadUrl("about:blank")
            view.removeAllViews()
            view.destroy()
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}
