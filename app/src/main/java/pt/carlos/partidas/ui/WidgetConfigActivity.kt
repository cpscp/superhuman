package pt.carlos.partidas.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pt.carlos.partidas.data.BoardStore
import pt.carlos.partidas.data.DepartureCache
import pt.carlos.partidas.data.WidgetLinks
import pt.carlos.partidas.widget.RefreshWorker
import pt.carlos.partidas.widget.Scheduler
import pt.carlos.partidas.widget.WidgetRenderer

/** Aberto pelo launcher ao adicionar o widget (ou ao reconfigurá-lo): escolhe que quadro mostra. */
class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        pt.carlos.partidas.data.Defaults.seedIfFirstRun(this)
        setResult(Activity.RESULT_CANCELED, result())

        // Widget pedido a partir da app: o quadro já está escolhido.
        if (WidgetLinks.get(this, widgetId) == null && WidgetLinks.adoptPending(this, widgetId)) {
            done(); return
        }
        enableEdgeToEdge()
        setContent {
            PartidasTheme {
                PickScreen(
                    current = WidgetLinks.get(this, widgetId),
                    onPick = { id -> WidgetLinks.set(this, widgetId, id); done() },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun result() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    private fun done() {
        val boardId = WidgetLinks.get(this, widgetId)
        WidgetRenderer.render(this, AppWidgetManager.getInstance(this), widgetId)
        if (boardId != null) {
            if (DepartureCache.get(this, boardId) == null) DepartureCache.markRefreshing(this, boardId)
            RefreshWorker.refresh(this, arrayOf(boardId))
        }
        Scheduler.ensure(this)
        setResult(Activity.RESULT_OK, result())
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickScreen(current: String?, onPick: (String) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val boards = remember { BoardStore.all(ctx) }
    val now = remember { System.currentTimeMillis() }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringExtra(MainActivity.EXTRA_BOARD)?.let(onPick)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escolhe o quadro") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancelar") } }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { create.launch(Intent(ctx, EditBoardActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Novo quadro") }

            if (boards.isEmpty()) {
                Text("Ainda não tens quadros — cria o primeiro.", style = MaterialTheme.typography.bodyMedium)
            }
            boards.forEach { b ->
                if (b.id == current) Text("Atual", style = MaterialTheme.typography.labelMedium)
                DepartureBoard(
                    board = b, cache = DepartureCache.get(ctx, b.id), now = now, refreshing = false,
                    height = 200.dp, onClick = { onPick(b.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
