package pt.carlos.painel.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
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
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class AppEntry(
    val key: String,
    val label: String,
    val originalLabel: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
)

/** Lista de apps instaladas, com ícones desenhados no estilo Pixel e em cache; atualiza sozinha. */
class AppsRepository(private val ctx: Context, private val prefs: Prefs) {
    private val la = ctx.getSystemService(LauncherApps::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val iconCache = HashMap<String, ImageBitmap>()
    private val iconPx = (64 * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(96)
    private val iconDir = File(ctx.filesDir, "icons").apply { mkdirs() }
    private var dark = false

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

    /** Volta a desenhar todos os ícones (forma, ícones temáticos ou tema mudaram). */
    fun restyle(darkTheme: Boolean = dark) {
        dark = darkTheme
        synchronized(iconCache) { iconCache.clear() }
        reload(null)
    }

    fun setDark(darkTheme: Boolean) { if (darkTheme != dark) restyle(darkTheme) }

    private fun themedColors(): Pair<Int, Int> {
        if (prefs.dynamicColor) {
            return if (dark) ctx.getColor(android.R.color.system_accent1_800) to ctx.getColor(android.R.color.system_accent1_100)
            else ctx.getColor(android.R.color.system_accent1_100) to ctx.getColor(android.R.color.system_accent1_700)
        }
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(prefs.seed, hsl)
        val s = hsl[1].coerceIn(0.35f, 0.85f)
        val bg = ColorUtils.HSLToColor(floatArrayOf(hsl[0], s, if (dark) 0.25f else 0.90f))
        val fg = ColorUtils.HSLToColor(floatArrayOf(hsl[0], s, if (dark) 0.88f else 0.30f))
        return bg to fg
    }

    private fun iconFile(key: String) = File(iconDir, Integer.toHexString(key.hashCode()) + ".png")

    fun reload(changedPkg: String?) {
        if (changedPkg != null) synchronized(iconCache) { iconCache.keys.removeAll { it.startsWith("$changedPkg/") } }
        val shape = prefs.iconShape
        val themed = prefs.themedIcons
        val labels = prefs.labelOverrides
        val (bg, fg) = themedColors()
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                la.getActivityList(null, Process.myUserHandle()).mapNotNull { info ->
                    runCatching {
                        val key = info.componentName.flattenToString()
                        val icon = synchronized(iconCache) {
                            iconCache.getOrPut(key) {
                                val f = iconFile(key)
                                val custom = if (f.exists()) BitmapFactory.decodeFile(f.path) else null
                                IconRenderer.render(info.getIcon(0), custom, iconPx, shape, themed, bg, fg).asImageBitmap()
                            }
                        }
                        val original = info.label.toString()
                        AppEntry(key, labels[key] ?: original, original, info.componentName, info.user, icon)
                    }.getOrNull()
                }.sortedBy { it.label.lowercase(Locale.ROOT) }
            }
            apps = list
            byKey = list.associateBy { it.key }
            loaded = true
        }
    }

    // ---- Edição de ícones ----

    fun setLabel(key: String, label: String) {
        val original = byKey[key]?.originalLabel
        prefs.setLabel(key, if (label.isBlank() || label == original) null else label.trim())
        reload(null)
    }

    fun setCustomIcon(key: String, uri: Uri) {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                    val bmp = ImageDecoder.decodeBitmap(src) { dec, _, _ ->
                        dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        dec.setTargetSampleSize(1)
                    }
                    val side = minOf(bmp.width, bmp.height)
                    val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
                    val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
                    iconFile(key).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
            synchronized(iconCache) { iconCache.remove(key) }
            reload(null)
        }
    }

    fun resetIcon(key: String) {
        iconFile(key).delete()
        prefs.setLabel(key, null)
        synchronized(iconCache) { iconCache.remove(key) }
        reload(null)
    }

    // ---- Ações ----

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
