package com.carlos.liquid.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.ClockFont
import com.carlos.liquid.data.InlineKind
import com.carlos.liquid.data.LockControl
import com.carlos.liquid.data.LockWidget
import com.carlos.liquid.data.NotifStyle
import com.carlos.liquid.data.Stop
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.TransitRepo
import com.carlos.liquid.ui.CheckRow
import com.carlos.liquid.ui.Fonts
import com.carlos.liquid.ui.LSlider
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalPalette
import com.carlos.liquid.ui.Segmented
import com.carlos.liquid.ui.SheetTitle
import com.carlos.liquid.ui.ToggleRow
import com.carlos.liquid.ui.home.WallpaperContent
import com.carlos.liquid.ui.noRippleClick
import com.carlos.liquid.ui.widgets.SearchField

fun inlineName(k: InlineKind) = when (k) {
    InlineKind.DATE -> "Data"; InlineKind.WEATHER -> "Meteorologia"; InlineKind.NEXT_EVENT -> "Próximo evento"
    InlineKind.ALARM -> "Próximo alarme"; InlineKind.STEPS -> "Passos"; InlineKind.BATTERY -> "Bateria"
}

fun lockWidgetName(w: LockWidget) = when (w) {
    LockWidget.BATTERY -> "Bateria"; LockWidget.WEATHER -> "Meteorologia"; LockWidget.ACTIVITY -> "Anéis de atividade"
    LockWidget.STEPS -> "Passos"; LockWidget.NEXT_EVENT -> "Próximo evento"; LockWidget.ALARM -> "Alarme"
    LockWidget.TRANSIT -> "Próxima partida"; LockWidget.WORLD_CLOCK -> "Relógio mundial"; LockWidget.CALENDAR_DAY -> "Dia"
}

fun controlName(c: LockControl) = when (c) {
    LockControl.NONE -> "Nenhum"; LockControl.FLASHLIGHT -> "Lanterna"; LockControl.CAMERA -> "Câmara"
    LockControl.CLOCK -> "Alarmes"; LockControl.CALCULATOR -> "Calculadora"; LockControl.DO_NOT_DISTURB -> "Não incomodar"
}

fun fontName(f: ClockFont) = when (f) {
    ClockFont.INTER -> "Inter"; ClockFont.INTER_LIGHT -> "Fina"; ClockFont.ROUNDED -> "Arredondada"; ClockFont.SERIF -> "Serifada"
    ClockFont.CONDENSED -> "Alta"; ClockFont.MONO -> "Mono"; ClockFont.CUSTOM -> Fonts.customName.ifBlank { "Importada" }
}

val clockColors = listOf(0xFFFFFFFF, 0xFFFFE8B0, 0xFFFFC9D9, 0xFFFF9F0A, 0xFFFF453A, 0xFFBF5AF2, 0xFF5E5CE6, 0xFF64D2FF, 0xFF30D158, 0xFF1C1C1E)

