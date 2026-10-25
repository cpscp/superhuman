package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Priority
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.SubTask
import com.carlos.superhuman.data.TaskItem
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.fmtMinutes
import com.carlos.superhuman.data.relativeDay
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.BareTextField
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.DetailRow
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHCheck
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.components.Tag
import com.carlos.superhuman.ui.components.pickDate
import com.carlos.superhuman.ui.components.pickTime
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate

object TaskOps {
    fun toggle(id: String) = Store.update { d ->
        d.copy(tasks = d.tasks.map {
            if (it.id == id) it.copy(done = !it.done, completedAt = if (!it.done) System.currentTimeMillis() else null) else it
        })
    }
    fun upsert(t: TaskItem) = Store.update { d ->
        val exists = d.tasks.any { it.id == t.id }
        val tags = (d.tags + t.tags).distinct()
        d.copy(tasks = if (exists) d.tasks.map { if (it.id == t.id) t else it } else d.tasks + t, tags = tags)
    }
    fun delete(id: String) = Store.update { d -> d.copy(tasks = d.tasks.filter { it.id != id }) }
}

@Composable
fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> SH.c.bad
    Priority.MEDIUM -> SH.c.warn
    Priority.LOW -> SH.c.good
}

fun priorityLabel(p: Priority) = when (p) { Priority.HIGH -> "Alta"; Priority.MEDIUM -> "Média"; Priority.LOW -> "Baixa" }

/** Cartão de task com barra vertical (estilo "Today's tasks"). */
@Composable
fun TaskRow(t: TaskItem, modifier: Modifier = Modifier) {
    val c = SH.c
    SHCard(modifier.fillMaxWidth(), padding = 0.dp, onClick = { Nav.go("task", t.id) }) {
        Row(Modifier.height(IntrinsicSize.Min).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (t.done) c.ink3 else c.ink))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, style = Type.title, color = if (t.done) c.ink3 else c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                )
                val status = when {
                    t.done -> "Concluída"
                    t.dueDay != null && t.dueDay < todayEpoch() -> "Atrasada"
                    else -> "Por fazer"
                }
                Text(status, style = Type.small, color = if (status == "Atrasada") c.bad else c.ink3)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(priorityLabel(t.priority), priorityColor(t.priority))
                    t.tags.take(2).forEach { Spacer(Modifier.width(6.dp)); Tag(it, c.ink2) }
                    Spacer(Modifier.weight(1f))
                    val due = t.dueDay?.let { relativeDay(it) + (t.dueMinutes?.let { m -> ": " + fmtMinutes(m) } ?: "") }
                    if (due != null) Text(due, style = Type.small, color = c.ink2)
                }
                if (t.subtasks.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("${t.subtasks.count { it.done }}/${t.subtasks.size} subtarefas", style = Type.tiny, color = c.ink3)
                }
            }
            Spacer(Modifier.width(12.dp))
            SHCheck(t.done) { TaskOps.toggle(t.id) }
        }
    }
}

