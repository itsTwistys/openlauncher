package com.openlauncher.app.ui.widget

import com.openlauncher.app.ui.components.SettingsDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openlauncher.app.data.*
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.util.dashboardTimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SportsWidget(settings: AppSettings, weather: WeatherState?, networkAvailable: Boolean, isEditing: Boolean,
                 onSettings: (AppSettings.() -> AppSettings) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { SportsRepository(context.applicationContext) }
    val favorites = settings.sportsLeagues.mapNotNull { name -> SportsLeague.entries.find { it.name == name } }.ifEmpty { SportsLeague.entries.toList() }
    val league = SportsLeague.entries.find { it.name == settings.sportsLeague && it in favorites } ?: favorites.first()
    val selection = SportsView.entries.find { it.name == settings.sportsView } ?: SportsView.LIVE
    val zone = dashboardTimeZone(settings.clockTimeZone, weather)
    val owner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var snapshot by remember(league, selection) { mutableStateOf<SportsSnapshot?>(null) }
    var loading by remember(league, selection) { mutableStateOf(false) }
    var error by remember(league, selection) { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var refresh by remember { mutableIntStateOf(0) }
    var lastManualRefresh by remember { mutableLongStateOf(0L) }
    var menu by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(league, selection, networkAvailable, resumed, isEditing, refresh, zone.id) {
        if (!resumed || isEditing) return@LaunchedEffect
        if (snapshot == null) snapshot = repository.cached(league, selection, System.currentTimeMillis())
        if (!networkAvailable) return@LaunchedEffect
        while (true) {
            loading = true
            try { snapshot = repository.fetch(league, selection, System.currentTimeMillis(), zone); error = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "Scores unavailable · retrying" }
            finally { loading = false }
            delay(if (selection == SportsView.LIVE) 30_000 else 300_000)
        }
    }
    LaunchedEffect(resumed) { if (resumed) while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    if (options && !isEditing) SettingsDialog(onDismissRequest = { options = false }, title = { Text("Favorite leagues") }, text = {
        Column(Modifier.fillMaxWidth()) {
            Text("Choose the leagues shown in the selector.", style = MaterialTheme.typography.bodyMedium)
            SportsLeague.entries.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(item in favorites, { checked ->
                        val next = if (checked) (favorites + item).distinct() else favorites.filter { it != item }
                        if (next.isNotEmpty()) onSettings { copy(sportsLeagues = next.map { it.name }, sportsLeague = if (league in next) league.name else next.first().name) }
                    }); Text(item.label)
                }
            }
            Text("Live scores refresh while this dashboard is visible. F1 shows published session status, schedules and the latest classified race result.", style = MaterialTheme.typography.bodyMedium)
        }
    }, confirmButton = { TextButton(onClick = { options = false }) { Text("Done") } },
        dismissButton = { TextButton(onClick = {
            if (now - lastManualRefresh >= 20_000) { lastManualRefresh = now; refresh++ }; options = false
        }, enabled = networkAvailable && !loading && !isEditing) { Icon(Icons.Default.Refresh, null); Text("Refresh") } })
    val age = snapshot?.let { (now - it.updatedAtMs).coerceAtLeast(0) }
    val current = networkAvailable && error == null && age != null && age < if (selection == SportsView.LIVE) 90_000 else 600_000
    val ageText = when {
        age == null -> ""
        age < 60_000 -> "just now"
        age < 3_600_000 -> "${age / 60_000}m ago"
        age < 86_400_000 -> "${age / 3_600_000}h ago"
        else -> "${age / 86_400_000}d ago"
    }
    val status = when {
        isEditing -> "Live scores, results and schedules"
        !networkAvailable -> if (snapshot != null) "Offline · saved $ageText" else "Offline · connect to load scores"
        error != null -> error!!
        snapshot == null && loading -> "Loading scores…"
        age != null -> "${if (current) "Updated" else "Saved"} $ageText"
        else -> "Waiting for scores"
    }
    Column(modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                TextButton(onClick = { menu = true }, enabled = !isEditing, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(league.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Icon(Icons.Default.ExpandMore, "Select league")
                }
                DropdownMenu(menu, { menu = false }) { favorites.forEach { item ->
                    DropdownMenuItem(text = { Text(item.label) }, onClick = { menu = false; onSettings { copy(sportsLeague = item.name) } })
                } }
            }
            IconButton(onClick = { options = true }, enabled = !isEditing) { Icon(Icons.Default.Tune, "Sports options") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SportsView.entries.forEach { item -> FilterChip(selected = selection == item, enabled = !isEditing, onClick = { onSettings { copy(sportsView = item.name) } }, label = { Text(item.label) }) }
        }
        Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp), maxLines = 2)
        val events = snapshot?.events.orEmpty()
        if (events.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(if (isEditing) "NFL · NBA · NHL · MLB · F1" else if (snapshot == null) "Scores will appear here" else when (selection) {
                SportsView.LIVE -> "No events live now"; SportsView.RESULTS -> "No published results in this period"; SportsView.UPCOMING -> "No upcoming events in this period"
            }, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(events, key = { it.id }) { event -> SportsEventRow(event, league, zone, settings.use12HourTime, current) }
        }
        Text("${snapshot?.source ?: if (league == SportsLeague.F1 && selection != SportsView.LIVE) "Jolpica F1" else "ESPN"} · ${if (league == SportsLeague.F1 && selection == SportsView.RESULTS) "Latest race" else if (league == SportsLeague.F1 && selection == SportsView.UPCOMING) "Race calendar" else if (selection == SportsView.RESULTS) "Past 7 days" else if (selection == SportsView.UPCOMING) "Next 14 days" else "Published updates"}", fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SportsEventRow(event: SportsEvent, league: SportsLeague, zone: TimeZone, twelveHour: Boolean, current: Boolean) {
    val interrupted = listOf("postpon", "cancel", "delay", "suspend").any { event.detail.lowercase(Locale.US).contains(it) }
    val live = event.state == "in" && current && !interrupted
    val time = SimpleDateFormat(if (!event.timeKnown) "EEE MMM d" else if (twelveHour) "EEE MMM d · h:mm a" else "EEE MMM d · HH:mm", Locale.getDefault()).apply { timeZone = if (event.timeKnown) zone else TimeZone.getTimeZone("UTC") }.format(Date(event.startMs)) + if (!event.timeKnown && event.state == "pre") " · Time TBA" else ""
    Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (live) "LIVE · ${event.detail}" else if (event.state == "in" && !current) "Saved · ${event.detail}" else if (interrupted) "${event.detail} · $time" else if (event.state == "pre") time else event.detail.ifBlank { "Final" },
                fontSize = 12.sp, fontWeight = if (live) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (league == SportsLeague.F1) {
            Text(event.title, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            event.competitors.filter { it.position != null || it.winner }.take(3).forEach { driver ->
                Text("${driver.position?.let { "$it. " }.orEmpty()}${driver.name}", fontSize = 15.sp)
            }
        } else event.competitors.take(2).forEach { team ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(team.abbreviation.ifBlank { team.name }, fontSize = 20.sp, fontWeight = if (team.winner) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(team.score ?: "–", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (event.state == "post" || (event.state == "in" && !current)) Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
