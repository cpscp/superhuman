package com.carlos.superhuman.data

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** ETF usados nos planos. Símbolo = Yahoo Finance (para as cotações). Confirma sempre o ISIN na corretora. */
data class Etf(
    val symbol: String,
    val ticker: String,
    val name: String,
    val isin: String,
    val ter: String,
    val kind: String,
    val simple: String,       // explicação para principiantes
    val expected: Double,     // retorno anual hipotético no cenário base (%)
)

object EtfCatalog {
    val VWCE = Etf(
        "VWCE.DE", "VWCE", "Vanguard FTSE All-World UCITS ETF (Acc)", "IE00BK5BQT80", "≈0,19–0,22%/ano", "Ações do mundo inteiro",
        "Compra de uma vez cerca de 3 700 empresas de mais de 40 países (EUA, Europa, Japão, emergentes). É o \"compra o mundo e esquece\". Acumula: os dividendos são reinvestidos automaticamente.",
        7.0,
    )
    val VAGF = Etf(
        "VAGF.DE", "VAGF", "Vanguard Global Aggregate Bond UCITS ETF EUR Hedged (Acc)", "IE00BG47KH54", "≈0,10%/ano", "Obrigações globais",
        "Empresta dinheiro a milhares de governos e empresas. Sobe menos que as ações, mas também cai menos: amortece as quedas. Com cobertura cambial para euros.",
        3.0,
    )
    val XEON = Etf(
        "XEON.DE", "XEON", "Xtrackers II EUR Overnight Rate Swap UCITS ETF 1C", "LU0290358497", "≈0,10%/ano", "Monetário (quase sem risco)",
        "Rende aproximadamente a taxa de juro diária do BCE. Praticamente não oscila: serve para dinheiro de curto prazo, a render mais do que parado.",
        2.5,
    )
    val all = listOf(VWCE, VAGF, XEON)
    fun bySymbol(s: String) = all.firstOrNull { it.symbol.equals(s, true) || it.ticker.equals(s, true) }
}

val riskNames = listOf("Cauteloso", "Equilibrado", "Dinâmico")
val riskExplain = listOf(
    "Prefiro dormir descansado. Uma queda de 10% já me tirava o sono.",
    "Aceito altos e baixos se a longo prazo compensar. Uma queda de 20% custa, mas não vendo.",
    "Penso a 10+ anos. Mesmo que caia 30–40% num ano mau, mantenho e até compro mais.",
)

data class Allocation(val etf: Etf, val weight: Double)

data class PlanResult(
    val allocations: List<Allocation>,
    val effectiveRisk: Int,
    val emergencyNow: Double,           // do valor inicial, quanto vai para o fundo de emergência
    val investNow: Double,              // do valor inicial, quanto vai para ETF
    val emergencyMonthly: Double,
    val investMonthly: Double,
    val nowSplit: List<Pair<Etf, Double>>,
    val monthlySplit: List<Pair<Etf, Double>>,
    val notes: List<String>,
    val expectedBase: Double,
)

object InvestPlanner {

    fun allocationFor(risk: Int, horizon: Int): Pair<List<Allocation>, Int> {
        if (horizon < 3) return listOf(Allocation(EtfCatalog.XEON, 1.0)) to 0
        val r = if (horizon < 5) minOf(risk, 1) else risk
        return when (r) {
            0 -> listOf(Allocation(EtfCatalog.VWCE, 0.4), Allocation(EtfCatalog.XEON, 0.6))
            1 -> listOf(Allocation(EtfCatalog.VWCE, 0.75), Allocation(EtfCatalog.VAGF, 0.25))
            else -> listOf(Allocation(EtfCatalog.VWCE, 1.0))
        } to r
    }

    /** Despesa mensal típica (fixas + média do variável dos últimos 3 meses) para dimensionar o fundo de emergência. */
    fun monthlyCost(d: AppData): Double {
        val fixed = d.fixedExpenses.filter { it.active }.sumOf { it.amount }
        val now = YearMonth.now()
        val months = (1..3).map { now.minusMonths(it.toLong()) }
        val variable = months.map { ym -> d.expenses.filter { !it.pending && YearMonth.from(it.time.toLocalDate()) == ym }.sumOf { it.amount } }
            .filter { it > 0 }
        val avgVar = if (variable.isNotEmpty()) variable.average() else d.expensesIn(now).sumOf { it.amount }
        return fixed + avgVar
    }

    fun suggestedEmergency(d: AppData): Double {
        val c = monthlyCost(d)
        return if (c > 0) (c * 4 / 100).roundToInt() * 100.0 else 3000.0
    }

