package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Expense
import com.carlos.superhuman.data.ExpenseSource
import com.carlos.superhuman.data.FixedExpense
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.expensesIn
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.fmtMonth
import com.carlos.superhuman.data.fmtTime
import com.carlos.superhuman.data.lastMonthsSpend
import com.carlos.superhuman.data.monthKey
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.startMillis
import com.carlos.superhuman.data.toLocalDate
import com.carlos.superhuman.service.WalletListener
import com.carlos.superhuman.ui.components.CategoryBars
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.DetailRow
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.HatchBarChart
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineChart
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.Ring
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.components.Tag
import com.carlos.superhuman.ui.components.parseMoney
import com.carlos.superhuman.ui.components.pickDate
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object FinanceOps {
    fun addExpense(e: Expense) = Store.update { it.copy(expenses = it.expenses + e) }
    fun upsertExpense(e: Expense) = Store.update { d ->
        if (d.expenses.any { it.id == e.id }) d.copy(expenses = d.expenses.map { if (it.id == e.id) e else it }) else d.copy(expenses = d.expenses + e)
    }
    fun deleteExpense(id: String) = Store.update { d -> d.copy(expenses = d.expenses.filter { it.id != id }) }
    fun confirm(id: String, category: String? = null) = Store.update { d ->
        d.copy(expenses = d.expenses.map { if (it.id == id) it.copy(pending = false, category = category ?: it.category) else it })
    }
    fun setBudget(ym: YearMonth, amount: Double) = Store.update { it.copy(budgets = it.budgets + (monthKey(ym) to amount)) }
    fun upsertFixed(f: FixedExpense) = Store.update { d ->
        if (d.fixedExpenses.any { it.id == f.id }) d.copy(fixedExpenses = d.fixedExpenses.map { if (it.id == f.id) f else it }) else d.copy(fixedExpenses = d.fixedExpenses + f)
    }
    fun deleteFixed(id: String) = Store.update { d -> d.copy(fixedExpenses = d.fixedExpenses.filter { it.id != id }) }
}

