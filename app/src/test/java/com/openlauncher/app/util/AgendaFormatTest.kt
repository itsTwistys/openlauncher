package com.openlauncher.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class AgendaFormatTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        GregorianCalendar(utc).apply { clear(); set(2026, 9, day, hour, minute, 0) }.timeInMillis // October 2026
    private val now = at(12, 10) // Monday 12 October, 10:00
    private fun event(start: Long, end: Long = start + 3_600_000, allDay: Boolean = false) = AgendaEvent("Title", start, end, allDay)
    private fun label(e: AgendaEvent, twelve: Boolean = false) = agendaTimeLabel(e, now, twelve, utc, Locale.US)

    @Test fun laterTodayShowsTheClockTime() {
        assertEquals("14:30", label(event(at(12, 14, 30))))
        assertTrue(label(event(at(12, 14, 30)), twelve = true).let { it.startsWith("2:30") && it.endsWith("PM") })
    }

    @Test fun ongoingAndAllDayEvents() {
        assertEquals("Now", label(event(at(12, 9), at(12, 11))))
        assertEquals("All day", label(event(at(12, 0), at(13, 0), allDay = true)))
    }

    @Test fun otherDays() {
        assertEquals("Tomorrow 09:00", label(event(at(13, 9))))
        assertEquals("Thu 09:00", label(event(at(15, 9))))
        assertEquals("09:00", label(event(at(25, 9))))
    }

    @Test fun midnightBoundaryCountsAsTheNextDay() {
        assertEquals("Tomorrow 00:00", label(event(at(13, 0))))
        assertEquals("23:59", label(event(at(12, 23, 59))))
    }
}
