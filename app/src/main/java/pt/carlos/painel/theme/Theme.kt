package pt.carlos.painel.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.ColorUtils
import pt.carlos.painel.R
import pt.carlos.painel.data.Prefs

/* Tipografia: exclusivamente Google Sans Flex, a fonte do Pixel (licença OFL, embutida na app). */
@OptIn(ExperimentalTextApi::class)
private fun gsf(weight: Int) = Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val GoogleSansFlex = FontFamily(gsf(400), gsf(500), gsf(600), gsf(700))

/** Google Sans Flex num peso exato e com cantos arredondados (eixo ROND), como o relógio do Pixel. */
@OptIn(ExperimentalTextApi::class)
fun googleSansFlexAt(weight: Int, round: Float = 0f) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("ROND", round)),
    )
)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

private val PainelTypography = Typography().withFont(GoogleSansFlex)

/* Cores a partir de uma cor-semente (quando a cor dinâmica do papel de parede está desligada). */
private fun tone(h: Float, s: Float, t: Int) = Color(ColorUtils.HSLToColor(floatArrayOf(h, s, t / 100f)))

fun seedScheme(seed: Int, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0.35f, 0.85f)
    val p = { t: Int -> tone(h, s, t) }
    val sec = { t: Int -> tone(h, s * 0.30f, t) }
    val ter = { t: Int -> tone((h + 60f) % 360f, s * 0.45f, t) }
    val n = { t: Int -> tone(h, 0.06f, t) }
    val nv = { t: Int -> tone(h, 0.12f, t) }
    return if (!dark) lightColorScheme(
        primary = p(40), onPrimary = p(100), primaryContainer = p(90), onPrimaryContainer = p(10),
        secondary = sec(40), onSecondary = sec(100), secondaryContainer = sec(90), onSecondaryContainer = sec(10),
        tertiary = ter(40), onTertiary = ter(100), tertiaryContainer = ter(90), onTertiaryContainer = ter(10),
        background = n(98), onBackground = n(10), surface = n(98), onSurface = n(10),
        surfaceVariant = nv(90), onSurfaceVariant = nv(30), outline = nv(50), outlineVariant = nv(80),
        inverseSurface = n(20), inverseOnSurface = n(95), inversePrimary = p(80),
        surfaceBright = n(98), surfaceDim = n(87),
        surfaceContainerLowest = n(100), surfaceContainerLow = n(96), surfaceContainer = n(94),
        surfaceContainerHigh = n(92), surfaceContainerHighest = n(90),
    ) else darkColorScheme(
        primary = p(80), onPrimary = p(20), primaryContainer = p(30), onPrimaryContainer = p(90),
        secondary = sec(80), onSecondary = sec(20), secondaryContainer = sec(30), onSecondaryContainer = sec(90),
        tertiary = ter(80), onTertiary = ter(20), tertiaryContainer = ter(30), onTertiaryContainer = ter(90),
        background = n(6), onBackground = n(90), surface = n(6), onSurface = n(90),
        surfaceVariant = nv(30), onSurfaceVariant = nv(80), outline = nv(60), outlineVariant = nv(30),
        inverseSurface = n(90), inverseOnSurface = n(20), inversePrimary = p(40),
        surfaceBright = n(24), surfaceDim = n(6),
        surfaceContainerLowest = n(4), surfaceContainerLow = n(10), surfaceContainer = n(12),
        surfaceContainerHigh = n(17), surfaceContainerHighest = n(22),
    )
}

@Composable
fun isPainelDark(prefs: Prefs): Boolean = when (prefs.themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun PainelTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isPainelDark(prefs)
    val scheme = remember(dark, prefs.dynamicColor, prefs.seed) {
        when {
            prefs.dynamicColor && dark -> dynamicDarkColorScheme(ctx)
            prefs.dynamicColor -> dynamicLightColorScheme(ctx)
            else -> seedScheme(prefs.seed, dark)
        }
    }
    MaterialTheme(colorScheme = scheme, typography = PainelTypography, content = content)
}
