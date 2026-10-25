package com.carlos.superhuman.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.AppData
import com.carlos.superhuman.data.Contribution
import com.carlos.superhuman.data.EtfCatalog
import com.carlos.superhuman.data.Holding
import com.carlos.superhuman.data.InvestContent
import com.carlos.superhuman.data.InvestPlanner
import com.carlos.superhuman.data.InvestProfile
import com.carlos.superhuman.data.Lesson
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.riskExplain
import com.carlos.superhuman.data.riskNames
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.net.Market
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.ProgressLine
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHCheck
import com.carlos.superhuman.ui.components.SHSwitch
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.components.Tag
import com.carlos.superhuman.ui.components.parseMoney
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

private fun stepKey(i: Int) = "step-$i"

private fun toggleDone(id: String) = Store.update { d ->
    d.copy(lessonsDone = if (id in d.lessonsDone) d.lessonsDone - id else d.lessonsDone + id)
}

// ======================= Separador "Plano" =======================
fun LazyListScope.planTab(
    d: AppData,
    onRegister: (String) -> Unit,
    onProfile: () -> Unit,
    onLesson: (Lesson) -> Unit,
    onTab: (Int) -> Unit,
) {
    val p = d.investProfile
    if (!p.set) {
        item { OnboardingCard(d, onProfile, onLesson) }
        item { TipCard() }
        return
    }
    val plan = InvestPlanner.build(p, d)

    // Este mês
    item {
        val c = SH.c
        val invested = InvestPlanner.investedThisMonth(d)
        val target = plan.investMonthly
        val today = LocalDate.now()
        val day = p.monthlyDay.coerceAtMost(YearMonth.now().lengthOfMonth())
        val done = target > 0 && invested >= target - 0.5
        SHCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Este mês", style = Type.small, color = c.ink2)
                    Text("${euro(invested, false)} de ${euro(target, false)}", style = Type.number, color = c.ink)
                    Text(
                        when {
                            target <= 0 -> "Sem investimento mensal definido."
                            done -> "Feito ✓ Próximo: dia $day de ${YearMonth.now().plusMonths(1).month.getDisplayName(java.time.format.TextStyle.FULL, com.carlos.superhuman.data.PT)}."
                            today.dayOfMonth < day -> "Dia de investir: $day (faltam ${day - today.dayOfMonth} dias)."
                            else -> "Já passou o dia $day: ainda faltam ${euro(target - invested, false)}."
                        },
                        style = Type.small, color = if (!done && today.dayOfMonth >= day && target > 0) c.accent else c.ink2,
                    )
                }
                val streak = InvestPlanner.monthStreak(d)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥 $streak", style = Type.h2, color = c.ink)
                    Text(if (streak == 1) "mês seguido" else "meses seguidos", style = Type.tiny, color = c.ink3)
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressLine(if (target > 0) (invested / target).toFloat() else 0f, height = 6.dp, color = if (done) c.good else c.ink)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(if (done) "Registar outro investimento" else "Registar investimento", Modifier.fillMaxWidth(), icon = Icons.Rounded.Check) { onRegister("") }
        }
    }

    // Próxima compra / reequilíbrio
    item {
        val c = SH.c
        val reb = InvestPlanner.rebalance(d, plan)
        Spacer(Modifier.height(10.dp))
        SHCard(Modifier.fillMaxWidth()) {
            Text("O que comprar este mês", style = Type.title, color = c.ink)
            Spacer(Modifier.height(10.dp))
            if (reb != null) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.warn.copy(alpha = 0.12f)).padding(10.dp)) {
                    Text("⚖️ ${reb.message}", style = Type.small, color = c.ink)
                }
                Spacer(Modifier.height(10.dp))
            }
            val tgt = reb?.target
            val split = if (tgt != null) listOf(tgt to plan.investMonthly) else plan.monthlySplit
            split.forEach { (etf, v) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onRegister(etf.symbol) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(c.inset), contentAlignment = Alignment.Center) { Text(etf.ticker.take(4), style = Type.tiny, color = c.ink) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(etf.ticker + " · " + etf.kind, style = Type.body, color = c.ink)
                        Text("ISIN ${etf.isin}", style = Type.tiny, color = c.ink3)
                    }
                    Text(euro(v, false), style = Type.h2, color = c.ink)
                }
            }
            if (plan.emergencyMonthly > 0) {
                Row(Modifier.fillMaxWidth().clickable { onRegister("FUNDO") }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(c.inset), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Savings, null, tint = c.ink, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Text("Fundo de emergência", style = Type.body, color = c.ink, modifier = Modifier.weight(1f))
                    Text(euro(plan.emergencyMonthly, false), style = Type.h2, color = c.ink)
                }
            }
            Text("Toca numa linha para registar essa compra.", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 4.dp))
        }
    }

    // Dica do dia
    item { Spacer(Modifier.height(10.dp)); TipCard(d.investFeed?.takeIf { it.date == LocalDate.now().toString() }?.beginnerNote) }

    // Fundo de emergência
    if (!p.hasEmergencyFund && p.emergencyTarget > 0) item {
        val c = SH.c
        Spacer(Modifier.height(10.dp))
        SHCard(Modifier.fillMaxWidth(), onClick = { onRegister("FUNDO") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Savings, null, tint = c.accent)
                Spacer(Modifier.width(8.dp))
                Text("Fundo de emergência", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                Text("${euro(p.emergencySaved, false)} / ${euro(p.emergencyTarget, false)}", style = Type.small, color = c.ink2)
            }
            Spacer(Modifier.height(10.dp))
            ProgressLine((p.emergencySaved / p.emergencyTarget).toFloat(), height = 6.dp, color = c.good)
            Text(
                if (p.emergencySaved >= p.emergencyTarget) "Completo ✓ A partir de agora, todo o valor mensal vai para os ETF." else "Conta poupança ou Certificados de Aforro. Toca para registar um depósito.",
                style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    // O teu plano
    item {
        SectionHeader("O teu plano · ${riskNames[plan.effectiveRisk]}", "Alterar") { onProfile() }
        PlanCard(plan)
    }

    // Projeção
    item {
        val c = SH.c
        Spacer(Modifier.height(10.dp))
        val years = p.horizonYears.coerceIn(1, 40)
        val base = plan.expectedBase
        val init = plan.investNow
        val monthly = plan.investMonthly
        val pess = InvestPlanner.project(init, monthly, years, base - 4)
        val mid = InvestPlanner.project(init, monthly, years, base)
        val opt = InvestPlanner.project(init, monthly, years, base + 2)
        val putIn = init + monthly * 12 * years
        SHCard(Modifier.fillMaxWidth()) {
            Text("Quanto pode valer em $years ${if (years == 1) "ano" else "anos"}", style = Type.title, color = c.ink)
            Text("Hipotético, não garantido. Retornos passados não garantem os futuros.", style = Type.tiny, color = c.ink3)
            Spacer(Modifier.height(12.dp))
            ProjectionChart(listOf(pess, mid, opt), putIn)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjFact("Puseste", euro(putIn, false), c.ink2, Modifier.weight(1f))
                ProjFact("Pessimista ${"%.1f".format(base - 4)}%", euro(pess.last(), false), c.ink3, Modifier.weight(1f))
                ProjFact("Base ${"%.1f".format(base)}%", euro(mid.last(), false), c.accent, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("Otimista (${"%.1f".format(base + 2)}%/ano): ${euro(opt.last(), false)}. Num ano mau a carteira pode cair temporariamente ${listOf("~15%", "~25%", "~35%")[plan.effectiveRisk]} ou mais.", style = Type.small, color = c.ink2)
        }
    }

    if (plan.notes.isNotEmpty()) item {
        val c = SH.c
        Spacer(Modifier.height(10.dp))
        SHCard(Modifier.fillMaxWidth()) {
            Text("Notas do teu plano", style = Type.title, color = c.ink)
            plan.notes.forEach { Text("• $it", style = Type.small, color = c.ink, modifier = Modifier.padding(top = 8.dp)) }
        }
    }

    // Passos
    item {
        val c = SH.c
        SectionHeader("Passos para começar")
        val steps = listOf(
            Triple("Ter o fundo de emergência", if (p.hasEmergencyFund) "Já tens ✓" else "Meta: ${euro(p.emergencyTarget, false)}", "emergencia"),
            Triple("Escolher uma corretora", "Regulada, barata e com planos automáticos", "corretora"),
            Triple("Abrir a conta e transferir", "Normalmente pede CC, NIF e IBAN; demora 1–3 dias", null),
            Triple("Primeira compra", plan.nowSplit.joinToString(" + ") { "${euro(it.second, false)} ${it.first.ticker}" }.ifBlank { "Começa pelo valor mensal" }, null),
            Triple("Ativar plano automático mensal", "Dia ${p.monthlyDay}: " + plan.monthlySplit.joinToString(" + ") { "${euro(it.second, false)} ${it.first.ticker}" }, "dca"),
            Triple("Esquecer até ao próximo mês", "Olha para a carteira pouco; regista as compras aqui", "comportamento"),
        )
        InsetBox(padding = 0.dp) {
            steps.forEachIndexed { i, (title, sub, lessonId) ->
                if (i > 0) LineDivider()
                val done = stepKey(i) in d.lessonsDone || (i == 0 && p.hasEmergencyFund)
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    SHCheck(done) { toggleDone(stepKey(i)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1}. $title", style = Type.body, color = if (done) c.ink3 else c.ink)
                        Text(sub, style = Type.tiny, color = c.ink3)
                    }
                    if (lessonId != null) {
                        Icon(Icons.Rounded.School, null, tint = c.ink2, modifier = Modifier.size(20.dp).clickable {
                            InvestContent.lessons.firstOrNull { it.id == lessonId }?.let(onLesson)
                        })
                    }
                }
            }
        }
    }
    item {
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Aprender o essencial (${InvestContent.lessons.count { it.id in d.lessonsDone }}/${InvestContent.lessons.size})", Modifier.fillMaxWidth(), icon = Icons.Rounded.School) { onTab(1) }
        Text("Informação educativa, não aconselhamento financeiro personalizado. Decide sempre com base na tua situação.", style = Type.tiny, color = SH.c.ink3, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun OnboardingCard(d: AppData, onProfile: () -> Unit, onLesson: (Lesson) -> Unit) {
    val c = SH.c
    SHCard(Modifier.fillMaxWidth()) {
        Text("Começar a investir", style = Type.h1, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text("Diz quanto queres investir e a app monta um plano simples, com 1 ou 2 ETF, quanto pôr em cada um e todos os passos. Demora 1 minuto.", style = Type.body, color = c.ink2)
        Spacer(Modifier.height(14.dp))
        listOf("💶" to "Escolhes o valor: de uma vez e/ou por mês", "🧺" to "Recebes um plano com ETF globais baratos", "📅" to "A app lembra-te de investir todos os meses", "📚" to "Aprendes o essencial em lições de 2 minutos")
            .forEach { (e, t) -> Row(Modifier.padding(vertical = 4.dp)) { Text(e); Spacer(Modifier.width(10.dp)); Text(t, style = Type.small, color = c.ink) } }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Criar o meu plano", Modifier.fillMaxWidth()) { onProfile() }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Primeiro, porque investir?", Modifier.fillMaxWidth()) { onLesson(InvestContent.lessons.first()) }
    }
}

@Composable
private fun TipCard(override: String? = null) {
    val c = SH.c
    var offset by remember { mutableIntStateOf(0) }
    SHCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Dica do dia", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
            Text("Outra ›", style = Type.small, color = c.ink2, modifier = Modifier.clickable { offset++ }.padding(4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(if (offset == 0 && !override.isNullOrBlank()) override else InvestPlanner.tipOfDay(offset), style = Type.body, color = c.ink)
    }
}

@Composable
private fun PlanCard(plan: com.carlos.superhuman.data.PlanResult) {
    val c = SH.c
    var open by remember { mutableStateOf<String?>(null) }
    val colors = listOf(c.ink, c.accent, c.ink3)
    SHCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50))) {
            plan.allocations.forEachIndexed { i, a -> Box(Modifier.weight(a.weight.toFloat()).height(14.dp).background(colors[i % 3])) }
        }
        Spacer(Modifier.height(12.dp))
        plan.allocations.forEachIndexed { i, a ->
            val now = plan.nowSplit.firstOrNull { it.first == a.etf }?.second ?: 0.0
            val monthly = plan.monthlySplit.firstOrNull { it.first == a.etf }?.second ?: 0.0
            Column(Modifier.fillMaxWidth().clickable { open = if (open == a.etf.symbol) null else a.etf.symbol }.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(colors[i % 3]))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${(a.weight * 100).roundToInt()}% · ${a.etf.ticker}", style = Type.title, color = c.ink)
                        Text(a.etf.kind, style = Type.tiny, color = c.ink3)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (now > 0) Text("${euro(now, false)} já", style = Type.small, color = c.ink)
                        if (monthly > 0) Text("${euro(monthly, false)}/mês", style = Type.small, color = c.ink2)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = c.ink3, modifier = Modifier.size(18.dp))
                }
                if (open == a.etf.symbol) {
                    Spacer(Modifier.height(8.dp))
                    InsetBox(Modifier.fillMaxWidth(), padding = 12.dp) {
                        Text(a.etf.name, style = Type.small, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(a.etf.simple, style = Type.small, color = c.ink2)
                        Spacer(Modifier.height(8.dp))
                        Text("ISIN ${a.etf.isin} · Custo ${a.etf.ter} · Ticker ${a.etf.ticker}", style = Type.tiny, color = c.ink3)
                    }
                }
            }
        }
        if (plan.investNow > 0 || plan.emergencyNow > 0) {
            LineDivider()
            Text(
                "Do valor inicial: ${euro(plan.investNow, false)} para ETF" + if (plan.emergencyNow > 0) " e ${euro(plan.emergencyNow, false)} para o fundo de emergência." else ".",
                style = Type.small, color = c.ink2, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ProjFact(label: String, value: String, color: Color, modifier: Modifier) {
    InsetBox(modifier, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(5.dp))
            Text(label, style = Type.tiny, color = SH.c.ink2, maxLines = 1)
        }
        Text(value, style = Type.title, color = SH.c.ink, maxLines = 1)
    }
}

@Composable
private fun ProjectionChart(series: List<List<Double>>, contributed: Double) {
    val c = SH.c
    val colors = listOf(c.ink3, c.accent, c.ink2)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val maxV = series.flatten().maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val n = series.first().size
        fun x(i: Int) = if (n <= 1) 0f else i * size.width / (n - 1)
        fun y(v: Double) = size.height - (v / maxV).toFloat() * (size.height - 6f)
        listOf(0.25f, 0.5f, 0.75f).forEach { f -> drawLine(c.line, Offset(0f, size.height * f), Offset(size.width, size.height * f), 1.5f) }
        // linha do que puseste (aprox. linear)
        val first = series.first().first()
        drawLine(c.ink3, Offset(0f, y(first)), Offset(size.width, y(contributed)), 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        series.forEachIndexed { si, s ->
            val p = Path()
            s.forEachIndexed { i, v -> if (i == 0) p.moveTo(x(i), y(v)) else p.lineTo(x(i), y(v)) }
            drawPath(p, colors[si], style = Stroke(if (si == 1) 5f else 3f, cap = StrokeCap.Round))
        }
        drawCircle(c.accent, 8f, Offset(x(n - 1), y(series[1].last())))
    }
}

// ======================= Separador "Aprender" =======================
fun LazyListScope.learnTab(d: AppData, onLesson: (Lesson) -> Unit) {
    item {
        val c = SH.c
        val done = InvestContent.lessons.count { it.id in d.lessonsDone }
        SHCard(Modifier.fillMaxWidth()) {
            Text("O essencial para investir", style = Type.title, color = c.ink)
            Text("${InvestContent.lessons.size} lições curtas · $done lidas", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(10.dp))
            ProgressLine(done.toFloat() / InvestContent.lessons.size, height = 6.dp, color = c.accent)
        }
        d.investFeed?.takeIf { it.lessonTitle.isNotBlank() }?.let { f ->
            Spacer(Modifier.height(10.dp))
            SHCard(Modifier.fillMaxWidth()) {
                Text("Lição do dia · ${f.date}", style = Type.tiny, color = c.accent)
                Text(f.lessonTitle, style = Type.title, color = c.ink, modifier = Modifier.padding(top = 4.dp))
                Text(f.lessonBody, style = Type.body, color = c.ink2, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    items(InvestContent.lessons, key = { it.id }) { l ->
        val c = SH.c
        val done = l.id in d.lessonsDone
        SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { onLesson(l) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.inset), contentAlignment = Alignment.Center) { Text(l.emoji, style = Type.h2) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.title, style = Type.title, color = c.ink)
                    Text("${l.minutes} min", style = Type.tiny, color = c.ink3)
                }
                if (done) Tag("Lida", c.good) else Icon(Icons.Rounded.ChevronRight, null, tint = c.ink3)
            }
        }
    }
}

@Composable
fun LessonSheet(l: Lesson, onDismiss: () -> Unit) {
    val c = SH.c
    val done = l.id in Store.data.lessonsDone
    Sheet("${l.emoji} ${l.title}", onDismiss) {
        l.body.forEach { Text(it, style = Type.body, color = c.ink, modifier = Modifier.padding(bottom = 12.dp)) }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.12f)).padding(12.dp)) {
            Text("Em resumo: ${l.takeaway}", style = Type.small, color = c.ink)
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(if (done) "Lida ✓" else "Marcar como lida", Modifier.fillMaxWidth()) {
            if (!done) toggleDone(l.id)
            onDismiss()
        }
    }
}

