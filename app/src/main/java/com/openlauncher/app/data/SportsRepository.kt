package com.openlauncher.app.data

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

enum class SportsLeague(val label: String, val path: String) {
    NFL("NFL", "football/nfl"), NBA("NBA", "basketball/nba"), NHL("NHL", "hockey/nhl"),
    MLB("MLB", "baseball/mlb"), F1("F1", "racing/f1")
}
enum class SportsView(val label: String) { LIVE("Live"), RESULTS("Results"), UPCOMING("Upcoming") }
data class SportsCompetitor(val name: String, val abbreviation: String, val score: String? = null,
                            val winner: Boolean = false, val position: Int? = null)
data class SportsEvent(val id: String, val title: String, val startMs: Long, val state: String,
                       val detail: String, val competitors: List<SportsCompetitor> = emptyList(), val timeKnown: Boolean = true)
data class SportsSnapshot(val league: SportsLeague, val view: SportsView, val events: List<SportsEvent>,
                          val updatedAtMs: Long, val source: String, val partial: Boolean = false)

/** Public score feeds have no delivery SLA. Retain dated results and never invent a score or live state. */
class SportsRepository(context: Context, private val readOverride: ((String) -> String)? = null) {
    private val directory = File(context.filesDir, "sports-cache").apply { mkdirs() }
    private val gson = Gson()
    private val days = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, String>>()
    private fun file(league: SportsLeague, view: SportsView) = AtomicFile(File(directory, "${league.name}-${view.name}.json"))

    suspend fun cached(league: SportsLeague, view: SportsView, now: Long): SportsSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            file(league, view).openRead().use { input ->
                val bytes = input.readBytesBounded(512_000)
                gson.fromJson(String(bytes, Charsets.UTF_8), SportsSnapshot::class.java).takeIf {
                    it.league == league && it.view == view && it.updatedAtMs > 0 && now - it.updatedAtMs >= -300_000 &&
                        it.events.size <= 200 && it.events.all { e -> e.startMs > 0 && e.title.length <= 180 && e.state in setOf("pre", "in", "post") }
                }
            }
        }.getOrNull()
    }
    suspend fun fetch(league: SportsLeague, view: SportsView, now: Long, zone: TimeZone): SportsSnapshot = withContext(Dispatchers.IO) {
        val result = if (league == SportsLeague.F1 && view != SportsView.LIVE) {
            val year = Calendar.getInstance(zone).apply { timeInMillis = now }.get(Calendar.YEAR)
            if (view == SportsView.RESULTS) {
                val current = SportsFeeds.f1Results(read("https://api.jolpi.ca/ergast/f1/$year/last/results.json"), now)
                if (current.events.isNotEmpty()) current else SportsFeeds.f1Results(read("https://api.jolpi.ca/ergast/f1/${year - 1}/last/results.json"), now)
            }
            else {
                val current = SportsFeeds.f1Schedule(read("https://api.jolpi.ca/ergast/f1/$year/races.json?limit=100"), now, zone)
                if (current.events.isNotEmpty()) current else SportsFeeds.f1Schedule(read("https://api.jolpi.ca/ergast/f1/${year + 1}/races.json?limit=100"), now, zone)
            }
        } else {
            val permits = Semaphore(3)
            val responses = coroutineScope {
                SportsFeeds.urls(league, view, now, zone).map { url -> async {
                    permits.withPermit {
                        ensureActive()
                        try {
                            val cached = days[url]?.takeIf { now - it.first in 0..25_000 }
                            val response = cached ?: (now to read(url)).also {
                                if (days.size >= 24) days.entries.minByOrNull { entry -> entry.value.first }?.let { entry -> days.remove(entry.key) }
                                days[url] = it
                            }
                            SportsFeeds.scoreboard(response.second, league, view, now)
                            Result.success(response)
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { Result.failure<Pair<Long, String>>(e) }
                    }
                } }.awaitAll()
            }
            val successful = responses.mapNotNull { it.getOrNull() }
            if (successful.isEmpty()) throw requireNotNull(responses.first().exceptionOrNull())
            val events = com.google.gson.JsonArray()
            successful.forEach { (_, json) ->
                val root = JsonParser.parseString(json).asJsonObject
                require(root.has("events") && root["events"].isJsonArray) { "Invalid score response" }
                root["events"].asJsonArray.forEach { events.add(it) }
            }
            SportsFeeds.scoreboard(JsonObject().apply { add("events", events) }.toString(), league, view,
                successful.minOf { it.first }).copy(partial = successful.size != responses.size)
        }
        ensureActive()
        try {
            val cache = file(league, view); val output = cache.startWrite()
            try { output.write(gson.toJson(result).toByteArray(Charsets.UTF_8)); cache.finishWrite(output) }
            catch (e: java.io.IOException) { cache.failWrite(output); throw e }
        } catch (_: java.io.IOException) { /* A response can still be used if storage is full. */ }
        result
    }
    private fun read(url: String): String {
        readOverride?.let { return it(url) }
        var target = URL(url)
        repeat(3) {
            val connection = target.openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000; connection.readTimeout = 8_000; connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "OpenLauncher/${com.openlauncher.app.BuildConfig.VERSION_NAME} (+https://github.com/itsTwistys/openlauncher)")
            connection.setRequestProperty("Accept", "application/json")
            try {
                val code = connection.responseCode
                if (code in setOf(301, 302, 303, 307, 308)) {
                    val next = URL(target, connection.getHeaderField("Location") ?: error("Invalid score redirect"))
                    require(next.protocol == "https" && next.host in setOf("site.api.espn.com", "api.jolpi.ca")) { "Unsupported score redirect" }
                    target = next
                } else {
                    if (code != 200) throw SportsHttpException(code)
                    return connection.inputStream.use { input ->
                        val decoded = if (connection.contentEncoding.equals("gzip", true)) java.util.zip.GZIPInputStream(input) else input
                        String(decoded.readBytesBounded(5_000_000), Charsets.UTF_8)
                    }
                }
            } finally { connection.disconnect() }
        }
        error("Too many score redirects")
    }
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) { val count = read(buffer); if (count < 0) break; require(output.size() + count <= limit); output.write(buffer, 0, count) }
    return output.toByteArray()
}

