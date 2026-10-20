package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.service.MediaListenerService.NavigationInfo
import com.openlauncher.app.service.TurnManeuver
import com.openlauncher.app.service.turnCue

/** Uses the same provider preference as Maps. No route history or notification text is saved. */
@Composable
fun DirectionsWidget(navigationPackage: String, isEditing: Boolean, modifier: Modifier = Modifier,
                     expanded: Boolean = false, onExpand: (() -> Unit)? = null) {
    val context = LocalContext.current
    val directions by MediaListenerService.navigation.collectAsState()
    val connected by MediaListenerService.isConnected.collectAsState()
    val navigation = selectDirections(directions, navigationPackage)
    DirectionsCard(navigation, connected, navigationPackage, isEditing, expanded, onExpand, onOpen = {
        val intent = if (!connected) Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) else {
            val opened = navigation?.openIntent?.let { runCatching { it.send() }.isSuccess } ?: false
            if (opened) return@DirectionsCard
            val chosen = navigation?.packageName ?: navigationPackage
            Intent(Intent.ACTION_VIEW, Uri.parse(if (chosen == "com.waze") "https://waze.com/ul" else "geo:0,0"))
                .apply { if (chosen.isNotBlank()) setPackage(chosen) }
        }
        if (runCatching { context.startActivity(intent) }.isFailure && connected) {
            val url = if ((navigation?.packageName ?: navigationPackage) == "com.waze") "https://waze.com/ul" else "https://www.google.com/maps"
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }, modifier = modifier)
}

internal fun selectDirections(items: List<NavigationInfo>, preferredPackage: String): NavigationInfo? =
    items.firstOrNull { preferredPackage.isBlank() || it.packageName == preferredPackage }

@Composable
internal fun DirectionsCard(navigation: NavigationInfo?, connected: Boolean, preferredPackage: String = "",
    isEditing: Boolean = false, expanded: Boolean = false, onExpand: (() -> Unit)? = null,
    onOpen: () -> Unit = {}, modifier: Modifier = Modifier) {
    // A disconnected listener cannot guarantee that a previously received instruction is still current.
    val active = navigation.takeIf { connected && !isEditing }
    val provider = when (active?.packageName ?: preferredPackage) {
        "com.waze" -> "Waze"
        "com.google.android.apps.maps" -> "Google Maps"
        else -> "Directions"
    }
    BoxWithConstraints(modifier) {
        val textScale = MaterialTheme.typography.bodyLarge.fontSize.value / 16f
        val tall = maxHeight >= 360.dp
        val stacked = tall && maxWidth < 340.dp
        val arrowSize = if (tall) 128.dp else 80.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(provider, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (!isEditing) {
                    IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) {
                        Icon(if (connected) Icons.Default.OpenInNew else Icons.Default.Notifications,
                            if (connected) "Open navigation" else "Notification access")
                    }
                    if (onExpand != null) IconButton(onClick = onExpand, modifier = Modifier.size(48.dp)) {
                        Icon(if (expanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            if (expanded) "Return to dashboard" else "Expand DIRECTIONS")
                    }
                }
            }
            if (active == null) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Directions, null, Modifier.size(if (tall) 96.dp else 48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(when { isEditing -> "Directions · drag to move"; !connected -> "Directions disconnected"; else -> "No active route" },
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(if (isEditing) "Large turn arrow and shared next steps" else if (!connected)
                        "Enable Notification Access for Open Launcher to receive turns."
                    else "Start navigation in ${if (preferredPackage.isBlank()) "Google Maps or Waze" else provider} on this device, then return Home.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else key(active.packageName) {
                val scroll = rememberScrollState()
                LaunchedEffect(active.title, active.details) { scroll.scrollTo(0) }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val cue = turnCue(active.title, active.details)
                    val text: @Composable ColumnScope.() -> Unit = {
                        if (cue.distance.isNotBlank()) Text(cue.distance,
                            fontSize = ((if (tall) 48 else 30) * textScale).sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() })
                        Text(cue.instruction, fontSize = ((if (tall) 25 else 19) * textScale).sp,
                            fontWeight = FontWeight.SemiBold)
                        // Some providers put the maneuver in the title and the road in the body.
                        if (active.details.isNotBlank() && cue.instruction == active.title && active.details != active.title)
                            Text(active.details.split(" · ").first(), style = MaterialTheme.typography.bodyLarge)
                    }
                    if (stacked) {
                        DirectionSymbol(cue.maneuver, active.maneuverIcon, arrowSize, Modifier.align(Alignment.CenterHorizontally))
                        Column(Modifier.fillMaxWidth(), content = text)
                    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DirectionSymbol(cue.maneuver, active.maneuverIcon, arrowSize)
                        Column(Modifier.weight(1f), content = text)
                    }
                    if (active.tripDetails.isNotBlank()) Text(active.tripDetails,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("THEN", style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { heading() })
                    if (active.followingSteps.isEmpty()) {
                        Text("Following steps are not shared by this app yet.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else active.followingSteps.take(3).forEach { step ->
                        val next = turnCue(step, "")
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DirectionSymbol(next.maneuver, null, 32.dp)
                            Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionSymbol(maneuver: TurnManeuver, supplied: android.graphics.Bitmap?, size: Dp,
                            modifier: Modifier = Modifier) {
    if (supplied != null) {
        // Provider symbols may contain black pixels or transparent backgrounds; keep their contrast stable.
        Surface(color = androidx.compose.ui.graphics.Color(0xFFE7EBEF),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), modifier = modifier) {
            Image(supplied.asImageBitmap(), "Navigation symbol supplied by navigation app", Modifier.size(size).padding(8.dp))
        }
    } else {
        val icon = when (maneuver) {
            TurnManeuver.LEFT -> Icons.Default.TurnLeft
            TurnManeuver.RIGHT -> Icons.Default.TurnRight
            TurnManeuver.SLIGHT_LEFT -> Icons.Default.TurnSlightLeft
            TurnManeuver.SLIGHT_RIGHT -> Icons.Default.TurnSlightRight
            TurnManeuver.UTURN_LEFT -> Icons.Default.UTurnLeft
            TurnManeuver.UTURN_RIGHT -> Icons.Default.UTurnRight
            TurnManeuver.ROUNDABOUT_LEFT -> Icons.Default.RoundaboutLeft
            TurnManeuver.ROUNDABOUT_RIGHT -> Icons.Default.RoundaboutRight
            TurnManeuver.STRAIGHT -> Icons.Default.Straight
            TurnManeuver.ARRIVE -> Icons.Default.Flag
            TurnManeuver.UNKNOWN -> Icons.Default.Directions
        }
        Icon(icon, if (maneuver == TurnManeuver.UNKNOWN) "Maneuver unavailable" else maneuver.name.replace('_', ' '),
            modifier.size(size), tint = MaterialTheme.colorScheme.onSurface)
    }
}
