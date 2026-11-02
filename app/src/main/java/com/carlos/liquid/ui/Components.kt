package com.carlos.liquid.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.Store
import com.carlos.liquid.glass.LocalGlass
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.glass.panelTint
import kotlin.math.roundToInt

@Composable
fun rememberHaptic(): (Boolean) -> Unit {
    val v = LocalView.current
    return remember(v) {
        { strong: Boolean ->
            if (Store.s.haptics) v.performHapticFeedback(if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}

fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }

/** Botão cápsula em Liquid Glass. */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    tint: Color? = null,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "pill")
    Row(
        modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .liquidGlass(corner = height / 2, tint = tint, bezel = 10.dp)
            .pointerInput(onClick) {
                detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { onClick() })
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        content = content,
    )
}

@Composable
fun GlassCircle(icon: ImageVector, size: Dp = 44.dp, iconColor: Color = Color.White, tint: Color? = null, active: Boolean = false, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "circle")
    Box(
        Modifier.size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .liquidGlass(corner = size / 2, tint = if (active) Color.White.copy(alpha = 0.85f) else tint, bezel = size / 4)
            .pointerInput(onClick) { detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) Color.Black else iconColor, modifier = Modifier.size(size * 0.46f)) }
}

/** Interruptor estilo iOS. */
@Composable
fun IOSSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val haptic = rememberHaptic()
    val t by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "sw")
    Box(
        Modifier.size(51.dp, 31.dp).clip(CircleShape)
            .background(lerpColor(p.fill, p.green, t))
            .noRippleClick { haptic(false); onChange(!checked) },
    ) {
        Box(
            Modifier.offset(x = (2 + 20 * t).dp, y = 2.dp).size(27.dp)
                .shadow(3.dp, CircleShape).background(Color.White, CircleShape)
        )
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)

/** Controlo segmentado iOS. */
@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    var w by remember { mutableFloatStateOf(0f) }
    val pos by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.8f, stiffness = 500f), label = "seg")
    val density = LocalDensity.current
    Box(
        modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp)).background(p.fill).padding(2.dp)
            .onSizeChanged { w = it.width.toFloat() },
    ) {
        val segW = if (options.isEmpty()) 0f else w / options.size
        Box(
            Modifier.offset { IntOffset((segW * pos).roundToInt(), 0) }
                .width(with(density) { segW.toDp() }).height(30.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .background(if (p.dark) Color(0xFF636366) else Color.White, RoundedCornerShape(8.dp))
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, o ->
                Box(Modifier.weight(1f).fillMaxSize().noRippleClick { onSelect(i) }, contentAlignment = Alignment.Center) {
                    LText(o, 13.sp, if (i == selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

/** Slider estilo iOS. */
@Composable
fun LSlider(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier, range: ClosedFloatingPointRange<Float> = 0f..1f, track: Brush? = null) {
    val p = LocalPalette.current
    var w by remember { mutableFloatStateOf(1f) }
    val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    fun set(x: Float) { val f = (x / w).coerceIn(0f, 1f); onChange(range.start + f * (range.endInclusive - range.start)) }
    Box(
        modifier.fillMaxWidth().height(32.dp).onSizeChanged { w = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { set(it.x) } }
            .pointerInput(Unit) { detectDragGestures { ch, _ -> set(ch.position.x) } },
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(track ?: Brush.horizontalGradient(listOf(p.fill, p.fill))))
        if (track == null) Box(Modifier.width(with(density) { (w * frac).toDp() }).height(6.dp).clip(CircleShape).background(p.accent))
        Box(Modifier.offset { IntOffset((w * frac - 14.dp.toPx()).roundToInt().coerceAtLeast(0), 0) }.size(28.dp).shadow(4.dp, CircleShape).background(Color.White, CircleShape))
    }
}

/** Folha inferior em vidro, com arrastar para fechar. */
@Composable
fun BoxScope.GlassSheet(visible: Boolean, onDismiss: () -> Unit, maxHeightFraction: Float = 0.88f, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)).noRippleClick(onDismiss))
    }
    AnimatedVisibility(
        visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it },
        exit = slideOutVertically(tween(220)) { it },
    ) {
        var drag by remember { mutableFloatStateOf(0f) }
        val env = LocalGlass.current
        val p = LocalPalette.current
        val density = LocalDensity.current
        val screenH = with(density) { env.screenH.toDp() }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
                .heightIn(max = screenH * maxHeightFraction)
                .offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }
                .liquidGlass(corner = 38.dp, tint = panelTint(env).copy(alpha = if (p.dark) 0.86f else 0.9f), strength = 0.5f, bezel = 22.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .wrapContentHeight(),
        ) {
            Box(
                Modifier.fillMaxWidth().height(26.dp).pointerInput(Unit) {
                    detectVerticalDragGestures(onDragEnd = { if (drag > 140f) onDismiss(); drag = 0f }) { _, d -> drag += d }
                },
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(36.dp, 5.dp).clip(CircleShape).background(p.tertiary)) }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp), content = content)
        }
    }
}

