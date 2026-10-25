package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtTime
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type

private data class Hit(val section: String, val title: String, val sub: String, val route: String, val arg: String?)

@Composable
fun SearchScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    var q by remember { mutableStateOf("") }
    val hits = remember(q, d) {
        if (q.length < 2) emptyList() else buildList {
            d.tasks.filter { it.title.contains(q, true) || it.notes.contains(q, true) || it.tags.any { t -> t.contains(q, true) } }
                .forEach { add(Hit("Tasks", it.title, if (it.done) "Concluída" else "Por fazer", "task", it.id)) }
            d.cachedEvents.filter { it.title.contains(q, true) }.forEach { add(Hit("Eventos de hoje", it.title, "${fmtTime(it.begin)} · ${it.calendarLabel}", "schedule", null)) }
            d.notes.filter { it.title.contains(q, true) || it.body.contains(q, true) }.forEach { add(Hit("Notas", it.title.ifBlank { "Sem título" }, it.body.take(60), "note", it.id)) }
            d.goals.filter { it.title.contains(q, true) }.forEach { add(Hit("Objetivos", it.title, it.area, "goal", it.id)) }
            d.homeDocs.filter { it.title.contains(q, true) || it.category.contains(q, true) }.forEach { add(Hit("Documentos", it.title, it.category, "casa", null)) }
            d.expenses.filter { it.merchant.contains(q, true) }.take(15).forEach { add(Hit("Despesas", it.merchant, euro(it.amount), "finance", null)) }
            d.shopping.filter { it.name.contains(q, true) }.forEach { add(Hit("Lista de compras", it.name, it.aisle, "super", null)) }
        }
    }
    ScreenScaffold(title = "Pesquisa", onBack = { Nav.back() }) {
        item {
            SHTextField(q, { q = it }, "Pesquisar em tudo…", Modifier.fillMaxWidth(), leading = Icons.Rounded.Search)
            Spacer(Modifier.height(10.dp))
            if (q.isNotBlank()) SmallPill("Perguntar à AI: \"$q\"") { Nav.go("assistant", q) }
        }
        if (q.length >= 2 && hits.isEmpty()) item { EmptyState("Sem resultados", "Experimenta perguntar ao assistente.") }
        hits.groupBy { it.section }.forEach { (section, list) ->
            item(key = "s-$section") { SectionHeader(section) }
            items(list) { h ->
                Column(Modifier.fillMaxWidth().clickable { Nav.go(h.route, h.arg) }.padding(vertical = 10.dp)) {
                    Text(h.title, style = Type.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(h.sub, style = Type.tiny, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
