package com.carlos.liquid.ui.widgets

import android.Manifest
import android.appwidget.AppWidgetProviderInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.carlos.liquid.data.BatteryRepo
import com.carlos.liquid.data.CalendarRepo
import com.carlos.liquid.data.HealthRepo
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.Stop
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.TransitRepo
import com.carlos.liquid.data.WeatherRepo
import com.carlos.liquid.data.WidgetKind
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalFont
import com.carlos.liquid.ui.LocalPalette
import com.carlos.liquid.ui.Segmented
import com.carlos.liquid.ui.SheetTitle
import com.carlos.liquid.ui.ToggleRow
import com.carlos.liquid.ui.noRippleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun widgetIcon(k: WidgetKind): Pair<ImageVector, Color> = when (k) {
    WidgetKind.CLOCK_ANALOG -> Icons.Rounded.Schedule to Color(0xFF1C1C1E)
    WidgetKind.CLOCK_DIGITAL -> Icons.Rounded.AccessTime to Color(0xFF1C1C1E)
    WidgetKind.WORLD_CLOCK -> Icons.Rounded.Public to Color(0xFF1C1C1E)
    WidgetKind.CALENDAR -> Icons.Rounded.CalendarMonth to Color(0xFFFF3B30)
    WidgetKind.WEATHER -> Icons.Rounded.WbSunny to Color(0xFF0A84FF)
    WidgetKind.ACTIVITY -> Icons.Rounded.LocalFireDepartment to Color(0xFFFA114F)
    WidgetKind.STEPS -> Icons.Rounded.Favorite to Color(0xFFFF2D55)
    WidgetKind.BATTERY -> Icons.Rounded.BatteryFull to Color(0xFF34C759)
    WidgetKind.TRANSIT -> Icons.Rounded.DirectionsBus to Color(0xFFE5A800)
    WidgetKind.PHOTOS -> Icons.Rounded.PhotoLibrary to Color(0xFFFF9500)
    WidgetKind.ANDROID -> Icons.Rounded.Widgets to Color(0xFF8E8E93)
}

data class WidgetDims(val smallW: Dp, val smallH: Dp, val wideW: Dp, val largeH: Dp) {
    fun of(s: WidgetSize) = when (s) { WidgetSize.SMALL -> smallW to smallH; WidgetSize.MEDIUM -> wideW to smallH; WidgetSize.LARGE -> wideW to largeH }
}

fun updateWidget(id: String, f: (HomeItem.Widget) -> HomeItem.Widget) = Store.layout { l ->
    fun map(list: List<HomeItem>) = list.map { if (it.id == id && it is HomeItem.Widget) f(it) else it }
    l.copy(pages = l.pages.map { it.copy(items = map(it.items)) }, today = l.today.map { if (it.id == id) f(it) else it })
}

fun findWidget(id: String): HomeItem.Widget? {
    val l = Store.l
    return (l.pages.flatMap { it.items } + l.today).firstOrNull { it.id == id } as? HomeItem.Widget
}

/** Pré-visualização à escala. */
@Composable
fun WidgetPreview(item: HomeItem.Widget, dims: WidgetDims, maxW: Dp) {
    val (w, h) = dims.of(item.size)
    val scale = (maxW / w).coerceAtMost(1f)
    Box(Modifier.size(w * scale, h * scale), contentAlignment = Alignment.Center) {
        Box(Modifier.requiredSize(w, h).graphicsLayer { scaleX = scale; scaleY = scale }) { WidgetView(item, w, h) }
    }
}

