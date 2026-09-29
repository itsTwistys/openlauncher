package com.openlauncher.app.ui.screen

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

private val WIDGET_RADIUS = RoundedCornerShape(0.dp)

internal data class WidgetTypeInfo(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val description: String
)

internal val ALL_WIDGET_TYPES = listOf(
    WidgetTypeInfo("CLOCK",       "CLOCK + WEATHER", Icons.Default.AccessTime, "Time, date and local weather"),
    WidgetTypeInfo("NOW_PLAYING", "NOW PLAYING", Icons.Default.MusicNote,   "Media controls"),
    WidgetTypeInfo("TELEMETRY",   "COMPASS",     Icons.Default.Explore,     "Speed & heading"),
    WidgetTypeInfo("ALTIMETER",   "ALTIMETER",   Icons.Default.FlightTakeoff, "Roll, pitch & altitude"),
    WidgetTypeInfo("SPEEDOMETER", "SPEED",       Icons.Default.Speed,         "GPS speed"),
    WidgetTypeInfo("VITALS",      "VITALS",      Icons.Default.Dns,           "Head Unit Health / Vitals"),
    WidgetTypeInfo("TRIP_TRACKER", "TRIP TRACKER", Icons.Default.Map,          "Trip logs & stats"),
    WidgetTypeInfo("MAP",          "MAP",          Icons.Default.Place,        "Live map and navigation instructions"),
    WidgetTypeInfo("CONNECTIVITY", "CONNECTIVITY", Icons.Default.Wifi,       "Network status"),
    WidgetTypeInfo("DESTINATIONS", "DESTINATIONS", Icons.Default.Home,      "Home and work shortcuts"),
    WidgetTypeInfo("RADAR",        "RADAR",        Icons.Default.Grain,      "Open live weather radar"),
    WidgetTypeInfo("TRAFFIC",      "TRAFFIC",      Icons.Default.Traffic,    "Open live traffic and ETA"),
    WidgetTypeInfo("SOUNDBOARD",  "SOUNDBOARD",  Icons.Default.Piano,         "Custom sound pads")
)

private fun canAddWidget(settings: com.openlauncher.app.data.AppSettings): Boolean {
    val visibleIds = settings.activeWidgetIds()
    val activeWidgets = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
    val occupied = buildSet<Pair<Int, Int>> {
        activeWidgets.forEach { w ->
            for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy)
        }
    }
    val hasFreeCell = (0 until com.openlauncher.app.data.GRID_ROWS).any { r ->
        (0 until com.openlauncher.app.data.GRID_COLS).any { c -> (c to r) !in occupied }
    }
    // Also true if any active widget spans >1 cell and can be shrunk to make room
    val hasShrinkable = activeWidgets.any { it.spanX * it.spanY > 1 }
    return hasFreeCell || hasShrinkable
}

