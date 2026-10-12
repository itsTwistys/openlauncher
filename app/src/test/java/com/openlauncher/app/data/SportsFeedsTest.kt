package com.openlauncher.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class SportsFeedsTest {
    private val now = SportsFeeds.timestamp("2026-10-12T03:00:00Z")!!
    private fun game(id: String, state: String, date: String = "2026-10-12T01:00Z") = """{"id":"$id","name":"Miami at Buffalo","competitions":[{"id":"$id","date":"$date","status":{"type":{"state":"$state","shortDetail":"${if (state == "post") "Final" else "Q4 2:10"}"}},"competitors":[{"homeAway":"home","team":{"displayName":"Buffalo Bills","abbreviation":"BUF"},"score":"21","winner":true},{"homeAway":"away","team":{"displayName":"Miami Dolphins","abbreviation":"MIA"},"score":"0"}]}]}"""
    @Test fun realZeroScoresStayVisibleWhileScheduledZerosAreSuppressed() {
        val json = """{"events":[${game("live", "in")},${game("final", "post")},${game("next", "pre", "2026-10-13T00:00Z")}]}"""
        val live = SportsFeeds.scoreboard(json, SportsLeague.NFL, SportsView.LIVE, now)
        assertEquals(listOf("live"), live.events.map { it.id })
        assertEquals("MIA", live.events.first().competitors.first().abbreviation)
        assertEquals("0", live.events.first().competitors.first().score)
        val upcoming = SportsFeeds.scoreboard(json, SportsLeague.NFL, SportsView.UPCOMING, now)
        assertEquals("next", upcoming.events.single().id)
        assertTrue(upcoming.events.single().competitors.all { it.score == null })
        assertEquals("final", SportsFeeds.scoreboard(json, SportsLeague.NFL, SportsView.RESULTS, now).events.single().id)
    }
    @Test fun invalidAndEmptyFeedsAreDifferent() {
        assertTrue(SportsFeeds.scoreboard("""{"events":[]}""", SportsLeague.NHL, SportsView.LIVE, now).events.isEmpty())
        assertThrows(Exception::class.java) { SportsFeeds.scoreboard("{}", SportsLeague.NHL, SportsView.LIVE, now) }
        assertThrows(Exception::class.java) { SportsFeeds.scoreboard("""{"events":[{"competitions":[]}]}""", SportsLeague.NBA, SportsView.LIVE, now) }
        assertNull(SportsFeeds.timestamp("2026-02-31T03:00:00Z"))
    }
    @Test fun allFiveLeaguePathsAndLocalDateBoundariesAreExplicit() {
        SportsLeague.entries.forEach { assertTrue(SportsFeeds.url(it, SportsView.LIVE, now, TimeZone.getTimeZone("America/New_York")).contains("${it.path}/scoreboard?dates=20261010-20261011")) }
    }
    @Test fun raceResultsUsePublishedPositionsAndUpcomingNeverInfersLiveFromTime() {
        val json = """{"MRData":{"RaceTable":{"Races":[{"season":"2026","round":"18","raceName":"Test Grand Prix","date":"2026-10-11","time":"12:00:00Z","Results":[{"position":"2","Driver":{"givenName":"Second","familyName":"Driver","code":"TWO"}},{"position":"1","Driver":{"givenName":"First","familyName":"Driver","code":"ONE"}}]}]}}}"""
        val result = SportsFeeds.f1Results(json, now).events.single()
        assertEquals(listOf(1, 2), result.competitors.map { it.position })
        assertTrue(result.competitors.first().winner)
        val schedule = """{"MRData":{"RaceTable":{"Races":[{"season":"2026","round":"19","raceName":"Next Grand Prix","date":"2026-10-25","time":"19:00:00Z","Qualifying":{"date":"2026-10-24","time":"18:00:00Z"}}]}}}"""
        val events = SportsFeeds.f1Schedule(schedule, now).events
        assertEquals(2, events.size)
        assertTrue(events.all { it.state == "pre" && it.competitors.isEmpty() })
    }
    @Test fun sportsSurvivesSettingsBackupAndDashboardPlacement() {
        val settings = AppSettings().copy(sportsLeague = "F1", sportsView = "UPCOMING", sportsLeagues = listOf("F1", "NFL"))
            .withAddedWidget("SPORTS")
        val decoded = SettingsBackup.decode(SettingsBackup.encode(settings))
        assertTrue("SPORTS" in decoded.activeWidgetIds())
        assertEquals("F1", decoded.sportsLeague)
        assertEquals(listOf("F1", "NFL"), decoded.sportsLeagues)
        assertTrue(validWidgetLayout(decoded.widgetLayout.filter { it.id in decoded.activeWidgetIds() }))
    }
}
