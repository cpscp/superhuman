package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Goal
import com.carlos.superhuman.data.Milestone
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.BareTextField
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.DetailRow
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.ProgressLine
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHCheck
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.Tag
import com.carlos.superhuman.ui.components.pickDate
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val goalAreas = listOf("Carreira", "Finanças", "Saúde", "Pessoal", "Família", "Viagens", "Aprender", "Casa")

object GoalOps {
    fun upsert(g: Goal) = Store.update { d ->
        if (d.goals.any { it.id == g.id }) d.copy(goals = d.goals.map { if (it.id == g.id) g else it }) else d.copy(goals = d.goals + g)
    }
    fun delete(id: String) = Store.update { d -> d.copy(goals = d.goals.filter { it.id != id }) }
}

fun Goal.effectiveProgress(): Int =
    if (milestones.isNotEmpty()) (milestones.count { it.done } * 100 / milestones.size) else progress

fun timeLeft(day: Long?): String {
    if (day == null) return "Sem prazo"
    val d = LocalDate.ofEpochDay(day)
    val months = ChronoUnit.MONTHS.between(LocalDate.now(), d)
    val days = day - todayEpoch()
    return when {
        days < 0 -> "Prazo passou"
        days < 45 -> "faltam $days dias"
        months < 24 -> "faltam $months meses"
        else -> "faltam ${months / 12} anos"
    }
}

@Composable
fun GoalsScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    var edit by remember { mutableStateOf<Goal?>(null) }
    var area by remember { mutableStateOf<String?>(null) }
    val list = d.goals.filter { area == null || it.area == area }.sortedBy { it.targetDay ?: Long.MAX_VALUE }

    ScreenScaffold(
        title = "Objetivos",
        onBack = { Nav.back() },
        actions = { CircleIconButton(Icons.Rounded.Add, filled = true) { edit = Goal(title = "") } },
    ) {
        item {
            val avg = if (d.goals.isNotEmpty()) d.goals.sumOf { it.effectiveProgress() } / d.goals.size else 0
            SHCard(Modifier.fillMaxWidth()) {
                Text("Life goals", style = Type.small, color = c.ink2)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$avg%", style = Type.number, color = c.ink)
                    Spacer(Modifier.width(8.dp))
                    Text("progresso médio · ${d.goals.size} objetivos", style = Type.small, color = c.ink2, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(10.dp))
                ProgressLine(avg / 100f, height = 6.dp, color = c.accent)
            }
            Spacer(Modifier.height(12.dp))
            ChipRow(listOf("Todos") + goalAreas, setOf(area ?: "Todos")) { area = if (it == "Todos") null else it }
            SectionHeader("Linha temporal")
        }
        if (list.isEmpty()) item { EmptyState("Sem objetivos", "Define o que queres conquistar e até quando.") }
        items(list, key = { it.id }) { g ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.width(56.dp).padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(g.targetDay?.let { LocalDate.ofEpochDay(it).year.toString() } ?: "—", style = Type.tiny, color = c.ink2)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (g.effectiveProgress() >= 100) c.good else c.ink))
                }
                SHCard(Modifier.weight(1f), onClick = { Nav.go("goal", g.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.title, style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                        Tag(g.area, c.accent)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(timeLeft(g.targetDay), style = Type.small, color = c.ink2)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressLine(g.effectiveProgress() / 100f, Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        Text("${g.effectiveProgress()}%", style = Type.tiny, color = c.ink2)
                    }
                }
            }
        }
    }
    edit?.let { GoalSheet(it) { edit = null } }
}

