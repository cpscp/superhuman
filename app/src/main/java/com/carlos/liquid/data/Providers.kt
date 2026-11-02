package com.carlos.liquid.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.provider.CalendarContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val b = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Linux; Android 15; Pixel 9a) Liquid/1.0")
        headers.forEach { (k, v) -> b.header(k, v) }
        client.newCall(b.build()).execute().use { r ->
            val body = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            body
        }
    }
}

// ============================ Meteorologia ============================

data class Hour(val time: String, val temp: Int, val code: Int, val day: Boolean)
data class Day(val name: String, val code: Int, val min: Int, val max: Int)
data class Weather(val city: String, val temp: Int, val code: Int, val isDay: Boolean, val min: Int, val max: Int, val hours: List<Hour>, val days: List<Day>, val fetched: Long)

object WeatherRepo {
    val state = MutableStateFlow<Weather?>(null)
    private var last = 0L

    fun describe(code: Int): String = when (code) {
        0 -> "Limpo"; 1 -> "Pouco nublado"; 2 -> "Parcialmente nublado"; 3 -> "Nublado"
        45, 48 -> "Nevoeiro"; 51, 53, 55 -> "Chuvisco"; 56, 57 -> "Chuvisco gelado"
        61, 63 -> "Chuva"; 65 -> "Chuva forte"; 66, 67 -> "Chuva gelada"
        71, 73, 75, 77 -> "Neve"; 80, 81 -> "Aguaceiros"; 82 -> "Aguaceiros fortes"
        85, 86 -> "Neve"; 95, 96, 99 -> "Trovoada"; else -> "—"
    }

    @SuppressLint("MissingPermission")
    suspend fun refresh(ctx: Context, force: Boolean = false) {
        if (!force && System.currentTimeMillis() - last < 20 * 60_000 && state.value != null) return
        val s = Store.s
        var lat = s.lat; var lon = s.lon; var city = s.cityName
        if (s.useLocation && ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lm = ctx.getSystemService(LocationManager::class.java)
            val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
            if (loc != null) {
                lat = loc.latitude; lon = loc.longitude
                city = runCatching {
                    val j = JSONObject(Http.get("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=pt"))
                    j.optString("city").ifBlank { j.optString("locality") }.ifBlank { city }
                }.getOrDefault(city)
            }
        }
        runCatching {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m,weather_code,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=7"
            val j = JSONObject(Http.get(url))
            val cur = j.getJSONObject("current")
            val h = j.getJSONObject("hourly")
            val ht = h.getJSONArray("time"); val htemp = h.getJSONArray("temperature_2m"); val hc = h.getJSONArray("weather_code"); val hd = h.getJSONArray("is_day")
            val nowStr = cur.getString("time").take(13)
            var start = 0
            for (i in 0 until ht.length()) if (ht.getString(i).take(13) >= nowStr) { start = i; break }
            val hours = (start until minOf(start + 6, ht.length())).mapIndexed { idx, i ->
                Hour(if (idx == 0) "Agora" else ht.getString(i).substring(11, 13) + "h", htemp.getDouble(i).toInt(), hc.getInt(i), hd.getInt(i) == 1)
            }
            val d = j.getJSONObject("daily")
            val dt = d.getJSONArray("time"); val dc = d.getJSONArray("weather_code"); val dmax = d.getJSONArray("temperature_2m_max"); val dmin = d.getJSONArray("temperature_2m_min")
            val names = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
            val days = (0 until dt.length()).map { i ->
                val date = LocalDate.parse(dt.getString(i))
                Day(if (i == 0) "Hoje" else names[date.dayOfWeek.value - 1], dc.getInt(i), Math.round(dmin.getDouble(i)).toInt(), Math.round(dmax.getDouble(i)).toInt())
            }
            state.value = Weather(city, Math.round(cur.getDouble("temperature_2m")).toInt(), cur.getInt("weather_code"), cur.getInt("is_day") == 1,
                days.firstOrNull()?.min ?: 0, days.firstOrNull()?.max ?: 0, hours, days, System.currentTimeMillis())
            last = System.currentTimeMillis()
        }
    }

