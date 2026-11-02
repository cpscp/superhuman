package com.carlos.liquid.ui.home

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.GridMath
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.IconMode
import com.carlos.liquid.data.IconStyle
import com.carlos.liquid.data.Page
import com.carlos.liquid.data.Settings
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.SwipeDown
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.icons.IconSpec
import com.carlos.liquid.lock.NotifService
import com.carlos.liquid.ui.GlassPill
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalPalette
import com.carlos.liquid.ui.rememberHaptic
import com.carlos.liquid.ui.widgets.LocalWStyle
import com.carlos.liquid.ui.widgets.WStyle
import com.carlos.liquid.ui.widgets.WidgetDims
import com.carlos.liquid.ui.widgets.WidgetView
import com.carlos.liquid.ui.widgets.widgetName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

sealed class PageKind { object Today : PageKind(); data class Home(val id: String) : PageKind(); object Library : PageKind() }

/** Ações que os itens podem pedir ao ecrã principal. */
class HomeActions(
    val launch: (key: String, itemId: String) -> Unit,
    val openFolder: (HomeItem.Folder) -> Unit,
    val menu: (HomeItem, Boolean) -> Unit,
    val drag: (HomeItem, DragSource, Offset) -> Unit,
    val minus: (HomeItem) -> Unit,
    val widgetTap: (HomeItem.Widget) -> Unit,
)

fun resolveDark(s: Settings, systemDark: Boolean) = when (s.iconMode) { IconMode.AUTO -> systemDark; IconMode.LIGHT -> false; IconMode.DARK -> true }

fun tintColorInt(s: Settings): Int = android.graphics.Color.HSVToColor(floatArrayOf(s.tintHue, s.tintSat, 1f))

@Composable
fun rememberIconSpec(s: Settings, uiDark: Boolean, iconPx: Int): IconSpec {
    val dark = resolveDark(s, uiDark)
    val style = if (s.iconStyle == IconStyle.DEFAULT && dark) IconStyle.DARK else s.iconStyle
    return remember(style, dark, s.tintHue, s.tintSat, iconPx) { IconSpec(style, dark, tintColorInt(s), iconPx.coerceIn(96, 256)) }
}

@Composable
fun rememberWStyle(s: Settings, uiDark: Boolean): WStyle {
    val dark = resolveDark(s, uiDark)
    return remember(s.iconStyle, dark, s.tintHue, s.tintSat) { WStyle(s.iconStyle, dark, Color(tintColorInt(s))) }
}

fun expandStatusBar(ctx: Context, settings: Boolean) {
    runCatching {
        val sb = ctx.getSystemService("statusbar")
        val c = Class.forName("android.app.StatusBarManager")
        c.getMethod(if (settings) "expandSettingsPanel" else "expandNotificationsPanel").invoke(sb)
    }
}

