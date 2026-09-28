package com.openlauncher.app.util

import java.net.URI
import java.net.URLDecoder

/** Only extract a video ID; never load an arbitrary user-provided URL into the player. */
fun youtubeVideoId(input: String): String? = runCatching {
    val value = input.trim()
    if (value.length > 2048) return null
    val uri = URI(if (value.startsWith("https://") || value.startsWith("http://")) value else "https://$value")
    if (uri.scheme !in setOf("https", "http") || uri.userInfo != null || uri.port != -1) return null
    val host = uri.host?.lowercase() ?: return null
    val parts = uri.path.orEmpty().trim('/').split('/')
    val id = when (host) {
        "youtu.be" -> parts.singleOrNull()
        "youtube.com", "www.youtube.com", "m.youtube.com" -> when {
            parts.size == 1 && parts[0] == "watch" -> uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("v=") }?.substringAfter("v=")?.let { URLDecoder.decode(it, "UTF-8") }
            parts.size == 2 && parts[0] in setOf("live", "shorts", "embed") -> parts[1]
            else -> null
        }
        else -> null
    }
    id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}.getOrNull()
