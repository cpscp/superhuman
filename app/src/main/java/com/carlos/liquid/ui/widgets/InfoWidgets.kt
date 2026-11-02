package com.carlos.liquid.ui.widgets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.BatteryRepo
import com.carlos.liquid.data.CalEvent
import com.carlos.liquid.data.CalendarRepo
import com.carlos.liquid.data.HealthRepo
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.WeatherRepo
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.ui.LText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ================================ Calendário ================================

fun eventTime(e: CalEvent): String {
    if (e.allDay) return "Todo o dia"
    val z = ZoneId.systemDefault()
    val s = ZonedDateTime.ofInstant(Instant.ofEpochMilli(e.start), z)
    val en = ZonedDateTime.ofInstant(Instant.ofEpochMilli(e.end), z)
    val f = DateTimeFormatter.ofPattern("HH:mm")
    val day = when (ChronoUnit.DAYS.between(LocalDate.now(), s.toLocalDate())) {
        0L -> ""; 1L -> "Amanhã, "; else -> s.format(DateTimeFormatter.ofPattern("EEE, ", PT))
    }
    return day + s.format(f) + "–" + en.format(f)
}

@Composable
fun CalendarWidget(size: WidgetSize, w: Dp, h: Dp) {
    val st = LocalWStyle.current
    val events by CalendarRepo.state.collectAsState()
    val now = rememberNow(60_000)
    val today = LocalDate.now()
    val red = st.accent(Color(0xFFFF3B30))
    val ctx = LocalContext.current
    val hasPerm = CalendarRepo.granted(ctx)
    WidgetFrame(w, h) {
        when (size) {
            WidgetSize.SMALL -> Column(Modifier.fillMaxSize()) {
                LText(today.format(DateTimeFormatter.ofPattern("EEEE", PT)).uppercase(), 13.sp, FontWeight.Bold, red, maxLines = 1)
                LText(today.dayOfMonth.toString(), 40.sp, FontWeight.Normal, st.fg, lineHeight = 44.sp)
                Spacer(Modifier.weight(1f))
                val next = events.firstOrNull { !it.allDay && it.start >= System.currentTimeMillis() - 3600_000 } ?: events.firstOrNull()
                if (next != null) EventLine(next, 1) else LText(if (hasPerm) "Sem mais eventos hoje" else "Permite o acesso ao calendário", 13.sp, color = st.fg2, maxLines = 2)
            }
            WidgetSize.MEDIUM -> Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(80.dp)) {
                    LText(today.format(DateTimeFormatter.ofPattern("EEEE", PT)).uppercase(), 12.sp, FontWeight.Bold, red, maxLines = 1)
                    LText(today.dayOfMonth.toString(), 40.sp, color = st.fg)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (events.isEmpty()) LText(if (hasPerm) "Sem eventos nos próximos dias" else "Permite o acesso ao calendário nas Definições do Liquid", 14.sp, color = st.fg2)
                    events.take(3).forEach { EventLine(it, 1) }
                }
            }
            WidgetSize.LARGE -> Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    LText(today.format(DateTimeFormatter.ofPattern("MMMM", PT)).replaceFirstChar { it.uppercase() }, 18.sp, FontWeight.Bold, red)
                    Spacer(Modifier.width(6.dp))
                    LText(today.year.toString(), 18.sp, FontWeight.Bold, st.fg2)
                }
                Spacer(Modifier.height(8.dp))
                MonthGrid(today, red)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { events.take(3).forEach { EventLine(it, 1) } }
            }
        }
    }
}

@Composable
private fun EventLine(e: CalEvent, lines: Int) {
    val st = LocalWStyle.current
    Row(Modifier.fillMaxWidth().height(38.dp)) {
        Box(Modifier.width(4.dp).fillMaxHeight().clip(CircleShape).background(st.accent(Color(e.color or 0xFF000000.toInt()))))
        Spacer(Modifier.width(7.dp))
        Column {
            LText(e.title, 14.sp, FontWeight.SemiBold, st.fg, maxLines = lines)
            LText(eventTime(e), 12.sp, color = st.fg2, maxLines = 1)
        }
    }
}

