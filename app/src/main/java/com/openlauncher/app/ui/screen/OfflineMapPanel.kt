package com.openlauncher.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openlauncher.app.data.MapTileCache
import com.openlauncher.app.data.OfflineMaps
import com.openlauncher.app.util.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val RADII = listOf(10, 25, 50)

/** Download or clear the area the map keeps for use without internet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OfflineMapPanel(location: LocationData?, networkAvailable: Boolean, mapEnabled: Boolean) {
    val context = LocalContext.current
    val progress by OfflineMaps.progress.collectAsState()
    var radius by remember { mutableIntStateOf(25) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(progress.done, progress.running) {
        cacheBytes = withContext(Dispatchers.IO) { MapTileCache.sizeBytes(context) }
        if (progress.running) delay(1_000)
    }
    val estimate = remember(location?.latitude, location?.longitude, radius) {
        location?.let { OfflineMaps.estimateTiles(it.latitude, it.longitude, radius) }
    }

    Text("Offline map", style = MaterialTheme.typography.bodyMedium)
    Text(
        "The embedded map already keeps what you view. Download an area to have it ready before you lose signal. " +
            "It uses the same OpenFreeMap data and sends the same tile requests as viewing the map, so the provider sees the area you save.",
    )
    if (!mapEnabled) Text("Turn on Settings → Online Map → Show Embedded Map to use the saved area.", color = MaterialTheme.colorScheme.error)
    Text("Saved on this device: %.1f MB".format(cacheBytes / 1_048_576.0))

    Text("Area around the current location", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RADII.forEach { km ->
            FilterChip(selected = radius == km, onClick = { radius = km }, label = { Text("$km km radius") }, enabled = !progress.running)
        }
    }
    val tooLarge = (estimate ?: 0) > OfflineMaps.MAX_TILES
    estimate?.let {
        if (tooLarge) Text("This radius needs too many files here. Choose a smaller one.", color = MaterialTheme.colorScheme.error)
        else Text("About $it files, roughly ${(it * 35L / 1024).coerceAtLeast(1)} MB.")
    }
    if (location == null) Text("Waiting for a GPS location.")
    if (!networkAvailable) Text("Connect to the internet to download.")

    if (progress.running) {
        LinearProgressIndicator(
            progress = { if (progress.total > 0) progress.done.toFloat() / progress.total else 0f },
            modifier = Modifier.fillMaxWidth()
        )
        Text("${progress.done} of ${progress.total} files")
        OutlinedButton(onClick = { OfflineMaps.cancel() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop download") }
    } else {
        OutlinedButton(
            onClick = { location?.let { OfflineMaps.start(context, it.latitude, it.longitude, radius) } },
            enabled = location != null && networkAvailable && !tooLarge,
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text("Download this area") }
    }
    if (progress.message.isNotBlank() && !progress.running) Text(progress.message)

    if (confirmClear) {
        Text("Delete all saved map data? The map will load from the internet again.")
        TextButton(onClick = {
            MapTileCache.clear(context); cacheBytes = 0L; confirmClear = false
        }) { Text("Delete saved map") }
        TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
    } else {
        TextButton(onClick = { confirmClear = true }, enabled = cacheBytes > 0 && !progress.running) { Text("Clear saved map data") }
    }
}
