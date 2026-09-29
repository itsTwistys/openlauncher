package com.openlauncher.app.service

import org.junit.Assert.*
import org.junit.Test

class MediaSessionStateCacheTest {
    private data class State(val signature: MediaSignature, val artwork: Any? = null)
    private val cache = MediaSessionStateCache<String, State> { it.signature }
    private fun state(
        title: String = "Song", artist: String = "Artist", playing: Boolean = true,
        uri: String? = null, artwork: Any? = null
    ) = State(MediaSignature(title, artist, playing, uri, artwork != null), artwork)

    @Test fun repeatedNotificationRetainsOriginalStateEvenWithFreshArtwork() {
        val first = cache.retain("music", state(artwork = Any()))
        assertSame(first, cache.retain("music", state(artwork = Any())))
        assertSame(first, cache.retain("music", state(artwork = Any())))
    }

    @Test fun meaningfulChangesReplaceOnlyAffectedSession() {
        val music = cache.retain("music", state())
        val radio = cache.retain("radio", state(title = "Station"))
        assertNotSame(music, cache.retain("music", state(title = "Next")))
        assertNotSame(radio, cache.retain("radio", state(title = "Station", artist = "Host")))
        assertNotSame(radio, cache.retain("other", state(title = "Station")))

        var previous = cache.retain("music", state())
        for (changed in listOf(
            state(playing = false), state(uri = "content://art/1"),
            state(uri = "content://art/2"), state(artwork = Any()), state()
        )) {
            val next = cache.retain("music", changed)
            assertNotSame(previous, next)
            previous = next
        }
    }

    @Test fun sessionsAreIndependentAndRemovedSessionStartsFresh() {
        val music = cache.retain("music", state())
        val radio = cache.retain("radio", state())
        assertSame(music, cache.retain("music", state()))
        assertSame(radio, cache.retain("radio", state()))
        cache.retainOnly(setOf("radio"))
        assertNotSame(music, cache.retain("music", state()))
        assertSame(radio, cache.retain("radio", state()))
        cache.clear()
        assertNotSame(radio, cache.retain("radio", state()))
    }
}
