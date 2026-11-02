package com.carlos.liquid.ui.home

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ShortcutInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.Settings
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.WidgetKind
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.glass.LocalGlass
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.glass.panelTint
import com.carlos.liquid.icons.IconSpec
import com.carlos.liquid.ui.AlertButton
import com.carlos.liquid.ui.GlassSheet
import com.carlos.liquid.ui.IOSAlert
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalFont
import com.carlos.liquid.ui.LocalPalette
import com.carlos.liquid.ui.SheetTitle
import com.carlos.liquid.ui.noRippleClick
import com.carlos.liquid.ui.widgets.GalleryContent
import com.carlos.liquid.ui.widgets.WidgetConfigContent
import com.carlos.liquid.ui.widgets.WidgetDims
import com.carlos.liquid.ui.widgets.WidgetHost
import com.carlos.liquid.ui.widgets.widgetName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

fun removeEverywhere(id: String) {
    val l = Store.l
    val w = (l.pages.flatMap { it.items } + l.today).firstOrNull { it.id == id } as? HomeItem.Widget
    if (w?.kind == WidgetKind.ANDROID) WidgetHost.delete(w.androidId)
    Store.layout { x ->
        x.copy(
            pages = x.pages.map { p -> p.copy(items = p.items.filterNot { it.id == id }) },
            dock = x.dock.filterNot { it.id == id },
            today = x.today.filterNot { it.id == id },
        )
    }
}

fun addWidget(item: HomeItem.Widget, toToday: Boolean, pageId: String?) = Store.layout { l ->
    if (toToday) l.copy(today = listOf(item) + l.today)
    else {
        val pid = pageId ?: l.pages.first().id
        l.copy(pages = l.pages.map { if (it.id == pid) it.copy(items = listOf<HomeItem>(item) + it.items) else it })
    }
}

@Composable
fun BoxScope.Overlays(
    st: HomeState, geo: Geo, spec: IconSpec, dims: WidgetDims, s: Settings, badges: Map<String, Int>, actions: HomeActions,
    onAddAndroidWidget: (AppWidgetProviderInfo, WidgetSize, Boolean) -> Unit,
    firstPageId: () -> String?,
) {
    st.menu?.let { m -> ContextMenu(m, st, geo, spec, dims, s, badges, actions) }
    st.folder?.let { f -> FolderOverlay(f, st, geo, spec, actions) }
    if (st.spotlight) Spotlight(st, spec, geo)
    if (st.librarySearch) LibrarySearch(st, spec, geo)

    val sheet = st.sheet
    GlassSheet(sheet != null, onDismiss = { st.sheet = null }) {
        when (sheet) {
            is Sheet.Gallery -> GalleryContent(dims,
                onAdd = { w ->
                    addWidget(w, sheet.toToday, firstPageId())
                    st.sheet = if (w.kind == WidgetKind.TRANSIT || w.kind == WidgetKind.PHOTOS) Sheet.WidgetConfig(w.id) else null
                },
                onAddAndroid = { info, size -> st.sheet = null; onAddAndroidWidget(info, size, sheet.toToday) })
            Sheet.Customize -> CustomizeContent(onWallpaper = { st.sheet = Sheet.Wallpaper })
            Sheet.Pages -> PagesContent()
            is Sheet.WidgetConfig -> WidgetConfigContent(sheet.itemId) { st.sheet = null }
            Sheet.Wallpaper -> WallpaperContent()
            null -> {}
        }
    }

    val r = st.remove
    val ctx = LocalContext.current
    if (r != null) {
        val item = r.item
        val (title, msg) = when (item) {
            is HomeItem.App -> "Remover «${AppRepo.byKey[item.key]?.label ?: "app"}»?" to "Remover do ecrã principal mantém a app na Biblioteca de apps."
            is HomeItem.Folder -> "Remover a pasta «${item.name}»?" to "As apps continuam disponíveis na Biblioteca de apps."
            is HomeItem.Widget -> "Remover o widget «${widgetName(item.kind)}»?" to "Os dados não são apagados."
        }
        val buttons = buildList {
            if (item is HomeItem.App) add(AlertButton("Eliminar app", destructive = true) { st.remove = null; AppRepo.uninstall(ctx, item.key) })
            add(AlertButton(if (item is HomeItem.App) "Remover do ecrã principal" else "Remover", destructive = item !is HomeItem.App) { st.remove = null; removeEverywhere(item.id) })
            add(AlertButton("Cancelar", bold = true) { st.remove = null })
        }
        IOSAlert(true, title, msg, buttons) { st.remove = null }
    }
}

