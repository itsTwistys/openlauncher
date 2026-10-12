package com.openlauncher.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** A map tile address in the standard web-mercator scheme. */
data class TileAddress(val z: Int, val x: Int, val y: Int)

fun tileX(longitude: Double, zoom: Int): Int {
    val n = 1 shl zoom
    return floor((longitude + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
}

fun tileY(latitude: Double, zoom: Int): Int {
    val n = 1 shl zoom
    val radians = Math.toRadians(latitude.coerceIn(-85.0511, 85.0511))
    return floor((1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
}

/** Every tile within roughly [radiusKm] of the point, for zooms [minZoom]..[maxZoom]. */
fun tilesAround(latitude: Double, longitude: Double, radiusKm: Double, minZoom: Int = 4, maxZoom: Int = 14): List<TileAddress> {
    require(radiusKm > 0 && minZoom in 0..maxZoom && maxZoom <= 18)
    val deltaLat = radiusKm / 111.32
    val deltaLon = radiusKm / (111.32 * cos(Math.toRadians(latitude.coerceIn(-85.0, 85.0))).coerceAtLeast(0.05))
    val south = (latitude - deltaLat).coerceAtLeast(-85.0511)
    val north = (latitude + deltaLat).coerceAtMost(85.0511)
    val west = (longitude - deltaLon).coerceAtLeast(-180.0)
    val east = (longitude + deltaLon).coerceAtMost(180.0)
    return (minZoom..maxZoom).flatMap { z ->
        val x0 = tileX(west, z); val x1 = tileX(east, z)
        val y0 = tileY(north, z); val y1 = tileY(south, z)
        (x0..x1).flatMap { x -> (y0..y1).map { y -> TileAddress(z, x, y) } }
    }
}

/**
 * Downloads the map around a point into [MapTileCache]. It requests exactly the style, fonts, sprites and tiles the map
 * itself would request for that area, politely and a few at a time. The area's tiles reveal the area to the provider,
 * exactly as viewing the map does.
 */
object OfflineMaps {
    data class Progress(val running: Boolean = false, val done: Int = 0, val total: Int = 0, val message: String = "")

    const val MAX_TILES = 6_000
    private const val STYLE_BASE = "https://${MapTileCache.HOST}/styles/"
    private const val TILEJSON_FALLBACK = "https://${MapTileCache.HOST}/planet"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress
    private var job: Job? = null

    /** Rough tile count, so the owner can see the size of a choice before starting. */
    fun estimateTiles(latitude: Double, longitude: Double, radiusKm: Int): Int =
        tilesAround(latitude, longitude, radiusKm.toDouble()).size

    @Synchronized
    fun start(context: Context, latitude: Double, longitude: Double, radiusKm: Int) {
        if (job?.isActive == true) return
        val appContext = context.applicationContext
        _progress.value = Progress(running = true, message = "Preparing…")
        job = scope.launch { download(appContext, latitude, longitude, radiusKm) }
    }

    @Synchronized
    fun cancel() {
        job?.cancel()
        job = null
        _progress.value = Progress(message = "Download stopped")
    }

    private suspend fun download(context: Context, latitude: Double, longitude: Double, radiusKm: Int) {
        try {
            val tiles = tilesAround(latitude, longitude, radiusKm.toDouble())
            if (tiles.size > MAX_TILES) { _progress.value = Progress(message = "That area is too large. Choose a smaller radius."); return }
            val urls = LinkedHashSet<String>()
            for (name in listOf("liberty", "dark")) collectStyleUrls(context, name, tiles, urls)
            if (urls.isEmpty()) { _progress.value = Progress(message = "The map provider could not be reached."); return }
            val total = urls.size
            val done = AtomicInteger()
            val failures = AtomicInteger()
            val consecutiveFailures = AtomicInteger()
            val aborted = java.util.concurrent.atomic.AtomicBoolean(false)
            _progress.value = Progress(running = true, done = 0, total = total, message = "Downloading $total files…")
            val queue = Channel<String>(Channel.UNLIMITED)
            urls.forEach { queue.trySend(it) }
            queue.close()
            coroutineScope {
                repeat(4) {
                    launch {
                        for (url in queue) {
                            ensureActive()
                            if (aborted.get()) break
                            if (MapTileCache.read(context, url) == null) {
                                when (val result = MapTileCache.fetch(url)) {
                                    is MapTileCache.Fetch.Ok -> { MapTileCache.write(context, url, result.entry); consecutiveFailures.set(0) }
                                    MapTileCache.Fetch.Empty -> consecutiveFailures.set(0)
                                    MapTileCache.Fetch.Failed -> { failures.incrementAndGet(); consecutiveFailures.incrementAndGet() }
                                }
                            }
                            _progress.value = Progress(running = true, done = done.incrementAndGet(), total = total, message = "Downloading…")
                            if (consecutiveFailures.get() >= 25) aborted.set(true)
                        }
                    }
                }
            }
            MapTileCache.trim(context)
            _progress.value = Progress(
                done = done.get(), total = total,
                message = when {
                    consecutiveFailures.get() >= 25 -> "Stopped: the connection kept failing. Saved ${done.get()} of $total files."
                    failures.get() > 0 -> "Finished with ${failures.get()} files missing. Run it again to retry them."
                    else -> "Offline map saved."
                }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _progress.value = Progress(message = "Download failed: ${e.message ?: "unknown error"}")
        }
    }

    /** Add every URL the style needs for [tiles]: style, tile index, sprites, a Latin glyph range and the tiles. */
    private fun collectStyleUrls(context: Context, name: String, tiles: List<TileAddress>, into: MutableSet<String>) {
        val styleUrl = STYLE_BASE + name
        val style = MapTileCache.load(context, styleUrl)?.let { runCatching { JSONObject(String(it.body, Charsets.UTF_8)) }.getOrNull() } ?: return
        into += styleUrl
        val sprite = style.optString("sprite")
        if (sprite.isNotBlank()) listOf(".json", ".png", "@2x.json", "@2x.png").forEach { into += sprite + it }
        val glyphs = style.optString("glyphs")
        if (glyphs.contains("{fontstack}") && glyphs.contains("{range}")) {
            for (stack in fontStacks(style)) {
                val encoded = URLEncoder.encode(stack, "UTF-8").replace("+", "%20")
                for (range in listOf("0-255", "256-511")) into += glyphs.replace("{fontstack}", encoded).replace("{range}", range)
            }
        }
        val sources = style.optJSONObject("sources")
        val vectorSource = sources?.let { all ->
            all.keys().asSequence().mapNotNull { all.optJSONObject(it) }.firstOrNull { it.optString("type") == "vector" }
        }
        val tileJsonUrl = vectorSource?.optString("url").orEmpty().ifBlank { TILEJSON_FALLBACK }
        val tileJson = MapTileCache.load(context, tileJsonUrl)?.let { runCatching { JSONObject(String(it.body, Charsets.UTF_8)) }.getOrNull() } ?: return
        into += tileJsonUrl
        val template = tileJson.optJSONArray("tiles")?.optString(0).orEmpty()
        if (!template.contains("{z}") || !template.contains("{x}") || !template.contains("{y}")) return
        val maxZoom = tileJson.optInt("maxzoom", 14)
        tiles.filter { it.z <= maxZoom }.forEach {
            into += template.replace("{z}", it.z.toString()).replace("{x}", it.x.toString()).replace("{y}", it.y.toString())
        }
    }

    private fun fontStacks(style: JSONObject): Set<String> {
        val stacks = LinkedHashSet<String>()
        val layers = style.optJSONArray("layers") ?: return stacks
        for (i in 0 until layers.length()) {
            val fonts = layers.optJSONObject(i)?.optJSONObject("layout")?.optJSONArray("text-font") ?: continue
            val names = (0 until fonts.length()).mapNotNull { fonts.optString(it).takeIf { name -> name.isNotBlank() } }
            if (names.isNotEmpty()) stacks += names.joinToString(",")
        }
        return stacks
    }
}
