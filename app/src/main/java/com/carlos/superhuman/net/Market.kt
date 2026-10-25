package com.carlos.superhuman.net

import com.carlos.superhuman.data.InvestFeed
import com.carlos.superhuman.data.PortfolioSnapshot
import com.carlos.superhuman.data.Quote
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.portfolioLines
import com.carlos.superhuman.data.today
import com.carlos.superhuman.data.todayEpoch
import java.net.URLEncoder

object Market {

    /** Cotação + histórico 1 mês (Yahoo Finance, sem chave). Símbolos tipo VUAA.L, SXR8.DE, AAPL. */
    suspend fun quote(symbol: String): Quote? = runCatching {
        val s = URLEncoder.encode(symbol.trim(), "UTF-8")
        val root = Http.parse(Http.get("https://query1.finance.yahoo.com/v8/finance/chart/$s?range=1mo&interval=1d"))
        val res = root.obj("chart").arr("result")?.firstOrNull() ?: return@runCatching null
        val meta = res.obj("meta")
        val closes = res.obj("indicators").arr("quote")?.firstOrNull().arr("close")
            ?.mapNotNull { it.asDoubleOrNull() } ?: emptyList()
        val price = meta.num("regularMarketPrice") ?: closes.lastOrNull() ?: return@runCatching null
        Quote(
            symbol = symbol,
            price = price,
            previousClose = meta.num("chartPreviousClose") ?: meta.num("previousClose") ?: price,
            currency = meta.str("currency") ?: "",
            history = closes,
        )
    }.getOrNull()

    suspend fun refreshQuotes() {
        val symbols = Store.data.holdings.map { it.symbol }.distinct()
        val fresh = symbols.mapNotNull { quote(it) }.associateBy { it.symbol }
        Store.update { d -> d.copy(quotes = d.quotes + fresh) }
        recordSnapshot()
    }

    private fun recordSnapshot() {
        val lines = Store.data.portfolioLines()
        if (lines.isEmpty()) return
        val snap = PortfolioSnapshot(todayEpoch(), lines.sumOf { it.value }, lines.sumOf { it.invested })
        Store.update { d -> d.copy(snapshots = (d.snapshots.filter { it.epochDay != snap.epochDay } + snap).sortedBy { it.epochDay }.takeLast(730)) }
    }

    /** 1) Tenta o feed publicado diariamente (GitHub). 2) Se falhar ou estiver desatualizado, gera com a Claude API. */
    suspend fun refreshFeed(forceGenerate: Boolean = false): Result<InvestFeed> = runCatching {
        val s = Store.data.settings
        var feed: InvestFeed? = null
        if (!forceGenerate && s.investFeedUrl.isNotBlank()) {
            feed = runCatching {
                Store.json.decodeFromString(InvestFeed.serializer(), Http.get(s.investFeedUrl + "?t=" + System.currentTimeMillis()))
            }.getOrNull()
        }
        val stale = feed == null || feed.date != today().toString()
        if ((stale && s.investAutoGenerate && Claude.hasKey()) || forceGenerate) {
            feed = generateFeed()
        }
        val result = feed ?: error("Ainda não há feed publicado. A análise diária aparece todas as manhãs às 06:34.")
        Store.update { it.copy(investFeed = result) }
        result
    }

    suspend fun generateFeed(): InvestFeed {
        val d = Store.data
        val portfolio = d.portfolioLines().joinToString("\n") {
            "- ${it.holding.symbol} (${it.holding.name}): ${it.holding.quantity} un, preço médio ${it.holding.avgPrice}, atual ${"%.2f".format(it.price)}, P/L ${"%.1f".format(it.plPct)}%"
        }.ifBlank { "(portfólio vazio)" }
        val system = """
            És o coach de investimento pessoal do Carlos, em português de Portugal.
            Hoje é ${today()}. Pesquisa na web o estado dos mercados (S&P 500, Nasdaq, Stoxx 600, PSI, juros, petróleo, EUR/USD, cripto) e notícias de negócio relevantes das últimas 24-48h.
            Perfil do investidor: ${d.settings.investProfile}
            Dá análise concreta e educativa: ideias de onde investir com potencial (ETF UCITS, ações, obrigações), com montante indicativo e potencial estimado (cenário base, com risco).
            Sê honesto sobre incerteza. Não prometas retornos. Isto é informação, não aconselhamento financeiro personalizado.
            Responde APENAS com JSON válido, sem texto antes ou depois, neste formato exato:
            {"date":"${today()}","generatedBy":"Claude","marketSummary":"3-5 frases","mood":"risk-on|neutro|risk-off",
             "indices":[{"name":"S&P 500","change":"+0,4%"}],
             "tips":[{"title":"","instrument":"ticker/nome","body":"porquê, 2-4 frases","horizon":"curto/médio/longo","risk":"baixo/médio/alto","suggestedAmount":"ex.: 100-200 €/mês","potential":"ex.: 6-9%/ano cenário base"}],
             "news":[{"title":"","source":"","url":"","summary":"1-2 frases","relevance":"porque importa para investir"}],
             "signals":[{"symbol":"","action":"MANTER|REFORÇAR|REDUZIR|VENDER|REVER","reason":"1-2 frases"}],
             "disclaimer":"..."}
            3 a 5 tips, 5 a 8 notícias, um signal por cada posição do portfólio.
        """.trimIndent()
        val prompt = "O meu portfólio atual:\n$portfolio\n\nFaz a análise de hoje."
        val text = Claude.ask(system, prompt, webSearch = true, maxTokens = 6000)
        val json = Claude.extractJson(text) ?: error("Resposta sem JSON")
        return Store.json.decodeFromString(InvestFeed.serializer(), json)
    }
}