@Composable
fun HomeScreen(
    st: HomeState,
    homeSignal: SharedFlow<Unit>,
    onAddAndroidWidget: (AppWidgetProviderInfo, WidgetSize, Boolean) -> Unit,
    openSettings: () -> Unit,
) {
    val s by Store.settings.collectAsState()
    val layout by Store.layout.collectAsState()
    val apps by AppRepo.apps.collectAsState()
    val badges by NotifService.badges.collectAsState()
    val ctx = LocalContext.current
    val view = LocalView.current
    val pal = LocalPalette.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    @Suppress("UNUSED_VARIABLE") val appsVersion = apps.size

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val d = density.density
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val top = WindowInsets.statusBars.getTop(density).toFloat()
        val bottom = WindowInsets.navigationBars.getBottom(density).toFloat()
        val geo = remember(w, h, top, bottom, s.columns, s.rows, s.dock, s.largeIcons) {
            val cols = s.columns; val rows = s.rows
            val hpad = (if (cols >= 5) 8f else 14f) * d
            val dockH = if (s.dock) 94f * d else 0f
            val dockBottom = h - bottom - 8f * d
            val dock = Rect(10f * d, dockBottom - dockH, w - 10f * d, dockBottom)
            val pillH = 34f * d
            val pillTop = if (s.dock) dock.top - 12f * d - pillH else dockBottom - pillH
            val gridTop = top + 16f * d
            val cellW = (w - 2 * hpad) / cols
            val cellH = (pillTop - 10f * d - gridTop) / rows
            val iconPx = minOf(cellW * (if (s.largeIcons) 0.78f else 0.64f), cellH * (if (s.largeIcons) 0.8f else 0.62f), (if (s.largeIcons) 74f else 62f) * d)
            Geo(w, h, hpad, gridTop, cellW, cellH, cols, rows, iconPx, (iconPx / d).dp, dock, Rect(w / 2 - 70 * d, pillTop, w / 2 + 70 * d, pillTop + pillH), d)
        }
        val spec = rememberIconSpec(s, pal.dark, geo.iconPx.roundToInt())
        val wstyle = rememberWStyle(s, pal.dark)
        val dims = remember(geo) {
            val (sw, sh) = geo.widgetSizePx(WidgetSize.SMALL); val (mw, _) = geo.widgetSizePx(WidgetSize.MEDIUM); val (_, lh) = geo.widgetSizePx(WidgetSize.LARGE)
            WidgetDims((sw / d).dp, (sh / d).dp, (mw / d).dp, (lh / d).dp)
        }

        val visible = layout.pages.filter { !it.hidden }.ifEmpty { layout.pages }
        val kinds: List<PageKind> = buildList {
            if (s.todayView) add(PageKind.Today)
            visible.forEach { add(PageKind.Home(it.id)) }
            if (s.appLibrary) add(PageKind.Library)
        }
        val firstHome = if (s.todayView) 1 else 0
        val lastHome = firstHome + visible.size - 1
        val pager = rememberPagerState(initialPage = firstHome) { kinds.size }

        fun currentPageId(): String? = (kinds.getOrNull(pager.currentPage) as? PageKind.Home)?.id

        val actions = remember(st) {
            HomeActions(
                launch = { key, id ->
                    val b = st.bounds[id]
                    AppRepo.launch(ctx, key, view, b?.let { android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) })
                },
                openFolder = { f -> st.folder = FolderOpen(f.id, st.bounds[f.id] ?: Rect.Zero) },
                menu = { item, inDock -> haptic(true); st.menu = MenuInfo(item, st.bounds[item.id] ?: Rect.Zero, inDock) },
                drag = { item, src, finger ->
                    haptic(true)
                    st.startDrag(item, src, finger, st.bounds[item.id] ?: Rect(finger, 1f))
                },
                minus = { item -> st.remove = RemoveAsk(item) },
                widgetTap = { wd -> if (st.edit) st.sheet = Sheet.WidgetConfig(wd.id) },
            )
        }

        // Botão Home: fecha camadas; senão volta à primeira página
        LaunchedEffect(Unit) {
            homeSignal.collect {
                if (!st.closeAll()) pager.animateScrollToPage(firstHome)
            }
        }

        // Ciclo do arrastar: temporizadores para pastas e mudança de página nos bordos
        LaunchedEffect(st.drag) {
            while (st.drag != null) {
                delay(90)
                val dr = st.drag ?: break
                val now = System.currentTimeMillis()
                st.updateTarget(geo, currentPageId(), now)
                val edge = 24f * d
                val dir = when { dr.pos.x < edge -> -1; dr.pos.x > w - edge -> 1; else -> 0 }
                if (dir == 0 || geo.dock.contains(dr.pos)) { dr.edgeSince = 0L; continue }
                if (dr.edgeSince == 0L) { dr.edgeSince = now; continue }
                if (now - dr.edgeSince > 650) {
                    dr.edgeSince = now
                    val target = pager.currentPage + dir
                    if (target in firstHome..lastHome) pager.animateScrollToPage(target)
                    else if (dir > 0 && target == lastHome + 1) {
                        val cur = currentPageId()
                        val onlyDragged = layout.pages.firstOrNull { it.id == cur }?.items?.all { it.id == dr.item.id } ?: true
                        if (!onlyDragged) { st.addPageForDrag(); delay(60); pager.animateScrollToPage(target) }
                    }
                }
            }
        }

        val blur = st.menu != null || st.folder != null || st.spotlight || st.librarySearch
        val blurPx by animateFloatAsState(if (blur) 28f else 0f, label = "blur")

        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val dr = st.drag ?: continue
                            val ch = ev.changes.firstOrNull() ?: continue
                            if (ch.pressed) {
                                dr.pos = ch.position
                                ch.consume()
                                st.updateTarget(geo, currentPageId(), System.currentTimeMillis())
                            } else {
                                ch.consume()
                                st.drop(currentPageId())
                            }
                        }
                    }
                },
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalWStyle provides wstyle) {
                Box(Modifier.fillMaxSize().then(if (blurPx > 0.5f && Build.VERSION.SDK_INT >= 31) Modifier.blur(blurPx.dp) else Modifier)) {
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = st.drag == null,
                        beyondViewportPageCount = 1,
                        key = { i -> when (val k = kinds.getOrNull(i)) { is PageKind.Home -> k.id; PageKind.Today -> "today"; PageKind.Library -> "library"; null -> i } },
                    ) { i ->
                        when (val k = kinds[i]) {
                            PageKind.Today -> TodayView(st, geo, dims, actions)
                            PageKind.Library -> AppLibrary(st, geo, spec, actions)
                            is PageKind.Home -> {
                                val page = layout.pages.firstOrNull { it.id == k.id }
                                if (page != null) HomePage(page, st, geo, spec, s, badges, dims, actions,
                                    onSwipeDown = { x, y ->
                                        when {
                                            s.topEdges && y < top + 48 * d && x < w / 2 -> expandStatusBar(ctx, false)
                                            s.topEdges && y < top + 48 * d -> expandStatusBar(ctx, true)
                                            s.swipeDown == SwipeDown.NOTIFICATIONS -> expandStatusBar(ctx, false)
                                            else -> st.spotlight = true
                                        }
                                    })
                            }
                        }
                    }

                    // Visibilidade da dock e da pesquisa (desaparecem na Vista Hoje e na Biblioteca)
                    val pos = pager.currentPage + pager.currentPageOffsetFraction
                    val homeAlpha = when {
                        pos < firstHome -> 1f - (firstHome - pos)
                        pos > lastHome -> 1f - (pos - lastHome)
                        else -> 1f
                    }.coerceIn(0f, 1f)

                    if (homeAlpha > 0.01f) {
                        // Pesquisa / indicador de páginas
                        Box(Modifier.fillMaxWidth().offset { IntOffset(0, geo.pill.top.roundToInt()) }.alpha(homeAlpha), contentAlignment = Alignment.TopCenter) {
                            val scrolling = pager.isScrollInProgress || !s.searchPill || st.edit
                            GlassPill(height = (geo.pill.height / d).dp, onClick = { if (st.edit) st.sheet = Sheet.Pages else st.spotlight = true }) {
                                if (scrolling && visible.size > 1) {
                                    visible.forEachIndexed { idx, _ ->
                                        val active = pager.currentPage - firstHome == idx
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (active) 1f else 0.4f)))
                                    }
                                } else {
                                    Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    LText("Pesquisar", 15.sp, FontWeight.SemiBold, Color.White)
                                }
                            }
                        }
                        if (s.dock) Dock(layout.dock, st, geo, spec, badges, actions, Modifier.alpha(homeAlpha))
                    }
                }

                // Item a ser arrastado
                st.drag?.let { dr ->
                    Box(
                        Modifier.offset { IntOffset((dr.pos.x - dr.grab.x).roundToInt(), (dr.pos.y - dr.grab.y).roundToInt()) }
                            .graphicsLayer { scaleX = 1.1f; scaleY = 1.1f; alpha = 0.95f },
                    ) { ItemVisual(dr.item, geo, spec, dims, badges, s, edit = false, label = false) }
                }

                // Barra do modo de edição
                AnimatedVisibility(st.edit && st.drag == null && !blur && st.sheet == null, enter = fadeIn(), exit = fadeOut()) {
                    EditBar(top / d, onAdd = { st.sheet = Sheet.Gallery(pager.currentPage < firstHome) }, onCustomize = { st.sheet = Sheet.Customize },
                        onPages = { st.sheet = Sheet.Pages }, onWallpaper = { st.sheet = Sheet.Wallpaper }, onSettings = openSettings, onDone = { st.edit = false })
                }

                Overlays(st, geo, spec, dims, s, badges, actions, onAddAndroidWidget = onAddAndroidWidget, firstPageId = { currentPageId() ?: visible.firstOrNull()?.id })
            }
        }
    }
}

