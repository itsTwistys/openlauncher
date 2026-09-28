package com.openlauncher.app.data

import com.openlauncher.app.util.youtubeVideoId
import org.junit.Assert.*
import org.junit.Test

class EmbeddedMediaTest {
    private val id = "aqz-KE-bpKQ"
    @Test fun acceptsVideoAndLiveStreamLinks() {
        listOf("https://www.youtube.com/watch?v=$id&feature=share", "https://youtu.be/$id?si=abc",
            "https://youtube.com/live/$id", "https://m.youtube.com/shorts/$id", "youtube.com/watch?v=$id")
            .forEach { assertEquals(id, youtubeVideoId(it)) }
    }
    @Test fun rejectsUntrustedHostsAndSchemes() {
        listOf("https://youtube.com.evil.test/watch?v=$id", "https://youtube.com@evil.test/watch?v=$id",
            "javascript:alert(1)", "file:///etc/passwd", "https://evil.test/$id",
            "https://youtube.com:8080/watch?v=$id", "https://user@youtube.com/watch?v=$id")
            .forEach { assertNull(youtubeVideoId(it)) }
    }
    @Test fun rejectsMalformedIdsAndChannelLinks() {
        listOf("", "https://youtube.com/watch?v=short", "https://youtube.com/@channel/live",
            "https://youtube.com/watch?v=%22%3E%3Cscript%3E", "https://youtu.be/$id/extra")
            .forEach { assertNull(youtubeVideoId(it)) }
    }
    @Test fun youtubeSurvivesBackupAndProfileVisibility() {
        val settings = AppSettings(youtubeUrl = "https://youtube.com/live/$id")
            .withWidgetVisibility(setOf("YOUTUBE"))
        val restored = SettingsBackup.decode(SettingsBackup.encode(settings))
        assertEquals(setOf("YOUTUBE"), restored.activeWidgetIds())
        assertEquals(settings.youtubeUrl, restored.youtubeUrl)
        assertFalse(restored.withWidgetVisibility(emptySet()).showYouTube)
    }
    @Test(expected = IllegalArgumentException::class)
    fun backupRejectsArbitraryPlayerUrl() {
        SettingsBackup.decode(SettingsBackup.encode(AppSettings(youtubeUrl = "https://evil.test/")))
    }
}
