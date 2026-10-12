package com.openlauncher.app.ui.screen

import com.openlauncher.app.ui.components.SettingsDialog
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.openlauncher.app.data.resizePreview
import com.openlauncher.app.data.activeWidgetIds
import com.openlauncher.app.data.LayoutProfile
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.data.computeWidgetMove
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.WidgetConfig
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.widget.*
import java.util.Calendar
import com.openlauncher.app.util.LocationData

// Dialogs for the dashboard's long-press widget menu and resize preview, split out of HomeScreen.kt.

@Composable
internal fun WidgetContextMenu(
    widgetId: String,
    accent: Color,
    clockStyle: ClockStyle,
    vitalsAsBars: Boolean,
    speedometerDigitalOnly: Boolean,
    carPlayPackage: String = "",
    androidAutoPackage: String = "",
    pipAppPackage: String = "",
    isDayMode: Boolean,
    onResize: () -> Unit,
    onArrange: () -> Unit,
    onRemove: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit,
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    com.openlauncher.app.ui.components.SettingsPanel("Widget options", onDismiss) {
            val inactiveMenuTint = MaterialTheme.colorScheme.onSurfaceVariant
            ContextRow("ARRANGE", Icons.Default.Dashboard, accent, onArrange, isDayMode = isDayMode)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ContextRow("RESIZE", Icons.Default.OpenWith, accent, onResize, isDayMode = isDayMode)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ContextRow("REMOVE WIDGET", Icons.Default.Delete, MaterialTheme.colorScheme.error, onRemove, isDayMode = isDayMode)
            if (widgetId == "CLOCK") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow(
                    label   = "DIGITAL",
                    icon    = Icons.Default.Schedule,
                    tint    = if (clockStyle == ClockStyle.DIGITAL) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.DIGITAL); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow(
                    label   = "ANALOG",
                    icon    = Icons.Default.Watch,
                    tint    = if (clockStyle == ClockStyle.ANALOG) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.ANALOG); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "VITALS") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow(
                    label   = "DIAL GAUGES",
                    icon    = Icons.Default.Adjust,
                    tint    = if (!vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow(
                    label   = "BARS VIEW",
                    icon    = Icons.Default.FormatAlignLeft,
                    tint    = if (vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "SPEEDOMETER") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow(
                    label   = "DIAL TRACK",
                    icon    = Icons.Default.Speed,
                    tint    = if (!speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow(
                    label   = "DIGITAL ONLY",
                    icon    = Icons.Default.Dialpad,
                    tint    = if (speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "NOW_PLAYING") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow("ASSIGN CARPLAY APP",      Icons.Default.PhoneAndroid,  accent, onAssignCarPlay, isDayMode = isDayMode)
                if (carPlayPackage.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ContextRow("CLEAR CARPLAY APP", Icons.Default.PhoneAndroid, MaterialTheme.colorScheme.error, onClearCarPlay, isDayMode = isDayMode)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ContextRow("ASSIGN ANDROID AUTO APP", Icons.Default.DirectionsCar, accent, onAssignAndroidAuto, isDayMode = isDayMode)
                if (androidAutoPackage.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ContextRow("CLEAR ANDROID AUTO APP", Icons.Default.DirectionsCar, MaterialTheme.colorScheme.error, onClearAndroidAuto, isDayMode = isDayMode)
                }
            }

    }
}

@Composable
internal fun ContextRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    isDayMode: Boolean = false
) {
    com.openlauncher.app.ui.components.SettingsAction(label, icon = icon, tint = tint, onClick = onClick)
}

@Composable
internal fun WidgetResizeDialog(
    config: WidgetConfig,
    settings: AppSettings,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (spanX: Int, spanY: Int) -> Unit
) {
    var spanX by remember { mutableStateOf(config.spanX) }
    var spanY by remember { mutableStateOf(config.spanY) }

    val preview = settings.resizePreview(config.id, spanX, spanY)
    val maxSpanX = GRID_COLS
    val maxSpanY = GRID_ROWS

    val dialogBg     = MaterialTheme.colorScheme.surface
    val dialogText   = MaterialTheme.colorScheme.onSurface
    val cancelColor  = MaterialTheme.colorScheme.onSurfaceVariant
    SettingsDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text          = config.id.replace('_', ' '),
                color         = dialogText,
                style = MaterialTheme.typography.bodyMedium,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("QUICK SIZES", color = dialogText, style = MaterialTheme.typography.bodyMedium, letterSpacing = 1.sp)
                (1..maxSpanY).forEach { height ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..maxSpanX).forEach { width ->
                            FilterChip(
                                enabled = settings.resizePreview(config.id, width, height) != null,
                                selected = spanX == width && spanY == height,
                                onClick = { spanX = width; spanY = height },
                                label = { Text("${width}×${height}", style = MaterialTheme.typography.bodyMedium) }
                            )
                        }
                    }
                }
                Text(if (preview == null) "This size does not fit. Remove or shrink another widget."
                    else "Preview: the highlighted cells show this widget. Disabled sizes need more room.",
                    color = dialogText.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                val target = preview?.firstOrNull { it.id == config.id }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(GRID_ROWS) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(GRID_COLS) { col ->
                                val highlighted = target != null && col in target.gridX until target.gridX + target.spanX &&
                                    row in target.gridY until target.gridY + target.spanY
                                Box(Modifier.size(width = 52.dp, height = 30.dp).background(
                                    if (highlighted) accent else dialogText.copy(alpha = 0.12f)))
                            }
                        }
                    }
                }
                SpanRow(label = "WIDTH",  value = spanX, min = 1, max = maxSpanX, accent = accent, isDayMode = isDayMode) { spanX = it }
                SpanRow(label = "HEIGHT", value = spanY, min = 1, max = maxSpanY, accent = accent, isDayMode = isDayMode) { spanY = it }
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null, onClick = { onConfirm(spanX, spanY) }) {
                Text("APPLY", color = accent, style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = cancelColor, style = MaterialTheme.typography.bodyMedium)
            }
        },
        containerColor    = dialogBg,
        titleContentColor = dialogText,
        textContentColor  = dialogText
    )
}

@Composable
internal fun SpanRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    accent: Color,
    isDayMode: Boolean,
    onChange: (Int) -> Unit
) {
    val textColor   = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor    = if (isDayMode) Color(0xFF495057) else Color(0xFFBFC7D2)
    val disabledC   = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF333333)
    val inactiveBg  = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF2A2A2A)
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text          = label,
            color         = dimColor,
            fontSize      = 10.sp,
            letterSpacing = 1.sp,
            modifier      = Modifier.width(52.dp)
        )
        IconButton(
            onClick  = { if (value > min) onChange(value - 1) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Remove, "Decrease $label",
                tint     = if (value > min) textColor else disabledC,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text      = "$value",
            color     = textColor,
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(24.dp)
        )
        IconButton(
            onClick  = { if (value < max) onChange(value + 1) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Add, "Increase $label",
                tint     = if (value < max) accent else disabledC,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(max) { i ->
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 10.dp)
                        .background(
                            if (i < value) accent.copy(alpha = 0.7f) else inactiveBg,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
