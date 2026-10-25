package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.HomeAdmin
import com.carlos.superhuman.data.HomeDoc
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.relativeDay
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.ChipRow
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.DetailRow
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHCheck
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

val docCategories = listOf("Identificação", "Casa", "Contratos", "Seguros", "Carro", "Saúde", "Finanças", "IRS", "Garantias", "Geral")
val adminKinds = listOf("Conta", "Manutenção", "Contrato", "Seguro", "Impostos")

@Composable
fun CasaScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var pendingDoc by remember { mutableStateOf<HomeDoc?>(null) }
    var adminSheet by remember { mutableStateOf<HomeAdmin?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            var name = "Documento"
            runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur ->
                    if (cur.moveToFirst()) name = cur.getString(0) ?: name
                }
            }
            pendingDoc = HomeDoc(title = name.substringBeforeLast('.'), uri = uri.toString(), mime = ctx.contentResolver.getType(uri) ?: "")
        }
    }

    ScreenScaffold(
        title = "Casa",
        onBack = { Nav.back() },
        actions = {
            CircleIconButton(Icons.Rounded.Add, filled = true) {
                if (tab == 0) picker.launch(arrayOf("*/*")) else adminSheet = HomeAdmin(title = "")
            }
        },
    ) {
        item {
            Segmented(listOf("Documentos", "Administração"), tab, Modifier.fillMaxWidth()) { tab = it }
            Spacer(Modifier.height(12.dp))
        }
        if (tab == 0) {
            val expiring = d.homeDocs.filter { it.expiryDay != null && it.expiryDay - todayEpoch() in 0..60 }
            if (expiring.isNotEmpty()) item {
                SHCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("A expirar em breve", style = Type.title, color = c.ink)
                    expiring.forEach { Text("• ${it.title} — ${relativeDay(it.expiryDay ?: 0)}", style = Type.small, color = c.warn) }
                }
            }
            val grouped = d.homeDocs.sortedBy { it.title.lowercase() }.groupBy { it.category }
            if (grouped.isEmpty()) item { EmptyState("Sem documentos", "Guarda CC, contratos, seguros, garantias… (PDF ou fotos).") }
            grouped.toSortedMap().forEach { (cat, list) ->
                item(key = "cat-$cat") {
                    SectionHeader(cat)
                    InsetBox(padding = 0.dp) {
                        list.forEachIndexed { i, doc ->
                            if (i > 0) LineDivider()
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(doc.uri), doc.mime.ifBlank { "*/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                    }.onFailure { Toast.makeText(ctx, "Não foi possível abrir", Toast.LENGTH_SHORT).show() }
                                }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(if (doc.mime.contains("pdf")) Icons.Rounded.PictureAsPdf else Icons.Rounded.Description, null, tint = c.ink, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(doc.title, style = Type.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(doc.expiryDay?.let { "Expira " + fmtDay(LocalDate.ofEpochDay(it)) } ?: "Adicionado " + fmtDay(java.time.Instant.ofEpochMilli(doc.addedAt).atZone(com.carlos.superhuman.data.zone).toLocalDate()), style = Type.tiny, color = c.ink3)
                                }
                                Icon(Icons.Rounded.Delete, null, tint = c.ink3, modifier = Modifier.size(18.dp).clickable {
                                    Store.update { s -> s.copy(homeDocs = s.homeDocs.filter { it.id != doc.id }) }
                                })
                            }
                        }
                    }
                }
            }
        } else {
            val list = d.homeAdmin.sortedWith(compareBy<HomeAdmin>({ it.done }, { it.dueDay ?: Long.MAX_VALUE }))
            val monthTotal = d.homeAdmin.filter { !it.done && it.amount != null && (it.dueDay ?: Long.MAX_VALUE) - todayEpoch() in 0..31 }.sumOf { it.amount ?: 0.0 }
            item {
                SHCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(c.inset), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Home, null, tint = c.ink) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Próximos 30 dias", style = Type.small, color = c.ink2)
                            Text(euro(monthTotal), style = Type.h2, color = c.ink)
                        }
                        Text("${list.count { !it.done }} pendentes", style = Type.small, color = c.ink2)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (list.isEmpty()) item { EmptyState("Nada pendente", "Contas, manutenções, contratos, seguros, IMI…") }
            items(list, key = { it.id }) { a ->
                SHCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { adminSheet = a }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.title, style = Type.title, color = if (a.done) c.ink3 else c.ink)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag(a.kind, c.ink2)
                                val due = a.dueDay
                                if (due != null) Tag(relativeDay(due), if (!a.done && due < todayEpoch()) c.bad else if (!a.done && due - todayEpoch() <= 7) c.warn else c.good)
                                if (a.repeatMonths > 0) Tag("a cada ${a.repeatMonths} m", c.ink3)
                            }
                        }
                        if (a.amount != null) { Text(euro(a.amount), style = Type.title, color = c.ink); Spacer(Modifier.width(12.dp)) }
                        SHCheck(a.done) { AdminOps.complete(a) }
                    }
                }
            }
        }
    }

    pendingDoc?.let { DocSheet(it) { pendingDoc = null } }
    adminSheet?.let { AdminSheet(it) { adminSheet = null } }
}

