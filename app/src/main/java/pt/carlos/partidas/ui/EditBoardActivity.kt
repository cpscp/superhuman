package pt.carlos.partidas.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import pt.carlos.partidas.data.BoardStore
import pt.carlos.partidas.data.CacheEntry
import pt.carlos.partidas.data.DepartureCache
import pt.carlos.partidas.data.Repo
import pt.carlos.partidas.data.StopsIndex
import pt.carlos.partidas.data.TrainApi
import pt.carlos.partidas.model.Board
import pt.carlos.partidas.model.Mode
import pt.carlos.partidas.model.Place
import pt.carlos.partidas.model.StopRef
import pt.carlos.partidas.widget.WidgetRenderer

/** Criar/editar um quadro. Devolve EXTRA_BOARD com o id guardado. */
class EditBoardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val existing = intent.getStringExtra(MainActivity.EXTRA_BOARD)?.let { BoardStore.get(this, it) }
        enableEdgeToEdge()
        setContent {
            PartidasTheme {
                EditScreen(existing, onSave = { b ->
                    BoardStore.upsert(this, b)
                    WidgetRenderer.renderBoard(this, b.id)
                    setResult(Activity.RESULT_OK, Intent().putExtra(MainActivity.EXTRA_BOARD, b.id))
                    finish()
                }, onCancel = { finish() })
            }
        }
    }
}

