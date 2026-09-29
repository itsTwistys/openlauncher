package com.openlauncher.app.data

import com.openlauncher.app.util.selectMediaSession
import org.junit.Assert.*
import org.junit.Test

class MediaPreferenceTest {
    data class Player(val pkg: String, val playing: Boolean)
    private fun choose(players: List<Player>, preference: String) =
        selectMediaSession(players, preference, { it.pkg }, { it.playing })

    @Test fun explicitPausedPlayerWinsOverOtherPlayingApp() {
        val selected = Player("com.example.music", false)
        assertEquals(selected, choose(listOf(Player("com.example.video", true), selected), selected.pkg))
    }
    @Test fun disconnectedPreferenceNeverFallsBack() {
        assertNull(choose(listOf(Player("com.example.video", true)), "com.example.music"))
    }
    @Test fun automaticFollowsPlayingSessionAndReconnectRestoresPreference() {
        val music = Player("com.example.music", true)
        val video = Player("com.example.video", false)
        assertEquals(music, choose(listOf(video, music), ""))
        assertNull(choose(listOf(video), music.pkg))
        assertEquals(music, choose(listOf(video, music), music.pkg))
    }
    @Test fun preferencesRoundTripWithoutChangingSavedLayout() {
        val s = AppSettings(preferredMediaPackage = "com.example.music", mapAutoZoom = false, mapHeadingUp = true)
        val restored = SettingsBackup.decode(SettingsBackup.encode(s))
        assertEquals(s.preferredMediaPackage, restored.preferredMediaPackage)
        assertFalse(restored.mapAutoZoom)
        assertTrue(restored.mapHeadingUp)
        assertEquals(s.widgetLayout, restored.widgetLayout)
    }
}
