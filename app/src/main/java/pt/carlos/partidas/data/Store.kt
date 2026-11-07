package pt.carlos.partidas.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pt.carlos.partidas.model.Board
import pt.carlos.partidas.model.Departure
import pt.carlos.partidas.model.Mode
import pt.carlos.partidas.model.StopRef

/** Os quadros criados na app. */
object BoardStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("boards", Context.MODE_PRIVATE)

    fun all(ctx: Context): List<Board> {
        val s = p(ctx).getString("list", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { runCatching { Board.fromJson(it) }.getOrNull() } }
        }.getOrDefault(emptyList())
    }

    fun get(ctx: Context, id: String): Board? = all(ctx).firstOrNull { it.id == id }

    private fun write(ctx: Context, list: List<Board>) {
        val a = JSONArray().also { arr -> list.forEach { arr.put(it.toJson()) } }
        p(ctx).edit().putString("list", a.toString()).commit()
    }

    fun upsert(ctx: Context, b: Board) {
        val list = all(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == b.id }
        if (i >= 0) list[i] = b else list += b
        write(ctx, list)
    }

    fun delete(ctx: Context, id: String) {
        write(ctx, all(ctx).filterNot { it.id == id })
        DepartureCache.remove(ctx, id)
    }

    fun move(ctx: Context, id: String, delta: Int) {
        val list = all(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        val b = list.removeAt(i)
        list.add(j, b)
        write(ctx, list)
    }
}

/** Que quadro mostra cada widget. */
object WidgetLinks {
    private const val PENDING_TTL = 10 * 60_000L
    private fun p(ctx: Context) = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun get(ctx: Context, widgetId: Int): String? = p(ctx).getString("w_$widgetId", null)

    fun set(ctx: Context, widgetId: Int, boardId: String) {
        p(ctx).edit().putString("w_$widgetId", boardId).commit()
    }

    fun remove(ctx: Context, widgetId: Int) {
        p(ctx).edit().remove("w_$widgetId").apply()
    }

    fun widgetsFor(ctx: Context, boardId: String, allIds: IntArray): List<Int> =
        allIds.filter { get(ctx, it) == boardId }

    /** Quadro escolhido na app à espera que o launcher confirme o novo widget. */
    fun savePending(ctx: Context, boardId: String) {
        p(ctx).edit().putString("pending", boardId).putLong("pending_at", System.currentTimeMillis()).commit()
    }

    fun adoptPending(ctx: Context, widgetId: Int): Boolean {
        val prefs = p(ctx)
        val b = prefs.getString("pending", null) ?: return false
        val fresh = System.currentTimeMillis() - prefs.getLong("pending_at", 0L) < PENDING_TTL
        val ed = prefs.edit().remove("pending").remove("pending_at")
        if (fresh) ed.putString("w_$widgetId", b)
        ed.commit()
        return fresh
    }
}

data class CacheEntry(val fetchedAt: Long, val items: List<Departure>, val error: String?)

/** Partidas em cache, por quadro (partilhadas entre a app e os widgets). */
object DepartureCache {
    private fun p(ctx: Context) = ctx.getSharedPreferences("cache", Context.MODE_PRIVATE)

    fun get(ctx: Context, boardId: String): CacheEntry? = p(ctx).getString("b_$boardId", null)?.let {
        runCatching {
            val o = JSONObject(it)
            CacheEntry(
                o.getLong("at"),
                Departure.listFromJson(o.optJSONArray("items")),
                if (o.isNull("err")) null else o.optString("err")
            )
        }.getOrNull()
    }

    private fun put(ctx: Context, boardId: String, e: CacheEntry) {
        val o = JSONObject()
            .put("at", e.fetchedAt)
            .put("items", Departure.listToJson(e.items))
            .put("err", e.error ?: JSONObject.NULL)
        p(ctx).edit().putString("b_$boardId", o.toString()).remove("r_$boardId").commit()
    }

