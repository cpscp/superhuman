package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Holding
import com.carlos.superhuman.data.HoldingSignal
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.pct
import com.carlos.superhuman.data.portfolioLines
import com.carlos.superhuman.data.today
import com.carlos.superhuman.net.Claude
import com.carlos.superhuman.net.Market
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineChart
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.Sparkline
import com.carlos.superhuman.ui.components.Tag
import com.carlos.superhuman.ui.components.parseMoney
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import kotlinx.coroutines.launch

@Composable
fun signalColor(action: String): Color = when (action.uppercase()) {
    "REFORÇAR", "COMPRAR" -> SH.c.good
    "VENDER" -> SH.c.bad
    "REDUZIR", "REVER" -> SH.c.warn
    else -> SH.c.ink2
}

@Composable
fun InvestScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var addHolding by remember { mutableStateOf<Holding?>(null) }
    val feed = d.investFeed

    fun refresh(force: Boolean) {
        if (loading) return
        loading = true
        scope.launch {
            Market.refreshQuotes()
            val r = Market.refreshFeed(forceGenerate = force)
            loading = false
            r.exceptionOrNull()?.let { Toast.makeText(ctx, it.message ?: "Erro", Toast.LENGTH_LONG).show() }
        }
    }

    LaunchedEffect(Unit) {
        if (d.holdings.isNotEmpty()) Market.refreshQuotes()
        if (feed == null || feed.date != today().toString()) refresh(false)
    }

    ScreenScaffold(
        title = "Invest",
        onBack = null,
        actions = {
            if (loading) Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), color = c.ink, strokeWidth = 2.dp) }
            else CircleIconButton(Icons.Rounded.Refresh) { refresh(false) }
            CircleIconButton(Icons.Rounded.Add, filled = true) { addHolding = Holding(symbol = "", quantity = 0.0, avgPrice = 0.0) }
        },
    ) {
        item {
            Segmented(listOf("Coach", "Portfólio", "Business"), tab, Modifier.fillMaxWidth()) { tab = it }
            Spacer(Modifier.height(12.dp))
        }
        when (tab) {
            0 -> {
                item {
                    if (feed == null) {
                        SHCard(Modifier.fillMaxWidth()) {
                            Text("Ainda sem análise de hoje", style = Type.title, color = c.ink)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (Claude.hasKey()) "Toca em gerar para a Claude pesquisar o mercado e preparar as dicas do dia."
                                else "A análise diária é publicada todas as manhãs às 06:34. Toca em atualizar para a ir buscar.",
                                style = Type.small, color = c.ink2,
                            )
                            Spacer(Modifier.height(12.dp))
                            if (Claude.hasKey()) PrimaryButton("Gerar análise agora", Modifier.fillMaxWidth(), enabled = !loading, icon = Icons.Rounded.AutoAwesome) { refresh(true) }
                            else PrimaryButton("Atualizar feed", Modifier.fillMaxWidth(), enabled = !loading, icon = Icons.Rounded.Refresh) { refresh(false) }
                        }
                    } else {
                        SHCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mercado hoje", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                                val moodColor = when (feed.mood.lowercase()) { "risk-on" -> c.good; "risk-off" -> c.bad; else -> c.warn }
                                Tag(feed.mood.ifBlank { "neutro" }, moodColor)
                            }
                            Text("${feed.date} · ${feed.generatedBy}", style = Type.tiny, color = c.ink3)
                            Spacer(Modifier.height(10.dp))
                            Text(feed.marketSummary, style = Type.body, color = c.ink)
                            if (feed.indices.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    feed.indices.forEach { ix ->
                                        val up = !ix.change.trim().startsWith("-") && !ix.change.trim().startsWith("−")
                                        InsetBox(padding = 10.dp) {
                                            Text(ix.name, style = Type.tiny, color = c.ink2)
                                            Text(ix.change, style = Type.title, color = if (up) c.good else c.bad)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (Claude.hasKey()) SecondaryButton("Gerar nova análise", Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome) { refresh(true) }
                            else SecondaryButton("Analisar o meu portfólio no Claude", Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome) {
                                com.carlos.superhuman.net.LocalAssistant.openInClaude(ctx, "Com base no meu portfólio e no mercado de hoje (" + feed.marketSummary.take(300) + "), diz-me para cada posição se devo manter, reforçar ou reduzir, e onde investir mais com potencial. Sou investidor moderado, horizonte 5-10 anos, em Portugal.")
                            }
                        }
                    }
                }
                if (feed != null) {
                    item { SectionHeader("Onde investir · ideias do dia") }
                    items(feed.tips) { tip ->
                        SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lightbulb, null, tint = c.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tip.title, style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                            }
                            if (tip.instrument.isNotBlank()) Text(tip.instrument, style = Type.small, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(tip.body, style = Type.body, color = c.ink)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TipFact("Montante", tip.suggestedAmount, Modifier.weight(1f))
                                TipFact("Potencial", tip.potential, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (tip.horizon.isNotBlank()) Tag("Horizonte ${tip.horizon}", c.ink2)
                                if (tip.risk.isNotBlank()) Tag("Risco ${tip.risk}", if (tip.risk.contains("alto", true)) c.bad else if (tip.risk.contains("médio", true)) c.warn else c.good)
                            }
                        }
                    }
                    item { Text(feed.disclaimer, style = Type.tiny, color = c.ink3, modifier = Modifier.padding(vertical = 8.dp)) }
                }
            }
            1 -> {
                val lines = d.portfolioLines()
                item {
                    val value = lines.sumOf { it.value }
                    val invested = lines.sumOf { it.invested }
                    val dayChange = lines.sumOf { it.value * it.dayPct / 100 }
                    SHCard(Modifier.fillMaxWidth()) {
                        Text("Valor do portfólio", style = Type.small, color = c.ink2)
                        Text(euro(value), style = Type.number, color = c.ink)
                        Row {
                            Text("${if (value - invested >= 0) "+" else ""}${euro(value - invested)} (${pct(if (invested > 0) (value / invested - 1) * 100 else 0.0)})", style = Type.small, color = if (value >= invested) c.good else c.bad)
                            Spacer(Modifier.width(10.dp))
                            Text("hoje ${euro(dayChange)}", style = Type.small, color = c.ink2)
                        }
                        Spacer(Modifier.height(14.dp))
                        val snaps = d.snapshots.takeLast(120)
                        if (snaps.size >= 2) {
                            LineChart(snaps.map { it.value }, reference = snaps.map { it.invested }, color = c.accent)
                            Text("Linha a tracejado = valor investido", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
                        } else Text("O gráfico de evolução enche-se a partir de amanhã (um ponto por dia).", style = Type.small, color = c.ink2)
                    }
                    SectionHeader("Posições")
                }
                if (lines.isEmpty()) item { EmptyState("Sem posições", "Adiciona o que investiste no + (símbolo Yahoo, ex.: VUAA.L, SXR8.DE, AAPL).") }
                items(lines, key = { it.holding.id }) { l ->
                    val signal: HoldingSignal? = feed?.signals?.firstOrNull { it.symbol.equals(l.holding.symbol, true) || it.symbol.equals(l.holding.symbol.substringBefore("."), true) }
                    SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { addHolding = l.holding }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.holding.symbol, style = Type.title, color = c.ink)
                                Text(l.holding.name.ifBlank { "${l.holding.quantity} un · PM ${"%.2f".format(l.holding.avgPrice)}" }, style = Type.tiny, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Sparkline(l.history, Modifier.width(70.dp).height(30.dp), color = if (l.dayPct >= 0) c.good else c.bad)
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(euro(l.value), style = Type.title, color = c.ink)
                                Text(pct(l.plPct), style = Type.tiny, color = if (l.pl >= 0) c.good else c.bad)
                            }
                        }
                        if (signal != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(signalColor(signal.action).copy(alpha = 0.1f)).padding(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(signal.action.uppercase(), style = Type.tiny, color = signalColor(signal.action))
                                Spacer(Modifier.width(8.dp))
                                Text(signal.reason, style = Type.small, color = c.ink)
                            }
                        }
                    }
                }
            }
            else -> {
                val news = feed?.news.orEmpty()
                if (news.isEmpty()) item { EmptyState("Sem notícias", "Atualiza o Coach para receber o resumo de business do dia.") }
                items(news) { n ->
                    SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = {
                        if (n.url.isNotBlank()) runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.url))) }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Newspaper, null, tint = c.ink2, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(n.source, style = Type.tiny, color = c.ink2)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(n.title, style = Type.title, color = c.ink)
                        if (n.summary.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(n.summary, style = Type.small, color = c.ink2) }
                        if (n.relevance.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            InsetBox(padding = 10.dp) { Text("Porque importa: ${n.relevance}", style = Type.small, color = c.ink) }
                        }
                    }
                }
            }
        }
    }

    addHolding?.let { HoldingSheet(it) { addHolding = null; scope.launch { Market.refreshQuotes() } } }
}