    suspend fun searchCity(q: String): List<Triple<String, Double, Double>> = runCatching {
        val j = JSONObject(Http.get("https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(q, "UTF-8")}&count=8&language=pt"))
        val r = j.optJSONArray("results") ?: JSONArray()
        (0 until r.length()).map { i -> val o = r.getJSONObject(i)
            Triple(o.getString("name") + (o.optString("admin1").let { if (it.isNotBlank()) ", $it" else "" }), o.getDouble("latitude"), o.getDouble("longitude")) }
    }.getOrDefault(emptyList())
}

// ============================ Calendário ============================

data class CalEvent(val title: String, val start: Long, val end: Long, val allDay: Boolean, val color: Int, val location: String)

object CalendarRepo {
    val state = MutableStateFlow<List<CalEvent>>(emptyList())

    fun granted(ctx: Context) = ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh(ctx: Context) = withContext(Dispatchers.IO) {
        if (!granted(ctx)) return@withContext
        runCatching {
            val now = System.currentTimeMillis()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(b, startOfDay); ContentUris.appendId(b, now + 8L * 86_400_000)
            val proj = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.DISPLAY_COLOR, CalendarContract.Instances.EVENT_LOCATION)
            val list = ArrayList<CalEvent>()
            ctx.contentResolver.query(b.build(), proj, null, null, CalendarContract.Instances.BEGIN + " ASC")?.use { c ->
                while (c.moveToNext()) {
                    val e = CalEvent(c.getString(0) ?: "(Sem título)", c.getLong(1), c.getLong(2), c.getInt(3) == 1, c.getInt(4), c.getString(5) ?: "")
                    if (e.end >= now || (e.allDay && e.start >= startOfDay - 86_400_000)) list += e
                }
            }
            state.value = list
        }
    }

    fun nextAlarm(ctx: Context): Long? = runCatching { ctx.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime }.getOrNull()
}

// ============================ Saúde (Health Connect) ============================

data class Health(
    val steps: Long = 0, val kcal: Double = 0.0, val exerciseMin: Long = 0, val distanceKm: Double = 0.0,
    val steps7: List<Long> = emptyList(), val sleepMin: Long = 0, val ok: Boolean = false,
)

object HealthRepo {
    val state = MutableStateFlow(Health())
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    fun available(ctx: Context) = runCatching { HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE }.getOrDefault(false)

    suspend fun granted(ctx: Context): Boolean = runCatching {
        HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions().containsAll(permissions)
    }.getOrDefault(false)

    suspend fun refresh(ctx: Context) {
        if (!available(ctx)) return
        runCatching {
            val client = HealthConnectClient.getOrCreate(ctx)
            val granted = client.permissionController.getGrantedPermissions()
            if (granted.isEmpty()) return
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val start = today.atStartOfDay(zone).toInstant()
            val now = Instant.now()
            val agg = client.aggregate(AggregateRequest(
                setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                TimeRangeFilter.between(start, now)))
            val weekly = runCatching {
                client.aggregateGroupByPeriod(AggregateGroupByPeriodRequest(setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()), Period.ofDays(1)))
                    .map { it.result[StepsRecord.COUNT_TOTAL] ?: 0L }
            }.getOrDefault(emptyList())
            val ex = runCatching {
                client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, now))).records
                    .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            }.getOrDefault(0L)
            val sleep = runCatching {
                client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start.minus(Duration.ofHours(14)), now))).records
                    .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            }.getOrDefault(0L)
            state.value = Health(
                agg[StepsRecord.COUNT_TOTAL] ?: 0L,
                agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0,
                ex, agg[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0, weekly, sleep, true,
            )
        }
    }
}

// ============================ Bateria ============================

data class BatteryDevice(val name: String, val level: Int, val charging: Boolean, val kind: String)

object BatteryRepo {
    val state = MutableStateFlow<List<BatteryDevice>>(emptyList())

