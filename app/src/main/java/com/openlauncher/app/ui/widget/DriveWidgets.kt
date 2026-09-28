package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectivityWidget(isWifi: Boolean, isData: Boolean, internetValidated: Boolean, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.clickable(enabled = enabled) {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }.padding(16.dp), verticalArrangement = Arrangement.Center) {
        Icon(if (isWifi) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null)
        Spacer(Modifier.height(8.dp))
        Text(if (isWifi) "WIFI CONNECTED" else if (isData) "MOBILE DATA" else "OFFLINE", fontSize = 14.sp)
        Text(if (internetValidated) "Internet available" else if (isWifi || isData)
            "Internet not verified" else "No network connection", fontSize = 11.sp)
        Text("Tap for network settings", fontSize = 10.sp)
    }
}

@Composable
fun DestinationsWidget(home: String, work: String, recent: List<String>, preferredPackage: String,
                       onNavigate: (String) -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showRecent by remember { mutableStateOf(false) }
    fun navigate(destination: String) {
        val uri = if (preferredPackage == "com.waze")
            Uri.parse("https://waze.com/ul?q=" + Uri.encode(destination) + "&navigate=yes")
        else Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination) +
            "&travelmode=driving&dir_action=navigate")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (preferredPackage.isNotBlank()) setPackage(preferredPackage)
        }
        val opened = runCatching { context.startActivity(intent) }.isSuccess ||
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
        if (opened) onNavigate(destination)
        else android.widget.Toast.makeText(context, "Install a maps app or browser", android.widget.Toast.LENGTH_SHORT).show()
    }
    Column(modifier.padding(8.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.Center) {
        listOf("Home" to home, "Work" to work).forEach { (label, destination) ->
            TextButton(onClick = { navigate(destination) }, enabled = enabled && destination.isNotBlank()) {
                Icon(if (label == "Home") Icons.Default.Home else Icons.Default.Work,
                    contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (destination.isBlank()) "Set $label in Settings" else "Navigate $label", fontSize = 12.sp, maxLines = 1)
            }
        }
        if (recent.isNotEmpty()) {
            Box {
                TextButton(enabled = enabled, onClick = { showRecent = true }) { Text("Recent destinations", fontSize = 11.sp) }
                DropdownMenu(expanded = showRecent, onDismissRequest = { showRecent = false }) {
                    recent.forEach { place -> DropdownMenuItem(text = { Text(place, maxLines = 2) },
                        onClick = { showRecent = false; navigate(place) }) }
                }
            }
        }
    }
}

@Composable
fun RadarWidget(enabled: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.clickable(enabled = enabled) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.rainviewer.com/weather-radar-map-live.html"))) }
        }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Grain, contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text("OPEN LIVE RADAR", fontSize = 12.sp)
        Text("Uses WiFi • opens RainViewer", fontSize = 9.sp)
    }
}

@Composable
fun TrafficWidget(location: com.openlauncher.app.util.LocationData?, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.clickable(enabled = enabled) {
            val center = if (location == null) "" else
                "&center=" + location.latitude + "%2C" + location.longitude + "&zoom=13"
            val url = Uri.parse("https://www.google.com/maps/@?api=1&map_action=map&layer=traffic" + center)
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
        }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Traffic, contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text("OPEN LIVE TRAFFIC", fontSize = 12.sp)
        Text("ETA available in maps", fontSize = 9.sp)
    }
}
