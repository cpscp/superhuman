@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package pt.carlos.painel.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.util.SizeF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.carlos.painel.PainelApp
import pt.carlos.painel.data.AppEntry
import pt.carlos.painel.data.Item
import pt.carlos.painel.widgets.PainelHostView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val OnWallpaper = Color.White
private val WallpaperShadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1.5f), blurRadius = 6f)

@Composable
fun HomeScreen(
    app: PainelApp,
    homeTick: Int,
    showSetDefault: Boolean,
    onSetDefault: () -> Unit,
    onPickWidget: (AppWidgetProviderInfo, Int) -> Unit,
    onCellSize: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val prefs = app.prefs
    val layout = app.layout
    val repo = app.apps
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var drawerOpen by remember { mutableStateOf(false) }
    var drawerSearch by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var homeMenu by remember { mutableStateOf(false) }
    val pager = rememberPagerState { layout.pages }

    // Botão Home: fecha tudo e volta à primeira página.
    LaunchedEffect(homeTick) {
        if (homeTick > 0) {
            drawerOpen = false; pickerOpen = false; homeMenu = false
            if (editing) { editing = false; layout.trimPages() }
            pager.animateScrollToPage(0)
        }
    }

    // Primeira vez: dock com as tuas apps principais (se estiverem instaladas).
    LaunchedEffect(repo.loaded) {
        if (repo.loaded && !prefs.dockSeeded) {
            val wanted = listOf(
                listOf("Calendário", "Calendar"), listOf("Superhuman"), listOf("Partidas"),
                listOf("PressReader"), listOf("YouTube"), listOf("Nuvio"), listOf("BitChord"),
            )
            for (names in wanted) {
                val e = repo.apps.firstOrNull { a -> names.any { a.label.equals(it, ignoreCase = true) } }
                    ?: repo.apps.firstOrNull { a -> names.any { a.label.contains(it, ignoreCase = true) } }
                if (e != null) layout.dockAdd(e.key)
            }
            prefs.dockSeeded = true
        }
    }

    BackHandler {
        when {
            pickerOpen -> pickerOpen = false
            drawerOpen -> drawerOpen = false
            editing -> { editing = false; layout.trimPages() }
        }
    }

    fun openDrawer(search: Boolean) { drawerSearch = search; drawerOpen = true }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .pointerInput(editing) {
                    if (!editing) {
                        var total = 0f
                        detectVerticalDragGestures(onDragStart = { total = 0f }) { _, d ->
                            total += d
                            if (total < -120f) { total = 0f; openDrawer(false) }
                        }
                    }
                }
        ) {
            when {
                editing -> EditBar(
                    onWidgets = { pickerOpen = true },
                    onNewPage = { val p = layout.addPage(); scope.launch { pager.animateScrollToPage(p) } },
                    onSettings = onOpenSettings,
                    onDone = { editing = false; layout.trimPages() },
                )
                prefs.showGlance -> Glance()
                else -> Spacer(Modifier.height(16.dp))
            }
            if (showSetDefault && !editing) SetDefaultCard(onSetDefault)

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                key = { it },
            ) { page ->
                PageGrid(
                    page = page,
                    app = app,
                    editing = editing,
                    onEnterEdit = { editing = true },
                    onLongPressEmpty = { homeMenu = true },
                    onCellSize = onCellSize,
                )
            }

            PageDots(count = layout.pages, current = pager.currentPage)
            Spacer(Modifier.height(8.dp))
            SearchPill(onClick = { openDrawer(true) })
            Dock(app = app, onOpenDrawer = { openDrawer(false) })
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically { it / 4 } + fadeIn(),
            exit = slideOutVertically { it / 4 } + fadeOut(),
        ) {
            AppDrawer(
                app = app,
                focusSearch = drawerSearch,
                onClose = { drawerOpen = false },
                onAddToHome = { e ->
                    layout.add(Item.APP, e.key, -1, 1, 1, pager.currentPage, prefs.columns, prefs.rows)
                    drawerOpen = false
                },
            )
        }

        if (pickerOpen) {
            WidgetPicker(
                onDismiss = { pickerOpen = false },
                onPick = { info -> pickerOpen = false; onPickWidget(info, pager.currentPage) },
            )
        }

        if (homeMenu) {
            ModalBottomSheet(onDismissRequest = { homeMenu = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ListItem(
                    headlineContent = { Text("Widgets") },
                    leadingContent = { Icon(Icons.Outlined.Widgets, null) },
                    colors = rowColors,
                    modifier = Modifier.clickable { homeMenu = false; pickerOpen = true },
                )
                ListItem(
                    headlineContent = { Text("Editar ecrã inicial") },
                    leadingContent = { Icon(Icons.Outlined.Edit, null) },
                    colors = rowColors,
                    modifier = Modifier.clickable { homeMenu = false; editing = true },
                )
                ListItem(
                    headlineContent = { Text("Papel de parede") },
                    leadingContent = { Icon(Icons.Outlined.Wallpaper, null) },
                    colors = rowColors,
                    modifier = Modifier.clickable {
                        homeMenu = false
                        runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Papel de parede")) }
                    },
                )
                ListItem(
                    headlineContent = { Text("Definições do Painel") },
                    leadingContent = { Icon(Icons.Outlined.Settings, null) },
                    colors = rowColors,
                    modifier = Modifier.clickable { homeMenu = false; onOpenSettings() },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}


// ---------- Cabeçalho ----------

@Composable
private fun Glance() {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); today = LocalDate.now() }
    }
    val text = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "PT")))
            .replaceFirstChar { it.titlecase(Locale("pt", "PT")) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(shadow = WallpaperShadow),
        color = OnWallpaper,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 12.dp),
    )
}

