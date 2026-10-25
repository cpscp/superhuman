package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SportsBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.health.connect.client.PermissionController
import com.carlos.superhuman.data.Habit
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.beersOn
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.daysWithoutBeerStreak
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.data.weekStart
import com.carlos.superhuman.device.HealthRepo
import com.carlos.superhuman.device.HealthSnapshot
import com.carlos.superhuman.net.Claude
import com.carlos.superhuman.net.Strava
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.HatchBarChart
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.Ring
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHCheck
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HealthOps {
    fun addBeer(delta: Int, day: Long = todayEpoch()) = Store.update { d ->
        val k = day.toString()
        val v = ((d.beers[k] ?: 0) + delta).coerceAtLeast(0)
        d.copy(beers = d.beers + (k to v))
    }
    fun toggleHabit(id: String, day: Long = todayEpoch()) = Store.update { d ->
        d.copy(habits = d.habits.map { h ->
            if (h.id != id) h else h.copy(doneDays = if (day in h.doneDays) h.doneDays - day else (h.doneDays + day).takeLast(400))
        })
    }
    fun addHabit(name: String, emoji: String, target: Int = 7) = Store.update { d -> d.copy(habits = d.habits + Habit(name = name, emoji = emoji, targetPerWeek = target)) }
    fun deleteHabit(id: String) = Store.update { d -> d.copy(habits = d.habits.filter { it.id != id }) }

    val suggestions = listOf(
        "💧" to "Beber 2 L de água",
        "🚶" to "8 000 passos",
        "📖" to "Ler 15 minutos",
        "🧘" to "Alongar 5 minutos",
        "🌙" to "Deitar antes da meia-noite",
        "📵" to "Sem ecrãs 30 min antes de dormir",
        "🏋️" to "Treino",
        "🥗" to "Refeição com legumes",
        "🚫🍺" to "Dia sem álcool",
    )
}