@Composable
fun ColumnScope.GalleryContent(dims: WidgetDims, onAdd: (HomeItem.Widget) -> Unit, onAddAndroid: (AppWidgetProviderInfo, WidgetSize) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf<WidgetKind?>(null) }
    var android by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var sizeIdx by remember { mutableIntStateOf(0) }
    val sel = selected
    val andr = android
    if (sel == null && andr == null) {
        SheetTitle("Widgets")
        WidgetKind.entries.filter { it != WidgetKind.ANDROID }.forEach { k ->
            val (ic, col) = widgetIcon(k)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { selected = k; sizeIdx = 0 }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(col), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    LText(widgetName(k), 17.sp, FontWeight.SemiBold)
                    LText(widgetDescription(k), 13.sp, color = p.secondary, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LText("WIDGETS DE OUTRAS APPS", 13.sp, color = p.secondary)
        Spacer(Modifier.height(6.dp))
        val providers = remember { WidgetHost.providers(ctx) }
        providers.forEach { info ->
            val app = remember(info) { runCatching { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(info.provider.packageName, 0)).toString() }.getOrDefault("") }
            val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, info) {
                value = withContext(Dispatchers.Default) { runCatching {
                    val d = ctx.packageManager.getApplicationIcon(info.provider.packageName)
                    val b = AndroidBitmap.createBitmap(96, 96, AndroidBitmap.Config.ARGB_8888)
                    d.setBounds(0, 0, 96, 96); d.draw(AndroidCanvas(b)); b.asImageBitmap()
                }.getOrNull() }
            }
            Row(Modifier.fillMaxWidth().clickable { android = info; sizeIdx = 0 }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp)) { icon?.let { Image(it, null, Modifier.size(36.dp)) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    LText(app, 15.sp, FontWeight.SemiBold, maxLines = 1)
                    LText(info.loadLabel(ctx.packageManager), 13.sp, color = p.secondary, maxLines = 1)
                }
            }
        }
        return
    }
    val title = if (sel != null) widgetName(sel) else andr!!.loadLabel(ctx.packageManager)
    SheetTitle(title, "Voltar") { selected = null; android = null }
    if (sel != null) LText(widgetDescription(sel), 15.sp, color = p.secondary, modifier = Modifier.padding(bottom = 14.dp))
    Segmented(listOf("Pequeno", "Médio", "Grande"), sizeIdx) { sizeIdx = it }
    Spacer(Modifier.height(18.dp))
    val size = WidgetSize.entries[sizeIdx]
    if (sel != null) {
        val preview = remember(sel, size) { HomeItem.Widget(id = "preview", kind = sel, size = size) }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { WidgetPreview(preview, dims, dims.wideW) }
    } else {
        val (w, h) = dims.of(size)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(w * 0.9f, h * 0.9f).clip(RoundedCornerShape(WidgetCorner)).background(p.fill), contentAlignment = Alignment.Center) {
                LText("${andr!!.loadLabel(ctx.packageManager)}", 15.sp, color = p.secondary)
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(p.accent).noRippleClick {
            if (sel != null) onAdd(HomeItem.Widget(kind = sel, size = size)) else onAddAndroid(andr!!, size)
        },
        contentAlignment = Alignment.Center,
    ) { LText("+  Adicionar widget", 17.sp, FontWeight.SemiBold, Color.White) }
}

// ============================ Configuração de um widget ============================

@Composable
fun ColumnScope.WidgetConfigContent(id: String, onDone: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val layout by Store.layout.collectAsState()
    val item = (layout.pages.flatMap { it.items } + layout.today).firstOrNull { it.id == id } as? HomeItem.Widget
    if (item == null) { LText("Widget removido.", 15.sp); return }
    SheetTitle(widgetName(item.kind), "OK", onDone)
    if (item.kind != WidgetKind.ANDROID) {
        Segmented(listOf("Pequeno", "Médio", "Grande"), item.size.ordinal) { i -> updateWidget(id) { it.copy(size = WidgetSize.entries[i]) } }
        Spacer(Modifier.height(16.dp))
    }
    when (item.kind) {
        WidgetKind.TRANSIT -> TransitConfig(item)
        WidgetKind.PHOTOS -> {
            val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
                scope.launch { savePhotos(ctx, id, uris); updateWidget(id) { it.copy(config = it.config + ("v" to System.currentTimeMillis().toString())) } }
            }
            ActionButton("Escolher fotografias") { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            LText("As fotos são copiadas para o launcher e vão rodando a cada hora.", 13.sp, color = p.secondary, modifier = Modifier.padding(top = 8.dp))
        }
        WidgetKind.WORLD_CLOCK, WidgetKind.CLOCK_ANALOG -> {
            val cur = item.config["zones"]?.split(",")?.filter { it.isNotBlank() } ?: Store.s.worldClocks
            LText("CIDADES (até 8)", 13.sp, color = p.secondary)
            Spacer(Modifier.height(6.dp))
            cityNames.forEach { (z, n) ->
                val on = z in cur
                Row(Modifier.fillMaxWidth().clickable {
                    val next = if (on) cur - z else (cur + z).take(8)
                    updateWidget(id) { it.copy(config = it.config + ("zones" to next.joinToString(","))) }
                }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LText(n, 16.sp, modifier = Modifier.weight(1f))
                    if (on) LText("✓", 17.sp, FontWeight.Bold, p.accent)
                }
            }
        }
        WidgetKind.ACTIVITY, WidgetKind.STEPS -> HealthPermissionBlock()
        WidgetKind.CALENDAR -> PermissionButton(Manifest.permission.READ_CALENDAR, "Permitir acesso ao calendário") { scope.launch { CalendarRepo.refresh(ctx) } }
        WidgetKind.BATTERY -> PermissionButton(Manifest.permission.BLUETOOTH_CONNECT, "Mostrar dispositivos Bluetooth") { BatteryRepo.refresh(ctx) }
        WidgetKind.WEATHER -> WeatherCityConfig()
        else -> {}
    }
}

@Composable
fun ActionButton(text: String, color: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().height(50.dp).clip(CircleShape).background(color ?: p.fill).noRippleClick(onClick), contentAlignment = Alignment.Center) {
        LText(text, 17.sp, FontWeight.SemiBold, if (color != null) Color.White else p.accent)
    }
}

@Composable
fun PermissionButton(permission: String, text: String, onGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) onGranted() }
    ActionButton(text) { launcher.launch(permission) }
}

