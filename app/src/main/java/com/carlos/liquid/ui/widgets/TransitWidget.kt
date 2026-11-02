package com.carlos.liquid.ui.widgets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.Departure
import com.carlos.liquid.data.TransitRepo
import com.carlos.liquid.data.WidgetSize
import com.carlos.liquid.ui.LText
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val CpGreen = Color(0xFF2E7D32)
val CarrisYellow = Color(0xFFFFDD00)

/** Hook partilhado: partidas em tempo real que atualizam a cada 30 s. */
@Composable
fun rememberDepartures(mode: String, id: String, line: String): Pair<List<Departure>, String?> {
    var list by remember(mode, id, line) { mutableStateOf<List<Departure>>(emptyList()) }
    var err by remember(mode, id, line) { mutableStateOf<String?>(null) }
    LaunchedEffect(mode, id, line) {
        if (id.isBlank()) return@LaunchedEffect
        while (true) {
            runCatching { TransitRepo.departures(mode, id, line) }
                .onSuccess { list = it; err = null }
                .onFailure { if (list.isEmpty()) err = "Sem ligação" }
            delay(30_000)
        }
    }
    return list to err
}

fun minutesTo(epoch: Long): Long = (epoch - System.currentTimeMillis() / 1000) / 60

fun clockOf(epoch: Long): String = Instant.ofEpochSecond(epoch).atZone(ZoneId.of("Europe/Lisbon")).format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
fun LiveDot(color: Color) {
    val tr = rememberInfiniteTransition(label = "live")
    val a by tr.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(6.dp).alpha(a).clip(CircleShape).background(color))
}

@Composable
fun LinePill(text: String, color: Color, mode: String) {
    val st = LocalWStyle.current
    val bg = if (st.solid) color else st.fg.copy(alpha = 0.18f)
    val fg = if (st.solid) (if (color.luminance() > 0.6f) Color.Black else Color.White) else st.fg
    Box(Modifier.widthIn(min = 40.dp).height(22.dp).clip(RoundedCornerShape(7.dp)).background(bg).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
        LText(text.ifBlank { if (mode == "cp") "CP" else "—" }, 12.sp, FontWeight.Bold, fg, maxLines = 1)
    }
}

@Composable
fun TransitWidget(size: WidgetSize, w: Dp, h: Dp, config: Map<String, String>) {
    val st = LocalWStyle.current
    val mode = config["mode"] ?: "carris"
    val id = config["id"].orEmpty()
    val name = config["name"].orEmpty()
    val line = config["line"].orEmpty()
    val (deps, err) = rememberDepartures(mode, id, line)
    rememberNow(30_000)
    val brand = if (mode == "cp") CpGreen else CarrisYellow
    WidgetFrame(w, h, padding = 14.dp) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (st.solid) brand else st.fg.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(if (mode == "cp") Icons.Rounded.Train else Icons.Rounded.DirectionsBus, null,
                        tint = if (st.solid) (if (mode == "cp") Color.White else Color.Black) else st.fg, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(7.dp))
                LText(name.ifBlank { if (mode == "cp") "Estação CP" else "Paragem Carris" }, 14.sp, FontWeight.Bold, st.fg, Modifier.weight(1f), maxLines = 1)
                if (deps.any { it.live }) LiveDot(st.accent(Color(0xFF34C759)))
            }
            Spacer(Modifier.height(8.dp))
            when {
                id.isBlank() -> LText("Mantém premido → Editar widget para escolher a paragem ou estação", 13.sp, color = st.fg2)
                deps.isEmpty() -> LText(err ?: "A carregar partidas…", 13.sp, color = st.fg2)
                else -> {
                    val n = when (size) { WidgetSize.SMALL -> 2; WidgetSize.MEDIUM -> 3; WidgetSize.LARGE -> 8 }
                    Column(verticalArrangement = Arrangement.spacedBy(if (size == WidgetSize.SMALL) 8.dp else 7.dp)) {
                        deps.take(n).forEach { d -> DepartureRow(d, mode, compact = size == WidgetSize.SMALL) }
                    }
                }
            }
        }
    }
}

@Composable
fun DepartureRow(d: Departure, mode: String, compact: Boolean) {
    val st = LocalWStyle.current
    val m = minutesTo(d.epoch)
    val whenText = when { m <= 0 -> "agora"; m < 60 -> "$m min"; else -> clockOf(d.epoch) }
    if (compact) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinePill(d.line, Color(d.color), mode)
                Spacer(Modifier.weight(1f))
                LText(whenText, 15.sp, FontWeight.Bold, if (d.live) st.accent(Color(0xFF34C759)) else st.fg)
            }
            LText(d.dest, 12.sp, FontWeight.Medium, st.fg2, maxLines = 1)
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinePill(d.line, Color(d.color), mode)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                LText(d.dest, 14.sp, FontWeight.SemiBold, st.fg, maxLines = 1)
                val sub = buildString {
                    append(clockOf(d.epoch))
                    if (d.platform.isNotBlank()) append(" · Linha ${d.platform}")
                    if (d.delay > 0) append(" · +${d.delay} min")
                }
                LText(sub, 11.sp, color = if (d.delay > 0) st.accent(Color(0xFFFF9500)) else st.fg2, maxLines = 1)
            }
            LText(whenText, 15.sp, FontWeight.Bold, if (d.live) st.accent(Color(0xFF34C759)) else st.fg)
        }
    }
}
