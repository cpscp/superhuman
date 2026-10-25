package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Note
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.toLocalDate
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.BareTextField
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.EmptyState
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type

object NoteOps {
    fun upsert(n: Note) = Store.update { d ->
        if (d.notes.any { it.id == n.id }) d.copy(notes = d.notes.map { if (it.id == n.id) n else it }) else d.copy(notes = d.notes + n)
    }
    fun delete(id: String) = Store.update { d -> d.copy(notes = d.notes.filter { it.id != id }) }
}

@Composable
fun NotesScreen() {
    val d by Store.state.collectAsState()
    val c = SH.c
    var q by remember { mutableStateOf("") }
    val list = d.notes
        .filter { q.isBlank() || it.title.contains(q, true) || it.body.contains(q, true) }
        .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })

    ScreenScaffold(
        title = "Notas",
        onBack = { Nav.back() },
        actions = { CircleIconButton(Icons.Rounded.Add, filled = true) { Nav.go("note", null) } },
    ) {
        item {
            SHTextField(q, { q = it }, "Pesquisar notas", Modifier.fillMaxWidth(), leading = Icons.Rounded.Search)
            Spacer(Modifier.height(12.dp))
        }
        if (list.isEmpty()) item { EmptyState("Sem notas", "Ideias, reuniões, listas — tudo aqui.") }
        list.chunked(2).forEach { row ->
            item(key = row.first().id) {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { n ->
                        SHCard(Modifier.weight(1f).height(150.dp), onClick = { Nav.go("note", n.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(n.title.ifBlank { "Sem título" }, style = Type.title, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (n.pinned) Icon(Icons.Rounded.PushPin, null, tint = c.accent, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(n.body, style = Type.small, color = c.ink2, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(fmtDay(n.updatedAt.toLocalDate()), style = Type.tiny, color = c.ink3)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun NoteEditScreen(id: String?) {
    val existing = remember(id) { Store.data.notes.firstOrNull { it.id == id } }
    val base = remember(id) { existing ?: Note() }
    var title by remember { mutableStateOf(base.title) }
    var body by remember { mutableStateOf(base.body) }
    var pinned by remember { mutableStateOf(base.pinned) }
    var deleted by remember { mutableStateOf(false) }

    fun save() {
        if (deleted) return
        if (title.isBlank() && body.isBlank()) return
        if (title == base.title && body == base.body && pinned == base.pinned && existing != null) return
        NoteOps.upsert(base.copy(title = title, body = body, pinned = pinned, updatedAt = System.currentTimeMillis()))
    }
    DisposableEffect(id) { onDispose { save() } }

    ScreenScaffold(
        title = "",
        onBack = { Nav.back() },
        actions = {
            CircleIconButton(Icons.Rounded.PushPin, filled = pinned) { pinned = !pinned }
            CircleIconButton(Icons.Rounded.Delete) { deleted = true; NoteOps.delete(base.id); Nav.back() }
        },
    ) {
        item {
            BareTextField(title, { title = it }, "Título", Type.h1, Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true)
            BareTextField(body, { body = it }, "Escreve aqui…", Type.body, Modifier.fillMaxWidth().padding(bottom = 300.dp))
        }
    }
}
