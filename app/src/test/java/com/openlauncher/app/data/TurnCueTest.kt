package com.openlauncher.app.data

import com.openlauncher.app.service.*
import org.junit.Assert.*
import org.junit.Test

class TurnCueTest {
    @Test fun parsesOnlyExplicitManeuversAndDoesNotInventArrowsFromRoadNames() {
        assertEquals(TurnManeuver.LEFT, turnCue("300 ft", "Turn left onto Main St").maneuver)
        assertEquals(TurnManeuver.SLIGHT_RIGHT, turnCue("In 1.0 mi", "Keep right toward downtown").maneuver)
        assertEquals(TurnManeuver.UTURN_RIGHT, turnCue("50 m", "Make a right U-turn").maneuver)
        assertEquals(TurnManeuver.UTURN_LEFT, turnCue("50 m", "Make a U-turn left").maneuver)
        assertEquals(TurnManeuver.UNKNOWN, turnCue("50 m", "Make a U-turn").maneuver)
        assertEquals(TurnManeuver.ROUNDABOUT_LEFT, turnCue("300 m", "Turn left at the roundabout").maneuver)
        assertEquals(TurnManeuver.ROUNDABOUT_RIGHT, turnCue("300 m", "Roundabout to the right").maneuver)
        assertEquals(TurnManeuver.UNKNOWN, turnCue("300 m", "Enter the roundabout").maneuver)
        assertEquals(TurnManeuver.UNKNOWN, turnCue("1.0 mi", "SW 57th Ave / Coral Gables Blvd").maneuver)
        assertEquals(TurnManeuver.UNKNOWN, turnCue("300 m", "Left Bank Road").maneuver)
        assertEquals(TurnManeuver.UNKNOWN, turnCue("300 m", "Gire a la izquierda").maneuver)
        assertEquals("1.0 mi", turnCue("In 1.0 mi", "Turn left").distance)
        assertEquals("Turn left", turnCue("300 ft", "Turn left · 15 min · 7.8 mi").instruction)
        assertEquals("Continue straight", turnCue("Continue straight", "1.2 mi").instruction)
        assertEquals("", turnCue("Main Street", "").distance)
    }
}
