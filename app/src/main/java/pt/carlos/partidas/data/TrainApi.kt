package pt.carlos.partidas.data

import org.json.JSONArray
import org.json.JSONObject
import pt.carlos.partidas.model.Departure
import pt.carlos.partidas.model.LiveStatus
import pt.carlos.partidas.model.Place
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * CP em tempo real — a mesma lógica do Glass TV (CpRepo), através dos serviços públicos do cp.pt:
 *  - https://www.cp.pt/fe-config.json            → URL e chaves públicas do frontend
 *  - {travelApi}/stations                         → catálogo de estações
 *  - {travelApi}/stations/{code}/timetable/{dia}  → partidas (view=DEPARTURES)
 *  - {travelApi}/trains/{n}/timetable/{dia}       → paragens do comboio (tempo de viagem)
 * Não é uma API oficial: se a CP mudar algo, só este ficheiro precisa de ajuste.
 * Tudo bloqueante — chamar fora da thread principal.
 */
object TrainApi {
    val LISBON: ZoneId = ZoneId.of("Europe/Lisbon")

    private data class Config(val url: String, val key: String, val id: String, val secret: String)

    @Volatile private var config: Config? = null
    @Volatile private var configAt = 0L
    @Volatile private var stationsCache: List<JSONObject>? = null
    private val travelCache = HashMap<String, Int?>()

    @Synchronized
    private fun cfg(force: Boolean = false): Config {
        val c = config
        if (!force && c != null && System.currentTimeMillis() - configAt < 12 * 3600_000L) return c
        val j = JSONObject(Http.get("https://www.cp.pt/fe-config.json", headers = mapOf("Accept" to "application/json")))
        return Config(
            url = j.getString("travelApiUrl").trimEnd('/'),
            key = j.getString("travelApiKey"),
            id = j.getString("xcck"),
            secret = j.getString("xccs")
        ).also { config = it; configAt = System.currentTimeMillis() }
    }

