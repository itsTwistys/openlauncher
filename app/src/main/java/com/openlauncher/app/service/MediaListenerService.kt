package com.openlauncher.app.service

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.openlauncher.app.model.NowPlayingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MediaListenerService : NotificationListenerService() {

    private val controllers = linkedMapOf<android.media.session.MediaSession.Token, MediaController>()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val sessionManager by lazy { getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager }
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { refreshNowPlaying() }
    private fun disconnectSessions() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChanged) }
        controllers.values.forEach { runCatching { it.unregisterCallback(controllerCallback) } }
        controllers.clear()
        _sessions.value = emptyList()
        _nowPlaying.value = null
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshNowPlaying()
        override fun onMetadataChanged(metadata: MediaMetadata?)   = refreshNowPlaying()
        override fun onSessionDestroyed()                          = refreshNowPlaying()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        isConnected.value = true
        runCatching { sessionManager.addOnActiveSessionsChangedListener(sessionsChanged,
            ComponentName(this, MediaListenerService::class.java), handler) }
        refreshNowPlaying()
        refreshNavigation()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        isConnected.value = false
        disconnectSessions()
        _nowPlaying.value = null
        _navigation.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNowPlaying()
        if (sbn?.packageName in navigationPackages) refreshNavigation()
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNowPlaying()
        if (sbn?.packageName in navigationPackages) refreshNavigation()
    }

    override fun onDestroy() {
        instance = null
        disconnectSessions()
        // Clear the static flow so the UI doesn't keep showing a dead session
        // (and pinning its album-art bitmap) after the service is killed
        _nowPlaying.value = null
        _navigation.value = emptyList()
        isConnected.value = false
        super.onDestroy()
    }

    private fun refreshNavigation() {
        // Read only supported navigation notifications. Never persist or transmit their contents.
        _navigation.value = runCatching {
            activeNotifications.orEmpty().filter { sbn ->
                sbn.packageName in navigationPackages &&
                    sbn.notification.category == android.app.Notification.CATEGORY_NAVIGATION &&
                    sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY == 0
            }.sortedByDescending { it.postTime }.mapNotNull { sbn ->
                val n = sbn.notification
                val extras = n.extras
                val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
                val lines = listOfNotNull(
                    extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString(),
                    extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString(),
                    extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString()
                ) + extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES).orEmpty().map { it.toString() }
                val details = lines.filter { it.isNotBlank() && it != title }.distinct().joinToString(" · ").take(1000)
                if (title.isBlank() && details.isBlank()) null else NavigationInfo(
                    sbn.packageName, title.take(300), details, n.contentIntent)
            }
        }.getOrDefault(emptyList())
    }

    private fun refreshNowPlaying() {
        val active = runCatching {
            sessionManager.getActiveSessions(ComponentName(this, MediaListenerService::class.java))
        }.getOrDefault(emptyList())
        val tokens = active.map { it.sessionToken }.toSet()
        controllers.keys.filter { it !in tokens }.forEach { token ->
            controllers.remove(token)?.unregisterCallback(controllerCallback)
        }
        active.forEach { controller ->
            if (controller.sessionToken !in controllers) {
                controllers[controller.sessionToken] = controller
                controller.registerCallback(controllerCallback, handler)
            }
        }
        val states = active.mapNotNull { controllers[it.sessionToken]?.let(::stateFromController) }
        _sessions.value = states
        _nowPlaying.value = com.openlauncher.app.util.selectMediaSession(states, "",
            { it.controller?.packageName.orEmpty() }, { it.isPlaying })
    }

    private fun stateFromController(controller: MediaController): NowPlayingState {
        val meta = controller.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        // Prefer the largest bitmap the source provides; many apps put a
        // downscaled image in ALBUM_ART and the full one in ART (or vice versa)
        val art = try {
            listOfNotNull(
                meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
                meta?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ).maxByOrNull { it.width * it.height }
        } catch (_: Exception) { null }
        val artUri = meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

        return NowPlayingState(
            title      = title,
            artist     = artist,
            albumArt   = art,
            artUri     = artUri,
            isPlaying  = isPlaying,
            controller = controller
        )
    }

    data class NavigationInfo(val packageName: String, val title: String, val details: String,
        val openIntent: android.app.PendingIntent?)

    companion object {
        private val navigationPackages = setOf("com.google.android.apps.maps", "com.waze")
        private val _navigation = MutableStateFlow<List<NavigationInfo>>(emptyList())
        val navigation: StateFlow<List<NavigationInfo>> = _navigation
        private val _nowPlaying = MutableStateFlow<NowPlayingState?>(null)
        val nowPlaying: StateFlow<NowPlayingState?> = _nowPlaying
        private val _sessions = MutableStateFlow<List<NowPlayingState>>(emptyList())
        val sessions: StateFlow<List<NowPlayingState>> = _sessions
        val isConnected = MutableStateFlow(false)

        @Volatile private var instance: MediaListenerService? = null
        fun requestRefresh() { instance?.refreshNowPlaying() }
    }
}