@Composable
fun SheetTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        LText(title, 22.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) LText(action, 17.sp, FontWeight.SemiBold, p.accent, Modifier.noRippleClick(onAction))
    }
}

data class AlertButton(val text: String, val destructive: Boolean = false, val bold: Boolean = false, val onClick: () -> Unit)

/** Alerta estilo iOS. */
@Composable
fun BoxScope.IOSAlert(visible: Boolean, title: String, message: String?, buttons: List<AlertButton>, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
        val p = LocalPalette.current
        val env = LocalGlass.current
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).noRippleClick(onDismiss), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(290.dp).liquidGlass(corner = 30.dp, tint = panelTint(env).copy(alpha = 0.92f), strength = 0.4f).noRippleClick { }
                    .padding(top = 20.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LText(title, 17.sp, FontWeight.SemiBold, align = TextAlign.Center)
                if (message != null) { Spacer(Modifier.height(6.dp)); LText(message, 14.sp, color = p.secondary, align = TextAlign.Center) }
                Spacer(Modifier.height(16.dp))
                buttons.forEach { b ->
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).height(46.dp).clip(CircleShape)
                            .background(if (b.bold) p.accent else p.fill).clickable { b.onClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        LText(b.text, 17.sp, if (b.bold) FontWeight.SemiBold else FontWeight.Medium,
                            if (b.bold) Color.White else if (b.destructive) p.red else p.label)
                    }
                }
            }
        }
    }
}

// ------------------- Listas agrupadas (Definições) -------------------

@Composable
fun Group(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 22.dp)) {
        if (header != null) LText(header.uppercase(), 13.sp, color = p.secondary, modifier = Modifier.padding(start = 16.dp, bottom = 7.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(p.cell), content = content)
        if (footer != null) LText(footer, 13.sp, color = p.secondary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp))
    }
}

@Composable
fun Row2(
    title: String,
    icon: ImageVector? = null,
    iconBg: Color = Color(0xFF8E8E93),
    value: String? = null,
    subtitle: String? = null,
    chevron: Boolean = false,
    destructive: Boolean = false,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .let { if (onClick != null) it.clickable(remember { MutableInteractionSource() }, null) { onClick() } else it }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                LText(title, 17.sp, color = if (destructive) p.red else p.label, maxLines = 2)
                if (subtitle != null) LText(subtitle, 13.sp, color = p.secondary, maxLines = 3)
            }
            if (value != null) LText(value, 17.sp, color = p.secondary, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
            if (chevron) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.KeyboardArrowRight, null, tint = p.tertiary, modifier = Modifier.size(22.dp))
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(start = if (icon != null) 60.dp else 16.dp).height(0.5.dp).background(p.separator))
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, icon: ImageVector? = null, iconBg: Color = Color(0xFF8E8E93), subtitle: String? = null, divider: Boolean = true, onChange: (Boolean) -> Unit) =
    Row2(title, icon, iconBg, subtitle = subtitle, divider = divider, trailing = { IOSSwitch(checked, onChange) })

@Composable
fun CheckRow(title: String, checked: Boolean, divider: Boolean = true, subtitle: String? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row2(title, subtitle = subtitle, divider = divider, onClick = onClick, trailing = {
        if (checked) Icon(Icons.Rounded.Check, null, tint = p.accent, modifier = Modifier.size(22.dp))
    })
}

@Composable
fun Spacer8() = Spacer(Modifier.height(8.dp))