@Composable
fun HealthScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<HealthSnapshot?>(null) }
    var granted by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var addHabit by remember { mutableStateOf(false) }
    var aiTips by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }
    val hcAvailable = remember { HealthRepo.available(ctx) }

    val permLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { reload++ }

    LaunchedEffect(reload) {
        if (hcAvailable) {
            granted = HealthRepo.granted(ctx)
            if (granted) snapshot = HealthRepo.read(ctx)
        }
        if (d.strava.refreshToken.isNotBlank()) Strava.refreshActivities()
    }

    ScreenScaffold(title = "Saúde", onBack = { Nav.back() }) {
        // Atividade
        item {
            val steps = snapshot?.stepsToday ?: 0L
            val goal = 8000L
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                        Ring(steps.toFloat() / goal, Modifier.size(92.dp), color = c.accent)
                        Icon(Icons.Rounded.DirectionsRun, null, tint = c.ink)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Passos hoje", style = Type.small, color = c.ink2)
                        Text(String.format("%,d", steps).replace(',', ' '), style = Type.number, color = c.ink)
                        Text("objetivo ${goal / 1000}k · ${"%.1f".format(snapshot?.distanceTodayKm ?: 0.0)} km", style = Type.small, color = c.ink2)
                    }
                }
                if (!hcAvailable) {
                    Spacer(Modifier.height(12.dp))
                    Text("O Health Connect não está disponível. Instala-o na Play Store para ligar Google Fit e Strava.", style = Type.small, color = c.ink2)
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Abrir Play Store", Modifier.fillMaxWidth()) {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) }
                    }
                } else if (!granted) {
                    Spacer(Modifier.height(12.dp))
                    Text("Liga o Health Connect para ler passos, sono e treinos (Google Fit e Strava escrevem lá).", style = Type.small, color = c.ink2)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Ligar Health Connect", Modifier.fillMaxWidth(), icon = Icons.Rounded.Favorite) { permLauncher.launch(HealthRepo.permissions) }
                } else snapshot?.let { s ->
                    if (s.steps7.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                        HatchBarChart(
                            values = s.steps7.map { it.toDouble() },
                            highlight = s.steps7.lastIndex,
                            labels = days.map { DateTimeFormatter.ofPattern("EEEEE", com.carlos.superhuman.data.PT).format(it).uppercase() },
                            height = 90.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val sleep = snapshot?.sleepLastNightMin ?: 0L
                SHCard(Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Bedtime, null, tint = c.ink)
                    Spacer(Modifier.height(14.dp))
                    Text("Sono", style = Type.small, color = c.ink2)
                    Text(if (sleep > 0) "${sleep / 60}h ${sleep % 60}m" else "—", style = Type.h2, color = c.ink)
                }
                SHCard(Modifier.weight(1f)) {
                    Icon(Icons.Rounded.DirectionsRun, null, tint = c.ink)
                    Spacer(Modifier.height(14.dp))
                    Text("Treinos 14 dias", style = Type.small, color = c.ink2)
                    Text("${(snapshot?.workouts?.size ?: 0).coerceAtLeast(d.stravaActivities.size)}", style = Type.h2, color = c.ink)
                }
            }
        }

        // Cervejas
        item {
            SectionHeader("Controlo de cervejas")
            val fb = d.beerFeedback()
            val todayCount = d.beersOn(todayEpoch())
            val color = when (fb.level) { 0 -> c.good; 1 -> c.warn; else -> c.bad }
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SportsBar, null, tint = c.accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hoje", style = Type.small, color = c.ink2)
                        Text("$todayCount", style = Type.number, color = c.ink)
                    }
                    CircleIconButton(Icons.Rounded.Remove, size = 46.dp) { HealthOps.addBeer(-1) }
                    Spacer(Modifier.width(10.dp))
                    CircleIconButton(Icons.Rounded.Add, filled = true, size = 46.dp) { HealthOps.addBeer(1) }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)).padding(12.dp)) {
                    Column {
                        Text(fb.title, style = Type.title, color = c.ink)
                        Text(fb.body, style = Type.small, color = c.ink2)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val ws = weekStart()
                val week = (0..6).map { d.beersOn(ws.plusDays(it.toLong()).toEpochDay()).toDouble() }
                HatchBarChart(
                    values = week,
                    highlight = (LocalDate.now().toEpochDay() - ws.toEpochDay()).toInt(),
                    labels = listOf("S", "T", "Q", "Q", "S", "S", "D"),
                    topLabels = week.map { it.toInt().toString() },
                    height = 70.dp,
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    Text("Semana: ${d.beersThisWeek()} / ${d.settings.beerWeeklyLimit}", style = Type.small, color = c.ink2, modifier = Modifier.weight(1f))
                    Text("${d.daysWithoutBeerStreak()} dias sem beber", style = Type.small, color = c.ink2)
                }
            }
        }

        // Hábitos
        item { SectionHeader("Hábitos", "+ Novo") { addHabit = true } }
        if (d.habits.isEmpty()) item {
            SHCard(Modifier.fillMaxWidth()) {
                Text("Sugestões para começar", style = Type.title, color = c.ink)
                Spacer(Modifier.height(10.dp))
                ChipRow(HealthOps.suggestions.map { "${it.first} ${it.second}" }, emptySet()) { label ->
                    val s = HealthOps.suggestions.first { "${it.first} ${it.second}" == label }
                    HealthOps.addHabit(s.second, s.first)
                }
            }
        }
        items(d.habits, key = { it.id }) { h -> HabitRow(h) }
        item {
            Spacer(Modifier.height(10.dp))
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = c.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Sugestões personalizadas", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                if (aiTips.isNotBlank()) Text(aiTips, style = Type.body, color = c.ink)
                else Text("A Claude analisa passos, sono, treinos, cervejas e hábitos e sugere 3 ajustes simples.", style = Type.small, color = c.ink2)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(if (aiLoading) "A pensar…" else "Pedir sugestões", Modifier.fillMaxWidth()) {
                    if (!Claude.hasKey()) { Toast.makeText(ctx, "Configura a chave da Claude API nas Definições", Toast.LENGTH_SHORT).show(); return@SecondaryButton }
                    if (aiLoading) return@SecondaryButton
                    aiLoading = true
                    scope.launch {
                        val s = snapshot
                        val ctxText = "Passos 7 dias: ${s?.steps7}. Sono 7 noites (min): ${s?.sleep7}. Treinos recentes: ${s?.workouts?.size ?: 0} + Strava ${d.stravaActivities.take(5).map { "${it.type} ${"%.1f".format(it.distanceKm)}km" }}. " +
                            "Cervejas esta semana: ${d.beersThisWeek()} (limite ${d.settings.beerWeeklyLimit}). Hábitos: ${d.habits.map { "${it.name} ${it.doneDays.count { x -> x >= weekStart().toEpochDay() }}/${it.targetPerWeek}" }}."
                        aiTips = runCatching {
                            Claude.ask(
                                "És um coach de bem-estar prático, em português de Portugal. Dá exatamente 3 sugestões curtas (1 frase cada, com emoji), concretas e realistas, baseadas nos dados. Sem diagnósticos médicos.",
                                ctxText, maxTokens = 400,
                            )
                        }.getOrElse { "Erro: ${it.message}" }
                        aiLoading = false
                    }
                }
            }
        }

        // Treinos
        item { SectionHeader("Treinos") }
        item {
            if (d.strava.refreshToken.isBlank()) {
                SHCard(Modifier.fillMaxWidth()) {
                    Text("Strava", style = Type.title, color = c.ink)
                    Text(
                        if (d.settings.stravaClientId.isBlank()) "Coloca o Client ID e Secret da tua app Strava nas Definições para ligar diretamente. (O Strava também pode sincronizar com o Health Connect.)"
                        else "Liga a tua conta para ver as atividades recentes.",
                        style = Type.small, color = c.ink2,
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Ligar Strava", Modifier.fillMaxWidth(), enabled = d.settings.stravaClientId.isNotBlank()) {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Strava.authorizeUrl()))) }
                    }
                }
            }
        }
        items(d.stravaActivities) { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.card), contentAlignment = Alignment.Center) {
                    Text(if (a.type.contains("Run")) "🏃" else if (a.type.contains("Ride")) "🚴" else if (a.type.contains("Walk")) "🚶" else "💪")
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = Type.body, color = c.ink)
                    Text("${a.startLocal.take(10)} · ${a.movingMin} min", style = Type.tiny, color = c.ink3)
                }
                Text("${"%.1f".format(a.distanceKm)} km", style = Type.title, color = c.ink)
            }
        }
        val hcWorkouts = snapshot?.workouts.orEmpty()
        items(hcWorkouts) { w ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.card), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DirectionsRun, null, tint = c.ink, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(w.title.ifBlank { HealthRepo.exerciseName(w.type) }, style = Type.body, color = c.ink)
                    Text("${HealthRepo.exerciseName(w.type)} · Health Connect", style = Type.tiny, color = c.ink3)
                }
                Text("${w.minutes} min", style = Type.title, color = c.ink)
            }
        }
        if (d.stravaActivities.isEmpty() && hcWorkouts.isEmpty()) item { EmptyState("Sem treinos recentes", "Os treinos do Strava e do Health Connect aparecem aqui.") }
    }

    if (addHabit) HabitSheet { addHabit = false }
}

