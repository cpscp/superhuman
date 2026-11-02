package com.carlos.liquid.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.AppEntry
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.Store
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.icons.IconSpec
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalFont
import com.carlos.liquid.ui.noRippleClick
import java.text.Normalizer

fun normalize(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

val categoryOrder = listOf("Social", "Utilitários", "Produtividade e finanças", "Criatividade", "Entretenimento", "Informação e leitura", "Viagens", "Saúde e forma física", "Compras e comida", "Jogos")

@Composable
fun AppLibrary(st: HomeState, geo: Geo, spec: IconSpec, actions: HomeActions) {
    val apps by AppRepo.apps.collectAsState()
    val usage by Store.usage.collectAsState()
    val s by Store.settings.collectAsState()
    val d = geo.density
    val visible = apps.filter { it.key !in s.hidden }
    val groups = remember(visible, usage) {
        val list = ArrayList<Pair<String, List<AppEntry>>>()
        val sugg = visible.sortedByDescending { usage[it.key] ?: 0 }.take(4)
        if (sugg.isNotEmpty()) list += "Sugestões" to sugg
        list += "Adicionados recentemente" to visible.sortedByDescending { it.installTime }.take(4)
        val byCat = visible.groupBy { it.category }
        categoryOrder.forEach { c -> byCat[c]?.let { l -> list += c to l.sortedByDescending { usage[it.key] ?: 0 } } }
        list
    }
    val tile = (geo.w - 3 * 20 * d) / 2
    val tileDp = (tile / d).dp
    val big = (tileDp - 30.dp) / 2
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(44.dp).liquidGlass(corner = 22.dp, bezel = 12.dp).noRippleClick { st.librarySearch = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            LText("Biblioteca de apps", 17.sp, color = Color.White.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(18.dp))
        groups.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                pair.forEach { (name, list) ->
                    Column(Modifier.width(tileDp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(tileDp).liquidGlass(corner = 30.dp, bezel = 18.dp, strength = 0.7f).padding(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                for (r in 0 until 2) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for (c in 0 until 2) {
                                        val idx = r * 2 + c
                                        val showCluster = idx == 3 && list.size > 4
                                        if (showCluster) {
                                            Box(Modifier.size(big).noRippleClick { st.folder = FolderOpen("cat:$name", Rect0) }) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    for (rr in 0 until 2) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        for (cc in 0 until 2) {
                                                            val e = list.getOrNull(3 + rr * 2 + cc)
                                                            Box(Modifier.size((big - 4.dp) / 2)) { if (e != null) IconImage(e.key, (big - 4.dp) / 2, spec) }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            val e = list.getOrNull(idx)
                                            Box(Modifier.size(big)) {
                                                if (e != null) {
                                                    val id = "lib:${e.key}"
                                                    Box(Modifier.onBounds(st, id).itemGestures(id, { false },
                                                        onTap = { actions.launch(e.key, id) },
                                                        onLongPress = { st.menu = MenuInfo(com.carlos.liquid.data.HomeItem.App(id = id, key = e.key), st.bounds[id] ?: Rect0, false, folderId = "library") },
                                                        onDragStart = {}, rootOffset = { st.bounds[id]?.topLeft ?: androidx.compose.ui.geometry.Offset.Zero })) {
                                                        IconImage(e.key, big, spec)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LText(name, 13.sp, FontWeight.Medium, Color.White, shadow = true, maxLines = 1, align = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(30.dp))
    }
}

val Rect0 = androidx.compose.ui.geometry.Rect.Zero

/** Lista alfabética da Biblioteca com pesquisa. */
@Composable
fun BoxScope.LibrarySearch(st: HomeState, spec: IconSpec, geo: Geo) {
    val apps by AppRepo.apps.collectAsState()
    var q by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    val list = remember(apps, q) {
        val n = normalize(q.trim())
        apps.filter { it.key !in Store.s.hidden && (n.isEmpty() || normalize(it.label).contains(n)) }
    }
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassField(q, "Biblioteca de apps", Modifier.weight(1f).focusRequester(fr)) { q = it }
            Spacer(Modifier.width(10.dp))
            LText("Cancelar", 17.sp, FontWeight.Medium, Color.White, Modifier.noRippleClick { st.librarySearch = false })
        }
        LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
        Spacer(Modifier.height(8.dp))
        val state = rememberLazyListState()
        LazyColumn(Modifier.weight(1f).windowInsetsPadding(WindowInsets.ime), state = state) {
            var lastLetter = ""
            val withHeaders = ArrayList<Any>()
            list.forEach { e ->
                val letter = normalize(e.label).firstOrNull()?.uppercaseChar()?.let { if (it.isLetter()) it.toString() else "#" } ?: "#"
                if (letter != lastLetter) { withHeaders += letter; lastLetter = letter }
                withHeaders += e
            }
            items(withHeaders, key = { if (it is AppEntry) it.key else "h:$it" }) { x ->
                if (x is String) LText(x, 15.sp, FontWeight.Bold, Color.White.copy(alpha = 0.7f), Modifier.padding(top = 14.dp, bottom = 4.dp))
                else {
                    val e = x as AppEntry
                    Row(Modifier.fillMaxWidth().clickable { st.librarySearch = false; AppRepo.launch(ctx, e.key) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppRowIcon(e.key, 44.dp, spec)
                        Spacer(Modifier.width(14.dp))
                        LText(e.label, 17.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassField(value: String, placeholder: String, modifier: Modifier = Modifier, onDone: () -> Unit = {}, onChange: (String) -> Unit) {
    val kb = LocalSoftwareKeyboardController.current
    Row(modifier.height(46.dp).liquidGlass(corner = 23.dp, bezel = 12.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) LText(placeholder, 17.sp, color = Color.White.copy(alpha = 0.6f))
            BasicTextField(value, onChange, singleLine = true, cursorBrush = SolidColor(Color.White),
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontFamily = LocalFont.current),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { kb?.hide(); onDone() }),
                modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp).noRippleClick { onChange("") })
    }
}

// ================================ Spotlight ================================

private fun calc(expr: String): String? {
    val e = expr.replace(',', '.').replace('x', '*').replace('×', '*').replace('÷', '/').replace(" ", "")
    if (e.isEmpty() || !e.any { it in "+-*/" } || !e.all { it.isDigit() || it in "+-*/.()%" }) return null
    return runCatching {
        val v = Parser(e).parse()
        if (v.isNaN() || v.isInfinite()) null
        else if (v == Math.floor(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else "%.6f".format(v).trimEnd('0').trimEnd('.').replace('.', ',')
    }.getOrNull()
}

private class Parser(val s: String) {
    var i = 0
    fun parse(): Double { val v = expr(); if (i != s.length) throw IllegalArgumentException(); return v }
    fun expr(): Double { var v = term(); while (i < s.length && (s[i] == '+' || s[i] == '-')) { val op = s[i++]; val t = term(); v = if (op == '+') v + t else v - t }; return v }
    fun term(): Double { var v = factor(); while (i < s.length && (s[i] == '*' || s[i] == '/' || s[i] == '%')) { val op = s[i++]; val f = factor(); v = when (op) { '*' -> v * f; '/' -> v / f; else -> v % f } }; return v }
    fun factor(): Double {
        if (i < s.length && s[i] == '-') { i++; return -factor() }
        if (i < s.length && s[i] == '(') { i++; val v = expr(); i++; return v }
        val st = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        return s.substring(st, i).toDouble()
    }
}

data class SettingLink(val name: String, val action: String, val keywords: String)

val settingLinks = listOf(
    SettingLink("Wi‑Fi", AndroidSettings.ACTION_WIFI_SETTINGS, "wifi internet rede"),
    SettingLink("Bluetooth", AndroidSettings.ACTION_BLUETOOTH_SETTINGS, "bluetooth relogio auscultadores"),
    SettingLink("Bateria", Intent.ACTION_POWER_USAGE_SUMMARY, "bateria energia"),
    SettingLink("Ecrã", AndroidSettings.ACTION_DISPLAY_SETTINGS, "ecra brilho display"),
    SettingLink("Som", AndroidSettings.ACTION_SOUND_SETTINGS, "som volume toque"),
    SettingLink("Notificações", AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS.let { "android.settings.ALL_APPS_NOTIFICATION_SETTINGS" }, "notificacoes"),
    SettingLink("Localização", AndroidSettings.ACTION_LOCATION_SOURCE_SETTINGS, "localizacao gps"),
    SettingLink("Armazenamento", AndroidSettings.ACTION_INTERNAL_STORAGE_SETTINGS, "armazenamento espaco"),
    SettingLink("Apps", AndroidSettings.ACTION_APPLICATION_SETTINGS, "apps aplicacoes"),
    SettingLink("Segurança", AndroidSettings.ACTION_SECURITY_SETTINGS, "seguranca impressao digital pin"),
)

@Composable
fun BoxScope.Spotlight(st: HomeState, spec: IconSpec, geo: Geo) {
    val apps by AppRepo.apps.collectAsState()
    val usage by Store.usage.collectAsState()
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    val prog = remember { Animatable(0f) }
    LaunchedEffect(Unit) { prog.animateTo(1f, tween(220)); runCatching { fr.requestFocus() } }
    val n = normalize(q.trim())
    val matches = remember(apps, n) {
        if (n.isEmpty()) apps.filter { it.key !in Store.s.hidden }.sortedByDescending { usage[it.key] ?: 0 }.take(8)
        else apps.filter { normalize(it.label).contains(n) }.sortedBy { if (normalize(it.label).startsWith(n)) 0 else 1 }.take(12)
    }
    val result = remember(q) { calc(q) }
    val settings = remember(n) { if (n.length < 2) emptyList() else settingLinks.filter { normalize(it.name).contains(n) || it.keywords.contains(n) } }
    fun open(i: Intent) { st.spotlight = false; runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f * prog.value)).noRippleClick { st.spotlight = false })
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.ime)
            .windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            if (result != null) ResultCard(Icons.Rounded.Calculate, "= $result", "Calculadora", Color(0xFFFF9500)) {}
            LText(if (n.isEmpty()) "Sugestões" else "Apps", 15.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.8f), Modifier.padding(start = 6.dp, bottom = 8.dp, top = 6.dp))
            Box(Modifier.fillMaxWidth().liquidGlass(corner = 28.dp, bezel = 16.dp, strength = 0.6f).padding(vertical = 14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (matches.isEmpty()) LText("Sem resultados", 15.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp))
                    matches.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { e ->
                                Box(Modifier.weight(1f).noRippleClick { st.spotlight = false; AppRepo.launch(ctx, e.key) }, contentAlignment = Alignment.TopCenter) {
                                    AppIcon(e.key, 56.dp, spec, e.label, 0, false, 0, null)
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            settings.forEach { sl -> ResultCard(Icons.Rounded.Settings, sl.name, "Definições", Color(0xFF8E8E93)) { open(Intent(sl.action)) } }
            if (n.isNotEmpty()) {
                ResultCard(Icons.Rounded.Public, "Pesquisar «${q.trim()}» na web", "Google", Color(0xFF0A84FF)) {
                    open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q.trim()))))
                }
                ResultCard(Icons.Rounded.Shop, "Procurar na Play Store", "Google Play", Color(0xFF34C759)) {
                    open(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q.trim()))))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        GlassField(q, "Pesquisar", Modifier.fillMaxWidth().focusRequester(fr), onDone = {
            matches.firstOrNull()?.let { st.spotlight = false; AppRepo.launch(ctx, it.key) }
        }) { q = it }
    }
}

@Composable
private fun ResultCard(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).liquidGlass(corner = 22.dp, bezel = 12.dp, strength = 0.6f).clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            LText(title, 17.sp, FontWeight.SemiBold, Color.White, maxLines = 1)
            LText(subtitle, 13.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Suppress("unused") private val keepCircle = CircleShape
