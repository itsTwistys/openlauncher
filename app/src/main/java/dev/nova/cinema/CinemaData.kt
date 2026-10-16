package dev.nova.cinema

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private const val CINEMETA = "https://v3-cinemeta.strem.io/manifest.json"

data class Movie(
    val id: String, val type: String, val title: String, val poster: String = "",
    val backdrop: String = "", val description: String = "", val year: String = "",
    val rating: String = "", val runtime: String = "",
    val genres: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(), val source: String = CINEMETA,
)
data class Episode(
    val id: String, val title: String, val season: Int, val number: Int,
    val thumbnail: String = "", val overview: String = "",
)
data class SubtitleTrack(val url: String, val language: String, val mimeType: String)
data class Stream(
    val label: String, val description: String, val url: String = "",
    val external: String = "", val infoHash: String = "", val ytId: String = "",
    val addonName: String, val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList(),
) {
    val direct get() = url.startsWith("https://", true) || url.startsWith("http://", true)
}
data class AddonCatalog(val id: String, val type: String, val name: String, val search: Boolean, val requiredExtras: List<String>)
data class Addon(
    val id: String, val name: String, val description: String, val url: String,
    val types: List<String>, val resources: List<String>, val prefixes: List<String>,
    val catalogs: List<AddonCatalog>,
) {
    fun supports(resource: String, type: String, id: String): Boolean =
        resources.contains(resource) && (types.isEmpty() || type in types) &&
            (prefixes.isEmpty() || prefixes.any { id.startsWith(it) })
}
data class MediaRow(val title: String, val movies: List<Movie>)
data class Playing(val movie: Movie, val episode: Episode?, val stream: Stream)

private fun JSONObject.str(key: String): String = optString(key, "").takeUnless { it == "null" }.orEmpty()
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

private fun Movie.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("type", type); put("name", title); put("poster", poster)
    put("background", backdrop); put("description", description); put("year", year)
    put("imdbRating", rating); put("runtime", runtime); put("genres", JSONArray(genres))
    put("source", source)
}
private fun JSONObject.toMovie(source: String): Movie {
    val videos = optJSONArray("videos")
    val episodes = if (videos == null) emptyList() else (0 until videos.length()).mapNotNull { i ->
        val v = videos.optJSONObject(i) ?: return@mapNotNull null
        val videoId = v.str("id")
        if (videoId.isBlank()) return@mapNotNull null
        Episode(videoId, v.str("title").ifBlank { "Episode ${v.optInt("episode", 0)}" },
            v.optInt("season", -1), v.optInt("episode", 0),
            v.str("thumbnail"), v.str("overview"))
    }.filter { it.season >= 0 }.sortedWith(compareBy({ it.season }, { it.number }))
    return Movie(
        id = str("id"), type = str("type"), title = str("name"),
        poster = str("poster"), backdrop = str("background"),
        description = str("description"),
        year = str("year").ifBlank { str("releaseInfo").take(4) },
        rating = str("imdbRating"), runtime = str("runtime"),
        genres = optJSONArray("genres").strings(),
        episodes = episodes, source = str("source").ifBlank { source },
    )
}

class StremioClient(context: Context) {
    private val prefs = context.getSharedPreferences("nova_library", Context.MODE_PRIVATE)
    private val cache = ConcurrentHashMap<String, Pair<Long, JSONObject>>()
    private var addons: List<Addon> = emptyList()

