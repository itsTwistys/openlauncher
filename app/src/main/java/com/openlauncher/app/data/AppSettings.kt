package com.openlauncher.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class ClockStyle { DIGITAL, ANALOG }
enum class UnitSystem { METRIC, IMPERIAL }
enum class AppFont { SYSTEM, JETBRAINS_MONO, SOURCE_CODE_PRO }
enum class DayNightMode { DARK, LIGHT, AUTO, SYSTEM }
enum class SidebarPosition { LEFT, RIGHT, BOTTOM }
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT, DIAGONAL, RADIAL }

enum class DefaultShortcutIcon {
    NONE,
    // Navigation & vehicle
    RADIO, CAMERA, PHONE, MAP, NAVIGATION, CAR, GAS_STATION, DASHBOARD,
    // Audio & media
    MUSIC, SPEAKER, HEADSET, EQUALIZER, VOLUME_UP,
    // Connectivity
    BLUETOOTH, WIFI,
    // Lighting & climate
    LIGHTBULB, BRIGHTNESS, AC, THERMOSTAT,
    // General utility
    TV, VIDEOCAM, STAR, MESSAGE, TIMER, LOCK, SETTINGS, FAVORITE,
    // Web / location
    GLOBE
}

data class SoundPadConfig(
    val label: String,
    val audioUri: String = "",
    val synthType: String = "BEEP"
)

fun defaultSoundboardPads() = listOf(
    SoundPadConfig("mario_jump",   synthType = "mario_jump"),
    SoundPadConfig("mario_coin",   synthType = "mario_coin"),
    SoundPadConfig("boom",         synthType = "boom"),
    SoundPadConfig("loud_fart",    synthType = "loud_fart"),
    SoundPadConfig("+",            synthType = ""),
    SoundPadConfig("+",            synthType = "")
)

data class ShortcutConfig(
    val packageName: String = "",
    val label: String = "",
    val isDefault: Boolean = false,
    val defaultIcon: DefaultShortcutIcon = DefaultShortcutIcon.NONE,
    // null = native app icon; non-null = override with this vector icon
    val customIconOverride: DefaultShortcutIcon? = null
)

const val GRID_COLS = 3
const val GRID_ROWS = 2

data class WidgetConfig(
    val id: String,          // "CLOCK" | "WEATHER" | "TELEMETRY" | "NOW_PLAYING"
    val gridX: Int,          // column 0..(GRID_COLS-1)
    val gridY: Int,          // row    0..(GRID_ROWS-1)
    val spanX: Int = 1,
    val spanY: Int = 1,
    val enabled: Boolean = true
)

data class LayoutProfile(
    val name: String,
    val layout: List<WidgetConfig>,
    val enabledIds: List<String>
)

data class AppSettings(
    val vehicleName: String = "MY CAR",
    val accentColor: Int = Color.White.toArgb(),
    val backgroundColor: Int = Color.Black.toArgb(),
    val fontColor: Int = Color.White.toArgb(),
    val wallpaperUri: String = "",
    val fontBold: Boolean = false,
    val textScale: Float = 1.2f,
    val uiScale: Float = 1.0f,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val use12HourTime: Boolean = false,
    val showClockSeconds: Boolean = false,
    val clockDateFormat: String = "LONG",
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appFont: AppFont = AppFont.JETBRAINS_MONO,
    val showWeather: Boolean = true,
    val showClock: Boolean = true,
    val showTelemetry: Boolean = true,
    val showNowPlaying: Boolean = true,
    val shortcuts: List<ShortcutConfig> = defaultShortcuts(),
    val widgetLayout: List<WidgetConfig> = defaultWidgetLayout(),
    val carPlayPackage: String = "",
    val androidAutoPackage: String = "",
    val useGradient: Boolean = false,
    val gradientEndColor: Int = Color.Black.toArgb(),
    val wallpaperDim: Float = 0.55f,
    val sidebarPosition: SidebarPosition = SidebarPosition.LEFT,
    val bottomBarShortcutsRight: Boolean = false,
    val showAltimeter: Boolean = false,
    val showSpeedometer: Boolean = false,
    val dayNightMode: DayNightMode = DayNightMode.DARK,
    val showPip: Boolean = false,
    val pipAppPackage: String = "",
    // Head unit's radio app — mirrored & controlled via its MediaSession
    val radioPackage: String = "",
    val onboardingCompleted: Boolean = false,
    val showVitals: Boolean = false,
    val showTripTracker: Boolean = false,
    val compassOffset: Float = 0f,
    val showSoundboard: Boolean = false,
    val showMap: Boolean = false,
    val onlineMapEnabled: Boolean = false,
    val showConnectivity: Boolean = false,
    val showDestinations: Boolean = false,
    val showRadar: Boolean = false,
    val showTraffic: Boolean = false,
    val homeDestination: String = "",
    val workDestination: String = "",
    val layoutProfiles: List<LayoutProfile> = emptyList(),
    val activeLayoutProfile: String = "",
    val autoDayNightProfiles: Boolean = false,
    val navigationPackage: String = "",
    val recentDestinations: List<String> = emptyList(),
    val soundboardPads: List<SoundPadConfig> = defaultSoundboardPads(),
    val vitalsAsBars: Boolean = false,
    val speedometerDigitalOnly: Boolean = false,
    val gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
    val useCustomBackgroundColor: Boolean = false
)

fun defaultShortcuts() = listOf(
    ShortcutConfig(label = "Radio", isDefault = true, defaultIcon = DefaultShortcutIcon.RADIO),
    ShortcutConfig(label = "Camera", isDefault = true, defaultIcon = DefaultShortcutIcon.CAMERA),
    ShortcutConfig(label = "Music", isDefault = true, defaultIcon = DefaultShortcutIcon.MUSIC),
    ShortcutConfig(label = "Phone", isDefault = true, defaultIcon = DefaultShortcutIcon.PHONE)
)