@Composable
fun FinanceScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var budgetSheet by remember { mutableStateOf(false) }
    var expenseSheet by remember { mutableStateOf<Expense?>(null) }
    var fixedSheet by remember { mutableStateOf<FixedExpense?>(null) }
    val m = d.monthSummary(ym)
    val pending = d.expenses.filter { it.pending }.sortedByDescending { it.time }
    val monthList = d.expensesIn(ym).filter { !it.pending }.sortedByDescending { it.time }

    ScreenScaffold(
        title = "Finanças",
        onBack = null,
        actions = { CircleIconButton(Icons.Rounded.Add, filled = true) { expenseSheet = Expense(amount = 0.0, merchant = "") } },
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallPill("‹") { ym = ym.minusMonths(1) }
                Text(fmtMonth(ym), style = Type.title, color = c.ink, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                SmallPill("›") { ym = ym.plusMonths(1) }
            }
            Spacer(Modifier.height(12.dp))
            // Cartão principal
            SHCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (m.budget > 0) "Restante do plafond" else "Sem plafond definido", style = Type.small, color = c.ink2)
                        Text(if (m.budget > 0) euro(m.remaining) else "—", style = Type.number, color = if (m.remaining < 0) c.bad else c.ink)
                        Spacer(Modifier.height(4.dp))
                        if (m.budget > 0) {
                            val daysLeft = (ym.lengthOfMonth() - (if (ym == YearMonth.now()) LocalDate.now().dayOfMonth else 0)).coerceAtLeast(1)
                            Text("≈ ${euro((m.remaining / daysLeft).coerceAtLeast(0.0))} por dia até ao fim do mês", style = Type.small, color = c.ink2)
                        }
                    }
                    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                        val used = if (m.budget > 0) ((m.spent + m.fixed) / m.budget).toFloat() else 0f
                        Ring(used, Modifier.size(84.dp), color = if (used > 1f) c.bad else if (used > 0.8f) c.warn else c.ink)
                        Text("${(used * 100).toInt()}%", style = Type.title, color = c.ink)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Breakdown("Plafond", euro(m.budget, false), Modifier.weight(1f))
                    Breakdown("Fixas", euro(m.fixed, false), Modifier.weight(1f))
                    Breakdown("Gasto", euro(m.spent, false), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(if (m.budget > 0) "Alterar plafond" else "Definir plafond", Modifier.fillMaxWidth(), icon = Icons.Rounded.Savings) { budgetSheet = true }
            }
        }

        // Wallet
        item {
            if (!WalletListener.isEnabled(ctx)) {
                Spacer(Modifier.height(10.dp))
                SHCard(Modifier.fillMaxWidth(), onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Payments, null, tint = c.accent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ligar Google Wallet", style = Type.title, color = c.ink)
                            Text("Ativa o acesso a notificações do Superhuman para captar os teus pagamentos.", style = Type.small, color = c.ink2)
                        }
                    }
                }
            }
        }
        if (pending.isNotEmpty()) {
            item { SectionHeader("Por confirmar · ${pending.size}") }
            items(pending, key = { it.id }) { e -> PendingRow(e, d.categories) }
        }

        // Gráficos
        item {
            SectionHeader("Ritmo do mês")
            SHCard(Modifier.fillMaxWidth()) {
                val ideal = if (m.budget > 0) (1..m.daysInMonth).map { (m.budget - m.fixed) * it / m.daysInMonth } else null
                val upTo = if (ym == YearMonth.now()) LocalDate.now().dayOfMonth else m.daysInMonth
                Text("Gasto acumulado vs ritmo ideal", style = Type.small, color = c.ink2)
                Spacer(Modifier.height(10.dp))
                LineChart(m.dailyCumulative.take(upTo.coerceAtLeast(2)), reference = ideal, color = c.accent)
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("Dia 1", style = Type.tiny, color = c.ink3, modifier = Modifier.weight(1f))
                    Text("Dia ${m.daysInMonth}", style = Type.tiny, color = c.ink3)
                }
            }
            Spacer(Modifier.height(10.dp))
            SHCard(Modifier.fillMaxWidth()) {
                Text("Por categoria", style = Type.title, color = c.ink)
                Spacer(Modifier.height(12.dp))
                if (m.byCategory.isEmpty()) Text("Sem despesas confirmadas este mês.", style = Type.small, color = c.ink2)
                else CategoryBars(m.byCategory, m.spent)
            }
            Spacer(Modifier.height(10.dp))
            val months = d.lastMonthsSpend(6)
            SHCard(Modifier.fillMaxWidth()) {
                Text("Últimos 6 meses", style = Type.title, color = c.ink)
                Spacer(Modifier.height(12.dp))
                HatchBarChart(
                    values = months.map { it.second },
                    highlight = months.lastIndex,
                    topLabels = months.map { euro(it.second, false).replace(" €", "") },
                    labels = months.map { DateTimeFormatter.ofPattern("MMM", com.carlos.superhuman.data.PT).format(it.first) },
                )
            }
        }

        // Fixas
        item { SectionHeader("Despesas fixas", "+ Adicionar") { fixedSheet = FixedExpense(name = "", amount = 0.0) } }
        if (d.fixedExpenses.isEmpty()) item { EmptyState("Sem despesas fixas", "Renda, luz, internet, ginásio…") }
        item {
            if (d.fixedExpenses.isNotEmpty()) InsetBox(padding = 0.dp) {
                d.fixedExpenses.sortedBy { it.dayOfMonth }.forEachIndexed { i, f ->
                    if (i > 0) LineDivider()
                    Row(Modifier.fillMaxWidth().clickable { fixedSheet = f }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = Type.body, color = if (f.active) c.ink else c.ink3)
                            Text("Dia ${f.dayOfMonth} · ${f.category}", style = Type.tiny, color = c.ink3)
                        }
                        Text(euro(f.amount), style = Type.body, color = c.ink)
                    }
                }
            }
        }

        // Movimentos
        item { SectionHeader("Movimentos · ${monthList.size}") }
        if (monthList.isEmpty()) item { EmptyState("Sem movimentos", "Adiciona no + ou liga o Google Wallet.") }
        items(monthList, key = { it.id }) { e -> ExpenseRow(e) { expenseSheet = e } }
    }

    if (budgetSheet) BudgetSheet(ym) { budgetSheet = false }
    expenseSheet?.let { ExpenseSheet(it) { expenseSheet = null } }
    fixedSheet?.let { FixedSheet(it) { fixedSheet = null } }
}

