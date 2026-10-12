package com.openlauncher.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.EventTrigger
import com.openlauncher.app.data.KeyBinding
import com.openlauncher.app.data.LauncherCommand
import com.openlauncher.app.data.MAX_EVENT_TRIGGERS
import com.openlauncher.app.data.MAX_KEY_BINDINGS
import com.openlauncher.app.data.isValidEventAction
import com.openlauncher.app.ui.components.SettingsRow
import com.openlauncher.app.ui.components.SettingsSection

@Composable
private fun switchColors(accent: Color) = SwitchDefaults.colors(
    checkedTrackColor = accent, checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline)

/**
 * Optional automation. Every control here is off or empty by default, and none of it locks anything:
 * the launcher never hides or disables a feature because the car is moving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AutomationSettings(
    settings: AppSettings,
    accent: Color,
    lastKeyCode: Int?,
    onChooseCameraApp: () -> Unit,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit
) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    SettingsSection("Automatic layouts") {
        val haveDriving = settings.layoutProfiles.any { it.name == "Driving" }
        val haveParked = settings.layoutProfiles.any { it.name == "Parked" }
        SettingsRow(
            label = "Switch Driving and Parked layouts",
            sublabel = if (haveDriving || haveParked) "Uses your saved Driving and Parked layouts" else "Save a Driving and a Parked layout first (Widget Layout Profiles)",
            icon = Icons.Default.DirectionsCar
        ) {
            Switch(
                checked = settings.autoDrivingProfiles,
                onCheckedChange = { on -> onUpdate { copy(autoDrivingProfiles = on, autoDayNightProfiles = if (on) false else autoDayNightProfiles) } },
                colors = switchColors(accent)
            )
        }
        Text(
            "Off by default. Driving applies after about 5 seconds above 10 km/h; Parked after the delay below at a standstill. " +
                "Traffic lights do not trigger a switch. Turning this on turns off the Day/Night layout switch.",
            style = MaterialTheme.typography.bodySmall
        )
        SettingsRow(label = "Parked after", sublabel = "${settings.parkedAfterMinutes} min stopped", icon = Icons.Default.Sync) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 5, 10, 15, 30).forEach { minutes ->
                    FilterChip(selected = settings.parkedAfterMinutes == minutes,
                        onClick = { onUpdate { copy(parkedAfterMinutes = minutes) } },
                        label = { Text("$minutes") })
                }
            }
        }
    }

    SettingsSection("Trip recording") {
        SettingsRow(
            label = "Record trips in the background",
            sublabel = "Keeps counting while another app, such as navigation, is on screen",
            icon = Icons.Default.Route
        ) {
            Switch(
                checked = settings.recordTripsInBackground,
                onCheckedChange = { on ->
                    onUpdate { copy(recordTripsInBackground = on) }
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                colors = switchColors(accent)
            )
        }
        Text(
            "Off by default. While a trip is running, a small ongoing notification keeps the launcher alive and GPS on. " +
                "Pause or save the trip to stop it. No route is stored, only totals.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    SettingsSection("Camera and event triggers") {
        SettingsRow(
            label = "Camera app",
            sublabel = settings.cameraPackage.ifBlank { "Not chosen" },
            icon = Icons.Default.PhotoCamera
        ) {
            TextButton(onClick = onChooseCameraApp, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose") }
            if (settings.cameraPackage.isNotBlank()) {
                TextButton(onClick = { onUpdate { copy(cameraPackage = "") } }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear") }
            }
        }
        Text(
            "Head units announce events such as reverse gear as Android broadcasts. Add the exact broadcast action from your unit's " +
                "documentation, then pick what the launcher should do. On Android 10 or newer, allowing Draw Over Other Apps lets the camera open from the background.",
            style = MaterialTheme.typography.bodySmall
        )
        settings.eventTriggers.forEachIndexed { index, trigger ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingsRow(
                label = trigger.action,
                sublabel = LauncherCommand.from(trigger.command)?.label ?: trigger.command,
                icon = Icons.Default.SettingsRemote
            ) {
                IconButton(onClick = { onUpdate { copy(eventTriggers = eventTriggers.filterIndexed { i, _ -> i != index }) } },
                    modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Remove trigger", tint = MaterialTheme.colorScheme.error) }
            }
        }
        if (settings.eventTriggers.size < MAX_EVENT_TRIGGERS) {
            var action by remember { mutableStateOf("") }
            var command by remember { mutableStateOf(LauncherCommand.OPEN_CAMERA) }
            val valid = isValidEventAction(action.trim())
            OutlinedTextField(
                value = action, onValueChange = { action = it.take(200) }, singleLine = true,
                label = { Text("Broadcast action, e.g. com.vendor.REVERSE_ON") },
                isError = action.isNotBlank() && !valid,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LauncherCommand.entries.forEach { option ->
                    FilterChip(selected = command == option, onClick = { command = option }, label = { Text(option.label) })
                }
            }
            TextButton(
                onClick = {
                    val trimmed = action.trim()
                    onUpdate {
                        copy(eventTriggers = (eventTriggers.filterNot { it.action == trimmed } + EventTrigger(trimmed, command.name)).take(MAX_EVENT_TRIGGERS))
                    }
                    action = ""
                },
                enabled = valid,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Add trigger") }
        }
    }

    SettingsSection("Steering-wheel keys") {
        SettingsRow(
            label = "Use steering-wheel keys",
            sublabel = "Map hardware keys that reach the launcher to actions",
            icon = Icons.Default.Keyboard
        ) {
            Switch(
                checked = settings.steeringKeysEnabled,
                onCheckedChange = { on -> onUpdate { copy(steeringKeysEnabled = on) } },
                colors = switchColors(accent)
            )
        }
        Text(
            "Off by default. Only keys delivered to the launcher while it is on screen are seen; many head units send these to the active media app instead. " +
                "Press a steering-wheel button, then bind it below.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            if (lastKeyCode == null) "Key tester: no key seen yet" else "Key tester: ${KeyEvent.keyCodeToString(lastKeyCode)} ($lastKeyCode)",
            style = MaterialTheme.typography.bodyMedium
        )
        var command by remember { mutableStateOf(LauncherCommand.MEDIA_NEXT) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LauncherCommand.entries.forEach { option ->
                FilterChip(selected = command == option, onClick = { command = option }, label = { Text(option.label) })
            }
        }
        val canBind = lastKeyCode != null && lastKeyCode > 0 &&
            (settings.steeringKeys.size < MAX_KEY_BINDINGS || settings.steeringKeys.any { it.keyCode == lastKeyCode })
        TextButton(
            onClick = {
                val key = lastKeyCode ?: return@TextButton
                onUpdate { copy(steeringKeys = (steeringKeys.filterNot { it.keyCode == key } + KeyBinding(key, command.name)).take(MAX_KEY_BINDINGS)) }
            },
            enabled = canBind,
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text("Bind last key to ${command.label}") }
        settings.steeringKeys.forEachIndexed { index, binding ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${KeyEvent.keyCodeToString(binding.keyCode)} → ${LauncherCommand.from(binding.command)?.label ?: binding.command}",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = { onUpdate { copy(steeringKeys = steeringKeys.filterIndexed { i, _ -> i != index }) } },
                    modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Remove binding", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
