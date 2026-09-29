package com.openlauncher.app.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Versioned, local JSON export. No network service receives the backup. */
object SettingsBackup {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val ids = setOf("CLOCK", "WEATHER", "NOW_PLAYING", "TELEMETRY", "ALTIMETER", "SPEEDOMETER",
        "VITALS", "TRIP_TRACKER", "SOUNDBOARD", "MAP", "CONNECTIVITY", "DESTINATIONS", "RADAR", "TRAFFIC")

    fun encode(settings: AppSettings): String = gson.toJson(JsonObject().apply {
        addProperty("format", "openlauncher-settings")
        addProperty("version", 1)
        add("settings", gson.toJsonTree(settings))
    })

    fun decode(json: String): AppSettings {
        require(json.length <= 1_000_000) { "Backup is too large" }
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("format")?.asString == "openlauncher-settings" && root.get("version")?.asInt == 1) {
            "Unsupported backup format"
        }
        val input = root.getAsJsonObject("settings") ?: error("Settings are missing")
        // Merge onto defaults so older backups can omit newly added preferences.
        val merged = gson.toJsonTree(AppSettings()).asJsonObject
        input.entrySet().forEach { (key, value) ->
            require(!value.isJsonNull) { "Invalid setting: $key" }
            if (merged.has(key)) merged.add(key, value)
        }
        val enumOptions = mapOf(
            "clockStyle" to ClockStyle.entries.map { it.name },
            "unitSystem" to UnitSystem.entries.map { it.name },
            "appFont" to AppFont.entries.map { it.name },
            "dayNightMode" to DayNightMode.entries.map { it.name },
            "sidebarPosition" to SidebarPosition.entries.map { it.name },
            "gradientDirection" to GradientDirection.entries.map { it.name }
        )
        enumOptions.forEach { (key, values) -> require(merged.get(key).asString in values) { "Invalid display option" } }
        val s = gson.fromJson(merged, AppSettings::class.java).withoutRetiredWidgets().withMergedClockWeather()
        require(s.uiScale in 0.6f..2f && s.textScale in 0.6f..2f && s.wallpaperDim in 0f..1f) { "Invalid display scale" }
        require(s.vehicleName.length <= 100 && s.homeDestination.length <= 500 && s.workDestination.length <= 500)
        require(s.clockDateFormat in setOf("LONG", "SHORT"))
        require(s.recentDestinations.size <= 5 && s.recentDestinations.all { it.length <= 500 })
        require(s.navigationPackage in setOf("", "com.google.android.apps.maps", "com.waze"))
        require(s.widgetLayout.size <= ids.size && s.widgetLayout.all { it.id in ids && validWidgetLayout(listOf(it)) })
        require(s.widgetLayout.map { it.id }.distinct().size == s.widgetLayout.size)
        require(validWidgetLayout(s.widgetLayout.filter { it.enabled && it.id in s.activeWidgetIds() })) { "Widgets overlap or are outside the grid" }
        require(s.layoutProfiles.size <= 4 && s.layoutProfiles.map { it.name }.distinct().size == s.layoutProfiles.size)
        s.layoutProfiles.forEach { p ->
            require(p.name in setOf("Driving", "Parked", "Day", "Night"))
            require(p.enabledIds.all { it in ids } && p.layout.size <= ids.size && p.layout.all { it.id in ids && validWidgetLayout(listOf(it)) })
            require(p.enabledIds.all { id -> p.layout.any { it.id == id } })
            require(p.layout.map { it.id }.distinct().size == p.layout.size) { "Duplicate widget in saved layout" }
            require(validWidgetLayout(p.layout.filter { it.enabled && it.id in p.enabledIds })) { "Invalid saved layout" }
        }
        require(s.shortcuts.size <= 128 && s.soundboardPads.size <= 6)
        require(s.shortcuts.all { it.defaultIcon in DefaultShortcutIcon.entries })
        // Device file grants cannot move between head units. Online location sharing is opt-in again.
        return s.copy(wallpaperUri = "", soundboardPads = s.soundboardPads.map { it.copy(audioUri = "") },
            onlineMapEnabled = false)
    }
}
