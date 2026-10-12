package com.openlauncher.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Prevent a launch/return-to-Home loop; only a cold start or a longer absence rearms startup. */
internal class MediaStartupGate {
    private var attempted = false
    private var stoppedAt: Long? = null
    fun stopped(now: Long) { stoppedAt = now }
    fun take(now: Long): Boolean {
        val eligible = !attempted || stoppedAt?.let { now - it >= 60_000 } == true
        stoppedAt = null
        if (eligible) attempted = true
        return eligible
    }
}

internal fun musicStartupPackage(preferred: String, lastUsed: String): String = when {
    preferred == "@radio" -> ""
    preferred.isNotBlank() -> preferred
    else -> lastUsed
}

/** Connect without moving the dashboard. Never play audio merely because Home resumed. */
internal class RememberedMediaStarter(private val context: Context) {
    private var browser: MediaBrowser? = null
    fun close() { browser?.let { runCatching { it.disconnect() } }; browser = null }

    suspend fun connect(packageName: String): Boolean {
        val component = context.packageManager.queryIntentServices(
            Intent("android.media.browse.MediaBrowserService").setPackage(packageName), 0)
            .firstOrNull { it.serviceInfo.exported && it.serviceInfo.enabled && it.serviceInfo.permission == null }
            ?.serviceInfo?.let { ComponentName(it.packageName, it.name) } ?: return false
        close()
        val handler = Handler(Looper.getMainLooper())
        val connected = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                lateinit var candidate: MediaBrowser
                var check: Runnable? = null
                fun finish(value: Boolean) {
                    if (!continuation.isActive) return
                    if (!value) close()
                    continuation.resume(value)
                }
                candidate = MediaBrowser(context, component, object : MediaBrowser.ConnectionCallback() {
                    override fun onConnectionFailed() = finish(false)
                    override fun onConnectionSuspended() = finish(false)
                    override fun onConnected() {
                        if (!continuation.isActive) return
                        try {
                            val controller = MediaController(context, candidate.sessionToken)
                            if (Build.VERSION.SDK_INT >= 24 && supportsAction(controller.playbackState?.actions ?: 0,
                                    PlaybackState.ACTION_PREPARE)) controller.transportControls.prepare()
                            check = Runnable {
                                val metadata = runCatching { controller.metadata }.getOrNull()
                                finish(!metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() ||
                                    !metadata?.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE).isNullOrBlank())
                            }
                            handler.postDelayed(requireNotNull(check), 800)
                        } catch (_: Exception) { finish(false) }
                    }
                }, null)
                browser = candidate
                continuation.invokeOnCancellation {
                    handler.post { check?.let { handler.removeCallbacks(it) }; if (browser === candidate) close() }
                }
                try { candidate.connect() } catch (_: Exception) { finish(false) }
            }
        } ?: false
        if (!connected) close()
        return connected
    }
}
