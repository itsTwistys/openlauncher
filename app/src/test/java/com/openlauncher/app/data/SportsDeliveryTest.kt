package com.openlauncher.app.data

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.Collections
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SportsDeliveryTest {
    @Test fun partialDayFailureRetainsDeliveredScoresAndCache() = runBlocking {
        val requests = Collections.synchronizedList(mutableListOf<String>())
        val now = SportsFeeds.timestamp("2026-10-12T11:00Z")!!
        val repo = SportsRepository(RuntimeEnvironment.getApplication()) { url ->
            requests.add(url)
            if ("dates=20261012" in url) throw SportsHttpException(503)
            """{"events":[{"id":"score","name":"Away at Home","date":"2026-10-11T23:00Z","status":{"type":{"state":"post","shortDetail":"Final"}},"competitions":[{"id":"score","competitors":[{"team":{"displayName":"Away","abbreviation":"AWY"},"homeAway":"away","score":"0"},{"team":{"displayName":"Home","abbreviation":"HME"},"score":"7"}]}]}]}"""
        }
        val result = repo.fetch(SportsLeague.NFL, SportsView.LIVE, now, TimeZone.getTimeZone("UTC"))
        assertEquals(3, requests.size)
        assertTrue(requests.all { it.substringAfter("dates=").substringBefore('&').matches(Regex("[0-9]{8}")) })
        assertTrue(result.partial)
        assertEquals("0", result.events.single().competitors.first().score)
        assertEquals(result, repo.cached(SportsLeague.NFL, SportsView.LIVE, now))
        assertTrue(sportsErrorLabel(SportsHttpException(400)).contains("HTTP 400"))
    }

    @Test fun publishedFeedsUseTheSameParserAsTheInstalledCard() {
        val directory = File("build/outputs/sports-probe")
        val fixtures = directory.listFiles()?.filter { it.name != "probe-meta.json" && it.extension == "json" }.orEmpty()
        assumeTrue("Live provider probes unavailable on this runner", fixtures.isNotEmpty())
        val now = JsonParser.parseString(File(directory, "probe-meta.json").readText()).asJsonObject["now"].asLong
        fixtures.forEach { file ->
            when (file.nameWithoutExtension) {
                "F1_RESULTS" -> SportsFeeds.f1Results(file.readText(), now)
                "F1_UPCOMING" -> SportsFeeds.f1Schedule(file.readText(), now)
                else -> SportsView.entries.forEach { view -> SportsFeeds.scoreboard(file.readText(), SportsLeague.valueOf(file.nameWithoutExtension), view, now) }
            }
        }
    }
}
