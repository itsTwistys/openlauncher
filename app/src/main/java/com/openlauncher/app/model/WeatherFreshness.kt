package com.openlauncher.app.model

/** Wall-clock age is visible even offline; future timestamps never imply fresh weather. */
fun WeatherState.freshnessLabel(nowMs: Long, online: Boolean): String {
    val age = nowMs - updatedAtMs
    val prefix = if (online) "Updated" else "Offline · updated"
    return when {
        updatedAtMs <= 0 || age < 0 -> "Update time unavailable"
        age < 60_000 -> "$prefix just now"
        age < 3_600_000 -> "$prefix ${age / 60_000}m ago" + if (age >= 1_800_000) " · stale" else ""
        age < 48 * 3_600_000L -> "$prefix ${age / 3_600_000}h ago · stale"
        else -> "Forecast expired · reconnect to refresh"
    }
}