    @SuppressLint("MissingPermission")
    fun refresh(ctx: Context) {
        val list = ArrayList<BatteryDevice>()
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (i != null) {
            val lvl = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, 0)
            list += BatteryDevice("Pixel", lvl, st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL, "phone")
        }
        if (ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) runCatching {
            val bm = ctx.getSystemService(BluetoothManager::class.java)
            bm.adapter?.bondedDevices?.forEach { d ->
                val connected = runCatching { d.javaClass.getMethod("isConnected").invoke(d) as Boolean }.getOrDefault(false)
                if (connected) {
                    val lvl = runCatching { d.javaClass.getMethod("getBatteryLevel").invoke(d) as Int }.getOrDefault(-1)
                    if (lvl in 0..100) {
                        val n = d.name ?: "Dispositivo"
                        val kind = when {
                            n.contains("watch", true) || n.contains("band", true) -> "watch"
                            d.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> "headphones"
                            else -> "bt"
                        }
                        list += BatteryDevice(n, lvl, false, kind)
                    }
                }
            }
        }
        state.value = list
    }

    @Suppress("unused") private fun unused(d: BluetoothDevice) = d
}

// ============================ Transportes (Carris Metropolitana + CP) ============================

data class Departure(val line: String, val color: Long, val dest: String, val epoch: Long, val live: Boolean, val platform: String = "", val delay: Int = 0)
data class Stop(val id: String, val name: String, val extra: String = "")

object TransitRepo {
    private val cache = HashMap<String, Pair<Long, List<Departure>>>()
    private var carrisStops: List<Stop>? = null
    private var carrisLines: Map<String, Long>? = null

    private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    suspend fun departures(mode: String, id: String, line: String = ""): List<Departure> {
        val key = "$mode:$id:$line"
        cache[key]?.let { if (System.currentTimeMillis() - it.first < 25_000) return it.second }
        val r = runCatching { if (mode == "cp") cp(id) else carris(id) }.getOrElse { cache[key]?.second ?: throw it }
        val filtered = if (line.isBlank()) r else r.filter { it.line.equals(line, true) }
        cache[key] = System.currentTimeMillis() to filtered
        return filtered
    }

