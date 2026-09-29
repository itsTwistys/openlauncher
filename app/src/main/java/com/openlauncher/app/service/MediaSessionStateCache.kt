package com.openlauncher.app.service

internal data class MediaSignature(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val artUri: String?,
    val hasArtwork: Boolean
)

/** Keeps the last state object when a session reports the same visible media information. */
internal class MediaSessionStateCache<K, V>(private val signature: (V) -> MediaSignature) {
    private val states = mutableMapOf<K, Pair<MediaSignature, V>>()

    fun retain(key: K, candidate: V): V {
        val current = signature(candidate)
        val previous = states[key]
        if (previous != null && previous.first == current) return previous.second
        states[key] = current to candidate
        return candidate
    }

    fun retainOnly(keys: Set<K>) { states.keys.retainAll(keys) }
    fun clear() { states.clear() }
}
