package com.carlos.superhuman.net

import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.WeatherNow
import java.net.URLEncoder

data class Place(val name: String, val admin: String, val country: String, val lat: Double, val lon: Double)

object Weather {
    suspend fun refresh(): WeatherNow? = runCatching {
        val s = Store.data.settings
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${s.latitude}&longitude=${s.longitude}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max" +
            "&hourly=temperature_2m&timezone=auto&forecast_days=1"
        val root = Http.parse(Http.get(url))
        val cur = root.obj("current")
        val daily = root.obj("daily")
        fun firstOf(key: String): Double? = daily.arr(key)?.firstOrNull()?.asDoubleOrNull()
        val hourly = root.obj("hourly").arr("temperature_2m")?.mapNotNull { it.asDoubleOrNull() } ?: emptyList()
        val w = WeatherNow(
            temp = cur.num("temperature_2m") ?: 0.0,
            feels = cur.num("apparent_temperature") ?: 0.0,
            code = cur.int("weather_code") ?: 0,
            wind = cur.num("wind_speed_10m") ?: 0.0,
            max = firstOf("temperature_2m_max") ?: 0.0,
            min = firstOf("temperature_2m_min") ?: 0.0,
            rainProb = (firstOf("precipitation_probability_max") ?: 0.0).toInt(),
            uv = firstOf("uv_index_max") ?: 0.0,
            hourly = hourly,
            updatedAt = System.currentTimeMillis(),
        )
        Store.update { it.copy(weather = w) }
        w
    }.getOrNull()

    suspend fun searchPlaces(query: String): List<Place> = runCatching {
        val q = URLEncoder.encode(query, "UTF-8")
        val root = Http.parse(Http.get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=pt"))
        root.arr("results")?.map {
            Place(
                name = it.str("name") ?: "",
                admin = it.str("admin1") ?: "",
                country = it.str("country") ?: "",
                lat = it.num("latitude") ?: 0.0,
                lon = it.num("longitude") ?: 0.0,
            )
        } ?: emptyList()
    }.getOrDefault(emptyList())
}