@Composable
fun HealthPermissionBlock() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { scope.launch { HealthRepo.refresh(ctx) } }
    if (!HealthRepo.available(ctx)) {
        LText("O Health Connect não está disponível neste telemóvel.", 15.sp, color = p.secondary)
        return
    }
    ActionButton("Ligar ao Health Connect") { launcher.launch(HealthRepo.permissions) }
    LText("Lê passos, calorias ativas, exercício, distância e sono (Google Fit, Mi Fitness, Strava…).", 13.sp, color = p.secondary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(12.dp)).background(p.fill).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, null, tint = p.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) LText(placeholder, 17.sp, color = p.secondary)
            BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = p.label, fontSize = 17.sp, fontFamily = LocalFont.current), cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun TransitConfig(item: HomeItem.Widget) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val mode = item.config["mode"] ?: "carris"
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    Segmented(listOf("Carris Metropolitana", "CP Comboios"), if (mode == "cp") 1 else 0) { i ->
        updateWidget(item.id) { it.copy(config = it.config + ("mode" to if (i == 1) "cp" else "carris")) }; results = emptyList()
    }
    Spacer(Modifier.height(12.dp))
    val cur = item.config["name"]
    if (!cur.isNullOrBlank()) LText("Atual: $cur", 15.sp, FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
    SearchField(q, if (mode == "cp") "Estação (ex.: Oriente)" else "Paragem ou número (ex.: 060361)") { q = it }
    LaunchedEffect(q, mode) {
        if (q.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(350)
        loading = true
        results = runCatching { if (mode == "cp") TransitRepo.searchCp(q) else TransitRepo.searchCarris(ctx, q) }.getOrDefault(emptyList())
        loading = false
    }
    if (loading) LText("A procurar…", 14.sp, color = p.secondary, modifier = Modifier.padding(top = 8.dp))
    results.forEach { s ->
        Column(Modifier.fillMaxWidth().clickable {
            updateWidget(item.id) { it.copy(config = it.config + mapOf("id" to s.id, "name" to s.name)) }
            q = ""; results = emptyList()
        }.padding(vertical = 10.dp)) {
            LText(s.name, 16.sp, FontWeight.Medium, maxLines = 1)
            LText(if (mode == "cp") "Estação ${s.id}" else "${s.id} · ${s.extra}", 13.sp, color = p.secondary, maxLines = 1)
        }
    }
    if (mode == "carris") {
        Spacer(Modifier.height(14.dp))
        LText("FILTRAR POR LINHA (opcional)", 13.sp, color = p.secondary)
        Spacer(Modifier.height(6.dp))
        var line by remember(item.id) { mutableStateOf(item.config["line"].orEmpty()) }
        SearchField(line, "Ex.: 2794") { line = it.filter { c -> c.isDigit() }.take(4); updateWidget(item.id) { w -> w.copy(config = w.config + ("line" to line)) } }
    }
}

@Composable
fun WeatherCityConfig() {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }
    val s = Store.s
    val loc = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        Store.settings { it.copy(useLocation = ok) }; scope.launch { WeatherRepo.refresh(ctx, true) }
    }
    Column(Modifier.clip(RoundedCornerShape(16.dp)).background(p.cell2)) {
        ToggleRow("Usar a localização atual", s.useLocation, divider = false) { on ->
            if (on) loc.launch(Manifest.permission.ACCESS_COARSE_LOCATION) else { Store.settings { it.copy(useLocation = false) }; scope.launch { WeatherRepo.refresh(ctx, true) } }
        }
    }
    Spacer(Modifier.height(12.dp))
    LText("Cidade: ${s.cityName}", 15.sp, FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    SearchField(q, "Procurar cidade") { q = it }
    LaunchedEffect(q) { if (q.length >= 2) { kotlinx.coroutines.delay(350); res = WeatherRepo.searchCity(q) } else res = emptyList() }
    res.forEach { (n, la, lo) ->
        LText(n, 16.sp, modifier = Modifier.fillMaxWidth().clickable {
            Store.settings { it.copy(cityName = n.substringBefore(","), lat = la, lon = lo, useLocation = false) }
            q = ""; res = emptyList(); scope.launch { WeatherRepo.refresh(ctx, true) }
        }.padding(vertical = 10.dp))
    }
}

suspend fun savePhotos(ctx: android.content.Context, widgetId: String, uris: List<Uri>) = withContext(Dispatchers.IO) {
    val dir = File(photosDir(ctx), widgetId); dir.mkdirs()
    dir.listFiles()?.forEach { it.delete() }
    uris.forEachIndexed { i, u ->
        runCatching {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
            var sample = 1; while (o.outWidth / (sample * 2) >= 1000) sample *= 2
            val b = ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@runCatching
            File(dir, "%03d.jpg".format(i)).outputStream().use { b.compress(AndroidBitmap.CompressFormat.JPEG, 88, it) }
            b.recycle()
        }
    }
}
