package pt.carlos.painel.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

private fun skyOf(code: Int) = when (code) {
    0 -> Sky.CLEAR
    1, 2 -> Sky.PARTLY
    3 -> Sky.CLOUDY
    45, 48 -> Sky.FOG
    in 51..67, in 80..82 -> Sky.RAIN
    in 71..77, 85, 86 -> Sky.SNOW
    in 95..99 -> Sky.STORM
    else -> Sky.CLOUDY
}

/**
 * Fundo do modo Display de acordo com o tempo: céu limpo, nublado, chuva, neve...
 * Animações leves (poucos elementos, sem desfoques) para o tablet não aquecer.
 */
@Composable
fun WeatherBackground(code: Int?, isDay: Boolean, modifier: Modifier = Modifier) {
    val sky = code?.let { skyOf(it) } ?: Sky.CLOUDY
    val colors: List<Color> = when (sky) {
        Sky.CLEAR -> if (isDay) listOf(Color(0xFF1B4F93), Color(0xFF3F7FC8), Color(0xFF8DB8E6)) else listOf(Color(0xFF070B1C), Color(0xFF111B38), Color(0xFF1E2C52))
        Sky.PARTLY -> if (isDay) listOf(Color(0xFF29598F), Color(0xFF5C88B8), Color(0xFFA9C2DC)) else listOf(Color(0xFF0B1022), Color(0xFF18223D), Color(0xFF2A3654))
        Sky.CLOUDY -> if (isDay) listOf(Color(0xFF39434F), Color(0xFF5B6674), Color(0xFF8792A0)) else listOf(Color(0xFF14181F), Color(0xFF222832), Color(0xFF323A46))
        Sky.FOG -> if (isDay) listOf(Color(0xFF5A636C), Color(0xFF7E878F), Color(0xFFA5ADB4)) else listOf(Color(0xFF1C2026), Color(0xFF2B3038), Color(0xFF3B414A))
        Sky.RAIN -> if (isDay) listOf(Color(0xFF1E2A37), Color(0xFF33445A), Color(0xFF52657C)) else listOf(Color(0xFF0C1118), Color(0xFF161E29), Color(0xFF243041))
        Sky.SNOW -> if (isDay) listOf(Color(0xFF4A5B72), Color(0xFF7C8EA6), Color(0xFFB8C6D8)) else listOf(Color(0xFF151B26), Color(0xFF263042), Color(0xFF3A465A))
        Sky.STORM -> listOf(Color(0xFF0E1015), Color(0xFF1C2029), Color(0xFF2C323E))
    }
    val t by rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (sky == Sky.RAIN || sky == Sky.STORM) 1400 else 60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when {
                sky == Sky.CLEAR && isDay -> drawCircle(
                    Brush.radialGradient(listOf(Color(0x66FFE08A), Color.Transparent), center = Offset(w * 0.86f, h * 0.10f), radius = w * 0.35f),
                    radius = w * 0.35f, center = Offset(w * 0.86f, h * 0.10f),
                )
                (sky == Sky.CLEAR || sky == Sky.PARTLY) && !isDay -> {
                    val r = Random(7)
                    repeat(70) {
                        val tw = 0.35f + 0.65f * ((it * 37 % 100) / 100f)
                        drawCircle(Color.White.copy(alpha = 0.25f + 0.45f * tw), radius = 1.2f + r.nextFloat() * 1.8f, center = Offset(r.nextFloat() * w, r.nextFloat() * h * 0.7f))
                    }
                }
                sky == Sky.PARTLY || sky == Sky.CLOUDY || sky == Sky.FOG -> {
                    // Nuvens suaves a deslizar devagar.
                    val alpha = if (sky == Sky.FOG) 0.10f else 0.08f
                    listOf(0.15f to 0.22f, 0.55f to 0.12f, 0.85f to 0.35f, 0.35f to 0.55f).forEachIndexed { i, (fx, fy) ->
                        val x = ((fx + t * (0.15f + i * 0.04f)) % 1.3f - 0.15f) * w
                        drawCircle(Color.White.copy(alpha = alpha), radius = w * (0.16f + i * 0.03f), center = Offset(x, fy * h))
                    }
                }
                sky == Sky.RAIN || sky == Sky.STORM -> {
                    val r = Random(3)
                    repeat(55) {
                        val x0 = r.nextFloat() * w
                        val speed = 0.7f + r.nextFloat() * 0.6f
                        val y = ((r.nextFloat() + t * speed) % 1f) * (h + 80f) - 40f
                        drawLine(Color.White.copy(alpha = 0.16f), Offset(x0, y), Offset(x0 - 8f, y + 34f), strokeWidth = 2f)
                    }
                }
                sky == Sky.SNOW -> {
                    val r = Random(5)
                    repeat(50) {
                        val x0 = r.nextFloat() * w
                        val y = ((r.nextFloat() + t * 6f * (0.5f + r.nextFloat())) % 1f) * h
                        drawCircle(Color.White.copy(alpha = 0.55f), radius = 2f + r.nextFloat() * 3f, center = Offset(x0 + 12f * kotlin.math.sin(y / 60f), y))
                    }
                }
            }
        }
    }
}
