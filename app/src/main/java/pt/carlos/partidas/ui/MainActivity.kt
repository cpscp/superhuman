package pt.carlos.partidas.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.carlos.partidas.data.BoardStore
import pt.carlos.partidas.data.Defaults
import pt.carlos.partidas.data.DepartureCache
import pt.carlos.partidas.data.Repo
import pt.carlos.partidas.data.WidgetLinks
import pt.carlos.partidas.model.Board
import pt.carlos.partidas.widget.DeparturesWidget
import pt.carlos.partidas.widget.Scheduler
import pt.carlos.partidas.widget.WidgetRenderer

class MainActivity : ComponentActivity() {
    private val version = mutableIntStateOf(0)
    private val openBoard = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Defaults.seedIfFirstRun(this)
        openBoard.value = intent.getStringExtra(EXTRA_BOARD)
        enableEdgeToEdge()
        setContent {
            PartidasTheme {
                AppScreen(
                    version = version.intValue,
                    openBoardId = openBoard.value,
                    onOpen = { openBoard.value = it },
                    onChanged = { version.intValue++ },
                    onAdd = { startActivity(Intent(this, EditBoardActivity::class.java)) },
                    onEdit = { id ->
                        startActivity(Intent(this, EditBoardActivity::class.java).putExtra(EXTRA_BOARD, id))
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_BOARD)?.let { openBoard.value = it }
    }

    override fun onResume() {
        super.onResume()
        if (WidgetRenderer.allIds(this).isNotEmpty()) Scheduler.ensure(this)
        version.intValue++
    }

    companion object {
        const val EXTRA_BOARD = "board_id"
    }
}

/** Pede ao launcher do Pixel para pôr um widget com este quadro no ecrã principal. */
fun pinBoard(ctx: Context, boardId: String) {
    val mgr = AppWidgetManager.getInstance(ctx)
    if (!mgr.isRequestPinAppWidgetSupported) {
        Toast.makeText(ctx, "O launcher não permite. Usa: manter premido › Widgets › Partidas.", Toast.LENGTH_LONG).show()
        return
    }
    WidgetLinks.savePending(ctx, boardId)
    val callback = PendingIntent.getBroadcast(
        ctx, 1,
        Intent(ctx, DeparturesWidget::class.java).setAction(DeparturesWidget.ACTION_PIN_DONE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    mgr.requestPinAppWidget(ComponentName(ctx, DeparturesWidget::class.java), null, callback)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScreen(
    version: Int,
    openBoardId: String?,
    onOpen: (String?) -> Unit,
    onChanged: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val boards = remember(version) { BoardStore.all(ctx) }
    var cacheVersion by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val refreshing = remember { mutableStateListOf<String>() }

    fun refresh(b: Board) {
        if (b.id in refreshing) return
        refreshing += b.id
        scope.launch {
            withContext(Dispatchers.IO) {
                Repo.refresh(ctx, b.id)
                WidgetRenderer.renderBoard(ctx, b.id)
            }
            refreshing -= b.id
            cacheVersion++
        }
    }

    // Relógio
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - now % 1_000L)
        }
    }
    // Dados: com a app aberta, cada quadro renova a cada 30 s
    LaunchedEffect(version) {
        while (true) {
            val t = System.currentTimeMillis()
            BoardStore.all(ctx).forEach { b ->
                val age = t - (DepartureCache.get(ctx, b.id)?.fetchedAt ?: 0L)
                if (age > 30_000L) refresh(b)
            }
            delay(10_000L)
        }
    }

    val open = openBoardId?.let { id -> boards.firstOrNull { it.id == id } }
    if (open != null) {
        FullBoard(open, cacheVersion, now, open.id in refreshing, onRefresh = { refresh(open) }, onClose = { onOpen(null) })
        return
    }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Partidas") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, text = { Text("Novo quadro") }, icon = { Text("+", style = MaterialTheme.typography.titleLarge) })
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (boards.isEmpty()) {
                Text(
                    "Cria um quadro para uma estação de comboio ou paragem de autocarro. " +
                        "Depois podes pô-lo como widget no ecrã principal.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            boards.forEachIndexed { index, b ->
                val cache = remember(cacheVersion, b.id) { DepartureCache.get(ctx, b.id) }
                val onHome = remember(version, cacheVersion, b.id) {
                    WidgetLinks.widgetsFor(ctx, b.id, WidgetRenderer.allIds(ctx)).isNotEmpty()
                }
                DepartureBoard(
                    board = b, cache = cache, now = now, refreshing = b.id in refreshing,
                    onRefresh = { refresh(b) },
                    onClick = { onOpen(b.id) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = { pinBoard(ctx, b.id) }) {
                        Text(if (onHome) "Adicionar outra vez ao ecrã" else "Adicionar ao ecrã principal")
                    }
                    TextButton(onClick = { onEdit(b.id) }) { Text("Editar") }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { menu = true }) { Text("⋯") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (index > 0) DropdownMenuItem(text = { Text("Mover para cima") }, onClick = {
                                menu = false; BoardStore.move(ctx, b.id, -1); onChanged()
                            })
                            if (index < boards.lastIndex) DropdownMenuItem(text = { Text("Mover para baixo") }, onClick = {
                                menu = false; BoardStore.move(ctx, b.id, 1); onChanged()
                            })
                            DropdownMenuItem(text = { Text("Apagar quadro") }, onClick = {
                                menu = false
                                BoardStore.delete(ctx, b.id)
                                WidgetRenderer.renderBoard(ctx, b.id)
                                onChanged()
                            })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                "Dica: no ecrã principal, arrasta um widget para a margem direita para criar uma página só de transportes. " +
                    "Widgets mais altos mostram mais partidas.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 96.dp)
            )
        }
    }
}

/** Quadro em ecrã inteiro, como um painel de estação. Mantém o ecrã ligado. */
@Composable
private fun FullBoard(board: Board, cacheVersion: Int, now: Long, refreshing: Boolean, onRefresh: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(onBack = onClose)
    val cache = remember(cacheVersion, board.id) { DepartureCache.get(ctx, board.id) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .safeDrawingPadding()
            .padding(12.dp)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            DepartureBoard(
                board = board, cache = cache, now = now, refreshing = refreshing,
                height = 340.dp, visibleTiles = 2, infoWidth = 124.dp, onRefresh = onRefresh,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) { Text("Fechar") }
        }
    }
}
