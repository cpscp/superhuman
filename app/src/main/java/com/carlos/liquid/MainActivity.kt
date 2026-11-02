package com.carlos.liquid

import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.BatteryRepo
import com.carlos.liquid.data.CalendarRepo
import com.carlos.liquid.data.HealthRepo
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.HomeLayout
import com.carlos.liquid.data.Page
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.WeatherRepo
import com.carlos.liquid.data.WidgetKind
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.settings.SetupSheet
import com.carlos.liquid.ui.LiquidRoot
import com.carlos.liquid.ui.home.HomeScreen
import com.carlos.liquid.ui.home.HomeState
import com.carlos.liquid.ui.home.addWidget
import com.carlos.liquid.ui.widgets.WidgetHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val homeSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val st = HomeState()

    private data class PendingWidget(val id: Int, val info: AppWidgetProviderInfo, val size: WidgetSize, val toToday: Boolean)
    private var pending: PendingWidget? = null

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pending ?: return@registerForActivityResult
        if (r.resultCode == RESULT_OK) configure(p) else { WidgetHost.delete(p.id); pending = null }
    }
    private val configLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pending ?: return@registerForActivityResult
        if (r.resultCode == RESULT_OK) finishAdd(p) else { WidgetHost.delete(p.id); pending = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        WidgetHost.init(this)
        setContent {
            val s by Store.settings.collectAsState()
            LiquidRoot {
                LaunchedEffect(Unit) { ensureDefaultLayout() }
                LaunchedEffect(Unit) {
                    while (true) { refreshData(); delay(5 * 60_000L) }
                }
                HomeScreen(st, homeSignal, ::addAndroidWidget) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
                if (!s.onboarded) SetupSheet(onDone = { Store.settings { it.copy(onboarded = true) } })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal.tryEmit(Unit)
    }

    override fun onStart() { super.onStart(); WidgetHost.start() }
    override fun onStop() { super.onStop(); WidgetHost.stop() }
    override fun onResume() { super.onResume(); lifecycleScope.launch { refreshData() } }

    private suspend fun refreshData() {
        BatteryRepo.refresh(this)
        lifecycleScope.launch { WeatherRepo.refresh(this@MainActivity) }
        lifecycleScope.launch { CalendarRepo.refresh(this@MainActivity) }
        lifecycleScope.launch { HealthRepo.refresh(this@MainActivity) }
    }

    fun isDefaultHome(): Boolean = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

    // ------------------- widgets Android -------------------
    private fun addAndroidWidget(info: AppWidgetProviderInfo, size: WidgetSize, toToday: Boolean) {
        val id = WidgetHost.host.allocateAppWidgetId()
        val p = PendingWidget(id, info, size, toToday)
        pending = p
        val ok = runCatching { WidgetHost.manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) }.getOrDefault(false)
        if (ok) configure(p)
        else bindLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile))
    }

    private fun configure(p: PendingWidget) {
        if (p.info.configure == null) { finishAdd(p); return }
        runCatching {
            configLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(p.info.configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, p.id))
        }.onFailure {
            runCatching { WidgetHost.host.startAppWidgetConfigureActivityForResult(this, p.id, 0, 4242, null) }
            finishAdd(p)
        }
    }

    private fun finishAdd(p: PendingWidget) {
        pending = null
        addWidget(HomeItem.Widget(kind = WidgetKind.ANDROID, size = p.size, androidId = p.id), p.toToday, null)
    }

    // ------------------- disposição inicial -------------------
    private suspend fun ensureDefaultLayout() {
        if (Store.l.initialized) return
        val apps = AppRepo.apps.first { it.isNotEmpty() }
        val keys = apps.map { it.key }.toSet()
        val dock = listOfNotNull(AppRepo.dialerKey(this), AppRepo.smsKey(this), AppRepo.browserKey(this), AppRepo.cameraKey(this))
            .distinct().filter { it in keys }.take(4)
        val favPkgs = listOf(
            "com.google.android.gm", "com.google.android.calendar", "com.google.android.apps.photos", "com.google.android.apps.maps",
            "com.google.android.youtube", "com.whatsapp", "com.spotify.music", "com.google.android.deskclock",
            "com.google.android.apps.bard", "com.google.android.keep", "com.google.android.apps.docs", "com.android.vending",
            "com.google.android.apps.nbu.files", "com.google.android.contacts", "com.android.settings",
        )
        val fav = favPkgs.mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg }?.key }.filter { it !in dock }.take(15).toMutableList()
        apps.firstOrNull { it.pkg == packageName }?.key?.let { fav += it }
        val rest = apps.map { it.key }.filter { it !in dock && it !in fav }
        val page1 = listOf<HomeItem>(
            HomeItem.Widget(kind = WidgetKind.WEATHER, size = WidgetSize.SMALL),
            HomeItem.Widget(kind = WidgetKind.CALENDAR, size = WidgetSize.SMALL),
        ) + fav.map { HomeItem.App(key = it) }
        val pages = listOf(Page(items = page1), Page(items = rest.map { HomeItem.App(key = it) }))
        val today = listOf(
            HomeItem.Widget(kind = WidgetKind.CLOCK_ANALOG, size = WidgetSize.SMALL),
            HomeItem.Widget(kind = WidgetKind.ACTIVITY, size = WidgetSize.SMALL),
            HomeItem.Widget(kind = WidgetKind.TRANSIT, size = WidgetSize.MEDIUM),
            HomeItem.Widget(kind = WidgetKind.WEATHER, size = WidgetSize.MEDIUM),
            HomeItem.Widget(kind = WidgetKind.BATTERY, size = WidgetSize.MEDIUM),
        )
        Store.layout { HomeLayout(pages = pages, dock = dock.map { HomeItem.App(key = it) }, today = today, initialized = true) }
    }
}
