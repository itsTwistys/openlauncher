package com.openlauncher.app.ui.widget

import android.media.MediaMetadata
import android.media.Rating
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.util.*
import kotlinx.coroutines.delay

@Composable
internal fun MediaArtworkPlayer(state: NowPlayingState, accent: Color, editing: Boolean, day: Boolean,
    onPlayPause: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit, onOpen: () -> Unit) {
    val context = LocalContext.current
    val art = state.artUri ?: state.albumArt
    val text = if (art != null) Color.White else MaterialTheme.colorScheme.onBackground
    var position by remember(state.controller, state.title) { mutableLongStateOf(0) }
    var duration by remember(state.controller, state.title) { mutableLongStateOf(0) }
    var seeking by remember { mutableStateOf(false) }
    var seekTo by remember { mutableFloatStateOf(0f) }
    var showRating by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(state)
    fun failed() = android.widget.Toast.makeText(context, "This player did not accept the control. Open the app for more options.", android.widget.Toast.LENGTH_SHORT).show()
    LaunchedEffect(state.controller, state.title) {
        while (true) {
            val ctrl = latest.controller
            val playback = runCatching { ctrl?.playbackState }.getOrNull()
            duration = runCatching { ctrl?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) }.getOrNull()?.coerceAtLeast(0) ?: 0
            position = projectedPosition(playback?.position ?: 0, playback?.lastPositionUpdateTime ?: 0,
                SystemClock.elapsedRealtime(), playback?.playbackSpeed ?: 0f, playback?.state == PlaybackState.STATE_PLAYING, duration)
            delay(500)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 260.dp
        if (art != null) {
            AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High, error = state.albumArt?.let { BitmapPainter(it.asImageBitmap()) },
                modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f),
                Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.8f)))))
        }
        Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 48.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.fillMaxWidth().clickable(enabled = !editing, onClick = onOpen)) {
                Text(state.title, color = text, fontFamily = FontFamily.SansSerif, fontSize = if (compact) 18.sp else 22.sp,
                    maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                Text(state.artist, color = text.copy(alpha = 0.85f), fontFamily = FontFamily.SansSerif, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column {
                if (duration > 0 && !compact) {
                    if (supportsAction(state.actions, PlaybackState.ACTION_SEEK_TO)) Slider(
                        value = if (seeking) seekTo else position.toFloat(), valueRange = 0f..duration.toFloat(),
                        onValueChange = { seeking = true; seekTo = it }, enabled = !editing,
                        onValueChangeFinished = {
                            if (supportsAction(state.controller?.playbackState?.actions ?: 0, PlaybackState.ACTION_SEEK_TO))
                                runCatching { state.controller?.transportControls?.seekTo(seekTo.toLong()) }.onFailure { failed() }
                            seeking = false
                        }, modifier = Modifier.fillMaxWidth().height(32.dp),
                        colors = SliderDefaults.colors(thumbColor = text, activeTrackColor = text))
                    else LinearProgressIndicator(progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.dp), color = text, trackColor = text.copy(alpha = 0.2f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(mediaTime(position), fontSize = 12.sp, color = text)
                        Text(mediaTime(duration), fontSize = 12.sp, color = text)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrev, enabled = !editing && state.controller != null, modifier = Modifier.size(if (compact) 48.dp else 64.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Previous track", tint = text, modifier = Modifier.size(30.dp))
                    }
                    IconButton(onClick = onPlayPause, enabled = !editing && state.controller != null,
                        modifier = Modifier.size(if (compact) 56.dp else 72.dp).clip(CircleShape).background(text)) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play",
                            tint = if (art != null || !day) Color.Black else Color.White, modifier = Modifier.size(34.dp))
                    }
                    IconButton(onClick = onNext, enabled = !editing && state.controller != null, modifier = Modifier.size(if (compact) 48.dp else 64.dp)) {
                        Icon(Icons.Default.SkipNext, "Next track", tint = text, modifier = Modifier.size(30.dp))
                    }
                }
                if (!compact) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.customActions.take(8).forEach { action ->
                        TextButton(enabled = !editing, onClick = { if (!sendExposedMediaAction(state.controller, action.id)) failed() },
                            colors = ButtonDefaults.textButtonColors(contentColor = text)) { Text(action.label, maxLines = 1) }
                    }
                    if (supportsAction(state.actions, PlaybackState.ACTION_SET_RATING)) {
                        TextButton(enabled = !editing, onClick = {
                            val rating = when (state.ratingStyle) {
                                Rating.RATING_HEART -> Rating.newHeartRating(state.userRating?.hasHeart() != true)
                                Rating.RATING_THUMB_UP_DOWN -> Rating.newThumbRating(state.userRating?.isThumbUp != true)
                                else -> null
                            }
                            if (rating != null) { if (!rateExposedMedia(state.controller, rating)) failed() } else showRating = true
                        }, colors = ButtonDefaults.textButtonColors(contentColor = text)) {
                            Icon(if (state.ratingStyle == Rating.RATING_HEART) Icons.Default.Favorite else Icons.Default.Star, null, Modifier.size(20.dp))
                            Text(if (state.ratingStyle == Rating.RATING_HEART && state.userRating?.hasHeart() == true) " Saved" else " Rate / Like")
                        }
                    }
                    TextButton(onClick = onOpen, enabled = !editing, colors = ButtonDefaults.textButtonColors(contentColor = text)) {
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp)); Text(" Open player")
                    }
                }
            }
        }
    }
    if (showRating) AlertDialog(onDismissRequest = { showRating = false }, title = { Text("Rate this track") }, text = {
        Column {
            val maximum = when (state.ratingStyle) { Rating.RATING_3_STARS -> 3; Rating.RATING_4_STARS -> 4; Rating.RATING_5_STARS -> 5; else -> 0 }
            if (maximum > 0) (1..maximum).forEach { stars -> TextButton(onClick = {
                if (!rateExposedMedia(state.controller, Rating.newStarRating(state.ratingStyle, stars.toFloat()))) failed()
                showRating = false
            }) { Text("$stars stars") } } else Text("Open the player app for this rating type.")
        }
    }, confirmButton = { TextButton(onClick = { showRating = false }) { Text("Close") } })
}
private fun mediaTime(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