@Composable
private fun TipFact(label: String, value: String, modifier: Modifier) {
    InsetBox(modifier, padding = 10.dp) {
        Text(label, style = Type.tiny, color = SH.c.ink2)
        Text(value.ifBlank { "—" }, style = Type.small, color = SH.c.ink)
    }
}

@Composable
private fun HoldingSheet(initial: Holding, onDismiss: () -> Unit) {
    val exists = Store.data.holdings.any { it.id == initial.id }
    var symbol by remember { mutableStateOf(initial.symbol) }
    var name by remember { mutableStateOf(initial.name) }
    var qty by remember { mutableStateOf(if (initial.quantity > 0) initial.quantity.toString() else "") }
    var price by remember { mutableStateOf(if (initial.avgPrice > 0) initial.avgPrice.toString() else "") }
    Sheet(if (exists) "Editar posição" else "Nova posição", onDismiss) {
        SHTextField(symbol, { symbol = it.uppercase().trim() }, "Símbolo (ex.: VUAA.L, SXR8.DE, AAPL)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SHTextField(name, { name = it }, "Nome (ex.: Vanguard S&P 500)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SHTextField(qty, { qty = it }, "Quantidade", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            SHTextField(price, { price = it }, "Preço médio", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exists) SecondaryButton("Apagar") { Store.update { d -> d.copy(holdings = d.holdings.filter { it.id != initial.id }) }; onDismiss() }
            PrimaryButton("Guardar", Modifier.weight(1f), enabled = symbol.isNotBlank() && parseMoney(qty) != null && parseMoney(price) != null) {
                val h = initial.copy(symbol = symbol, name = name, quantity = parseMoney(qty) ?: 0.0, avgPrice = parseMoney(price) ?: 0.0)
                Store.update { d -> d.copy(holdings = if (exists) d.holdings.map { if (it.id == h.id) h else it } else d.holdings + h) }
                onDismiss()
            }
        }
    }
}
