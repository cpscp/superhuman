package pt.carlos.painel.transit

import org.json.JSONArray
import org.json.JSONObject

data class StopRef(val id: String, val name: String) {
    val label: String get() = name.ifBlank { "Paragem $id" }
}

/** Estado comum a comboios e autocarros (igual ao Glass TV). */
enum class LiveStatus(val label: String) { PREVISTO("Previsto"), ATRASADO("Atrasado"), SUPRIMIDO("Suprimido") }

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