@Composable
private fun Breakdown(label: String, value: String, modifier: Modifier) {
    InsetBox(modifier, padding = 10.dp) {
        Text(label, style = Type.tiny, color = SH.c.ink2)
        Text(value, style = Type.title, color = SH.c.ink, maxLines = 1)
    }
}

@Composable
fun ExpenseRow(e: Expense, onClick: () -> Unit) {
    val c = SH.c
    Row(Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.card), contentAlignment = Alignment.Center) {
            Icon(if (e.source == ExpenseSource.WALLET) Icons.Rounded.Payments else if (e.source == ExpenseSource.SUPERMERCADO) Icons.Rounded.Store else Icons.Rounded.Receipt, null, tint = c.ink, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.merchant.ifBlank { "Despesa" }, style = Type.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${fmtDay(e.time.toLocalDate())} · ${fmtTime(e.time)} · ${e.category}", style = Type.tiny, color = c.ink3)
        }
        Text("−" + euro(e.amount), style = Type.title, color = c.ink)
    }
}

@Composable
private fun PendingRow(e: Expense, categories: List<String>) {
    val c = SH.c
    var cat by remember(e.id) { mutableStateOf(e.category) }
    SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.merchant, style = Type.title, color = c.ink)
                Text("${fmtDay(e.time.toLocalDate())} · ${fmtTime(e.time)} · Google Wallet", style = Type.tiny, color = c.ink3)
            }
            Text(euro(e.amount), style = Type.h2, color = c.ink)
        }
        Spacer(Modifier.height(10.dp))
        ChipRow(categories, setOf(cat)) { cat = it }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Ignorar", Modifier.weight(1f), icon = Icons.Rounded.Close) { FinanceOps.deleteExpense(e.id) }
            PrimaryButton("Confirmar", Modifier.weight(1f), icon = Icons.Rounded.Check) { FinanceOps.confirm(e.id, cat) }
        }
    }
}

@Composable
fun BudgetSheet(current: YearMonth, onDismiss: () -> Unit) {
    val d = Store.data
    val c = SH.c
    val options = listOf(YearMonth.now(), YearMonth.now().plusMonths(1))
    var sel by remember { mutableStateOf(if (current in options) options.indexOf(current) else 0) }
    var amount by remember(sel) { mutableStateOf(d.budgets[monthKey(options[sel])]?.let { "%.2f".format(it) } ?: "") }
    Sheet("Plafond do mês", onDismiss) {
        Segmented(options.map { fmtMonth(it) }, sel, Modifier.fillMaxWidth()) { sel = it }
        Spacer(Modifier.height(8.dp))
        Text(
            if (sel == 1) "Estás a definir o plafond do próximo mês antes de começar." else "Plafond total do mês (inclui as despesas fixas).",
            style = Type.small, color = c.ink2,
        )
        Spacer(Modifier.height(12.dp))
        SHTextField(amount, { amount = it }, "Ex.: 1500", Modifier.fillMaxWidth(), style = Type.h2, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Spacer(Modifier.height(8.dp))
        val fixed = d.fixedExpenses.filter { it.active }.sumOf { it.amount }
        Text("Fixas ativas: ${euro(fixed)} · Disponível para o dia a dia: ${euro((parseMoney(amount) ?: 0.0) - fixed)}", style = Type.small, color = c.ink2)
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Guardar plafond", Modifier.fillMaxWidth(), enabled = parseMoney(amount) != null) {
            FinanceOps.setBudget(options[sel], parseMoney(amount) ?: 0.0); onDismiss()
        }
    }
}