@Composable
private fun MonthGrid(today: LocalDate, red: Color) {
    val st = LocalWStyle.current
    val first = today.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1
    val days = today.lengthOfMonth()
    val heads = listOf("S", "T", "Q", "Q", "S", "S", "D")
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth()) { heads.forEach { LText(it, 11.sp, FontWeight.SemiBold, st.fg2, Modifier.weight(1f), align = TextAlign.Center) } }
        val cells = offset + days
        for (r in 0 until (cells + 6) / 7) Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 7) {
                val d = r * 7 + c - offset + 1
                Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                    if (d in 1..days) {
                        val isToday = d == today.dayOfMonth
                        if (isToday) Box(Modifier.size(24.dp).clip(CircleShape).background(red))
                        LText(d.toString(), 12.sp, if (isToday) FontWeight.Bold else FontWeight.Medium, if (isToday) Color.White else st.fg)
                    }
                }
            }
        }
    }
}

// ================================ Meteorologia ================================

fun weatherIcon(code: Int, day: Boolean): ImageVector = when (code) {
    0, 1 -> if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2 -> Icons.Rounded.WbCloudy
    3, 45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57 -> Icons.Rounded.Grain
    61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

fun weatherIconColor(code: Int, day: Boolean): Color = when (code) {
    0, 1 -> if (day) Color(0xFFFFD60A) else Color.White
    else -> Color.White
}

fun skyBrush(code: Int, day: Boolean): Brush = when {
    !day -> Brush.verticalGradient(listOf(Color(0xFF0B1A3A), Color(0xFF2A3A63)))
    code >= 61 || code in 51..57 -> Brush.verticalGradient(listOf(Color(0xFF4B5A6D), Color(0xFF7D8C9E)))
    code >= 2 -> Brush.verticalGradient(listOf(Color(0xFF4F79A6), Color(0xFF8DAACB)))
    else -> Brush.verticalGradient(listOf(Color(0xFF2E7BD6), Color(0xFF6FB1F0)))
}

@Composable
fun WeatherWidget(size: WidgetSize, w: Dp, h: Dp) {
    val wx by WeatherRepo.state.collectAsState()
    val st = LocalWStyle.current
    val x = wx
    val fg = if (st.solid) Color.White else st.fg
    val fg2 = fg.copy(alpha = 0.75f)
    WidgetFrame(w, h, solidBg = if (x != null) skyBrush(x.code, x.isDay) else skyBrush(0, true)) {
        if (x == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                LText(Store.s.cityName, 15.sp, FontWeight.SemiBold, fg)
                LText("A obter o tempo…", 13.sp, color = fg2)
            }
            return@WidgetFrame
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LText(x.city, 15.sp, FontWeight.SemiBold, fg, maxLines = 1)
                        if (Store.s.useLocation) Icon(Icons.Rounded.NearMe, null, tint = fg, modifier = Modifier.padding(start = 3.dp).size(12.dp))
                    }
                    LText("${x.temp}°", if (size == WidgetSize.SMALL) 42.sp else 44.sp, FontWeight.Light, fg, lineHeight = 48.sp)
                }
                if (size != WidgetSize.SMALL) Column(horizontalAlignment = Alignment.End) {
                    Icon(weatherIcon(x.code, x.isDay), null, tint = if (st.solid) weatherIconColor(x.code, x.isDay) else fg, modifier = Modifier.size(22.dp))
                    LText(WeatherRepo.describe(x.code), 13.sp, FontWeight.SemiBold, fg)
                    LText("Máx.:${x.max}° Mín.:${x.min}°", 13.sp, FontWeight.Medium, fg)
                }
            }
            Spacer(Modifier.weight(1f))
            if (size == WidgetSize.SMALL) {
                Icon(weatherIcon(x.code, x.isDay), null, tint = if (st.solid) weatherIconColor(x.code, x.isDay) else fg, modifier = Modifier.size(18.dp))
                LText(WeatherRepo.describe(x.code), 13.sp, FontWeight.SemiBold, fg, maxLines = 1)
                LText("Máx.:${x.max}° Mín.:${x.min}°", 13.sp, FontWeight.Medium, fg, maxLines = 1)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    x.hours.forEach { hh ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LText(hh.time, 12.sp, FontWeight.SemiBold, fg2)
                            Spacer(Modifier.height(4.dp))
                            Icon(weatherIcon(hh.code, hh.day), null, tint = if (st.solid) weatherIconColor(hh.code, hh.day) else fg, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            LText("${hh.temp}°", 14.sp, FontWeight.SemiBold, fg)
                        }
                    }
                }
                if (size == WidgetSize.LARGE) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(fg.copy(alpha = 0.3f)))
                    val lo = x.days.minOfOrNull { it.min } ?: 0; val hi = x.days.maxOfOrNull { it.max } ?: 1
                    x.days.drop(1).take(5).forEach { d ->
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            LText(d.name, 14.sp, FontWeight.SemiBold, fg, Modifier.width(44.dp))
                            Icon(weatherIcon(d.code, true), null, tint = if (st.solid) weatherIconColor(d.code, true) else fg, modifier = Modifier.size(18.dp))
                            LText("${d.min}°", 14.sp, FontWeight.Medium, fg2, Modifier.width(40.dp), align = TextAlign.End)
                            Canvas(Modifier.weight(1f).height(5.dp).padding(horizontal = 8.dp)) {
                                val span = (hi - lo).coerceAtLeast(1).toFloat()
                                drawRoundRect(fg.copy(alpha = 0.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
                                val x0 = this.size.width * (d.min - lo) / span; val x1 = this.size.width * (d.max - lo) / span
                                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF64D2FF), Color(0xFFFFD60A), Color(0xFFFF9F0A))),
                                    Offset(x0, 0f), Size((x1 - x0).coerceAtLeast(6f), this.size.height), androidx.compose.ui.geometry.CornerRadius(10f))
                            }
                            LText("${d.max}°", 14.sp, FontWeight.SemiBold, fg, Modifier.width(34.dp), align = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

// ================================ Atividade / Saúde ================================

@Composable
fun Rings(size: Dp, values: List<Float>, colors: List<Color>, stroke: Dp) {
    Canvas(Modifier.size(size)) {
        val sw = stroke.toPx()
        values.forEachIndexed { i, v ->
            val inset = sw / 2 + i * (sw + 2.dp.toPx())
            val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            val c = colors[i]
            drawArc(c.copy(alpha = 0.22f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            val sweep = 360f * v.coerceIn(0f, 2f)
            if (sweep > 0.5f) drawArc(c, -90f, sweep.coerceAtMost(359.9f), false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun ActivityWidget(size: WidgetSize, w: Dp, h: Dp) {
    val hs by HealthRepo.state.collectAsState()
    val st = LocalWStyle.current
    val s = Store.s
    val move = Color(0xFFFA114F); val ex = Color(0xFF92E82A); val stp = Color(0xFF1EEAEF)
    val cols = listOf(st.accent(move), if (st.tinted) st.fg.copy(alpha = 0.75f) else st.accent(ex), if (st.tinted) st.fg.copy(alpha = 0.5f) else st.accent(stp))
    val vals = listOf((hs.kcal / s.moveGoal).toFloat(), hs.exerciseMin.toFloat() / s.exerciseGoal, hs.steps.toFloat() / s.stepsGoal)
    val dark = st.solid
    val fg = if (dark) Color.White else st.fg
    WidgetFrame(w, h, solidBg = if (dark) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else null) {
        if (!hs.ok) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Rings(64.dp, listOf(0f, 0f, 0f), cols, 9.dp)
                Spacer(Modifier.height(8.dp))
                LText("Liga o Health Connect nas Definições", 12.sp, color = fg.copy(alpha = 0.7f), align = TextAlign.Center)
            }
            return@WidgetFrame
        }
        val lines = @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatLine("Mover", "${hs.kcal.toInt()}/${s.moveGoal}", "KCAL", cols[0])
                StatLine("Exercício", "${hs.exerciseMin}/${s.exerciseGoal}", "MIN", cols[1])
                StatLine("Passos", "%,d".format(PT, hs.steps).replace(',', ' '), "", cols[2])
            }
        }
        when (size) {
            WidgetSize.SMALL -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Rings(h - 64.dp, vals, cols, 12.dp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LText("${hs.kcal.toInt()}", 13.sp, FontWeight.Bold, cols[0]); LText("${hs.exerciseMin}", 13.sp, FontWeight.Bold, cols[1]); LText("${hs.steps / 1000}k", 13.sp, FontWeight.Bold, cols[2])
                }
            }
            WidgetSize.MEDIUM -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Rings(h - 32.dp, vals, cols, 13.dp)
                Spacer(Modifier.width(16.dp))
                lines()
            }
            WidgetSize.LARGE -> Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) { Rings(120.dp, vals, cols, 14.dp); Spacer(Modifier.width(16.dp)); lines() }
                Spacer(Modifier.height(14.dp))
                LText("Passos · 7 dias", 13.sp, FontWeight.SemiBold, fg.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
                Bars(hs.steps7, cols[2], Modifier.fillMaxWidth().weight(1f), fg)
                if (hs.sleepMin > 0) LText("Sono: ${hs.sleepMin / 60} h ${hs.sleepMin % 60} min", 13.sp, FontWeight.Medium, fg.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, unit: String, c: Color) {
    Column {
        LText(label, 12.sp, FontWeight.SemiBold, LocalWStyle.current.let { if (it.solid) Color.White else it.fg })
        Row(verticalAlignment = Alignment.Bottom) {
            LText(value, 18.sp, FontWeight.Bold, c)
            if (unit.isNotBlank()) LText(unit, 11.sp, FontWeight.Bold, c, Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
    }
}

@Composable
fun Bars(values: List<Long>, color: Color, modifier: Modifier, fg: Color) {
    val labels = listOf("S", "T", "Q", "Q", "S", "S", "D")
    val today = LocalDate.now()
    val max = (values.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val d = today.minusDays((values.size - 1 - i).toLong())
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f, fill = true).width(14.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight((v / max).coerceIn(0.03f, 1f)).clip(RoundedCornerShape(5.dp)).background(if (i == values.size - 1) color else color.copy(alpha = 0.5f)))
                }
                LText(labels[d.dayOfWeek.value - 1], 11.sp, FontWeight.SemiBold, fg.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun StepsWidget(size: WidgetSize, w: Dp, h: Dp) {
    val hs by HealthRepo.state.collectAsState()
    val st = LocalWStyle.current
    val pink = st.accent(Color(0xFFFF2D55))
    WidgetFrame(w, h) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, null, tint = pink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp)); LText("Passos", 14.sp, FontWeight.SemiBold, pink)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                LText("%,d".format(PT, hs.steps).replace(',', ' '), 30.sp, FontWeight.Bold, st.fg)
                LText(" passos", 13.sp, FontWeight.SemiBold, st.fg2, Modifier.padding(bottom = 5.dp))
            }
            LText("%.1f km".format(PT, hs.distanceKm), 14.sp, FontWeight.Medium, st.fg2)
            Spacer(Modifier.height(8.dp))
            if (size == WidgetSize.SMALL) {
                Spacer(Modifier.weight(1f))
                val f = (hs.steps.toFloat() / Store.s.stepsGoal).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(st.fg3)) {
                    Box(Modifier.fillMaxWidth(f).height(8.dp).clip(CircleShape).background(pink))
                }
                LText("Objetivo ${Store.s.stepsGoal / 1000} mil", 11.sp, color = st.fg2, modifier = Modifier.padding(top = 4.dp))
            } else Bars(hs.steps7.ifEmpty { listOf(0, 0, 0, 0, 0, 0, hs.steps) }, pink, Modifier.fillMaxWidth().weight(1f), st.fg)
        }
    }
}

// ================================ Bateria ================================

fun batteryColor(l: Int, charging: Boolean): Color = when { charging -> Color(0xFF34C759); l <= 20 -> Color(0xFFFF3B30); else -> Color(0xFF34C759) }

fun deviceIcon(kind: String) = when (kind) { "watch" -> Icons.Rounded.Watch; "headphones" -> Icons.Rounded.Headphones; "phone" -> Icons.Rounded.PhoneAndroid; else -> Icons.Rounded.Bluetooth }

@Composable
fun BatteryRing(level: Int, charging: Boolean, kind: String, size: Dp) {
    val st = LocalWStyle.current
    val c = st.accent(batteryColor(level, charging))
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.toPx() * 0.09f
            drawArc(st.fg3, 0f, 360f, false, Offset(sw / 2, sw / 2), Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw))
            drawArc(c, -90f, 3.6f * level, false, Offset(sw / 2, sw / 2), Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
        }
        Icon(if (charging) Icons.Rounded.BatteryChargingFull else deviceIcon(kind), null, tint = st.fg, modifier = Modifier.size(size * 0.4f))
    }
}

