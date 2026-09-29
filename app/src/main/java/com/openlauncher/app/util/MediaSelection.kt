package com.openlauncher.app.util

/** An explicit preference never falls back to an unrelated app. */
fun <T> selectMediaSession(sessions: List<T>, preferredPackage: String,
                          packageName: (T) -> String, playing: (T) -> Boolean): T? {
    val matching = if (preferredPackage.isBlank()) sessions else sessions.filter { packageName(it) == preferredPackage }
    return matching.firstOrNull(playing) ?: matching.firstOrNull()
}
