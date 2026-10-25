package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Expense
import com.carlos.superhuman.data.ExpenseSource
import com.carlos.superhuman.data.ShoppingItem
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.SuperTrip
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.fmtMonth
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.CategoryBars
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.DetailRow
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.HatchBarChart
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.MiniStatRow
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHCheck
import com.carlos.superhuman.ui.components.SHSwitch
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.components.parseMoney
import com.carlos.superhuman.ui.components.pickDate
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object SuperOps {
    fun addTrip(trip: SuperTrip, alsoExpense: Boolean) = Store.update { d ->
        if (alsoExpense) {
            val e = Expense(
                time = LocalDate.ofEpochDay(trip.epochDay).startMillis() + 12 * 3_600_000L,
                amount = trip.amount, merchant = trip.store, category = "Supermercado", source = ExpenseSource.SUPERMERCADO,
            )
            d.copy(superTrips = d.superTrips + trip.copy(expenseId = e.id), expenses = d.expenses + e)
        } else d.copy(superTrips = d.superTrips + trip)
    }

    fun deleteTrip(t: SuperTrip) = Store.update { d ->
        d.copy(superTrips = d.superTrips.filter { it.id != t.id }, expenses = d.expenses.filter { it.id != t.expenseId })
    }

    fun addItem(name: String, aisle: String = "Geral") = Store.update { d -> d.copy(shopping = d.shopping + ShoppingItem(name = name, aisle = aisle)) }
    fun toggleItem(id: String) = Store.update { d -> d.copy(shopping = d.shopping.map { if (it.id == id) it.copy(checked = !it.checked) else it }) }
    fun clearChecked() = Store.update { d -> d.copy(shopping = d.shopping.filter { !it.checked }) }

    /** Importa CSV exportado do Excel: data;supermercado;valor (aceita , ou ; e datas dd/MM/yyyy ou yyyy-MM-dd). */
    fun importCsv(text: String): Int {
        val trips = mutableListOf<SuperTrip>()
        val fmts = listOf("dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy").map { DateTimeFormatter.ofPattern(it) }
        text.lines().forEach { line ->
            val parts = line.split(';', '\t').let { if (it.size >= 3) it else line.split(',') }.map { it.trim().trim('"') }
            if (parts.size < 3) return@forEach
            val date = fmts.firstNotNullOfOrNull { f -> runCatching { LocalDate.parse(parts[0], f) }.getOrNull() } ?: return@forEach
            val amount = parseMoney(parts.drop(2).joinToString(".").takeIf { parts.size > 3 && parts[2].all(Char::isDigit) } ?: parts[2]) ?: return@forEach
            trips += SuperTrip(epochDay = date.toEpochDay(), store = parts[1], amount = amount)
        }
        if (trips.isNotEmpty()) Store.update { d ->
            val existing = d.superTrips.map { Triple(it.epochDay, it.store, it.amount) }.toSet()
            d.copy(superTrips = d.superTrips + trips.filter { Triple(it.epochDay, it.store, it.amount) !in existing })
        }
        return trips.size
    }
}

