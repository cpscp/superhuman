package pt.carlos.painel.transit

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Carris Metropolitana — API pública oficial (github.com/carrismetropolitana/api). */
object BusApi {
    const val BASE = "https://api.carrismetropolitana.pt/v2"

    fun departures(ctx: Context, stops: List<StopRef>, nowMs: Long, count: Int = 10): List<Departure> {
        val lines = LineColors.get(ctx)
        val seenTrips = HashSet<String>()
        return mergeStops(stops) { stop ->
            val arr = JSONArray(Http.get("$BASE/arrivals/by_stop/${stop.id.trim()}"))
            arr.objects().mapNotNull { o ->
                if (!o.isNull("observed_arrival_unix")) return@mapNotNull null        // já passou
                if (o.isNull("scheduled_arrival_unix")) return@mapNotNull null
                val sched = o.getLong("scheduled_arrival_unix") * 1000L
                val est = if (o.isNull("estimated_arrival_unix")) null else o.getLong("estimated_arrival_unix") * 1000L
                val eta = est ?: sched
                if (eta < nowMs - 30_000L || eta > nowMs + 3 * 3_600_000L) return@mapNotNull null
                val trip = o.str("trip_id")
                synchronized(seenTrips) { if (trip.isNotEmpty() && !seenTrips.add(trip)) return@mapNotNull null }
                val lineId = o.str("line_id")
                val info = lines[lineId]
                val delay = ((eta - sched) / 60_000L).toInt()
                Departure(
                    etaMs = eta, scheduledMs = sched,
                    badge = info?.shortName?.ifBlank { null } ?: lineId,
                    destination = o.str("headsign"),
                    detail = "${stop.label} · ${stop.id}".let { if (stop.name.isBlank()) "Paragem ${stop.id}" else it } +
                        if (est == null) " · horário" else "",
                    status = if (delay >= 2) LiveStatus.ATRASADO else LiveStatus.PREVISTO,
                    delayMin = delay.coerceAtLeast(0),
                    realtime = est != null,
                    badgeColor = info?.color,
                    badgeTextColor = info?.textColor,
                    tags = stop.id
                )
            }
        }.sortedBy { it.etaMs }.take(count)
    }
}

/** Corre [fetch] em cada paragem; só falha se falharem todas (como no Glass TV). */
internal fun mergeStops(stops: List<StopRef>, fetch: (StopRef) -> List<Departure>): List<Departure> {
    if (stops.isEmpty()) return emptyList()
    val results = stops.map { st -> runCatching { fetch(st) } }
    if (results.all { it.isFailure }) throw (results.first().exceptionOrNull() as? Exception ?: Exception("Sem dados"))
    return results.mapNotNull { it.getOrNull() }.flatten()
}

/** Cores oficiais das linhas, guardadas em cache durante 7 dias. */
object LineColors {
    data class Info(val shortName: String, val color: Int?, val textColor: Int?)

    @Volatile private var mem: Map<String, Info>? = null
    private const val MAX_AGE = 7L * 24 * 3_600_000

    fun get(ctx: Context): Map<String, Info> {
        mem?.let { return it }
        val f = File(ctx.filesDir, "cm_lines.json")
        val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE
        val txt = if (fresh) f.readText() else {
            try {
                Http.get("${BusApi.BASE}/lines").also { f.writeText(it) }
            } catch (e: Exception) {
                if (f.exists()) f.readText() else return emptyMap()
            }
        }
        val map = HashMap<String, Info>()
        runCatching {
            val a = JSONArray(txt)
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                map[o.str("id")] = Info(o.str("short_name"), color(o.str("color")), color(o.str("text_color")))
            }
        }
        mem = map
        return map
    }

    private fun color(s: String): Int? = if (s.isBlank()) null else runCatching { Color.parseColor(s) }.getOrNull()
}

