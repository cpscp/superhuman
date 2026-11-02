package com.carlos.liquid.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.carlos.liquid.data.GridMath
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.HomeLayout
import com.carlos.liquid.data.Page
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.data.newId

/** Geometria do ecrã principal (em px). */
data class Geo(
    val w: Float, val h: Float,
    val hpad: Float, val gridTop: Float, val cellW: Float, val cellH: Float,
    val cols: Int, val rows: Int,
    val iconPx: Float, val iconDp: Dp,
    val dock: Rect, val pill: Rect,
    val density: Float,
) {
    fun cellOrigin(col: Int, row: Int) = Offset(hpad + col * cellW, gridTop + row * cellH)
    fun widgetSizePx(size: WidgetSize, cols: Int = this.cols): Pair<Float, Float> {
        val gap = 12f * density
        val small = minOf(2 * cellW - gap * 1.5f, 2 * cellH - 26f * density)
        val wide = when (size) { WidgetSize.SMALL -> small; else -> minOf(4, cols) * cellW - gap * 1.5f }
        val tall = when (size) { WidgetSize.LARGE -> 4 * cellH - 26f * density; else -> small }
        return wide to tall
    }
}

sealed class DragSource { data class FromPage(val pageId: String) : DragSource(); object FromDock : DragSource() }
sealed class DropTarget {
    data class ToPage(val pageId: String, val index: Int) : DropTarget()
    data class ToDock(val index: Int) : DropTarget()
    data class Merge(val itemId: String) : DropTarget()
}

class DragInfo(val item: HomeItem, val source: DragSource, val grab: Offset, val visual: IntSize) {
    var pos by mutableStateOf(Offset.Zero)
    var target: DropTarget? by mutableStateOf(null)
    var mergeCandidate: String? = null
    var mergeSince = 0L
    var edgeSince = 0L
    var base: HomeLayout = HomeLayout()
}

data class MenuInfo(val item: HomeItem, val bounds: Rect, val inDock: Boolean, val folderId: String? = null, val inToday: Boolean = false)
data class FolderOpen(val folderId: String, val origin: Rect)

sealed class Sheet {
    data class Gallery(val toToday: Boolean) : Sheet()
    object Customize : Sheet()
    object Pages : Sheet()
    data class WidgetConfig(val itemId: String) : Sheet()
    object Wallpaper : Sheet()
}

data class RemoveAsk(val item: HomeItem)

@Stable
class HomeState {
    var edit by mutableStateOf(false)
    var drag: DragInfo? by mutableStateOf(null)
    var menu: MenuInfo? by mutableStateOf(null)
    var folder: FolderOpen? by mutableStateOf(null)
    var spotlight by mutableStateOf(false)
    var librarySearch by mutableStateOf(false)
    var sheet: Sheet? by mutableStateOf(null)
    var remove: RemoveAsk? by mutableStateOf(null)
    var renameFolder: String? by mutableStateOf(null)
    val bounds = HashMap<String, Rect>()

    val overlayOpen get() = menu != null || folder != null || spotlight || sheet != null || remove != null || librarySearch

    fun closeAll(): Boolean {
        if (overlayOpen) {
            menu = null; folder = null; spotlight = false; sheet = null; remove = null; librarySearch = false; renameFolder = null
            return true
        }
        if (edit) { edit = false; return true }
        return false
    }

    // ------------------------- arrastar -------------------------

    fun startDrag(item: HomeItem, source: DragSource, fingerRoot: Offset, itemBounds: Rect) {
        menu = null
        edit = true
        val d = DragInfo(item, source, fingerRoot - itemBounds.topLeft, IntSize(itemBounds.width.toInt(), itemBounds.height.toInt()))
        d.pos = fingerRoot
        val cur = Store.l
        val orig: DropTarget = when (source) {
            is DragSource.FromPage -> DropTarget.ToPage(source.pageId, indexIn(cur, source.pageId, item).coerceAtLeast(0))
            DragSource.FromDock -> DropTarget.ToDock(cur.dock.indexOfFirst { it.id == item.id }.coerceAtLeast(0))
        }
        d.base = removeItem(cur, item.id)
        d.target = orig
        drag = d
        Store.layoutRaw { applyTarget(d.base, item, orig) }
    }

    private fun indexIn(l: HomeLayout, pageId: String, item: HomeItem) = l.pages.firstOrNull { it.id == pageId }?.items?.indexOfFirst { it.id == item.id } ?: 0

    fun removeItem(l: HomeLayout, id: String) = l.copy(
        pages = l.pages.map { p -> p.copy(items = p.items.filterNot { it.id == id }) },
        dock = l.dock.filterNot { it.id == id },
    )

