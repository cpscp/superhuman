package com.carlos.liquid.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlos.liquid.data.IconStyle
import com.carlos.liquid.glass.liquidGlass
import kotlinx.coroutines.delay

/** Estilo dos widgets: segue o estilo dos ícones, como no iOS 18/26. */
@Immutable
data class WStyle(val mode: IconStyle, val dark: Boolean, val tint: Color) {
    val clear get() = mode == IconStyle.CLEAR
    val tinted get() = mode == IconStyle.TINTED
    val solid get() = mode == IconStyle.DEFAULT || mode == IconStyle.DARK
    val isDark get() = dark || mode == IconStyle.DARK || clear || (tinted && dark)
    val bg: Color get() = when {
        clear -> Color.Transparent
        tinted -> if (dark) Color(0xFF121214) else Color(0xFFF7F7F9)
        isDark -> Color(0xFF1C1C1E)
        else -> Color.White
    }
    val fg: Color get() = when {
        tinted -> if (dark) tint else tint.darker(0.65f)
        clear -> Color.White
        isDark -> Color.White
        else -> Color.Black
    }
    val fg2: Color get() = fg.copy(alpha = 0.6f)
    val fg3: Color get() = fg.copy(alpha = 0.25f)
    fun accent(c: Color): Color = when { tinted -> fg; clear -> Color.White; else -> c }
}

fun Color.darker(f: Float) = Color(red * f, green * f, blue * f, alpha)

val LocalWStyle = staticCompositionLocalOf { WStyle(IconStyle.DEFAULT, false, Color(0xFF0A84FF)) }

val WidgetCorner = 26.dp

@Composable
fun WidgetFrame(
    w: Dp, h: Dp,
    modifier: Modifier = Modifier,
    solidBg: Brush? = null,
    padding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val st = LocalWStyle.current
    val base = modifier.size(w, h)
    val bgMod = when {
        st.clear -> base.liquidGlass(corner = WidgetCorner, bezel = 18.dp, strength = 0.7f)
        solidBg != null && st.solid -> base.clip(RoundedCornerShape(WidgetCorner)).background(solidBg)
        else -> base.clip(RoundedCornerShape(WidgetCorner)).background(st.bg)
    }
    Box(bgMod.padding(padding), content = content)
}

/** Relógio que atualiza ao segundo enquanto está visível. */
@Composable
fun rememberNow(everyMs: Long = 1000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(everyMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(everyMs - now % everyMs)
        }
    }
    return now
}