@Composable
fun HomeScreen(
    settings: AppSettings,
    trips: com.openlauncher.app.data.TripLog,
    tripError: String?,
    onToggleTrip: () -> Unit,
    onResetTrip: () -> Unit,
    onFinishTrip: () -> Unit,
    onClearTrips: () -> Unit,
    weatherError: String?,
    onRefreshWeather: () -> Unit,
    onSettings: (AppSettings.() -> AppSettings) -> Unit,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    mediaApps: List<com.openlauncher.app.model.AppInfo>,
    onSelectMedia: (String) -> Unit,
    onOpenSelectedMedia: () -> Unit,
    onMapOptions: (Boolean, Boolean) -> Unit,
    location: LocationData?,
    bearing: Float,
    internetValidated: Boolean,
    networkAvailable: Boolean,
    onApplyProfile: (String) -> Unit,
    onRestoreLayout: (LayoutProfile) -> Unit,
    onRestoreDefault: () -> Unit,
    onRememberDestination: (String) -> Unit,
    isWifi: Boolean,
    isData: Boolean,
    isDayMode: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onLaunchPip: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onUpdateWidget: (id: String, spanX: Int, spanY: Int) -> Unit,
    onMoveWidget: (id: String, gridX: Int, gridY: Int) -> Unit,
    onAddWidget: (id: String) -> Unit,
    onRemoveWidget: (id: String) -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit = {},
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit = {},
    onUpdateSoundPad: (index: Int, pad: com.openlauncher.app.data.SoundPadConfig) -> Unit = { _, _ -> },
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent       = Color(settings.accentColor)
    val gap          = 6.dp
    val hasWallpaper = settings.wallpaperUri.isNotEmpty()
    val widgetBg     = when {
        isDayMode    -> Color(0xFFFFFFFF)
        hasWallpaper -> Color(0xCC000000)
        else         -> Color.Black.copy(alpha = 0.35f)
    }
    val widgetBorder = when {
        isDayMode    -> Color(0xFFCCCCCC)
        hasWallpaper -> Color(0x22FFFFFF)
        else         -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    }
    val headerTextColor   = if (isDayMode) Color(0xFF111111) else accent
    val statusIconColor   = if (isDayMode) Color(0xFF444444) else Color(0xFFBFC7D2)
    val controlIconColor  = if (isDayMode) Color(0xFF666666) else Color(0xFFBFC7D2)

    var toolsPage by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedWidget by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = expandedWidget != null) { expandedWidget = null }
    LaunchedEffect(settings.activeWidgetIds()) {
        if (expandedWidget !in settings.activeWidgetIds()) expandedWidget = null
    }
    var resizingId    by remember { mutableStateOf<String?>(null) }
    var contextMenuId by remember { mutableStateOf<String?>(null) }

    val configuration    = LocalConfiguration.current
    val isLandscape      = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var editMode         by remember { mutableStateOf(false) }
    var widgetLibraryOpen by remember { mutableStateOf(false) }

    var profileMenu by remember { mutableStateOf(false) }
    var removedLayout by remember { mutableStateOf<LayoutProfile?>(null) }
    fun removeWithUndo(id: String) {
        removedLayout = LayoutProfile("Undo", settings.widgetLayout, settings.activeWidgetIds().toList())
        onRemoveWidget(id)
    }
    LaunchedEffect(removedLayout) {
        if (removedLayout != null) { kotlinx.coroutines.delay(8000); removedLayout = null }
    }
    Column(modifier = modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text          = settings.vehicleName.uppercase(),
                style         = MaterialTheme.typography.titleLarge,
                color         = headerTextColor,
                letterSpacing = 3.sp,
                fontSize      = 14.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (expandedWidget != null) {
                TextButton(onClick = { expandedWidget = null }) { Text("Back to dashboard", fontSize = 16.sp) }
            }
            if (settings.layoutProfiles.isNotEmpty() && expandedWidget == null) {
                Box {
                    TextButton(onClick = { profileMenu = true }) {
                        Text(settings.activeLayoutProfile.ifBlank { "Layouts" }, fontSize = 14.sp)
                    }
                    DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                        settings.layoutProfiles.forEach { profile ->
                            DropdownMenuItem(text = { Text(profile.name) }, onClick = {
                                profileMenu = false; removedLayout = null; onApplyProfile(profile.name)
                            })
                        }
                    }
                }
            }
            IconButton(onClick = { toolsPage = "Quick controls" }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Tune, "Dashboard controls", tint = controlIconColor, modifier = Modifier.size(24.dp))
            }
            AnimatedVisibility(visible = isWifi, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.Wifi, "WiFi", tint = statusIconColor, modifier = Modifier.size(16.dp))
            }
            if (isWifi) Spacer(Modifier.width(6.dp))
            AnimatedVisibility(visible = isData, enter = fadeIn(), exit = fadeOut()) {
                Icon(Icons.Default.SignalCellularAlt, "Data", tint = statusIconColor, modifier = Modifier.size(16.dp))
            }
            run {
                Spacer(Modifier.width(8.dp))
                if (editMode) {
                    IconButton(
                        onClick  = { widgetLibraryOpen = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Dashboard,
                            contentDescription = "Widget library",
                            tint               = controlIconColor,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                }
                IconButton(
                    onClick  = { expandedWidget = null; if (editMode) editMode = false else widgetLibraryOpen = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = if (editMode) "Finish arranging" else "Edit Dashboard",
                        tint               = if (editMode) accent else controlIconColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF141414))

        if (removedLayout != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Widget removed", modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { removedLayout?.let(onRestoreLayout); removedLayout = null }) { Text("Undo") }
            }
        }

        // ── Widget Grid ─────────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(gap)
        ) {
            val cellW = (maxWidth  - gap * (GRID_COLS - 1)) / GRID_COLS
            val cellH = (maxHeight - gap * (GRID_ROWS - 1)) / GRID_ROWS
            val density = LocalDensity.current
            val cellStepXPx = with(density) { (cellW + gap).toPx() }
            val cellStepYPx = with(density) { (cellH + gap).toPx() }

            // WEATHER stays in the set even with no data: the commit path
            // (LauncherViewModel.moveWidgetConfig) computes against settings flags
            // only, so dropping it here would make the drop ghost and the committed
            // layout disagree. With no data the cell renders fully transparent.
            val visibleIds = settings.activeWidgetIds()

            // Keep only visible widgets exactly as configured in settings, allowing explicit resizing to dictate layout
            val visible = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
            val rendered = visible

            // ── Drag state ───────────────────────────────────────────────────
            var draggingId   by remember { mutableStateOf<String?>(null) }
            var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }

            // Compute snap target for the widget being dragged (uses original spanX)
            val draggingOriginal = if (draggingId != null) visible.find { it.id == draggingId } else null
            val targetGridX = draggingOriginal?.let {
                (it.gridX + (dragOffsetPx.x / cellStepXPx).roundToInt()).coerceIn(0, GRID_COLS - it.spanX)
            }
            val targetGridY = draggingOriginal?.let {
                (it.gridY + (dragOffsetPx.y / cellStepYPx).roundToInt()).coerceIn(0, GRID_ROWS - it.spanY)
            }

            // Compute proposed layout (push preview) while dragging
            val proposedLayout = if (draggingOriginal != null && targetGridX != null && targetGridY != null)
                computeWidgetMove(visible, draggingOriginal.id, targetGridX, targetGridY)
            else null

            // Drop ghost — rendered before widgets so it appears beneath them
            if (draggingOriginal != null && targetGridX != null && targetGridY != null) {
                val gX = (cellW + gap) * targetGridX
                val gY = (cellH + gap) * targetGridY
                val gW = cellW * draggingOriginal.spanX + gap * (draggingOriginal.spanX - 1)
                val gH = cellH * draggingOriginal.spanY + gap * (draggingOriginal.spanY - 1)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = gX, y = gY)
                        .size(gW, gH)
                        .background(accent.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.5f), WIDGET_RADIUS)
                )
            }

            // Displacement ghosts — show where pushed widgets will land
            if (proposedLayout != null && draggingOriginal != null) {
                proposedLayout
                    .filter { it.id != draggingOriginal.id }
                    .forEach { proposed ->
                        val original = visible.find { it.id == proposed.id } ?: return@forEach
                        if (proposed.gridX != original.gridX || proposed.gridY != original.gridY) {
                            val dX = (cellW + gap) * proposed.gridX
                            val dY = (cellH + gap) * proposed.gridY
                            val dW = cellW * proposed.spanX + gap * (proposed.spanX - 1)
                            val dH = cellH * proposed.spanY + gap * (proposed.spanY - 1)
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = dX, y = dY)
                                    .size(dW, dH)
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), WIDGET_RADIUS)
                            )
                        }
                    }
            }

            rendered.forEach { w ->
                key(w.id) {
                val expanded = expandedWidget == w.id
                val expandable = w.id in setOf("MAP", "NOW_PLAYING")
                val xOff = if (expanded) 0.dp else (cellW + gap) * w.gridX
                val yOff = if (expanded) 0.dp else (cellH + gap) * w.gridY
                val width = if (expanded) maxWidth else cellW * w.spanX + gap * (w.spanX - 1)
                val height = if (expanded) maxHeight else cellH * w.spanY + gap * (w.spanY - 1)

                val label = when (w.id) {
                    "CLOCK"       -> "CLOCK + WEATHER"
                    "WEATHER"     -> "WEATHER"
                    "NOW_PLAYING" -> "NOW PLAYING"
                    "TELEMETRY"   -> "COMPASS"
                    "ALTIMETER"   -> "ALTIMETER"
                    "SPEEDOMETER" -> "SPEED"
                    "TRIP_TRACKER" -> "TRIP"
                    "MAP"         -> "MAP"
                    "CONNECTIVITY" -> "NETWORK"
                    "DESTINATIONS" -> "PLACES"
                    "RADAR" -> "RADAR"
                    "TRAFFIC" -> "TRAFFIC"
                    "SOUNDBOARD"  -> "SOUND"
                    else          -> w.id
                }

                // Original (pre-auto-expand) spanX needed for drag boundary clamping
                val origSpanX  = visible.find { it.id == w.id }?.spanX ?: 1
                val isDragging = draggingId == w.id
                // Weather with no data reserves its cell but draws nothing
                // (still visible in edit mode so it can be moved/removed)
                val isGhost    = w.id == "WEATHER" && weather == null && !editMode
                val dragDpX    = if (isDragging) with(density) { dragOffsetPx.x.toDp() } else 0.dp
                val dragDpY    = if (isDragging) with(density) { dragOffsetPx.y.toDp() } else 0.dp

                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = xOff + dragDpX, y = yOff + dragDpY)
                        .size(width, height)
                        .zIndex(if (expanded) 2f else if (isDragging) 1f else 0f)
                        .graphicsLayer { alpha = if (expandedWidget != null && !expanded) 0f else 1f }
                        .then(if (expandedWidget != null && !expanded) Modifier.clearAndSetSemantics { } else Modifier)
                        .clip(WIDGET_RADIUS)
                        .background(if (expanded) MaterialTheme.colorScheme.surface else if (isGhost) Color.Transparent else widgetBg)
                        .border(
                            width = if (editMode) 1.5.dp else 1.dp,
                            color = when {
                                editMode -> accent.copy(alpha = 0.45f)
                                isGhost  -> Color.Transparent
                                else     -> widgetBorder
                            },
                            shape = WIDGET_RADIUS
                        )
                        .combinedClickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick           = { if (editMode) contextMenuId = w.id },
                            onLongClick       = { if (!editMode) contextMenuId = w.id }
                        )
                        .then(
                            if (editMode) Modifier.pointerInput(editMode, w.id, w.gridX, w.gridY) {
                                var hasSignificantDrag = false
                                // Touch-slop gate: without it, sub-pixel jitter during a
                                // long-press counts as a drag and the context menu never opens
                                val slop = viewConfiguration.touchSlop
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        draggingId         = w.id
                                        dragOffsetPx       = Offset.Zero
                                        hasSignificantDrag = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx      += dragAmount
                                        if (!hasSignificantDrag && dragOffsetPx.getDistance() > slop) {
                                            hasSignificantDrag = true
                                        }
                                    },
                                    onDragEnd = {
                                        if (hasSignificantDrag) {
                                            val newX = (w.gridX + (dragOffsetPx.x / cellStepXPx).roundToInt())
                                                .coerceIn(0, GRID_COLS - origSpanX)
                                            val newY = (w.gridY + (dragOffsetPx.y / cellStepYPx).roundToInt())
                                                .coerceIn(0, GRID_ROWS - w.spanY)
                                            removedLayout = null
                                            onMoveWidget(w.id, newX, newY)
                                        } else {
                                            contextMenuId = w.id
                                        }
                                        draggingId   = null
                                        dragOffsetPx = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingId   = null
                                        dragOffsetPx = Offset.Zero
                                    }
                                )
                            } else Modifier
                        )
                ) {
                    Box(Modifier.fillMaxSize().padding(top = if (w.id == "MAP" && !editMode) 48.dp else 0.dp)) {
                    when (w.id) {
                        "CLOCK" -> ClockWidget(
                            style      = settings.clockStyle,
                            accent     = accent,
                            isDayMode  = isDayMode,
                            use12HourTime = settings.use12HourTime,
                            showSeconds = settings.showClockSeconds,
                            dateFormat = settings.clockDateFormat,
                            timeZoneChoice = settings.clockTimeZone,
                            weather = weather,
                            metric = settings.unitSystem.name == "METRIC",
                            networkAvailable = networkAvailable,
                            modifier   = Modifier.fillMaxSize().combinedClickable(enabled = !editMode, onClick = { toolsPage = "Weather" }, onLongClick = { contextMenuId = "CLOCK" })
                        )
                        "WEATHER" -> WeatherWidget(
                            state      = weather,
                            accent     = accent,
                            metric     = settings.unitSystem.name == "METRIC",
                            isDayMode  = isDayMode,
                            modifier   = Modifier.fillMaxSize()
                        )
                        "NOW_PLAYING" -> NowPlayingWidget(
                            state               = nowPlaying,
                            preferredPackage = settings.preferredMediaPackage,
                            mediaApps = mediaApps,
                            onSelectMedia = onSelectMedia,
                            onOpenSelectedMedia = onOpenSelectedMedia,
                            accent              = accent,
                            carPlayPackage      = settings.carPlayPackage,
                            androidAutoPackage  = settings.androidAutoPackage,
                            onPlayPause         = onPlayPause,
                            onNext              = onNext,
                            onPrev              = onPrev,
                            onLaunchCarPlay     = onLaunchCarPlay,
                            onLaunchAndroidAuto = onLaunchAndroidAuto,
                            onTapToOpenApp      = onTapNowPlaying,
                            modifier            = Modifier.fillMaxSize(),
                            isEditing           = editMode,
                            isDayMode           = isDayMode,
                            hardwareRadio         = hardwareRadio,
                            onLaunchHardwareRadio = onLaunchHardwareRadio,
                            onStopHardwareRadio   = onStopHardwareRadio,
                            onRadioSeekUp         = onRadioSeekUp,
                            onRadioSeekDown       = onRadioSeekDown,
                            onRadioCycleFm        = onRadioCycleFm,
                            onRadioSwitchAm       = onRadioSwitchAm,
                            onRadioTune           = onRadioTune,
                            onAssignRadio         = onAssignRadio
                        )
                        "TELEMETRY" -> TelemetryWidget(
                            location  = location,
                            bearing   = (bearing + settings.compassOffset + 360f) % 360f,
                            accent    = accent,
                            isDayMode = isDayMode,
                            modifier  = Modifier.fillMaxSize()
                        )
                        "ALTIMETER" -> AltimeterWidget(
                            location  = location,
                            isMetric  = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
                            accent    = accent,
                            isDayMode = isDayMode,
                            modifier  = Modifier.fillMaxSize()
                        )
                        "SPEEDOMETER" -> SpeedometerWidget(
                            location  = location,
                            isMetric  = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
                            accent    = accent,
                            isDayMode = isDayMode,
                            digitalOnly = settings.speedometerDigitalOnly,
                            modifier  = Modifier.fillMaxSize()
                        )
                        "VITALS" -> VitalsWidget(
                            accent    = accent,
                            isDayMode = isDayMode,
                            asBars    = settings.vitalsAsBars,
                            modifier  = Modifier.fillMaxSize()
                        )
                        "TRIP_TRACKER" -> TripTrackerWidget(
                            trip = trips.current, onToggleTrip = onToggleTrip, onResetTrip = onResetTrip, onFinishTrip = onFinishTrip,
                            location  = location,
                            isMetric  = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
                            accent    = accent,
                            isDayMode = isDayMode,
                            modifier  = Modifier.fillMaxSize()
                        )
                        "MAP" -> MapWidget(
                            autoZoom = settings.mapAutoZoom,
                            headingUp = settings.mapHeadingUp,
                            softwareRendering = settings.mapSoftwareRendering,
                            onSoftwareRendering = { value -> onSettings { copy(mapSoftwareRendering = value) } },
                            onMapOptions = onMapOptions,
                            location = location,
                            isEditing = editMode,
                            onlineEnabled = settings.onlineMapEnabled,
                            navigationPackage = settings.navigationPackage,
                            networkAvailable = networkAvailable,
                            modifier = Modifier.fillMaxSize()
                        )
                        "CONNECTIVITY" -> ConnectivityWidget(
                            isWifi = isWifi, isData = isData, internetValidated = internetValidated, enabled = !editMode, modifier = Modifier.fillMaxSize()
                        )
                        "DESTINATIONS" -> DestinationsWidget(
                            home = settings.homeDestination,
                            work = settings.workDestination,
                            recent = settings.recentDestinations,
                            preferredPackage = settings.navigationPackage,
                            onNavigate = onRememberDestination,
                            enabled = !editMode,
                            modifier = Modifier.fillMaxSize()
                        )
                        "RADAR" -> RadarWidget(enabled = !editMode, modifier = Modifier.fillMaxSize())
                        "TRAFFIC" -> TrafficWidget(location = location, enabled = !editMode, modifier = Modifier.fillMaxSize())
                        "SOUNDBOARD" -> SoundboardWidget(
                            pads      = settings.soundboardPads,
                            accent    = accent,
                            isDayMode = isDayMode,
                            isEditing = editMode,
                            onUpdatePad = onUpdateSoundPad,
                            modifier  = Modifier.fillMaxSize()
                        )
                    }

                    }
                    if (expandable && !editMode) {
                        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().height(48.dp).padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (w.id == "MAP") Text("Map", fontSize = 16.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, modifier = Modifier.weight(1f))
                            else Spacer(Modifier.weight(1f))
                            IconButton(onClick = { expandedWidget = if (expanded) null else w.id }, modifier = Modifier.size(48.dp)) {
                                Icon(if (expanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (expanded) "Return to dashboard" else "Expand ${w.id.replace('_', ' ')}",
                                    tint = if (w.id == "NOW_PLAYING" && (nowPlaying?.albumArt != null || nowPlaying?.artUri != null)) Color.White else MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                    // Label — hide when album art fills the widget background
                    val labelColor = when {
                        w.id in setOf("NOW_PLAYING", "MAP") -> Color.Transparent
                        isGhost -> Color.Transparent
                        w.id == "NOW_PLAYING" && nowPlaying?.albumArt != null && nowPlaying.title.isNotEmpty() -> Color.Transparent
                        isDayMode -> Color(0xFF999999)
                        else      -> Color(0xFF3A3A3A)
                    }
                    Text(
                        text          = label,
                        style         = MaterialTheme.typography.labelSmall,
                        color         = labelColor,
                        letterSpacing = 2.sp,
                        fontSize      = 8.sp,
                        modifier      = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 7.dp)
                    )
                }
                } // Stable identity keeps native views with their widget during edits.
            }
        }
    }

    // ── Widget context menu (long-press any cell) ────────────────────────────
    contextMenuId?.let { id ->
        WidgetContextMenu(
            widgetId            = id,
            accent              = accent,
            clockStyle          = settings.clockStyle,
            vitalsAsBars        = settings.vitalsAsBars,
            speedometerDigitalOnly = settings.speedometerDigitalOnly,
            carPlayPackage      = settings.carPlayPackage,
            androidAutoPackage  = settings.androidAutoPackage,
            pipAppPackage       = settings.pipAppPackage,
            isDayMode           = isDayMode,
            onResize            = { contextMenuId = null; resizingId = id },
            onArrange           = { contextMenuId = null; editMode = true },
            onRemove            = { contextMenuId = null; removeWithUndo(id) },
            onAssignCarPlay     = { contextMenuId = null; onAssignCarPlay() },
            onAssignAndroidAuto = { contextMenuId = null; onAssignAndroidAuto() },
            onClearCarPlay      = { contextMenuId = null; onClearCarPlay() },
            onClearAndroidAuto  = { contextMenuId = null; onClearAndroidAuto() },
            onAssignPip         = { contextMenuId = null; onAssignPip() },
            onClearPip          = { contextMenuId = null; onClearPip() },
            onSetClockStyle     = { onSetClockStyle(it) },
            onSetVitalsAsBars   = { onSetVitalsAsBars(it) },
            onSetSpeedometerDigitalOnly = { onSetSpeedometerDigitalOnly(it) },
            onDismiss           = { contextMenuId = null }
        )
    }

    // ── Resize dialog ────────────────────────────────────────────────────────
    resizingId?.let { id ->
        val config = settings.widgetLayout.find { it.id == id }
        if (config != null) {
            WidgetResizeDialog(
                config    = config,
                settings  = settings,
                accent    = accent,
                isDayMode = isDayMode,
                onDismiss = { resizingId = null },
                onConfirm = { sx, sy ->
                    removedLayout = null
                    onUpdateWidget(id, sx, sy)
                    resizingId = null
                }
            )
        }
    }

    toolsPage?.let { page -> DashboardTools(page, settings, weather, weatherError, location, internetValidated,
        trips, tripError, onToggleTrip, onFinishTrip, onClearTrips, onRefreshWeather, onSettings, { toolsPage = null },
        networkAvailable = networkAvailable) }

    // ── Widget library ────────────────────────────────────────────────────────
    if (widgetLibraryOpen) {
        DashboardEditor(settings, onApply = { profile -> removedLayout = null; onRestoreLayout(profile) },
            onDismiss = { widgetLibraryOpen = false; editMode = false })
    }
}