    private suspend fun lineColors(): Map<String, Long> {
        carrisLines?.let { return it }
        val arr = JSONArray(Http.get("https://api.carrismetropolitana.pt/v2/lines"))
        val m = HashMap<String, Long>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val c = o.optString("color").removePrefix("#")
            m[o.optString("id")] = (c.toLongOrNull(16) ?: 0xED1944L) or 0xFF000000L
        }
        carrisLines = m
        return m
    }

    private suspend fun carris(stopId: String): List<Departure> {
        val colors = runCatching { lineColors() }.getOrDefault(emptyMap())
        val arr = JSONArray(Http.get("https://api.carrismetropolitana.pt/v2/arrivals/by_stop/$stopId"))
        val now = System.currentTimeMillis() / 1000
        val out = ArrayList<Departure>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            if (!a.isNull("observed_arrival_unix")) continue
            val est = if (a.isNull("estimated_arrival_unix")) 0L else a.optLong("estimated_arrival_unix")
            val sch = if (a.isNull("scheduled_arrival_unix")) 0L else a.optLong("scheduled_arrival_unix")
            val t = if (est > 0) est else sch
            if (t <= 0 || t < now - 60) continue
            val line = a.optString("line_id")
            out += Departure(line, colors[line] ?: 0xFFED1944, a.optString("headsign"), t, est > 0, delay = if (est > 0 && sch > 0) ((est - sch) / 60).toInt() else 0)
        }
        return out.sortedBy { it.epoch }.take(12)
    }

    private suspend fun cp(station: String): List<Departure> {
        val clean = station.filter { it.isDigit() }
        val formatted = if (clean.length > 2) clean.substring(0, 2) + "-" + clean.substring(2) else clean
        val zone = ZoneId.of("Europe/Lisbon")
        val today = LocalDate.now(zone)
        fun epochOf(hhmm: String): Long {
            val p = hhmm.trim().split(":"); if (p.size < 2) return 0
            var t = today.atTime(p[0].toInt() % 24, p[1].toInt()).atZone(zone).toEpochSecond()
            if (t < System.currentTimeMillis() / 1000 - 6 * 3600) t += 86400
            return t
        }
        // 1) API usada pelo site da CP
        val viaCp = runCatching {
            val arr = JSONArray(Http.get("https://www.cp.pt/sites/spring/station/trains?stationId=$formatted", mapOf("Accept" to "application/json")))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val dep = o.optString("departureTime").ifBlank { return@mapNotNull null }
                val etd = o.optString("etd")
                val dest = o.optJSONObject("trainDestination")?.optString("designation").orEmpty()
                val service = o.optJSONObject("trainService")?.optString("designation").orEmpty()
                val delay = if (o.isNull("delay")) 0 else o.optInt("delay")
                val t = epochOf(if (etd.isNotBlank() && etd.contains(":")) etd else dep)
                Departure(service.ifBlank { o.optString("trainNumber") }, 0xFF2E7D32, dest, t, etd.isNotBlank() || delay != 0, o.optString("platform"), delay)
            }
        }.getOrNull()
        if (!viaCp.isNullOrEmpty()) return viaCp.filter { it.epoch >= System.currentTimeMillis() / 1000 - 60 }.sortedBy { it.epoch }.take(12)
        // 2) Alternativa: Infraestruturas de Portugal
        val now = java.time.LocalDateTime.now(zone)
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val from = java.net.URLEncoder.encode(now.format(fmt), "UTF-8").replace("+", "%20")
        val to = java.net.URLEncoder.encode(now.plusHours(3).format(fmt), "UTF-8").replace("+", "%20")
        val types = "INTERNACIONAL,%20ALFA,%20IC,%20IR,%20REGIONAL,%20URB%7CSUBUR,%20ESPECIAL"
        val j = JSONObject(Http.get("https://www.infraestruturasdeportugal.pt/negocios-e-servicos/partidas-chegadas/$clean/$from/$to/$types"))
        val resp = j.optJSONArray("response") ?: return emptyList()
        val out = ArrayList<Departure>()
        for (i in 0 until resp.length()) {
            val passes = resp.getJSONObject(i).optJSONArray("NodesPassagemComboio") ?: continue
            for (k in 0 until passes.length()) {
                val o = passes.getJSONObject(k)
                if (o.optBoolean("ComboioPassou")) continue
                val hora = o.optString("DataHoraPartidaChegada").let { if (it.length >= 16) it.substring(11, 16) else it.take(5) }
                val obs = o.optString("Observacoes")
                val delay = Regex("(\\d+)").find(obs)?.value?.toIntOrNull()?.takeIf { obs.contains("atraso", true) } ?: 0
                out += Departure(o.optString("TipoServico").ifBlank { o.optString("NComboio1") }, 0xFF2E7D32,
                    o.optString("NomeEstacaoDestino"), epochOf(hora) + delay * 60, delay > 0, delay = delay)
            }
        }
        return out.sortedBy { it.epoch }.take(12)
    }

    // ---- pesquisa de paragens / estações ----
    suspend fun searchCarris(ctx: Context, q: String): List<Stop> {
        val stops = carrisStops ?: loadCarrisStops(ctx).also { carrisStops = it }
        val n = norm(q.trim())
        if (n.isBlank()) return emptyList()
        if (n.all { it.isDigit() }) return stops.filter { it.id.contains(n) }.take(40)
        val words = n.split(" ").filter { it.isNotBlank() }
        return stops.filter { s -> val sn = norm(s.name + " " + s.extra); words.all { sn.contains(it) } }.take(40)
    }

    private suspend fun loadCarrisStops(ctx: Context): List<Stop> = withContext(Dispatchers.IO) {
        val f = File(ctx.cacheDir, "carris_stops.json")
        val text = if (f.exists() && System.currentTimeMillis() - f.lastModified() < 7L * 86_400_000) f.readText()
        else Http.get("https://api.carrismetropolitana.pt/v2/stops").also { f.writeText(it) }
        val arr = JSONArray(text)
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
            val lines = o.optJSONArray("line_ids")
            Stop(o.optString("id"), o.optString("long_name"), buildString {
                append(o.optString("locality").ifBlank { o.optString("municipality_name") })
                if (lines != null && lines.length() > 0) { append(" · "); append((0 until minOf(lines.length(), 6)).joinToString(" ") { lines.getString(it) }) }
            })
        }
    }

    suspend fun searchCp(q: String): List<Stop> = runCatching {
        val j = JSONObject(Http.get("https://www.infraestruturasdeportugal.pt/negocios-e-servicos/estacao-nome/" + java.net.URLEncoder.encode(q.trim(), "UTF-8").replace("+", "%20")))
        val r = j.optJSONArray("response") ?: JSONArray()
        (0 until r.length()).map { i -> val o = r.getJSONObject(i); Stop(o.opt("NodeID").toString(), o.optString("Nome"), "CP") }
    }.getOrDefault(emptyList())
}
