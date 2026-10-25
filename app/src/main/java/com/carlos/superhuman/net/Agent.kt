package com.carlos.superhuman.net

import android.content.Context
import com.carlos.superhuman.data.ChatMsg
import com.carlos.superhuman.data.Expense
import com.carlos.superhuman.data.Goal
import com.carlos.superhuman.data.Habit
import com.carlos.superhuman.data.Note
import com.carlos.superhuman.data.Priority
import com.carlos.superhuman.data.ShoppingItem
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.TaskItem
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtTime
import com.carlos.superhuman.data.monthKey
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.portfolioLines
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.data.tasksForToday
import com.carlos.superhuman.data.today
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.device.CalendarRepo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Assistente com ferramentas: a Claude pode ler e alterar dados da app
 * (tasks, despesas, notas, calendário, compras, objetivos…) e pesquisar na web.
 */
object Agent {

    private fun tool(name: String, description: String, props: Map<String, Pair<String, String>>, required: List<String> = emptyList()): JsonObject =
        buildJsonObject {
            put("name", name)
            put("description", description)
            putJsonObject("input_schema") {
                put("type", "object")
                putJsonObject("properties") {
                    props.forEach { (k, v) ->
                        putJsonObject(k) {
                            if (v.first == "array") {
                                put("type", "array")
                                putJsonObject("items") { put("type", "string") }
                            } else put("type", v.first)
                            put("description", v.second)
                        }
                    }
                }
                putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
            }
        }

    private val tools: JsonArray by lazy {
        buildJsonArray {
            add(tool("get_overview", "Resumo do dia: eventos, tasks de hoje, finanças do mês, cervejas, portfólio.", emptyMap()))
            add(tool("list_tasks", "Lista tasks.", mapOf("filter" to ("string" to "today | upcoming | all | done"))))
            add(tool("create_task", "Cria uma task.", mapOf(
                "title" to ("string" to "Título"),
                "notes" to ("string" to "Detalhes"),
                "due_date" to ("string" to "YYYY-MM-DD"),
                "time" to ("string" to "HH:MM"),
                "priority" to ("string" to "LOW | MEDIUM | HIGH"),
                "tags" to ("array" to "Etiquetas"),
            ), listOf("title")))
            add(tool("complete_task", "Marca como concluída a task cujo título contém o texto.", mapOf("query" to ("string" to "parte do título")), listOf("query")))
            add(tool("add_expense", "Regista uma despesa (deduz ao plafond).", mapOf(
                "amount" to ("number" to "Valor em euros"),
                "merchant" to ("string" to "Onde"),
                "category" to ("string" to "Supermercado, Restauração, Transportes, Casa, Lazer, Saúde, Compras, Subscrições, Outros"),
                "date" to ("string" to "YYYY-MM-DD, por defeito hoje"),
            ), listOf("amount", "merchant")))
            add(tool("set_budget", "Define o plafond de um mês.", mapOf("month" to ("string" to "YYYY-MM"), "amount" to ("number" to "Euros")), listOf("month", "amount")))
            add(tool("get_finance", "Resumo financeiro de um mês.", mapOf("month" to ("string" to "YYYY-MM, por defeito o atual"))))
            add(tool("add_note", "Cria uma nota.", mapOf("title" to ("string" to "Título"), "body" to ("string" to "Conteúdo")), listOf("body")))
            add(tool("search_notes", "Pesquisa nas notas.", mapOf("query" to ("string" to "texto")), listOf("query")))
            add(tool("add_beer", "Regista cervejas bebidas hoje.", mapOf("count" to ("integer" to "Quantas")), listOf("count")))
            add(tool("add_shopping_items", "Adiciona itens à lista de compras.", mapOf("items" to ("array" to "Itens")), listOf("items")))
            add(tool("create_event", "Cria um evento no calendário padrão.", mapOf(
                "title" to ("string" to "Título"),
                "date" to ("string" to "YYYY-MM-DD"),
                "start" to ("string" to "HH:MM"),
                "end" to ("string" to "HH:MM"),
            ), listOf("title", "date", "start", "end")))
            add(tool("add_goal", "Cria um objetivo de vida.", mapOf(
                "title" to ("string" to "Objetivo"), "area" to ("string" to "Carreira, Finanças, Saúde, Pessoal, Família, Viagens, Aprender, Casa"),
                "target_date" to ("string" to "YYYY-MM-DD"), "why" to ("string" to "Motivação"),
            ), listOf("title")))
            add(tool("add_habit", "Cria um hábito a acompanhar.", mapOf("name" to ("string" to "Hábito"), "emoji" to ("string" to "Emoji")), listOf("name")))
            add(Claude.webSearchTool(4))
        }
    }

