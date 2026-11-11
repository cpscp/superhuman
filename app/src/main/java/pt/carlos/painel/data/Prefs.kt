package pt.carlos.painel.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Definições do Painel: cada valor é estado Compose (o ecrã muda logo) e fica guardado. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("painel", Context.MODE_PRIVATE)

    private var _themeMode by mutableStateOf(sp.getInt("themeMode", 0))
    /** 0 = sistema, 1 = claro, 2 = escuro */
    var themeMode: Int
        get() = _themeMode
        set(v) { _themeMode = v; sp.edit().putInt("themeMode", v).apply() }

    private var _dynamicColor by mutableStateOf(sp.getBoolean("dynamicColor", true))
    var dynamicColor: Boolean
        get() = _dynamicColor
        set(v) { _dynamicColor = v; sp.edit().putBoolean("dynamicColor", v).apply() }

    private var _seed by mutableStateOf(sp.getInt("seed", 0xFF1A73E8.toInt()))
    var seed: Int
        get() = _seed
        set(v) { _seed = v; sp.edit().putInt("seed", v).apply() }

    private var _columns by mutableStateOf(sp.getInt("columns", 6))
    var columns: Int
        get() = _columns
        set(v) { _columns = v; sp.edit().putInt("columns", v).apply() }

    private var _rows by mutableStateOf(sp.getInt("rows", 4))
    var rows: Int
        get() = _rows
        set(v) { _rows = v; sp.edit().putInt("rows", v).apply() }

    private var _showLabels by mutableStateOf(sp.getBoolean("showLabels", true))
    var showLabels: Boolean
        get() = _showLabels
        set(v) { _showLabels = v; sp.edit().putBoolean("showLabels", v).apply() }

    private var _showGlance by mutableStateOf(sp.getBoolean("showGlance", true))
    var showGlance: Boolean
        get() = _showGlance
        set(v) { _showGlance = v; sp.edit().putBoolean("showGlance", v).apply() }

    private var _hiddenApps by mutableStateOf(sp.getStringSet("hiddenApps", emptySet())!!.toSet())
    val hiddenApps: Set<String> get() = _hiddenApps
    fun setHidden(key: String, hidden: Boolean) {
        _hiddenApps = if (hidden) _hiddenApps + key else _hiddenApps - key
        sp.edit().putStringSet("hiddenApps", _hiddenApps).apply()
    }


    private var _iconShape by mutableStateOf(sp.getInt("iconShape", 0))
    /** 0 = círculo (Pixel), 1 = squircle, 2 = quadrado arredondado */
    var iconShape: Int
        get() = _iconShape
        set(v) { _iconShape = v; sp.edit().putInt("iconShape", v).apply() }

    private var _themedIcons by mutableStateOf(sp.getBoolean("themedIcons", false))
    var themedIcons: Boolean
        get() = _themedIcons
        set(v) { _themedIcons = v; sp.edit().putBoolean("themedIcons", v).apply() }

    private var _lockScreen by mutableStateOf(sp.getBoolean("lockScreen", false))
    var lockScreen: Boolean
        get() = _lockScreen
        set(v) { _lockScreen = v; sp.edit().putBoolean("lockScreen", v).apply() }

    private var _clockStyle by mutableStateOf(sp.getInt("clockStyle", 0))
    /** 0 = grande (duas linhas), 1 = fino, 2 = forte */
    var clockStyle: Int
        get() = _clockStyle
        set(v) { _clockStyle = v; sp.edit().putInt("clockStyle", v).apply() }

    private var _labels by mutableStateOf(readLabels())
    val labelOverrides: Map<String, String> get() = _labels
    private fun readLabels(): Map<String, String> = runCatching {
        val o = org.json.JSONObject(sp.getString("labels", "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())
    fun setLabel(key: String, label: String?) {
        _labels = if (label.isNullOrBlank()) _labels - key else _labels + (key to label)
        sp.edit().putString("labels", org.json.JSONObject(_labels).toString()).apply()
    }

    var dockSeeded: Boolean
        get() = sp.getBoolean("dockSeeded", false)
        set(v) { sp.edit().putBoolean("dockSeeded", v).apply() }
}
