package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectivityWidget(isWifi: Boolean, isData: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.clickable {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }.padding(16.dp), verticalArrangement = Arrangement.Center) {
        Icon(if (isWifi) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null)
        Spacer(Modifier.height(8.dp))
        Text(if (isWifi) "WIFI CONNECTED" else if (isData) "MOBILE DATA" else "OFFLINE", fontSize = 14.sp)
        Text("Tap for network settings", fontSize = 10.sp)
    }
}

@Composable
fun DestinationsWidget(home: String, work: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
        listOf("HOME" to home, "WORK" to work).forEach { (label, destination) ->
            TextButton(
                onClick = {
                    if (destination.isNotBlank()) {
                        // The address is shared with the selected navigation app only on tap.
                        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    }
                },
                enabled = destination.isNotBlank()
            ) {
                Icon(if (label == "HOME") Icons.Default.Home else Icons.Default.Work,
                    contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (destination.isBlank()) "Set $label in Settings" else label)
            }
        }
    }
}

@Composable
fun RadarWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.clickable {
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
fun TrafficWidget(location: com.openlauncher.app.util.LocationData?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.clickable {
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
