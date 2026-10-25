package com.carlos.superhuman.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

val PT: Locale = Locale("pt", "PT")
val zone: ZoneId get() = ZoneId.systemDefault()

fun today(): LocalDate = LocalDate.now()
fun todayEpoch(): Long = today().toEpochDay()
fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun LocalDate.startMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()
fun LocalDate.endMillis(): Long = plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
fun monthKey(ym: YearMonth): String = ym.toString() // "2026-10"
fun weekStart(d: LocalDate = today()): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
fun weekKey(d: LocalDate = today()): String = "W-${d.get(IsoFields.WEEK_BASED_YEAR)}-${d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

fun euro(v: Double, decimals: Boolean = true): String {
    val s = if (decimals) String.format(PT, "%,.2f", v) else String.format(PT, "%,.0f", v)
    return "$s €"
}

fun pct(v: Double): String = (if (v >= 0) "+" else "") + String.format(PT, "%.2f%%", v)

fun fmtTime(millis: Long): String = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(millis).atZone(zone))
fun fmtMinutes(m: Int): String = String.format("%02d:%02d", m / 60, m % 60)
fun fmtDay(d: LocalDate): String = DateTimeFormatter.ofPattern("EEE, d MMM", PT).format(d).replaceFirstChar { it.uppercase() }
fun fmtDayLong(d: LocalDate): String = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT).format(d).replaceFirstChar { it.uppercase() }
fun fmtMonth(ym: YearMonth): String = DateTimeFormatter.ofPattern("MMMM yyyy", PT).format(ym).replaceFirstChar { it.uppercase() }

fun relativeDay(epochDay: Long): String {
    val diff = epochDay - todayEpoch()
    return when (diff) {
        0L -> "Hoje"
        1L -> "Amanhã"
        -1L -> "Ontem"
        else -> fmtDay(LocalDate.ofEpochDay(epochDay))
    }
}

fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 6 -> "Boa noite"
        h < 13 -> "Bom dia"
        h < 20 -> "Boa tarde"
        else -> "Boa noite"
    }
}

// ---------------- Finanças ----------------
data class MonthSummary(
    val month: YearMonth,
    val budget: Double,
    val fixed: Double,
    val spent: Double,
    val pending: Int,
    val remaining: Double,
    val byCategory: List<Pair<String, Double>>,
    val dailyCumulative: List<Double>,
    val daysInMonth: Int,
)

fun AppData.expensesIn(ym: YearMonth): List<Expense> = expenses.filter {
    val d = it.time.toLocalDate(); YearMonth.from(d) == ym
}

fun AppData.monthSummary(ym: YearMonth = YearMonth.now()): MonthSummary {
    val budget = budgets[monthKey(ym)] ?: 0.0
    val fixed = fixedExpenses.filter { it.active }.sumOf { it.amount }
    val list = expensesIn(ym)
    val confirmed = list.filter { !it.pending }
    val spent = confirmed.sumOf { it.amount }
    val byCat = confirmed.groupBy { it.category }.map { it.key to it.value.sumOf { e -> e.amount } }.sortedByDescending { it.second }
    val days = ym.lengthOfMonth()
    val perDay = DoubleArray(days)
    confirmed.forEach { perDay[it.time.toLocalDate().dayOfMonth - 1] += it.amount }
    var acc = 0.0
    val cumulative = perDay.map { acc += it; acc }
    return MonthSummary(ym, budget, fixed, spent, list.count { it.pending }, budget - fixed - spent, byCat, cumulative, days)
}

fun AppData.lastMonthsSpend(n: Int = 6): List<Pair<YearMonth, Double>> {
    val now = YearMonth.now()
    return (n - 1 downTo 0).map { i ->
        val ym = now.minusMonths(i.toLong())
        ym to expensesIn(ym).filter { !it.pending }.sumOf { it.amount }
    }
}

// ---------------- Cervejas ----------------
fun AppData.beersOn(epochDay: Long): Int = beers[epochDay.toString()] ?: 0
fun AppData.beersThisWeek(): Int {
    val start = weekStart().toEpochDay()
    return (start..todayEpoch()).sumOf { beersOn(it) }
}
fun AppData.daysWithoutBeerStreak(): Int {
    var d = todayEpoch()
    var streak = 0
    while (beersOn(d) == 0 && streak < 365) { streak++; d-- }
    return streak
}

data class BeerFeedback(val title: String, val body: String, val level: Int) // 0 bom, 1 atenção, 2 alerta

fun AppData.beerFeedback(): BeerFeedback {
    val week = beersThisWeek()
    val limit = settings.beerWeeklyLimit.coerceAtLeast(1)
    val todayCount = beersOn(todayEpoch())
    return when {
        week == 0 -> BeerFeedback("Semana limpa 💪", "Ainda nenhuma esta semana. Excelente controlo.", 0)
        week > limit -> BeerFeedback("Deves beber menos", "Já vais em $week esta semana, acima do teu limite de $limit. Abranda nos próximos dias.", 2)
        todayCount >= 3 -> BeerFeedback("Hoje já chega", "$todayCount hoje. Alterna com água e fica por aqui.", 2)
        week.toDouble() / limit >= 0.75 -> BeerFeedback("Atenção ao limite", "$week de $limit esta semana. Resta pouca margem.", 1)
        else -> BeerFeedback("Bom controlo 👌", "$week de $limit esta semana. Continua assim.", 0)
    }
}

