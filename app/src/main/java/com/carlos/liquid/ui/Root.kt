package com.carlos.liquid.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.carlos.liquid.data.Appearance
import com.carlos.liquid.data.GlassStyle
import com.carlos.liquid.data.Store
import com.carlos.liquid.glass.Blur
import com.carlos.liquid.glass.GlassEnv
import com.carlos.liquid.glass.LocalGlass
import com.carlos.liquid.glass.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadedWallpaper(val key: String, val sharp: Bitmap, val glass: Bitmap, val topLum: Float)

object WallpaperCache {
    private val map = HashMap<String, LoadedWallpaper>()
    private var lastApplied: String? = null

    suspend fun get(ctx: Context, id: String, dark: Boolean): LoadedWallpaper {
        val key = "$id:${if (id.startsWith("photo:")) "" else dark}"
        map[key]?.let { return it }
        return withContext(Dispatchers.Default) {
            val b = Wallpapers.load(ctx, id, dark)
            val g = Blur.glassSource(b, 10, 9)
            val lw = LoadedWallpaper(key, b, g, Wallpapers.luminance(b, 0f, 0.06f))
            synchronized(map) { if (map.size > 3) map.clear(); map[key] = lw }
            lw
        }
    }

    suspend fun applySystemIfNeeded(ctx: Context, home: LoadedWallpaper, lock: LoadedWallpaper?) {
        val key = home.key + "|" + (lock?.key ?: "")
        if (key == lastApplied) return
        val prefs = ctx.getSharedPreferences("liquid", Context.MODE_PRIVATE)
        if (prefs.getString("sysWallpaper", null) == key) { lastApplied = key; return }
        lastApplied = key
        prefs.edit().putString("sysWallpaper", key).commit()
        Wallpapers.applySystem(ctx, home.sharp, lock?.sharp ?: home.sharp)
    }
}

@Composable
fun isDark(): Boolean {
    val s by Store.settings.collectAsState()
    val sys = isSystemInDarkTheme()
    return when (s.appearance) { Appearance.LIGHT -> false; Appearance.DARK -> true; Appearance.AUTO -> sys }
}

/**
 * Raiz comum: tema (claro/escuro), tipo de letra, wallpaper e ambiente do vidro.
 * [lock] = usa o wallpaper do ecrã bloqueado.
 */
@Composable
fun LiquidRoot(lock: Boolean = false, drawWallpaper: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val s by Store.settings.collectAsState()
    val dark = isDark()
    val ctx = LocalContext.current
    val id = if (lock && !s.lock.sameWallpaper) s.lock.wallpaperId else s.wallpaperId
    val wp by produceState<LoadedWallpaper?>(null, id, dark) { value = WallpaperCache.get(ctx, id, dark) }
    val (sw, sh) = Wallpapers.screenSize(ctx)
    val dim = when {
        lock -> s.lock.dim
        dark && s.dimInDark -> 0.22f
        else -> 0f
    }
    val env = androidx.compose.runtime.remember(wp, sw, sh, dark, s.glass, s.refraction, dim, s.advancedGlass) { GlassEnv(wp?.glass, sw, sh, dark, s.glass == GlassStyle.TINTED, s.refraction, dim, s.advancedGlass) }

    if (!lock) LaunchedEffect(wp, s.syncSystemWallpaper, s.lock.sameWallpaper, s.lock.wallpaperId) {
        val home = wp ?: return@LaunchedEffect
        if (!s.syncSystemWallpaper) return@LaunchedEffect
        val lockWp = if (s.lock.sameWallpaper) null else WallpaperCache.get(ctx, s.lock.wallpaperId, dark)
        WallpaperCache.applySystemIfNeeded(ctx, home, lockWp)
    }

    val view = LocalView.current
    val lightBars = (wp?.topLum ?: 0f) > 0.62f && dim < 0.2f
    SideEffect {
        (view.context as? Activity)?.window?.let { w ->
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = if (drawWallpaper) lightBars else !dark
            c.isAppearanceLightNavigationBars = if (drawWallpaper) lightBars else !dark
        }
    }

    CompositionLocalProvider(
        LocalPalette provides Palette(dark),
        LocalFont provides Fonts.ui(s.font),
        LocalGlass provides env,
    ) {
        Box(Modifier.fillMaxSize().background(if (drawWallpaper) Color.Black else Palette(dark).groupedBg)) {
            if (drawWallpaper) {
                wp?.let { val img = androidx.compose.runtime.remember(it) { it.sharp.asImageBitmap() }; Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
                if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
            }
            content()
        }
    }
}