@Composable
fun HomePage(
    page: Page, st: HomeState, geo: Geo, spec: IconSpec, s: Settings, badges: Map<String, Int>, dims: WidgetDims, actions: HomeActions,
    onSwipeDown: (Float, Float) -> Unit,
) {
    val d = geo.density
    val placed = remember(page.items, geo.cols, geo.rows) { GridMath.place(page.items, geo.cols, geo.rows).placed }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { st.edit = true }, onTap = { if (st.edit) st.edit = false })
            }
            .pointerInput(Unit) {
                var sx = 0f; var sy = 0f; var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { sx = it.x; sy = it.y; total = 0f },
                    onDragEnd = { if (total > 70f * d && !st.edit) onSwipeDown(sx, sy) },
                ) { _, dy -> total += dy }
            },
    ) {
        placed.forEach { p ->
            androidx.compose.runtime.key(p.item.id) {
                val o = geo.cellOrigin(p.col, p.row)
                val target = IntOffset(o.x.roundToInt(), o.y.roundToInt())
                val anim by animateIntOffsetAsState(target, spring(dampingRatio = 0.8f, stiffness = 420f), label = "pos")
                val cw = (geo.cellW * p.w / d).dp
                val ch = (geo.cellH * p.h / d).dp
                Box(Modifier.offset { anim }.size(cw, ch), contentAlignment = Alignment.TopCenter) {
                    ItemHost(p.item, DragSource.FromPage(page.id), false, st, geo, spec, s, badges, dims, actions)
                }
            }
        }
    }
}