object AdminOps {
    /** Marca como feito; se repetir, cria a próxima ocorrência. */
    fun complete(a: HomeAdmin) = Store.update { d ->
        val updated = d.homeAdmin.map { if (it.id == a.id) it.copy(done = !it.done) else it }
        val next = if (!a.done && a.repeatMonths > 0 && a.dueDay != null)
            listOf(a.copy(id = com.carlos.superhuman.data.newId(), done = false, dueDay = LocalDate.ofEpochDay(a.dueDay).plusMonths(a.repeatMonths.toLong()).toEpochDay()))
        else emptyList()
        d.copy(homeAdmin = updated + next)
    }
}

@Composable
private fun DocSheet(initial: HomeDoc, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial.title) }
    var cat by remember { mutableStateOf(initial.category) }
    var expiry by remember { mutableStateOf(initial.expiryDay) }
    Sheet("Guardar documento", onDismiss) {
        SHTextField(title, { title = it }, "Nome", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        ChipRow(docCategories, setOf(cat)) { cat = it }
        Spacer(Modifier.height(10.dp))
        InsetBox(padding = 0.dp) {
            DetailRow(Icons.Rounded.CalendarMonth, "Validade (opcional)", expiry?.let { fmtDay(LocalDate.ofEpochDay(it)) } ?: "—", onClick = {
                pickDate(ctx) { expiry = it.toEpochDay() }
            })
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Guardar", Modifier.fillMaxWidth(), enabled = title.isNotBlank()) {
            Store.update { d -> d.copy(homeDocs = d.homeDocs + initial.copy(title = title.trim(), category = cat, expiryDay = expiry)) }
            onDismiss()
        }
    }
}

@Composable
private fun AdminSheet(initial: HomeAdmin, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val exists = Store.data.homeAdmin.any { it.id == initial.id }
    var title by remember { mutableStateOf(initial.title) }
    var kind by remember { mutableStateOf(initial.kind) }
    var amount by remember { mutableStateOf(initial.amount?.let { "%.2f".format(it) } ?: "") }
    var due by remember { mutableStateOf(initial.dueDay) }
    var repeat by remember { mutableIntStateOf(initial.repeatMonths) }
    var notes by remember { mutableStateOf(initial.notes) }
    Sheet(if (exists) "Editar" else "Nova tarefa da casa", onDismiss) {
        SHTextField(title, { title = it }, "Ex.: Revisão da caldeira, IMI, Seguro casa", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        ChipRow(adminKinds, setOf(kind)) { kind = it }
        Spacer(Modifier.height(10.dp))
        SHTextField(amount, { amount = it }, "Valor € (opcional)", Modifier.fillMaxWidth(), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Spacer(Modifier.height(10.dp))
        InsetBox(padding = 0.dp) {
            DetailRow(Icons.Rounded.CalendarMonth, "Data limite", due?.let { fmtDay(LocalDate.ofEpochDay(it)) } ?: "—", onClick = { pickDate(ctx) { due = it.toEpochDay() } })
        }
        Spacer(Modifier.height(10.dp))
        Text("Repetir", style = Type.small, color = SH.c.ink2)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "Não", 1 to "Mensal", 3 to "Trimestral", 6 to "Semestral", 12 to "Anual").forEach { (m, l) -> SmallPill(l, repeat == m) { repeat = m } }
        }
        Spacer(Modifier.height(10.dp))
        SHTextField(notes, { notes = it }, "Notas", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exists) SecondaryButton("Apagar") { Store.update { d -> d.copy(homeAdmin = d.homeAdmin.filter { it.id != initial.id }) }; onDismiss() }
            PrimaryButton("Guardar", Modifier.weight(1f), enabled = title.isNotBlank()) {
                val a = initial.copy(title = title.trim(), kind = kind, amount = parseMoney(amount), dueDay = due, repeatMonths = repeat, notes = notes)
                Store.update { d -> d.copy(homeAdmin = if (exists) d.homeAdmin.map { if (it.id == a.id) a else it } else d.homeAdmin + a) }
                onDismiss()
            }
        }
    }
}
