package com.openlauncher.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** The dashboard's flat, bordered surface, shared by every configuration window. */
@Composable
private fun SettingsWindow(onDismiss: () -> Unit, expanded: Boolean = false,
    color: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    val height = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = if (expanded) 1600.dp else 680.dp).fillMaxWidth(0.96f)
            .heightIn(max = height).then(if (expanded) Modifier.height(height) else Modifier).imePadding(),
            shape = RoundedCornerShape(4.dp), color = color, contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/** Scroll the body while keeping dismissal and confirmation reachable in landscape. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null, containerColor: Color = MaterialTheme.colorScheme.surface,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurface) {
    SettingsWindow(onDismissRequest, color = containerColor) {
        title?.let {
            CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) { it() }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)) {
            CompositionLocalProvider(LocalContentColor provides textContentColor) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text?.invoke() }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            dismissButton?.let { Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) { it() } }
            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) { confirmButton() }
        }
    }
}

@Composable
fun SettingsPanel(title: String, onDismiss: () -> Unit, expanded: Boolean = false,
    scrollable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    SettingsWindow(onDismiss, expanded) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(Modifier.weight(1f, fill = expanded).fillMaxWidth()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(top = 12.dp), content = content)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(Modifier.fillMaxWidth().padding(12.dp), content = content)
        }
    }
}

@Composable
fun SettingsRow(label: String, sublabel: String = "", icon: ImageVector,
    content: (@Composable RowScope.() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val controlWidth = maxWidth * 0.55f
        val stacked = maxWidth < 780.dp || LocalConfiguration.current.fontScale > 1.2f
        val description: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    if (sublabel.isNotEmpty()) Text(sublabel, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            description()
            if (content != null) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 36.dp),
                verticalAlignment = Alignment.CenterVertically, content = content)
        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) { description() }
            if (content != null) Row(Modifier.widthIn(max = controlWidth).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun SettingsAction(label: String, sublabel: String = "", icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (sublabel.isNotEmpty()) Text(sublabel, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