    /** Quanto sobrou do plafond em média nos últimos meses (capacidade de investimento). */
    fun monthlyCapacity(d: AppData): Double? {
        val now = YearMonth.now()
        val vals = (1..3).map { now.minusMonths(it.toLong()) }.mapNotNull { ym ->
            val s = d.monthSummary(ym); if (s.budget > 0) s.remaining else null
        }
        return if (vals.isEmpty()) null else max(0.0, vals.average())
    }

    fun build(p: InvestProfile, d: AppData): PlanResult {
        val (alloc, eff) = allocationFor(p.risk, p.horizonYears)
        val notes = mutableListOf<String>()
        val missing = if (p.hasEmergencyFund) 0.0 else max(0.0, p.emergencyTarget - p.emergencySaved)
        val emergencyNow = minOf(p.oneOff, missing)
        val investNow = p.oneOff - emergencyNow
        val stillMissing = missing - emergencyNow
        val emergencyMonthly = if (stillMissing > 0) (p.monthly / 2).roundToInt().toDouble() else 0.0
        val investMonthly = p.monthly - emergencyMonthly

        if (p.horizonYears < 3) notes += "Com menos de 3 anos, ações são arriscadas: se caírem perto de precisares do dinheiro, não há tempo para recuperar. Por isso o plano usa só o monetário."
        else if (p.horizonYears < 5 && p.risk == 2) notes += "Para menos de 5 anos baixei o risco para Equilibrado."
        if (missing > 0) notes += "Primeiro o fundo de emergência: faltam ${euro(missing, false)} para teres ~4 meses de despesas de lado. Guarda-o numa conta poupança ou em Certificados de Aforro, não em ações."
        if (stillMissing > 0 && p.monthly > 0) notes += "Até completares o fundo, metade do valor mensal (${euro(emergencyMonthly, false)}) vai para lá. Faltam ~${(stillMissing / emergencyMonthly.coerceAtLeast(1.0)).roundToInt()} meses."
        if (p.monthly in 0.01..24.99) notes += "Valores mensais muito baixos podem ser comidos pelas comissões. Procura uma corretora com planos de investimento automáticos sem comissão."

        fun split(total: Double): List<Pair<Etf, Double>> = alloc.map { it.etf to (total * it.weight).roundToInt().toDouble() }.filter { it.second > 0 }

        val expected = alloc.sumOf { it.etf.expected * it.weight }
        return PlanResult(alloc, eff, emergencyNow, investNow, emergencyMonthly, investMonthly, split(investNow), split(investMonthly), notes, expected)
    }

    /** Projeção anual (valor no fim de cada ano) para um retorno anual em %. */
    fun project(initial: Double, monthly: Double, years: Int, ratePct: Double): List<Double> {
        val r = (1 + ratePct / 100).pow(1.0 / 12) - 1
        var v = initial
        val out = mutableListOf(initial)
        repeat(years) {
            repeat(12) { v = v * (1 + r) + monthly }
            out += v
        }
        return out
    }

    data class Rebalance(val message: String, val target: Etf?)

    /** Compara o portfólio com o plano. Se algum ETF se afastar mais de 5 p.p., sugere onde pôr o próximo investimento. */
    fun rebalance(d: AppData, plan: PlanResult): Rebalance? {
        if (plan.allocations.size < 2) return null
        val lines = d.portfolioLines()
        val values = plan.allocations.associate { a -> a.etf to lines.filter { it.holding.symbol.equals(a.etf.symbol, true) }.sumOf { it.value } }
        val total = values.values.sum()
        if (total < 200) return null
        val drift = plan.allocations.map { a -> a to (values[a.etf]!! / total - a.weight) }
        val worst = drift.minByOrNull { it.second } ?: return null
        if (worst.second > -0.05) return null
        return Rebalance(
            "${worst.first.etf.ticker} está ${(-worst.second * 100).roundToInt()} p.p. abaixo do plano (${((values[worst.first.etf]!! / total) * 100).roundToInt()}% vs ${(worst.first.weight * 100).roundToInt()}%). Põe o próximo investimento todo em ${worst.first.etf.ticker} para reequilibrar, sem precisares de vender nada.",
            worst.first.etf,
        )
    }

    fun investedThisMonth(d: AppData): Double {
        val ym = YearMonth.now()
        return d.contributions.filter { it.symbol != "FUNDO" && YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == ym }.sumOf { it.amount }
    }

    fun monthStreak(d: AppData): Int {
        var ym = YearMonth.now()
        val months = d.contributions.map { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }.toSet()
        if (ym !in months) ym = ym.minusMonths(1)
        var n = 0
        while (ym in months) { n++; ym = ym.minusMonths(1) }
        return n
    }

    fun totalContributed(d: AppData) = d.contributions.filter { it.symbol != "FUNDO" }.sumOf { it.amount }