    private fun api(path: String): String {
        fun call(c: Config) = Http.get(
            c.url + path,
            headers = mapOf(
                "Accept" to "application/json",
                "x-api-key" to c.key,
                "x-cp-connect-id" to c.id,
                "x-cp-connect-secret" to c.secret,
                "Origin" to "https://www.cp.pt",
                "Referer" to "https://www.cp.pt/"
            )
        )
        return try {
            call(cfg())
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("403") == true) call(cfg(force = true))
            else throw e
        }
    }

    private fun stations(): List<JSONObject> =
        stationsCache ?: JSONArray(api("/stations")).objects().also { stationsCache = it }

    /** Pesquisa de estações pelo nome (para o editor). */
    fun search(query: String): List<Place> {
        val toks = Text.key(query).split(" ").filter { it.isNotBlank() }
        return stations()
            .filter { s -> Text.key(s.optString("designation")).let { n -> toks.all { n.contains(it) } } }
            .take(30)
            .map { Place(it.optString("code"), it.optString("designation"), "Estação CP") }
    }

    /** Resolve um nome ("Alcântara-Mar") para (código, nome oficial). */
    fun station(name: String): Pair<String, String> {
        val key = Text.key(name)
        val list = stations()
        val hit = list.firstOrNull { Text.key(it.optString("designation")) == key }
            ?: list.firstOrNull { Text.key(it.optString("designation")).contains(key) }
            ?: error("Estação \"$name\" não encontrada na CP")
        return hit.getString("code") to hit.getString("designation")
    }

    /**
     * Próximas [count] partidas no sentido [destination] (vazio = todos).
     * A CP devolve só 5 partidas por pedido, nos dois sentidos: pede páginas sucessivas
     * até juntar [count] no sentido certo (igual ao Glass TV).
     */
    fun departures(stationCode: String, stationName: String, destination: String, nowMs: Long, count: Int = 10): List<Departure> {
        val code = stationCode.ifBlank { station(stationName).first }
        val now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMs), LISBON)
        val dest = Text.key(destination)

        data class T(val number: String, val dest: String, val sched: LocalDateTime, val exp: LocalDateTime,
                     val delay: Int, val status: LiveStatus, val platform: String?)
        val out = ArrayList<T>()
        var cursor = now.minusMinutes(10)
        var pages = 0
        while (out.size < count && pages < 12) {
            pages++
            val day = cursor.toLocalDate()
            val body = JSONObject(api("/stations/$code/timetable/$day?view=DEPARTURES&start=%02d:%02d".format(cursor.hour, cursor.minute)))
            val stops = body.optJSONArray("stationStops")?.objects().orEmpty()
            if (stops.isEmpty()) { cursor = day.plusDays(1).atStartOfDay(); continue }
            var last = cursor
            for (s in stops) {
                val sched = relative(s.strOrNull("departureTime") ?: s.strOrNull("arrivalTime"), cursor) ?: continue
                if (sched.isAfter(last)) last = sched
                val destName = s.optJSONObject("trainDestination")?.optString("designation").orEmpty()
                if (!sameDirection(destName, dest)) continue
                val delay = s.optInt("delay", 0).coerceAtLeast(0)
                val etd = relative(s.strOrNull("ETD"), sched.minusHours(1))
                val expected = etd ?: sched.plusMinutes(delay.toLong())
                if (expected.isBefore(now.minusSeconds(30))) continue
                val suppressed = s.optJSONObject("supression") != null
                val realDelay = maxOf(delay, Duration.between(sched, expected).toMinutes().toInt())
                out += T(
                    number = s.optString("trainNumber"),
                    dest = destName.ifBlank { destination },
                    sched = sched, exp = expected, delay = realDelay,
                    status = when {
                        suppressed -> LiveStatus.SUPRIMIDO
                        realDelay >= 1 -> LiveStatus.ATRASADO
                        else -> LiveStatus.PREVISTO
                    },
                    platform = s.strOrNull("platform")
                )
            }
            // próxima página: 1 min depois do último comboio desta (nunca para trás)
            val next = last.plusMinutes(1)
            cursor = if (next.isAfter(cursor)) next else cursor.plusMinutes(30)
        }

        return out.distinctBy { it.number + it.sched }.sortedBy { it.exp }.take(count).map { t ->
            val travel = travelTime(t.number, t.sched.toLocalDate(), code, dest)
            Departure(
                etaMs = t.exp.atZone(LISBON).toInstant().toEpochMilli(),
                scheduledMs = t.sched.atZone(LISBON).toInstant().toEpochMilli(),
                badge = t.platform?.let { "Linha $it" } ?: "",
                destination = t.dest,
                detail = listOfNotNull(travel?.let { "Viagem $it min" }, "Nº ${t.number}").joinToString(" · "),
                status = t.status,
                delayMin = t.delay,
                realtime = true,
                tags = t.number
            )
        }
    }

    /** Minutos entre a partida na origem e a chegada ao destino (cacheado por comboio/dia). */
    private fun travelTime(number: String, day: LocalDate, originCode: String, dest: String): Int? {
        val key = "$number|$day|$dest"
        synchronized(travelCache) { if (travelCache.containsKey(key)) return travelCache[key] }
        val minutes = runCatching {
            val j = JSONObject(api("/trains/$number/timetable/$day"))
            val stops = j.optJSONArray("trainStops")?.objects().orEmpty()
            val o = stops.firstOrNull { it.optJSONObject("station")?.optString("code") == originCode }
            val d = (if (dest.isBlank()) null else stops.lastOrNull {
                Text.key(it.optJSONObject("station")?.optString("designation").orEmpty()).contains(dest)
            }) ?: stops.lastOrNull()
            val dep = parseTime(o?.strOrNull("departure"), day)
            val arr = parseTime(d?.strOrNull("arrival") ?: d?.strOrNull("departure"), day)
            if (dep != null && arr != null) Duration.between(dep, if (arr.isBefore(dep)) arr.plusDays(1) else arr).toMinutes().toInt() else null
        }.getOrNull()
        synchronized(travelCache) {
            if (travelCache.size > 200) travelCache.clear()
            travelCache[key] = minutes
        }
        return minutes
    }

    /**
     * "Sentido Cascais" = todos os comboios que seguem para oeste, mesmo que terminem antes
     * (Caxias, Oeiras…). Só se excluem os que vão para Cais do Sodré (o sentido oposto).
     */
    private val westbound = listOf(
        "santo amaro", "belem", "alges", "cruz quebrada", "caxias", "paco de arcos", "oeiras",
        "carcavelos", "parede", "sao pedro do estoril", "s pedro do estoril", "sao joao do estoril",
        "s joao do estoril", "estoril", "monte estoril", "cascais"
    )

    private fun sameDirection(destName: String, wanted: String): Boolean {
        val d = Text.key(destName)
        if (d.isBlank()) return false
        if (wanted.isBlank()) return true
        if (d.contains(wanted)) return true
        if (wanted == "cascais") return westbound.any { d.contains(it) } && !d.contains("cais do sodre")
        return false
    }

    /** "HH:mm" → data/hora mais próxima a seguir à referência (trata a passagem da meia-noite). */
    private fun relative(raw: String?, ref: LocalDateTime): LocalDateTime? {
        val t = parseTime(raw, ref.toLocalDate()) ?: return null
        return if (t.isBefore(ref.minusHours(6))) t.plusDays(1) else t
    }

    private fun parseTime(raw: String?, day: LocalDate): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        runCatching { return LocalDateTime.parse(raw.take(19)) }
        val m = Regex("(\\d{1,2}):(\\d{2})").find(raw) ?: return null
        val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
        return if (h >= 24) day.plusDays(1).atTime(h - 24, min) else day.atTime(h, min)
    }
}
