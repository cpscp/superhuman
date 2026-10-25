package com.carlos.superhuman.net

import com.carlos.superhuman.data.AppData
import com.carlos.superhuman.data.Priority
import com.carlos.superhuman.data.beersOn
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.endMillis
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.data.weekStart
import com.carlos.superhuman.device.HealthSnapshot
import java.time.LocalDate
import java.time.YearMonth

/** Reflexões e sugestões geradas localmente a partir dos dados (sem API, sem custos). */
object Reflection {

    fun build(d: AppData, weekly: Boolean, mood: Int, wins: String, improve: String, next: String): String {
        val out = mutableListOf<String>()
        val today = LocalDate.now()
        val from = if (weekly) weekStart().startMillis() else today.startMillis()
        val to = today.endMillis()
        val done = d.tasks.filter { (it.completedAt ?: 0L) in from until to }
        val open = d.tasks.filter { !it.done }
        val highOpen = open.count { it.priority == Priority.HIGH }
        val late = open.count { (it.dueDay ?: Long.MAX_VALUE) < todayEpoch() }

        // 1. O que correu bem
        out += when {
            done.size >= (if (weekly) 15 else 5) -> "Excelente ritmo: ${done.size} tasks concluídas ${if (weekly) "esta semana" else "hoje"}."
            done.isNotEmpty() -> "Fechaste ${done.size} task${if (done.size > 1) "s" else ""}" + (done.firstOrNull { it.priority == Priority.HIGH }?.let { ", incluindo \"${it.title}\"" } ?: "") + "."
            else -> "Não fechaste tasks ${if (weekly) "esta semana" else "hoje"}; às vezes o dia é de reuniões e apagar fogos, e está tudo bem."
        }
        if (wins.isNotBlank()) out += "Fica registado como vitória: ${wins.lines().first().trim().trimEnd('.')}."

        // 2. Padrões nos dados
        val m = d.monthSummary()
        if (m.budget > 0) {
            val ym = YearMonth.now()
            val expectedShare = today.dayOfMonth.toDouble() / ym.lengthOfMonth()
            val usedShare = (m.spent) / (m.budget - m.fixed).coerceAtLeast(1.0)
            out += when {
                usedShare > expectedShare + 0.15 -> "Nas finanças vais acima do ritmo: já usaste ${(usedShare * 100).toInt()}% do variável com ${(expectedShare * 100).toInt()}% do mês passado. ${m.byCategory.firstOrNull()?.let { "${it.first} é onde mais pesa (${euro(it.second, false)})." } ?: ""}"
                usedShare < expectedShare - 0.1 -> "Nas finanças estás abaixo do ritmo previsto, com ${euro(m.remaining)} ainda disponíveis."
                else -> "Os gastos estão alinhados com o plafond."
            }
        }
        val beers = if (weekly) d.beersThisWeek() else d.beersOn(todayEpoch())
        val limit = d.settings.beerWeeklyLimit
        if (weekly && beers > limit) out += "Passaste o limite de cervejas ($beers de $limit). Escolhe já dois dias sem álcool para a próxima semana."
        else if (!weekly && beers >= 3) out += "$beers cervejas hoje; amanhã vale a pena ser um dia sem álcool."
        val habitsWeek = d.habits.map { h -> h to h.doneDays.count { it >= weekStart().toEpochDay() } }
        habitsWeek.minByOrNull { (h, c) -> c.toDouble() / h.targetPerWeek }?.let { (h, c) ->
            if (c < h.targetPerWeek / 2) out += "O hábito mais esquecido é \"${h.name}\" ($c/${h.targetPerWeek})."
        }
        if (mood <= 2) out += "O humor esteve em baixo: protege um bloco de descanso amanhã, sem reuniões."

        // 3. Ações
        val actions = mutableListOf<String>()
        next.lines().map { it.trim().trimStart('-', '•', '*', ' ') }.firstOrNull { it.isNotBlank() }?.let { actions += "começa por \"$it\" logo de manhã" }
        if (highOpen > 0 && actions.isEmpty()) actions += "ataca primeiro as $highOpen task${if (highOpen > 1) "s" else ""} de prioridade alta"
        if (late > 0) actions += "reagenda ou elimina as $late task${if (late > 1) "s" else ""} atrasadas"
        if (improve.isNotBlank()) actions += "para melhorar \"${improve.lines().first().trim().trimEnd('.')}\", define um passo de 10 minutos"
        if (actions.isEmpty()) actions += "escolhe a única coisa que tornaria ${if (weekly) "a próxima semana" else "amanhã"} um sucesso"
        out += (if (weekly) "Para a próxima semana: " else "Para amanhã: ") + actions.take(2).joinToString(" e ") + "."
        return out.joinToString(" ")
    }

    fun healthTips(d: AppData, s: HealthSnapshot?): String {
        val tips = mutableListOf<String>()
        val steps7 = s?.steps7.orEmpty()
        val avgSteps = if (steps7.isNotEmpty()) steps7.average() else 0.0
        if (s != null) {
            when {
                avgSteps in 1.0..5999.0 -> tips += "🚶 Média de ${avgSteps.toInt()} passos/dia: junta uma caminhada de 20 min depois do almoço."
                avgSteps >= 10000 -> tips += "🚶 ${avgSteps.toInt()} passos/dia em média, ótimo; mantém."
                avgSteps > 0 -> tips += "🚶 Estás perto dos 8 000 passos; desce uma paragem antes no regresso a casa."
            }
            val sleeps = s.sleep7.filter { it > 0 }
            if (sleeps.isNotEmpty()) {
                val avg = sleeps.average()
                tips += if (avg < 420) "😴 Dormes em média ${avg.toInt() / 60}h${avg.toInt() % 60}; tenta deitar 30 min mais cedo durante 3 noites."
                else "😴 Sono médio de ${avg.toInt() / 60}h${avg.toInt() % 60}, dentro do recomendado."
            }
            if (s.workouts.isEmpty() && d.stravaActivities.isEmpty()) tips += "🏃 Sem treinos nas últimas 2 semanas: marca 2 sessões curtas na agenda."
        }
        val beers = d.beersThisWeek(); val limit = d.settings.beerWeeklyLimit
        tips += when {
            beers > limit -> "🍺 $beers cervejas esta semana (limite $limit): troca as próximas por água com gás ou sem álcool."
            beers >= limit * 0.75 -> "🍺 Estás perto do limite semanal ($beers/$limit); guarda margem para o fim de semana."
            else -> "🍺 Bom controlo de álcool ($beers/$limit)."
        }
        d.habits.minByOrNull { h -> h.doneDays.count { it >= weekStart().toEpochDay() }.toDouble() / h.targetPerWeek }?.let { h ->
            val c = h.doneDays.count { it >= weekStart().toEpochDay() }
            if (c < h.targetPerWeek) tips += "${h.emoji} \"${h.name}\" vai em $c/${h.targetPerWeek}; liga-o a algo que já fazes (ex.: depois do café)."
        }
        if (d.habits.isEmpty()) tips += "✨ Ainda sem hábitos: começa só com um, como beber 2 L de água."
        return tips.take(4).joinToString("\n")
    }
}