@Composable
fun Dock(items: List<HomeItem>, st: HomeState, geo: Geo, spec: IconSpec, badges: Map<String, Int>, actions: HomeActions, modifier: Modifier) {
    val d = geo.density
    val r = geo.dock
    Box(modifier.offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }.size((r.width / d).dp, (r.height / d).dp)) {
        Box(Modifier.fillMaxSize().liquidGlass(corner = 36.dp, bezel = 22.dp))
        val slot = r.width / geo.cols
        val n = items.size
        val start = (r.width - n * slot) / 2
        items.forEachIndexed { i, item ->
            androidx.compose.runtime.key(item.id) {
                val x = start + i * slot + (slot - geo.iconPx) / 2
                val y = (r.height - geo.iconPx) / 2
                val anim by animateIntOffsetAsState(IntOffset(x.roundToInt(), y.roundToInt()), spring(dampingRatio = 0.8f, stiffness = 420f), label = "dock")
                Box(Modifier.offset { anim }) {
                    ItemHost(item, DragSource.FromDock, true, st, geo, spec, Store.s, badges, null, actions)
                }
            }
        }
    }
}

/** Liga um item aos gestos e desenha-o. */
@Composable
fun ItemHost(
    item: HomeItem, source: DragSource, inDock: Boolean, st: HomeState, geo: Geo, spec: IconSpec, s: Settings,
    badges: Map<String, Int>, dims: WidgetDims?, actions: HomeActions,
) {
    val dragging = st.drag?.item?.id == item.id
    val merge = (st.drag?.target as? DropTarget.Merge)?.itemId == item.id
    val labels = s.showLabels && !s.largeIcons && !inDock
    val topPad = if (item is HomeItem.Widget || inDock) 0f else ((geo.cellH - geo.iconPx - (if (labels) 20f * geo.density else 0f)) / 2f).coerceAtLeast(0f)
    Box(
        Modifier.padding(top = (topPad / geo.density).dp)
            .alpha(if (dragging) 0f else 1f)
            .onGloballyPositioned { st.bounds[item.id] = it.boundsInRoot() }
            .itemGestures(
                key = item.id,
                editMode = { st.edit },
                onTap = {
                    when (item) {
                        is HomeItem.App -> if (!st.edit) actions.launch(item.key, item.id)
                        is HomeItem.Folder -> actions.openFolder(item)
                        is HomeItem.Widget -> actions.widgetTap(item)
                    }
                },
                onLongPress = { actions.menu(item, inDock) },
                onDragStart = { f -> actions.drag(item, source, f) },
                rootOffset = { st.bounds[item.id]?.topLeft ?: Offset.Zero },
            ),
    ) {
        Box(Modifier.graphicsLayer { val sc = if (merge) 1.15f else 1f; scaleX = sc; scaleY = sc }) {
            ItemVisual(item, geo, spec, dims, badges, s, st.edit, labels, onMinus = { actions.minus(item) })
        }
    }
}

