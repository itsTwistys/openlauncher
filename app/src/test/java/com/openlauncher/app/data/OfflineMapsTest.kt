package com.openlauncher.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapsTest {
    @Test fun tileColumnsFollowLongitude() {
        assertEquals(0, tileX(-180.0, 1))
        assertEquals(1, tileX(0.0, 1))
        assertEquals(1, tileX(179.999, 1))
        assertEquals(511, tileX(-0.1278, 10)) // London
        assertEquals(0, tileX(-200.0, 3)) // clamped
    }

    @Test fun tileRowsFollowLatitude() {
        assertEquals(1, tileY(0.0, 1))
        assertEquals(0, tileY(85.0, 1))
        assertEquals(1, tileY(-85.0, 1))
        assertTrue(tileY(51.5074, 10) in 339..341) // London
        assertEquals(0, tileY(89.9, 4)) // clamped to the mercator limit
    }

    @Test fun areaContainsTheCenterTileAtEveryZoom() {
        val lat = 25.7; val lon = -80.2
        val tiles = tilesAround(lat, lon, 10.0, minZoom = 6, maxZoom = 14)
        for (z in 6..14) assertTrue(TileAddress(z, tileX(lon, z), tileY(lat, z)) in tiles)
        assertEquals(tiles.size, tiles.toSet().size)
        assertTrue(tiles.all { it.z in 6..14 })
    }

    @Test fun deeperZoomsNeedMoreTiles() {
        val tiles = tilesAround(25.7, -80.2, 25.0, minZoom = 4, maxZoom = 14)
        val byZoom = tiles.groupingBy { it.z }.eachCount()
        assertTrue(byZoom.getValue(14) > byZoom.getValue(10))
        assertTrue(byZoom.getValue(10) >= byZoom.getValue(6))
        assertTrue(tiles.size < OfflineMaps.MAX_TILES)
    }

    @Test fun largestChoiceStaysWithinTheTileLimit() {
        assertTrue(tilesAround(25.7, -80.2, 50.0).size <= OfflineMaps.MAX_TILES)
        // Tiles shrink toward the poles, so the same radius needs more of them there; small radii must still fit.
        assertTrue(tilesAround(64.1, -21.9, 10.0).size <= OfflineMaps.MAX_TILES)
        assertTrue(tilesAround(64.1, -21.9, 50.0).size > tilesAround(25.7, -80.2, 50.0).size)
    }

    @Test fun onlyTheMapProviderHostIsCacheable() {
        assertTrue(MapTileCache.isCacheableUrl("https://tiles.openfreemap.org/planet/20250101/14/4349/7014.pbf"))
        assertFalse(MapTileCache.isCacheableUrl("http://tiles.openfreemap.org/planet"))
        assertFalse(MapTileCache.isCacheableUrl("https://example.com/planet"))
        assertFalse(MapTileCache.isCacheableUrl("https://tiles.openfreemap.org.example.com/planet"))
        assertFalse(MapTileCache.isCacheableUrl("https://tiles.openfreemap.org:8443/planet"))
        assertFalse(MapTileCache.isCacheableUrl("not a url"))
    }

    @Test fun stylesAndTileIndexAreRefreshedButTilesAreNot() {
        assertTrue(MapTileCache.isMutable("https://tiles.openfreemap.org/styles/liberty"))
        assertTrue(MapTileCache.isMutable("https://tiles.openfreemap.org/planet"))
        assertFalse(MapTileCache.isMutable("https://tiles.openfreemap.org/planet/20250101/14/4349/7014.pbf"))
        assertFalse(MapTileCache.isMutable("https://tiles.openfreemap.org/fonts/Noto%20Sans%20Bold/0-255.pbf"))
    }

    @Test fun cacheKeysAreStableAndDistinct() {
        val a = MapTileCache.key("https://tiles.openfreemap.org/planet/1/2/3.pbf")
        assertEquals(a, MapTileCache.key("https://tiles.openfreemap.org/planet/1/2/3.pbf"))
        assertNotEquals(a, MapTileCache.key("https://tiles.openfreemap.org/planet/1/2/4.pbf"))
        assertEquals(40, a.length)
    }
}