    private fun readJson(url: String): JSONObject {
        val cached = cache[url]
        if (cached != null && System.currentTimeMillis() - cached.first < 10 * 60_000) return cached.second
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 11000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "NovaCinema/1.0 (Stremio addon client)")
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { reader ->
                val out = StringBuilder()
                val chunk = CharArray(4096)
                while (true) {
                    val count = reader.read(chunk)
                    if (count < 0) break
                    out.append(chunk, 0, count)
                    if (out.length > 2_000_000) error("Response exceeds safety limit")
                }
                out.toString()
            }
            return JSONObject(body).also { cache[url] = System.currentTimeMillis() to it }
        } finally { connection.disconnect() }
    }

    private fun normalized(input: String): String {
        var url = input.trim()
        if (url.startsWith("stremio://", ignoreCase = true))
            url = "https://" + url.substringAfter("://")
        val uri = Uri.parse(url)
        require(uri.scheme == "https" || uri.scheme == "http") { "Use an HTTPS or HTTP add-on manifest link." }
        require(!uri.host.isNullOrBlank()) { "Enter a valid Stremio add-on address." }
        require(!uri.encodedAuthority.orEmpty().contains('@')) { "Embedded credentials are not supported." }
        if (!uri.path.orEmpty().endsWith("/manifest.json")) url = url.trimEnd('/') + "/manifest.json"
        return url
    }

    private fun manifest(url: String): Addon {
        val raw = readJson(url)
        val id = raw.str("id")
        require(id.isNotBlank()) { "That address does not return a Stremio manifest." }
        val resources = mutableListOf<String>()
        raw.optJSONArray("resources")?.let { a ->
            for (i in 0 until a.length()) {
                val entry = a.opt(i)
                val resource = when (entry) {
                    is String -> entry
                    is JSONObject -> entry.str("name")
                    else -> ""
                }
                if (resource.isNotBlank()) resources.add(resource)
            }
        }
        val catalogs = mutableListOf<AddonCatalog>()
        raw.optJSONArray("catalogs")?.let { a ->
            for (i in 0 until a.length()) {
                val cat = a.optJSONObject(i) ?: continue
                val extras = cat.optJSONArray("extra")
                val required = mutableListOf<String>()
                var search = false
                if (extras != null) for (j in 0 until extras.length()) {
                    val extra = extras.optJSONObject(j) ?: continue
                    if (extra.str("name") == "search") search = true
                    if (extra.optBoolean("isRequired") && extra.str("name").isNotBlank()) required.add(extra.str("name"))
                }
                catalogs.add(AddonCatalog(cat.str("id"), cat.str("type"),
                    cat.str("name").ifBlank { "Explore" }, search, required))
            }
        }
        return Addon(id, raw.str("name").ifBlank { id }, raw.str("description"), url,
            raw.optJSONArray("types").strings(), resources.distinct(),
            raw.optJSONArray("idPrefixes").strings(), catalogs)
    }

    suspend fun loadAddons(): List<Addon> = withContext(Dispatchers.IO) {
        val arr = runCatching { JSONArray(prefs.getString("addons", "[]")) }.getOrDefault(JSONArray())
        val urls = listOf(CINEMETA) + (0 until arr.length()).map { arr.optString(it) }
        val found = urls.distinct().mapNotNull { url ->
            runCatching { manifest(url) }.getOrElse {
                if (url == CINEMETA) defaultCinemeta() else null
            }
        }
        addons = found
        found
    }

    private fun defaultCinemeta() = Addon(
        "com.linvo.cinemeta", "Cinemeta", "Official movie and series metadata",
        CINEMETA, listOf("movie", "series"), listOf("catalog", "meta"),
        listOf("tt"), listOf(
            AddonCatalog("top", "movie", "Popular movies", true, emptyList()),
            AddonCatalog("top", "series", "Popular series", true, emptyList()),
            AddonCatalog("imdbRating", "movie", "Acclaimed movies", false, emptyList()),
        ))

    suspend fun install(input: String): List<Addon> = withContext(Dispatchers.IO) {
        val url = normalized(input)
        val addon = manifest(url)
        require(addons.none { it.id == addon.id }) { "This add-on is already installed." }
        val arr = runCatching { JSONArray(prefs.getString("addons", "[]")) }.getOrDefault(JSONArray())
        arr.put(url)
        prefs.edit().putString("addons", arr.toString()).apply()
        cache.clear()
        loadAddons()
    }

    suspend fun uninstall(id: String): List<Addon> = withContext(Dispatchers.IO) {
        require(id != "com.linvo.cinemeta") { "Cinemeta provides the built-in catalog." }
        val urls = addons.filterNot { it.id == id || it.url == CINEMETA }.map { it.url }
        prefs.edit().putString("addons", JSONArray(urls).toString()).apply()
        cache.clear()
        loadAddons()
    }

    private fun endpoint(addon: Addon, resource: String, type: String, id: String, extra: String = ""): String {
        val root = addon.url.removeSuffix("manifest.json").trimEnd('/')
        val base = "$root/$resource/${Uri.encode(type)}/${Uri.encode(id)}"
        return base + (if (extra.isNotBlank()) "/$extra" else "") + ".json"
    }

    suspend fun homeRows(type: String? = null): List<MediaRow> = supervisorScope {
        val specs = addons.flatMap { addon ->
            addon.catalogs.filter { c ->
                c.id.isNotBlank() && c.type in listOf("movie", "series") &&
                    (type == null || c.type == type) && c.requiredExtras.isEmpty()
            }.take(if (addon.url == CINEMETA) 4 else 2).map { addon to it }
        }.take(10)
        specs.map { (addon, cat) ->
            async(Dispatchers.IO) {
                runCatching {
                    val arr = readJson(endpoint(addon, "catalog", cat.type, cat.id)).optJSONArray("metas") ?: JSONArray()
                    val items = (0 until minOf(arr.length(), 25)).mapNotNull { i ->
                        arr.optJSONObject(i)?.toMovie(addon.url)?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
                    }
                    val name = if (addon.url == CINEMETA) {
                        when ("${cat.type}:${cat.id}") {
                            "movie:top" -> "Trending films"
                            "series:top" -> "Series worth staying in for"
                            "movie:imdbRating" -> "Critically acclaimed"
                            "series:imdbRating" -> "Top-rated series"
                            else -> cat.name
                        }
                    } else "${addon.name} · ${cat.name}"
                    MediaRow(name, items.distinctBy { it.id })
                }.getOrNull()
            }
        }.awaitAll().filterNotNull().filter { it.movies.isNotEmpty() }
    }

    suspend fun search(query: String): List<Movie> = supervisorScope {
        val tasks = addons.flatMap { addon ->
            addon.catalogs.filter { it.search && it.type in listOf("movie", "series") &&
                it.requiredExtras.all { extra -> extra == "search" }
            }.take(3).map { addon to it }
        }.take(12)
        tasks.map { (addon, cat) ->
            async(Dispatchers.IO) {
                runCatching {
                    val url = endpoint(addon, "catalog", cat.type, cat.id, "search=${Uri.encode(query)}")
                    val arr = readJson(url).optJSONArray("metas") ?: JSONArray()
                    (0 until minOf(20, arr.length())).mapNotNull { i ->
                        arr.optJSONObject(i)?.toMovie(addon.url)?.takeIf { it.id.isNotBlank() }
                    }
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten().distinctBy { it.type + ":" + it.id }.take(60)
    }

    suspend fun details(item: Movie): Movie = withContext(Dispatchers.IO) {
        val ordered = addons.sortedByDescending { it.url == item.source }
        ordered.firstNotNullOfOrNull { addon ->
            if (!addon.supports("meta", item.type, item.id)) return@firstNotNullOfOrNull null
            runCatching {
                readJson(endpoint(addon, "meta", item.type, item.id))
                    .optJSONObject("meta")?.toMovie(addon.url)?.takeIf { it.id.isNotBlank() }
            }.getOrNull()
        } ?: item
    }

    suspend fun streams(item: Movie, episode: Episode?): List<Stream> = supervisorScope {
        val videoId = episode?.id ?: item.id
        addons.filter { it.supports("stream", item.type, videoId) }.map { addon ->
            async(Dispatchers.IO) {
                runCatching {
                    val arr = readJson(endpoint(addon, "stream", item.type, videoId)).optJSONArray("streams") ?: JSONArray()
                    (0 until minOf(50, arr.length())).mapNotNull { i ->
                        val s = arr.optJSONObject(i) ?: return@mapNotNull null
                        val hints = s.optJSONObject("behaviorHints")
                        val props = hints?.optJSONObject("proxyHeaders")?.optJSONObject("request")
                        val headers = if (props == null) emptyMap() else props.keys().asSequence().associateWith { props.optString(it) }
                        val subs = s.optJSONArray("subtitles")
                        val subtitles = if (subs == null) emptyList() else (0 until subs.length()).mapNotNull { j ->
                            val track = subs.optJSONObject(j) ?: return@mapNotNull null
                            val link = track.str("url")
                            if (!link.startsWith("http")) return@mapNotNull null
                            SubtitleTrack(link, track.str("lang").ifBlank { "en" },
                                if (link.contains(".srt", true)) "application/x-subrip" else "text/vtt")
                        }
                        Stream(s.str("name").ifBlank { addon.name }, s.str("title").ifBlank { "Available stream" },
                            s.str("url"), s.str("externalUrl"), s.str("infoHash"), s.str("ytId"),
                            addon.name, headers, subtitles)
                            .takeIf { it.direct || it.external.isNotBlank() || it.infoHash.isNotBlank() || it.ytId.isNotBlank() }
                    }
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten().distinctBy { "${it.url}|${it.infoHash}|${it.external}|${it.ytId}" }
    }

    fun saved(): List<Movie> = runCatching {
        val arr = JSONArray(prefs.getString("saved", "[]"))
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.toMovie(CINEMETA) }
    }.getOrDefault(emptyList())

    fun toggleSaved(movie: Movie): List<Movie> {
        val list = saved().toMutableList()
        if (list.any { it.id == movie.id && it.type == movie.type })
            list.removeAll { it.id == movie.id && it.type == movie.type }
        else list.add(0, movie)
        prefs.edit().putString("saved", JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()).apply()
        return list
    }
}

data class CinemaState(
    val tab: String = "Home", val rows: List<MediaRow> = emptyList(), val loading: Boolean = true,
    val addons: List<Addon> = emptyList(), val saved: List<Movie> = emptyList(),
    val details: Movie? = null, val detailLoading: Boolean = false,
    val season: Int = 1, val episode: Episode? = null,
    val streams: List<Stream> = emptyList(), val streamsLoading: Boolean = false,
    val player: Playing? = null, val query: String = "",
    val searchResults: List<Movie> = emptyList(), val searching: Boolean = false,
    val changingAddon: Boolean = false, val message: String = "",
)

class CinemaModel(application: Application) : AndroidViewModel(application) {
    private val client = StremioClient(application)
    var state by mutableStateOf(CinemaState(saved = client.saved()))
        private set

    init {
        viewModelScope.launch {
            state = state.copy(addons = client.loadAddons())
            refresh()
        }
    }
    fun selectTab(tab: String) {
        state = state.copy(tab = tab, message = "")
        if (tab == "Discover" && state.rows.isEmpty()) refresh()
    }
    fun refresh() {
        viewModelScope.launch {
            state = state.copy(loading = true)
            val rows = client.homeRows()
            state = state.copy(rows = rows, loading = false)
        }
    }
    fun open(item: Movie) {
        state = state.copy(details = item, detailLoading = true, streams = emptyList(), episode = null)
        viewModelScope.launch {
            val full = client.details(item)
            val firstSeason = full.episodes.firstOrNull { it.season > 0 }?.season ?: 1
            state = state.copy(details = full, detailLoading = false, season = firstSeason)
            loadStreams(null)
        }
    }
    fun closeDetails() { state = state.copy(details = null, streams = emptyList(), episode = null, message = "") }
    fun selectSeason(season: Int) { state = state.copy(season = season, episode = null, streams = emptyList()) }
    fun selectEpisode(episode: Episode) { state = state.copy(episode = episode); loadStreams(episode) }
    fun loadStreams(episode: Episode?) {
        val item = state.details ?: return
        if (item.type == "series" && episode == null) {
            state = state.copy(streams = emptyList(), streamsLoading = false); return
        }
        state = state.copy(episode = episode, streams = emptyList(), streamsLoading = true)
        viewModelScope.launch {
            val streams = client.streams(item, episode)
            if (state.details?.id == item.id && state.episode?.id == episode?.id)
                state = state.copy(streams = streams, streamsLoading = false)
        }
    }
    fun play(stream: Stream) {
        val movie = state.details ?: return
        state = state.copy(player = Playing(movie, state.episode, stream))
    }
    fun closePlayer() { state = state.copy(player = null) }
    fun toggleSaved(item: Movie) { state = state.copy(saved = client.toggleSaved(item)) }
    fun updateQuery(value: String) {
        state = state.copy(query = value)
        if (value.isBlank()) state = state.copy(searchResults = emptyList(), searching = false)
    }
    fun search(value: String) {
        if (value.isBlank()) return
        viewModelScope.launch {
            state = state.copy(searching = true)
            val result = client.search(value)
            if (state.query == value) state = state.copy(searchResults = result, searching = false)
        }
    }
    fun install(url: String) {
        viewModelScope.launch {
            state = state.copy(changingAddon = true, message = "")
            runCatching { client.install(url) }.onSuccess {
                state = state.copy(addons = it, changingAddon = false, message = "Add-on installed.")
                refresh()
            }.onFailure {
                state = state.copy(changingAddon = false, message = it.message ?: "Could not install add-on.")
            }
        }
    }
    fun remove(id: String) {
        viewModelScope.launch {
            state = state.copy(changingAddon = true)
            runCatching { client.uninstall(id) }.onSuccess {
                state = state.copy(addons = it, changingAddon = false, message = "Add-on removed.")
                refresh()
            }.onFailure {
                state = state.copy(changingAddon = false, message = it.message ?: "Unable to remove add-on.")
            }
        }
    }
}
