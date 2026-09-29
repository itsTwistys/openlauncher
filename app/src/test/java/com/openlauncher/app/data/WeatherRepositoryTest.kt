package com.openlauncher.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.openlauncher.app.model.WeatherState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherRepositoryTest {
    @Test fun newRepositoryRestoresConditionsWithoutResettingTheirAge() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = 1_800_000_000_000L
        val weather = WeatherState(21.5, 3, 12.0, updatedAtMs = now - 600_000, isDay = true)
        WeatherRepository(context).save(weather)
        assertEquals(weather, WeatherRepository(context).load(now))
        assertNull(WeatherRepository(context).load(now + WeatherCache.MAX_AGE_MS))
    }
    @Test fun interruptedWriteRestoresLastSuccessfulForecast() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = 1_800_000_000_000L
        val weather = WeatherState(21.5, 3, 12.0, updatedAtMs = now, isDay = true)
        WeatherRepository(context).save(weather)
        // AtomicFile leaves a .new file after power loss; its committed file is still readable.
        File(context.filesDir, "weather-cache.json.new").writeText("{interrupted")
        assertEquals(weather, WeatherRepository(context).load(now))
    }
}
