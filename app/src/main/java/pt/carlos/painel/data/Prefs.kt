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


    // ---- Modo Display ----
    private fun str(key: String, def: String) = mutableStateOf(sp.getString(key, def) ?: def)
    private fun putStr(key: String, v: String) = sp.edit().putString(key, v).apply()

    private var _trainStation by str("trainStation", "Alcântara-Mar")
    var trainStation: String
        get() = _trainStation
        set(v) { _trainStation = v; putStr("trainStation", v) }

    private var _trainDirection by str("trainDirection", "Cascais")
    var trainDirection: String
        get() = _trainDirection
        set(v) { _trainDirection = v; putStr("trainDirection", v) }

    private var _busStops by str("busStops", "10210, 10209, 10203, 10202, 10201")
    /** Paragens da Carris Metropolitana, separadas por vírgulas. */
    var busStops: String
        get() = _busStops
        set(v) { _busStops = v; putStr("busStops", v) }

    private var _busTitle by str("busTitle", "Alcântara")
    var busTitle: String
        get() = _busTitle
        set(v) { _busTitle = v; putStr("busTitle", v) }

    private var _weatherPlace by str("weatherPlace", "Lisboa")
    val weatherPlace: String get() = _weatherPlace
    private var _weatherLat by str("weatherLat", "38.7069")
    val weatherLat: Double get() = _weatherLat.toDoubleOrNull() ?: 38.7069
    private var _weatherLon by str("weatherLon", "-9.1766")
    val weatherLon: Double get() = _weatherLon.toDoubleOrNull() ?: -9.1766
    fun setWeatherPlace(name: String, lat: Double, lon: Double) {
        _weatherPlace = name; _weatherLat = lat.toString(); _weatherLon = lon.toString()
        sp.edit().putString("weatherPlace", name).putString("weatherLat", lat.toString()).putString("weatherLon", lon.toString()).apply()
    }

    private var _hiddenCalendars by mutableStateOf(sp.getStringSet("hiddenCalendars", emptySet())!!.toSet())
    val hiddenCalendars: Set<String> get() = _hiddenCalendars
    fun setCalendarHidden(id: Long, hidden: Boolean) {
        _hiddenCalendars = if (hidden) _hiddenCalendars + id.toString() else _hiddenCalendars - id.toString()
        sp.edit().putStringSet("hiddenCalendars", _hiddenCalendars).apply()
    }

    private var _nightAuto by mutableStateOf(sp.getBoolean("nightAuto", true))
    var nightAuto: Boolean
        get() = _nightAuto
        set(v) { _nightAuto = v; sp.edit().putBoolean("nightAuto", v).apply() }

    private var _nightStart by mutableStateOf(sp.getInt("nightStart", 23))
    var nightStart: Int
        get() = _nightStart
        set(v) { _nightStart = v; sp.edit().putInt("nightStart", v).apply() }

    private var _nightEnd by mutableStateOf(sp.getInt("nightEnd", 7))
    var nightEnd: Int
        get() = _nightEnd
        set(v) { _nightEnd = v; sp.edit().putInt("nightEnd", v).apply() }

    fun isNight(hour: Int): Boolean =
        nightAuto && (if (nightStart > nightEnd) hour >= nightStart || hour < nightEnd else hour in nightStart until nightEnd)

    var dockSeeded: Boolean
        get() = sp.getBoolean("dockSeeded", false)
        set(v) { sp.edit().putBoolean("dockSeeded", v).apply() }
}