@Composable
private fun HabitRow(h: Habit) {
    val c = SH.c
    val ws = weekStart()
    val doneToday = todayEpoch() in h.doneDays
    val weekCount = h.doneDays.count { it >= ws.toEpochDay() }
    SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(h.emoji, style = Type.h2)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, style = Type.title, color = c.ink)
                Text("$weekCount/${h.targetPerWeek} esta semana", style = Type.tiny, color = c.ink3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..6).forEach { i ->
                    val day = ws.plusDays(i.toLong()).toEpochDay()
                    Box(
                        Modifier.size(9.dp).clip(CircleShape)
                            .background(if (day in h.doneDays) c.ink else if (day > todayEpoch()) c.inset.copy(alpha = 0.5f) else c.inset)
                            .clickable { if (day <= todayEpoch()) HealthOps.toggleHabit(h.id, day) },
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            SHCheck(doneToday) { HealthOps.toggleHabit(h.id) }
        }
    }
}

@Composable
private fun HabitSheet(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var target by remember { mutableIntStateOf(7) }
    Sheet("Novo hábito", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SHTextField(emoji, { emoji = it.take(4) }, "✨", Modifier.width(70.dp))
            SHTextField(name, { name = it }, "Nome do hábito", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Vezes por semana", style = Type.small, color = SH.c.ink2)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (1..7).forEach { n -> SmallPill("$n", n == target) { target = n } } }
        Spacer(Modifier.height(12.dp))
        ChipRow(HealthOps.suggestions.map { "${it.first} ${it.second}" }, emptySet()) { label ->
            val s = HealthOps.suggestions.first { "${it.first} ${it.second}" == label }
            emoji = s.first; name = s.second
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Criar hábito", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { HealthOps.addHabit(name.trim(), emoji, target); onDismiss() }
    }
}
