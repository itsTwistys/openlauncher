package dev.nova.cinema

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(playing: Playing, onClose: () -> Unit) {
    val context = LocalContext.current
    var playbackError by remember(playing.stream.url) { mutableStateOf("") }
    var showHeading by remember { mutableStateOf(true) }
    val mediaPlayer = remember(context, playing.stream.url, playing.stream.headers) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("NovaCinema/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(playing.stream.headers)
        val datasource = DefaultDataSource.Factory(context, http)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(datasource))
            .build().apply {
                val config = playing.stream.subtitles.map { subtitle ->
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                        .setMimeType(subtitle.mimeType)
                        .setLanguage(subtitle.language).build()
                }
                val builder = MediaItem.Builder().setUri(playing.stream.url)
                    .setSubtitleConfigurations(config)
                val path = playing.stream.url.substringBefore('?').lowercase()
                if (path.endsWith(".m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                if (path.endsWith(".mpd")) builder.setMimeType(MimeTypes.APPLICATION_MPD)
                setMediaItem(builder.build())
                prepare()
                playWhenReady = true
            }
    }
    DisposableEffect(mediaPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "This stream could not be played."
                showHeading = true
            }
        }
        mediaPlayer.addListener(listener)
        onDispose {
            mediaPlayer.removeListener(listener)
            mediaPlayer.release()
        }
    }
    LaunchedEffect(showHeading) {
        if (showHeading && playbackError.isBlank()) {
            delay(5300)
            showHeading = false
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = mediaPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = true
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = mediaPlayer },
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(showHeading, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .65f))
                .statusBarsPadding().padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose,
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = .13f))) {
                    Icon(Icons.Rounded.ArrowBack, "Close player", tint = WarmWhite)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(playing.movie.title, color = WarmWhite,
                        fontSize = 16.sp, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text(listOfNotNull(playing.episode?.title, playing.stream.addonName)
                        .joinToString(" · "), color = SoftWhite, fontSize = 11.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Amber.copy(alpha = .15f))
                    .padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text("NOVA", fontSize = 10.sp, color = Amber)
                }
            }
        }
        if (playbackError.isNotBlank()) {
            Column(Modifier.align(Alignment.Center)
                .padding(25.dp).clip(RoundedCornerShape(18.dp))
                .background(Layer).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.WarningAmber, null, tint = Amber,
                    modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(10.dp))
                Text("Unable to play this stream", color = WarmWhite, fontSize = 17.sp)
                Spacer(Modifier.height(7.dp))
                Text(playbackError, color = SoftWhite, fontSize = 12.sp)
            }
        }
        if (!showHeading) {
            IconButton(onClick = { showHeading = true },
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
                    .padding(10.dp)) {
                Icon(Icons.Rounded.ArrowBack, "Show playback controls", tint = WarmWhite)
            }
        }
    }
}
