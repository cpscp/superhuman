package com.carlos.liquid.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.CalendarRepo
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.ui.LText
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

val PT: Locale = Locale("pt", "PT")

val cityNames = linkedMapOf(
    "Europe/Lisbon" to "Lisboa", "Atlantic/Madeira" to "Funchal", "Atlantic/Azores" to "Ponta Delgada",
    "Europe/London" to "Londres", "Europe/Madrid" to "Madrid", "Europe/Paris" to "Paris", "Europe/Berlin" to "Berlim",
    "Europe/Rome" to "Roma", "Europe/Zurich" to "Zurique", "Europe/Amsterdam" to "Amesterdão", "Europe/Brussels" to "Bruxelas",
    "Europe/Luxembourg" to "Luxemburgo", "Europe/Athens" to "Atenas", "Europe/Istanbul" to "Istambul", "Europe/Moscow" to "Moscovo",
    "America/New_York" to "Nova Iorque", "America/Toronto" to "Toronto", "America/Chicago" to "Chicago", "America/Los_Angeles" to "Los Angeles",
    "America/Mexico_City" to "Cid. México", "America/Sao_Paulo" to "São Paulo", "America/Argentina/Buenos_Aires" to "Buenos Aires",
    "Africa/Luanda" to "Luanda", "Africa/Maputo" to "Maputo", "Atlantic/Cape_Verde" to "Praia", "Asia/Dubai" to "Dubai",
    "Asia/Kolkata" to "Bombaim", "Asia/Macau" to "Macau", "Asia/Shanghai" to "Xangai", "Asia/Singapore" to "Singapura",
    "Asia/Tokyo" to "Tóquio", "Asia/Seoul" to "Seul", "Australia/Sydney" to "Sydney", "Pacific/Auckland" to "Auckland",
)

fun cityOf(zone: String) = cityNames[zone] ?: zone.substringAfterLast('/').replace('_', ' ')

