package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.ChatMsg
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.net.Agent
import com.carlos.superhuman.net.Claude
import com.carlos.superhuman.net.LocalAssistant
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(initial: String?) {
    val d by Store.state.collectAsState()
    val c = SH.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var lastUnhandled by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    fun send(text: String) {
        val t = text.trim()
        if (t.isBlank() || busy) return
        if (!Claude.hasKey()) {
            val r = LocalAssistant.handle(ctx, t)
            lastUnhandled = if (r.handled) null else t
            Store.update { it.copy(chat = (it.chat + ChatMsg("user", t) + ChatMsg("assistant", r.text)).takeLast(80)) }
            input = ""
            return
        }
        Store.update { it.copy(chat = (it.chat + ChatMsg("user", t)).takeLast(80)) }
        input = ""
        busy = true
        scope.launch {
            val answer = runCatching { Agent.run(ctx, Store.data.chat) }.getOrElse { "Erro: ${it.message}" }
            Store.update { it.copy(chat = (it.chat + ChatMsg("assistant", answer)).takeLast(80)) }
            busy = false
        }
    }

    LaunchedEffect(initial) { if (!initial.isNullOrBlank()) send(initial) }
    LaunchedEffect(d.chat.size, busy) { if (d.chat.isNotEmpty()) listState.animateScrollToItem(d.chat.size + (if (busy) 1 else 0)) }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack) { Nav.back() }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Assistente", style = Type.h2, color = c.ink)
                Text(if (busy) "a trabalhar…" else if (Claude.hasKey()) "Claude API · app e web" else "Local · grátis e offline", style = Type.tiny, color = c.ink3)
            }
            CircleIconButton(Icons.Rounded.DeleteSweep) { Store.update { it.copy(chat = emptyList()) } }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                if (d.chat.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Em que posso ajudar?", style = Type.h1, color = c.ink)
                        Spacer(Modifier.size(6.dp))
                        Text(if (Claude.hasKey()) "Posso criar tasks, lançar despesas, marcar eventos, gerir a lista de compras, procurar notas ou pesquisar na web." else LocalAssistant.HELP, style = Type.small, color = c.ink2)
                    }
                }
            }
            items(d.chat) { m -> Bubble(m) }
            if (busy) item { Bubble(ChatMsg("assistant", "…")) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!Claude.hasKey()) SmallPill("✦ Continuar no Claude", selected = true) {
                LocalAssistant.openInClaude(ctx, lastUnhandled ?: input.ifBlank { "Ajuda-me a organizar o meu dia com base neste contexto." })
            }
            listOf(
                "Como está o meu dia?",
                "Quanto me resta do plafond?",
                "Cria task: ligar ao cliente amanhã às 10h",
                "Junta leite e ovos à lista",
                "Gastei 12,50 no almoço",
                "+1 cerveja",
                "Vou precisar de casaco?",
            ).forEach { s -> SmallPill(s) { send(s) } }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            SHTextField(input, { input = it }, "Pede algo…", Modifier.weight(1f), singleLine = false)
            Spacer(Modifier.size(8.dp))
            CircleIconButton(Icons.AutoMirrored.Rounded.Send, filled = true, size = 48.dp) { send(input) }
        }
    }
}

@Composable
private fun Bubble(m: ChatMsg) {
    val c = SH.c
    val mine = m.role == "user"
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(
            Modifier.widthIn(max = 310.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp))
                .background(if (mine) c.ink else c.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text(m.text, style = Type.body, color = if (mine) c.onInk else c.ink) }
    }
}