private val MODES = listOf(
    Mode.TRAIN to "Comboio · CP",
    Mode.BUS to "Autocarro · Carris",
    Mode.BUS_CM to "Carris Metropolitana"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreen(initial: Board?, onSave: (Board) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(initial?.mode ?: Mode.TRAIN) }
    // Comboio: 1 estação. Autocarro: uma ou mais paragens.
    var stops by remember { mutableStateOf(initial?.stops ?: emptyList()) }
    var direction by remember { mutableStateOf(initial?.direction ?: "") }
    var filter by remember { mutableStateOf(initial?.filter ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var adding by remember { mutableStateOf(initial == null) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CacheEntry?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val newId = remember { java.util.UUID.randomUUID().toString() }
    val draft = Board(
        id = initial?.id ?: newId,
        mode = mode,
        stopId = stops.firstOrNull()?.id ?: "",
        stopName = stops.firstOrNull()?.name ?: "",
        filter = filter.trim(), title = title.trim(),
        extraStops = stops.drop(1),
        direction = direction.trim()
    )
    val stopKey = stops.joinToString(",") { it.id + it.name }
    val isBus = mode != Mode.TRAIN
    val canAdd = isBus || stops.isEmpty()

    // Pesquisa (CP: estações do cp.pt; Carris Metropolitana: índice local de paragens).
    // Carris (Lisboa) não tem pesquisa pública: escreve-se o número da paragem.
    LaunchedEffect(query, mode) {
        results = emptyList()
        status = null
        val q = query.trim()
        if (mode == Mode.BUS || q.length < 2) return@LaunchedEffect
        delay(350)
        status = "A procurar…"
        try {
            val found = withContext(Dispatchers.IO) {
                when (mode) {
                    Mode.TRAIN -> TrainApi.search(q)
                    else -> {
                        StopsIndex.ensure(ctx) { p -> status = p }
                        val list = StopsIndex.search(ctx, q)
                        if (q.all { it.isDigit() } && q.length >= 3 && list.none { it.id == q })
                            listOf(Place(q, "Paragem $q", "Usar este número de paragem")) + list
                        else list
                    }
                }
            }
            results = found
            status = if (found.isEmpty()) "Sem resultados" else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = "Erro: ${Repo.friendly(e)}"
        }
    }

    // Pré-visualização ao vivo, já com o aspeto do quadro
    LaunchedEffect(stopKey, mode, filter, direction) {
        preview = null
        if (stops.isEmpty()) return@LaunchedEffect
        delay(500)
        val t = System.currentTimeMillis()
        preview = try {
            CacheEntry(t, withContext(Dispatchers.IO) { Repo.fetch(ctx, draft, t) }, null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CacheEntry(t, emptyList(), Repo.friendly(e))
        }
        now = System.currentTimeMillis()
    }

    fun addStop(id: String, name: String) {
        val clean = id.trim()
        if ((clean.isEmpty() && name.isBlank()) || stops.any { it.id == clean && it.name == name }) return
        stops = stops + StopRef(clean, name)
        query = ""
        adding = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Novo quadro" else "Editar quadro") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancelar") } }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODES.forEach { (m, label) ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { if (mode != m) { mode = m; stops = emptyList(); adding = true; query = "" } },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }

            Text(
                when (mode) {
                    Mode.TRAIN -> "Escolhe a estação e o sentido. Como no Glass TV, \"Cascais\" inclui todos os comboios para oeste (Oeiras, Caxias…)."
                    Mode.BUS -> "Junta as paragens da Carris onde apanhas o autocarro (o número está na paragem ou em paragem.alx.carris.pt). " +
                        "O quadro mostra todos os autocarros que chegam a essas paragens."
                    Mode.BUS_CM -> "Junta as paragens da Carris Metropolitana. O quadro mostra todos os autocarros que passam nelas."
                },
                style = MaterialTheme.typography.bodySmall
            )

            stops.forEach { st ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (mode == Mode.TRAIN) st.name.ifBlank { st.id } else st.label, style = MaterialTheme.typography.titleMedium)
                            Text(if (mode == Mode.TRAIN) "Estação CP" else "Paragem ${st.id}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            stops = stops.filterNot { it == st }
                            if (stops.isEmpty()) adding = true
                        }) { Text(if (mode == Mode.TRAIN) "Alterar" else "Remover") }
                    }
                }
            }

            if (canAdd && (adding || stops.isEmpty())) {
                if (mode == Mode.BUS) {
                    // Carris (Lisboa): número da paragem
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query, onValueChange = { v -> query = v.filter { it.isDigit() } },
                            modifier = Modifier.weight(1f), singleLine = true,
                            label = { Text("Nº da paragem (ex.: 10210)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (query.length >= 3) addStop(query, "") })
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { addStop(query, "") }, enabled = query.length >= 3) { Text("Adicionar") }
                    }
                } else {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (mode == Mode.TRAIN) "Estação (ex.: Alcântara-Mar)" else "Paragem: nome ou número") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                    status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Column {
                        results.filterNot { r -> stops.any { it.id == r.id } }.forEach { p ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { addStop(p.id, if (p.name.startsWith("Paragem ")) "" else p.name) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                if (p.sub.isNotBlank()) Text(p.sub, style = MaterialTheme.typography.bodySmall)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            } else if (isBus) {
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Adicionar outra paragem")
                }
            }

            if (mode == Mode.TRAIN) {
                OutlinedTextField(
                    value = direction, onValueChange = { direction = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Sentido") },
                    supportingText = { Text("Destino ou estação do caminho, ex.: Cascais · vazio = todos") }
                )
            } else {
                OutlinedTextField(
                    value = filter, onValueChange = { filter = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Filtro de carreiras (opcional)") },
                    supportingText = { Text("Carreiras ou destinos separados por vírgula, ex.: 714, 732") }
                )
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Nome no quadro (opcional)") },
                supportingText = {
                    Text(if (mode == Mode.TRAIN) "Por omissão usa o nome da estação" else "Ex.: Alcântara · por omissão \"N paragens\"")
                }
            )

            if (stops.isNotEmpty()) {
                Text("Pré-visualização", style = MaterialTheme.typography.titleSmall)
                DepartureBoard(
                    board = draft, cache = preview, now = now, refreshing = preview == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    // A pré-visualização serve logo de cache para a app e o widget
                    preview?.let { if (it.error == null) DepartureCache.saveOk(ctx, draft.id, it.fetchedAt, it.items) }
                    onSave(draft)
                },
                enabled = stops.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar quadro") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
