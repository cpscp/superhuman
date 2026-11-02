package com.carlos.liquid.data

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings as AS
import android.telephony.TelephonyManager
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

data class AppEntry(
    val key: String,
    val pkg: String,
    val label: String,
    val category: String,
    val installTime: Long,
    val info: LauncherActivityInfo,
)

object AppRepo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps
    @Volatile var byKey: Map<String, AppEntry> = emptyMap(); private set
    private lateinit var la: LauncherApps
    private lateinit var app: Context

    fun keyOf(i: LauncherActivityInfo) = i.componentName.packageName + "/" + i.componentName.className

    fun init(ctx: Context) {
        app = ctx.applicationContext
        la = app.getSystemService(LauncherApps::class.java)
        reload()
        la.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) { reload(removed = packageName) }
            override fun onPackageAdded(packageName: String, user: UserHandle) { reload(added = packageName) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { reload() }
            override fun onPackagesAvailable(p: Array<out String>?, user: UserHandle?, r: Boolean) { reload() }
            override fun onPackagesUnavailable(p: Array<out String>?, user: UserHandle?, r: Boolean) { reload() }
        })
    }

    fun reload(removed: String? = null, added: String? = null) {
        scope.launch {
            val user = Process.myUserHandle()
            val col = Collator.getInstance(Locale("pt", "PT"))
            val list = la.getActivityList(null, user)
                .filter { it.componentName.packageName != app.packageName || it.componentName.className.endsWith("SettingsActivity") }
                .map { i ->
                    val ai = i.applicationInfo
                    AppEntry(keyOf(i), i.componentName.packageName, i.label.toString(), categoryOf(ai, i.componentName.packageName), firstInstall(i.componentName.packageName), i)
                }
                .sortedWith { a, b -> col.compare(a.label, b.label) }
            byKey = list.associateBy { it.key }
            _apps.value = list
            IconCache.clearMissing(byKey.keys)
            if (removed != null) Store.layout { l -> removeKeys(l) { k -> !byKey.containsKey(k) } }
            if (added != null && Store.s.newApps == NewApps.HOME && Store.l.initialized) {
                val newKeys = list.filter { it.pkg == added }.map { it.key }
                    .filter { k -> !layoutKeys(Store.l).contains(k) }
                if (newKeys.isNotEmpty()) Store.layout { l ->
                    val pages = l.pages.toMutableList()
                    val last = pages.last()
                    pages[pages.size - 1] = last.copy(items = last.items + newKeys.map { HomeItem.App(key = it) })
                    l.copy(pages = pages)
                }
            }
        }
    }

    private fun firstInstall(pkg: String): Long = runCatching {
        app.packageManager.getPackageInfo(pkg, 0).firstInstallTime
    }.getOrDefault(0L)

    fun layoutKeys(l: HomeLayout): Set<String> {
        val s = HashSet<String>()
        fun add(i: HomeItem) { when (i) { is HomeItem.App -> s += i.key; is HomeItem.Folder -> s += i.apps; else -> {} } }
        l.pages.forEach { p -> p.items.forEach(::add) }
        l.dock.forEach(::add)
        return s
    }

    fun removeKeys(l: HomeLayout, gone: (String) -> Boolean): HomeLayout {
        fun clean(items: List<HomeItem>): List<HomeItem> = items.mapNotNull { i ->
            when (i) {
                is HomeItem.App -> if (gone(i.key)) null else i
                is HomeItem.Folder -> {
                    val a = i.apps.filterNot(gone)
                    when { a.isEmpty() -> null; a.size == 1 -> HomeItem.App(key = a[0]); else -> i.copy(apps = a) }
                }
                else -> i
            }
        }
        return l.copy(pages = l.pages.map { it.copy(items = clean(it.items)) }, dock = clean(l.dock))
    }

    fun launch(ctx: Context, key: String, view: View? = null, bounds: Rect? = null) {
        val e = byKey[key] ?: return
        Store.recordLaunch(key)
        val opts: Bundle? = if (view != null && bounds != null)
            ActivityOptions.makeScaleUpAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
        else null
        runCatching { la.startMainActivity(e.info.componentName, e.info.user, bounds, opts) }
            .onFailure {
                runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(e.info.componentName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
    }

    fun appInfo(ctx: Context, key: String) {
        val e = byKey[key] ?: return
        runCatching { la.startAppDetailsActivity(e.info.componentName, e.info.user, null, null) }
    }

    fun uninstall(ctx: Context, key: String) {
        val e = byKey[key] ?: return
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + e.pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun shortcuts(key: String): List<ShortcutInfo> {
        val e = byKey[key] ?: return emptyList()
        if (!la.hasShortcutHostPermission()) return emptyList()
        return runCatching {
            val q = LauncherApps.ShortcutQuery()
                .setPackage(e.pkg)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            la.getShortcuts(q, Process.myUserHandle()).orEmpty()
                .filter { it.activity == null || it.activity == e.info.componentName }
                .sortedBy { it.rank }.take(4)
        }.getOrDefault(emptyList())
    }

    fun startShortcut(s: ShortcutInfo, bounds: Rect?) {
        runCatching { la.startShortcut(s, bounds, null) }
    }

    fun shortcutIcon(ctx: Context, s: ShortcutInfo) = runCatching { la.getShortcutIconDrawable(s, ctx.resources.displayMetrics.densityDpi) }.getOrNull()

    /** Categorias da Biblioteca de apps (nomes iguais aos do iOS). */
    private fun categoryOf(ai: ApplicationInfo, pkg: String): String {
        val p = pkg.lowercase()
        fun has(vararg k: String) = k.any { p.contains(it) }
        return when {
            has("bank", "banco", "millennium", "caixa", "santander", "revolut", "mbway", "paypal", "wallet", "moey", "activobank", "novobanco", "bpi", "cgd", "finance", "invest", "trading", "crypto") -> "Produtividade e finanças"
            has("fit", "health", "strava", "garmin", "mifit", "zepp", "sleep", "workout", "mi.health", "xiaomi.wearable") -> "Saúde e forma física"
            has("shop", "amazon", "aliexpress", "temu", "shein", "zara", "ikea", "glovo", "ubereats", "eats", "bolt.food", "continente", "pingodoce", "lidl", "worten", "vinted", "olx", "mercadona") -> "Compras e comida"
            has("uber", "bolt", "maps", "waze", "booking", "airbnb", "ryanair", "tap", "easyjet", "cp.", "carris", "metro", "transit", "citymapper", "moovit", "flight", "trip") -> "Viagens"
            ai.category == ApplicationInfo.CATEGORY_GAME || has("game", "games") -> "Jogos"
            ai.category == ApplicationInfo.CATEGORY_SOCIAL || has("whatsapp", "instagram", "facebook", "twitter", "telegram", "messenger", "snapchat", "tiktok", "linkedin", "reddit", "discord", "signal", "threads", "bluesky") -> "Social"
            ai.category == ApplicationInfo.CATEGORY_AUDIO || ai.category == ApplicationInfo.CATEGORY_VIDEO || has("spotify", "netflix", "youtube", "music", "disney", "hbo", "max", "prime", "twitch", "stremio", "nuvio", "podcast", "dazn", "sporttv", "rtp", "sic", "tvi", "opto") -> "Entretenimento"
            ai.category == ApplicationInfo.CATEGORY_IMAGE || has("photo", "camera", "gallery", "lightroom", "snapseed", "canva", "picsart", "capcut", "lente") -> "Criatividade"
            ai.category == ApplicationInfo.CATEGORY_NEWS || has("news", "noticias", "publico", "expresso", "observador", "pressreader", "kindle", "books", "reader", "medium") -> "Informação e leitura"
            ai.category == ApplicationInfo.CATEGORY_MAPS -> "Viagens"
            ai.category == ApplicationInfo.CATEGORY_PRODUCTIVITY || has("docs", "sheets", "slides", "office", "outlook", "teams", "gmail", "calendar", "drive", "keep", "notion", "slack", "zoom", "word", "excel", "powerpoint", "onedrive", "todo", "tasks", "claude", "chatgpt", "gemini", "superhuman") -> "Produtividade e finanças"
            else -> "Utilitários"
        }
    }

    // ------------ Apps "do sistema" para a dock inicial ------------
    fun defaultFor(ctx: Context, intent: Intent): String? = runCatching {
        val ri = ctx.packageManager.resolveActivity(intent, 0) ?: return null
        val pkg = ri.activityInfo.packageName
        _apps.value.firstOrNull { it.pkg == pkg }?.key
    }.getOrNull()

    fun dialerKey(ctx: Context) = defaultFor(ctx, Intent(Intent.ACTION_DIAL))
    fun browserKey(ctx: Context) = defaultFor(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    fun cameraKey(ctx: Context) = defaultFor(ctx, Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    fun smsKey(ctx: Context): String? = runCatching {
        val pkg = android.provider.Telephony.Sms.getDefaultSmsPackage(ctx)
        _apps.value.firstOrNull { it.pkg == pkg }?.key
    }.getOrNull()
    fun settingsKey(ctx: Context) = defaultFor(ctx, Intent(AS.ACTION_SETTINGS))
    fun firstOf(vararg pkgs: String): String? = pkgs.firstNotNullOfOrNull { p -> _apps.value.firstOrNull { it.pkg == p }?.key }
}