@Composable
fun AnalogDial(size: Dp, now: Long, zone: ZoneId = ZoneId.systemDefault(), numbers: Boolean = true, seconds: Boolean = true, forceDark: Boolean? = null) {
    val st = LocalWStyle.current
    val dark = forceDark ?: (st.isDark || st.tinted)
    val face = when { st.clear -> Color.White.copy(alpha = 0.12f); st.tinted -> st.fg.copy(alpha = 0.12f); dark -> Color(0xFF000000); else -> Color.White }
    val ink = when { st.tinted -> st.fg; st.clear -> Color.White; dark -> Color.White; else -> Color.Black }
    val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
    val sec = t.second + (now % 1000) / 1000f
    val min = t.minute + sec / 60f
    val hr = (t.hour % 12) + min / 60f
    val secColor = st.accent(Color(0xFFFF9500))
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            val c = center
            drawCircle(face, r)
            if (!st.solid || dark) drawCircle(ink.copy(alpha = 0.15f), r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90)
                val major = i % 5 == 0
                if (numbers && major) continue
                val r1 = r * 0.93f; val r2 = r * (if (major) 0.82f else 0.88f)
                drawLine(ink.copy(alpha = if (major) 0.8f else 0.3f), Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                    Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()), strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx())
            }
            fun hand(angleDeg: Float, len: Float, w: Float, col: Color, tail: Float = 0.12f) {
                val a = Math.toRadians(angleDeg - 90.0)
                val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
                drawLine(col, Offset(c.x - ca * r * tail, c.y - sa * r * tail), Offset(c.x + ca * r * len, c.y + sa * r * len), strokeWidth = w, cap = StrokeCap.Round)
            }
            hand(hr * 30f, 0.5f, r * 0.075f, ink, 0f)
            hand(min * 6f, 0.76f, r * 0.06f, ink, 0f)
            drawCircle(ink, r * 0.065f, c)
            if (seconds) { hand(sec * 6f, 0.86f, r * 0.022f, secColor, 0.18f); drawCircle(secColor, r * 0.04f, c) }
        }
        if (numbers) {
            val r = size / 2
            for (n in 1..12) {
                val a = Math.toRadians(n * 30.0 - 90)
                val rr = r * 0.74f
                LText(n.toString(), (size.value * 0.12f).sp, FontWeight.SemiBold, ink,
                    Modifier.offset(x = rr * cos(a).toFloat(), y = rr * sin(a).toFloat()), align = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ClockAnalogWidget(size: WidgetSize, w: Dp, h: Dp, zones: List<String>) {
    val now = rememberNow()
    when (size) {
        WidgetSize.SMALL -> WidgetFrame(w, h, padding = 10.dp) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AnalogDial(minOf(w, h) - 20.dp, now) } }
        else -> WorldClockWidget(size, w, h, zones)
    }
}

@Composable
fun WorldClockWidget(size: WidgetSize, w: Dp, h: Dp, zones: List<String>) {
    val now = rememberNow()
    val st = LocalWStyle.current
    val local = ZoneId.systemDefault()
    val list = zones.ifEmpty { listOf(local.id) }
    if (size == WidgetSize.SMALL) {
        val z = ZoneId.of(list.first())
        WidgetFrame(w, h, padding = 12.dp) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogDial(h - 50.dp, now, z, numbers = false)
                Spacer(Modifier.height(6.dp))
                LText(cityOf(z.id), 13.sp, FontWeight.SemiBold, st.fg, maxLines = 1)
            }
        }
        return
    }
    WidgetFrame(w, h, padding = 12.dp) {
        val rows = if (size == WidgetSize.LARGE) 2 else 1
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until 4) {
                        val zid = list.getOrNull(r * 4 + i) ?: continue
                        val z = runCatching { ZoneId.of(zid) }.getOrElse { local }
                        val there = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), z)
                        val here = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), local)
                        val diffH = (there.offset.totalSeconds - here.offset.totalSeconds) / 3600
                        val dayLabel = when {
                            there.toLocalDate().isAfter(here.toLocalDate()) -> "Amanhã"
                            there.toLocalDate().isBefore(here.toLocalDate()) -> "Ontem"
                            else -> "Hoje"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnalogDial(((w - 60.dp) / 4).coerceAtMost(72.dp), now, z, numbers = false)
                            Spacer(Modifier.height(5.dp))
                            LText(cityOf(z.id), 12.sp, FontWeight.SemiBold, st.fg, maxLines = 1)
                            LText("$dayLabel ${if (diffH >= 0) "+" else ""}${diffH}h", 11.sp, color = st.fg2, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClockDigitalWidget(size: WidgetSize, w: Dp, h: Dp) {
    val now = rememberNow()
    val st = LocalWStyle.current
    val ctx = LocalContext.current
    val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
    val time = t.format(DateTimeFormatter.ofPattern("HH:mm"))
    val date = t.format(DateTimeFormatter.ofPattern("EEEE, d MMM", PT)).replaceFirstChar { it.uppercase() }
    WidgetFrame(w, h) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val big = (maxWidth.value * (if (size == WidgetSize.SMALL) 0.36f else 0.3f)).coerceAtMost(maxHeight.value * 0.55f)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = if (size == WidgetSize.SMALL) Alignment.CenterHorizontally else Alignment.Start) {
                LText(date, 14.sp, FontWeight.SemiBold, st.accent(Color(0xFFFF3B30)), maxLines = 1)
                LText(time, big.sp, FontWeight.Bold, st.fg, maxLines = 1, letterSpacing = (-0.04f * big).sp)
                val alarm = CalendarRepo.nextAlarm(ctx)
                if (alarm != null && size != WidgetSize.SMALL) {
                    val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(alarm), ZoneId.systemDefault())
                    LText("⏰ " + at.format(DateTimeFormatter.ofPattern("EEE HH:mm", PT)), 14.sp, FontWeight.Medium, st.fg2)
                }
            }
        }
    }
}
