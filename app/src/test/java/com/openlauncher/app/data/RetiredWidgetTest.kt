package com.openlauncher.app.data

import org.junit.Assert.*
import org.junit.Test

class RetiredWidgetTest {
    @Test fun removesVideoFromPreferencesAndProfilesWithoutMovingMap() {
        val map = WidgetConfig("MAP", gridX = 1, gridY = 0, spanX = 2, spanY = 2)
        val video = WidgetConfig("YOUTUBE", gridX = 0, gridY = 0)
        val settings = AppSettings(showMap = true, widgetLayout = listOf(video, map),
            layoutProfiles = listOf(LayoutProfile("Parked", listOf(video, map), listOf("YOUTUBE", "MAP"))))
        val migrated = settings.withoutRetiredWidgets()
        assertEquals(listOf(map), migrated.widgetLayout)
        assertEquals(listOf(map), migrated.layoutProfiles.single().layout)
        assertEquals(listOf("MAP"), migrated.layoutProfiles.single().enabledIds)
        assertTrue(migrated.showMap)
        assertEquals(migrated, migrated.withoutRetiredWidgets())
    }

    @Test fun importsOldVideoBackupAndPreservesMap() {
        val restored = SettingsBackup.decode("""{
          "format":"openlauncher-settings", "version":1,
          "settings":{"showYouTube":true,"youtubeUrl":"https://youtu.be/aqz-KE-bpKQ",
            "showMap":true,"showClock":false,"showWeather":false,"showTelemetry":false,"showNowPlaying":false,
            "widgetLayout":[{"id":"YOUTUBE","gridX":0,"gridY":0,"spanX":1,"spanY":1,"enabled":true},
              {"id":"MAP","gridX":1,"gridY":0,"spanX":2,"spanY":2,"enabled":true}],
            "layoutProfiles":[{"name":"Parked","enabledIds":["YOUTUBE","MAP"],"layout":[
              {"id":"YOUTUBE","gridX":0,"gridY":0,"spanX":1,"spanY":1,"enabled":true},
              {"id":"MAP","gridX":1,"gridY":0,"spanX":2,"spanY":2,"enabled":true}]}]}}
        """)
        assertEquals(setOf("MAP"), restored.activeWidgetIds())
        assertEquals(listOf("MAP"), restored.widgetLayout.map { it.id })
        assertEquals(listOf("MAP"), restored.layoutProfiles.single().enabledIds)
        assertFalse(SettingsBackup.encode(restored).contains("YOUTUBE"))
    }
}