@Composable
private fun SetDefaultCard(onSetDefault: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                "O Painel ainda não é o ecrã inicial do tablet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSetDefault) { Text("Definir") }
        }
    }
}

@Composable
private fun EditBar(onWidgets: () -> Unit, onNewPage: () -> Unit, onSettings: () -> Unit, onDone: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Editar ecrã inicial", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onWidgets) { Text("Widgets") }
            TextButton(onClick = onNewPage) { Text("Nova página") }
            TextButton(onClick = onSettings) { Text("Definições") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onDone) { Text("Concluído") }
        }
    }
}

// ---------- Grelha ----------

@Composable
private fun PageGrid(
    page: Int,
    app: PainelApp,
    editing: Boolean,
    onEnterEdit: () -> Unit,
    onLongPressEmpty: () -> Unit,
    onCellSize: (Float, Float) -> Unit,
) {
    val cols = app.prefs.columns
    val rows = app.prefs.rows
    val outline = MaterialTheme.colorScheme.outlineVariant
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(editing) { detectTapGestures(onLongPress = { if (!editing) onLongPressEmpty() }) }
    ) {
        val cw = maxWidth / cols
        val ch = maxHeight / rows
        LaunchedEffect(cw, ch) { onCellSize(cw.value, ch.value) }

        if (editing) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width / cols
                val h = size.height / rows
                for (x in 0 until cols) for (y in 0 until rows) {
                    drawRoundRect(
                        color = outline.copy(alpha = 0.5f),
                        topLeft = Offset(x * w + 6f, y * h + 6f),
                        size = Size(w - 12f, h - 12f),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 2f),
                    )
                }
            }
        }

        val pageItems = app.layout.items.filter { it.page == page }
        for (item in pageItems) {
            key(item.id) {
                GridItem(item = item, cw = cw, ch = ch, cols = cols, rows = rows, editing = editing, app = app, onEnterEdit = onEnterEdit)
            }
        }
    }
}

