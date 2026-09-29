package com.openlauncher.app.data

import org.junit.Assert.*
import org.junit.Test

class MapDiagnosticsTest {
    @Test fun recoveryAndPageReloadRetainFailureEvidence() {
        var now = 100L
        val diagnostics = MapDiagnostics { now }
        diagnostics.beginPage()
        diagnostics.engine(MapEngine.TIMED_OUT)
        diagnostics.failure(MapFailure.HTTP, MapResource.TILE, 429)
        diagnostics.tiles(true, false, 4, 2, 1)
        val failure = diagnostics.map.value.lastFailure
        now = 200L
        diagnostics.engine(MapEngine.READY)
        diagnostics.mapStatus("Map ready")
        diagnostics.tiles(true, false, 9, 2, 1)
        val recovered = diagnostics.map.value
        assertEquals(failure, recovered.lastFailure)
        assertEquals(200L, recovered.updatedAtMs)
        assertEquals(1, recovered.failures[MapFailure.TIMEOUT])
        assertEquals(1, recovered.failures[MapFailure.HTTP])
        diagnostics.beginPage()
        val reloaded = diagnostics.map.value
        assertEquals(2, reloaded.attempts)
        assertEquals(MapEngine.INITIALIZING, reloaded.engine)
        assertEquals(0, reloaded.tileLoads)
        assertFalse(reloaded.tilesStarted)
        assertEquals(failure, reloaded.lastFailure)
    }
    @Test fun tileSnapshotsDoNotDoubleCountAndDoNotLoseFailuresOnRetry() {
        val d = MapDiagnostics()
        d.tiles(true, true, 3, 2, 1)
        repeat(3) { d.tiles(true, false, 3, 2, 1) }
        d.tiles(true, false, -1, 0, 0)
        assertEquals(3, d.map.value.tileLoads)
        assertEquals(2, d.map.value.tileErrors)
        assertEquals(1, d.map.value.tileTimeouts)
    }
    @Test fun allInitializationFailureKindsAreRetainedAndExported() {
        val d = MapDiagnostics { 123 }
        listOf(MapEngine.CREATE_FAILED, MapEngine.SCRIPT_FAILED, MapEngine.TIMED_OUT,
            MapEngine.EVALUATION_FAILED, MapEngine.RENDERER_GONE).forEach(d::engine)
        d.engine(MapEngine.READY)
        assertEquals(2, d.map.value.failures[MapFailure.INITIALIZATION])
        val export = d.map.value.evidenceLines().joinToString()
        assertTrue(export.contains("READY"))
        assertTrue(export.contains("Map TIMEOUT, count=1"))
        assertTrue(export.contains("Map EVALUATION, count=1"))
        assertTrue(export.contains("Map RENDERER, count=1"))
        assertTrue(export.contains("epochMs=123"))
    }
    @Test fun arbitraryStatusTextCannotLeakIntoDiagnostics() {
        val d = MapDiagnostics()
        d.mapStatus("https://tile.openstreetmap.org/17/123/456.png?lat=25.761&lon=-80.191 private title")
        assertEquals("Map status unavailable", d.map.value.status)
        val text = d.map.value.toString() + d.map.value.evidenceLines().joinToString()
        listOf("https", "123/456", "25.761", "-80.191", "private title").forEach { assertFalse(text.contains(it)) }
    }
}
