package com.openlauncher.app.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.*
import com.openlauncher.app.model.*
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.util.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardTools(initialPage: String, settings: AppSettings, weather: WeatherState?, weatherError: String?,
    location: LocationData?, internet: Boolean, trips: TripLog, tripError: String?, onToggleTrip: () -> Unit, onFinishTrip: () -> Unit,
    onClearTrips: () -> Unit, onRefreshWeather: () -> Unit, onSettings: (AppSettings.() -> AppSettings) -> Unit,
    onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember(initialPage) { mutableStateOf(initialPage) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            message = runCatching { withContext(Dispatchers.IO) {
                requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(pendingExport) }
            }; "Export saved" }.getOrElse { "Could not save export: ${it.message}" }
        }
    }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { message = "No app can open this link." }
    }
    val metric = settings.unitSystem == UnitSystem.METRIC
    val info = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    val installed = info.versionName.orEmpty()
    DashboardPanel("Dashboard controls", onDismiss) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Quick controls", "Nearby", "Trips", "Weather", "Diagnostics", "Updates").forEach { item ->
                FilterChip(page == item, { page = item; message = null }, label = { Text(item) })
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            when (page) {
                "Quick controls" -> {
                    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
                    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
                    var dragging by remember { mutableStateOf(false) }
                    var beforeMute by remember { mutableIntStateOf(volume.toInt().coerceAtLeast(1)) }
                    LaunchedEffect(Unit) { while (true) { if (!dragging) volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat(); delay(500) } }
                    Text("Media volume · ${volume.roundToInt()} / $maxVolume")
                    Slider(volume, { dragging = true; volume = it }, valueRange = 0f..maxVolume.toFloat(),
                        onValueChangeFinished = {
                            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume.roundToInt(), 0) }
                                .onFailure { message = "Volume is controlled by your head unit." }; dragging = false
                        }, enabled = !audio.isVolumeFixed)
                    if (audio.isVolumeFixed) Text("Use the head unit's volume controls.")
                    Button(onClick = {
                        runCatching {
                            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                            if (current > 0) { beforeMute = current; audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) }
                            else audio.setStreamVolume(AudioManager.STREAM_MUSIC, beforeMute.coerceAtMost(maxVolume), 0)
                            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                        }.onFailure { message = "Volume is controlled by your head unit." }
                    }, enabled = !audio.isVolumeFixed) { Text(if (volume == 0f) "Unmute" else "Mute") }
                    var brightness by remember(settings.launcherBrightness) { mutableFloatStateOf(settings.launcherBrightness.takeIf { it >= 0 } ?: 0.5f) }
                    Text("Launcher brightness · ${if (settings.launcherBrightness < 0) "System" else "${(brightness * 100).roundToInt()}%"}")
                    Slider(brightness, { brightness = it }, valueRange = 0.05f..1f,
                        onValueChangeFinished = { onSettings { copy(launcherBrightness = brightness) } })
                    TextButton(onClick = { onSettings { copy(launcherBrightness = -1f) } }) { Text("Use system brightness") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val launch = settings.navigationPackage.takeIf { it.isNotBlank() }?.let { context.packageManager.getLaunchIntentForPackage(it) }
                            if (launch != null) runCatching { context.startActivity(launch) }.onFailure { message = "Navigation app unavailable." }
                            else open("https://www.google.com/maps")
                        }) { Text("Navigation") }
                        OutlinedButton(onClick = { runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }.onFailure { message = "Wi-Fi settings unavailable." } }) { Text("Wi-Fi settings") }
                    }
                }
                "Nearby" -> {
                    Text("Find a stop in Google Maps", fontSize = 20.sp)
                    Text("Search opens in the Maps app or browser. Choose a result there to navigate.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Gas stations", "Parking", "Coffee", "Restaurants", "EV charging", "Rest areas").forEach { category ->
                            OutlinedButton(onClick = { open("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("$category near me")) }, modifier = Modifier.heightIn(min = 56.dp)) { Text(category) }
                        }
                    }
                }
                "Trips" -> {
                    tripError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    val t = trips.current
                    Text(if (t.running) "Recording trip" else if (t.startedAtMs > 0) "Trip paused" else "Ready for a trip", fontSize = 20.sp)
                    Text(tripSummary(t, metric))
                    Text("Records while the launcher is visible. Stale GPS and sleep gaps are excluded. Restored trips start paused.", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onToggleTrip) { Text(if (t.running) "Pause" else "Start / Resume") }
                        OutlinedButton(onClick = onFinishTrip, enabled = t.startedAtMs > 0) { Text("Save trip") }
                    }
                    Text("Saved trips (${trips.history.size}/200)", fontSize = 18.sp)
                    if (trips.history.isEmpty()) Text("Saved trips will appear here.")
                    var confirmClear by remember { mutableStateOf(false) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pendingExport = tripsCsv(trips.history); export.launch("openlauncher-trips.csv") }, enabled = trips.history.isNotEmpty()) { Text("Export CSV") }
                        TextButton(onClick = { confirmClear = true }, enabled = trips.history.isNotEmpty()) { Text("Clear history") }
                    }
                    if (confirmClear) {
                        Text("Delete all saved trips? Your current trip is kept.")
                        Row { TextButton(onClick = { onClearTrips(); confirmClear = false }) { Text("Delete history") }; TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
                    }
                    trips.history.forEach { trip ->
                        HorizontalDivider()
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(trip.startedAtMs)))
                        Text(tripSummary(trip, metric))
                    }
                }
                "Weather" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Local forecast", Modifier.weight(1f), fontSize = 20.sp)
                        OutlinedButton(onClick = onRefreshWeather, enabled = location != null && internet) { Text("Refresh") }
                    }
                    when {
                        location == null -> Text("Waiting for GPS location.")
                        !internet -> Text("Offline. Showing the last available forecast.")
                        weatherError != null -> Text("Could not refresh weather. Try again.")
                        weather == null -> Text("Loading forecast…")
                    }
                    weather?.let { w ->
                        Text("${w.temperatureDisplay(metric)} · ${w.conditionLabel}", fontSize = 24.sp)
                        Text("High ${temperatureText(w.highCelsius, metric)}   Low ${temperatureText(w.lowCelsius, metric)}")
                        val hours = w.hourly.filter { it.timeMs >= System.currentTimeMillis() - 3_600_000 }.take(12)
                        Text("Feels like ${temperatureText(hours.firstOrNull()?.feelsLikeCelsius, metric)}")
                        Text("Updated ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(w.updatedAtMs))}", fontSize = 13.sp)
                        Text("Next 12 hours · temperature / rain chance", fontSize = 17.sp)
                        val formatter = remember(settings.use12HourTime, w.utcOffsetSeconds) {
                            SimpleDateFormat(if (settings.use12HourTime) "h a" else "HH:mm", Locale.getDefault()).apply {
                                timeZone = SimpleTimeZone((w.utcOffsetSeconds * 1000).toInt(), "Forecast")
                            }
                        }
                        hours.forEach { hour ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatter.format(Date(hour.timeMs)))
                                Text(temperatureText(hour.celsius, metric))
                                Text(hour.rainPercent?.let { "$it%" } ?: "Unavailable")
                            }
                        }
                        if (hours.isEmpty()) Text("Hourly forecast expired. Refresh when connected.")
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subtle weather background", Modifier.weight(1f))
                        Switch(settings.weatherBackground, { onSettings { copy(weatherBackground = it) } })
                    }
                    Text("Optional tint behind your existing dashboard. Off by default.", fontSize = 13.sp)
                    TextButton(onClick = { open("https://open-meteo.com/") }) { Text("Weather by Open-Meteo") }
                }
                "Diagnostics" -> {
                    val connected by MediaListenerService.isConnected.collectAsState()
                    val sessions by MediaListenerService.sessions.collectAsState()
                    val map by DashboardDiagnostics.map.collectAsState()
                    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
                    LaunchedEffect(Unit) { while (true) { now = SystemClock.elapsedRealtime(); delay(1000) } }
                    val gpsPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val locationEnabled = androidx.core.location.LocationManagerCompat.isLocationEnabled(context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager)
                    val webView = androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)
                    val lines = listOf(
                        "App" to "$installed (${context.packageName})",
                        "Device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}",
                        "GPS permission" to if (gpsPermission) "Allowed" else "Missing",
                        "Device location" to if (locationEnabled) "On" else "Off",
                        "GPS fix" to (location?.let { "${((now - it.elapsedRealtimeMs).coerceAtLeast(0) / 1000)} s old · ±${it.accuracy.toInt()} m" } ?: "Waiting for signal"),
                        "Internet" to if (internet) "Connected and validated" else "Unavailable / not validated",
                        "Media access" to if (connected) "Connected · ${sessions.size} sessions" else "Disconnected",
                        "Selected player" to settings.preferredMediaPackage.ifBlank { "Automatic" },
                        "WebView" to (webView?.let { "${it.packageName} ${it.versionName}" } ?: "Unavailable"),
                        "Map" to if (!settings.onlineMapEnabled) "Online map disabled" else map.status,
                        "Map status time" to if (map.updatedAtMs > 0) DateFormat.getTimeInstance().format(Date(map.updatedAtMs)) else "Not opened",
                        "Weather" to (weatherError?.let { "Refresh failed" } ?: if (weather != null) "Available" else "Waiting"))
                    lines.forEach { (name, value) -> Text("$name: $value") }
                    Text("Export contains these status details only, without coordinates, destinations or media titles.", fontSize = 13.sp)
                    OutlinedButton(onClick = { pendingExport = lines.joinToString("\n") { "${it.first}: ${it.second}" }; export.launch("openlauncher-diagnostics.txt") }) { Text("Export diagnostics") }
                }
                "Updates" -> {
                    var loading by remember { mutableStateOf(false) }
                    var result by remember { mutableStateOf<ReleaseUpdate?>(null) }
                    Text("Installed: $installed", fontSize = 20.sp)
                    Text("Checks GitHub for previews that passed the build and unit-test workflow. Installation stays under your control.")
                    Button(onClick = {
                        loading = true; message = null
                        scope.launch {
                            try { result = ReleaseUpdates.latest(); message = if (newerVersion(result!!.version, installed)) "A newer preview is available." else "No newer version found." }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (e: Exception) { message = e.message ?: "Update check failed. Try again." }
                            finally { loading = false }
                        }
                    }, enabled = !loading && internet) { Text(if (loading) "Checking…" else "Check for updates") }
                    if (!internet) Text("Connect to Wi-Fi to check for updates.")
                    result?.let { r ->
                        Text(r.version, fontSize = 20.sp)
                        Text(r.notes.ifBlank { "No release notes provided." })
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { open(r.pageUrl) }) { Text("Release notes and checksum") }
                            OutlinedButton(onClick = { open(r.apkUrl) }) { Text("Download APK") }
                        }
                    }
                }
            }
        }
    }
}

private fun tripSummary(t: TripRecord, metric: Boolean): String {
    val distance = t.distanceMeters / if (metric) 1000 else 1609.344
    val speed = t.averageMps * if (metric) 3.6 else 2.236936
    val seconds = (t.driveSeconds + t.idleSeconds).toLong()
    return "%.2f %s · %d:%02d:%02d · avg %.1f %s".format(distance, if (metric) "km" else "mi",
        seconds / 3600, seconds / 60 % 60, seconds % 60, speed, if (metric) "km/h" else "mph")
}