@Composable
private fun WidgetContextMenu(
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
    val menuBg    = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val menuBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val menuDivider = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp)
                .width(280.dp)
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val inactiveMenuTint = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA)
            ContextRow("ARRANGE", Icons.Default.Dashboard, accent, onArrange, isDayMode = isDayMode)
            HorizontalDivider(color = menuDivider)
            ContextRow("RESIZE", Icons.Default.OpenWith, accent, onResize, isDayMode = isDayMode)
            HorizontalDivider(color = menuDivider)
            ContextRow("REMOVE WIDGET", Icons.Default.Delete, Color(0xFF884444), onRemove, isDayMode = isDayMode)
            if (widgetId == "CLOCK") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL",
                    icon    = Icons.Default.Schedule,
                    tint    = if (clockStyle == ClockStyle.DIGITAL) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.DIGITAL); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "ANALOG",
                    icon    = Icons.Default.Watch,
                    tint    = if (clockStyle == ClockStyle.ANALOG) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.ANALOG); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "VITALS") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIAL GAUGES",
                    icon    = Icons.Default.Adjust,
                    tint    = if (!vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "BARS VIEW",
                    icon    = Icons.Default.FormatAlignLeft,
                    tint    = if (vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "SPEEDOMETER") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIAL TRACK",
                    icon    = Icons.Default.Speed,
                    tint    = if (!speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL ONLY",
                    icon    = Icons.Default.Dialpad,
                    tint    = if (speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "NOW_PLAYING") {
                HorizontalDivider(color = menuDivider)
                ContextRow("ASSIGN CARPLAY APP",      Icons.Default.PhoneAndroid,  accent, onAssignCarPlay, isDayMode = isDayMode)
                if (carPlayPackage.isNotEmpty()) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow("CLEAR CARPLAY APP", Icons.Default.PhoneAndroid, Color(0xFF884444), onClearCarPlay, isDayMode = isDayMode)
                }
                HorizontalDivider(color = menuDivider)
                ContextRow("ASSIGN ANDROID AUTO APP", Icons.Default.DirectionsCar, accent, onAssignAndroidAuto, isDayMode = isDayMode)
                if (androidAutoPackage.isNotEmpty()) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow("CLEAR ANDROID AUTO APP", Icons.Default.DirectionsCar, Color(0xFF884444), onClearAndroidAuto, isDayMode = isDayMode)
                }
            }

        }
    }
}

