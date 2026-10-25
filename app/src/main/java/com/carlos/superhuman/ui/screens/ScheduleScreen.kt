@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.fmtMinutes
import com.carlos.superhuman.data.fmtMonth
import com.carlos.superhuman.data.fmtTime
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.data.weekStart
import com.carlos.superhuman.device.CalEvent
import com.carlos.superhuman.device.CalendarRepo
import com.carlos.superhuman.device.CalInfo
import com.carlos.superhuman.ui.components.BareTextField
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.DetailRow
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.components.Tag
import com.carlos.superhuman.ui.components.pickDate
import com.carlos.superhuman.ui.components.pickTime
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

sealed class AgendaItem {
    data class Ev(val e: CalEvent) : AgendaItem()
    data class Free(val startMin: Int, val endMin: Int) : AgendaItem()
}

fun minutesOf(millis: Long): Int {
    val t = Instant.ofEpochMilli(millis).atZone(com.carlos.superhuman.data.zone).toLocalTime()
    return t.hour * 60 + t.minute
}

/** Mistura eventos e espaços livres (≥30 min) entre as 08:00 e as 20:00. */
fun buildAgenda(events: List<CalEvent>, day: LocalDate): List<AgendaItem> {
    val timed = events.filter { !it.allDay }.sortedBy { it.begin }
    val out = mutableListOf<AgendaItem>()
    val nowMin = if (day == LocalDate.now()) LocalTime.now().let { ((it.hour * 60 + it.minute + 29) / 30) * 30 } else 8 * 60
    var cursor = maxOf(8 * 60, nowMin)
    val endDay = 20 * 60
    for (e in timed) {
        val s = if (e.begin < day.startMillis()) 0 else minutesOf(e.begin)
        val en = if (e.end >= day.plusDays(1).startMillis()) 24 * 60 else minutesOf(e.end)
        if (s - cursor >= 30 && cursor < endDay) out += AgendaItem.Free(cursor, minOf(s, endDay))
        out += AgendaItem.Ev(e)
        cursor = maxOf(cursor, en)
    }
    if (endDay - cursor >= 30) out += AgendaItem.Free(cursor, endDay)
    return out
}

@Composable
fun EventCard(e: CalEvent) {
    val c = SH.c
    val calColor = Color(e.color or 0xFF000000.toInt())
    SHCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(Modifier.height(IntrinsicSize.Min).padding(14.dp)) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(50)).background(c.ink))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = Type.title, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(e.label, calColor)
                    if (e.location.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Place, null, tint = c.ink3, modifier = Modifier.size(13.dp))
                        Text(e.location, style = Type.tiny, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Schedule, null, tint = c.ink2, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (e.allDay) "Todo o dia" else "${fmtTime(e.begin)} – ${fmtTime(e.end)}", style = Type.small, color = c.ink2)
                }
            }
        }
    }
}

@Composable
fun FreeSlotRow(startMin: Int, endMin: Int, onClick: () -> Unit) {
    val c = SH.c
    val dash = c.ink3
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(dash, cornerRadius = CornerRadius(18.dp.toPx()), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Livre · ${fmtMinutes(startMin)} – ${fmtMinutes(endMin)}", style = Type.small, color = c.ink2, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.Add, null, tint = c.ink2, modifier = Modifier.size(18.dp))
        Text("slot", style = Type.small, color = c.ink2)
    }
}