@Composable
private fun GridItem(
    item: Item,
    cw: Dp,
    ch: Dp,
    cols: Int,
    rows: Int,
    editing: Boolean,
    app: PainelApp,
    onEnterEdit: () -> Unit,
) {
    val density = LocalDensity.current
    val cwPx = with(density) { cw.toPx() }
    val chPx = with(density) { ch.toPx() }
    var drag by remember(item.id) { mutableStateOf(Offset.Zero) }
    var resize by remember(item.id) { mutableStateOf(Offset.Zero) }
    val w = item.w.coerceIn(1, cols)
    val h = item.h.coerceIn(1, rows)
    val x = item.x.coerceIn(0, cols - w)
    val y = item.y.coerceIn(0, rows - h)
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Box(
        Modifier
            .offset { IntOffset((x * cwPx + drag.x).roundToInt(), (y * chPx + drag.y).roundToInt()) }
            .size(
                width = cw * w + with(density) { resize.x.toDp() },
                height = ch * h + with(density) { resize.y.toDp() },
            )
            .zIndex(if (drag != Offset.Zero || resize != Offset.Zero) 1f else 0f)
            .then(
                if (editing) Modifier.pointerInput(item, cwPx, chPx) {
                    detectDragGestures(
                        onDragEnd = {
                            val nx = ((x * cwPx + drag.x) / cwPx).roundToInt()
                            val ny = ((y * chPx + drag.y) / chPx).roundToInt()
                            if (app.layout.isFree(item.page, nx, ny, w, h, cols, rows, item.id)) {
                                app.layout.update(item.copy(x = nx, y = ny))
                            }
                            drag = Offset.Zero
                        },
                        onDragCancel = { drag = Offset.Zero },
                    ) { change, amount -> change.consume(); drag += amount }
                } else Modifier
            )
    ) {
        when (item.kind) {
            Item.APP -> {
                val entry = app.apps.byKey[item.app]
                if (entry != null) {
                    HomeAppIcon(entry = entry, app = app, item = item, onEnterEdit = onEnterEdit)
                }
            }
            else -> WidgetView(
                widgetId = item.widgetId,
                wDp = (cw * w).value,
                hDp = (ch * h).value,
                onLongPress = onEnterEdit,
            )
        }

        if (editing) {
            // Camada por cima: no modo de edição os toques movem o elemento em vez de o usar.
            Box(
                Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .background(Color.White.copy(alpha = 0.08f), shape)
                    .border(2.dp, primary, shape)
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            )
            Surface(
                onClick = {
                    if (item.kind == Item.WIDGET) runCatching { app.widgetHost.deleteAppWidgetId(item.widgetId) }
                    app.layout.remove(item.id)
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.align(Alignment.TopStart).size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, "Remover", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            if (item.kind == Item.WIDGET) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .background(primary, CircleShape)
                        .pointerInput(item, cwPx, chPx) {
                            detectDragGestures(
                                onDragEnd = {
                                    val nw = ((w * cwPx + resize.x) / cwPx).roundToInt().coerceAtLeast(1)
                                    val nh = ((h * chPx + resize.y) / chPx).roundToInt().coerceAtLeast(1)
                                    if (app.layout.isFree(item.page, x, y, nw, nh, cols, rows, item.id)) {
                                        app.layout.update(item.copy(x = x, y = y, w = nw, h = nh))
                                    }
                                    resize = Offset.Zero
                                },
                                onDragCancel = { resize = Offset.Zero },
                            ) { change, amount -> change.consume(); resize += amount }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.OpenInFull, "Redimensionar", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun HomeAppIcon(entry: AppEntry, app: PainelApp, item: Item, onEnterEdit: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppIcon(
            entry = entry,
            showLabel = app.prefs.showLabels,
            onWallpaper = true,
            modifier = Modifier.combinedClickable(
                onClick = { app.apps.launch(entry) },
                onLongClick = { menu = true },
            ),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Remover do ecrã") },
                leadingIcon = { Icon(Icons.Outlined.Close, null) },
                onClick = { menu = false; app.layout.remove(item.id) },
            )
            DropdownMenuItem(
                text = { Text("Informações da app") },
                leadingIcon = { Icon(Icons.Outlined.Info, null) },
                onClick = { menu = false; app.apps.details(entry) },
            )
            DropdownMenuItem(
                text = { Text("Editar ecrã inicial") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                onClick = { menu = false; onEnterEdit() },
            )
        }
    }
}

/** Ícone de app com nome, em Google Sans Flex. */
@Composable
fun AppIcon(entry: AppEntry, showLabel: Boolean, onWallpaper: Boolean, modifier: Modifier = Modifier, iconSize: Dp = 60.dp) {
    Column(
        modifier = modifier.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(bitmap = entry.icon, contentDescription = entry.label, modifier = Modifier.size(iconSize))
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge.copy(shadow = if (onWallpaper) WallpaperShadow else null),
                color = if (onWallpaper) OnWallpaper else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 104.dp),
            )
        }
    }
}

@Composable
private fun WidgetView(widgetId: Int, wDp: Float, hDp: Float, onLongPress: () -> Unit) {
    val app = PainelApp.instance
    val info = remember(widgetId) { AppWidgetManager.getInstance(app).getAppWidgetInfo(widgetId) }
    if (info == null) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxSize().padding(4.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Widget indisponível", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    AndroidView<AppWidgetHostView>(
        factory = { c -> app.widgetHost.createView(c, widgetId, info) },
        update = { v ->
            (v as? PainelHostView)?.onLongPress = onLongPress
            val sizeKey = "${wDp.roundToInt()}x${hDp.roundToInt()}"
            if (v.tag != sizeKey) {
                v.tag = sizeKey
                runCatching { v.updateAppWidgetSize(Bundle(), listOf(SizeF(wDp, hDp))) }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// ---------- Rodapé: pontos, pesquisa e dock ----------

@Composable
private fun PageDots(count: Int, current: Int) {
    if (count <= 1) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == current) 8.dp else 6.dp)
                    .background(OnWallpaper.copy(alpha = if (i == current) 1f else 0.5f), CircleShape)
            )
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 620.dp).fillMaxWidth().height(56.dp),
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Text("Pesquisar apps", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Dock(app: PainelApp, onOpenDrawer: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).navigationBarsPadding(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dockApps = app.layout.dock.mapNotNull { k -> app.apps.byKey[k]?.let { k to it } }
        dockApps.forEach { (dk, e) -> key(dk) {
            var menu by remember { mutableStateOf(false) }
            Box {
                AppIcon(
                    entry = e,
                    showLabel = false,
                    onWallpaper = true,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .combinedClickable(onClick = { app.apps.launch(e) }, onLongClick = { menu = true }),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Tirar da dock") }, onClick = { menu = false; app.layout.dockRemove(dk) })
                    DropdownMenuItem(text = { Text("Informações da app") }, onClick = { menu = false; app.apps.details(e) })
                }
            }
        } }
        Surface(
            onClick = onOpenDrawer,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = 16.dp).size(60.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Apps, "Todas as apps", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