@Composable
fun TasksScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    var filter by remember { mutableIntStateOf(0) } // 0 hoje, 1 próximas, 2 todas, 3 feitas
    var tagFilter by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortByPriority by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    val t0 = todayEpoch()

    val list = d.tasks.filter { t ->
        val f = when (filter) {
            0 -> !t.done && (t.dueDay == null || t.dueDay <= t0)
            1 -> !t.done && t.dueDay != null && t.dueDay > t0
            2 -> !t.done
            else -> t.done
        }
        f && (tagFilter.isEmpty() || t.tags.any { it in tagFilter })
    }.let { l ->
        if (filter == 3) l.sortedByDescending { it.completedAt ?: 0L }
        else if (sortByPriority) l.sortedWith(compareBy<TaskItem>({ -it.priority.ordinal }, { it.dueDay ?: Long.MAX_VALUE }, { it.dueMinutes ?: 9999 }))
        else l.sortedWith(compareBy<TaskItem>({ it.dueDay ?: Long.MAX_VALUE }, { it.dueMinutes ?: 9999 }))
    }

    ScreenScaffold(
        title = "Tasks",
        onBack = null,
        actions = { CircleIconButton(Icons.Rounded.Add, filled = true) { editing = TaskItem(title = "") } },
    ) {
        item {
            Segmented(listOf("Hoje", "Próximas", "Todas", "Feitas"), filter, Modifier.fillMaxWidth()) { filter = it }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipRow(d.tags, tagFilter, Modifier.weight(1f)) { tg -> tagFilter = if (tg in tagFilter) tagFilter - tg else tagFilter + tg }
                Spacer(Modifier.width(8.dp))
                SmallPill(if (sortByPriority) "↓ Prioridade" else "↓ Data") { sortByPriority = !sortByPriority }
            }
            Spacer(Modifier.height(14.dp))
            val open = d.tasks.count { !it.done }
            val high = d.tasks.count { !it.done && it.priority == Priority.HIGH }
            val late = d.tasks.count { !it.done && (it.dueDay ?: Long.MAX_VALUE) < t0 }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Abertas", open.toString(), Modifier.weight(1f))
                MiniStat("Alta prioridade", high.toString(), Modifier.weight(1f))
                MiniStat("Atrasadas", late.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
        }
        if (list.isEmpty()) item { EmptyState("Nada aqui", "Cria uma task no botão +.") }
        items(list, key = { it.id }) { t -> TaskRow(t, Modifier.padding(bottom = 10.dp)) }
    }

    editing?.let { TaskSheet(it, onDismiss = { editing = null }) }
}

@Composable
fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    SHCard(modifier, padding = 12.dp) {
        Text(value, style = Type.h2, color = SH.c.ink)
        Text(label, style = Type.tiny, color = SH.c.ink2, maxLines = 1)
    }
}

/** Criar / editar task — inspirado no ecrã "New task" do vídeo. */
@Composable
fun TaskSheet(initial: TaskItem, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val c = SH.c
    val d = Store.data
    val isNew = d.tasks.none { it.id == initial.id }
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var tags by remember { mutableStateOf(initial.tags.toSet()) }
    var due by remember { mutableStateOf(initial.dueDay ?: if (isNew) todayEpoch() else null) }
    var time by remember { mutableStateOf(initial.dueMinutes) }
    var prio by remember { mutableStateOf(initial.priority) }
    var newTag by remember { mutableStateOf("") }

    Sheet(if (isNew) "Nova task" else "Editar task", onDismiss) {
        BareTextField(title, { title = it }, "O que tens de fazer?", Type.h2, Modifier.fillMaxWidth().padding(top = 6.dp))
        BareTextField(notes, { notes = it }, "Detalhes, links, contexto…", Type.small, Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp))
        InsetBox(padding = 0.dp) {
            DetailRow(Icons.Rounded.CalendarMonth, "Data", due?.let { fmtDay(LocalDate.ofEpochDay(it)) } ?: "Sem data", onClick = {
                pickDate(ctx, due?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()) { due = it.toEpochDay() }
            })
            LineDivider()
            DetailRow(Icons.Rounded.Schedule, "Hora", time?.let { fmtMinutes(it) } ?: "—", onClick = { pickTime(ctx, time ?: 9 * 60) { time = it } })
            LineDivider()
            DetailRow(Icons.Rounded.Label, "Etiquetas", "", trailing = {
                Text(if (tags.isEmpty()) "Nenhuma" else tags.joinToString(", "), style = Type.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            })
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (d.tags + tags).distinct().forEach { tg -> SmallPill(tg, tg in tags) { tags = if (tg in tags) tags - tg else tags + tg } }
            }
            Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SHTextField(newTag, { newTag = it }, "Nova etiqueta", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.Add, size = 40.dp) { if (newTag.isNotBlank()) { tags = tags + newTag.trim(); newTag = "" } }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Flag, null, tint = c.ink2, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Prioridade", style = Type.body, color = c.ink2, modifier = Modifier.weight(1f))
            Segmented(listOf("Baixa", "Média", "Alta"), prio.ordinal, Modifier.width(210.dp)) { prio = Priority.entries[it] }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isNew) SecondaryButton("Apagar", icon = Icons.Rounded.Delete) { TaskOps.delete(initial.id); onDismiss(); if (Nav.current.name == "task") Nav.back() }
            else SecondaryButton("Cancelar") { onDismiss() }
            PrimaryButton(if (isNew) "Criar task" else "Guardar", Modifier.weight(1f), enabled = title.isNotBlank()) {
                TaskOps.upsert(initial.copy(title = title.trim(), notes = notes, tags = tags.toList(), dueDay = due, dueMinutes = time, priority = prio))
                onDismiss()
            }
        }
    }
}

