package com.openlauncher.app.service

import android.app.Notification

internal data class NavigationText(val title: String, val instruction: String, val trip: String)

/** Keep instruction and trip fields separate; never infer a route from notification wording. */
internal fun navigationText(notification: Notification): NavigationText {
    val extras = notification.extras
    fun clean(value: CharSequence?) = value?.toString()?.replace(Regex("\\s+"), " ")?.trim().orEmpty().take(500)
    fun text(key: String) = clean(extras.getCharSequence(key))
    val title = text(Notification.EXTRA_TITLE).ifBlank { text(Notification.EXTRA_TITLE_BIG) }
    val trip = listOf(text(Notification.EXTRA_SUB_TEXT), text(Notification.EXTRA_SUMMARY_TEXT), text(Notification.EXTRA_INFO_TEXT))
        .filter { it.isNotBlank() && it != title }.distinct().joinToString(" · ").take(500)
    val base = text(Notification.EXTRA_TEXT)
    val expanded = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().lines().map(::clean)
    val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty().map(::clean)
    val parts = (listOf(base) + expanded + lines).filter { it.isNotBlank() && it != title && it != trip }.distinct()
    val instruction = parts.filterNot { candidate -> parts.any { other -> other != candidate && other.contains(candidate) } }
        .joinToString(" · ").take(1000)
    return NavigationText(title, instruction, trip)
}