    /** Atualiza o alvo enquanto o dedo se move. Devolve true se mudou. */
    fun updateTarget(geo: Geo, pageId: String?, now: Long) {
        val d = drag ?: return
        val p = d.pos
        val base = d.base
        var t: DropTarget? = null
        if (geo.dock.contains(p) && d.item !is HomeItem.Widget && (base.dock.size < geo.cols || d.source == DragSource.FromDock)) {
            val n = base.dock.size + 1
            val slot = geo.dock.width / n
            t = DropTarget.ToDock(((p.x - geo.dock.left) / slot).toInt().coerceIn(0, base.dock.size))
        } else if (pageId != null) {
            val page = base.pages.firstOrNull { it.id == pageId }
            val items = page?.items.orEmpty()
            val placed = GridMath.place(items, geo.cols, geo.rows).placed
            val col = ((p.x - geo.hpad) / geo.cellW).toInt().coerceIn(0, geo.cols - 1)
            val row = ((p.y - geo.gridTop) / geo.cellH).toInt().coerceIn(0, geo.rows - 1)
            val hit = placed.firstOrNull { col >= it.col && col < it.col + it.w && row >= it.row && row < it.row + it.h }
            if (hit != null) {
                val canMerge = d.item is HomeItem.App && (hit.item is HomeItem.App || hit.item is HomeItem.Folder)
                val c = geo.cellOrigin(hit.col, hit.row) + Offset(geo.cellW / 2, geo.iconPx / 2 + 6 * geo.density)
                if (canMerge && (p - c).getDistance() < geo.iconPx * 0.42f) {
                    if (d.mergeCandidate != hit.item.id) { d.mergeCandidate = hit.item.id; d.mergeSince = now }
                    t = if (now - d.mergeSince > 380) DropTarget.Merge(hit.item.id) else d.target
                } else {
                    d.mergeCandidate = null
                    t = DropTarget.ToPage(pageId, items.indexOf(hit.item))
                }
            } else {
                d.mergeCandidate = null
                t = DropTarget.ToPage(pageId, placed.count { it.row * geo.cols + it.col < row * geo.cols + col })
            }
        }
        if (t != d.target) {
            d.target = t
            Store.layoutRaw { applyTarget(d.base, d.item, t) }
        }
    }

    private fun applyTarget(base: HomeLayout, item: HomeItem, t: DropTarget?): HomeLayout = when (t) {
        is DropTarget.ToPage -> base.copy(pages = base.pages.map { p ->
            if (p.id == t.pageId) p.copy(items = p.items.toMutableList().apply { add(t.index.coerceIn(0, size), item) }) else p
        })
        is DropTarget.ToDock -> base.copy(dock = base.dock.toMutableList().apply { add(t.index.coerceIn(0, size), item) })
        else -> base
    }

    fun drop(fallbackPageId: String?) {
        val d = drag ?: return
        drag = null
        val t = d.target
        when {
            t is DropTarget.Merge -> {
                val app = d.item as HomeItem.App
                Store.layout { _ ->
                    val b = d.base
                    fun merge(list: List<HomeItem>) = list.map { i ->
                        when {
                            i.id != t.itemId -> i
                            i is HomeItem.Folder -> i.copy(apps = i.apps + app.key)
                            i is HomeItem.App -> HomeItem.Folder(newId(), suggestFolderName(i.key, app.key), listOf(i.key, app.key))
                            else -> i
                        }
                    }
                    b.copy(pages = b.pages.map { it.copy(items = merge(it.items)) }, dock = merge(b.dock))
                }
            }
            t == null -> {
                // sem alvo: volta para o fim da página atual (ou primeira)
                Store.layout { _ ->
                    val b = d.base
                    val pid = fallbackPageId ?: b.pages.firstOrNull()?.id
                    if (pid == null) b.copy(pages = listOf(Page(items = listOf(d.item))))
                    else b.copy(pages = b.pages.map { if (it.id == pid) it.copy(items = it.items + d.item) else it })
                }
            }
            else -> Store.commitLayout()
        }
    }

    fun addPageForDrag(): String {
        val d = drag ?: return ""
        val p = Page()
        d.base = d.base.copy(pages = d.base.pages + p)
        Store.layoutRaw { it.copy(pages = it.pages + p) }
        return p.id
    }

    companion object {
        fun suggestFolderName(a: String, b: String): String {
            val ca = com.carlos.liquid.data.AppRepo.byKey[a]?.category
            val cb = com.carlos.liquid.data.AppRepo.byKey[b]?.category
            return when {
                ca != null && ca == cb -> ca.substringBefore(" e ")
                else -> "Pasta"
            }
        }
    }
}
