package com.carlos.superhuman.net

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.carlos.superhuman.data.AppData
import com.carlos.superhuman.data.Expense
import com.carlos.superhuman.data.Goal
import com.carlos.superhuman.data.Note
import com.carlos.superhuman.data.Priority
import com.carlos.superhuman.data.ShoppingItem
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.TaskItem
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.beersOn
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.clothingFor
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.feelLabel
import com.carlos.superhuman.data.fmtTime
import com.carlos.superhuman.data.monthKey
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.portfolioLines
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.data.tasksForToday
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.data.weatherLabel
import com.carlos.superhuman.data.weekStart
import com.carlos.superhuman.data.weekStats
import com.carlos.superhuman.device.CalendarRepo
import com.carlos.superhuman.service.WalletListener
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/**
 * Assistente local: percebe comandos em português sem internet e sem custos.
 * Para perguntas abertas, prepara o contexto e abre a app do Claude (usa a tua subscrição).
 */
object LocalAssistant {

    data class Reply(val text: String, val handled: Boolean)

    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

    private val amountRx = Regex("""(\d+(?:[.,]\d{1,2})?)\s*(?:€|eur|euros?)?""")
    private val timeRx = Regex("""(?:as|às|pelas)?\s*(\d{1,2})(?:[:h](\d{2}))?\s*h?\b""")

    private val weekdays = mapOf(
        "segunda" to DayOfWeek.MONDAY, "terca" to DayOfWeek.TUESDAY, "quarta" to DayOfWeek.WEDNESDAY,
        "quinta" to DayOfWeek.THURSDAY, "sexta" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY, "domingo" to DayOfWeek.SUNDAY,
    )

    private fun parseDay(n: String): LocalDate? {
        val today = LocalDate.now()
        if ("depois de amanha" in n) return today.plusDays(2)
        if ("amanha" in n) return today.plusDays(1)
        if (Regex("""\bhoje\b""").containsMatchIn(n)) return today
        if ("proxima semana" in n || "semana que vem" in n) return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        weekdays.forEach { (k, v) -> if (Regex("""\b$k\b""").containsMatchIn(n)) return today.with(TemporalAdjusters.next(v)) }
        Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""").find(n)?.let { m ->
            val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt()
            val y = m.groupValues[3].takeIf { it.isNotBlank() }?.toInt()?.let { if (it < 100) 2000 + it else it } ?: today.year
            return runCatching { LocalDate.of(y, mo, d) }.getOrNull()
        }
        return null
    }

    private fun parseTime(n: String): Int? {
        // procura "às 10", "10h", "10:30", "pelas 9h15"
        val m = Regex("""(?:\bas\b|\bpelas\b)\s*(\d{1,2})(?:[:h](\d{2}))?|\b(\d{1,2})[:h](\d{2})?\b""").find(n) ?: return null
        val h = (m.groupValues[1].ifBlank { m.groupValues[3] }).toIntOrNull() ?: return null
        val min = (m.groupValues[2].ifBlank { m.groupValues[4] }).toIntOrNull() ?: 0
        return if (h in 0..23 && min in 0..59) h * 60 + min else null
    }

    private fun stripDateWords(s: String): String = s
        .replace(Regex("""(?i)\b(depois de amanh[ãa]|amanh[ãa]|hoje|pr[óo]xima semana|semana que vem)\b"""), "")
        .replace(Regex("""(?i)\b(na |no |à |a )?(segunda|ter[çc]a|quarta|quinta|sexta|s[áa]bado|domingo)(-feira)?\b"""), "")
        .replace(Regex("""(?i)\b(à|a)s?\s*\d{1,2}([:h]\d{2})?\s*h?\b|\bpelas\s*\d{1,2}([:h]\d{2})?\s*h?\b|\b\d{1,2}[:h]\d{0,2}\b"""), "")
        .replace(Regex("""(?i)\b(com prioridade|prioridade)\s*(alta|m[ée]dia|baixa)\b|\burgente\b"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', ',', '.', ':', '-')

    private fun parseAmount(n: String): Double? =
        Regex("""(\d+(?:[.,]\d{1,2})?)\s*(?:€|eur\b|euros?\b)|(?:€)\s*(\d+(?:[.,]\d{1,2})?)|\b(\d+[.,]\d{1,2})\b|\b(\d+)\b""")
            .find(n)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.replace(",", ".")?.toDoubleOrNull()

    private val categoryWords = listOf(
        "Supermercado" to listOf("supermercado", "continente", "pingo doce", "lidl", "auchan", "mercadona", "aldi", "minipreco", "compras de casa"),
        "Restauração" to listOf("almoco", "jantar", "cafe", "pequeno-almoco", "pequeno almoco", "restaurante", "lanche", "snack", "pizza", "sushi", "hamburguer", "glovo", "uber eats", "bolt food"),
        "Transportes" to listOf("gasolina", "gasoleo", "combustivel", "uber", "bolt", "taxi", "comboio", "cp", "metro", "carris", "portagem", "via verde", "estacionamento", "parque"),
        "Lazer" to listOf("cinema", "concerto", "bilhete", "jogo", "futebol", "sporting", "bar", "cerveja", "copos", "festa"),
        "Saúde" to listOf("farmacia", "medico", "consulta", "dentista", "ginasio", "suplementos"),
        "Casa" to listOf("luz", "agua", "gas", "renda", "internet", "ikea", "leroy", "limpeza"),
        "Subscrições" to listOf("netflix", "spotify", "disney", "hbo", "max", "youtube", "icloud", "google one", "subscricao"),
        "Compras" to listOf("roupa", "sapatilhas", "zara", "worten", "fnac", "amazon", "presente", "prenda", "decathlon"),
    )

    private fun guessCategory(n: String): String =
        categoryWords.firstOrNull { (_, words) -> words.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(n) } }?.first ?: "Outros"

