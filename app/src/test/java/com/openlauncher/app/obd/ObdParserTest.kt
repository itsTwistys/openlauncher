package com.openlauncher.app.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdParserTest {
    @Test fun parsesRpm() {
        assertEquals(1726, ObdParser.parse(ObdPid.RPM, "41 0C 1A F8 \r\r"))
        assertEquals(1000, ObdParser.parse(ObdPid.RPM, "SEARCHING...\r41 0C 0F A0 \r"))
    }

    @Test fun parsesSingleByteValues() {
        assertEquals(60, ObdParser.parse(ObdPid.SPEED, "41 0D 3C"))
        assertEquals(83, ObdParser.parse(ObdPid.COOLANT, "41 05 7B"))
        assertEquals(50, ObdParser.parse(ObdPid.THROTTLE, "41 11 80"))
        assertEquals(100, ObdParser.parse(ObdPid.FUEL, "41 2F FF"))
        assertEquals(-40, ObdParser.parse(ObdPid.INTAKE, "41 0F 00"))
    }

    @Test fun toleratesSpacingEchoAndTwoEcus() {
        assertEquals(60, ObdParser.parse(ObdPid.SPEED, "010D\r410D3C\r410D3D\r"))
        assertNull(ObdParser.parse(ObdPid.SPEED, "41\n0D\n")) // a header split across lines is not an answer
    }

    @Test fun rejectsNoDataAndWrongPids() {
        assertNull(ObdParser.parse(ObdPid.RPM, "NO DATA"))
        assertNull(ObdParser.parse(ObdPid.RPM, "UNABLE TO CONNECT"))
        assertNull(ObdParser.parse(ObdPid.RPM, ""))
        assertNull(ObdParser.parse(ObdPid.SPEED, "41 0C 1A F8"))
        assertNull(ObdParser.parse(ObdPid.RPM, "41 0C 1A")) // one byte short
        assertNull(ObdParser.parse(ObdPid.SPEED, "41 0D")) // header only
    }

    @Test fun parsesBatteryVoltage() {
        assertEquals(12.6, ObdParser.parseVoltage("12.6V")!!, 0.001)
        assertEquals(14.2, ObdParser.parseVoltage("\r14.2V \r")!!, 0.001)
        assertNull(ObdParser.parseVoltage("ELM327 v1.5"))
        assertNull(ObdParser.parseVoltage("2.1V")) // implausible for a car
        assertNull(ObdParser.parseVoltage("?"))
    }

    @Test fun buildsAReadingFromReplies() {
        val reading = ObdParser.readingFrom(
            mapOf(ObdPid.RPM to "41 0C 1A F8", ObdPid.COOLANT to "NO DATA", ObdPid.SPEED to "41 0D 3C"), "13.8V")
        assertEquals(1726, reading.rpm)
        assertNull(reading.coolantC)
        assertEquals(60, reading.speedKmh)
        assertEquals(13.8, reading.batteryVolts!!, 0.001)
        assertNull(ObdParser.readingFrom(emptyMap(), null).rpm)
    }
}