@Composable
fun ExpenseSheet(initial: Expense, onDismiss: () -> Unit) {
    val d = Store.data
    val ctx = LocalContext.current
    val isNew = d.expenses.none { it.id == initial.id }
    var amount by remember { mutableStateOf(if (initial.amount > 0) "%.2f".format(initial.amount) else "") }
    var merchant by remember { mutableStateOf(initial.merchant) }
    var cat by remember { mutableStateOf(initial.category) }
    var date by remember { mutableStateOf(initial.time.toLocalDate()) }
    var note by remember { mutableStateOf(initial.note) }
    Sheet(if (isNew) "Nova despesa" else "Editar despesa", onDismiss) {
        SHTextField(amount, { amount = it }, "Valor (€)", Modifier.fillMaxWidth(), style = Type.h2, keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Spacer(Modifier.height(10.dp))
        SHTextField(merchant, { merchant = it }, "Onde? (ex.: Continente)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        ChipRow(d.categories, setOf(cat)) { cat = it }
        Spacer(Modifier.height(10.dp))
        InsetBox(padding = 0.dp) {
            DetailRow(Icons.Rounded.CalendarMonth, "Data", fmtDay(date), onClick = { pickDate(ctx, date) { date = it } })
        }
        Spacer(Modifier.height(10.dp))
        SHTextField(note, { note = it }, "Nota (opcional)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isNew) SecondaryButton("Apagar", icon = Icons.Rounded.Delete) { FinanceOps.deleteExpense(initial.id); onDismiss() }
            PrimaryButton(if (isNew) "Adicionar" else "Guardar", Modifier.weight(1f), enabled = parseMoney(amount) != null) {
                val keepTime = initial.time.toLocalDate() == date
                val time = if (keepTime) initial.time else date.startMillis() + LocalTime.now().toSecondOfDay() * 1000L
                FinanceOps.upsertExpense(initial.copy(amount = parseMoney(amount) ?: 0.0, merchant = merchant.trim(), category = cat, time = time, note = note, pending = false))
                onDismiss()
            }
        }
    }
}

@Composable
fun FixedSheet(initial: FixedExpense, onDismiss: () -> Unit) {
    val d = Store.data
    val isNew = d.fixedExpenses.none { it.id == initial.id }
    var name by remember { mutableStateOf(initial.name) }
    var amount by remember { mutableStateOf(if (initial.amount > 0) "%.2f".format(initial.amount) else "") }
    var day by remember { mutableStateOf(initial.dayOfMonth.toString()) }
    var cat by remember { mutableStateOf(initial.category) }
    var active by remember { mutableStateOf(initial.active) }
    Sheet(if (isNew) "Nova despesa fixa" else "Despesa fixa", onDismiss) {
        SHTextField(name, { name = it }, "Nome (ex.: Renda)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SHTextField(amount, { amount = it }, "Valor €", Modifier.weight(1f), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            SHTextField(day, { day = it.filter { ch -> ch.isDigit() }.take(2) }, "Dia", Modifier.width(90.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Spacer(Modifier.height(10.dp))
        ChipRow(d.categories, setOf(cat)) { cat = it }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ativa", style = Type.body, color = SH.c.ink, modifier = Modifier.weight(1f))
            com.carlos.superhuman.ui.components.SHSwitch(active) { active = it }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isNew) SecondaryButton("Apagar", icon = Icons.Rounded.Delete) { FinanceOps.deleteFixed(initial.id); onDismiss() }
            PrimaryButton("Guardar", Modifier.weight(1f), enabled = name.isNotBlank() && parseMoney(amount) != null) {
                FinanceOps.upsertFixed(initial.copy(name = name.trim(), amount = parseMoney(amount) ?: 0.0, dayOfMonth = (day.toIntOrNull() ?: 1).coerceIn(1, 31), category = cat, active = active))
                onDismiss()
            }
        }
    }
}