// ================================ Menu de contexto ================================

data class MenuEntry(val text: String, val icon: ImageVector?, val destructive: Boolean = false, val image: ImageBitmap? = null, val action: () -> Unit)

@Composable
fun BoxScope.ContextMenu(m: MenuInfo, st: HomeState, geo: Geo, spec: IconSpec, dims: WidgetDims, s: Settings, badges: Map<String, Int>, actions: HomeActions) {
    val ctx = LocalContext.current
    val d = geo.density
    val shortcuts: List<ShortcutInfo> = remember(m) { (m.item as? HomeItem.App)?.let { AppRepo.shortcuts(it.key) }.orEmpty() }
    val scIcons by produceState<Map<String, ImageBitmap>>(emptyMap(), m) {
        value = withContext(Dispatchers.Default) {
            shortcuts.mapNotNull { sc ->
                val dr = AppRepo.shortcutIcon(ctx, sc) ?: return@mapNotNull null
                runCatching {
                    val b = android.graphics.Bitmap.createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888)
                    dr.setBounds(0, 0, 72, 72); dr.draw(android.graphics.Canvas(b)); sc.id to b.asImageBitmap()
                }.getOrNull()
            }.toMap()
        }
    }
    val close = { st.menu = null }
    val entries = buildList {
        when (val it = m.item) {
            is HomeItem.App -> {
                if (m.folderId != null) add(MenuEntry("Mover para o ecrã principal", Icons.Rounded.GridView) {
                    close(); st.folder = null
                    Store.layout { l ->
                        fun fix(list: List<HomeItem>) = list.mapNotNull { x ->
                            if (x is HomeItem.Folder && x.id == m.folderId) { val a = x.apps - it.key; if (a.size == 1) HomeItem.App(key = a[0]) else if (a.isEmpty()) null else x.copy(apps = a) } else x
                        }
                        val pages = l.pages.map { p -> p.copy(items = fix(p.items)) }
                        l.copy(pages = pages.dropLast(1) + pages.last().copy(items = pages.last().items + HomeItem.App(key = it.key)), dock = fix(l.dock))
                    }
                })
                shortcuts.forEach { sc -> add(MenuEntry(sc.shortLabel?.toString() ?: "", null, image = scIcons[sc.id]) { close(); AppRepo.startShortcut(sc, null) }) }
                if (m.folderId == null) add(MenuEntry("Editar ecrã principal", Icons.Rounded.Edit) { close(); st.edit = true })
                add(MenuEntry("Informações da app", Icons.Rounded.Info) { close(); AppRepo.appInfo(ctx, it.key) })
                if (m.folderId == null) add(MenuEntry("Remover app", Icons.Rounded.RemoveCircleOutline, destructive = true) { close(); st.remove = RemoveAsk(it) })
            }
            is HomeItem.Folder -> {
                add(MenuEntry("Mudar o nome", Icons.Rounded.DriveFileRenameOutline) { close(); st.renameFolder = it.id; st.folder = FolderOpen(it.id, m.bounds) })
                add(MenuEntry("Editar ecrã principal", Icons.Rounded.Edit) { close(); st.edit = true })
                add(MenuEntry("Remover pasta", Icons.Rounded.RemoveCircleOutline, destructive = true) { close(); st.remove = RemoveAsk(it) })
            }
            is HomeItem.Widget -> {
                if (it.kind != WidgetKind.ANDROID) add(MenuEntry("Editar widget", Icons.Rounded.Tune) { close(); st.sheet = Sheet.WidgetConfig(it.id) })
                add(MenuEntry("Editar ecrã principal", Icons.Rounded.Edit) { close(); st.edit = true })
                add(MenuEntry("Remover widget", Icons.Rounded.RemoveCircleOutline, destructive = true) { close(); st.remove = RemoveAsk(it) })
            }
        }
    }
    val isWidget = m.item is HomeItem.Widget
    val menuH = (entries.size * 50 + (if (isWidget) 58 else 0) + 12) * d
    val below = m.bounds.bottom + 12 * d + menuH < geo.h - 40 * d
    val menuW = 250 * d
    val x = (m.bounds.center.x - menuW / 2).coerceIn(12 * d, geo.w - menuW - 12 * d)
    val y = if (below) m.bounds.bottom + 12 * d else (m.bounds.top - 12 * d - menuH).coerceAtLeast(40 * d)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).noRippleClick(close))
    // item "levantado"
    Box(Modifier.offset { IntOffset(m.bounds.left.roundToInt(), m.bounds.top.roundToInt()) }.graphicsLayer { scaleX = 1.05f; scaleY = 1.05f }) {
        ItemVisual(m.item, geo, spec, dims, badges, s, edit = false, label = false)
    }
    AnimatedVisibility(remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }, enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = 500f), initialScale = 0.6f) + fadeIn(), exit = scaleOut() + fadeOut(),
        modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }) {
        val env = LocalGlass.current
        Column(Modifier.width(250.dp).liquidGlass(corner = 26.dp, tint = panelTint(env).copy(alpha = 0.78f), strength = 0.5f).padding(vertical = 6.dp)) {
            if (isWidget) {
                val wd = m.item as HomeItem.Widget
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetSize.entries.forEach { sz ->
                        val sel = wd.size == sz
                        val pal = LocalPalette.current
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) pal.accent else pal.fill)
                            .clickable { close(); com.carlos.liquid.ui.widgets.updateWidget(wd.id) { it.copy(size = sz) } }, contentAlignment = Alignment.Center) {
                            LText(when (sz) { WidgetSize.SMALL -> "P"; WidgetSize.MEDIUM -> "M"; WidgetSize.LARGE -> "G" }, 15.sp, FontWeight.Bold, if (sel) Color.White else pal.label)
                        }
                    }
                }
            }
            entries.forEach { e -> MenuRow(e) }
        }
    }
}

