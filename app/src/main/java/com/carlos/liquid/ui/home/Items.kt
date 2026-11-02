package com.carlos.liquid.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.IconStyle
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.icons.IconCache
import com.carlos.liquid.icons.IconSpec
import com.carlos.liquid.icons.SquircleShape
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.noRippleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Gestos de um item: toque, toque longo (menu), arrastar (modo de edição). */
fun Modifier.itemGestures(
    key: Any,
    editMode: () -> Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: (Offset) -> Unit,
    rootOffset: () -> Offset,
): Modifier = this.pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val edit = editMode()
        val timeout = if (edit) 170L else viewConfiguration.longPressTimeoutMillis
        val slop = viewConfiguration.touchSlop
        var last = down.position
        val r: Int? = withTimeoutOrNull(timeout) {
            var res = 0
            while (res == 0) {
                val ev = awaitPointerEvent()
                val ch = ev.changes.firstOrNull { it.id == down.id }
                if (ch == null || !ch.pressed) { res = 1 } else {
                    last = ch.position
                    if ((ch.position - down.position).getDistance() > slop) { if (edit) ch.consume(); res = 2 }
                }
            }
            res
        }
        when (r) {
            1 -> onTap()
            2 -> if (edit) onDragStart(rootOffset() + last)
            else -> {
                if (edit) { onDragStart(rootOffset() + last); return@awaitEachGesture }
                onLongPress()
                while (true) {
                    val ev = awaitPointerEvent()
                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!ch.pressed) break
                    if ((ch.position - down.position).getDistance() > slop * 1.6f) {
                        ch.consume(); onDragStart(rootOffset() + ch.position); break
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.jiggle(active: Boolean, seed: Int): Modifier {
    if (!active) return this
    val tr = rememberInfiniteTransition(label = "jiggle")
    val dur = 120 + (seed and 0x3f)
    val a by tr.animateFloat(-1.7f, 1.7f, infiniteRepeatable(tween(dur, easing = LinearEasing), RepeatMode.Reverse), label = "rot")
    return this.graphicsLayer { rotationZ = a }
}

@Composable
fun IconImage(key: String, size: Dp, spec: IconSpec) {
    val ctx = LocalContext.current
    val bmp: ImageBitmap? by produceState(IconCache.get(key, spec), key, spec) {
        if (value == null) {
            val e = AppRepo.byKey[key]
            if (e != null) value = withContext(Dispatchers.Default) { runCatching { IconCache.render(ctx, e, spec) }.getOrNull() }
        }
    }
    Box(Modifier.size(size)) {
        if (spec.style == IconStyle.CLEAR) Box(Modifier.size(size).liquidGlass(squircle = true, bezel = size * 0.2f, strength = 0.8f))
        bmp?.let { Image(it, null, Modifier.size(size)) }
    }
}

@Composable
fun MinusBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(24.dp).liquidGlass(corner = 12.dp, tint = Color(0xFFE5E5EA).copy(alpha = 0.9f), bezel = 6.dp).noRippleClick(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Remove, null, tint = Color.Black, modifier = Modifier.size(16.dp)) }
}

@Composable
fun Badge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.height(22.dp).widthIn(min = 22.dp).clip(CircleShape).background(Color(0xFFFF3B30)).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) { LText(if (count > 99) "99+" else count.toString(), 13.sp, FontWeight.SemiBold, Color.White) }
}

@Composable
fun AppIcon(
    key: String,
    iconDp: Dp,
    spec: IconSpec,
    label: String?,
    badge: Int,
    edit: Boolean,
    seed: Int,
    onMinus: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(iconDp).jiggle(edit, seed)) {
            IconImage(key, iconDp, spec)
            if (badge > 0 && !edit) Badge(badge, Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-7).dp))
            if (edit && onMinus != null) MinusBadge(Modifier.align(Alignment.TopStart).offset(x = (-7).dp, y = (-7).dp), onMinus)
        }
        if (label != null) {
            Spacer(Modifier.height(5.dp))
            LText(label, 12.sp, FontWeight.Medium, Color.White, Modifier.width(iconDp + 22.dp), maxLines = 1, align = TextAlign.Center, shadow = true)
        }
    }
}

@Composable
fun FolderIcon(
    apps: List<String>,
    name: String?,
    iconDp: Dp,
    spec: IconSpec,
    badge: Int,
    edit: Boolean,
    seed: Int,
    highlight: Boolean,
    onMinus: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scale = if (highlight) 1.18f else 1f
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(iconDp).jiggle(edit, seed).graphicsLayer { scaleX = scale; scaleY = scale }) {
            Box(Modifier.size(iconDp).liquidGlass(squircle = true, bezel = iconDp * 0.2f, strength = 0.8f))
            val mini = iconDp * 0.235f
            val gap = iconDp * 0.05f
            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(gap)) {
                for (r in 0 until 3) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until 3) {
                        val k = apps.getOrNull(r * 3 + c)
                        Box(Modifier.size(mini).clip(SquircleShape)) { if (k != null) IconImage(k, mini, spec.copy(size = (spec.size * 0.4f).toInt().coerceAtLeast(48))) }
                    }
                }
            }
            if (badge > 0 && !edit) Badge(badge, Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-7).dp))
            if (edit && onMinus != null) MinusBadge(Modifier.align(Alignment.TopStart).offset(x = (-7).dp, y = (-7).dp), onMinus)
        }
        if (name != null) {
            Spacer(Modifier.height(5.dp))
            LText(name, 12.sp, FontWeight.Medium, Color.White, Modifier.width(iconDp + 22.dp), maxLines = 1, align = TextAlign.Center, shadow = true)
        }
    }
}

/** Para as listas: ícone e nome numa linha (Biblioteca, Spotlight). */
@Composable
fun AppRowIcon(key: String, size: Dp, spec: IconSpec) = Box(Modifier.size(size)) { IconImage(key, size, spec) }

@Composable
fun rememberSeed(id: String) = remember(id) { id.hashCode() }
