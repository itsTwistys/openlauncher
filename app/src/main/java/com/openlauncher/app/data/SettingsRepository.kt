package com.openlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val VEHICLE_NAME       = stringPreferencesKey("vehicle_name")
        val ACCENT_COLOR       = intPreferencesKey("accent_color")
        val BG_COLOR           = intPreferencesKey("bg_color")
        val FONT_COLOR         = intPreferencesKey("font_color")
        val WALLPAPER_URI      = stringPreferencesKey("wallpaper_uri")
        val FONT_BOLD          = booleanPreferencesKey("font_bold")
        val TEXT_SCALE         = floatPreferencesKey("text_scale")
        val UI_SCALE           = floatPreferencesKey("ui_scale")
        val CLOCK_STYLE        = stringPreferencesKey("clock_style")
        val USE_12_HOUR_TIME   = booleanPreferencesKey("use_12_hour_time")
        val CLOCK_SECONDS      = booleanPreferencesKey("clock_seconds")
        val CLOCK_DATE_FORMAT  = stringPreferencesKey("clock_date_format")
        val UNIT_SYSTEM        = stringPreferencesKey("unit_system")
        val APP_FONT           = stringPreferencesKey("app_font")
        val SHOW_WEATHER       = booleanPreferencesKey("show_weather")
        val SHOW_CLOCK         = booleanPreferencesKey("show_clock")
        val SHOW_TELEMETRY     = booleanPreferencesKey("show_telemetry")
        val PREFERRED_MEDIA = stringPreferencesKey("preferred_media_package")
        val LAUNCHER_BRIGHTNESS = floatPreferencesKey("launcher_brightness")
        val WEATHER_BACKGROUND = booleanPreferencesKey("weather_background")
        val LAYOUT_HISTORY = stringPreferencesKey("layout_history")
        val MAP_AUTO_ZOOM = booleanPreferencesKey("map_auto_zoom")
        val MAP_HEADING_UP = booleanPreferencesKey("map_heading_up")
        val MAP_SOFTWARE_RENDERING = booleanPreferencesKey("map_software_rendering")
        val SHOW_NOW_PLAYING   = booleanPreferencesKey("show_now_playing")
        val SHOW_ALTIMETER     = booleanPreferencesKey("show_altimeter")
        val SHOW_SPEEDOMETER   = booleanPreferencesKey("show_speedometer")
        val SHORTCUTS_JSON     = stringPreferencesKey("shortcuts_json")
        val WIDGET_LAYOUT_JSON = stringPreferencesKey("widget_layout_json")
        val CAR_PLAY_PACKAGE      = stringPreferencesKey("car_play_package")
        val ANDROID_AUTO_PACKAGE  = stringPreferencesKey("android_auto_package")
        val USE_GRADIENT          = booleanPreferencesKey("use_gradient")
        val GRADIENT_END_COLOR    = intPreferencesKey("gradient_end_color")
        val WALLPAPER_DIM         = floatPreferencesKey("wallpaper_dim")
        val RIGHT_HAND_DRIVE      = booleanPreferencesKey("right_hand_drive") // kept for migration
        val SIDEBAR_POSITION           = stringPreferencesKey("sidebar_position")
        val BOTTOM_BAR_SHORTCUTS_RIGHT = booleanPreferencesKey("bottom_bar_shortcuts_right")
        val DAY_NIGHT_MODE        = stringPreferencesKey("day_night_mode")
        val SHOW_PIP              = booleanPreferencesKey("show_pip")
        val PIP_APP_PACKAGE       = stringPreferencesKey("pip_app_package")
        val RADIO_PACKAGE         = stringPreferencesKey("radio_package")
        val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        val SHOW_VITALS           = booleanPreferencesKey("show_vitals")
        val SHOW_TRIP_TRACKER     = booleanPreferencesKey("show_trip_tracker")
        val COMPASS_OFFSET        = floatPreferencesKey("compass_offset")
        val SHOW_SOUNDBOARD       = booleanPreferencesKey("show_soundboard")
        val SHOW_MAP            = booleanPreferencesKey("show_map")
        val ONLINE_MAP_ENABLED = booleanPreferencesKey("online_map_enabled")
        val SHOW_CONNECTIVITY   = booleanPreferencesKey("show_connectivity")
        val SHOW_DESTINATIONS   = booleanPreferencesKey("show_destinations")
        val SHOW_RADAR          = booleanPreferencesKey("show_radar")
        val SHOW_TRAFFIC        = booleanPreferencesKey("show_traffic")
        val HOME_DESTINATION    = stringPreferencesKey("home_destination")
        val WORK_DESTINATION    = stringPreferencesKey("work_destination")
        val LAYOUT_PROFILES_JSON = stringPreferencesKey("layout_profiles_json")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val AUTO_PROFILES = booleanPreferencesKey("auto_profiles")
        val NAVIGATION_PACKAGE = stringPreferencesKey("navigation_package")
        val RECENT_DESTINATIONS = stringPreferencesKey("recent_destinations")
        val SOUNDBOARD_PADS_JSON  = stringPreferencesKey("soundboard_pads_json")
        val VITALS_AS_BARS        = booleanPreferencesKey("vitals_as_bars")
        val SPEEDOMETER_DIGITAL_ONLY = booleanPreferencesKey("speedometer_digital_only")
        val GRADIENT_DIRECTION    = stringPreferencesKey("gradient_direction")
        val USE_CUSTOM_BG_COLOR   = booleanPreferencesKey("use_custom_bg_color")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> readSettings(prefs) }

    private fun readSettings(prefs: Preferences): AppSettings {
            val defaults = AppSettings()
            val shortcutsJson = prefs[Keys.SHORTCUTS_JSON]
            val shortcuts = if (shortcutsJson != null) {
                runCatching {
                    gson.fromJson<List<ShortcutConfig>>(
                        shortcutsJson,
                        object : TypeToken<List<ShortcutConfig>>() {}.type
                    )
                }.getOrNull() ?: defaults.shortcuts
            } else defaults.shortcuts

            val widgetJson = prefs[Keys.WIDGET_LAYOUT_JSON]
            val widgets = if (widgetJson != null) {
                val loaded = runCatching {
                    gson.fromJson<List<WidgetConfig>>(
                        widgetJson,
                        object : TypeToken<List<WidgetConfig>>() {}.type
                    )
                }.getOrNull() ?: defaults.widgetLayout
                // Keep saved positions; an empty right-hand column is a valid custom layout.
                loaded
            } else defaults.widgetLayout

            return AppSettings(
                launcherBrightness = prefs[Keys.LAUNCHER_BRIGHTNESS] ?: -1f,
                weatherBackground = prefs[Keys.WEATHER_BACKGROUND] ?: false,
                layoutHistory = runCatching { gson.fromJson<List<LayoutProfile>>(prefs[Keys.LAYOUT_HISTORY] ?: "[]",
                    object : TypeToken<List<LayoutProfile>>() {}.type) }.getOrNull().orEmpty().filter { validWidgetLayout(it.layout) }.take(8),
                preferredMediaPackage = prefs[Keys.PREFERRED_MEDIA] ?: "",
                mapAutoZoom = prefs[Keys.MAP_AUTO_ZOOM] ?: true,
                mapHeadingUp = prefs[Keys.MAP_HEADING_UP] ?: false,
                mapSoftwareRendering = prefs[Keys.MAP_SOFTWARE_RENDERING] ?: true,
                vehicleName    = prefs[Keys.VEHICLE_NAME]     ?: defaults.vehicleName,
                accentColor    = prefs[Keys.ACCENT_COLOR]     ?: defaults.accentColor,
                backgroundColor = prefs[Keys.BG_COLOR]        ?: defaults.backgroundColor,
                fontColor      = prefs[Keys.FONT_COLOR]       ?: defaults.fontColor,
                wallpaperUri   = prefs[Keys.WALLPAPER_URI]    ?: defaults.wallpaperUri,
                fontBold       = prefs[Keys.FONT_BOLD]        ?: defaults.fontBold,
                textScale      = prefs[Keys.TEXT_SCALE]       ?: defaults.textScale,
                uiScale        = prefs[Keys.UI_SCALE]         ?: defaults.uiScale,
                clockStyle     = prefs[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: defaults.clockStyle,
                use12HourTime  = prefs[Keys.USE_12_HOUR_TIME] ?: defaults.use12HourTime,
                showClockSeconds = prefs[Keys.CLOCK_SECONDS] ?: defaults.showClockSeconds,
                clockDateFormat = prefs[Keys.CLOCK_DATE_FORMAT] ?: defaults.clockDateFormat,
                unitSystem     = prefs[Keys.UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: defaults.unitSystem,
                appFont        = prefs[Keys.APP_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() } ?: defaults.appFont,
                showWeather    = prefs[Keys.SHOW_WEATHER]     ?: defaults.showWeather,
                showClock      = prefs[Keys.SHOW_CLOCK]       ?: defaults.showClock,
                showTelemetry  = prefs[Keys.SHOW_TELEMETRY]   ?: defaults.showTelemetry,
                showNowPlaying = prefs[Keys.SHOW_NOW_PLAYING] ?: defaults.showNowPlaying,
                showAltimeter   = prefs[Keys.SHOW_ALTIMETER]   ?: defaults.showAltimeter,
                showSpeedometer = prefs[Keys.SHOW_SPEEDOMETER] ?: defaults.showSpeedometer,
                shortcuts      = shortcuts,
                widgetLayout   = widgets,
                carPlayPackage      = prefs[Keys.CAR_PLAY_PACKAGE]      ?: defaults.carPlayPackage,
                androidAutoPackage  = prefs[Keys.ANDROID_AUTO_PACKAGE]  ?: defaults.androidAutoPackage,
                useGradient      = prefs[Keys.USE_GRADIENT]        ?: defaults.useGradient,
                gradientEndColor = prefs[Keys.GRADIENT_END_COLOR]  ?: defaults.gradientEndColor,
                wallpaperDim     = prefs[Keys.WALLPAPER_DIM]       ?: defaults.wallpaperDim,
                sidebarPosition  = prefs[Keys.SIDEBAR_POSITION]?.let { runCatching { SidebarPosition.valueOf(it) }.getOrNull() }
                                   ?: if (prefs[Keys.RIGHT_HAND_DRIVE] == true) SidebarPosition.RIGHT else defaults.sidebarPosition,
                bottomBarShortcutsRight = prefs[Keys.BOTTOM_BAR_SHORTCUTS_RIGHT] ?: defaults.bottomBarShortcutsRight,
                dayNightMode     = prefs[Keys.DAY_NIGHT_MODE]?.let { runCatching { DayNightMode.valueOf(it) }.getOrNull() } ?: defaults.dayNightMode,
                showPip          = prefs[Keys.SHOW_PIP]         ?: defaults.showPip,
                pipAppPackage    = prefs[Keys.PIP_APP_PACKAGE]  ?: defaults.pipAppPackage,
                radioPackage     = prefs[Keys.RADIO_PACKAGE]    ?: defaults.radioPackage,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
                showVitals       = prefs[Keys.SHOW_VITALS]      ?: defaults.showVitals,
                showTripTracker  = prefs[Keys.SHOW_TRIP_TRACKER] ?: defaults.showTripTracker,
                compassOffset    = prefs[Keys.COMPASS_OFFSET]    ?: defaults.compassOffset,
                showSoundboard   = prefs[Keys.SHOW_SOUNDBOARD]   ?: defaults.showSoundboard,
                showMap          = prefs[Keys.SHOW_MAP]          ?: (if (widgetJson != null) widgets.any { it.id == "MAP" && it.enabled } else defaults.showMap),
                onlineMapEnabled = prefs[Keys.ONLINE_MAP_ENABLED] ?: defaults.onlineMapEnabled,
                showConnectivity = prefs[Keys.SHOW_CONNECTIVITY] ?: defaults.showConnectivity,
                showDestinations = prefs[Keys.SHOW_DESTINATIONS] ?: defaults.showDestinations,
                showRadar = prefs[Keys.SHOW_RADAR] ?: defaults.showRadar,
                showTraffic = prefs[Keys.SHOW_TRAFFIC] ?: defaults.showTraffic,
                homeDestination = prefs[Keys.HOME_DESTINATION] ?: defaults.homeDestination,
                workDestination = prefs[Keys.WORK_DESTINATION] ?: defaults.workDestination,
                activeLayoutProfile = prefs[Keys.ACTIVE_PROFILE] ?: "",
                autoDayNightProfiles = prefs[Keys.AUTO_PROFILES] ?: false,
                navigationPackage = prefs[Keys.NAVIGATION_PACKAGE] ?: "",
                recentDestinations = prefs[Keys.RECENT_DESTINATIONS]?.let {
                    runCatching { gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type) }.getOrNull()
                } ?: emptyList(),
                layoutProfiles = prefs[Keys.LAYOUT_PROFILES_JSON]?.let {
                    runCatching { gson.fromJson<List<LayoutProfile>>(it, object : TypeToken<List<LayoutProfile>>() {}.type) }.getOrNull()
                } ?: defaults.layoutProfiles,
                soundboardPads   = prefs[Keys.SOUNDBOARD_PADS_JSON]?.let {
                    runCatching {
                        gson.fromJson<List<SoundPadConfig>>(it, object : com.google.gson.reflect.TypeToken<List<SoundPadConfig>>() {}.type)
                    }.getOrNull()
                } ?: defaults.soundboardPads,
                vitalsAsBars     = prefs[Keys.VITALS_AS_BARS] ?: defaults.vitalsAsBars,
                speedometerDigitalOnly = prefs[Keys.SPEEDOMETER_DIGITAL_ONLY] ?: defaults.speedometerDigitalOnly,
                gradientDirection = prefs[Keys.GRADIENT_DIRECTION]?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() } ?: defaults.gradientDirection,
                useCustomBackgroundColor = prefs[Keys.USE_CUSTOM_BG_COLOR] ?: defaults.useCustomBackgroundColor
            ).withoutRetiredWidgets().withMergedClockWeather()
    }

    suspend fun saveSettings(s: AppSettings) {
        context.dataStore.edit { prefs -> writeSettings(prefs, s.rememberLayoutBefore(readSettings(prefs), System.currentTimeMillis())) }
    }

    /**
     * Atomic read-modify-write. DataStore serializes edit blocks, so concurrent
     * updates can't overwrite each other (unlike transforming a stale snapshot).
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs -> val before = readSettings(prefs); writeSettings(prefs, transform(before).rememberLayoutBefore(before, System.currentTimeMillis())) }
    }

    private fun writeSettings(prefs: MutablePreferences, s: AppSettings) {
            prefs[Keys.LAUNCHER_BRIGHTNESS] = s.launcherBrightness
            prefs[Keys.WEATHER_BACKGROUND] = s.weatherBackground
            prefs[Keys.LAYOUT_HISTORY] = gson.toJson(s.layoutHistory)
            prefs[Keys.PREFERRED_MEDIA] = s.preferredMediaPackage
            prefs[Keys.MAP_AUTO_ZOOM] = s.mapAutoZoom
            prefs[Keys.MAP_HEADING_UP] = s.mapHeadingUp
            prefs[Keys.MAP_SOFTWARE_RENDERING] = s.mapSoftwareRendering
            prefs[Keys.VEHICLE_NAME]       = s.vehicleName
            prefs[Keys.ACCENT_COLOR]       = s.accentColor
            prefs[Keys.BG_COLOR]           = s.backgroundColor
            prefs[Keys.FONT_COLOR]         = s.fontColor
            prefs[Keys.WALLPAPER_URI]      = s.wallpaperUri
            prefs[Keys.FONT_BOLD]          = s.fontBold
            prefs[Keys.TEXT_SCALE]         = s.textScale
            prefs[Keys.UI_SCALE]           = s.uiScale
            prefs[Keys.CLOCK_STYLE]        = s.clockStyle.name
            prefs[Keys.USE_12_HOUR_TIME]   = s.use12HourTime
            prefs[Keys.CLOCK_SECONDS]      = s.showClockSeconds
            prefs[Keys.CLOCK_DATE_FORMAT]  = s.clockDateFormat
            prefs[Keys.UNIT_SYSTEM]        = s.unitSystem.name
            prefs[Keys.APP_FONT]           = s.appFont.name
            prefs[Keys.SHOW_WEATHER]       = s.showWeather
            prefs[Keys.SHOW_CLOCK]         = s.showClock
            prefs[Keys.SHOW_TELEMETRY]     = s.showTelemetry
            prefs[Keys.SHOW_NOW_PLAYING]   = s.showNowPlaying
            prefs[Keys.SHOW_ALTIMETER]     = s.showAltimeter
            prefs[Keys.SHOW_SPEEDOMETER]   = s.showSpeedometer
            prefs[Keys.SHORTCUTS_JSON]     = gson.toJson(s.shortcuts)
            prefs[Keys.WIDGET_LAYOUT_JSON] = gson.toJson(s.widgetLayout)
            prefs[Keys.CAR_PLAY_PACKAGE]      = s.carPlayPackage
            prefs[Keys.ANDROID_AUTO_PACKAGE]  = s.androidAutoPackage
            prefs[Keys.USE_GRADIENT]       = s.useGradient
            prefs[Keys.GRADIENT_END_COLOR] = s.gradientEndColor
            prefs[Keys.WALLPAPER_DIM]      = s.wallpaperDim
            prefs[Keys.SIDEBAR_POSITION]           = s.sidebarPosition.name
            prefs[Keys.BOTTOM_BAR_SHORTCUTS_RIGHT] = s.bottomBarShortcutsRight
            prefs[Keys.DAY_NIGHT_MODE]     = s.dayNightMode.name
            prefs[Keys.SHOW_PIP]           = s.showPip
            prefs[Keys.PIP_APP_PACKAGE]    = s.pipAppPackage
            prefs[Keys.RADIO_PACKAGE]      = s.radioPackage
            prefs[Keys.ONBOARDING_COMPLETED] = s.onboardingCompleted
            prefs[Keys.SHOW_VITALS]        = s.showVitals
            prefs[Keys.SHOW_TRIP_TRACKER]  = s.showTripTracker
            prefs[Keys.COMPASS_OFFSET]     = s.compassOffset
            prefs[Keys.SHOW_SOUNDBOARD]    = s.showSoundboard
            prefs[Keys.SHOW_MAP]           = s.showMap
            prefs[Keys.ONLINE_MAP_ENABLED] = s.onlineMapEnabled
            prefs[Keys.SHOW_CONNECTIVITY]  = s.showConnectivity
            prefs[Keys.SHOW_DESTINATIONS]  = s.showDestinations
            prefs[Keys.SHOW_RADAR]         = s.showRadar
            prefs[Keys.SHOW_TRAFFIC]       = s.showTraffic
            prefs[Keys.HOME_DESTINATION]   = s.homeDestination
            prefs[Keys.WORK_DESTINATION]   = s.workDestination
            prefs[Keys.LAYOUT_PROFILES_JSON] = gson.toJson(s.layoutProfiles)
            prefs[Keys.ACTIVE_PROFILE] = s.activeLayoutProfile
            prefs[Keys.AUTO_PROFILES] = s.autoDayNightProfiles
            prefs[Keys.NAVIGATION_PACKAGE] = s.navigationPackage
            prefs[Keys.RECENT_DESTINATIONS] = gson.toJson(s.recentDestinations)
            prefs[Keys.SOUNDBOARD_PADS_JSON] = gson.toJson(s.soundboardPads)
            prefs[Keys.VITALS_AS_BARS]     = s.vitalsAsBars
            prefs[Keys.SPEEDOMETER_DIGITAL_ONLY] = s.speedometerDigitalOnly
            prefs[Keys.GRADIENT_DIRECTION] = s.gradientDirection.name
            prefs[Keys.USE_CUSTOM_BG_COLOR] = s.useCustomBackgroundColor
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
