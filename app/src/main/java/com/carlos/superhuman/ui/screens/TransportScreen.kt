package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.TransportCard
import com.carlos.superhuman.data.TransportRoute
import com.carlos.superhuman.net.Http
import com.carlos.superhuman.net.str
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import kotlinx.serialization.json.JsonArray
import java.time.LocalTime

fun parseHex(hex: String, fallback: Color = Color(0xFF1B1A19)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

/** Próximas partidas a partir dos horários fixos guardados. */
fun nextDepartures(times: List<String>, n: Int = 3): List<Pair<String, Int>> {
    val now = LocalTime.now()
    val nowMin = now.hour * 60 + now.minute
    return times.mapNotNull { t ->
        val p = t.trim().split(":")
        val m = (p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
        t.trim() to m
    }.sortedBy { it.second }.filter { it.second >= nowMin }.take(n).map { it.first to (it.second - nowMin) }
}

@Composable
fun TransportScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    var editCard by remember { mutableStateOf<TransportCard?>(null) }
    var editRoute by remember { mutableStateOf<TransportRoute?>(null) }

    ScreenScaffold(
        title = "Transportes",
        onBack = { Nav.back() },
        actions = { CircleIconButton(Icons.Rounded.Add, filled = true) { editRoute = TransportRoute(operator = "CP", from = "", to = "") } },
    ) {
        item { SectionHeader("Cartões", "+ Cartão") { editCard = TransportCard(operator = "Carris", title = "") } }
        items(d.transportCards, key = { it.id }) { card ->
            TransportCardView(card, Modifier.padding(bottom = 12.dp)) { editCard = card }
        }
        item { SectionHeader("Trajetos favoritos") }
        if (d.transportRoutes.isEmpty()) item { EmptyState("Sem trajetos", "Adiciona os teus trajetos CP e Carris.") }
        items(d.transportRoutes, key = { it.id }) { r -> RouteCard(r) { editRoute = r } }
    }

    editCard?.let { CardSheet(it) { editCard = null } }
    editRoute?.let { RouteSheet(it) { editRoute = null } }
}

@Composable
fun TransportCardView(card: TransportCard, modifier: Modifier = Modifier, onEdit: () -> Unit) {
    val base = parseHex(card.colorHex)
    val onColor = if (base.luminance() > 0.55f) Color(0xFF1B1A19) else Color.White
    Box(
        modifier.fillMaxWidth().aspectRatio(1.62f).clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.78f))))
            .clickable(onClick = onEdit)
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (card.operator == "CP") Icons.Rounded.Train else Icons.Rounded.DirectionsBus, null, tint = onColor, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text(card.operator, style = Type.h2, color = onColor, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Edit, null, tint = onColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(card.title, style = Type.title, color = onColor.copy(alpha = 0.85f))
            Spacer(Modifier.height(4.dp))
            Text(card.number.chunked(4).joinToString(" "), style = Type.h1, color = onColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Titular", style = Type.tiny, color = onColor.copy(alpha = 0.7f))
                    Text(card.holder, style = Type.small, color = onColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Válido até", style = Type.tiny, color = onColor.copy(alpha = 0.7f))
                    Text(card.validUntil, style = Type.small, color = onColor)
                }
            }
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun RouteCard(r: TransportRoute, onEdit: () -> Unit) {
    val c = SH.c
    val ctx = LocalContext.current
    val live by produceState<List<Pair<String, String>>?>(initialValue = null, r.stopId) {
        value = if (r.stopId.isBlank()) null else runCatching {
            // Carris Metropolitana — tempo real por paragem (opcional)
            val arr = Http.parse(Http.get("https://api.carrismetropolitana.pt/stops/${r.stopId}/realtime")) as? JsonArray
            arr?.filter { r.line.isBlank() || it.str("line_id") == r.line }?.mapNotNull {
                val eta = it.str("estimated_arrival") ?: it.str("scheduled_arrival") ?: return@mapNotNull null
                (it.str("headsign") ?: it.str("line_id") ?: "") to eta.take(5)
            }?.take(3)
        }.getOrNull()
    }
    SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.inset), contentAlignment = Alignment.Center) {
                Icon(if (r.operator == "CP") Icons.Rounded.Train else Icons.Rounded.DirectionsBus, null, tint = c.ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${r.from} → ${r.to}", style = Type.title, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.operator}${if (r.line.isNotBlank()) " · " + r.line else ""}", style = Type.tiny, color = c.ink3)
            }
            CircleIconButton(Icons.Rounded.Edit, size = 36.dp, onClick = onEdit)
            if (r.url.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                CircleIconButton(Icons.Rounded.OpenInNew, size = 36.dp) {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val next = nextDepartures(r.departures)
        val rows: List<Pair<String, String>> = live ?: next.map { (t, inMin) -> t to if (inMin < 60) "em $inMin min" else "" }
        if (rows.isEmpty()) {
            Text(if (r.departures.isEmpty()) "Adiciona horários (ex.: 07:42, 08:12) ou um ID de paragem para tempo real." else "Sem mais partidas hoje.", style = Type.small, color = c.ink2)
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { i, (a, b) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (i == 0) c.ink else c.inset).padding(10.dp),
                ) {
                    Text(if (live != null) b else a, style = Type.title, color = if (i == 0) c.onInk else c.ink)
                    Text(if (live != null) a else b, style = Type.tiny, color = if (i == 0) c.onInk.copy(alpha = 0.7f) else c.ink3, maxLines = 1)
                }
            }
        }
        if (live != null) Text("Tempo real · Carris Metropolitana", style = Type.tiny, color = c.good, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CardSheet(initial: TransportCard, onDismiss: () -> Unit) {
    val exists = Store.data.transportCards.any { it.id == initial.id }
    var op by remember { mutableStateOf(if (initial.operator == "CP") 0 else 1) }
    var title by remember { mutableStateOf(initial.title) }
    var number by remember { mutableStateOf(initial.number) }
    var holder by remember { mutableStateOf(initial.holder) }
    var valid by remember { mutableStateOf(initial.validUntil) }
    var color by remember { mutableStateOf(initial.colorHex) }
    Sheet(if (exists) "Editar cartão" else "Novo cartão", onDismiss) {
        Segmented(listOf("CP", "Carris"), op, Modifier.fillMaxWidth()) { op = it }
        Spacer(Modifier.height(10.dp))
        SHTextField(title, { title = it }, "Nome (ex.: Navegante Metropolitano)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(number, { number = it }, "Número do cartão", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(holder, { holder = it }, "Titular", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(valid, { valid = it }, "Válido até (ex.: 31/10/2026)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(color, { color = it }, "Cor (#RRGGBB)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exists) SecondaryButton("Apagar") {
                Store.update { d -> d.copy(transportCards = d.transportCards.filter { it.id != initial.id }) }; onDismiss()
            }
            PrimaryButton("Guardar", Modifier.weight(1f)) {
                val card = initial.copy(operator = if (op == 0) "CP" else "Carris", title = title, number = number, holder = holder, validUntil = valid, colorHex = color)
                Store.update { d ->
                    d.copy(transportCards = if (exists) d.transportCards.map { if (it.id == card.id) card else it } else d.transportCards + card)
                }
                onDismiss()
            }
        }
    }
}

@Composable
private fun RouteSheet(initial: TransportRoute, onDismiss: () -> Unit) {
    val exists = Store.data.transportRoutes.any { it.id == initial.id }
    var op by remember { mutableStateOf(if (initial.operator == "CP") 0 else 1) }
    var from by remember { mutableStateOf(initial.from) }
    var to by remember { mutableStateOf(initial.to) }
    var line by remember { mutableStateOf(initial.line) }
    var stop by remember { mutableStateOf(initial.stopId) }
    var times by remember { mutableStateOf(initial.departures.joinToString(", ")) }
    var url by remember { mutableStateOf(initial.url) }
    Sheet(if (exists) "Editar trajeto" else "Novo trajeto", onDismiss) {
        Segmented(listOf("CP", "Carris"), op, Modifier.fillMaxWidth()) { op = it }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SHTextField(from, { from = it }, "Origem / paragem", Modifier.weight(1f))
            SHTextField(to, { to = it }, "Destino", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        SHTextField(line, { line = it }, "Linha (opcional)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(times, { times = it }, "Horários: 07:42, 08:12, 18:05…", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
        Spacer(Modifier.height(8.dp))
        SHTextField(stop, { stop = it }, "ID da paragem (tempo real Carris Metropolitana, opcional)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(url, { url = it }, "Link (horários / app)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exists) SecondaryButton("Apagar") {
                Store.update { d -> d.copy(transportRoutes = d.transportRoutes.filter { it.id != initial.id }) }; onDismiss()
            }
            PrimaryButton("Guardar", Modifier.weight(1f), enabled = from.isNotBlank() && to.isNotBlank()) {
                val r = initial.copy(
                    operator = if (op == 0) "CP" else "Carris", from = from, to = to, line = line, stopId = stop.trim(), url = url.trim(),
                    departures = times.split(",", ";", " ", "\n").map { it.trim() }.filter { it.matches(Regex("""\d{1,2}:\d{2}""")) },
                )
                Store.update { d -> d.copy(transportRoutes = if (exists) d.transportRoutes.map { if (it.id == r.id) r else it } else d.transportRoutes + r) }
                onDismiss()
            }
        }
    }
}
