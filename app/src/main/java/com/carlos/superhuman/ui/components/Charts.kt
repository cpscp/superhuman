package com.carlos.superhuman.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import kotlin.math.max

/** Barras "tracejadas" com uma barra destacada a preto e linha laranja opcional (estilo do vídeo). */
@Composable
fun HatchBarChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    highlight: Int = values.indices.maxByOrNull { values[it] } ?: -1,
    labels: List<String> = emptyList(),
    topLabels: List<String> = emptyList(),
    overlay: List<Double>? = null,
    height: Dp = 130.dp,
) {
    val c = SH.c
    Column(modifier) {
        if (topLabels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                topLabels.forEachIndexed { i, t ->
                    Text(t, style = Type.tiny, color = if (i == highlight) c.ink else c.ink2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val maxV = max(values.maxOrNull() ?: 1.0, overlay?.maxOrNull() ?: 0.0).coerceAtLeast(0.0001)
            val slot = size.width / values.size
            val barW = slot * 0.62f
            values.forEachIndexed { i, v ->
                val h = (v / maxV).toFloat() * size.height
                val left = i * slot + (slot - barW) / 2
                val top = size.height - h.coerceAtLeast(4f)
                val rr = RoundRect(Rect(left, top, left + barW, size.height), CornerRadius(10f, 10f))
                if (i == highlight) {
                    drawPath(Path().apply { addRoundRect(rr) }, c.ink)
                } else {
                    val p = Path().apply { addRoundRect(rr) }
                    drawPath(p, c.inset)
                    clipPath(p) { hatch(Rect(left, top, left + barW, size.height), c.hatch) }
                }
            }
            if (overlay != null && overlay.size == values.size) {
                val path = Path()
                overlay.forEachIndexed { i, v ->
                    val x = i * slot + slot / 2
                    val y = size.height - (v / maxV).toFloat() * size.height
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, c.accent, style = Stroke(width = 4f, cap = StrokeCap.Round))
                overlay.forEachIndexed { i, v ->
                    if (i == 0 || i == overlay.lastIndex) {
                        drawCircle(c.accent, 9f, Offset(i * slot + slot / 2, size.height - (v / maxV).toFloat() * size.height))
                    }
                }
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                labels.forEach { Text(it, style = Type.tiny, color = c.ink3, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
            }
        }
    }
}

private fun DrawScope.hatch(r: Rect, color: Color) {
    val step = 14f
    var x = r.left - r.height
    while (x < r.right) {
        drawLine(color, Offset(x, r.bottom), Offset(x + r.height, r.top), strokeWidth = 3f)
        x += step
    }
}

/** Heatmap tipo "See your performance": colunas = semanas, linhas = dias. Valores -1..4. */
@Composable
fun Heatmap(cells: List<Int>, modifier: Modifier = Modifier, rows: Int = 7, height: Dp = 92.dp) {
    val c = SH.c
    Canvas(modifier.height(height)) {
        val cols = (cells.size + rows - 1) / rows
        if (cols == 0) return@Canvas
        val gap = 4f
        val cell = minOf((size.width - gap * (cols - 1)) / cols, (size.height - gap * (rows - 1)) / rows)
        cells.forEachIndexed { i, v ->
            val col = i / rows
            val row = i % rows
            val color = when (v) {
                -1 -> Color.Transparent
                0 -> c.inset
                1 -> c.ink.copy(alpha = 0.25f)
                2 -> c.ink.copy(alpha = 0.5f)
                3 -> c.ink.copy(alpha = 0.75f)
                else -> c.ink
            }
            drawRoundRect(color, Offset(col * (cell + gap), row * (cell + gap)), Size(cell, cell), CornerRadius(3f, 3f))
        }
    }
}

/** Linha simples com área suave e linha de referência opcional (ex.: ritmo ideal do plafond). */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier, reference: List<Double>? = null, color: Color = SH.c.ink, height: Dp = 140.dp, fill: Boolean = true) {
    val c = SH.c
    Canvas(modifier.fillMaxWidth().height(height)) {
        val all = values + (reference ?: emptyList())
        if (values.size < 2) return@Canvas
        val maxV = (all.maxOrNull() ?: 1.0)
        val minV = (all.minOrNull() ?: 0.0).coerceAtMost(maxV - 0.0001)
        fun y(v: Double) = size.height - ((v - minV) / (maxV - minV)).toFloat() * (size.height - 8f) - 4f
        val n = max(values.size, reference?.size ?: 0)
        fun x(i: Int) = i * size.width / (n - 1).coerceAtLeast(1)
        reference?.let { ref ->
            val p = Path()
            ref.forEachIndexed { i, v -> if (i == 0) p.moveTo(x(i), y(v)) else p.lineTo(x(i), y(v)) }
            drawPath(p, c.ink3, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
        }
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        if (fill) {
            val area = Path().apply {
                addPath(path)
                lineTo(x(values.lastIndex), size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, color.copy(alpha = 0.08f))
        }
        drawPath(path, color, style = Stroke(5f, cap = StrokeCap.Round))
        drawCircle(color, 8f, Offset(x(values.lastIndex), y(values.last())))
    }
}

@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier, color: Color = SH.c.ink) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val maxV = values.max()
        val minV = values.min().coerceAtMost(maxV - 0.0001)
        val p = Path()
        values.forEachIndexed { i, v ->
            val x = i * size.width / (values.size - 1)
            val y = size.height - ((v - minV) / (maxV - minV)).toFloat() * size.height
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, color, style = Stroke(3.5f, cap = StrokeCap.Round))
    }
}

/** Barras horizontais de categorias. */
@Composable
fun CategoryBars(items: List<Pair<String, Double>>, total: Double, modifier: Modifier = Modifier) {
    val c = SH.c
    val palette = listOf(c.ink, c.accent, c.ink2, c.warn, c.good, c.ink3)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.take(6).forEachIndexed { i, (name, v) ->
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Text(name, style = Type.small, color = c.ink, modifier = Modifier.weight(1f))
                    Text(com.carlos.superhuman.data.euro(v), style = Type.small, color = c.ink2)
                }
                Spacer(Modifier.height(5.dp))
                ProgressLine((v / total.coerceAtLeast(0.01)).toFloat(), color = palette[i % palette.size], height = 6.dp)
            }
        }
    }
}

/** Anel de progresso. */
@Composable
fun Ring(progress: Float, modifier: Modifier = Modifier, color: Color = SH.c.ink, stroke: Float = 14f) {
    val c = SH.c
    Canvas(modifier.padding(4.dp)) {
        drawArc(c.inset, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
