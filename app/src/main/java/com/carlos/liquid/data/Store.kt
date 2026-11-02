package com.carlos.liquid.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/** Estado persistente do launcher (definições, disposição do ecrã, uso das apps). */
object Store {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dir: File

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings
    private val _layout = MutableStateFlow(HomeLayout())
    val layout: StateFlow<HomeLayout> = _layout
    private val _usage = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usage: StateFlow<Map<String, Int>> = _usage

    private var saveJobs = mutableMapOf<String, Job>()

    fun init(ctx: Context) {
        dir = ctx.filesDir
        runCatching { _settings.value = json.decodeFromString(Settings.serializer(), File(dir, "settings.json").readText()) }
        runCatching { _layout.value = json.decodeFromString(HomeLayout.serializer(), File(dir, "layout.json").readText()) }
        runCatching {
            _usage.value = File(dir, "usage.txt").readLines().mapNotNull { l ->
                val i = l.lastIndexOf('='); if (i <= 0) null else l.substring(0, i) to (l.substring(i + 1).toIntOrNull() ?: 0)
            }.toMap()
        }
    }

    fun reloadFromDisk() = init2()
    private fun init2() {
        runCatching { _settings.value = json.decodeFromString(Settings.serializer(), File(dir, "settings.json").readText()) }
        runCatching { _layout.value = json.decodeFromString(HomeLayout.serializer(), File(dir, "layout.json").readText()) }
    }

    val s: Settings get() = _settings.value
    val l: HomeLayout get() = _layout.value

    fun settings(f: (Settings) -> Settings) {
        _settings.value = f(_settings.value)
        save("settings.json") { json.encodeToString(Settings.serializer(), _settings.value) }
    }

    fun lock(f: (LockSettings) -> LockSettings) = settings { it.copy(lock = f(it.lock)) }

    fun layout(f: (HomeLayout) -> HomeLayout) {
        _layout.value = normalize(f(_layout.value), _settings.value)
        save("layout.json") { json.encodeToString(HomeLayout.serializer(), _layout.value) }
    }

    /** Muda a disposição sem normalizar (usado durante o arrastar). */
    fun layoutRaw(f: (HomeLayout) -> HomeLayout) {
        _layout.value = f(_layout.value)
    }

    fun commitLayout() = layout { it }

    fun recordLaunch(key: String) {
        _usage.value = _usage.value + (key to ((_usage.value[key] ?: 0) + 1))
        save("usage.txt") { _usage.value.entries.joinToString("\n") { "${it.key}=${it.value}" } }
    }

    fun exportAll(): String = json.encodeToString(Backup.serializer(), Backup(_settings.value, _layout.value))

    fun importAll(text: String): Boolean = runCatching {
        val b = json.decodeFromString(Backup.serializer(), text)
        settings { b.settings }
        layout { b.layout }
    }.isSuccess

    private fun save(name: String, content: () -> String) {
        saveJobs[name]?.cancel()
        saveJobs[name] = io.launch {
            delay(250)
            val text = content()
            val tmp = File(dir, "$name.tmp")
            tmp.writeText(text)
            tmp.renameTo(File(dir, name))
        }
    }

    /** Empurra o excesso de cada página para a seguinte e remove páginas vazias. */
    fun normalize(l: HomeLayout, s: Settings): HomeLayout {
        val pages = l.pages.map { it.items.toMutableList() to it }.toMutableList()
        var i = 0
        while (i < pages.size) {
            val (items, _) = pages[i]
            val res = GridMath.place(items, s.columns, s.rows)
            if (res.overflow.isNotEmpty()) {
                items.removeAll { o -> res.overflow.any { it.id == o.id } }
                if (i + 1 >= pages.size) pages.add(mutableListOf<HomeItem>() to Page())
                pages[i + 1].first.addAll(0, res.overflow)
            }
            i++
        }
        val out = pages.filter { it.first.isNotEmpty() }.map { (items, p) -> p.copy(items = items.toList()) }
        return l.copy(pages = out.ifEmpty { listOf(Page()) })
    }
}

@kotlinx.serialization.Serializable
data class Backup(val settings: Settings, val layout: HomeLayout)

data class Placed(val item: HomeItem, val col: Int, val row: Int, val w: Int, val h: Int)
data class PlaceResult(val placed: List<Placed>, val overflow: List<HomeItem>)

object GridMath {
    /** Colocação em fluxo, como no iOS: cada item vai para o primeiro lugar livre. */
    fun place(items: List<HomeItem>, cols: Int, rows: Int): PlaceResult {
        val occ = Array(rows) { BooleanArray(cols) }
        val placed = ArrayList<Placed>()
        val overflow = ArrayList<HomeItem>()
        var overflowing = false
        for (it in items) {
            if (overflowing) { overflow += it; continue }
            val (w, h) = it.span(cols)
            var done = false
            loop@ for (r in 0..rows - h) for (c in 0..cols - w) {
                if (free(occ, r, c, w, h)) {
                    for (rr in r until r + h) for (cc in c until c + w) occ[rr][cc] = true
                    placed += Placed(it, c, r, w, h); done = true; break@loop
                }
            }
            if (!done) { overflowing = true; overflow += it }
        }
        return PlaceResult(placed, overflow)
    }

    private fun free(o: Array<BooleanArray>, r: Int, c: Int, w: Int, h: Int): Boolean {
        for (rr in r until r + h) for (cc in c until c + w) if (o[rr][cc]) return false
        return true
    }
}