@Composable
fun TaskDetailScreen(id: String?) {
    val d by Store.state.collectAsState()
    val c = SH.c
    val t = d.tasks.firstOrNull { it.id == id }
    var edit by remember { mutableStateOf(false) }
    var newSub by remember { mutableStateOf("") }
    if (t == null) {
        ScreenScaffold("Task", onBack = { Nav.back() }) { item { EmptyState("Task não encontrada", "Pode ter sido apagada.") } }
        return
    }
    ScreenScaffold(
        title = "Detalhe",
        onBack = { Nav.back() },
        actions = { CircleIconButton(Icons.Rounded.Edit) { edit = true } },
    ) {
        item {
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(t.title, style = Type.h1, color = c.ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    SHCheck(t.done) { TaskOps.toggle(t.id) }
                }
                if (t.notes.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(t.notes, style = Type.body, color = c.ink2) }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(priorityLabel(t.priority), priorityColor(t.priority))
                    t.tags.forEach { Tag(it, c.ink2) }
                }
            }
            Spacer(Modifier.height(10.dp))
            InsetBox(padding = 0.dp) {
                DetailRow(Icons.Rounded.CalendarMonth, "Data", t.dueDay?.let { fmtDay(LocalDate.ofEpochDay(it)) } ?: "Sem data", onClick = { edit = true })
                LineDivider()
                DetailRow(Icons.Rounded.Schedule, "Hora", t.dueMinutes?.let { fmtMinutes(it) } ?: "—", onClick = { edit = true })
                LineDivider()
                DetailRow(Icons.Rounded.Flag, "Estado", if (t.done) "Concluída" else "Por fazer")
            }
            SectionHeader("Subtarefas")
        }
        items(t.subtasks, key = { it.id }) { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SHCheck(s.done) {
                    TaskOps.upsert(t.copy(subtasks = t.subtasks.map { if (it.id == s.id) it.copy(done = !it.done) else it }))
                }
                Spacer(Modifier.width(12.dp))
                Text(s.title, style = Type.body, color = if (s.done) c.ink3 else c.ink, modifier = Modifier.weight(1f),
                    textDecoration = if (s.done) TextDecoration.LineThrough else null)
                Icon(Icons.Rounded.Delete, null, tint = c.ink3, modifier = Modifier.size(18.dp).clickable { TaskOps.upsert(t.copy(subtasks = t.subtasks.filter { it.id != s.id })) })
            }
        }
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SHTextField(newSub, { newSub = it }, "Adicionar subtarefa", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.Add, filled = true, size = 46.dp) {
                    if (newSub.isNotBlank()) { TaskOps.upsert(t.copy(subtasks = t.subtasks + SubTask(title = newSub.trim()))); newSub = "" }
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Editar task", Modifier.fillMaxWidth()) { edit = true }
        }
    }
    if (edit) TaskSheet(t, onDismiss = { edit = false })
}