@Composable
fun ColumnScope.LockZoneSheet(zone: String, onDone: () -> Unit) {
    val s by Store.settings.collectAsState()
    val ls = s.lock
    val p = LocalPalette.current
    when (zone) {
        "inline" -> {
            SheetTitle("Por cima do relógio", "OK", onDone)
            Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
                InlineKind.entries.forEachIndexed { i, k -> CheckRow(inlineName(k), ls.inline == k, divider = i < InlineKind.entries.size - 1) { Store.lock { it.copy(inline = k) } } }
            }
        }
        "clock" -> {
            SheetTitle("Tipo de letra e cor", "OK", onDone)
            val fonts = ClockFont.entries.filter { it != ClockFont.CUSTOM || Fonts.custom != null }
            fonts.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { f ->
                        val sel = ls.clockFont == f
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sel) p.accent.copy(alpha = 0.2f) else p.fill)
                                .border(if (sel) 2.dp else 0.dp, if (sel) p.accent else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { Store.lock { it.copy(clockFont = f) } }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LText("12", 30.sp, clockWeight(ls.copy(clockFont = f)), family = Fonts.clock(f))
                            LText(fontName(f), 11.sp, color = p.secondary, maxLines = 1)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (ls.clockFont in setOf(ClockFont.INTER, ClockFont.CUSTOM, ClockFont.ROUNDED, ClockFont.CONDENSED)) {
                LText("Espessura", 13.sp, color = p.secondary, modifier = Modifier.padding(top = 8.dp))
                LSlider(ls.clockWeight, { v -> Store.lock { it.copy(clockWeight = v) } })
            }
            LText("Tamanho", 13.sp, color = p.secondary)
            LSlider(ls.clockSize, { v -> Store.lock { it.copy(clockSize = v) } }, range = 0.7f..1.5f)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
                ToggleRow("Liquid Glass", ls.clockGlass, subtitle = "Os números refratam o wallpaper, como no iOS 26.", divider = false) { v -> Store.lock { it.copy(clockGlass = v) } }
            }
            if (!ls.clockGlass) {
                Spacer(Modifier.height(12.dp))
                LText("Cor", 13.sp, color = p.secondary, modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    clockColors.forEach { c ->
                        val sel = ls.clockColor == c
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(c))
                            .border(if (sel) 3.dp else 1.dp, if (sel) p.accent else p.separator, CircleShape)
                            .noRippleClick { Store.lock { it.copy(clockColor = c) } })
                    }
                }
                Spacer(Modifier.height(8.dp))
                val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(ls.clockColor.toInt(), it) }
                LSlider(hsv[0], { h -> Store.lock { it.copy(clockColor = Color.hsv(h, 0.55f, 1f).toArgb().toLong() and 0xFFFFFFFFL) } }, range = 0f..360f,
                    track = Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.55f, 1f) }))
            }
        }
        "widgets" -> {
            SheetTitle("Widgets", "OK", onDone)
            val used = ls.widgets.sumOf { widgetSlots(it) }
            LText("Até 4 espaços. Próximo evento e Próxima partida ocupam 2.", 13.sp, color = p.secondary, modifier = Modifier.padding(bottom = 10.dp))
            Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
                LockWidget.entries.forEachIndexed { i, w ->
                    val on = w in ls.widgets
                    val canAdd = used + widgetSlots(w) <= 4
                    CheckRow(lockWidgetName(w) + if (widgetSlots(w) == 2) " (largo)" else "", on, divider = i < LockWidget.entries.size - 1,
                        subtitle = if (!on && !canAdd) "Sem espaço" else null) {
                        Store.lock { l -> if (on) l.copy(widgets = l.widgets - w) else if (canAdd) l.copy(widgets = l.widgets + w) else l }
                    }
                }
            }
            if (LockWidget.TRANSIT in ls.widgets) { Spacer(Modifier.height(14.dp)); LockTransitPicker() }
        }
        "notifs" -> {
            SheetTitle("Notificações", "OK", onDone)
            Segmented(listOf("Contagem", "Pilha", "Lista"), ls.notifStyle.ordinal) { i -> Store.lock { it.copy(notifStyle = NotifStyle.entries[i]) } }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
                ToggleRow("Mostrar pré-visualizações", ls.showPreviews, subtitle = "Desligado: o texto só aparece depois de desbloquear.", divider = false) { v -> Store.lock { it.copy(showPreviews = v) } }
            }
            Spacer(Modifier.height(14.dp))
            LText("Escurecer wallpaper", 13.sp, color = p.secondary)
            LSlider(ls.dim, { v -> Store.lock { it.copy(dim = v) } }, range = 0f..0.6f)
        }
        "controls" -> {
            SheetTitle("Controlos", "OK", onDone)
            LText("Mantém premido um controlo no ecrã bloqueado para o usar.", 13.sp, color = p.secondary, modifier = Modifier.padding(bottom = 10.dp))
            LText("ESQUERDA", 13.sp, color = p.secondary)
            Column(Modifier.padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
                LockControl.entries.forEachIndexed { i, c -> CheckRow(controlName(c), ls.leftControl == c, divider = i < LockControl.entries.size - 1) { Store.lock { it.copy(leftControl = c) } } }
            }
            Spacer(Modifier.height(10.dp))
            LText("DIREITA", 13.sp, color = p.secondary)
            Column(Modifier.padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
                LockControl.entries.forEachIndexed { i, c -> CheckRow(controlName(c), ls.rightControl == c, divider = i < LockControl.entries.size - 1) { Store.lock { it.copy(rightControl = c) } } }
            }
        }
        "wallpaper" -> WallpaperContent()
    }
}

@Composable
fun LockTransitPicker() {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val ls = Store.settings.collectAsState().value.lock
    val parts = ls.transitStop.split(":", limit = 3)
    var mode by remember { mutableStateOf(parts.getOrNull(0)?.ifBlank { null } ?: "carris") }
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Stop>>(emptyList()) }
    LText("PARAGEM / ESTAÇÃO", 13.sp, color = p.secondary)
    Spacer(Modifier.height(6.dp))
    Segmented(listOf("Carris Metropolitana", "CP"), if (mode == "cp") 1 else 0) { mode = if (it == 1) "cp" else "carris"; res = emptyList() }
    Spacer(Modifier.height(8.dp))
    if (parts.size == 3) LText("Atual: ${parts[2]}", 15.sp, FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
    SearchField(q, "Procurar") { q = it }
    LaunchedEffect(q, mode) {
        if (q.length < 2) { res = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(350)
        res = runCatching { if (mode == "cp") TransitRepo.searchCp(q) else TransitRepo.searchCarris(ctx, q) }.getOrDefault(emptyList())
    }
    res.forEach { st ->
        LText(st.name, 16.sp, modifier = Modifier.fillMaxWidth().clickable {
            Store.lock { it.copy(transitStop = "$mode:${st.id}:${st.name}") }; q = ""; res = emptyList()
        }.padding(vertical = 10.dp))
    }
}

@Suppress("unused") private fun keepAspect(m: Modifier) = m.aspectRatio(1f)