// ---------------- Tempo e roupa ----------------
fun weatherLabel(code: Int): String = when (code) {
    0 -> "Céu limpo"
    1, 2 -> "Pouco nublado"
    3 -> "Nublado"
    45, 48 -> "Nevoeiro"
    51, 53, 55, 56, 57 -> "Chuvisco"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Chuva"
    71, 73, 75, 77, 85, 86 -> "Neve"
    95, 96, 99 -> "Trovoada"
    else -> "—"
}

fun feelLabel(feels: Double): String = when {
    feels < 5 -> "Muito frio"
    feels < 11 -> "Frio"
    feels < 16 -> "Fresco"
    feels < 22 -> "Agradável"
    feels < 28 -> "Quente"
    else -> "Muito quente"
}

fun clothingFor(w: WeatherNow): List<String> {
    val f = w.feels
    val out = mutableListOf<String>()
    when {
        f < 5 -> out += listOf("Casaco quente", "Camisola de malha", "Cachecol", "Calças grossas")
        f < 11 -> out += listOf("Casaco", "Camisola", "Calças compridas")
        f < 16 -> out += listOf("Blusão leve", "Camisola fina", "Calças")
        f < 22 -> out += listOf("T-shirt ou camisa", "Sobretudo leve à noite")
        f < 28 -> out += listOf("T-shirt", "Calções ou chinos leves")
        else -> out += listOf("Roupa leve e clara", "Calções")
    }
    if (w.rainProb >= 50 || w.code in 61..82) out += "Guarda-chuva"
    else if (w.rainProb >= 30) out += "Impermeável compacto"
    if (w.wind >= 30) out += "Corta-vento"
    if (w.uv >= 6) out += "Óculos de sol e protetor"
    if (w.max - w.min >= 9) out += "Camadas (amplitude de ${(w.max - w.min).roundToInt()}°)"
    return out
}

// ---------------- Invest ----------------
data class PortfolioLine(val holding: Holding, val price: Double, val value: Double, val invested: Double, val pl: Double, val plPct: Double, val dayPct: Double, val history: List<Double>)

fun AppData.portfolioLines(): List<PortfolioLine> = holdings.map { h ->
    val q = quotes[h.symbol]
    val price = q?.price ?: h.avgPrice
    val value = price * h.quantity
    val invested = h.avgPrice * h.quantity
    val pl = value - invested
    val day = if (q != null && q.previousClose > 0) (q.price / q.previousClose - 1) * 100 else 0.0
    PortfolioLine(h, price, value, invested, pl, if (invested > 0) pl / invested * 100 else 0.0, day, q?.history ?: emptyList())
}

// ---------------- Tasks ----------------
fun AppData.tasksForToday(): List<TaskItem> {
    val t = todayEpoch()
    return tasks.filter { !it.done && (it.dueDay == null || it.dueDay <= t) }
        .sortedWith(compareBy<TaskItem>({ it.dueDay == null }, { -it.priority.ordinal }, { it.dueMinutes ?: 9999 }))
}

/** Intensidade diária (0..4) para o heatmap de produtividade das últimas [weeks] semanas. */
fun AppData.productivityGrid(weeks: Int = 12): List<Int> {
    val end = today()
    val start = weekStart(end).minusWeeks((weeks - 1).toLong())
    val counts = HashMap<Long, Int>()
    tasks.forEach { t -> t.completedAt?.let { val d = it.toLocalDate().toEpochDay(); counts[d] = (counts[d] ?: 0) + 1 } }
    habits.forEach { h -> h.doneDays.forEach { d -> counts[d] = (counts[d] ?: 0) + 1 } }
    return (0 until weeks * 7).map { i ->
        val d = start.plusDays(i.toLong())
        if (d.isAfter(end)) -1 else {
            val c = counts[d.toEpochDay()] ?: 0
            when { c == 0 -> 0; c <= 1 -> 1; c <= 3 -> 2; c <= 5 -> 3; else -> 4 }
        }
    }
}

fun AppData.weekStats(): Map<String, String> {
    val start = weekStart().startMillis()
    val doneWeek = tasks.count { (it.completedAt ?: 0L) >= start }
    val spentWeek = expenses.filter { !it.pending && it.time >= start }.sumOf { it.amount }
    return mapOf(
        "Tasks concluídas" to doneWeek.toString(),
        "Gasto da semana" to euro(spentWeek),
        "Cervejas" to beersThisWeek().toString(),
        "Hábitos cumpridos" to habits.sumOf { h -> h.doneDays.count { it >= weekStart().toEpochDay() } }.toString(),
    )
}
