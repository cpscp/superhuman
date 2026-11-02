package com.carlos.liquid.ui.home

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.GridMath
import com.carlos.liquid.data.Store
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.ui.GlassPill
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.noRippleClick
import com.carlos.liquid.ui.widgets.PT
import com.carlos.liquid.ui.widgets.WidgetDims
import com.carlos.liquid.ui.widgets.WidgetView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Vista Hoje (página à esquerda), só com widgets. */
@Composable
fun TodayView(st: HomeState, geo: Geo, dims: WidgetDims, actions: HomeActions) {
    val layout by Store.layout.collectAsState()
    val d = geo.density
    val cols = 4
    val placed = GridMath.place(layout.today, cols, 400).placed
    val cellW = (geo.w - 2 * geo.hpad) / cols
    val rowH = cellW * 0.92f
    val maxRow = (placed.maxOfOrNull { it.row + it.h } ?: 0)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(44.dp).liquidGlass(corner = 22.dp, bezel = 12.dp)
                .noRippleClick { st.spotlight = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp)); LText("Pesquisar", 17.sp, color = Color.White.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(16.dp))
        val today = LocalDate.now()
        LText(today.format(DateTimeFormatter.ofPattern("EEEE", PT)).replaceFirstChar { it.uppercase() }, 17.sp, FontWeight.SemiBold, Color.White, Modifier.padding(horizontal = 22.dp), shadow = true)
        LText(today.format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT)), 34.sp, FontWeight.Bold, Color.White, Modifier.padding(horizontal = 22.dp), shadow = true)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(((maxRow * rowH) / d).dp)) {
            placed.filter { it.item is com.carlos.liquid.data.HomeItem.Widget }.forEach { p ->
                androidx.compose.runtime.key(p.item.id) {
                    val item = p.item as com.carlos.liquid.data.HomeItem.Widget
                    val (wpx, hpx) = when (item.size) {
                        com.carlos.liquid.data.WidgetSize.SMALL -> (2 * cellW - 14 * d) to (2 * rowH - 14 * d)
                        com.carlos.liquid.data.WidgetSize.MEDIUM -> (4 * cellW - 14 * d) to (2 * rowH - 14 * d)
                        com.carlos.liquid.data.WidgetSize.LARGE -> (4 * cellW - 14 * d) to (4 * rowH - 14 * d)
                    }
                    val x = geo.hpad + p.col * cellW + 7 * d
                    val y = p.row * rowH
                    val wdp = (wpx / d).dp; val hdp = (hpx / d).dp
                    Box(
                        Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .onBounds(st, item.id)
                            .jiggle(st.edit, item.id.hashCode())
                            .itemGestures(item.id, { false },
                                onTap = { if (st.edit) st.sheet = Sheet.WidgetConfig(item.id) },
                                onLongPress = { st.menu = MenuInfo(item, st.bounds[item.id] ?: Rect0, false, inToday = true) },
                                onDragStart = {}, rootOffset = { st.bounds[item.id]?.topLeft ?: Offset.Zero }),
                    ) {
                        Box(Modifier.requiredSize(wdp, hdp)) { WidgetView(item, wdp, hdp) }
                        if (st.edit) MinusBadge(Modifier.align(Alignment.TopStart).offset(x = (-7).dp, y = (-7).dp)) { actions.minus(item) }
                    }
                }
            }
        }
        if (layout.today.isEmpty()) LText("Adiciona widgets à Vista Hoje.", 15.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(22.dp), shadow = true)
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            GlassPill(height = 38.dp, onClick = { st.sheet = Sheet.Gallery(true) }) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)); LText("Adicionar widget", 15.sp, FontWeight.SemiBold, Color.White)
            }
            Spacer(Modifier.width(10.dp))
            GlassPill(height = 38.dp, onClick = { st.edit = !st.edit }) { LText(if (st.edit) "OK" else "Editar", 15.sp, FontWeight.SemiBold, Color.White) }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(20.dp))
    }
}