    private fun system(): String {
        val d = Store.data
        return """
            És o assistente pessoal do ${d.settings.userName} dentro da app Superhuman, a base de controlo da vida dele.
            Fala em português de Portugal, curto e direto. Hoje é ${today()} (${LocalTime.now().withNano(0)}), localização ${d.settings.locationName}.
            Usa as ferramentas para consultar e alterar dados em vez de inventar. Confirma em 1 frase o que fizeste.
            Para investimentos, dá informação e análise, não ordens; lembra o risco quando relevante.
        """.trimIndent()
    }

    suspend fun run(ctx: Context, history: List<ChatMsg>): String {
        var messages: List<JsonElement> = history.takeLast(20).map {
            buildJsonObject { put("role", it.role); put("content", it.text) }
        }
        // A API exige que a conversa comece pelo utilizador
        messages = messages.dropWhile { it.str("role") != "user" }
        repeat(8) {
            val resp = Claude.send(system(), JsonArray(messages), tools, maxTokens = 1500)
            val content = resp["content"] ?: JsonArray(emptyList())
            messages = messages + buildJsonObject { put("role", "assistant"); put("content", content) }
            when (resp.str("stop_reason")) {
                "tool_use" -> {
                    val results = buildJsonArray {
                        content.jsonArray.filter { it.str("type") == "tool_use" }.forEach { block ->
                            val out = runCatching { exec(ctx, block.str("name") ?: "", block.jsonObject["input"]?.jsonObject ?: JsonObject(emptyMap())) }
                                .getOrElse { "Erro: ${it.message}" }
                            add(buildJsonObject {
                                put("type", "tool_result")
                                put("tool_use_id", block.str("id") ?: "")
                                put("content", out)
                            })
                        }
                    }
                    messages = messages + buildJsonObject { put("role", "user"); put("content", results) }
                }
                "pause_turn" -> Unit
                else -> return Claude.textOf(resp).ifBlank { "Feito." }
            }
        }
        return "Não consegui concluir o pedido."
    }

    private fun JsonObject.s(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    private fun JsonObject.n(k: String): Double? = (this[k] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
    private fun JsonObject.list(k: String): List<String> = runCatching { this[k]!!.jsonArray.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } }.getOrDefault(emptyList())
    private fun minutes(hhmm: String?): Int? = hhmm?.split(":")?.let { p -> (p.getOrNull(0)?.toIntOrNull() ?: return null) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) }

