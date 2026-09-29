package com.openlauncher.app.data

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ReleaseUpdate(val version: String, val pageUrl: String, val apkUrl: String, val notes: String)
fun versionParts(version: String): List<Int> = Regex("[0-9]+\\.[0-9]+\\.[0-9]+").find(version)?.value
    ?.split('.')?.map { it.toIntOrNull() ?: 0 } ?: emptyList()
fun newerVersion(candidate: String, installed: String): Boolean {
    val a = versionParts(candidate); val b = versionParts(installed)
    if (a.size != 3 || b.size != 3) return false
    for (i in 0..2) { if (a[i] != b[i]) return a[i] > b[i] }
    return false
}
fun parseReleases(json: String): List<ReleaseUpdate> = JsonParser.parseString(json).asJsonArray.mapNotNull { element ->
    runCatching {
        val r = element.asJsonObject
        if (r.get("draft")?.asBoolean == true) return@runCatching null
        val assets = r.getAsJsonArray("assets") ?: return@runCatching null
        val apk = assets.firstOrNull { it.asJsonObject.get("name")?.asString == "openlauncher-preview.apk" }
            ?.asJsonObject?.get("browser_download_url")?.asString ?: return@runCatching null
        if (assets.none { it.asJsonObject.get("name")?.asString == "SHA256SUMS.txt" }) return@runCatching null
        val url = r.get("html_url").asString
        if (!url.startsWith("https://github.com/itsTwistys/openlauncher/releases/tag/") ||
            !apk.startsWith("https://github.com/itsTwistys/openlauncher/releases/download/")) return@runCatching null
        ReleaseUpdate(r.get("name").asString, url, apk,
            r.get("body")?.takeUnless { it.isJsonNull }?.asString.orEmpty().take(6000))
    }.getOrNull()
}
object ReleaseUpdates {
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    suspend fun latest(): ReleaseUpdate = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.github.com/repos/itsTwistys/openlauncher/releases?per_page=30")
            .header("Accept", "application/vnd.github+json").header("User-Agent", "OpenLauncher-update-check").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { if (response.code == 403 || response.code == 429) "GitHub rate limit. Try again later." else "GitHub returned ${response.code}. Try again." }
            parseReleases(response.body?.string().orEmpty()).maxWithOrNull { a, b ->
                val x = versionParts(a.version); val y = versionParts(b.version)
                (0..2).map { (x.getOrElse(it) { 0 }).compareTo(y.getOrElse(it) { 0 }) }.firstOrNull { it != 0 } ?: 0
            } ?: error("No installable preview found.")
        }
    }
}