@Composable
private fun ContextRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    isDayMode: Boolean = false
) {
    val finalTint = if (isDayMode) {
        if (tint == Color(0xFF884444)) {
            tint
        } else if (tint == Color(0xFF666666)) {
            Color(0xFF666666)
        } else {
            Color(0xFF111111)
        }
    } else {
        if (tint == Color(0xFF884444)) Color(0xFFEF9A9A) else tint
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = finalTint, modifier = Modifier.size(16.dp))
        Text(label, color = finalTint, fontSize = 14.sp)
    }
}

@Composable
private fun WidgetResizeDialog(
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

    val dialogBg     = if (isDayMode) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.background
    val dialogText   = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val cancelColor  = if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text          = config.id.replace('_', ' '),
                color         = dialogText,
                fontSize      = 11.sp,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("QUICK SIZES", color = dialogText, fontSize = 9.sp, letterSpacing = 1.sp)
                (1..maxSpanY).forEach { height ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..maxSpanX).forEach { width ->
                            FilterChip(
                                enabled = settings.resizePreview(config.id, width, height) != null,
                                selected = spanX == width && spanY == height,
                                onClick = { spanX = width; spanY = height },
                                label = { Text("${width}×${height}", fontSize = 10.sp) }
                            )
                        }
                    }
                }
                Text(if (preview == null) "This size does not fit. Remove or shrink another widget."
                    else "Preview: the highlighted cells show this widget. Disabled sizes need more room.",
                    color = dialogText.copy(alpha = 0.7f), fontSize = 11.sp)
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
                Text("APPLY", color = accent, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = cancelColor, fontSize = 16.sp)
            }
        },
        containerColor    = dialogBg,
        titleContentColor = dialogText,
        textContentColor  = dialogText
    )
}

@Composable
private fun SpanRow(
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
