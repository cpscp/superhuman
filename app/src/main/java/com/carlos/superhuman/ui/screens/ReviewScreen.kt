package com.carlos.superhuman.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Review
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.TaskItem
import com.carlos.superhuman.data.beersOn
import com.carlos.superhuman.data.endMillis
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtDayLong
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.data.today
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.data.weekKey
import com.carlos.superhuman.data.weekStart
import com.carlos.superhuman.data.weekStats
import com.carlos.superhuman.net.Claude
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.MiniStatRow
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHSwitch
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import kotlinx.coroutines.launch

private val moods = listOf("😫", "😕", "😐", "🙂", "🤩")

@Composable
fun ReviewScreen(weekly: Boolean) {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWeekly by remember { mutableStateOf(weekly) }
    val key = if (isWeekly) weekKey() else "D-${todayEpoch()}"
    val existing = d.reviews.firstOrNull { it.key == key }
    var mood by remember(key) { mutableIntStateOf(existing?.mood ?: 3) }
    var wins by remember(key) { mutableStateOf(existing?.wins ?: "") }
    var improve by remember(key) { mutableStateOf(existing?.improve ?: "") }
    var next by remember(key) { mutableStateOf(existing?.next ?: "") }
    var ai by remember(key) { mutableStateOf(existing?.aiSummary ?: "") }
    var aiBusy by remember { mutableStateOf(false) }
    var makeTasks by remember { mutableStateOf(true) }

    val t = today()
    val statLine: List<Pair<String, String>>
    val statsText: String
    if (isWeekly) {
        val w = d.weekStats()
        statLine = w.entries.take(3).map { it.key to it.value }
        val m = d.monthSummary()
        statsText = w.entries.joinToString(", ") { "${it.key}: ${it.value}" } + ". Plafond restante do mês: ${euro(m.remaining)}."
    } else {
        val start = t.startMillis(); val end = t.endMillis()
        val doneToday = d.tasks.count { (it.completedAt ?: 0L) in start until end }
        val spentToday = d.expenses.filter { !it.pending && it.time in start until end }.sumOf { it.amount }
        val habitsToday = d.habits.count { todayEpoch() in it.doneDays }
        statLine = listOf("Tasks feitas" to "$doneToday", "Gasto hoje" to euro(spentToday, false), "Cervejas" to "${d.beersOn(todayEpoch())}")
        statsText = "Tasks concluídas hoje: $doneToday. Pendentes: ${d.tasks.count { !it.done }}. Gasto hoje: ${euro(spentToday)}. Hábitos feitos: $habitsToday/${d.habits.size}. Cervejas hoje: ${d.beersOn(todayEpoch())}. Eventos: ${d.cachedEvents.joinToString { it.title }}."
    }

    ScreenScaffold(title = if (isWeekly) "Weekly review" else "Daily review", onBack = { Nav.back() }) {
        item {
            Segmented(listOf("Diária", "Semanal"), if (isWeekly) 1 else 0, Modifier.fillMaxWidth()) { isWeekly = it == 1 }
            Spacer(Modifier.height(10.dp))
            Text(if (isWeekly) "Semana de ${fmtDayLong(weekStart())}" else fmtDayLong(t), style = Type.small, color = c.ink2)
            Spacer(Modifier.height(10.dp))
            MiniStatRow(statLine)
            SectionHeader("Como correu?")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                moods.forEachIndexed { i, e ->
                    val sel = mood == i + 1
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(if (sel) c.ink else c.card).border(1.dp, c.line, CircleShape).clickable { mood = i + 1 },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, style = Type.h2) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(if (isWeekly) "Vitórias da semana" else "O que correu bem hoje", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(6.dp))
            SHTextField(wins, { wins = it }, "…", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
            Spacer(Modifier.height(12.dp))
            Text("O que melhorar", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(6.dp))
            SHTextField(improve, { improve = it }, "…", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
            Spacer(Modifier.height(12.dp))
            Text(if (isWeekly) "Prioridades da próxima semana (uma por linha)" else "Top 3 para amanhã (uma por linha)", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(6.dp))
            SHTextField(next, { next = it }, "…", Modifier.fillMaxWidth(), singleLine = false, minLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Criar tasks a partir destas linhas", style = Type.small, color = c.ink, modifier = Modifier.weight(1f))
                SHSwitch(makeTasks) { makeTasks = it }
            }

            Spacer(Modifier.height(14.dp))
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = c.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Reflexão da Claude", style = Type.title, color = c.ink)
                }
                Spacer(Modifier.height(8.dp))
                Text(ai.ifBlank { "Gera um resumo com padrões e uma sugestão concreta para amanhã/semana." }, style = Type.body, color = if (ai.isBlank()) c.ink2 else c.ink)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(if (aiBusy) "A pensar…" else "Gerar reflexão", Modifier.fillMaxWidth()) {
                    if (!Claude.hasKey()) { Toast.makeText(ctx, "Configura a chave da Claude API", Toast.LENGTH_SHORT).show(); return@SecondaryButton }
                    if (aiBusy) return@SecondaryButton
                    aiBusy = true
                    scope.launch {
                        ai = runCatching {
                            Claude.ask(
                                "És um coach de produtividade e bem-estar, em português de Portugal. Escreve 4-6 frases: o que correu bem, um padrão que notas nos dados, e 1-2 ações concretas. Tom direto e encorajador, sem floreados.",
                                "Revisão ${if (isWeekly) "semanal" else "diária"}. Humor ${mood}/5. Dados: $statsText\nVitórias: $wins\nMelhorar: $improve\nPróximos passos: $next",
                                maxTokens = 500,
                            )
                        }.getOrElse { "Erro: ${it.message}" }
                        aiBusy = false
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(if (existing != null) "Atualizar revisão" else "Guardar revisão", Modifier.fillMaxWidth()) {
                val r = Review(key = key, weekly = isWeekly, mood = mood, wins = wins, improve = improve, next = next, aiSummary = ai)
                val due = if (isWeekly) weekStart().plusWeeks(1).toEpochDay() else todayEpoch() + 1
                val newTasks = if (makeTasks && existing == null) next.lines().map { it.trim().trimStart('-', '•', '*', ' ') }.filter { it.isNotBlank() }
                    .map { TaskItem(title = it, dueDay = due, tags = listOf(if (isWeekly) "Semana" else "Amanhã")) } else emptyList()
                Store.update { s ->
                    s.copy(
                        reviews = s.reviews.filter { it.key != key } + r,
                        tasks = s.tasks + newTasks,
                    )
                }
                Toast.makeText(ctx, "Revisão guardada" + if (newTasks.isNotEmpty()) " · ${newTasks.size} tasks criadas" else "", Toast.LENGTH_SHORT).show()
            }
            SectionHeader("Histórico")
        }
        items(d.reviews.filter { it.weekly == isWeekly }.sortedByDescending { it.savedAt }.take(20)) { r ->
            SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(moods.getOrElse(r.mood - 1) { "😐" }, style = Type.h2)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (r.weekly) r.key.removePrefix("W-").replace("-", " · semana ") else fmtDayLong(java.time.LocalDate.ofEpochDay(r.key.removePrefix("D-").toLongOrNull() ?: 0)), style = Type.title, color = c.ink)
                        if (r.wins.isNotBlank()) Text(r.wins, style = Type.small, color = c.ink2, maxLines = 2)
                    }
                }
            }
        }
    }
}
