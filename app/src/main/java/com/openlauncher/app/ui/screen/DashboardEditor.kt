package com.openlauncher.app.ui.screen

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openlauncher.app.data.*
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DashboardPanel(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f), shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title.uppercase(), modifier = Modifier.weight(1f), fontSize = 18.sp)
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
                }
                HorizontalDivider()
                content()
            }
        }
    }
}

/** Draw each spanning widget once, in the same proportions as the dashboard. */
@Composable
internal fun LayoutPreview(layout: List<WidgetConfig>, ids: Set<String>, selected: String? = null,
                           onSelect: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.7f).border(1.dp, MaterialTheme.colorScheme.outline)) {
        val cw = maxWidth / GRID_COLS; val ch = maxHeight / GRID_ROWS
        for (w in layout.filter { it.enabled && it.id in ids }) {
            Box(Modifier.offset(cw * w.gridX, ch * w.gridY).size(cw * w.spanX, ch * w.spanY).padding(3.dp)
                .background(accent.copy(alpha = if (selected == w.id) 0.22f else 0.06f))
                .border(1.dp, if (selected == w.id) accent else MaterialTheme.colorScheme.outline)
                .clickable { onSelect(w.id) }.padding(6.dp), contentAlignment = Alignment.Center) {
                Text(ALL_WIDGET_TYPES.firstOrNull { it.id == w.id }?.label ?: w.id, fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardEditor(settings: AppSettings, onApply: (LayoutProfile) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(settings) }
    var selected by remember { mutableStateOf(settings.activeWidgetIds().firstOrNull()) }
    var tab by remember { mutableStateOf("Widgets") }
    var notice by remember { mutableStateOf("Select a widget to resize or move it.") }
    var confirmDefaults by remember { mutableStateOf(false) }
    val ids = draft.activeWidgetIds()
    val active = draft.widgetLayout.filter { it.enabled && it.id in ids }
    val config = active.firstOrNull { it.id == selected }
    DashboardPanel("Edit Dashboard", onDismiss) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Preview changes before applying", Modifier.weight(1f), fontSize = 13.sp)
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = { onApply(LayoutProfile("", active, ids.toList())); onDismiss() },
                enabled = validWidgetLayout(active)) { Text("Apply") }
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth >= 620.dp
            val preview: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth()) {
                    LayoutPreview(active, ids, selected, { selected = it })
                    Text(notice, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    if (config != null) {
                        Text(ALL_WIDGET_TYPES.first { it.id == config.id }.label, fontSize = 15.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (h in 1..GRID_ROWS) for (w in 1..GRID_COLS) {
                                FilterChip(selected = config.spanX == w && config.spanY == h,
                                    enabled = draft.resizePreview(config.id, w, h) != null,
                                    onClick = { draft.resizePreview(config.id, w, h)?.let { draft = draft.copy(widgetLayout = it) } },
                                    label = { Text("$w × $h") })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(Icons.Default.ArrowBack to (-1 to 0), Icons.Default.ArrowForward to (1 to 0),
                                Icons.Default.ArrowUpward to (0 to -1), Icons.Default.ArrowDownward to (0 to 1)).forEach { (icon, delta) ->
                                val x = config.gridX + delta.first; val y = config.gridY + delta.second
                                IconButton(onClick = { draft = draft.copy(widgetLayout = computeWidgetMove(active, config.id, x, y)) },
                                    enabled = x >= 0 && y >= 0 && x + config.spanX <= GRID_COLS && y + config.spanY <= GRID_ROWS) {
                                    Icon(icon, "Move ${if (delta.first < 0) "left" else if (delta.first > 0) "right" else if (delta.second < 0) "up" else "down"}")
                                }
                            }
                            TextButton(onClick = { draft = draft.withWidgetVisibility(ids - config.id); selected = null }) { Text("Remove") }
                        }
                    }
                    if (confirmDefaults) {
                        Text("Replace the draft with the default dashboard?", fontSize = 13.sp)
                        Row {
                            TextButton(onClick = { draft = draft.withDefaultDashboard(); confirmDefaults = false }) { Text("Restore") }
                            TextButton(onClick = { confirmDefaults = false }) { Text("Cancel") }
                        }
                    } else TextButton(onClick = { confirmDefaults = true }) { Text("Restore default") }
                }
            }
            val picker: @Composable () -> Unit = {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Widgets", "Recovery").forEach { t -> FilterChip(tab == t, { tab = t }, label = { Text(t) }) }
                    }
                    if (tab == "Widgets") LazyVerticalGrid(GridCells.Adaptive(140.dp), Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ALL_WIDGET_TYPES, key = { it.id }) { info ->
                            val enabled = info.id in ids
                            Column(Modifier.fillMaxWidth().heightIn(min = 112.dp)
                                .border(1.dp, if (selected == info.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                .background(if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    if (enabled) selected = info.id else {
                                        val next = draft.withAddedWidget(info.id)
                                        notice = if (next == draft) "Dashboard full. Remove a widget first." else if (active.sumOf { it.spanX * it.spanY } >= 6)
                                            "Cards were reduced in this preview to make room. Apply to keep changes." else "Widget added to preview."
                                        draft = next; selected = info.id
                                    }
                                }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(info.icon, null, Modifier.size(24.dp))
                                Text(info.label, fontSize = 13.sp)
                                Text(if (enabled) "ADDED · TAP TO EDIT" else "ADD", fontSize = 11.sp)
                            }
                        }
                    } else androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (settings.layoutHistory.isEmpty()) item { Text("Previous layouts appear here after your next change.") }
                        items(settings.layoutHistory.size) { i ->
                            val saved = settings.layoutHistory[i]
                            Column {
                                Text(saved.name.toLongOrNull()?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) } ?: "Previous layout")
                                LayoutPreview(saved.layout, saved.enabledIds.toSet(), onSelect = {
                                    draft = draft.copy(widgetLayout = saved.layout).withWidgetVisibility(saved.enabledIds.toSet()); notice = "Recovered layout preview. Apply to keep it."
                                })
                                TextButton(onClick = { draft = draft.copy(widgetLayout = saved.layout).withWidgetVisibility(saved.enabledIds.toSet()); notice = "Recovered layout preview. Apply to keep it." }) { Text("Preview this layout") }
                            }
                        }
                    }
                }
            }
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).verticalScrollCompat()) { preview() }
                Box(Modifier.weight(1f)) { picker() }
            } else Column {
                Box(Modifier.weight(1f).verticalScrollCompat()) { preview() }
                Box(Modifier.weight(1f)) { picker() }
            }
        }
    }
}

@Composable
private fun Modifier.verticalScrollCompat() = this.then(Modifier.verticalScroll(rememberScrollState()))
