package pt.carlos.partidas.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * TRAIN  = CP (cp.pt), uma estação + sentido
 * BUS    = Carris (Lisboa), uma ou mais paragens — como no Glass TV
 * BUS_CM = Carris Metropolitana, uma ou mais paragens
 */
enum class Mode { TRAIN, BUS, BUS_CM }

data class StopRef(val id: String, val name: String) {
    val label: String get() = name.ifBlank { "Paragem $id" }
}

/** Estado comum a comboios e autocarros (igual ao Glass TV). */
enum class LiveStatus(val label: String) { PREVISTO("Previsto"), ATRASADO("Atrasado"), SUPRIMIDO("Suprimido") }

/**
 * Um quadro de partidas. Vive na app; um widget mostra um quadro.
 * Comboio: [stopId]/[stopName] = estação, [direction] = sentido (ex.: Cascais).
 * Autocarro: paragens = [stopId] + [extraStops]; o quadro junta tudo o que passa nelas.
 */
data class Board(
    val id: String = UUID.randomUUID().toString(),
    val mode: Mode,
    val stopId: String,
    val stopName: String,
    val filter: String = "",
    val title: String = "",
    val extraStops: List<StopRef> = emptyList(),
    val direction: String = ""
) {
    val stops: List<StopRef> get() =
        (listOf(StopRef(stopId, stopName)) + extraStops).filter { it.id.isNotBlank() || it.name.isNotBlank() }.distinctBy { it.id + "|" + it.name }

    val displayTitle: String get() = title.ifBlank {
        when (mode) {
            Mode.TRAIN -> stopName.ifBlank { "Estação" }
            else -> if (stops.size > 1) "${stops.size} paragens" else StopRef(stopId, stopName).label
        }
    }

    /** Linha por baixo do título, como no Glass TV ("Sentido Cascais", "5 paragens"). */
    val subtitle: String get() = when (mode) {
        Mode.TRAIN -> if (direction.isBlank()) "Todos os sentidos" else "Sentido $direction"
        else -> if (stops.size == 1) "Paragem ${stops.first().id}" else "${stops.size} paragens"
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("mode", mode.name)
        .put("stopId", stopId).put("stopName", stopName)
        .put("filter", filter).put("title", title)
        .put("direction", direction)
        .put("extra", JSONArray().also { a ->
            extraStops.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
        })

    companion object {
        fun fromJson(o: JSONObject) = Board(
            id = o.getString("id"),
            mode = runCatching { Mode.valueOf(o.getString("mode")) }.getOrDefault(Mode.TRAIN),
            stopId = o.getString("stopId"),
            stopName = o.optString("stopName", ""),
            filter = o.optString("filter", ""),
            title = o.optString("title", ""),
            direction = o.optString("direction", ""),
            extraStops = o.optJSONArray("extra")?.let { a ->
                (0 until a.length()).mapNotNull { i ->
                    a.optJSONObject(i)?.let { StopRef(it.optString("id", ""), it.optString("name", "")) }
                }
            } ?: emptyList()
        )
    }
}

/**
 * Uma partida = um mosaico do quadro. Tempos em epoch millis.
 * [badge]  "Linha 2" (plataforma CP) ou número da carreira
 * [detail] "Viagem 25 min · Nº 19217" ou "Paragem 10210"
 */
data class Departure(
    val etaMs: Long,
    val scheduledMs: Long,
    val badge: String,
    val destination: String,
    val detail: String,
    val status: LiveStatus,
    val delayMin: Int,
    val realtime: Boolean,
    /** cores próprias da linha (Carris Metropolitana); null = cores do quadro */
    val badgeColor: Int? = null,
    val badgeTextColor: Int? = null,
    /** para o filtro: nº do comboio, id da paragem… */
    val tags: String = ""
) {
    val cancelled: Boolean get() = status == LiveStatus.SUPRIMIDO

    /** Hora original riscada, quando há atraso (como no Glass TV). */
    val oldTimeMs: Long? get() = if (status == LiveStatus.ATRASADO && scheduledMs != etaMs) scheduledMs else null

    fun toJson(): JSONObject = JSONObject()
        .put("e", etaMs).put("s", scheduledMs)
        .put("b", badge).put("d", destination).put("i", detail)
        .put("st", status.name).put("dl", delayMin).put("r", realtime)
        .put("c", badgeColor ?: JSONObject.NULL)
        .put("t", badgeTextColor ?: JSONObject.NULL)
        .put("g", tags)

    companion object {
        fun fromJson(o: JSONObject) = Departure(
            etaMs = o.getLong("e"),
            scheduledMs = o.getLong("s"),
            badge = o.optString("b", ""),
            destination = o.optString("d", ""),
            detail = o.optString("i", ""),
            status = runCatching { LiveStatus.valueOf(o.getString("st")) }.getOrDefault(LiveStatus.PREVISTO),
            delayMin = o.optInt("dl", 0),
            realtime = o.optBoolean("r", false),
            badgeColor = if (o.isNull("c")) null else o.getInt("c"),
            badgeTextColor = if (o.isNull("t")) null else o.getInt("t"),
            tags = o.optString("g", "")
        )

        fun listToJson(list: List<Departure>): JSONArray =
            JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }

        fun listFromJson(a: JSONArray?): List<Departure> =
            if (a == null) emptyList()
            else (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { runCatching { fromJson(it) }.getOrNull() } }
    }
}

/** Resultado de pesquisa de estação/paragem. */
data class Place(val id: String, val name: String, val sub: String = "")
