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

    var dockSeeded: Boolean
        get() = sp.getBoolean("dockSeeded", false)
        set(v) { sp.edit().putBoolean("dockSeeded", v).apply() }
}
