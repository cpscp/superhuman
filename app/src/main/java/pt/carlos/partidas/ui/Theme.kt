package pt.carlos.partidas.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pt.carlos.partidas.R

/** Google Sans Flex, a fonte do Pixel (licença OFL, incluída na app em res/font). */
val GoogleSans = FontFamily(
    Font(R.font.gsf_regular, FontWeight.Normal),
    Font(R.font.gsf_medium, FontWeight.Medium),
    Font(R.font.gsf_semibold, FontWeight.SemiBold),
    Font(R.font.gsf_bold, FontWeight.Bold)
)

/** Tema fixo em cinzento escuro (não segue as cores do papel de parede). */
private val GreyScheme = darkColorScheme(
    primary = Color(0xFFE6E6E6),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = Color(0xFFFFB627),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB5B5B5),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF383838)
)

private fun TextStyle.inter() = copy(fontFamily = GoogleSans)

private val InterTypography = Typography().run {
    copy(
        displayLarge = displayLarge.inter(), displayMedium = displayMedium.inter(), displaySmall = displaySmall.inter(),
        headlineLarge = headlineLarge.inter(), headlineMedium = headlineMedium.inter(), headlineSmall = headlineSmall.inter(),
        titleLarge = titleLarge.inter(), titleMedium = titleMedium.inter(), titleSmall = titleSmall.inter(),
        bodyLarge = bodyLarge.inter(), bodyMedium = bodyMedium.inter(), bodySmall = bodySmall.inter(),
        labelLarge = labelLarge.inter(), labelMedium = labelMedium.inter(), labelSmall = labelSmall.inter()
    )
}

@Composable
fun PartidasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GreyScheme, typography = InterTypography, content = content)
}