@Composable
fun ItemVisual(
    item: HomeItem, geo: Geo, spec: IconSpec, dims: WidgetDims?, badges: Map<String, Int>, s: Settings,
    edit: Boolean, label: Boolean, onMinus: (() -> Unit)? = null,
) {
    val seed = rememberSeed(item.id)
    when (item) {
        is HomeItem.App -> {
            val e = AppRepo.byKey[item.key]
            val pkg = e?.pkg
            AppIcon(item.key, geo.iconDp, spec, if (label) e?.label ?: "" else null,
                if (s.badges && pkg != null) badges[pkg] ?: 0 else 0, edit, seed, onMinus)
        }
        is HomeItem.Folder -> {
            val b = if (s.badges) item.apps.sumOf { k -> AppRepo.byKey[k]?.pkg?.let { badges[it] } ?: 0 } else 0
            FolderIcon(item.apps, if (label) item.name else null, geo.iconDp, spec, b, edit, seed, false, onMinus)
        }
        is HomeItem.Widget -> {
            val (wpx, hpx) = geo.widgetSizePx(item.size)
            val wdp = (wpx / geo.density).dp; val hdp = (hpx / geo.density).dp
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                Box(Modifier.jiggle(edit, seed)) {
                    Box(Modifier.requiredSize(wdp, hdp)) { WidgetView(item, wdp, hdp) }
                    if (edit && onMinus != null) MinusBadge(Modifier.align(Alignment.TopStart).offset(x = (-7).dp, y = (-7).dp), onMinus)
                }
                if (s.showLabels) {
                    Spacer(Modifier.height(5.dp))
                    LText(widgetName(item.kind), 12.sp, FontWeight.Medium, Color.White, shadow = true, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun EditBar(topDp: Float, onAdd: () -> Unit, onCustomize: () -> Unit, onPages: () -> Unit, onWallpaper: () -> Unit, onSettings: () -> Unit, onDone: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = (topDp + 4).dp, start = 14.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassPill(height = 36.dp, onClick = { menu = !menu }) { LText("Editar", 15.sp, FontWeight.SemiBold, Color.White) }
            Spacer(Modifier.width(8.dp))
            GlassPill(height = 36.dp, onClick = onAdd) { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            GlassPill(height = 36.dp, onClick = onDone) { LText("OK", 15.sp, FontWeight.Bold, Color.White) }
        }
        AnimatedVisibility(menu, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.padding(top = (topDp + 48).dp, start = 14.dp)) {
            Column(Modifier.width(250.dp).liquidGlass(corner = 26.dp, tint = Color(0xFF1C1C1E).copy(alpha = 0.55f), strength = 0.5f).padding(vertical = 6.dp)) {
                listOf(
                    "Adicionar widget" to onAdd, "Personalizar" to onCustomize, "Editar páginas" to onPages,
                    "Wallpaper" to onWallpaper, "Definições do Liquid" to onSettings,
                ).forEach { (t, f) ->
                    LText(t, 17.sp, color = Color.White, modifier = Modifier.fillMaxWidth().pointerInput(t) { detectTapGestures { menu = false; f() } }.padding(horizontal = 18.dp, vertical = 12.dp))
                }
            }
        }
    }
}