internal class SportsHttpException(val code: Int) : java.io.IOException("Scores service returned HTTP $code")
internal fun sportsErrorLabel(error: Exception): String = when (error) {
    is SportsHttpException -> "Scores service HTTP ${error.code} · tap Retry"
    is java.net.SocketTimeoutException -> "Scores request timed out · tap Retry"
    is java.io.IOException -> "Could not connect to scores · tap Retry"
    else -> "Score response unavailable · tap Retry"
}

internal object SportsFeeds {
    private fun JsonObject.text(key: String) = runCatching { get(key)?.takeUnless { it.isJsonNull }?.asString }.getOrNull()?.take(180).orEmpty()
    private fun JsonObject.obj(key: String) = runCatching { getAsJsonObject(key) }.getOrNull() ?: JsonObject()
    private fun JsonObject.rows(key: String) = runCatching { getAsJsonArray(key)?.mapNotNull { it.takeIf { row -> row.isJsonObject }?.asJsonObject } }.getOrNull().orEmpty()
    private fun JsonObject.flag(key: String) = runCatching { get(key)?.asBoolean == true }.getOrDefault(false)
    fun timestamp(value: String): Long? {
        val clean = value.replace(Regex("\\.\\d+Z$"), "Z")
        val pattern = when {
            clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) -> "yyyy-MM-dd'T'HH:mm:ss'Z'"
            clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z")) -> "yyyy-MM-dd'T'HH:mm'Z'"
            clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> "yyyy-MM-dd"
            else -> return null
        }
        return runCatching { SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }.parse(clean)?.time }.getOrNull()
    }
    fun urls(league: SportsLeague, view: SportsView, now: Long, zone: TimeZone): List<String> {
        // ESPN team scoreboards reject YYYYMMDD-YYYYMMDD ranges with HTTP 400.
        val offsets = when (view) { SportsView.LIVE -> -1..1; SportsView.RESULTS -> -6..0; SportsView.UPCOMING -> 0..13 }
        val format = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = zone }
        return offsets.map { offset ->
            val day = Calendar.getInstance(zone).apply { timeInMillis = now; add(Calendar.DATE, offset) }
            "https://site.api.espn.com/apis/site/v2/sports/${league.path}/scoreboard?dates=${format.format(day.time)}&limit=200"
        }
    }
    fun scoreboard(json: String, league: SportsLeague, view: SportsView, now: Long): SportsSnapshot {
        val root = JsonParser.parseString(json).asJsonObject
        require(root.has("events") && root["events"].isJsonArray) { "Invalid score response" }
        val parsed = root.rows("events").flatMap { event ->
            event.rows("competitions").mapNotNull { competition ->
                val type = competition.obj("status").obj("type").takeIf { it.entrySet().isNotEmpty() } ?: event.obj("status").obj("type")
                val state = type.text("state"); val date = timestamp(competition.text("date").ifBlank { event.text("date") }) ?: return@mapNotNull null
                if (state !in setOf("pre", "in", "post")) return@mapNotNull null
                val competitors = competition.rows("competitors").sortedBy { if (it.text("homeAway") == "away") 0 else 1 }.map { c ->
                    val team = c.obj(if (league == SportsLeague.F1) "athlete" else "team")
                    SportsCompetitor(team.text("displayName").ifBlank { team.text("fullName") }, team.text("abbreviation").ifBlank { team.text("shortName") },
                        c.text("score").takeIf { state != "pre" && it.matches(Regex("\\d{1,3}")) }, state == "post" && c.flag("winner"))
                }.filter { it.name.isNotBlank() }
                if (league != SportsLeague.F1 && competitors.size != 2) return@mapNotNull null
                val session = if (league == SportsLeague.F1) competition.obj("type").text("abbreviation") else ""
                SportsEvent(competition.text("id").ifBlank { event.text("id") }, event.text("shortName").ifBlank { event.text("name") } + if (session.isBlank()) "" else " · $session",
                    date, state, type.text("shortDetail").ifBlank { type.text("description") }, competitors, !competition.has("timeValid") || competition.flag("timeValid"))
            }
        }
        require(parsed.isNotEmpty() || root.rows("events").isEmpty()) { "Unrecognized score data" }
        val unique = parsed.distinctBy { it.id }
        val events = when (view) {
            SportsView.LIVE -> unique.filter { it.state == "in" }.sortedBy { it.startMs }.ifEmpty {
                unique.filter { it.state == "post" }.sortedByDescending { it.startMs }.take(4) +
                    unique.filter { it.state == "pre" && it.startMs >= now }.sortedBy { it.startMs }.take(4)
            }
            SportsView.RESULTS -> unique.filter { it.state == "post" }.sortedByDescending { it.startMs }
            SportsView.UPCOMING -> unique.filter { it.state == "pre" && it.startMs >= now }.sortedBy { it.startMs }
        }.take(200)
        return SportsSnapshot(league, view, events, now, "ESPN")
    }
    fun f1Results(json: String, now: Long): SportsSnapshot {
        val table = JsonParser.parseString(json).asJsonObject.obj("MRData").obj("RaceTable")
        require(table.has("Races")) { "Invalid race response" }
        val events = table.rows("Races").mapNotNull { race ->
            val date = timestamp(race.text("date") + "T" + race.text("time").ifBlank { "00:00:00Z" }) ?: return@mapNotNull null
            val drivers = race.rows("Results").mapNotNull { result ->
                val position = result.text("position").toIntOrNull()?.takeIf { it in 1..100 } ?: return@mapNotNull null
                val driver = result.obj("Driver")
                SportsCompetitor((driver.text("givenName") + " " + driver.text("familyName")).trim(), driver.text("code"), position = position, winner = position == 1)
            }.sortedBy { it.position }
            if (drivers.isEmpty()) return@mapNotNull null
            SportsEvent("f1-${race.text("season")}-${race.text("round")}", race.text("raceName"), date, "post", "Final", drivers, race.text("time").isNotBlank())
        }
        return SportsSnapshot(SportsLeague.F1, SportsView.RESULTS, events, now, "Jolpica F1")
    }
    fun f1Schedule(json: String, now: Long, zone: TimeZone = TimeZone.getDefault()): SportsSnapshot {
        val table = JsonParser.parseString(json).asJsonObject.obj("MRData").obj("RaceTable")
        require(table.has("Races")) { "Invalid race response" }
        val events = table.rows("Races").flatMap { race ->
            (listOf("FirstPractice" to "Practice 1", "SecondPractice" to "Practice 2", "ThirdPractice" to "Practice 3", "Qualifying" to "Qualifying", "Sprint" to "Sprint") + ("" to "Race")).mapNotNull { (key, label) ->
                val session = if (key.isBlank()) race else race.obj(key)
                val known = session.text("time").isNotBlank()
                val date = timestamp(session.text("date") + if (known) "T" + session.text("time") else "") ?: return@mapNotNull null
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date(now))
                if (if (known) date < now else session.text("date") < today) return@mapNotNull null
                SportsEvent("f1-${race.text("season")}-${race.text("round")}-$key", race.text("raceName") + " · $label", date, "pre", "Scheduled", timeKnown = known)
            }
        }.sortedBy { it.startMs }.take(30)
        return SportsSnapshot(SportsLeague.F1, SportsView.UPCOMING, events, now, "Jolpica F1")
    }
}