@Composable
fun SuperScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var addTrip by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf("") }
    var aisle by remember { mutableStateOf("Geral") }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            val n = SuperOps.importCsv(text)
            Toast.makeText(ctx, "$n idas importadas", Toast.LENGTH_SHORT).show()
        }
    }
    val year = LocalDate.now().year
    val yearTrips = d.superTrips.filter { LocalDate.ofEpochDay(it.epochDay).year == year }
    val thisMonth = d.superTrips.filter { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == YearMonth.now() }

    ScreenScaffold(
        title = "Supermercado",
        onBack = { Nav.back() },
        actions = {
            CircleIconButton(Icons.Rounded.FileUpload) { importer.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel")) }
            CircleIconButton(Icons.Rounded.Add, filled = true) { if (tab != 2) addTrip = true }
        },
    ) {
        item {
            Segmented(listOf("Painel", "Idas", "Lista de compras"), tab, Modifier.fillMaxWidth()) { tab = it }
            Spacer(Modifier.height(12.dp))
        }
        when (tab) {
            0 -> {
                item {
                    MiniStatRow(
                        listOf(
                            "Este mês" to euro(thisMonth.sumOf { it.amount }, false),
                            "Idas no mês" to thisMonth.size.toString(),
                            "Média/ida" to euro(if (yearTrips.isNotEmpty()) yearTrips.sumOf { it.amount } / yearTrips.size else 0.0, false),
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    val months = (11 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
                    val spend = months.map { ym -> d.superTrips.filter { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == ym }.sumOf { it.amount } }
                    val visits = months.map { ym -> d.superTrips.count { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == ym }.toDouble() }
                    SHCard(Modifier.fillMaxWidth()) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Gasto e idas · 12 meses", style = Type.title, color = c.ink)
                                Text("Barras = gasto · linha = nº de idas", style = Type.tiny, color = c.ink3)
                            }
                            Text(euro(yearTrips.sumOf { it.amount }, false), style = Type.h2, color = c.ink)
                        }
                        Spacer(Modifier.height(12.dp))
                        val maxS = spend.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                        val maxV = visits.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                        HatchBarChart(
                            values = spend,
                            highlight = 11,
                            overlay = visits.map { it / maxV * maxS * 0.9 },
                            labels = months.map { DateTimeFormatter.ofPattern("MMMMM", com.carlos.superhuman.data.PT).format(it).uppercase() },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val byStore = yearTrips.groupBy { it.store }.map { it.key to it.value.sumOf { t -> t.amount } }.sortedByDescending { it.second }
                    val freq = yearTrips.groupBy { it.store }.maxByOrNull { it.value.size }
                    SHCard(Modifier.fillMaxWidth()) {
                        Text("Supermercados em $year", style = Type.title, color = c.ink)
                        if (freq != null) Text("Mais frequente: ${freq.key} (${freq.value.size} idas)", style = Type.small, color = c.ink2)
                        Spacer(Modifier.height(12.dp))
                        if (byStore.isEmpty()) Text("Sem idas registadas este ano.", style = Type.small, color = c.ink2)
                        else CategoryBars(byStore, yearTrips.sumOf { it.amount })
                    }
                    Spacer(Modifier.height(10.dp))
                    SHCard(Modifier.fillMaxWidth()) {
                        Text("Importar o histórico", style = Type.title, color = c.ink)
                        Text("Exporta cada mês do Excel como CSV (data;supermercado;valor) e importa no ícone ↑. Duplicados são ignorados.", style = Type.small, color = c.ink2)
                    }
                }
            }
            1 -> {
                val grouped = d.superTrips.sortedByDescending { it.epochDay }.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }
                if (grouped.isEmpty()) item { EmptyState("Sem idas", "Regista a primeira no +.") }
                grouped.forEach { (ym, list) ->
                    item(key = ym.toString()) { SectionHeader("${fmtMonth(ym)} · ${euro(list.sumOf { it.amount })}") }
                    items(list, key = { it.id }) { t ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.store, style = Type.body, color = c.ink)
                                Text(fmtDay(LocalDate.ofEpochDay(t.epochDay)), style = Type.tiny, color = c.ink3)
                            }
                            Text(euro(t.amount), style = Type.title, color = c.ink)
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Rounded.Delete, null, tint = c.ink3, modifier = Modifier.size(18.dp).clickable { SuperOps.deleteTrip(t) })
                        }
                    }
                }
            }
            else -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SHTextField(newItem, { newItem = it }, "Adicionar à lista…", Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        CircleIconButton(Icons.Rounded.Add, filled = true, size = 48.dp) {
                            newItem.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { SuperOps.addItem(it, aisle) }
                            newItem = ""
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ChipRow(listOf("Geral", "Frescos", "Talho/Peixe", "Mercearia", "Laticínios", "Bebidas", "Limpeza", "Higiene", "Congelados"), setOf(aisle)) { aisle = it }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallPill("Limpar comprados") { SuperOps.clearChecked() }
                        SmallPill("Partilhar") {
                            val text = d.shopping.filter { !it.checked }.groupBy { it.aisle }.entries.joinToString("\n\n") { (a, l) -> "$a\n" + l.joinToString("\n") { "• ${it.name}" } }
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Lista de compras\n\n$text"), "Partilhar lista"))
                        }
                    }
                }
                val byAisle = d.shopping.sortedBy { it.checked }.groupBy { it.aisle }
                if (byAisle.isEmpty()) item { EmptyState("Lista vazia", "Separa vários itens por vírgulas.") }
                byAisle.forEach { (a, list) ->
                    item(key = "aisle-$a") {
                        SectionHeader(a)
                        InsetBox(padding = 0.dp) {
                            list.forEachIndexed { i, it2 ->
                                if (i > 0) LineDivider()
                                Row(Modifier.fillMaxWidth().clickable { SuperOps.toggleItem(it2.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SHCheck(it2.checked) { SuperOps.toggleItem(it2.id) }
                                    Spacer(Modifier.width(12.dp))
                                    Text(it2.name, style = Type.body, color = if (it2.checked) c.ink3 else c.ink, textDecoration = if (it2.checked) TextDecoration.LineThrough else null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (addTrip) TripSheet { addTrip = false }
}

@Composable
private fun TripSheet(onDismiss: () -> Unit) {
    val d = Store.data
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    var store by remember { mutableStateOf(d.superTrips.maxByOrNull { it.epochDay }?.store ?: d.stores.first()) }
    var amount by remember { mutableStateOf("") }
    var alsoExpense by remember { mutableStateOf(true) }
    Sheet("Nova ida", onDismiss) {
        SHTextField(amount, { amount = it }, "Valor gasto (€)", Modifier.fillMaxWidth(), style = Type.h2, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Spacer(Modifier.height(10.dp))
        ChipRow(d.stores, setOf(store)) { store = it }
        Spacer(Modifier.height(10.dp))
        InsetBox(padding = 0.dp) { DetailRow(Icons.Rounded.CalendarMonth, "Data", fmtDay(date), onClick = { pickDate(ctx, date) { date = it } }) }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lançar também em Finanças (deduz ao plafond)", style = Type.small, color = SH.c.ink, modifier = Modifier.weight(1f))
            SHSwitch(alsoExpense) { alsoExpense = it }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Guardar ida", Modifier.fillMaxWidth(), enabled = parseMoney(amount) != null) {
            SuperOps.addTrip(SuperTrip(epochDay = date.toEpochDay(), store = store, amount = parseMoney(amount) ?: 0.0), alsoExpense)
            onDismiss()
        }
    }
}
