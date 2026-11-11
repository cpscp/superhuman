package pt.carlos.painel.transit

import org.json.JSONObject

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

