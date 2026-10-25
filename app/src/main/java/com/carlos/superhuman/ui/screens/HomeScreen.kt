package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.WeatherNow
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.clothingFor
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.feelLabel
import com.carlos.superhuman.data.fmtDayLong
import com.carlos.superhuman.data.greeting
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.pct
import com.carlos.superhuman.data.portfolioLines
import com.carlos.superhuman.data.productivityGrid
import com.carlos.superhuman.data.tasksForToday
import com.carlos.superhuman.data.weatherLabel
import com.carlos.superhuman.data.weekStart
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.device.CalendarRepo
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.Heatmap
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.noRippleClick
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var slot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val today = LocalDate.now()
    val events = rememberEvents(today, refresh)
    val agenda = remember(events) { buildAgenda(events, today) }
    LaunchedEffect(events) { CalendarRepo.cacheToday(ctx) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 130.dp),
    ) {
        // Cabeçalho
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fmtDayLong(today), style = Type.small, color = c.ink2)
                    Text("${greeting()}, ${d.settings.userName}", style = Type.h1, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val evening = LocalTime.now().hour >= d.settings.dailyReviewHour
                val reviewDone = d.reviews.any { it.key == "D-${today.toEpochDay()}" }
                CircleIconButton(Icons.Rounded.Notifications, badge = evening && !reviewDone) { Nav.go("review", if (today.dayOfWeek.value == 5) "weekly" else "daily") }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.CalendarMonth) { Nav.go("schedule") }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(c.card).border(1.dp, c.line, RoundedCornerShape(50))
                    .clickable { Nav.go("search") }.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.ink2, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text("Pesquisar tasks, notas, eventos…", style = Type.body, color = c.ink3, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.AutoAwesome, null, tint = c.ink2, modifier = Modifier.size(19.dp).noRippleClick { Nav.go("assistant") })
            }
        }

        // Tempo
        item {
            Spacer(Modifier.height(14.dp))
            WeatherCard(d.weather, d.settings.locationName)
        }

        // Despesas por confirmar (Google Wallet)
        val pending = d.expenses.count { it.pending }
        if (pending > 0) item {
            Spacer(Modifier.height(10.dp))
            SHCard(Modifier.fillMaxWidth(), onClick = { Nav.go("finance") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Receipt, null, tint = c.accent)
                    Spacer(Modifier.width(10.dp))
                    Text("$pending ${if (pending == 1) "pagamento" else "pagamentos"} do Wallet por confirmar", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                    Text("Rever", style = Type.small, color = c.ink2)
                }
            }
        }

        // Performance
        item {
            Spacer(Modifier.height(10.dp))
            val start = weekStart().startMillis()
            val prevStart = weekStart().minusWeeks(1).startMillis()
            val thisWeek = d.tasks.count { (it.completedAt ?: 0L) >= start }
            val lastWeek = d.tasks.count { val t = it.completedAt ?: 0L; t in prevStart until start }
            val change = if (lastWeek > 0) (thisWeek - lastWeek) * 100.0 / lastWeek else 0.0
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(118.dp)) {
                        Text("A tua\nperformance", style = Type.title, color = c.ink)
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.clip(RoundedCornerShape(50)).background(c.ink).clickable { Nav.go("review", "weekly") }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text("Ver semana", style = Type.small, color = c.onInk)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Heatmap(d.productivityGrid(12), Modifier.fillMaxWidth())
                        Box(
                            Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(50)).background(c.card).padding(horizontal = 8.dp, vertical = 3.dp),
                        ) { Text("▲ ${pct(change)}".replace("▲ -", "▼ -"), style = Type.tiny, color = if (change >= 0) c.good else c.bad) }
                    }
                }
            }
        }

        // Pulso rápido
        item {
            Spacer(Modifier.height(10.dp))
            val m = d.monthSummary()
            val beer = d.beerFeedback()
            val pl = d.portfolioLines()
            val dayChange = if (pl.isNotEmpty()) pl.sumOf { it.value * it.dayPct / 100 } else null
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PulseTile("Plafond restante", if (m.budget > 0) euro(m.remaining) else "Definir", if (m.budget > 0) "${((m.spent + m.fixed) / m.budget * 100).roundToInt()}% usado" else "toca para definir") { Nav.go("finance") }
                PulseTile("Cervejas semana", "${d.beersThisWeek()} / ${d.settings.beerWeeklyLimit}", beer.title) { Nav.go("health") }
                PulseTile("Portfólio hoje", if (dayChange != null) euro(dayChange) else "—", if (d.investFeed != null) "Coach: ${d.investFeed?.mood}" else "abrir Invest") { Nav.go("invest") }
            }
        }

        // O teu dia
        item {
            SectionHeader("O teu dia", "Agenda") { Nav.go("schedule") }
            if (!CalendarRepo.hasPermission(ctx)) EmptyState("Calendário não ligado", "Dá permissão para ver eventos de trabalho e pessoais.")
            else AgendaTimeline(agenda, events.filter { it.allDay }) { s, e -> slot = s to e }
        }

        // Tasks
        item { SectionHeader("Tasks de hoje", "Ver todas") { Nav.go("tasks") } }
        val todayTasks = d.tasksForToday().take(6)
        if (todayTasks.isEmpty()) item { EmptyState("Tudo feito ✨", "Sem tasks pendentes para hoje.") }
        items(todayTasks, key = { it.id }) { t ->
            TaskRow(t, Modifier.padding(bottom = 10.dp))
        }
    }

    slot?.let { (s, e) -> AddEventSheet(today, s, e, onDismiss = { slot = null }, onCreated = { refresh++; slot = null }) }
}