@Composable
fun BatteryWidget(size: WidgetSize, w: Dp, h: Dp) {
    val list by BatteryRepo.state.collectAsState()
    val st = LocalWStyle.current
    WidgetFrame(w, h, padding = 14.dp) {
        if (size == WidgetSize.SMALL) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until 2) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (c in 0 until 2) {
                        val d = list.getOrNull(r * 2 + c)
                        if (d != null) BatteryRing(d.level, d.charging, d.kind, (h - 52.dp) / 2)
                        else Box(Modifier.size((h - 52.dp) / 2).clip(CircleShape).background(st.fg3.copy(alpha = 0.12f)))
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.take(if (size == WidgetSize.LARGE) 6 else 3).forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BatteryRing(d.level, d.charging, d.kind, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        LText(d.name, 15.sp, FontWeight.SemiBold, st.fg, Modifier.weight(1f), maxLines = 1)
                        LText("${d.level}%", 17.sp, FontWeight.SemiBold, st.fg)
                    }
                }
            }
        }
    }
}

// ================================ Fotografias ================================

fun photosDir(ctx: android.content.Context) = File(ctx.filesDir, "photos")

@Composable
fun PhotosWidget(w: Dp, h: Dp, widgetId: String) {
    val ctx = LocalContext.current
    val st = LocalWStyle.current
    val hour = rememberNow(3_600_000) / 3_600_000
    val img: ImageBitmap? by produceState<ImageBitmap?>(null, hour, widgetId) {
        value = withContext(Dispatchers.IO) {
            val files = File(photosDir(ctx), widgetId).listFiles()?.sortedBy { it.name }.orEmpty()
            if (files.isEmpty()) null else runCatching {
                val f = files[(hour % files.size).toInt()]
                BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap()
            }.getOrNull()
        }
    }
    WidgetFrame(w, h, padding = 0.dp) {
        val i = img
        if (i != null) Image(i, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.PhotoLibrary, null, tint = st.accent(Color(0xFFFF9500)), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            LText("Mantém premido → Editar widget para escolher fotos", 12.sp, color = st.fg2, align = TextAlign.Center)
        }
    }
}