    private fun exec(ctx: Context, name: String, input: JsonObject): String {
        val d = Store.data
        return when (name) {
            "get_overview" -> {
                val m = d.monthSummary()
                val events = d.cachedEvents.joinToString("; ") { "${fmtTime(it.begin)}-${fmtTime(it.end)} ${it.title} [${it.calendarLabel}]" }
                val tasks = d.tasksForToday().take(10).joinToString("; ") { "${it.title} (${it.priority})" }
                val pl = d.portfolioLines()
                """
                Eventos hoje: ${events.ifBlank { "nenhum" }}
                Tasks de hoje: ${tasks.ifBlank { "nenhuma" }}
                Plafond ${euro(m.budget)}, fixas ${euro(m.fixed)}, gasto ${euro(m.spent)}, restante ${euro(m.remaining)}, ${m.pending} pagamentos por confirmar
                Cervejas semana: ${d.beersThisWeek()}/${d.settings.beerWeeklyLimit} (${d.beerFeedback().title})
                Portfólio: ${euro(pl.sumOf { it.value })} (investido ${euro(pl.sumOf { it.invested })})
                Tempo: ${d.weather.temp}°C, sensação ${d.weather.feels}°C, chuva ${d.weather.rainProb}%
                """.trimIndent()
            }
            "list_tasks" -> {
                val f = input.s("filter") ?: "today"
                val t0 = todayEpoch()
                val l = when (f) {
                    "upcoming" -> d.tasks.filter { !it.done && (it.dueDay ?: 0L) > t0 }
                    "all" -> d.tasks.filter { !it.done }
                    "done" -> d.tasks.filter { it.done }.takeLast(20)
                    else -> d.tasksForToday()
                }
                l.joinToString("\n") { "- ${it.title} | ${it.priority} | ${it.dueDay?.let { x -> LocalDate.ofEpochDay(x) } ?: "sem data"} | ${it.tags.joinToString(",")}" }.ifBlank { "Sem tasks." }
            }
            "create_task" -> {
                val t = TaskItem(
                    title = input.s("title") ?: "Task",
                    notes = input.s("notes") ?: "",
                    dueDay = input.s("due_date")?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() } ?: todayEpoch(),
                    dueMinutes = minutes(input.s("time")),
                    priority = runCatching { Priority.valueOf(input.s("priority") ?: "MEDIUM") }.getOrDefault(Priority.MEDIUM),
                    tags = input.list("tags"),
                )
                Store.update { it.copy(tasks = it.tasks + t, tags = (it.tags + t.tags).distinct()) }
                "Task criada: ${t.title}"
            }
            "complete_task" -> {
                val q = input.s("query") ?: return "Falta query"
                val t = d.tasks.firstOrNull { !it.done && it.title.contains(q, true) } ?: return "Não encontrei task com \"$q\""
                Store.update { st -> st.copy(tasks = st.tasks.map { if (it.id == t.id) it.copy(done = true, completedAt = System.currentTimeMillis()) else it }) }
                "Concluída: ${t.title}"
            }
            "add_expense" -> {
                val amount = input.n("amount") ?: return "Falta valor"
                val date = input.s("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
                val time = if (date == LocalDate.now()) System.currentTimeMillis() else date.startMillis() + 12 * 3_600_000L
                val e = Expense(time = time, amount = amount, merchant = input.s("merchant") ?: "Despesa", category = input.s("category") ?: "Outros")
                Store.update { it.copy(expenses = it.expenses + e) }
                val m = Store.data.monthSummary()
                "Despesa de ${euro(amount)} registada. Restante do plafond: ${euro(m.remaining)}"
            }
            "set_budget" -> {
                val ym = runCatching { YearMonth.parse(input.s("month")) }.getOrNull() ?: return "Mês inválido"
                val amount = input.n("amount") ?: return "Falta valor"
                Store.update { it.copy(budgets = it.budgets + (monthKey(ym) to amount)) }
                "Plafond de $ym definido: ${euro(amount)}"
            }
            "get_finance" -> {
                val ym = input.s("month")?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
                val m = d.monthSummary(ym)
                "Mês $ym: plafond ${euro(m.budget)}, fixas ${euro(m.fixed)}, gasto ${euro(m.spent)}, restante ${euro(m.remaining)}. Por categoria: " +
                    m.byCategory.joinToString(", ") { "${it.first} ${euro(it.second)}" }
            }
            "add_note" -> {
                val n = Note(title = input.s("title") ?: "", body = input.s("body") ?: "")
                Store.update { it.copy(notes = it.notes + n) }
                "Nota criada."
            }
            "search_notes" -> {
                val q = input.s("query") ?: ""
                d.notes.filter { it.title.contains(q, true) || it.body.contains(q, true) }.take(5)
                    .joinToString("\n---\n") { "${it.title}\n${it.body.take(600)}" }.ifBlank { "Sem notas com \"$q\"." }
            }
            "add_beer" -> {
                val n = (input.n("count") ?: 1.0).toInt()
                val k = todayEpoch().toString()
                Store.update { it.copy(beers = it.beers + (k to ((it.beers[k] ?: 0) + n).coerceAtLeast(0))) }
                val fb = Store.data.beerFeedback()
                "Registado. ${fb.title}: ${fb.body}"
            }
            "add_shopping_items" -> {
                val items = input.list("items")
                Store.update { it.copy(shopping = it.shopping + items.map { n -> ShoppingItem(name = n) }) }
                "Adicionados ${items.size} itens à lista."
            }
            "create_event" -> {
                val date = runCatching { LocalDate.parse(input.s("date")) }.getOrNull() ?: return "Data inválida"
                val startM = minutes(input.s("start")) ?: return "Hora inválida"
                val endM = minutes(input.s("end")) ?: (startM + 60)
                val title = input.s("title") ?: "Evento"
                val cal = CalendarRepo.defaultCalendar(ctx) ?: return "Sem calendário com escrita"
                val ok = CalendarRepo.insert(ctx, cal.id, title, date.startMillis() + startM * 60_000L, date.startMillis() + endM * 60_000L)
                if (ok) CalendarRepo.cacheToday(ctx)
                if (ok) "Evento criado em ${cal.name}." else "Falhou a criação do evento."
            }
            "add_goal" -> {
                val g = Goal(
                    title = input.s("title") ?: "Objetivo", area = input.s("area") ?: "Pessoal", why = input.s("why") ?: "",
                    targetDay = input.s("target_date")?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() },
                )
                Store.update { it.copy(goals = it.goals + g) }
                "Objetivo criado: ${g.title}"
            }
            "add_habit" -> {
                val h = Habit(name = input.s("name") ?: "Hábito", emoji = input.s("emoji") ?: "✨")
                Store.update { it.copy(habits = it.habits + h) }
                "Hábito criado: ${h.name}"
            }
            else -> "Ferramenta desconhecida: $name"
        }
    }
}