// ======================= Perfil / valor a investir =======================
@Composable
fun InvestProfileSheet(onDismiss: () -> Unit) {
    val d = Store.data
    val c = SH.c
    val p0 = d.investProfile
    var oneOff by remember { mutableStateOf(if (p0.oneOff > 0) p0.oneOff.roundToInt().toString() else "") }
    var monthly by remember { mutableStateOf(if (p0.monthly > 0) p0.monthly.roundToInt().toString() else "") }
    var horizon by remember { mutableIntStateOf(p0.horizonYears) }
    var risk by remember { mutableIntStateOf(p0.risk) }
    var hasFund by remember { mutableStateOf(p0.hasEmergencyFund) }
    var fundTarget by remember { mutableStateOf((if (p0.emergencyTarget > 0) p0.emergencyTarget else InvestPlanner.suggestedEmergency(d)).roundToInt().toString()) }
    var fundSaved by remember { mutableStateOf(if (p0.emergencySaved > 0) p0.emergencySaved.roundToInt().toString() else "") }
    var day by remember { mutableIntStateOf(p0.monthlyDay) }
    var reminders by remember { mutableStateOf(p0.remindersOn) }
    val cap = remember { InvestPlanner.monthlyCapacity(d) }

    val ok = (parseMoney(oneOff) ?: 0.0) + (parseMoney(monthly) ?: 0.0) > 0
    Sheet(if (p0.set) "Alterar plano" else "Criar o meu plano", onDismiss, footer = {
        PrimaryButton(if (p0.set) "Guardar plano" else "Ver o meu plano", Modifier.fillMaxWidth(), enabled = ok) {
            Store.update {
                it.copy(investProfile = InvestProfile(
                    set = true,
                    oneOff = parseMoney(oneOff) ?: 0.0,
                    monthly = parseMoney(monthly) ?: 0.0,
                    horizonYears = horizon, risk = risk,
                    hasEmergencyFund = hasFund,
                    emergencyTarget = if (hasFund) 0.0 else parseMoney(fundTarget) ?: 0.0,
                    emergencySaved = if (hasFund) 0.0 else parseMoney(fundSaved) ?: 0.0,
                    monthlyDay = day, goal = p0.goal, remindersOn = reminders,
                ))
            }
            onDismiss()
        }
        if (!ok) Text("Escreve pelo menos um valor.", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
    }) {
        Text("1. Quanto queres investir?", style = Type.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("De uma vez (€)", style = Type.tiny, color = c.ink2)
                SHTextField(oneOff, { oneOff = it }, "ex.: 1000", Modifier.fillMaxWidth().padding(top = 4.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Column(Modifier.weight(1f)) {
                Text("Por mês (€)", style = Type.tiny, color = c.ink2)
                SHTextField(monthly, { monthly = it }, "ex.: 150", Modifier.fillMaxWidth().padding(top = 4.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        if (cap != null && cap > 0) Text("Nos últimos meses sobraram-te em média ${euro(cap, false)} do plafond.", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(50, 100, 150, 250, 500).forEach { v -> SmallPill("$v €", monthly == v.toString()) { monthly = v.toString() } }
        }

        Spacer(Modifier.height(18.dp))
        Text("2. Durante quanto tempo?", style = Type.title, color = c.ink)
        Text("Quando vais precisar deste dinheiro?", style = Type.tiny, color = c.ink3)
        Spacer(Modifier.height(8.dp))
        ChipRow(listOf("< 3 anos", "3–5 anos", "5–10 anos", "10–20 anos", "20+ anos"), setOf(
            when { horizon < 3 -> "< 3 anos"; horizon < 5 -> "3–5 anos"; horizon < 10 -> "5–10 anos"; horizon < 20 -> "10–20 anos"; else -> "20+ anos" }
        )) { horizon = when (it) { "< 3 anos" -> 2; "3–5 anos" -> 4; "5–10 anos" -> 7; "10–20 anos" -> 15; else -> 25 } }

        Spacer(Modifier.height(18.dp))
        Text("3. Como reages a quedas?", style = Type.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        riskNames.forEachIndexed { i, name ->
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (risk == i) c.ink else c.inset).clickable { risk = i }.padding(12.dp),
            ) {
                Text(name, style = Type.title, color = if (risk == i) c.onInk else c.ink)
                Text(riskExplain[i], style = Type.small, color = if (risk == i) c.onInk.copy(alpha = 0.8f) else c.ink2)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("4. Fundo de emergência", style = Type.title, color = c.ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Já tenho 3–6 meses de despesas de lado", style = Type.small, color = c.ink, modifier = Modifier.weight(1f))
            SHSwitch(hasFund) { hasFund = it }
        }
        if (!hasFund) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Meta (€)", style = Type.tiny, color = c.ink2)
                    SHTextField(fundTarget, { fundTarget = it }, "3000", Modifier.fillMaxWidth().padding(top = 4.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Column(Modifier.weight(1f)) {
                    Text("Já tenho (€)", style = Type.tiny, color = c.ink2)
                    SHTextField(fundSaved, { fundSaved = it }, "0", Modifier.fillMaxWidth().padding(top = 4.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
            Text("Sugestão: ~4 meses das tuas despesas registadas.", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.height(18.dp))
        Text("5. Dia do mês para investir", style = Type.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 5, 10, 15, 25, 28).forEach { v -> SmallPill("$v", day == v) { day = v } }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lembrar-me nesse dia", style = Type.small, color = c.ink, modifier = Modifier.weight(1f))
            SHSwitch(reminders) { reminders = it }
        }

    }
}

// ======================= Registar investimento =======================
@Composable
fun RegisterInvestSheet(preset: String, onDismiss: () -> Unit) {
    val d = Store.data
    val c = SH.c
    val ctx = LocalContext.current
    val plan = if (d.investProfile.set) InvestPlanner.build(d.investProfile, d) else null
    val options = (plan?.allocations?.map { it.etf.symbol } ?: emptyList()) + d.holdings.map { it.symbol } +
        (if (d.investProfile.set && !d.investProfile.hasEmergencyFund) listOf("FUNDO") else emptyList())
    val opts = options.distinct()
    var symbol by remember { mutableStateOf(preset.ifBlank { opts.firstOrNull() ?: "" }) }
    var other by remember { mutableStateOf("") }
    val suggested = plan?.let { pl -> (pl.monthlySplit.firstOrNull { it.first.symbol == symbol }?.second) ?: if (symbol == "FUNDO") pl.emergencyMonthly else null }
    var amount by remember(symbol) { mutableStateOf(suggested?.takeIf { it > 0 }?.roundToInt()?.toString() ?: "") }
    var price by remember(symbol) { mutableStateOf("") }
    var loadingPrice by remember { mutableStateOf(false) }
    val effective = if (symbol == "OUTRO") other.trim().uppercase() else symbol

    LaunchedEffect(effective) {
        if (effective.isNotBlank() && effective != "FUNDO") {
            val q = d.quotes[effective]
            if (q != null) price = "%.2f".format(q.price)
            else { loadingPrice = true; Market.quote(effective)?.let { price = "%.2f".format(it.price) }; loadingPrice = false }
        }
    }

    fun label(s: String) = when (s) { "FUNDO" -> "Fundo emergência"; "OUTRO" -> "Outro"; else -> EtfCatalog.bySymbol(s)?.ticker ?: s }

    Sheet("Registar investimento", onDismiss) {
        ChipRow(opts.map { label(it) } + "Outro", setOf(label(symbol))) { lbl ->
            symbol = if (lbl == "Outro") "OUTRO" else opts.first { label(it) == lbl }
        }
        if (symbol == "OUTRO") {
            Spacer(Modifier.height(8.dp))
            SHTextField(other, { other = it }, "Símbolo Yahoo (ex.: VUAA.L, AAPL)", Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        SHTextField(amount, { amount = it }, "Valor investido (€)", Modifier.fillMaxWidth(), style = Type.h2, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        if (symbol != "FUNDO") {
            Spacer(Modifier.height(8.dp))
            SHTextField(price, { price = it }, if (loadingPrice) "A obter preço…" else "Preço por unidade", Modifier.fillMaxWidth(), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            val a = parseMoney(amount); val pr = parseMoney(price)
            if (a != null && pr != null && pr > 0) Text("≈ ${"%.4f".format(a / pr)} unidades", style = Type.small, color = c.ink2, modifier = Modifier.padding(top = 6.dp))
            Text("Usa o preço a que compraste na corretora (frações são normais).", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(18.dp))
        val a = parseMoney(amount)
        val pr = parseMoney(price)
        PrimaryButton("Guardar", Modifier.fillMaxWidth(), enabled = a != null && a > 0 && effective.isNotBlank() && (effective == "FUNDO" || (pr != null && pr > 0))) {
            val amt = a ?: return@PrimaryButton
            if (effective == "FUNDO") {
                Store.update { s ->
                    s.copy(
                        investProfile = s.investProfile.copy(emergencySaved = s.investProfile.emergencySaved + amt),
                        contributions = s.contributions + Contribution(epochDay = todayEpoch(), amount = amt, symbol = "FUNDO"),
                    )
                }
                Toast.makeText(ctx, "Fundo de emergência +${euro(amt, false)}", Toast.LENGTH_SHORT).show()
            } else {
                val unit = pr ?: return@PrimaryButton
                val qty = amt / unit
                val name = EtfCatalog.bySymbol(effective)?.name ?: ""
                Store.update { s ->
                    val existing = s.holdings.firstOrNull { it.symbol.equals(effective, true) }
                    val holdings = if (existing != null) {
                        val newQty = existing.quantity + qty
                        s.holdings.map { if (it.id == existing.id) it.copy(quantity = newQty, avgPrice = (existing.avgPrice * existing.quantity + amt) / newQty) else it }
                    } else s.holdings + Holding(symbol = effective, name = name, quantity = qty, avgPrice = unit)
                    s.copy(holdings = holdings, contributions = s.contributions + Contribution(epochDay = todayEpoch(), amount = amt, symbol = effective))
                }
                Toast.makeText(ctx, "Registado: ${euro(amt, false)} em ${label(effective)}", Toast.LENGTH_SHORT).show()
            }
            onDismiss()
        }
        val recent = d.contributions.sortedByDescending { it.epochDay }.take(4)
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Últimos registos", style = Type.small, color = c.ink2)
            recent.forEach {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("${fmtDay(LocalDate.ofEpochDay(it.epochDay))} · ${label(it.symbol)}", style = Type.small, color = c.ink, modifier = Modifier.weight(1f))
                    Text(euro(it.amount, false), style = Type.small, color = c.ink)
                }
            }
        }
    }
}