@Composable
fun GoalDetailScreen(id: String?) {
    val d by Store.state.collectAsState()
    val c = SH.c
    val g = d.goals.firstOrNull { it.id == id }
    var edit by remember { mutableStateOf(false) }
    var newMs by remember { mutableStateOf("") }
    if (g == null) { ScreenScaffold("Objetivo", onBack = { Nav.back() }) { item { EmptyState("Não encontrado", "") } }; return }
    var slider by remember(g.id) { mutableFloatStateOf(g.progress.toFloat()) }

    ScreenScaffold(
        title = g.area,
        onBack = { Nav.back() },
        actions = { CircleIconButton(Icons.Rounded.Edit) { edit = true } },
    ) {
        item {
            Text(g.title, style = Type.h1, color = c.ink)
            if (g.why.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("Porquê: ${g.why}", style = Type.body, color = c.ink2) }
            Spacer(Modifier.height(14.dp))
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${g.effectiveProgress()}%", style = Type.number, color = c.ink, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(g.targetDay?.let { fmtDay(LocalDate.ofEpochDay(it)) } ?: "Sem prazo", style = Type.title, color = c.ink)
                        Text(timeLeft(g.targetDay), style = Type.small, color = c.ink2)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (g.milestones.isEmpty()) {
                    Slider(
                        value = slider, onValueChange = { slider = it },
                        onValueChangeFinished = { GoalOps.upsert(g.copy(progress = slider.toInt())) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = c.ink, activeTrackColor = c.ink, inactiveTrackColor = c.inset),
                    )
                    Text("Arrasta para atualizar, ou adiciona marcos para calcular automaticamente.", style = Type.tiny, color = c.ink3)
                } else ProgressLine(g.effectiveProgress() / 100f, height = 6.dp, color = c.accent)
            }
            SectionHeader("Marcos")
        }
        items(g.milestones, key = { it.id }) { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SHCheck(m.done) { GoalOps.upsert(g.copy(milestones = g.milestones.map { if (it.id == m.id) it.copy(done = !it.done) else it })) }
                Spacer(Modifier.width(12.dp))
                Text(m.title, style = Type.body, color = if (m.done) c.ink3 else c.ink, modifier = Modifier.weight(1f), textDecoration = if (m.done) TextDecoration.LineThrough else null)
                Icon(Icons.Rounded.Delete, null, tint = c.ink3, modifier = Modifier.size(18.dp).clickable {
                    GoalOps.upsert(g.copy(milestones = g.milestones.filter { it.id != m.id }))
                })
            }
        }
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SHTextField(newMs, { newMs = it }, "Novo marco", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.Add, filled = true, size = 46.dp) {
                    if (newMs.isNotBlank()) { GoalOps.upsert(g.copy(milestones = g.milestones + Milestone(title = newMs.trim()))); newMs = "" }
                }
            }
        }
    }
    if (edit) GoalSheet(g) { edit = false }
}

@Composable
fun GoalSheet(initial: Goal, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val exists = Store.data.goals.any { it.id == initial.id }
    var title by remember { mutableStateOf(initial.title) }
    var why by remember { mutableStateOf(initial.why) }
    var area by remember { mutableStateOf(initial.area) }
    var target by remember { mutableStateOf(initial.targetDay) }
    Sheet(if (exists) "Editar objetivo" else "Novo objetivo", onDismiss) {
        BareTextField(title, { title = it }, "Ex.: Correr uma meia maratona", Type.h2, Modifier.fillMaxWidth().padding(vertical = 6.dp))
        BareTextField(why, { why = it }, "Porque é importante para ti?", Type.small, Modifier.fillMaxWidth().padding(bottom = 12.dp))
        ChipRow(goalAreas, setOf(area)) { area = it }
        Spacer(Modifier.height(10.dp))
        InsetBox(padding = 0.dp) {
            DetailRow(Icons.Rounded.CalendarMonth, "Até quando", target?.let { fmtDay(LocalDate.ofEpochDay(it)) } ?: "Escolher", onClick = {
                pickDate(ctx, target?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().plusYears(1)) { target = it.toEpochDay() }
            })
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exists) SecondaryButton("Apagar") { GoalOps.delete(initial.id); onDismiss(); if (Nav.current.name == "goal") Nav.back() }
            PrimaryButton("Guardar", Modifier.weight(1f), enabled = title.isNotBlank()) {
                GoalOps.upsert(initial.copy(title = title.trim(), why = why, area = area, targetDay = target)); onDismiss()
            }
        }
    }
}