fun defaultWidgetLayout() = listOf(
    WidgetConfig("CLOCK",       gridX = 0, gridY = 0, spanX = 1, spanY = 1),
    WidgetConfig("WEATHER",     gridX = 1, gridY = 0, spanX = 1, spanY = 1),
    WidgetConfig("TELEMETRY",   gridX = 2, gridY = 0, spanX = 1, spanY = 2),
    WidgetConfig("NOW_PLAYING", gridX = 0, gridY = 1, spanX = 2, spanY = 1)
)

fun AppSettings.activeWidgetIds(): Set<String> = buildSet {
    if (showClock) add("CLOCK")
    if (showWeather) add("WEATHER")
    if (showNowPlaying) add("NOW_PLAYING")
    if (showTelemetry) add("TELEMETRY")
    if (showAltimeter) add("ALTIMETER")
    if (showSpeedometer) add("SPEEDOMETER")
    if (showVitals) add("VITALS")
    if (showTripTracker) add("TRIP_TRACKER")
    if (showSoundboard) add("SOUNDBOARD")
    if (showMap) add("MAP")
    if (showConnectivity) add("CONNECTIVITY")
    if (showDestinations) add("DESTINATIONS")
    if (showRadar) add("RADAR")
    if (showTraffic) add("TRAFFIC")
}

fun AppSettings.withWidgetVisibility(ids: Set<String>): AppSettings = copy(
    showClock = "CLOCK" in ids, showWeather = "WEATHER" in ids,
    showNowPlaying = "NOW_PLAYING" in ids, showTelemetry = "TELEMETRY" in ids,
    showAltimeter = "ALTIMETER" in ids, showSpeedometer = "SPEEDOMETER" in ids,
    showVitals = "VITALS" in ids, showTripTracker = "TRIP_TRACKER" in ids,
    showSoundboard = "SOUNDBOARD" in ids, showMap = "MAP" in ids,
    showConnectivity = "CONNECTIVITY" in ids,
    showDestinations = "DESTINATIONS" in ids, showRadar = "RADAR" in ids,
    showTraffic = "TRAFFIC" in ids
)

/** Exhaustive placement for a six-cell grid; never persists overlaps. */
fun fitWidgetLayout(layout: List<WidgetConfig>, target: WidgetConfig): List<WidgetConfig>? {
    if (target.spanX !in 1..GRID_COLS || target.spanY !in 1..GRID_ROWS) return null
    val others = layout.filter { it.id != target.id }.sortedByDescending { it.spanX * it.spanY }
    if (others.sumOf { it.spanX * it.spanY } + target.spanX * target.spanY > GRID_COLS * GRID_ROWS) return null
    fun positions(w: WidgetConfig) = (0..GRID_ROWS - w.spanY).flatMap { y ->
        (0..GRID_COLS - w.spanX).map { x -> w.copy(gridX = x, gridY = y) }
    }.sortedBy { kotlin.math.abs(it.gridX - w.gridX) + kotlin.math.abs(it.gridY - w.gridY) }
    fun place(todo: List<WidgetConfig>, placed: List<WidgetConfig>): List<WidgetConfig>? {
        if (todo.isEmpty()) return placed
        val w = todo.first()
        for (candidate in positions(w)) {
            if (placed.none { widgetsOverlap(it, candidate) }) {
                val result = place(todo.drop(1), placed + candidate)
                if (result != null) return result
            }
        }
        return null
    }
    for (candidate in positions(target)) {
        val result = place(others, listOf(candidate))
        if (result != null) return layout.map { w -> result.first { it.id == w.id } }
    }
    return null
}

fun computeWidgetMove(layout: List<WidgetConfig>, movingId: String, targetX: Int, targetY: Int): List<WidgetConfig> {
    val moving = layout.firstOrNull { it.id == movingId } ?: return layout
    return fitWidgetLayout(layout, moving.copy(gridX = targetX, gridY = targetY)) ?: layout
}

fun AppSettings.resizePreview(id: String, spanX: Int, spanY: Int): List<WidgetConfig>? {
    val active = widgetLayout.filter { it.enabled && it.id in activeWidgetIds() }
    val target = active.firstOrNull { it.id == id } ?: return null
    return fitWidgetLayout(active, target.copy(spanX = spanX, spanY = spanY))
}

fun validWidgetLayout(layout: List<WidgetConfig>): Boolean =
    layout.map { it.id }.distinct().size == layout.size && layout.all {
        it.spanX in 1..GRID_COLS && it.spanY in 1..GRID_ROWS && it.gridX >= 0 && it.gridY >= 0 &&
            it.gridX + it.spanX <= GRID_COLS && it.gridY + it.spanY <= GRID_ROWS
    } && layout.indices.all { i -> (i + 1 until layout.size).all { j -> !widgetsOverlap(layout[i], layout[j]) } }

private fun widgetsOverlap(a: WidgetConfig, b: WidgetConfig): Boolean =
    a.gridX < b.gridX + b.spanX && a.gridX + a.spanX > b.gridX &&
    a.gridY < b.gridY + b.spanY && a.gridY + a.spanY > b.gridY

/** Drop retired video cards when upgrading preferences or importing an older backup. */
fun AppSettings.withoutRetiredWidgets(): AppSettings = copy(
    widgetLayout = widgetLayout.filterNot { it.id == "YOUTUBE" },
    layoutProfiles = layoutProfiles.map { profile -> profile.copy(
        layout = profile.layout.filterNot { it.id == "YOUTUBE" },
        enabledIds = profile.enabledIds.filterNot { it == "YOUTUBE" }
    ) }
)
