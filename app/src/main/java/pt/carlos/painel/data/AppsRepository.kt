package pt.carlos.painel.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
)

/** Lista de apps instaladas, com ícones em cache; atualiza sozinha quando instalas ou removes apps. */
class AppsRepository(private val ctx: Context) {
    private val la = ctx.getSystemService(LauncherApps::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val iconCache = HashMap<String, ImageBitmap>()
    private val iconPx = (64 * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(96)

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var byKey by mutableStateOf<Map<String, AppEntry>>(emptyMap())
        private set
    var loaded by mutableStateOf(false)
        private set

    init {
        la.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload(packageName)
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload(packageName)
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload(packageName)
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload(null)
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload(null)
        }, Handler(Looper.getMainLooper()))
        reload(null)
    }

    fun reload(changedPkg: String?) {
        if (changedPkg != null) synchronized(iconCache) { iconCache.keys.removeAll { it.startsWith("$changedPkg/") } }
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                la.getActivityList(null, Process.myUserHandle()).map { info ->
                    val key = info.componentName.flattenToString()
                    val icon = synchronized(iconCache) {
                        iconCache.getOrPut(key) { info.getIcon(0).toBitmap(iconPx, iconPx).asImageBitmap() }
                    }
                    AppEntry(key, info.label.toString(), info.componentName, info.user, icon)
                }.sortedBy { it.label.lowercase(Locale.ROOT) }
            }
            apps = list
            byKey = list.associateBy { it.key }
            loaded = true
        }
    }

    fun launch(e: AppEntry, bounds: Rect? = null) {
        runCatching { la.startMainActivity(e.component, e.user, bounds, null) }
    }

    fun details(e: AppEntry) {
        runCatching { la.startAppDetailsActivity(e.component, e.user, null, null) }
    }

    fun uninstall(e: AppEntry) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${e.component.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
