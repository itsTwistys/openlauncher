package com.openlauncher.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationSettingsTest {
    @Test fun everythingAutomaticIsOffByDefault() {
        val s = AppSettings()
        assertFalse(s.autoDrivingProfiles)
        assertFalse(s.autoDayNightProfiles)
        assertFalse(s.recordTripsInBackground)
        assertFalse(s.steeringKeysEnabled)
        assertTrue(s.eventTriggers.isEmpty())
        assertTrue(s.steeringKeys.isEmpty())
        assertEquals("", s.cameraPackage)
        assertEquals("", s.obdDeviceAddress)
        assertFalse(s.showObd || s.showAgenda || s.showPhone)
        assertEquals(setOf("MAP", "NOW_PLAYING", "CLOCK"), s.activeWidgetIds())
    }

    @Test fun automationSurvivesBackupRoundTrip() {
        val original = AppSettings(
            autoDrivingProfiles = true, parkedAfterMinutes = 10, recordTripsInBackground = true,
            cameraPackage = "com.vendor.camera", obdDeviceAddress = "AA:BB:CC:DD:EE:FF", obdDeviceName = "OBDII",
            eventTriggers = listOf(EventTrigger("com.vendor.REVERSE_ON", LauncherCommand.OPEN_CAMERA.name)),
            steeringKeysEnabled = true, steeringKeys = listOf(KeyBinding(87, LauncherCommand.MEDIA_NEXT.name))
        )
        val decoded = SettingsBackup.decode(SettingsBackup.encode(original))
        assertTrue(decoded.autoDrivingProfiles)
        assertEquals(10, decoded.parkedAfterMinutes)
        assertTrue(decoded.recordTripsInBackground)
        assertEquals("com.vendor.camera", decoded.cameraPackage)
        assertEquals("AA:BB:CC:DD:EE:FF", decoded.obdDeviceAddress)
        assertEquals(original.eventTriggers, decoded.eventTriggers)
        assertEquals(original.steeringKeys, decoded.steeringKeys)
        assertTrue(decoded.steeringKeysEnabled)
    }

    @Test fun olderBackupsKeepNewFeaturesOff() {
        val decoded = SettingsBackup.decode("""{"format":"openlauncher-settings","version":1,"settings":{}}""")
        assertFalse(decoded.autoDrivingProfiles)
        assertEquals(5, decoded.parkedAfterMinutes)
        assertTrue(decoded.eventTriggers.isEmpty())
    }

    @Test fun backupsRejectUnsafeAutomation() {
        fun decode(s: AppSettings) = SettingsBackup.decode(SettingsBackup.encode(s))
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(eventTriggers = listOf(EventTrigger("not an action")))) }
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(eventTriggers = listOf(EventTrigger("com.vendor.X", "FORMAT_DISK")))) }
        assertThrows(IllegalArgumentException::class.java) {
            decode(AppSettings(eventTriggers = (1..MAX_EVENT_TRIGGERS + 1).map { EventTrigger("com.vendor.A$it") }))
        }
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(steeringKeys = listOf(KeyBinding(0, "MEDIA_NEXT")))) }
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(steeringKeys = listOf(KeyBinding(24, "DO_SOMETHING")))) }
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(parkedAfterMinutes = 0)) }
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(obdDeviceAddress = "not-a-mac")) }
        assertThrows(IllegalArgumentException::class.java) { decode(AppSettings(cameraPackage = "bad package!")) }
    }

    @Test fun broadcastActionsMustLookLikeDottedNames() {
        assertTrue(isValidEventAction("com.vendor.REVERSE_ON"))
        assertTrue(isValidEventAction("android.intent.action.BOOT_COMPLETED"))
        assertTrue(isValidEventAction("com.szchoiceway.eventcenter.EventUtils.ACTION_MCU_CMD_EVENT"))
        assertFalse(isValidEventAction(""))
        assertFalse(isValidEventAction("REVERSE"))
        assertFalse(isValidEventAction("com.vendor. spaced"))
        assertFalse(isValidEventAction("com..vendor"))
        assertFalse(isValidEventAction("a.".repeat(150) + "b"))
    }

    @Test fun bluetoothAddressesAreValidated() {
        assertTrue(isValidBluetoothAddress(""))
        assertTrue(isValidBluetoothAddress("00:1D:A5:68:98:8B"))
        assertTrue(isValidBluetoothAddress("aa:bb:cc:dd:ee:ff"))
        assertFalse(isValidBluetoothAddress("00:1D:A5:68:98"))
        assertFalse(isValidBluetoothAddress("00-1D-A5-68-98-8B"))
        assertFalse(isValidBluetoothAddress("ZZ:1D:A5:68:98:8B"))
    }

    @Test fun newWidgetsFitTheDashboardAndAreVisibilityToggled() {
        listOf("OBD" to { s: AppSettings -> s.showObd }, "AGENDA" to { s: AppSettings -> s.showAgenda }, "PHONE" to { s: AppSettings -> s.showPhone })
            .forEach { (id, shown) ->
                val withWidget = AppSettings().withAddedWidget(id)
                assertTrue("$id should be shown", shown(withWidget))
                assertTrue(validWidgetLayout(withWidget.widgetLayout.filter { it.enabled && it.id in withWidget.activeWidgetIds() }))
                assertNotNull(withWidget.widgetLayout.firstOrNull { it.id == id })
                val decoded = SettingsBackup.decode(SettingsBackup.encode(withWidget))
                assertTrue(shown(decoded))
                assertFalse(shown(withWidget.withWidgetVisibility(withWidget.activeWidgetIds() - id)))
            }
    }

    @Test fun commandsRoundTripByName() {
        LauncherCommand.entries.forEach { assertEquals(it, LauncherCommand.from(it.name)) }
        assertEquals(null, LauncherCommand.from("nope"))
    }
}
