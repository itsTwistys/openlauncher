package com.openlauncher.app.ui.widget

import com.openlauncher.app.ui.components.SettingsFilledButton as Button
import com.openlauncher.app.ui.components.SettingsOutlinedButton as OutlinedButton
import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.SoundPadConfig

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundboardWidget(
    pads: List<SoundPadConfig>,
    accent: Color,
    isDayMode: Boolean = false,
    isEditing: Boolean = false,
    onUpdatePad: (index: Int, pad: SoundPadConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor     = if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val borderColor  = if (isDayMode) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)

    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var assigningIndex by remember { mutableStateOf<Int?>(null) }

    val safePads = remember(pads) {
        if (pads.size >= 6) pads.take(6)
        else pads + List(6 - pads.size) { SoundPadConfig("+", synthType = "") }
    }

    // Outer grid Column with top padding = 22.dp to leave room for card header label "SOUNDBOARD"
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { col ->
                    val idx = row * 3 + col
                    val pad = safePads[idx]
                    val isActive = activePadIndex == idx
                    val hasCustomAudio = pad.audioUri.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                1.dp,
                                if (isActive) accent else borderColor,
                                RoundedCornerShape(3.dp)
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .then(
                                if (!isEditing) Modifier.combinedClickable(
                                    onClick = {
                                        if (pad.label == "+" || (pad.audioUri.isEmpty() && pad.synthType.isEmpty())) {
                                            assigningIndex = idx
                                        } else {
                                            activePadIndex = idx
                                            playSoundPad(
                                                context = context,
                                                pad = pad,
                                                onDone = { activePadIndex = null }
                                            )
                                        }
                                    },
                                    onLongClick = { assigningIndex = idx }
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val isPlus = pad.label == "+"
                        Text(
                            text = pad.label,
                            color = if (isActive) accent else if (isPlus) dimColor else contentColor,
                            fontSize = if (isPlus) 16.sp else 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    assigningIndex?.let { idx ->
        PadAssignDialog(
            pad = safePads[idx],
            accent = accent,
            isDayMode = isDayMode,
            onDismiss = { assigningIndex = null },
            onSave = { updated ->
                onUpdatePad(idx, updated)
                assigningIndex = null
            }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PadAssignDialog(
    pad: SoundPadConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (SoundPadConfig) -> Unit
) {
    val context    = LocalContext.current

    var labelText   by remember { mutableStateOf(pad.label) }
    var synthType   by remember { mutableStateOf(pad.synthType) }
    var audioUri    by remember { mutableStateOf(pad.audioUri) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioUri = uri.toString()
            val rawName = uri.path?.substringAfterLast('/') ?: "custom_sound"
            labelText = rawName.substringAfterLast(':').substringBeforeLast('.')
        }
    }

    com.openlauncher.app.ui.components.SettingsDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign pad sound") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(labelText, { if (it.length <= 12) labelText = it },
                    label = { Text("Pad label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Preloaded audio", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("mario_jump" to "Mario jump", "mario_coin" to "Mario coin",
                        "boom" to "Boom", "loud_fart" to "Fart").forEach { (type, name) ->
                        FilterChip(synthType == type && audioUri.isEmpty(),
                            { synthType = type; audioUri = ""; labelText = name }, label = { Text(name) })
                    }
                }
                OutlinedButton(onClick = { filePicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.AudioFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (audioUri.isNotEmpty()) "Change audio file" else "Pick audio file")
                }
                if (audioUri.isNotEmpty()) {
                    Text(audioUri.substringAfterLast('/'), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { audioUri = "" }) { Text("Use preloaded audio") }
                }
                if (pad.label != "+" || pad.audioUri.isNotEmpty() || pad.synthType.isNotEmpty()) {
                    TextButton(onClick = { onSave(SoundPadConfig(label = "+", audioUri = "", synthType = "")) }) {
                        Text("Clear sound", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(SoundPadConfig(
            label = labelText.trim().ifEmpty { pad.label }, audioUri = audioUri, synthType = synthType)) }) { Text("Save sound") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun playSoundPad(context: android.content.Context, pad: SoundPadConfig, onDone: () -> Unit) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mainHandler.post {
        try {
            if (pad.audioUri.isNotEmpty()) {
                val player = MediaPlayer()
                try {
                    player.setDataSource(context, android.net.Uri.parse(pad.audioUri))
                    player.setOnCompletionListener { mp ->
                        mp.release()
                        onDone()
                    }
                    // Async playback errors (revoked SAF grant, deleted file) must
                    // also release the player and un-highlight the pad
                    player.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        onDone()
                        true
                    }
                    // prepareAsync: a blocking prepare() on the main thread is an
                    // ANR risk for content URIs on slow head-unit storage
                    player.setOnPreparedListener { it.start() }
                    player.prepareAsync()
                } catch (e: Exception) {
                    // Release on synchronous failure — this leaked a native player per failed tap
                    runCatching { player.release() }
                    throw e
                }
            } else {
                var resName = pad.synthType.lowercase().trim()
                var resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                if (resId == 0) {
                    // Fallback mapping for legacy synth types saved in user settings
                    resName = when (resName) {
                        "horn", "beep", "alert" -> "mario_jump"
                        "kick", "snare", "bass" -> "mario_coin"
                        else -> "mario_jump"
                    }
                    resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                }
                if (resId != 0) {
                    val player = MediaPlayer.create(context, resId)
                    if (player != null) {
                        player.setOnCompletionListener { mp ->
                            mp.release()
                            onDone()
                        }
                        player.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            onDone()
                            true
                        }
                        player.start()
                    } else {
                        onDone()
                    }
                } else {
                    onDone()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Soundboard", "Error playing pad", e)
            onDone()
        }
    }
}
