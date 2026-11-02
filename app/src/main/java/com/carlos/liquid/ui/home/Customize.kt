package com.carlos.liquid.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.Appearance
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.GlassStyle
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.IconMode
import com.carlos.liquid.data.IconStyle
import com.carlos.liquid.data.Store
import com.carlos.liquid.glass.Wallpapers
import com.carlos.liquid.icons.IconSpec
import com.carlos.liquid.ui.LSlider
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalPalette
import com.carlos.liquid.ui.Segmented
import com.carlos.liquid.ui.SheetTitle
import com.carlos.liquid.ui.ToggleRow
import com.carlos.liquid.ui.noRippleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun styleName(s: IconStyle) = when (s) { IconStyle.DEFAULT -> "Predefinido"; IconStyle.DARK -> "Escuro"; IconStyle.CLEAR -> "Transparente"; IconStyle.TINTED -> "Tingido" }

/** Folha "Personalizar" — as mesmas opções do iOS 18/26. */
@Composable
fun ColumnScope.CustomizeContent(onWallpaper: () -> Unit) {
    val s by Store.settings.collectAsState()
    val layout by Store.layout.collectAsState()
    val apps by AppRepo.apps.collectAsState()
    val p = LocalPalette.current
    val sample = remember(layout, apps) {
        (layout.dock.filterIsInstance<HomeItem.App>().map { it.key } + apps.map { it.key }).distinct().take(3)
    }
    SheetTitle("Personalizar")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconStyle.entries.forEach { style ->
            val sel = s.iconStyle == style
            val dark = resolveDark(s, p.dark)
            val eff = if (style == IconStyle.DEFAULT && dark) IconStyle.DARK else style
            val spec = IconSpec(eff, if (style == IconStyle.DARK) true else dark, tintColorInt(s), 120)
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .background(if (sel) p.accent.copy(alpha = 0.18f) else p.fill)
                    .border(if (sel) 2.dp else 0.dp, if (sel) p.accent else Color.Transparent, RoundedCornerShape(18.dp))
                    .noRippleClick { Store.settings { it.copy(iconStyle = style) } }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF5E5CE6), Color(0xFF64D2FF)))), contentAlignment = Alignment.Center) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { sample.take(2).forEach { IconImage(it, 22.dp, spec) } }
                        sample.drop(2).take(1).forEach { IconImage(it, 22.dp, spec) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LText(styleName(style), 12.sp, if (sel) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Segmented(listOf("Automático", "Claro", "Escuro"), s.iconMode.ordinal) { i -> Store.settings { it.copy(iconMode = IconMode.entries[i]) } }
    if (s.iconStyle == IconStyle.TINTED) {
        Spacer(Modifier.height(16.dp))
        LText("Cor", 13.sp, color = p.secondary)
        LSlider(s.tintHue, { v -> Store.settings { it.copy(tintHue = v) } }, range = 0f..360f,
            track = Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.9f, 1f) }))
        LText("Intensidade", 13.sp, color = p.secondary)
        LSlider(s.tintSat, { v -> Store.settings { it.copy(tintSat = v) } },
            track = Brush.horizontalGradient(listOf(Color.hsv(s.tintHue, 0f, 1f), Color.hsv(s.tintHue, 1f, 1f))))
    }
    Spacer(Modifier.height(18.dp))
    LText("Tamanho dos ícones", 13.sp, color = p.secondary, modifier = Modifier.padding(bottom = 6.dp))
    Segmented(listOf("Pequeno (com nomes)", "Grande"), if (s.largeIcons) 1 else 0) { i -> Store.settings { it.copy(largeIcons = i == 1) } }
    Spacer(Modifier.height(18.dp))
    LText("Liquid Glass", 13.sp, color = p.secondary, modifier = Modifier.padding(bottom = 6.dp))
    Segmented(listOf("Transparente", "Tingido"), s.glass.ordinal) { i -> Store.settings { it.copy(glass = GlassStyle.entries[i]) } }
    Spacer(Modifier.height(8.dp))
    LText("Refração das bordas", 13.sp, color = p.secondary)
    LSlider(s.refraction, { v -> Store.settings { it.copy(refraction = v) } }, range = 0f..1.6f)
    Spacer(Modifier.height(18.dp))
    LText("Aspeto", 13.sp, color = p.secondary, modifier = Modifier.padding(bottom = 6.dp))
    Segmented(listOf("Claro", "Escuro", "Automático"), s.appearance.ordinal) { i -> Store.settings { it.copy(appearance = Appearance.entries[i]) } }
    Spacer(Modifier.height(18.dp))
    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
        ToggleRow("Mostrar nomes das apps", s.showLabels) { v -> Store.settings { it.copy(showLabels = v) } }
        ToggleRow("Indicadores de notificações", s.badges, divider = false) { v -> Store.settings { it.copy(badges = v) } }
    }
    Spacer(Modifier.height(14.dp))
    com.carlos.liquid.ui.widgets.ActionButton("Wallpaper…", onClick = onWallpaper)
}

