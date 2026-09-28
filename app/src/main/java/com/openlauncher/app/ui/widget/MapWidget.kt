package com.openlauncher.app.ui.widget

import android.content.Intent
import com.openlauncher.app.util.LocationData
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun MapWidget(location: LocationData?, isEditing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clickable(enabled = !isEditing) {
                // Location is passed to the installed map app only after the user taps.
                val uri = if (location == null) Uri.parse("geo:0,0") else
                    Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f", location.latitude,
                        location.longitude, location.latitude, location.longitude))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                runCatching { context.startActivity(intent) }
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(if (location == null) "OPEN MAPS" else "OPEN MAP AT CURRENT LOCATION", fontSize = 11.sp)
        if (location == null) Text("Waiting for GPS", fontSize = 9.sp)
    }
}
