package com.carlos.superhuman.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.carlos.superhuman.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

@Immutable
data class Palette(
    val bg: Color,
    val card: Color,
    val inset: Color,      // fundo interior de cards / campos
    val line: Color,
    val ink: Color,        // texto principal e botões fortes
    val ink2: Color,       // texto secundário
    val ink3: Color,       // texto terciário / placeholders
    val onInk: Color,      // texto sobre botão forte
    val accent: Color,     // laranja dos gráficos
    val good: Color,
    val bad: Color,
    val warn: Color,
    val hatch: Color,      // barras "listradas" dos gráficos
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFECEAE6),
    card = Color(0xFFF7F6F4),
    inset = Color(0xFFE4E1DC),
    line = Color(0xFFDCD8D2),
    ink = Color(0xFF1B1A19),
    ink2 = Color(0xFF6E6A65),
    ink3 = Color(0xFFA8A39D),
    onInk = Color(0xFFF7F6F4),
    accent = Color(0xFFEF5B2A),
    good = Color(0xFF2FA866),
    bad = Color(0xFFE0483C),
    warn = Color(0xFFE8A13A),
    hatch = Color(0xFFCFC9C1),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF121211),
    card = Color(0xFF1D1C1B),
    inset = Color(0xFF2A2927),
    line = Color(0xFF2F2E2C),
    ink = Color(0xFFF2F0ED),
    ink2 = Color(0xFFA19C96),
    ink3 = Color(0xFF6C6863),
    onInk = Color(0xFF121211),
    accent = Color(0xFFF26A3B),
    good = Color(0xFF3BC17A),
    bad = Color(0xFFF0605A),
    warn = Color(0xFFF0B04F),
    hatch = Color(0xFF3B3936),
    dark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

object SH {
    val c: Palette @Composable get() = LocalPalette.current
}

object Type {
    val display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.8).sp)
    val h1 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.5).sp)
    val h2 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.3).sp)
    val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = (-0.1).sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val tiny = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp)
    val number = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-1).sp)
}

@Composable
fun SuperhumanTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(
        primary = p.ink, onPrimary = p.onInk, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink,
        surfaceVariant = p.inset, outline = p.line, secondary = p.accent,
    ) else lightColorScheme(
        primary = p.ink, onPrimary = p.onInk, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink,
        surfaceVariant = p.inset, outline = p.line, secondary = p.accent,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.let { t ->
            t.copy(
                bodyLarge = t.bodyLarge.copy(fontFamily = Inter),
                bodyMedium = t.bodyMedium.copy(fontFamily = Inter),
                labelLarge = t.labelLarge.copy(fontFamily = Inter),
                titleMedium = t.titleMedium.copy(fontFamily = Inter),
                titleLarge = t.titleLarge.copy(fontFamily = Inter),
                headlineSmall = t.headlineSmall.copy(fontFamily = Inter),
            )
        }, content = content)
    }
}
