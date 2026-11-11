package pt.carlos.painel.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherNow(val tempC: Int, val code: Int, val isDay: Boolean)

/** Tempo atual pelo Open-Meteo (gratuito, sem chave). Guarda 20 minutos em cache. */
object Weather {
    // Lisboa (Alcântara). Na Fase 2 passa a ser configurável nas Definições.
    private const val LAT = 38.7069
    private const val LON = -9.1766
    private const val TTL = 20 * 60 * 1000L

    suspend fun now(ctx: Context): WeatherNow? = withContext(Dispatchers.IO) {
        val sp = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)
        val cachedAt = sp.getLong("at", 0L)
        if (System.currentTimeMillis() - cachedAt < TTL && sp.contains("t")) {
            return@withContext WeatherNow(sp.getInt("t", 0), sp.getInt("c", 0), sp.getBoolean("d", true))
        }
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
                    "&current=temperature_2m,weather_code,is_day&timezone=Europe%2FLisbon"
            )
            val con = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 6000; readTimeout = 6000 }
            val body = con.inputStream.bufferedReader().use { it.readText() }
            val cur = JSONObject(body).getJSONObject("current")
            val w = WeatherNow(
                tempC = Math.round(cur.getDouble("temperature_2m")).toInt(),
                code = cur.getInt("weather_code"),
                isDay = cur.optInt("is_day", 1) == 1,
            )
            sp.edit().putLong("at", System.currentTimeMillis()).putInt("t", w.tempC).putInt("c", w.code).putBoolean("d", w.isDay).apply()
            w
        }.getOrElse {
            if (sp.contains("t")) WeatherNow(sp.getInt("t", 0), sp.getInt("c", 0), sp.getBoolean("d", true)) else null
        }
    }
}