// ================================ Wallpaper ================================

@Composable
fun ColumnScope.WallpaperContent() {
    val s by Store.settings.collectAsState()
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var forLock by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf(Wallpapers.photoIds(ctx)) }
    val current = if (forLock && !s.lock.sameWallpaper) s.lock.wallpaperId else s.wallpaperId
    fun choose(id: String) {
        if (forLock) Store.lock { it.copy(wallpaperId = id, sameWallpaper = false) } else Store.settings { it.copy(wallpaperId = id) }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { Wallpapers.importPhoto(ctx, uri)?.let { choose(it); photos = Wallpapers.photoIds(ctx) } }
    }
    SheetTitle("Wallpaper")
    Segmented(listOf("Ecrã principal", "Ecrã bloqueado"), if (forLock) 1 else 0) { forLock = it == 1 }
    Spacer(Modifier.height(14.dp))
    if (forLock) {
        Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
            ToggleRow("Igual ao ecrã principal", s.lock.sameWallpaper, divider = false) { v -> Store.lock { it.copy(sameWallpaper = v) } }
        }
        Spacer(Modifier.height(14.dp))
    }
    val all = Wallpapers.presets.map { it.id } + photos
    all.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { id ->
                val sel = id == current && !(forLock && s.lock.sameWallpaper)
                val thumb by produceState<ImageBitmap?>(null, id, p.dark) {
                    value = withContext(Dispatchers.Default) {
                        runCatching {
                            if (id.startsWith("photo:")) BitmapFactory.decodeFile(Wallpapers.photoFile(ctx, id.removePrefix("photo:")).absolutePath, BitmapFactory.Options().apply { inSampleSize = 8 })?.asImageBitmap()
                            else Wallpapers.generate(Wallpapers.presets.first { it.id == id }, p.dark, 160, 346).asImageBitmap()
                        }.getOrNull()
                    }
                }
                Box(
                    Modifier.weight(1f).aspectRatio(0.46f).clip(RoundedCornerShape(14.dp)).background(p.fill)
                        .border(if (sel) 3.dp else 0.dp, if (sel) p.accent else Color.Transparent, RoundedCornerShape(14.dp))
                        .noRippleClick { choose(id) },
                ) { thumb?.let { Image(it, null, Modifier.fillMaxWidth().aspectRatio(0.46f), contentScale = ContentScale.Crop) } }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Row(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(p.fill)
            .noRippleClick { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) { Icon(Icons.Rounded.Add, null, tint = p.accent); Spacer(Modifier.width(6.dp)); LText("Escolher fotografia", 17.sp, FontWeight.SemiBold, p.accent) }
    Spacer(Modifier.height(14.dp))
    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(p.cell2)) {
        ToggleRow("Escurecer no modo escuro", s.dimInDark) { v -> Store.settings { it.copy(dimInDark = v) } }
        ToggleRow("Aplicar também ao wallpaper do sistema", s.syncSystemWallpaper, divider = false, subtitle = "Para as transições e o ecrã bloqueado do Android combinarem.") { v -> Store.settings { it.copy(syncSystemWallpaper = v) } }
    }
    LText("Os wallpapers do Liquid mudam automaticamente entre a versão clara e escura.", 13.sp, color = p.secondary, modifier = Modifier.padding(top = 8.dp))
}
