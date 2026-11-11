package pt.carlos.painel.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Ícone e cor para cada código de tempo do Open-Meteo. */
fun weatherIcon(code: Int, isDay: Boolean): Pair<ImageVector, Color> = when (code) {
    0, 1 -> if (isDay) Icons.Outlined.WbSunny to Color(0xFFFDD663) else Icons.Outlined.NightsStay to Color(0xFFE8EAED)
    2, 3, 45, 48 -> Icons.Outlined.Cloud to Color(0xFFDADCE0)
    in 51..67, in 80..82 -> Icons.Outlined.WaterDrop to Color(0xFF8AB4F8)
    in 71..77, 85, 86 -> Icons.Outlined.AcUnit to Color(0xFFE8EAED)
    else -> Icons.Outlined.FlashOn to Color(0xFFFDD663)
}