@Composable
fun MenuRow(e: MenuEntry) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(48.dp).clickable { e.action() }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        LText(e.text, 17.sp, color = if (e.destructive) p.red else p.label, modifier = Modifier.weight(1f), maxLines = 1)
        when {
            e.image != null -> Image(e.image, null, Modifier.size(22.dp))
            e.icon != null -> Icon(e.icon, null, tint = if (e.destructive) p.red else p.label, modifier = Modifier.size(20.dp))
        }
    }
}

// ================================ Pasta aberta ================================

@Composable
fun BoxScope.FolderOverlay(f: FolderOpen, st: HomeState, geo: Geo, spec: IconSpec, actions: HomeActions) {
    val layout by Store.layout.collectAsState()
    val apps by AppRepo.apps.collectAsState()
    val isCategory = f.folderId.startsWith("cat:")
    val folder = if (isCategory) null else (layout.pages.flatMap { it.items } + layout.dock).firstOrNull { it.id == f.folderId } as? HomeItem.Folder
    val keys = if (isCategory) apps.filter { it.category == f.folderId.removePrefix("cat:") && it.key !in Store.s.hidden }.map { it.key } else folder?.apps.orEmpty()
    val name = if (isCategory) f.folderId.removePrefix("cat:") else folder?.name ?: ""
    if (!isCategory && folder == null) { LaunchedEffect(Unit) { st.folder = null }; return }
    val d = geo.density
    val prog = remember { Animatable(0f) }
    LaunchedEffect(f) { prog.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 380f)) }
    val close = { st.folder = null; st.renameFolder = null }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f * prog.value)).noRippleClick(close))
    val panelW = geo.w - 2 * 36 * d
    val cols = 3
    val rows = ((keys.size + cols - 1) / cols).coerceIn(1, 4)
    val cell = panelW / cols
    val panelH = rows * (geo.iconPx + 38 * d) + 40 * d
    val cx = geo.w / 2; val cy = geo.h * 0.47f
    val startScale = if (f.origin.width > 0) f.origin.width / panelW else 0.2f
    val ox = if (f.origin.width > 0) f.origin.center.x - cx else 0f
    val oy = if (f.origin.width > 0) f.origin.center.y - cy else 0f
    val p = prog.value
    Column(
        Modifier.offset { IntOffset((cx - panelW / 2).roundToInt(), (cy - panelH / 2 - 70 * d).roundToInt()) }
            .graphicsLayer {
                val sc = startScale + (1 - startScale) * p
                scaleX = sc; scaleY = sc; translationX = ox * (1 - p); translationY = oy * (1 - p); alpha = p.coerceIn(0f, 1f)
            },
    ) {
        val renaming = st.renameFolder == f.folderId || (st.edit && !isCategory)
        Box(Modifier.width((panelW / d).dp).height(64.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            if (renaming && folder != null) {
                var text by remember(folder.id) { mutableStateOf(folder.name) }
                BasicTextField(text, { t -> text = t; Store.layout { l ->
                    fun ren(list: List<HomeItem>) = list.map { if (it.id == folder.id && it is HomeItem.Folder) it.copy(name = t) else it }
                    l.copy(pages = l.pages.map { pg -> pg.copy(items = ren(pg.items)) }, dock = ren(l.dock)) } },
                    singleLine = true, cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = LocalFont.current),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp))
            } else LText(name, 30.sp, FontWeight.Bold, Color.White, shadow = true, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.width((panelW / d).dp).heightIn(max = ((panelH / d) + 140).dp)
                .liquidGlass(corner = 40.dp, bezel = 26.dp, strength = 0.8f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                keys.chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { k ->
                            val id = "f:${f.folderId}:$k"
                            Box(Modifier.width((cell / d).dp), contentAlignment = Alignment.TopCenter) {
                                Box(Modifier.itemGestures(id, { false },
                                    onTap = { close(); actions.launch(k, id) },
                                    onLongPress = { if (!isCategory) { st.menu = MenuInfo(HomeItem.App(id = id, key = k), st.bounds[id] ?: Rect.Zero, false, folderId = f.folderId) } },
                                    onDragStart = {}, rootOffset = { st.bounds[id]?.topLeft ?: androidx.compose.ui.geometry.Offset.Zero })
                                    .then(Modifier.onBounds(st, id))) {
                                    AppIcon(k, geo.iconDp, spec, AppRepo.byKey[k]?.label ?: "", 0, false, 0, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.onBounds(st: HomeState, id: String): Modifier = this.onGloballyPositioned { st.bounds[id] = it.boundsInRoot() }

// ================================ Páginas ================================

@Composable
fun PagesContent() {
    val layout by Store.layout.collectAsState()
    val p = LocalPalette.current
    SheetTitle("Páginas do ecrã principal")
    LText("Oculta páginas sem as apagar, ou muda a ordem.", 14.sp, color = p.secondary, modifier = Modifier.padding(bottom = 12.dp))
    layout.pages.forEachIndexed { i, page ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(p.cell2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LText("Página ${i + 1}", 17.sp, FontWeight.SemiBold)
                LText("${page.items.size} itens" + if (page.hidden) " · oculta" else "", 13.sp, color = p.secondary)
            }
            IconBtn(Icons.Rounded.ArrowUpward, i > 0) { Store.layout { l -> l.copy(pages = l.pages.toMutableList().apply { add(i - 1, removeAt(i)) }) } }
            IconBtn(Icons.Rounded.ArrowDownward, i < layout.pages.size - 1) { Store.layout { l -> l.copy(pages = l.pages.toMutableList().apply { add(i + 1, removeAt(i)) }) } }
            IconBtn(if (page.hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, true) {
                Store.layout { l -> l.copy(pages = l.pages.map { if (it.id == page.id) it.copy(hidden = !it.hidden) else it }) }
            }
            IconBtn(Icons.Rounded.Delete, layout.pages.size > 1, p.red) {
                Store.layout { l ->
                    val keys = page.items
                    val rest = l.pages.filterNot { it.id == page.id }
                    // apps da página apagada vão para a Biblioteca; widgets Android são libertados
                    keys.filterIsInstance<HomeItem.Widget>().filter { it.kind == WidgetKind.ANDROID }.forEach { WidgetHost.delete(it.androidId) }
                    l.copy(pages = rest)
                }
            }
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, enabled: Boolean, color: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.size(38.dp).clip(CircleShape).let { if (enabled) it.clickable(onClick = onClick) else it }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = (color ?: p.accent).copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(21.dp))
    }
}

