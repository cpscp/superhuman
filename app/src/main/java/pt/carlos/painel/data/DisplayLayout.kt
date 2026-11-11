package pt.carlos.painel.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Um mosaico do modo Display. [weight] 0 = altura do conteúdo; >0 = parte da altura livre da coluna.
 * Tipos: clock, weather, trains, buses, home, agenda, music, widget.
 */
data class DModule(val id: Long, val type: String, val weight: Float = 1f, val widgetId: Int = -1)

/** Arrumação do modo Display: três colunas de mosaicos, guardada nas preferências. */
class DisplayLayout(ctx: Context) {
    private val sp = ctx.getSharedPreferences("display_layout", Context.MODE_PRIVATE)
    private var lastId = System.currentTimeMillis()

    var columns by mutableStateOf(load() ?: DEFAULT)
        private set

    companion object {
        val TYPES = linkedMapOf(
            "clock" to "Relógio", "weather" to "Tempo", "trains" to "Comboios", "buses" to "Autocarros",
            "home" to "Casa", "agenda" to "Agenda", "music" to "Música", "widget" to "Widget",
        )
        val WEIGHTS = listOf(0f, 1f, 1.5f, 2f)
        val DEFAULT = listOf(
            listOf(DModule(1, "clock", 0f), DModule(2, "weather", 1f)),
            listOf(DModule(3, "trains", 1f), DModule(4, "buses", 1f), DModule(5, "home", 0f)),
            listOf(DModule(6, "agenda", 1f), DModule(7, "music", 1f)),
        )
    }

    private fun load(): List<List<DModule>>? = runCatching {
        val raw = sp.getString("cols", null) ?: return null
        val a = JSONArray(raw)
        (0 until a.length()).map { c ->
            val col = a.getJSONArray(c)
            (0 until col.length()).map { i ->
                val o = col.getJSONObject(i)
                DModule(o.getLong("id"), o.getString("type"), o.optDouble("weight", 1.0).toFloat(), o.optInt("widgetId", -1))
            }
        }
    }.getOrNull()?.takeIf { it.size == 3 }

    private fun save(cols: List<List<DModule>>) {
        columns = cols
        val a = JSONArray()
        cols.forEach { col ->
            val c = JSONArray()
            col.forEach { m -> c.put(JSONObject().put("id", m.id).put("type", m.type).put("weight", m.weight.toDouble()).put("widgetId", m.widgetId)) }
            a.put(c)
        }
        sp.edit().putString("cols", a.toString()).apply()
    }

    private fun newId(): Long { lastId = maxOf(lastId + 1, System.currentTimeMillis()); return lastId }

    private fun locate(id: Long): Pair<Int, Int>? {
        columns.forEachIndexed { c, col -> col.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { return c to it } }
        return null
    }

    fun add(column: Int, type: String, widgetId: Int = -1) {
        val cols = columns.map { it.toMutableList() }
        cols[column.coerceIn(0, 2)].add(DModule(newId(), type, if (type == "clock" || type == "home") 0f else 1f, widgetId))
        save(cols)
    }

    fun remove(id: Long): DModule? {
        val (c, i) = locate(id) ?: return null
        val cols = columns.map { it.toMutableList() }
        val m = cols[c].removeAt(i)
        save(cols)
        return m
    }

    /** dy: -1 sobe, +1 desce; dx: -1 coluna à esquerda, +1 à direita. */
    fun move(id: Long, dx: Int, dy: Int) {
        val (c, i) = locate(id) ?: return
        val cols = columns.map { it.toMutableList() }
        if (dx != 0) {
            val nc = c + dx
            if (nc !in 0..2) return
            val m = cols[c].removeAt(i)
            cols[nc].add(i.coerceAtMost(cols[nc].size), m)
        } else {
            val ni = i + dy
            if (ni !in cols[c].indices) return
            val m = cols[c].removeAt(i)
            cols[c].add(ni, m)
        }
        save(cols)
    }

    fun cycleSize(id: Long) {
        val (c, i) = locate(id) ?: return
        val cols = columns.map { it.toMutableList() }
        val m = cols[c][i]
        val next = WEIGHTS[(WEIGHTS.indexOf(m.weight).coerceAtLeast(0) + 1) % WEIGHTS.size]
        cols[c][i] = m.copy(weight = next)
        save(cols)
    }

    fun reset() { save(DEFAULT) }

    fun has(type: String) = columns.any { col -> col.any { it.type == type } }
}
