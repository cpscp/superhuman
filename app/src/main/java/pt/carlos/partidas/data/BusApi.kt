package pt.carlos.partidas.data

import android.content.Context
import android.graphics.Color
import android.util.JsonReader
import android.util.JsonToken
import org.json.JSONArray
import org.json.JSONObject
import pt.carlos.partidas.model.Departure
import pt.carlos.partidas.model.LiveStatus
import pt.carlos.partidas.model.Place
import pt.carlos.partidas.model.StopRef
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStreamReader

/**
 * Carris (Lisboa) — o mesmo serviço da página oficial paragem.alx.carris.pt, como no Glass TV.
 * Resposta: { arrivalTimes: [ { arrivalTime: { routeNumber, destinationFullName,
 *   estimatedArrivalTimeLeft (minutos), hasAlert, busNumber } } ] }
 */
object CarrisApi {
    fun departures(stops: List<StopRef>, nowMs: Long, count: Int = 10): List<Departure> =
        mergeStops(stops) { stop -> one(stop, nowMs) }
            .filter { it.etaMs >= nowMs - 30_000L }
            .distinctBy { it.badge + it.destination + it.etaMs / 60_000L }
            .sortedBy { it.etaMs }
            .take(count)

    private fun one(stop: StopRef, nowMs: Long): List<Departure> {
        val j = JSONObject(
            Http.get(
                "https://paragem.alx.carris.pt/api/busstop/${stop.id.trim()}",
                headers = mapOf("Accept" to "application/json", "Referer" to "https://paragem.alx.carris.pt/busstop/${stop.id.trim()}")
            )
        )
        val minuteNow = nowMs - nowMs % 60_000L
        return j.optJSONArray("arrivalTimes")?.objects().orEmpty().mapNotNull { item ->
            val a = item.optJSONObject("arrivalTime") ?: item
            val left = a.opt("estimatedArrivalTimeLeft")?.toString()
                ?.let { Regex("-?\\d+").find(it)?.value?.toLongOrNull() } ?: return@mapNotNull null
            val eta = minuteNow + left.coerceAtLeast(0) * 60_000L
            Departure(
                etaMs = eta, scheduledMs = eta,
                badge = a.optString("routeNumber"),
                destination = a.strOrNull("destinationFullName") ?: a.optString("destinationName"),
                detail = "${stop.label} · ${stop.id}".let { if (stop.name.isBlank()) "Paragem ${stop.id}" else it },
                status = if (a.optBoolean("hasAlert", false)) LiveStatus.ATRASADO else LiveStatus.PREVISTO,
                delayMin = 0,
                realtime = true,
                tags = stop.id
            )
        }
    }
}

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

/**
 * Índice local das paragens (id, nome, linhas) para pesquisar por nome.
 * Descarrega /stops uma vez (alguns MB) e guarda um ficheiro compacto.
 */
object StopsIndex {
    private const val MAX_AGE = 30L * 24 * 3_600_000

    private fun file(ctx: Context) = File(ctx.filesDir, "cm_stops.tsv")

    fun ensure(ctx: Context, onProgress: (String) -> Unit) {
        val f = file(ctx)
        if (f.exists() && f.length() > 1000 && System.currentTimeMillis() - f.lastModified() < MAX_AGE) return
        onProgress("A descarregar paragens (só na primeira vez)…")
        val tmp = File(f.path + ".tmp")
        val conn = Http.open("${BusApi.BASE}/stops", 60_000)
        try {
            JsonReader(InputStreamReader(BufferedInputStream(conn.inputStream), Charsets.UTF_8)).use { r ->
                tmp.bufferedWriter().use { w ->
                    r.beginArray()
                    var n = 0
                    while (r.hasNext()) {
                        var id = ""
                        var name = ""
                        val lines = ArrayList<String>()
                        r.beginObject()
                        while (r.hasNext()) {
                            when (r.nextName()) {
                                "id" -> id = r.stringOrEmpty()
                                "long_name" -> name = r.stringOrEmpty()
                                "line_ids" -> {
                                    if (r.peek() == JsonToken.BEGIN_ARRAY) {
                                        r.beginArray()
                                        while (r.hasNext()) lines += r.stringOrEmpty()
                                        r.endArray()
                                    } else r.skipValue()
                                }
                                else -> r.skipValue()
                            }
                        }
                        r.endObject()
                        if (id.isNotEmpty()) {
                            w.write(id); w.write("\t")
                            w.write(name.replace('\t', ' ').replace('\n', ' ')); w.write("\t")
                            w.write(lines.joinToString(",")); w.write("\n")
                        }
                        n++
                        if (n % 2000 == 0) onProgress("A preparar paragens… $n")
                    }
                    r.endArray()
                }
            }
        } finally {
            conn.disconnect()
        }
        if (f.exists()) f.delete()
        tmp.renameTo(f)
    }

    fun search(ctx: Context, query: String, limit: Int = 40): List<Place> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        val q = query.trim()
        val isId = q.all { it.isDigit() }
        val tokens = Text.norm(q).split(Regex("\\s+")).filter { it.isNotBlank() }
        val out = ArrayList<Place>()
        f.useLines { seq ->
            for (line in seq) {
                val p = line.split('\t')
                if (p.size < 2) continue
                val match = if (isId) p[0].startsWith(q)
                            else Text.norm(p[1]).let { n -> tokens.all { n.contains(it) } }
                if (match) {
                    val lines = p.getOrNull(2).orEmpty().split(',').filter { it.isNotBlank() }
                    val sub = "Paragem ${p[0]}" + if (lines.isNotEmpty())
                        " · linhas " + lines.take(8).joinToString(" ") + (if (lines.size > 8) "…" else "") else ""
                    out += Place(p[0], p[1], sub)
                    if (out.size >= limit) break
                }
            }
        }
        return out
    }

    private fun JsonReader.stringOrEmpty(): String =
        if (peek() == JsonToken.NULL) { nextNull(); "" } else nextString()
}
