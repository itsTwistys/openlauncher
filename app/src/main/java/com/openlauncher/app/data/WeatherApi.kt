package com.openlauncher.app.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class OpenMeteoResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    val timezone: String? = null,
    val hourly: HourlyWeather? = null,
    val daily: DailyWeather? = null,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Long = 0

)

data class CurrentWeather(
    @SerializedName("temperature")  val temperature: Double,
    @SerializedName("windspeed")    val windspeed: Double,
    @SerializedName("weathercode")  val weathercode: Int,
    @SerializedName("is_day")       val isDay: Int
)

data class HourlyWeather(val time: List<Long>?,
    @SerializedName("temperature_2m") val temperature: List<Double?>?,
    @SerializedName("apparent_temperature") val apparent: List<Double?>?,
    @SerializedName("precipitation_probability") val rain: List<Int?>?)
data class DailyWeather(@SerializedName("temperature_2m_max") val high: List<Double?>?,
    @SerializedName("temperature_2m_min") val low: List<Double?>?)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude")         latitude: Double,
        @Query("longitude")        longitude: Double,
        @Query("current_weather")  currentWeather: Boolean = true,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("windspeed_unit")   windspeedUnit: String = "kmh",
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,precipitation_probability",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("timeformat") timeformat: String = "unixtime",
        @Query("forecast_days") days: Int = 2
    ): OpenMeteoResponse
}

object WeatherApi {
    private val client = OkHttpClient.Builder().build()

    val service: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)
}
