package com.openlauncher.app.ui.widget

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.obd.ObdReading
import com.openlauncher.app.obd.ObdSession
import com.openlauncher.app.obd.PairedAdapter
import com.openlauncher.app.obd.hasBluetoothPermission
import com.openlauncher.app.obd.pairedAdapters
import com.openlauncher.app.ui.components.SettingsDialog
import kotlin.math.roundToInt

/** Live engine data from a paired ELM327 Bluetooth adapter. Nothing connects until an adapter is chosen. */
@Composable
fun ObdWidget(
    adapterAddress: String,
    adapterName: String,
    metric: Boolean,
    accent: Color,
    isDayMode: Boolean,
    isEditing: Boolean,
    onSelectAdapter: (address: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permitted by remember { mutableStateOf(hasBluetoothPermission(context)) }
    var picking by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = granted
        if (granted) picking = true
    }
    DisposableEffect(adapterAddress, permitted, isEditing) {
        if (adapterAddress.isNotEmpty() && permitted && !isEditing) ObdSession.start(context, adapterAddress)
        onDispose { ObdSession.stop() }
    }
    val session by ObdSession.state.collectAsState()
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dim = contentColor.copy(alpha = 0.6f)

    if (picking) AdapterPickerDialog(
        adapters = remember { pairedAdapters(context) },
        onPick = { adapter -> picking = false; onSelectAdapter(adapter.address, adapter.name) },
        onDismiss = { picking = false }
    )
    fun choose() {
        if (permitted) picking = true
        else if (Build.VERSION.SDK_INT >= 31) permissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
    }

    BoxWithConstraints(modifier.padding(12.dp)) {
        val roomy = maxHeight >= 200.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (session.status == ObdSession.Status.CONNECTED) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    null, Modifier.size(18.dp), tint = if (session.status == ObdSession.Status.CONNECTED) accent else dim)
                Text(
                    when {
                        adapterAddress.isEmpty() -> "OBD2 adapter not chosen"
                        else -> adapterName.ifBlank { adapterAddress }
                    },
                    Modifier.weight(1f).padding(start = 8.dp), fontSize = 12.sp, color = contentColor, maxLines = 1
                )
                if (!isEditing) TextButton(onClick = ::choose, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (adapterAddress.isEmpty()) "Choose" else "Change", fontSize = 12.sp)
                }
            }
            when {
                adapterAddress.isEmpty() -> Text(
                    "Pair an ELM327 Bluetooth adapter in Android settings, then choose it here.", fontSize = 12.sp, color = dim)
                !permitted -> Box(Modifier.fillMaxWidth().clickable(enabled = !isEditing) { choose() }) {
                    Text("Tap to allow Bluetooth access.", fontSize = 12.sp, color = dim)
                }
                session.status != ObdSession.Status.CONNECTED -> Text(
                    session.message.ifBlank { "Waiting for adapter…" }, fontSize = 12.sp, color = dim)
                else -> ObdValues(session.reading, metric, roomy, contentColor, dim)
            }
            if (session.status == ObdSession.Status.CONNECTED && session.message.contains("off")) {
                Text(session.message, fontSize = 11.sp, color = dim)
            }
        }
    }
}

@Composable
private fun ObdValues(reading: ObdReading, metric: Boolean, roomy: Boolean, color: Color, dim: Color) {
    fun temp(celsius: Int?): String = celsius?.let { if (metric) "$it°C" else "${(it * 9f / 5f + 32f).roundToInt()}°F" } ?: "—"
    fun pct(value: Int?): String = value?.let { "$it%" } ?: "—"
    val speed = reading.speedKmh?.let { if (metric) "$it km/h" else "${(it * 0.621371f).roundToInt()} mph" } ?: "—"
    val cells = buildList {
        add("RPM" to (reading.rpm?.toString() ?: "—"))
        add("COOLANT" to temp(reading.coolantC))
        add("BATTERY" to (reading.batteryVolts?.let { "%.1f V".format(it) } ?: "—"))
        add("SPEED" to speed)
        if (roomy) {
            add("THROTTLE" to pct(reading.throttlePct))
            add("LOAD" to pct(reading.loadPct))
            add("FUEL" to pct(reading.fuelPct))
            add("INTAKE" to temp(reading.intakeC))
        }
    }
    cells.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = dim)
                    Text(value, fontSize = 22.sp, color = color, maxLines = 1)
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdapterPickerDialog(adapters: List<PairedAdapter>, onPick: (PairedAdapter) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    SettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose OBD2 adapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (adapters.isEmpty()) Text("No paired Bluetooth devices. Pair the adapter in Android Bluetooth settings first.")
                adapters.forEach { adapter ->
                    TextButton(onClick = { onPick(adapter) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(adapter.name)
                            Text(adapter.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open Bluetooth settings") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
