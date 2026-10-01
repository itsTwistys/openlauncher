package com.openlauncher.app.service

/** Conservative text fallback when a provider does not supply a maneuver image. */
internal enum class TurnManeuver {
    LEFT, RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, UTURN_LEFT, UTURN_RIGHT,
    ROUNDABOUT_LEFT, ROUNDABOUT_RIGHT, STRAIGHT, ARRIVE, UNKNOWN
}
internal data class TurnCue(val distance: String, val instruction: String, val maneuver: TurnManeuver)
internal fun turnCue(title: String, details: String): TurnCue {
    val distancePattern = Regex("^(?:in\\s+)?(\\d+(?:[.,]\\d+)?\\s*(?:ft|feet|mi|mile(?:s)?|m|km|meter(?:s)?|metre(?:s)?))(?=\\s|$)", RegexOption.IGNORE_CASE)
    val match = distancePattern.find(title.trim())
    val localizedDistance = Regex("\\d+(?:[.,]\\d+)?\\s*(?:ft|feet|mi|miles?|km|m)(?=\\s|$)", RegexOption.IGNORE_CASE).find(title)
    val distance = match?.groupValues?.get(1) ?: localizedDistance?.value.orEmpty()
    val titleRemainder = if (match == null) title.trim() else title.trim().removeRange(match.range).trim(' ', '·', '-', ':')
    // Trip totals belong in the details sheet, not the next-turn line.
    val instruction = (if (localizedDistance != null && details.isNotBlank()) details
        else titleRemainder.ifBlank { details }).split(" · ").first().trim()
    val text = "$title $details".lowercase(java.util.Locale.ROOT)
    fun has(pattern: String) = Regex(pattern).containsMatchIn(text)
    val maneuver = when {
        has("\\b(?:left\\s+u[- ]?turn|u[- ]?turn\\s+(?:to the\\s+)?left)\\b") -> TurnManeuver.UTURN_LEFT
        has("\\b(?:right\\s+u[- ]?turn|u[- ]?turn\\s+(?:to the\\s+)?right)\\b") -> TurnManeuver.UTURN_RIGHT
        has("\\bleft\\s+(?:at|onto|around)\\s+(?:the\\s+)?(?:roundabout|traffic circle)\\b|\\b(?:roundabout|traffic circle)\\s+(?:to the\\s+)?left\\b") -> TurnManeuver.ROUNDABOUT_LEFT
        has("\\bright\\s+(?:at|onto|around)\\s+(?:the\\s+)?(?:roundabout|traffic circle)\\b|\\b(?:roundabout|traffic circle)\\s+(?:to the\\s+)?right\\b") -> TurnManeuver.ROUNDABOUT_RIGHT
        has("\\bu[- ]?turn\\b|\\broundabout\\b|\\btraffic circle\\b") -> TurnManeuver.UNKNOWN
        has("\\b(?:arrive|arrived|destination is)\\b") -> TurnManeuver.ARRIVE
        has("\\b(?:slight|bear|keep) left\\b") -> TurnManeuver.SLIGHT_LEFT
        has("\\b(?:slight|bear|keep) right\\b") -> TurnManeuver.SLIGHT_RIGHT
        has("\\bturn left\\b|\\bsharp left\\b") -> TurnManeuver.LEFT
        has("\\bturn right\\b|\\bsharp right\\b") -> TurnManeuver.RIGHT
        has("\\b(?:continue|head|go) straight\\b") -> TurnManeuver.STRAIGHT
        else -> TurnManeuver.UNKNOWN
    }
    return TurnCue(distance, instruction.ifBlank { "Navigation active" }, maneuver)
}