    fun tipOfDay(offset: Int = 0): String = InvestContent.tips[((LocalDate.now().dayOfYear + offset) % InvestContent.tips.size + InvestContent.tips.size) % InvestContent.tips.size]
}

data class Lesson(val id: String, val emoji: String, val title: String, val minutes: Int, val body: List<String>, val takeaway: String)

object InvestContent {
    val lessons = listOf(
        Lesson("porque", "🌱", "Porque investir (e não só poupar)", 2, listOf(
            "Dinheiro parado perde valor: com inflação de 3% ao ano, 10 000 € hoje compram o equivalente a cerca de 7 400 € daqui a 10 anos.",
            "Investir é pôr o dinheiro a trabalhar em empresas e obrigações que, a longo prazo, crescem acima da inflação.",
            "O grande aliado é o juro composto: os ganhos geram novos ganhos. Quanto mais cedo começares, mais trabalha por ti.",
        ), "O tempo no mercado conta mais do que o momento certo para entrar."),
        Lesson("emergencia", "🛟", "Primeiro: o fundo de emergência", 2, listOf(
            "Antes de investir, guarda 3 a 6 meses de despesas num sítio seguro e sempre disponível: conta poupança ou Certificados de Aforro.",
            "Serve para imprevistos (carro, saúde, desemprego) sem teres de vender investimentos numa altura má.",
            "Só o dinheiro de que não precisas nos próximos anos deve ir para ações.",
        ), "Fundo de emergência = paz de espírito para nunca venderes em pânico."),
        Lesson("etf", "🧺", "O que é um ETF", 3, listOf(
            "Um ETF é um cesto de muitas ações (ou obrigações) que compras como se fosse uma única ação na bolsa.",
            "Com um só ETF global ficas dono de um bocadinho de milhares de empresas: Apple, Nestlé, Toyota, ASML…",
            "Se uma empresa correr mal, pesa pouco no total. É diversificação automática e barata.",
            "Procura ETF \"UCITS\" (regulados na Europa) e com custo anual (TER) baixo, idealmente abaixo de 0,25%.",
        ), "Um ETF global é a forma mais simples de investir no mundo inteiro."),
        Lesson("acc", "🔁", "Acumulação vs distribuição", 2, listOf(
            "ETF de distribuição (Dist) pagam-te os dividendos em dinheiro, que em Portugal são tributados a 28% todos os anos.",
            "ETF de acumulação (Acc) reinvestem os dividendos automaticamente. Só pagas imposto quando venderes.",
            "Para quem está a construir património a longo prazo, Acc é normalmente mais simples e eficiente.",
        ), "Os ETF do teu plano são todos de acumulação."),
        Lesson("risco", "🎢", "Risco e volatilidade", 3, listOf(
            "As ações sobem e descem. Em 2008 e 2020 o mercado mundial chegou a cair mais de 30%, e recuperou nos anos seguintes.",
            "Quedas são normais e temporárias para quem tem horizonte longo. O erro mais caro é vender durante a queda.",
            "Obrigações e monetário oscilam menos: misturá-los com ações reduz os sustos (e também o retorno esperado).",
            "Escolhe um risco com que consigas viver nos dias maus, não só nos bons.",
        ), "O teu perfil de risco é o que consegues aguentar sem vender."),
        Lesson("dca", "📅", "Investir todos os meses (DCA)", 2, listOf(
            "Em vez de tentar adivinhar o melhor momento, investes o mesmo valor todos os meses, aconteça o que acontecer.",
            "Quando o mercado cai, o mesmo valor compra mais unidades. Quando sobe, compra menos. Fica tudo pela média.",
            "É automático, tira a emoção da decisão e cria o hábito. Muitas corretoras têm planos automáticos para isto.",
        ), "Consistência vence timing."),
        Lesson("custos", "🧾", "Custos que fazem diferença", 2, listOf(
            "Há três custos a olhar: o TER do ETF (anual), a comissão de compra da corretora e eventuais custos de câmbio.",
            "1% ao ano parece pouco, mas em 30 anos pode comer mais de 20% do resultado final.",
            "Prefere ETF baratos e corretoras com comissões baixas ou planos de investimento sem comissão.",
        ), "Cada 0,1% poupado em custos fica a trabalhar para ti."),
        Lesson("corretora", "🏦", "Como escolher uma corretora", 3, listOf(
            "A corretora é onde compras os ETF. Confirma que é regulada (CMVM ou outro supervisor da UE) e que os teus ativos ficam segregados.",
            "Compara: comissões, se tem os ETF do plano, se permite planos automáticos mensais e se tem app simples.",
            "Exemplos usados em Portugal: o teu banco, Trade Republic, XTB, DEGIRO, Interactive Brokers. Isto não é uma recomendação; compara as condições atuais.",
            "Corretoras estrangeiras normalmente não retêm IRS: declaras tu as vendas e dividendos no IRS.",
        ), "Regulada, barata e simples: esses são os três critérios."),
        Lesson("impostos", "🇵🇹", "Impostos em Portugal (o essencial)", 3, listOf(
            "Ganhos na venda (mais-valias) e dividendos são tributados, por regra, a 28%. Só pagas imposto sobre o lucro, quando vendes.",
            "Podes optar pelo englobamento (somar ao teu rendimento); só compensa com rendimentos baixos. Ganhos de ativos detidos menos de 1 ano podem ter englobamento obrigatório para rendimentos muito altos.",
            "Com corretora estrangeira, as operações declaram-se no Anexo J do IRS.",
            "As regras mudam: confirma no Portal das Finanças ou com um contabilista antes de vender.",
        ), "Com ETF de acumulação e sem vender, não há imposto a pagar todos os anos."),
        Lesson("comportamento", "🧠", "O inimigo é o comportamento", 2, listOf(
            "Os maiores erros: vender em pânico nas quedas, comprar o que está na moda, mexer demasiado na carteira.",
            "Define o plano, automatiza o investimento mensal e olha para a carteira poucas vezes.",
            "Revê uma vez por ano: o risco ainda faz sentido? Precisas de reequilibrar?",
        ), "Um plano chato seguido durante anos ganha a quase tudo."),
    )