@Composable
private fun PulseTile(label: String, value: String, sub: String, onClick: () -> Unit) {
    val c = SH.c
    SHCard(Modifier.width(158.dp), padding = 14.dp, onClick = onClick) {
        Text(label, style = Type.small, color = c.ink2)
        Spacer(Modifier.height(10.dp))
        Text(value, style = Type.h2, color = c.ink, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(sub, style = Type.tiny, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun weatherIcon(code: Int, night: Boolean = false): ImageVector = when {
    code in 95..99 -> Icons.Rounded.Thunderstorm
    code in 51..82 -> Icons.Rounded.Umbrella
    code >= 3 -> Icons.Rounded.Cloud
    night -> Icons.Rounded.NightsStay
    else -> Icons.Rounded.WbSunny
}

@Composable
fun WeatherCard(w: WeatherNow, place: String) {
    val c = SH.c
    val night = LocalTime.now().hour !in 7..19
    SHCard(Modifier.fillMaxWidth(), onClick = { Nav.go("settings") }) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("$place · ${weatherLabel(w.code)}", style = Type.small, color = c.ink2)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${w.temp.roundToInt()}°", style = Type.display, color = c.ink)
                    Spacer(Modifier.width(10.dp))
                    Text("sensação ${w.feels.roundToInt()}°", style = Type.small, color = c.ink2, modifier = Modifier.padding(bottom = 7.dp))
                }
                Text("↑ ${w.max.roundToInt()}°   ↓ ${w.min.roundToInt()}°   ☂ ${w.rainProb}%", style = Type.small, color = c.ink2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(weatherIcon(w.code, night), null, tint = c.accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(if (w.feels < 16) c.ink else c.accent).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(feelLabel(w.feels), style = Type.small, color = c.onInk)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        InsetBox(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Checkroom, null, tint = c.ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Veste hoje", style = Type.small, color = c.ink)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Tune, null, tint = c.ink3, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                clothingFor(w).forEach {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(c.card).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(it, style = Type.small, color = c.ink)
                    }
                }
            }
        }
        if (w.updatedAt == 0L) Text("A carregar o tempo…", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 8.dp))
    }
}