    private fun merchantFrom(raw: String): String {
        val m = Regex("""(?i)\b(?:no|na|em|ao|à|no\s+o)\s+([A-Za-zÀ-ÿ0-9' &.-]{2,40})$""").find(raw.trim().trimEnd('.', '!'))
        val base = m?.groupValues?.get(1)?.trim()
            ?: raw.replace(Regex("""(?i)^(gastei|paguei|despesa( de)?|comprei)\s*"""), "")
                .replace(Regex("""(?i)\d+([.,]\d{1,2})?\s*(€|eur|euros?)?"""), "")
                .replace(Regex("""(?i)^\s*(em|no|na|num|numa|de|com)\s+"""), "").trim()
        return base.replaceFirstChar { it.uppercase() }.ifBlank { "Despesa" }
    }

    fun handle(ctx: Context, raw: String): Reply {
        val n = norm(raw)
        val d = Store.data

        // ---- Cervejas ----
        Regex("""^\+\s*(\d+)?\s*cervej""").find(n)?.let { m ->
            return addBeers(m.groupValues[1].toIntOrNull() ?: 1)
        }
        if (Regex("""\b(bebi|mais|regista|registar|adiciona)\b.*\bcervej""").containsMatchIn(n)) {
            val k = Regex("""(\d+)\s*cervej""").find(n)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\b(uma|duas|tres|quatro|cinco)\b""").find(n)?.groupValues?.get(1)?.let { listOf("uma", "duas", "tres", "quatro", "cinco").indexOf(it) + 1 }
                ?: 1
            return addBeers(k)
        }
        if ("cervej" in n) {
            val fb = d.beerFeedback()
            return Reply("Hoje: ${d.beersOn(todayEpoch())}. Semana: ${d.beersThisWeek()} de ${d.settings.beerWeeklyLimit}. ${fb.title} ${fb.body}", true)
        }

        // ---- Lista de compras ----
        val shop = Regex("""^(?:junta|adiciona|acrescenta|poe|mete|preciso de|falta|faltam|comprar)\s+(.+?)(?:\s+(?:a|na|para a)\s+lista(?: de compras)?)?$""").find(n)
        if (shop != null && ("lista" in n || n.startsWith("comprar") || n.startsWith("preciso de") || n.startsWith("falta"))) {
            val itemsRaw = Regex("""(?i)^(?:junta|adiciona|acrescenta|p[õo]e|mete|preciso de|falta|faltam|comprar)\s+""").replace(raw.trim(), "")
                .replace(Regex("""(?i)\s+(?:à|a|na|para a)\s+lista( de compras)?\.?$"""), "")
            val items = itemsRaw.split(Regex("""\s*(?:,|\be\b|;)\s*""")).map { it.trim().trim('.') }.filter { it.isNotBlank() }
                .map { it.replaceFirstChar { c -> c.uppercase() } }
            if (items.isNotEmpty()) {
                Store.update { it.copy(shopping = it.shopping + items.map { name -> ShoppingItem(name = name) }) }
                return Reply("Adicionei à lista: ${items.joinToString(", ")}.", true)
            }
        }
        if (Regex("""(o que|que) (tenho|falta|esta) (na|para) (a )?lista|lista de compras\??$""").containsMatchIn(n)) {
            val open = d.shopping.filter { !it.checked }
            return Reply(if (open.isEmpty()) "A lista de compras está vazia." else "Na lista (${open.size}): " + open.joinToString(", ") { it.name } + ".", true)
        }

        // ---- Despesas ----
        if (Regex("""^(gastei|paguei|despesa|comprei)\b""").containsMatchIn(n) && Regex("""\d""").containsMatchIn(n)) {
            val amount = parseAmount(n) ?: return Reply("Não percebi o valor. Experimenta: \"gastei 12,50 no almoço\".", true)
            val day = parseDay(n)
            val time = if (day == null || day == LocalDate.now()) System.currentTimeMillis() else day.startMillis() + 12 * 3_600_000L
            val e = Expense(time = time, amount = amount, merchant = merchantFrom(stripDateWords(raw)), category = guessCategory(n))
            Store.update { it.copy(expenses = it.expenses + e) }
            val m = Store.data.monthSummary()
            return Reply("Registei ${euro(amount)} em ${e.category} (${e.merchant}). " +
                if (m.budget > 0) "Restam ${euro(m.remaining)} do plafond." else "Ainda não definiste o plafond deste mês.", true)
        }

        // ---- Plafond ----
        Regex("""(?:plafond|orcamento)\s+(?:de\s+)?(?:(novembro|dezembro|janeiro|proximo mes|este mes)\s+)?(?:e|de|para|=)?\s*(\d+(?:[.,]\d{1,2})?)""").find(n)?.let { m ->
            if (Regex("""\b(define|poe|mete|muda|altera|e de|=)\b""").containsMatchIn(n) || m.groupValues[2].isNotBlank()) {
                val ym = when (m.groupValues[1]) { "proximo mes", "novembro", "dezembro", "janeiro" -> YearMonth.now().plusMonths(1); else -> YearMonth.now() }
                val v = m.groupValues[2].replace(",", ".").toDouble()
                Store.update { it.copy(budgets = it.budgets + (monthKey(ym) to v)) }
                return Reply("Plafond de ${ym.monthValue}/${ym.year} definido: ${euro(v)}.", true)
            }
        }
        if (Regex("""plafond|quanto (me )?(resta|sobra|falta)|quanto gastei|orcamento|financas""").containsMatchIn(n)) {
            val m = d.monthSummary()
            if (m.budget <= 0) return Reply("Gastaste ${euro(m.spent)} este mês. Ainda não tens plafond definido: diz por exemplo \"plafond 1500\".", true)
            val daysLeft = (YearMonth.now().lengthOfMonth() - LocalDate.now().dayOfMonth + 1).coerceAtLeast(1)
            val top = m.byCategory.take(2).joinToString(" e ") { "${it.first} (${euro(it.second, false)})" }
            return Reply("Restam ${euro(m.remaining)} de ${euro(m.budget, false)}, cerca de ${euro(m.remaining / daysLeft)} por dia. Gasto ${euro(m.spent)} + fixas ${euro(m.fixed)}." +
                (if (top.isNotBlank()) " Onde mais gastas: $top." else "") +
                (if (m.pending > 0) " Tens ${m.pending} pagamento(s) do Wallet por confirmar." else ""), true)
        }

        // ---- Tasks ----
        val taskCmd = Regex("""^(?:cria(?:r)?\s+(?:uma\s+)?task|nova task|task|tarefa|lembra-me de|lembrar de|lembra me de|tenho de|preciso de)\s*:?\s*(.+)$""").find(n)
        if (taskCmd != null) {
            val prefix = Regex("""(?i)^(?:cria(?:r)?\s+(?:uma\s+)?task|nova task|task|tarefa|lembra-?\s?me de|lembrar de|tenho de|preciso de)\s*:?\s*""")
            val title = stripDateWords(prefix.replace(raw.trim(), "")).replaceFirstChar { it.uppercase() }
            if (title.isBlank()) return Reply("Qual é a task? Ex.: \"task ligar ao cliente amanhã às 10h\".", true)
            val prio = when {
                Regex("""\burgente\b|prioridade alta|\balta\b""").containsMatchIn(n) -> Priority.HIGH
                Regex("""prioridade baixa|\bbaixa\b""").containsMatchIn(n) -> Priority.LOW
                else -> Priority.MEDIUM
            }
            val day = parseDay(n) ?: LocalDate.now()
            val t = TaskItem(title = title, dueDay = day.toEpochDay(), dueMinutes = parseTime(n), priority = prio)
            Store.update { it.copy(tasks = it.tasks + t) }
            val whenTxt = (if (day == LocalDate.now()) "hoje" else if (day == LocalDate.now().plusDays(1)) "amanhã" else "${day.dayOfMonth}/${day.monthValue}") +
                (t.dueMinutes?.let { " às %02d:%02d".format(it / 60, it % 60) } ?: "")
            return Reply("Criei a task \"$title\" para $whenTxt${if (prio == Priority.HIGH) ", prioridade alta" else ""}.", true)
        }
        Regex("""^(?:feito|feita|conclui|concluir|marca como feita|ja fiz|acabei)\s*:?\s*(.+)$""").find(n)?.let { m ->
            val q = m.groupValues[1].trim()
            val t = d.tasks.firstOrNull { !it.done && norm(it.title).contains(q) } ?: return Reply("Não encontrei nenhuma task por fazer com \"$q\".", true)
            Store.update { s -> s.copy(tasks = s.tasks.map { if (it.id == t.id) it.copy(done = true, completedAt = System.currentTimeMillis()) else it }) }
            return Reply("Boa! \"${t.title}\" concluída.", true)
        }
        if (Regex("""(que|quais) tasks|tasks (de|para) hoje|o que tenho (de|para) fazer|pendentes""").containsMatchIn(n)) {
            val list = d.tasksForToday()
            return Reply(if (list.isEmpty()) "Não tens tasks pendentes para hoje. ✨" else "Para hoje (${list.size}): " +
                list.take(8).joinToString("; ") { it.title + if (it.priority == Priority.HIGH) " (alta)" else "" } + ".", true)
        }

        // ---- Eventos ----
        val evCmd = Regex("""^(?:marca|marcar|agenda|agendar|evento|reuniao|cria evento)\s*:?\s*(.+)$""").find(n)
        if (evCmd != null && parseTime(n) != null) {
            val title = stripDateWords(Regex("""(?i)^(?:marca|marcar|agenda|agendar|evento|cria evento)\s*:?\s*""").replace(raw.trim(), "")).replaceFirstChar { it.uppercase() }
            val day = parseDay(n) ?: LocalDate.now()
            val start = parseTime(n)!!
            val cal = CalendarRepo.defaultCalendar(ctx) ?: return Reply("Não tenho acesso a um calendário com escrita. Dá permissão de calendário.", true)
            val ok = CalendarRepo.insert(ctx, cal.id, title.ifBlank { "Evento" }, day.startMillis() + start * 60_000L, day.startMillis() + (start + 60) * 60_000L)
            if (ok) CalendarRepo.cacheToday(ctx)
            return Reply(if (ok) "Marquei \"${title.ifBlank { "Evento" }}\" a ${day.dayOfMonth}/${day.monthValue} às %02d:%02d (1h) em ${cal.name}.".format(start / 60, start % 60) else "Não consegui criar o evento.", true)
        }

        // ---- Notas / objetivos ----
        Regex("""^(?:nota|anota|aponta|guarda)\s*:?\s*(.+)$""").find(n)?.let {
            val body = Regex("""(?i)^(?:nota|anota|aponta|guarda)\s*:?\s*""").replace(raw.trim(), "")
            Store.update { s -> s.copy(notes = s.notes + Note(title = body.take(40), body = body)) }
            return Reply("Guardei nas notas.", true)
        }
        Regex("""^(?:objetivo|meta)\s*:?\s*(.+)$""").find(n)?.let {
            val title = Regex("""(?i)^(?:objetivo|meta)\s*:?\s*""").replace(raw.trim(), "")
            Store.update { s -> s.copy(goals = s.goals + Goal(title = title.replaceFirstChar { c -> c.uppercase() })) }
            return Reply("Objetivo criado. Abre Objetivos para definir prazo e marcos.", true)
        }

        // ---- Tempo / roupa ----
        if (Regex("""tempo|chover|chuva|frio|calor|vestir|roupa|guarda-chuva|casaco""").containsMatchIn(n)) {
            val w = d.weather
            return Reply("${d.settings.locationName}: ${w.temp.roundToInt()}° (sensação ${w.feels.roundToInt()}°), ${weatherLabel(w.code).lowercase()}, máx ${w.max.roundToInt()}° / mín ${w.min.roundToInt()}°, chuva ${w.rainProb}%. " +
                "${feelLabel(w.feels)}: ${clothingFor(w).joinToString(", ").lowercase()}.", true)
        }

        // ---- Portfólio / mercado ----
        if (Regex("""portfolio|carteira|investimentos|acoes|etf|mercado|bolsa|invest""").containsMatchIn(n)) {
            val pl = d.portfolioLines()
            val f = d.investFeed
            val part1 = if (pl.isEmpty()) "Ainda não tens posições no portfólio." else {
                val v = pl.sumOf { it.value }; val inv = pl.sumOf { it.invested }
                "Portfólio: ${euro(v)} (${if (v >= inv) "+" else ""}${euro(v - inv)} desde a compra)."
            }
            val part2 = f?.let { " Mercado (${it.date}): ${it.mood}. ${it.marketSummary.take(220)}" + (it.tips.firstOrNull()?.let { tip -> " Ideia do dia: ${tip.title}." } ?: "") } ?: ""
            return Reply(part1 + part2, true)
        }

        // ---- Resumo do dia / semana ----
        if (Regex("""(como (esta|e|vai) o meu dia|resumo|o meu dia|agenda|o que tenho hoje|briefing)""").containsMatchIn(n)) {
            return Reply(if ("semana" in n) weekSummary(d) else daySummary(d), true)
        }

        if (Regex("""^(ajuda|help|o que (sabes|consegues) fazer)""").containsMatchIn(n)) return Reply(HELP, true)

        return Reply("Isto é melhor perguntar ao Claude. Toca em \"Continuar no Claude\": abro a app com a pergunta e o contexto do teu dia, sem custos extra.", false)
    }

    private fun addBeers(k: Int): Reply {
        val key = todayEpoch().toString()
        Store.update { it.copy(beers = it.beers + (key to ((it.beers[key] ?: 0) + k).coerceAtLeast(0))) }
        val fb = Store.data.beerFeedback()
        return Reply("Registei +$k. ${fb.title} ${fb.body}", true)
    }

    fun daySummary(d: AppData): String {
        val now = System.currentTimeMillis()
        val ev = d.cachedEvents.filter { it.allDay || it.end > now }
        val tasks = d.tasksForToday()
        val high = tasks.count { it.priority == Priority.HIGH }
        val m = d.monthSummary()
        val w = d.weather
        return buildString {
            append(if (ev.isEmpty()) "Sem mais eventos hoje." else "${ev.size} evento(s): " + ev.take(5).joinToString("; ") { (if (it.allDay) "todo o dia" else fmtTime(it.begin)) + " " + it.title + " [" + it.calendarLabel + "]" } + ".")
            append(" ${tasks.size} task(s) por fazer" + (if (high > 0) ", $high de prioridade alta" else "") + ".")
            if (m.budget > 0) append(" Plafond: restam ${euro(m.remaining)}.")
            append(" Tempo: ${w.temp.roundToInt()}°, ${feelLabel(w.feels).lowercase()}, chuva ${w.rainProb}%.")
            append(" ${d.beerFeedback().title}")
        }
    }

    fun weekSummary(d: AppData): String =
        "Esta semana: " + d.weekStats().entries.joinToString(", ") { "${it.key.lowercase()} ${it.value}" } + ". " + d.beerFeedback().title

    /** Contexto compacto para colar numa conversa com o Claude. */
    fun context(d: AppData = Store.data): String {
        val m = d.monthSummary()
        val pl = d.portfolioLines()
        return buildString {
            appendLine("Contexto da minha app Superhuman (${LocalDate.now()}):")
            appendLine("- Agenda hoje: " + d.cachedEvents.joinToString("; ") { "${fmtTime(it.begin)}-${fmtTime(it.end)} ${it.title} [${it.calendarLabel}]" }.ifBlank { "sem eventos" })
            appendLine("- Tasks por fazer: " + d.tasks.filter { !it.done }.take(12).joinToString("; ") { "${it.title} (${it.priority})" }.ifBlank { "nenhuma" })
            appendLine("- Finanças do mês: plafond ${euro(m.budget)}, fixas ${euro(m.fixed)}, gasto ${euro(m.spent)}, restante ${euro(m.remaining)}; por categoria " + m.byCategory.joinToString(", ") { "${it.first} ${euro(it.second, false)}" })
            appendLine("- Cervejas esta semana: ${d.beersThisWeek()} (limite ${d.settings.beerWeeklyLimit})")
            appendLine("- Hábitos: " + d.habits.joinToString(", ") { h -> "${h.name} ${h.doneDays.count { it >= weekStart().toEpochDay() }}/${h.targetPerWeek}" }.ifBlank { "nenhum" })
            if (pl.isNotEmpty()) appendLine("- Portfólio: " + pl.joinToString(", ") { "${it.holding.symbol} ${euro(it.value, false)} (${"%.1f".format(it.plPct)}%)" })
            appendLine("- Objetivos: " + d.goals.joinToString(", ") { it.title }.ifBlank { "nenhum" })
        }
    }

    /** Abre a app do Claude com o texto (ou partilha/copia se não estiver instalada). */
    fun openInClaude(ctx: Context, question: String, includeContext: Boolean = true) {
        val text = (if (includeContext) context() + "\n" else "") + question
        runCatching {
            (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Superhuman", text))
        }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(Intent(send).setPackage("com.anthropic.claude"))
        } catch (e: ActivityNotFoundException) {
            try {
                ctx.startActivity(Intent.createChooser(send, "Enviar para…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { }
        }
        Toast.makeText(ctx, "Texto copiado. Se não aparecer na conversa, cola-o.", Toast.LENGTH_SHORT).show()
    }

    const val HELP = """Posso fazer sem internet:
• "gastei 12,50 no almoço"  • "plafond 1500"  • "quanto me resta?"
• "task ligar ao cliente amanhã às 10h"  • "feito ligar ao cliente"
• "junta leite e ovos à lista"  • "o que tenho na lista?"
• "marca dentista sexta às 15h"  • "+2 cervejas"
• "como está o meu dia?"  • "vou precisar de casaco?"
• "nota: ideia para a app…"  • "objetivo correr a meia maratona"
Para o resto, "Continuar no Claude"."""
}