    val tips = listOf(
        "Começa com pouco, mas começa: 50 € por mês durante 20 anos a 6% são cerca de 23 000 €, dos quais só 12 000 € saíram do teu bolso.",
        "Escolhe um dia fixo do mês para investir, logo a seguir a receberes o salário. Paga-te a ti primeiro.",
        "Não precisas de 10 ETF. Um ETF global já te dá milhares de empresas.",
        "Quando o mercado cair, não é altura de vender: é a altura em que o teu investimento mensal compra mais barato.",
        "Antes de comprar um ETF, confirma o ISIN na corretora. O nome pode variar, o ISIN não.",
        "Evita olhar para a carteira todos os dias. Os números diários são ruído.",
        "O TER é cobrado automaticamente dentro do ETF. Não aparece como despesa, mas reduz o retorno.",
        "Se uma dica parece demasiado boa para ser verdade (retornos garantidos, enriquecer rápido), é.",
        "Ações individuais são mais arriscadas que ETF. Se quiseres experimentar, limita-as a 5–10% da carteira.",
        "Reequilibrar não obriga a vender: basta pôr os próximos investimentos no ETF que ficou abaixo do plano.",
        "Aumenta o investimento mensal sempre que tiveres um aumento de salário, mesmo que só 10% do aumento.",
        "Os Certificados de Aforro são bons para o fundo de emergência: garantidos pelo Estado e resgatáveis.",
        "Inflação a 2% corta para metade o poder de compra em cerca de 35 anos. Investir é proteção.",
        "Guarda os comprovativos das compras: vais precisar do preço de compra para o IRS quando venderes.",
        "Diversificar por países também conta: os EUA pesam muito, mas não são o mundo todo.",
        "Uma queda de 50% precisa de uma subida de 100% para recuperar. Por isso o risco deve caber no teu perfil.",
        "Prefere ETF de acumulação para não pagares imposto sobre dividendos todos os anos.",
        "Crypto não é ETF de ações: é muito mais volátil. Se entrar, que seja uma fatia pequena que aceites perder.",
        "Não invistas dinheiro de que vais precisar em menos de 3 anos em ações.",
        "Tens dívidas caras (cartão de crédito, crédito pessoal)? Pagá-las primeiro é um \"retorno\" garantido de 10–20%.",
        "O melhor momento para investir foi há 10 anos. O segundo melhor é hoje.",
        "Automatiza: um plano automático na corretora faz o trabalho por ti e evita esquecimentos.",
        "Taxas de câmbio contam: comprar ETF cotados em euros evita custos de conversão.",
        "Um ETF não vai à falência como uma empresa: os ativos estão separados da gestora.",
        "Faz uma revisão anual: idade, objetivos e risco mudam, e o plano pode acompanhar.",
        "Mexer muito na carteira gera comissões e impostos. Menos é mais.",
        "Investir não é jogar: o objetivo é crescer devagar e de forma consistente durante anos.",
        "Se o mercado cair 20% e não dormires, o teu risco está alto demais. Ajusta para Equilibrado ou Cauteloso.",
        "Dividendos altos não são sinónimo de bom investimento. Conta o retorno total.",
        "Regista cada compra na app: assim vês o teu progresso real e o plano sabe quando reequilibrar.",
    )
}
