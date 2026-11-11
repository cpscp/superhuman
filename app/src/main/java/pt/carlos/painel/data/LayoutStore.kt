package pt.carlos.painel.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Um elemento do ecrã inicial: uma app (1x1) ou um widget (w x h células). */
data class Item(
    val id: Long,
    val kind: Int,
    val page: Int,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val app: String = "",
    val widgetId: Int = -1,
) {
    companion object {
        const val APP = 0
        const val WIDGET = 1
    }
}

/** Páginas, posições e dock, guardados em JSON nas preferências. */
class LayoutStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("layout", Context.MODE_PRIVATE)
    val items = mutableStateListOf<Item>()
    val dock = mutableStateListOf<String>()
    var pages by mutableIntStateOf(1)
        private set
    private var lastId = System.currentTimeMillis()

    init { load() }

    private fun load() {
        val raw = sp.getString("layout", null) ?: return
        runCatching {
            val o = JSONObject(raw)
            pages = o.optInt("pages", 1).coerceAtLeast(1)
            val arr = o.optJSONArray("items") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val j = arr.getJSONObject(i)
                items.add(
                    Item(
                        id = j.getLong("id"), kind = j.getInt("kind"), page = j.getInt("page"),
                        x = j.getInt("x"), y = j.getInt("y"), w = j.getInt("w"), h = j.getInt("h"),
                        app = j.optString("app", ""), widgetId = j.optInt("widgetId", -1),
                    )
                )
            }
            val d = o.optJSONArray("dock") ?: JSONArray()
            for (i in 0 until d.length()) dock.add(d.getString(i))
        }
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("kind", it.kind).put("page", it.page)
                    .put("x", it.x).put("y", it.y).put("w", it.w).put("h", it.h)
                    .put("app", it.app).put("widgetId", it.widgetId)
            )
        }
        val o = JSONObject().put("pages", pages).put("items", arr).put("dock", JSONArray(dock.toList()))
        sp.edit().putString("layout", o.toString()).apply()
    }

    private fun newId(): Long { lastId = maxOf(lastId + 1, System.currentTimeMillis()); return lastId }

    private fun overlaps(a: Item, x: Int, y: Int, w: Int, h: Int) =
        a.x < x + w && x < a.x + a.w && a.y < y + h && y < a.y + a.h

    fun isFree(page: Int, x: Int, y: Int, w: Int, h: Int, cols: Int, rows: Int, ignore: Long = -1): Boolean =
        x >= 0 && y >= 0 && x + w <= cols && y + h <= rows &&
            items.none { it.id != ignore && it.page == page && overlaps(it, x, y, w, h) }

    private fun findFree(page: Int, w: Int, h: Int, cols: Int, rows: Int): Pair<Int, Int>? {
        for (y in 0..rows - h) for (x in 0..cols - w) if (isFree(page, x, y, w, h, cols, rows)) return x to y
        return null
    }

    /** Põe o elemento no primeiro espaço livre a partir da página indicada (cria página se for preciso). */
    fun add(kind: Int, app: String, widgetId: Int, w: Int, h: Int, page: Int, cols: Int, rows: Int): Item {
        val ww = w.coerceIn(1, cols)
        val hh = h.coerceIn(1, rows)
        var p = page.coerceAtLeast(0)
        while (true) {
            val spot = findFree(p, ww, hh, cols, rows)
            if (spot != null) {
                val item = Item(newId(), kind, p, spot.first, spot.second, ww, hh, app, widgetId)
                items.add(item)
                if (p >= pages) pages = p + 1
                save()
                return item
            }
            p++
        }
    }

    fun update(item: Item) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) { items[i] = item; save() }
    }

    fun remove(id: Long) { items.removeAll { it.id == id }; save() }

    fun addPage(): Int { pages += 1; save(); return pages - 1 }

    /** Tira as páginas vazias (fica sempre pelo menos uma). */
    fun trimPages() {
        val used = items.map { it.page }.toSortedSet()
        val keep = (0 until pages).filter { it == 0 || it in used }
        val remap = keep.withIndex().associate { (newIdx, old) -> old to newIdx }
        for (i in items.indices) items[i] = items[i].copy(page = remap[items[i].page] ?: 0)
        pages = keep.size.coerceAtLeast(1)
        save()
    }

    fun inDock(key: String) = key in dock
    fun dockAdd(key: String) { if (key !in dock && dock.size < 7) { dock.add(key); save() } }
    fun dockRemove(key: String) { dock.remove(key); save() }
}