/** Linha temporal com coluna de horas à esquerda (como no ecrã "Schedule" do vídeo). */
@Composable
fun AgendaTimeline(items: List<AgendaItem>, allDay: List<CalEvent>, onSlot: (Int, Int) -> Unit) {
    val c = SH.c
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        allDay.forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dia", style = Type.tiny, color = c.ink3, modifier = Modifier.width(50.dp))
                Box(Modifier.weight(1f)) { EventCard(e) }
            }
        }
        if (items.isEmpty() && allDay.isEmpty()) EmptyState("Dia livre", "Sem eventos. Toca em + para criar um slot.")
        items.forEach { it2 ->
            Row(verticalAlignment = Alignment.Top) {
                val start = when (it2) { is AgendaItem.Ev -> fmtTime(it2.e.begin); is AgendaItem.Free -> fmtMinutes(it2.startMin) }
                Text(start, style = Type.tiny, color = c.ink3, modifier = Modifier.width(50.dp).padding(top = 14.dp))
                Box(Modifier.weight(1f)) {
                    when (it2) {
                        is AgendaItem.Ev -> EventCard(it2.e)
                        is AgendaItem.Free -> FreeSlotRow(it2.startMin, it2.endMin) { onSlot(it2.startMin, minOf(it2.startMin + 60, it2.endMin)) }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberEvents(day: LocalDate, refresh: Int): List<CalEvent> {
    val ctx = LocalContext.current
    val hidden = Store.state.value.settings.hiddenCalendarIds
    val ev by produceState(initialValue = emptyList<CalEvent>(), day, refresh, hidden) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CalendarRepo.events(ctx, day) }
    }
    return ev
}

@Composable
fun ScheduleScreen() {
    val c = SH.c
    var day by remember { mutableStateOf(LocalDate.now()) }
    var refresh by remember { mutableIntStateOf(0) }
    var slot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val events = rememberEvents(day, refresh)
    val agenda = remember(events, day) { buildAgenda(events, day) }
    val ctx = LocalContext.current

    ScreenScaffold(
        title = "Agenda",
        onBack = { Nav.back() },
        actions = {
            CircleIconButton(Icons.Rounded.CalendarMonth) { pickDate(ctx, day) { day = it } }
            CircleIconButton(Icons.Rounded.Add, filled = true) { slot = 9 * 60 to 10 * 60 }
        },
    ) {
        item {
            Text(fmtMonth(YearMonth.from(day)), style = Type.title, color = c.ink, modifier = Modifier.padding(vertical = 8.dp))
            val ws = weekStart(day)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0..6).forEach { i ->
                    val d = ws.plusDays(i.toLong())
                    val sel = d == day
                    val isToday = d == LocalDate.now()
                    Column(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(if (sel) c.card else Color.Transparent)
                            .clickable { day = d }.padding(horizontal = 9.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(DateTimeFormatter.ofPattern("EEE", com.carlos.superhuman.data.PT).format(d).take(3).replaceFirstChar { it.uppercase() }, style = Type.small, color = c.ink2)
                        Spacer(Modifier.height(6.dp))
                        Text(d.dayOfMonth.toString(), style = Type.title, color = if (isToday) c.accent else c.ink)
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPill("‹ Semana") { day = day.minusWeeks(1) }
                SmallPill("Hoje", selected = day == LocalDate.now()) { day = LocalDate.now() }
                SmallPill("Semana ›") { day = day.plusWeeks(1) }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            if (!CalendarRepo.hasPermission(ctx)) {
                EmptyState("Sem acesso ao calendário", "Dá permissão de calendário nas definições do Android.")
            } else {
                AgendaTimeline(agenda, events.filter { it.allDay }) { s, e -> slot = s to e }
            }
        }
    }

    slot?.let { (s, e) ->
        AddEventSheet(day, s, e, onDismiss = { slot = null }, onCreated = { refresh++; slot = null })
    }
}

@Composable
fun AddEventSheet(day: LocalDate, startMin: Int, endMin: Int, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val ctx = LocalContext.current
    val c = SH.c
    val calendars = remember { CalendarRepo.calendars(ctx).filter { it.writable } }
    var title by remember { mutableStateOf("") }
    var d by remember { mutableStateOf(day) }
    var s by remember { mutableIntStateOf(startMin) }
    var e by remember { mutableIntStateOf(endMin) }
    var cal by remember { mutableStateOf<CalInfo?>(CalendarRepo.defaultCalendar(ctx)) }

    Sheet("Novo slot", onDismiss) {
        BareTextField(title, { title = it }, "Título do evento", Type.h2, Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Spacer(Modifier.height(10.dp))
        InsetBox(padding = 0.dp) {
            DetailRow(Icons.Rounded.CalendarMonth, "Data", com.carlos.superhuman.data.fmtDay(d), onClick = { pickDate(ctx, d) { d = it } })
            LineDivider()
            DetailRow(Icons.Rounded.Schedule, "Início", fmtMinutes(s), onClick = { pickTime(ctx, s) { s = it; if (e <= it) e = it + 30 } })
            LineDivider()
            DetailRow(Icons.Rounded.Schedule, "Fim", fmtMinutes(e), onClick = { pickTime(ctx, e) { e = it } })
        }
        Spacer(Modifier.height(12.dp))
        Text("Calendário", style = Type.small, color = c.ink2)
        Spacer(Modifier.height(8.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            calendars.forEach { ci ->
                val label = Store.data.settings.calendarLabels[ci.id.toString()] ?: ci.name
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (cal?.id == ci.id) c.ink else c.card)
                        .border(1.dp, c.line, RoundedCornerShape(50))
                        .clickable { cal = ci }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color(ci.color or 0xFF000000.toInt())))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = Type.small, color = if (cal?.id == ci.id) c.onInk else c.ink)
                }
            }
        }
        if (calendars.isEmpty()) Text("Sem calendários com escrita disponíveis.", style = Type.small, color = c.bad)
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Criar evento", Modifier.fillMaxWidth(), enabled = title.isNotBlank() && cal != null, icon = Icons.Rounded.Event) {
            val calId = cal?.id ?: return@PrimaryButton
            val begin = d.startMillis() + s * 60_000L
            val end = d.startMillis() + e * 60_000L
            val ok = CalendarRepo.insert(ctx, calId, title.trim(), begin, end)
            Toast.makeText(ctx, if (ok) "Evento criado" else "Não foi possível criar o evento", Toast.LENGTH_SHORT).show()
            if (ok) { CalendarRepo.cacheToday(ctx); onCreated() }
        }
    }
}
