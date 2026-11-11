package pt.carlos.painel.ui

import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pt.carlos.painel.PainelApp
import pt.carlos.painel.data.Item
import pt.carlos.painel.theme.PainelTheme
import kotlin.math.ceil

/** O ecrã inicial do tablet (HOME). O botão Home traz sempre aqui. */
class HomeActivity : ComponentActivity() {
    private val app get() = PainelApp.instance
    private var homeTick by mutableIntStateOf(0)
    private var isDefaultHome by mutableStateOf(true)
    private var cellW = 120f
    private var cellH = 120f

    // Widget a meio de ser adicionado (autorização e configuração).
    private var pendingId = -1
    private var pendingInfo: AppWidgetProviderInfo? = null
    private var pendingPage = 0

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) configureOrAdd() else cancelPending()
    }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = checkDefaultHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            PainelTheme(app.prefs) {
                HomeScreen(
                    app = app,
                    homeTick = homeTick,
                    showSetDefault = !isDefaultHome,
                    onSetDefault = ::requestDefaultHome,
                    onPickWidget = ::startAddWidget,
                    onCellSize = { w, h -> cellW = w; cellH = h },
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { app.widgetHost.startListening() }
    }

    override fun onResume() {
        super.onResume()
        isDefaultHome = checkDefaultHome()
    }

    override fun onStop() {
        super.onStop()
        runCatching { app.widgetHost.stopListening() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) homeTick++
    }

    private fun checkDefaultHome(): Boolean =
        runCatching { getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(true)

    private fun requestDefaultHome() {
        val rm = getSystemService(RoleManager::class.java)
        val ok = runCatching {
            if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); true
            } else false
        }.getOrDefault(false)
        if (!ok) runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }

    // ---- Adicionar widgets ----

    private fun startAddWidget(info: AppWidgetProviderInfo, page: Int) {
        val id = app.widgetHost.allocateAppWidgetId()
        pendingId = id; pendingInfo = info; pendingPage = page
        val awm = AppWidgetManager.getInstance(this)
        if (awm.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configureOrAdd()
        } else {
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            )
        }
    }

    private fun configureOrAdd() {
        val info = pendingInfo ?: return
        val optional = (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure != null && !optional) {
            val started = runCatching {
                app.widgetHost.startAppWidgetConfigureActivityForResult(this, pendingId, 0, REQ_CONFIG, null)
            }.isSuccess
            if (!started) cancelPending()
        } else addPending()
    }

    @Deprecated("Resultado da configuração do widget")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIG) {
            if (resultCode == RESULT_OK) addPending() else cancelPending()
        }
    }

    private fun spanFor(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val d = resources.displayMetrics.density
        val cols = app.prefs.columns
        val rows = app.prefs.rows
        val w = if (info.targetCellWidth > 0) info.targetCellWidth else ceil((info.minWidth / d) / cellW).toInt()
        val h = if (info.targetCellHeight > 0) info.targetCellHeight else ceil((info.minHeight / d) / cellH).toInt()
        return w.coerceIn(1, cols) to h.coerceIn(1, rows)
    }

    private fun addPending() {
        val info = pendingInfo ?: return
        val (w, h) = spanFor(info)
        app.layout.add(Item.WIDGET, "", pendingId, w, h, pendingPage, app.prefs.columns, app.prefs.rows)
        pendingId = -1; pendingInfo = null
    }

    private fun cancelPending() {
        if (pendingId != -1) runCatching { app.widgetHost.deleteAppWidgetId(pendingId) }
        pendingId = -1; pendingInfo = null
    }

    companion object { private const val REQ_CONFIG = 41 }
}
