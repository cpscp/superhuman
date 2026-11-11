package pt.carlos.painel.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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
    /** Pasta: apps lá dentro e nome. */
    val folderApps: List<String> = emptyList(),
    val name: String = "",
) {
    companion object {
        const val APP = 0
        const val WIDGET = 1
        const val FOLDER = 2
    }
}

/**
 * Páginas, posições e dock, guardados em JSON nas preferências.
 * Há uma arrumação para a horizontal e outra para a vertical; a grelha (colunas × linhas)
 * é calculada a partir do tamanho do ecrã em cada orientação.
 */
class LayoutStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("layout", Context.MODE_PRIVATE)
    private val land = mutableStateListOf<Item>()
    private val port = mutableStateListOf<Item>()
    private var landPages by mutableIntStateOf(1)
    private var portPages by mutableIntStateOf(1)
    private var landCopied = false
    private var portCopied = false
    val dock = mutableStateListOf<String>()

    /** Orientação em uso; muda a arrumação mostrada. */
    var landscape by mutableStateOf(true)
        private set
    /** Grelha atual (calculada pelo ecrã). */
    var cols by mutableIntStateOf(6)
        private set
    var rows by mutableIntStateOf(4)
        private set

    val items: SnapshotStateList<Item> get() = if (landscape) land else port
    var pages: Int
        get() = if (landscape) landPages else portPages
        private set(v) { if (landscape) landPages = v else portPages = v }

    private var lastId = System.currentTimeMillis()

    init { load() }

    private fun readItems(arr: JSONArray?, into: MutableList<Item>) {
        if (arr == null) return
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            into.add(
                Item(
                    id = j.getLong("id"), kind = j.getInt("kind"), page = j.getInt("page"),
                    x = j.getInt("x"), y = j.getInt("y"), w = j.getInt("w"), h = j.getInt("h"),
                    app = j.optString("app", ""), widgetId = j.optInt("widgetId", -1),
                    folderApps = j.optJSONArray("apps")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                    name = j.optString("name", ""),
                )
            )
        }
    }

    private fun writeItems(list: List<Item>) = JSONArray().also { arr ->
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("kind", it.kind).put("page", it.page)
                    .put("x", it.x).put("y", it.y).put("w", it.w).put("h", it.h)
                    .put("app", it.app).put("widgetId", it.widgetId)
                    .put("apps", JSONArray(it.folderApps)).put("name", it.name)
            )
        }
    }

    private fun load() {
        val raw = sp.getString("layout", null) ?: return
        runCatching {
            val o = JSONObject(raw)
            if (o.has("land")) {
                val l = o.getJSONObject("land"); val p = o.getJSONObject("port")
                landPages = l.optInt("pages", 1).coerceAtLeast(1); readItems(l.optJSONArray("items"), land)
                portPages = p.optInt("pages", 1).coerceAtLeast(1); readItems(p.optJSONArray("items"), port)
                landCopied = o.optBoolean("landCopied", false); portCopied = o.optBoolean("portCopied", false)
            } else {
                // Formato antigo (uma só arrumação): passa a ser a da horizontal.
                landPages = o.optInt("pages", 1).coerceAtLeast(1)
                readItems(o.optJSONArray("items"), land)
            }
            val d = o.optJSONArray("dock") ?: JSONArray()
            for (i in 0 until d.length()) dock.add(d.getString(i))
        }
    }

    private fun save() {
        val o = JSONObject()
            .put("land", JSONObject().put("pages", landPages).put("items", writeItems(land)))
            .put("port", JSONObject().put("pages", portPages).put("items", writeItems(port)))
            .put("landCopied", landCopied).put("portCopied", portCopied)
            .put("dock", JSONArray(dock.toList()))
        sp.edit().putString("layout", o.toString()).apply()
    }

    /**
     * Chamado pelo ecrã com a orientação e a grelha calculada. Na primeira vez numa orientação vazia,
     * copia a arrumação da outra; depois arruma o que não couber (fora da grelha ou sobreposto).
     */
    fun setGrid(isLandscape: Boolean, c: Int, r: Int) {
        landscape = isLandscape
        cols = c; rows = r
        val (target, other) = if (isLandscape) land to port else port to land
        val copied = if (isLandscape) landCopied else portCopied
        if (target.isEmpty() && other.isNotEmpty() && !copied) {
            other.forEach { target.add(it.copy(id = newId())) }
            pages = (if (isLandscape) portPages else landPages)
        }
        if (isLandscape) landCopied = true else portCopied = true
        relayout()
        save()
    }

    private fun relayout() {
        val placed = ArrayList<Item>()
        val sorted = items.sortedWith(compareBy({ it.page }, { it.y }, { it.x }))
        fun fits(it: Item) = it.x >= 0 && it.y >= 0 && it.x + it.w <= cols && it.y + it.h <= rows &&
            placed.none { o -> o.page == it.page && overlaps(o, it.x, it.y, it.w, it.h) }
        var maxPage = 0
        for (orig in sorted) {
            var it = orig.copy(w = orig.w.coerceIn(1, cols), h = orig.h.coerceIn(1, rows))
            if (!fits(it)) {
                var p = it.page
                var spot: Pair<Int, Int>? = null
                while (spot == null) {
                    for (y in 0..rows - it.h) { for (x in 0..cols - it.w) {
                        val cand = it.copy(page = p, x = x, y = y)
                        if (spot == null && fits(cand)) spot = x to y
                    } }
                    if (spot == null) p++
                }
                val s2 = spot!!
                it = it.copy(page = p, x = s2.first, y = s2.second)
            }
            placed.add(it)
            maxPage = maxOf(maxPage, it.page)
        }
        if (placed != items.toList()) {
            items.clear(); items.addAll(placed)
        }
        if (maxPage + 1 > pages) pages = maxPage + 1
    }

    /** O mesmo widget pode estar nas duas arrumações: só se apaga quando nenhuma o usa. */
    fun usesWidget(widgetId: Int) = land.any { it.widgetId == widgetId } || port.any { it.widgetId == widgetId }

    private fun newId(): Long { lastId = maxOf(lastId + 1, System.currentTimeMillis()); return lastId }

    private fun overlaps(a: Item, x: Int, y: Int, w: Int, h: Int) =
        a.x < x + w && x < a.x + a.w && a.y < y + h && y < a.y + a.h

    fun isFree(page: Int, x: Int, y: Int, w: Int, h: Int, cols: Int = this.cols, rows: Int = this.rows, ignore: Long = -1): Boolean =
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

    /** Elemento que ocupa a célula (x, y) da página, se houver. */
    fun itemAt(page: Int, x: Int, y: Int, ignore: Long = -1): Item? =
        items.firstOrNull { it.id != ignore && it.page == page && x >= it.x && x < it.x + it.w && y >= it.y && y < it.y + it.h }

    // ---- Pastas ----

    /** Largar uma app em cima de outra cria uma pasta no lugar da segunda. */
    fun createFolder(target: Item, dragged: Item) {
        val i = items.indexOfFirst { it.id == target.id }
        if (i < 0) return
        items[i] = target.copy(kind = Item.FOLDER, app = "", folderApps = listOf(target.app, dragged.app), name = "Pasta")
        items.removeAll { it.id == dragged.id }
        save()
    }

    fun addToFolder(folderId: Long, key: String, removeItemId: Long? = null) {
        val i = items.indexOfFirst { it.id == folderId }
        if (i < 0) return
        val f = items[i]
        if (key !in f.folderApps) items[i] = f.copy(folderApps = f.folderApps + key)
        if (removeItemId != null) items.removeAll { it.id == removeItemId }
        save()
    }

    /** Tirar uma app da pasta: volta ao ecrã; uma pasta com uma só app passa a ser essa app. */
    fun removeFromFolder(folderId: Long, key: String, cols: Int, rows: Int) {
        val i = items.indexOfFirst { it.id == folderId }
        if (i < 0) return
        val f = items[i]
        val rest = f.folderApps - key
        when (rest.size) {
            0 -> items.removeAt(i)
            1 -> items[i] = f.copy(kind = Item.APP, app = rest.first(), folderApps = emptyList(), name = "")
            else -> items[i] = f.copy(folderApps = rest)
        }
        save()
        add(Item.APP, key, -1, 1, 1, f.page, cols, rows)
    }

    fun renameFolder(folderId: Long, name: String) {
        val i = items.indexOfFirst { it.id == folderId }
        if (i >= 0) { items[i] = items[i].copy(name = name.ifBlank { "Pasta" }); save() }
    }

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