    fun saveOk(ctx: Context, boardId: String, at: Long, items: List<Departure>) =
        put(ctx, boardId, CacheEntry(at, items, null))

    /** Mantém as últimas partidas boas e só regista o erro. */
    fun saveError(ctx: Context, boardId: String, at: Long, msg: String) {
        val old = get(ctx, boardId)
        put(ctx, boardId, CacheEntry(old?.fetchedAt ?: at, old?.items ?: emptyList(), msg))
    }

    fun markRefreshing(ctx: Context, boardId: String) {
        p(ctx).edit().putLong("r_$boardId", System.currentTimeMillis()).commit()
    }

    fun isRefreshing(ctx: Context, boardId: String): Boolean =
        System.currentTimeMillis() - p(ctx).getLong("r_$boardId", 0L) < 45_000L

    fun remove(ctx: Context, boardId: String) {
        p(ctx).edit().remove("b_$boardId").remove("r_$boardId").apply()
    }
}

object Repo {
    /** Vai buscar as partidas de um quadro (sem tocar na cache). Até 10, como no Glass TV. */
    fun fetch(ctx: Context, b: Board, now: Long): List<Departure> {
        val raw = when (b.mode) {
            Mode.TRAIN -> TrainApi.departures(b.stopId, b.stopName, b.direction, now, 10)
            Mode.BUS -> CarrisApi.departures(b.stops, now, 20)
            Mode.BUS_CM -> BusApi.departures(ctx, b.stops, now, 20)
        }
        return applyFilter(raw, b.filter).sortedBy { it.etaMs }.take(10)
    }

    fun refresh(ctx: Context, boardId: String) {
        val b = BoardStore.get(ctx, boardId) ?: return
        val now = System.currentTimeMillis()
        try {
            DepartureCache.saveOk(ctx, boardId, now, fetch(ctx, b, now))
        } catch (e: Exception) {
            DepartureCache.saveError(ctx, boardId, now, friendly(e))
        }
    }

    fun friendly(e: Exception): String = when (e) {
        is java.net.UnknownHostException -> "sem internet"
        is java.net.SocketTimeoutException -> "o serviço não respondeu"
        is javax.net.ssl.SSLException -> "erro de certificado do serviço"
        is org.json.JSONException -> "resposta inesperada do serviço"
        else -> e.message ?: e.javaClass.simpleName
    }

    /** Filtro opcional: palavras separadas por vírgula; casa com destino (contém), linha/carreira ou paragem (igual). */
    fun applyFilter(list: List<Departure>, filter: String): List<Departure> {
        val toks = filter.split(',', ';').map { Text.key(it) }.filter { it.isNotEmpty() }
        if (toks.isEmpty()) return list
        return list.filter { d ->
            val dn = Text.key(d.destination)
            val bn = Text.key(d.badge)
            toks.any { t -> dn.contains(t) || bn == t || bn == "linha $t" || Text.key(d.tags) == t }
        }
    }
}

/** Quadros iniciais = os do Glass TV (glass-config.json). Só no primeiro arranque. */
object Defaults {
    fun seedIfFirstRun(ctx: Context) {
        val p = ctx.getSharedPreferences("boards", Context.MODE_PRIVATE)
        if (p.getBoolean("seeded", false)) return
        if (BoardStore.all(ctx).isEmpty()) {
            BoardStore.upsert(ctx, Board(
                mode = Mode.TRAIN, stopId = "", stopName = "Alcântara-Mar", direction = "Cascais"
            ))
            val stops = listOf("10210", "10209", "10203", "10202", "10201")
            BoardStore.upsert(ctx, Board(
                mode = Mode.BUS, stopId = stops.first(), stopName = "",
                extraStops = stops.drop(1).map { StopRef(it, "") },
                title = "Alcântara"
            ))
        }
        p.edit().putBoolean("seeded", true).apply()
    }
}
