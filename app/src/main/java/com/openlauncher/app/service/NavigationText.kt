package com.openlauncher.app.service

import android.app.Notification

internal data class NavigationText(
    val title: String, val instruction: String, val trip: String,
    val followingSteps: List<String> = emptyList()
)

/** Notification fields are presentation text, not a route API. Only explicit sequences become steps. */
internal fun navigationText(notification: Notification): NavigationText {
    val extras = notification.extras
    fun clean(value: CharSequence?) = value?.toString()?.replace(Regex("\\s+"), " ")?.trim().orEmpty().take(500)
    fun text(key: String) = clean(extras.getCharSequence(key))
    val title = text(Notification.EXTRA_TITLE).ifBlank { text(Notification.EXTRA_TITLE_BIG) }
    val tripParts = listOf(text(Notification.EXTRA_SUB_TEXT), text(Notification.EXTRA_SUMMARY_TEXT), text(Notification.EXTRA_INFO_TEXT))
        .filter { it.isNotBlank() && !it.equals(title, true) }.distinct()
    val trip = tripParts.joinToString(" · ").take(500)
    val sequenceMarker = "(?:(?:then|after that)(?:[:,]\\s*|\\s+(?=(?:turn|keep|bear|continue|head|go|take|merge|exit|enter|make|arrive|in)\\b))|next\\s*:\\s*)\\s*"
    val marker = Regex("^$sequenceMarker", RegexOption.IGNORE_CASE)
    val number = Regex("^(\\d{1,2})[.)]\\s+")
    val summary = Regex("^(?:ETA\\b|arrival(?: time)?\\s*[:]|\\d+\\s*(?:min(?:ute)?s?|h(?:ou)?rs?)\\b)", RegexOption.IGNORE_CASE)
    fun pieces(value: CharSequence?) = value?.toString().orEmpty().take(8000).lines().take(30).flatMap { line ->
        line.split(Regex("(?:[,;]\\s*|\\s+)(?=$sequenceMarker)", RegexOption.IGNORE_CASE))
    }.map(::clean).filter { it.isNotBlank() }
    val base = pieces(extras.getCharSequence(Notification.EXTRA_TEXT))
    val expanded = pieces(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
    val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty().take(30).flatMap(::pieces)
    val current = mutableListOf<String>()
    val following = mutableListOf<String>()
    // Numbered steps are accepted only after an earlier numbered current instruction in that field.
    for (field in listOf(base, expanded, lines)) {
        var previousNumber: Int? = null
        for (raw in field) {
            val numbered = number.find(raw)
            val sequence = numbered?.groupValues?.get(1)?.toIntOrNull()
            val value = raw.replace(number, "").replace(marker, "").trim()
            val explicitlyFollowing = marker.containsMatchIn(raw) ||
                (sequence != null && previousNumber?.let { sequence == it + 1 } == true)
            if (sequence != null) previousNumber = sequence
            if (value.isBlank() || value.equals(title, true) || tripParts.any { it.equals(value, true) } ||
                summary.containsMatchIn(value)) continue
            if (explicitlyFollowing) following += value else current += value
        }
    }
    val primary = current.distinctBy { it.lowercase(java.util.Locale.ROOT) }.filterNot { candidate ->
        current.any { other -> !other.equals(candidate, true) && other.contains(candidate, ignoreCase = true) }
    }.joinToString(" · ").take(1000)
    val steps = following.distinctBy { it.lowercase(java.util.Locale.ROOT) }
        .filterNot { step -> current.any { it.equals(step, true) || it.contains(step, true) } }.take(3)
    return NavigationText(title, primary, trip, steps)
}
