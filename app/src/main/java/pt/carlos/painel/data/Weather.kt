package pt.carlos.painel.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pt.carlos.painel.PainelApp
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherNow(val tempC: Int, val code: Int, val isDay: Boolean)
data class WeatherHour(val hour: Int, val tempC: Int, val code: Int, val isDay: Boolean, val rainPct: Int)
data class WeatherDay(val date: java.time.LocalDate, val code: Int, val max: Int, val min: Int, val rainPct: Int)
data class Forecast(val now: WeatherNow, val max: Int, val min: Int, val hours: List<WeatherHour>, val days: List<WeatherDay>, val place: String)
data class GeoPlace(val name: String, val region: String, val lat: Double, val lon: Double)

/** Tempo pelo Open-Meteo (gratuito, sem chave). Cache de 20 minutos por localização. */
object Weather {
    private const val TTL = 20 * 60 * 1000L

    private fun get(url: String): String {
        val con = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
        try { return con.inputStream.bufferedReader().use { it.readText() } } finally { con.disconnect() }
    }

    suspend fun forecast(ctx: Context): Forecast? = withContext(Dispatchers.IO) {
        val prefs = PainelApp.instance.prefs
        val lat = prefs.weatherLat
        val lon = prefs.weatherLon
        val sp = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)
        val locKey = "v2:$lat,$lon"
        val fresh = sp.getString("loc", "") == locKey && System.currentTimeMillis() - sp.getLong("at", 0) < TTL
        val body = if (fresh) sp.getString("json", null) else null
        val text = body ?: runCatching {
            get(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,is_day" +
                    "&hourly=temperature_2m,weather_code,is_day,precipitation_probability" +
                    "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max&forecast_days=6&timezone=auto"
            ).also { sp.edit().putString("json", it).putString("loc", locKey).putLong("at", System.currentTimeMillis()).apply() }
        }.getOrNull() ?: sp.getString("json", null) ?: return@withContext null
        runCatching { parse(text, prefs.weatherPlace) }.getOrNull()
    }

    suspend fun now(ctx: Context): WeatherNow? = forecast(ctx)?.now

    private fun parse(text: String, place: String): Forecast {
        val j = JSONObject(text)
        val cur = j.getJSONObject("current")
        val now = WeatherNow(Math.round(cur.getDouble("temperature_2m")).toInt(), cur.getInt("weather_code"), cur.optInt("is_day", 1) == 1)
        val h = j.getJSONObject("hourly")
        val times = h.getJSONArray("time")
        val curTime = cur.getString("time").take(13)   // "2026-11-11T15"
        var start = 0
        for (i in 0 until times.length()) if (times.getString(i).take(13) >= curTime) { start = i; break }
        val hours = (start + 1..minOf(start + 6, times.length() - 1)).map { i ->
            WeatherHour(
                hour = times.getString(i).substring(11, 13).toInt(),
                tempC = Math.round(h.getJSONArray("temperature_2m").getDouble(i)).toInt(),
                code = h.getJSONArray("weather_code").getInt(i),
                isDay = h.getJSONArray("is_day").optInt(i, 1) == 1,
                rainPct = h.getJSONArray("precipitation_probability").optInt(i, 0),
            )
        }
        val d = j.getJSONObject("daily")
        val dTimes = d.getJSONArray("time")
        val days = (1 until dTimes.length()).map { i ->
            WeatherDay(
                date = java.time.LocalDate.parse(dTimes.getString(i)),
                code = d.getJSONArray("weather_code").getInt(i),
                max = Math.round(d.getJSONArray("temperature_2m_max").getDouble(i)).toInt(),
                min = Math.round(d.getJSONArray("temperature_2m_min").getDouble(i)).toInt(),
                rainPct = d.getJSONArray("precipitation_probability_max").optInt(i, 0),
            )
        }.take(5)
        return Forecast(
            now = now,
            max = Math.round(d.getJSONArray("temperature_2m_max").getDouble(0)).toInt(),
            min = Math.round(d.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
            hours = hours,
            days = days,
            place = place,
        )
    }

    /** Pesquisa de localidades (para as Definições). */
    suspend fun search(query: String): List<GeoPlace> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val a = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=pt&format=json"))
                .optJSONArray("results") ?: return@runCatching emptyList()
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                GeoPlace(
                    name = o.getString("name"),
                    region = listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                    lat = o.getDouble("latitude"),
                    lon = o.getDouble("longitude"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Céu limpo"
        1 -> "Pouco nublado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Nevoeiro"
        in 51..57 -> "Chuvisco"
        in 61..67 -> "Chuva"
        in 71..77, 85, 86 -> "Neve"
        in 80..82 -> "Aguaceiros"
        else -> "Trovoada"
    }
}
