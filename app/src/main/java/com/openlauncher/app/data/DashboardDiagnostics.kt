package com.openlauncher.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class MapEngine { NOT_OPENED, INITIALIZING, READY, SCRIPT_FAILED, TIMED_OUT, CREATE_FAILED, EVALUATION_FAILED, RENDERER_GONE }
enum class MapResource { PAGE, LOCAL_ASSET, TILE, OTHER }
enum class MapFailure { INITIALIZATION, TIMEOUT, EVALUATION, CONSOLE, HTTP, NETWORK, SSL, RENDERER }

data class MapFailureEvidence(val kind: MapFailure, val resource: MapResource?, val code: Int?, val atMs: Long)
data class MapHealth(
    val status: String = "Map has not opened in this session", val updatedAtMs: Long = 0,
    val engine: MapEngine = MapEngine.NOT_OPENED, val attempts: Int = 0,
    val failures: Map<MapFailure, Int> = emptyMap(),
    val lastByKind: Map<MapFailure, MapFailureEvidence> = emptyMap(), val lastFailure: MapFailureEvidence? = null,
    val consoleWarnings: Int = 0,
    // Cumulative page counters survive tile retries. A new WebView starts a new page.
    val tileLoads: Int = 0, val tileErrors: Int = 0, val tileTimeouts: Int = 0,
    val tilesStarted: Boolean = false, val tilesLoading: Boolean = false
)

/** In-memory, process-session evidence. Never accepts URLs, error/console text or location data. */
open class MapDiagnostics(private val clock: () -> Long = System::currentTimeMillis) {
    private val _map = MutableStateFlow(MapHealth())
    val map: StateFlow<MapHealth> = _map
    fun mapStatus(status: String) { _map.update { it.copy(status = status.takeIf { it in SAFE_MAP_STATUSES } ?: "Map status unavailable", updatedAtMs = clock()) } }
    fun beginPage() { _map.update { it.copy(engine = MapEngine.INITIALIZING, attempts = it.attempts + 1,
        tileLoads = 0, tileErrors = 0, tileTimeouts = 0, tilesStarted = false, tilesLoading = false) } }
    fun engine(state: MapEngine) {
        _map.update { it.copy(engine = state) }
        when (state) {
            MapEngine.CREATE_FAILED, MapEngine.SCRIPT_FAILED -> failure(MapFailure.INITIALIZATION)
            MapEngine.TIMED_OUT -> failure(MapFailure.TIMEOUT)
            MapEngine.EVALUATION_FAILED -> failure(MapFailure.EVALUATION)
            MapEngine.RENDERER_GONE -> failure(MapFailure.RENDERER)
            else -> Unit
        }
    }
    fun failure(kind: MapFailure, resource: MapResource? = null, code: Int? = null) {
        val at = clock()
        val evidence = MapFailureEvidence(kind, resource, code, at)
        _map.update { it.copy(failures = it.failures + (kind to ((it.failures[kind] ?: 0) + 1)),
            lastByKind = it.lastByKind + (kind to evidence), lastFailure = evidence) }
    }
    fun consoleWarning() { _map.update { it.copy(consoleWarnings = it.consoleWarnings + 1) } }
    fun tiles(started: Boolean, loading: Boolean, loaded: Int, errors: Int, timeouts: Int) {
        _map.update { it.copy(tilesStarted = started, tilesLoading = loading,
            tileLoads = loaded.coerceAtLeast(it.tileLoads), tileErrors = errors.coerceAtLeast(it.tileErrors),
            tileTimeouts = timeouts.coerceAtLeast(it.tileTimeouts)) }
    }
}
object DashboardDiagnostics : MapDiagnostics()

/** One source for the existing UI and text export; only enums, counts, codes and timestamps. */
fun MapHealth.evidenceLines(): List<Pair<String, String>> = listOf(
    "Map engine" to "$engine · $attempts initialization attempts (app session)",
    "Map console warnings" to consoleWarnings.toString(),
    "Map tiles (current page)" to "started=$tilesStarted, loading=$tilesLoading, loaded=$tileLoads, errors=$tileErrors, timeouts=$tileTimeouts",
    "Map last failure" to (lastFailure?.let { "${it.kind} · epochMs=${it.atMs}" } ?: "None")
) + MapFailure.entries.map { kind ->
    "Map $kind" to ("count=${failures[kind] ?: 0}" + (lastByKind[kind]?.let {
        " · ${it.resource ?: "ENGINE"} · code=${it.code ?: "none"} · epochMs=${it.atMs}"
    } ?: ""))
}

// Only locally defined UI messages may enter the export, never arbitrary WebView text.
private val SAFE_MAP_STATUSES = setOf(
    "Loading map engine…", "Offline · map will retry when connected", "Waiting for GPS · map is ready",
    "Loading map tiles…", "Some map tiles unavailable · check internet", "Waiting for map tiles…",
    "Heading unavailable · north up", "Manual zoom · Recenter resumes auto zoom", "Map ready",
    "Map engine unavailable. Check Android System WebView or tap Reload.",
    "Map script did not initialize. Retrying…", "Map script evaluation failed. Retrying…",
    "Map page failed to load. Retrying…", "Map renderer stopped. Recovering…",
    "Map WebView could not initialize. Retrying…"
)
