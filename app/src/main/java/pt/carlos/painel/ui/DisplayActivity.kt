@file:OptIn(ExperimentalMaterial3Api::class)

package pt.carlos.painel.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import pt.carlos.painel.home.HomeDev
import pt.carlos.painel.home.HomeRepo
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import pt.carlos.painel.PainelApp
import pt.carlos.painel.data.CalEvent
import pt.carlos.painel.data.CalendarRepo
import pt.carlos.painel.data.Forecast
import pt.carlos.painel.data.Weather
import pt.carlos.painel.media.Media
import pt.carlos.painel.theme.PainelTheme
import pt.carlos.painel.theme.googleSansFlexAt
import pt.carlos.painel.transit.CarrisApi
import pt.carlos.painel.transit.Departure
import pt.carlos.painel.transit.LiveStatus
import pt.carlos.painel.transit.StopRef
import pt.carlos.painel.transit.Text as TText
import pt.carlos.painel.transit.TrainApi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Modo Display (Operações): ecrã da entrada com hora, tempo, partidas, agenda e música. */
class DisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        Media.start(this)
        HomeRepo.register(this, this)
        setContent {
            PainelTheme(PainelApp.instance.prefs, forceDark = true) {
                DisplayScreen(
                    onClose = { finish() },
                    onNight = { night ->
                        window.attributes = window.attributes.apply {
                            screenBrightness = if (night) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Media.start(this)
    }
}

private val pt = Locale("pt", "PT")
private val hm = DateTimeFormatter.ofPattern("HH:mm")

private fun hhmm(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(hm)

@Composable
private fun DisplayScreen(onClose: () -> Unit, onNight: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val prefs = PainelApp.instance.prefs

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var forecast by remember { mutableStateOf<Forecast?>(null) }
    var trains by remember { mutableStateOf<List<Departure>?>(null) }
    var trainError by remember { mutableStateOf(false) }
    var buses by remember { mutableStateOf<List<Departure>?>(null) }
    var busError by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    var calOk by remember { mutableStateOf(CalendarRepo.hasPermission(ctx)) }
    var tick by remember { mutableLongStateOf(0L) }

    val calLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { calOk = it }

    // Relógio (a cada segundo, para a barra da música) e modo noite.
    LaunchedEffect(Unit) {
        while (true) { now = LocalDateTime.now(); tick++; delay(1000) }
    }
    val night = prefs.isNight(now.hour)
    LaunchedEffect(night) { onNight(night) }

    // Partidas: a cada 30 s enquanto o ecrã está visível.
    LaunchedEffect(prefs.trainStation, prefs.trainDirection) {
        while (true) {
            val r = withContext(Dispatchers.IO) {
                runCatching { TrainApi.departures("", prefs.trainStation, prefs.trainDirection, System.currentTimeMillis(), 5) }
            }
            r.onSuccess { trains = it; trainError = false }.onFailure { trainError = true }
            delay(30_000)
        }
    }
    LaunchedEffect(prefs.busStops) {
        val stops = prefs.busStops.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }.map { StopRef(it, "") }
        while (true) {
            val r = withContext(Dispatchers.IO) {
                runCatching { CarrisApi.departures(stops, System.currentTimeMillis(), 6) }
            }
            r.onSuccess { buses = it; busError = false }.onFailure { busError = true }
            delay(30_000)
        }
    }
    // Tempo (cache de 20 min) e agenda (a cada 5 min).
    LaunchedEffect(prefs.weatherLat, prefs.weatherLon) {
        while (true) { forecast = Weather.forecast(ctx) ?: forecast; delay(10 * 60_000) }
    }
    LaunchedEffect(calOk, prefs.hiddenCalendars) {
        while (calOk) {
            val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            events = CalendarRepo.events(ctx, start, start + 2 * 86_400_000L, prefs.hiddenCalendars)
            delay(5 * 60_000)
        }
    }

    // Pequena deslocação a cada minuto (proteção do ecrã sempre ligado).
    val shift = (now.minute % 4) * 2

    val bg by animateColorAsState(if (night) Color.Black else MaterialTheme.colorScheme.surface, label = "bg")

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .offset(x = shift.dp, y = shift.dp)
    ) {
        if (night) {
            NightView(now, trains)
        } else {
            Row(
                Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.weight(1.05f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ClockBlock(now)
                    WeatherCard(forecast, Modifier.weight(1f))
                }
                Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    TransitCard(
                        title = prefs.trainStation,
                        subtitle = "Sentido ${prefs.trainDirection}",
                        icon = { Icon(Icons.Outlined.Train, null, tint = Color(0xFF7FD48B)) },
                        list = trains, error = trainError, train = true, nowMs = System.currentTimeMillis(),
                        modifier = Modifier.weight(1f),
                    )
                    TransitCard(
                        title = prefs.busTitle,
                        subtitle = "Carris",
                        icon = { Icon(Icons.Outlined.DirectionsBus, null, tint = Color(0xFFFDD663)) },
                        list = buses, error = busError, train = false, nowMs = System.currentTimeMillis(),
                        modifier = Modifier.weight(1f),
                    )
                    HomeCard()
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    AgendaCard(
                        events = events,
                        calOk = calOk,
                        onAsk = { calLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        modifier = Modifier.weight(1f),
                    )
                    MusicCard(tick, Modifier.weight(1f))
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
        ) { Icon(Icons.Outlined.Close, "Sair do modo Display") }
    }
}

// ---------- Blocos ----------

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Composable
private fun ClockBlock(now: LocalDateTime) {
    Column(Modifier.padding(start = 8.dp)) {
        Text(
            now.format(hm),
            style = TextStyle(fontFamily = googleSansFlexAt(500, 100f), fontSize = 112.sp, lineHeight = 112.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", pt)).replaceFirstChar { it.titlecase(pt) },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeatherCard(f: Forecast?, modifier: Modifier) {
    Card(modifier) {
        if (f == null) {
            Text("A carregar o tempo…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Card
        }
        Text(f.place, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, tint) = weatherIcon(f.now.code, f.now.isDay)
            Icon(icon, null, tint = tint, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Text("${f.now.tempC}°", style = TextStyle(fontFamily = googleSansFlexAt(400, 100f), fontSize = 72.sp), color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(Weather.describe(f.now.code), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Máx. ${f.max}° · Mín. ${f.min}°", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Hoje, por hora", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            f.hours.forEach { h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%02d h".format(h.hour), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val (icon, tint) = weatherIcon(h.code, h.isDay)
                    Icon(icon, null, tint = tint, modifier = Modifier.padding(vertical = 6.dp).size(24.dp))
                    Text("${h.tempC}°", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (h.rainPct >= 20) Text("${h.rainPct}%", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8AB4F8))
                }
            }
        }
        if (f.days.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(Modifier.height(14.dp))
            Text("Próximos 5 dias", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                f.days.forEach { d ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            d.date.format(DateTimeFormatter.ofPattern("EEE", pt)).trimEnd('.').replaceFirstChar { it.titlecase(pt) },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val (icon, tint) = weatherIcon(d.code, true)
                        Icon(icon, null, tint = tint, modifier = Modifier.padding(vertical = 6.dp).size(24.dp))
                        Text("${d.max}°", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("${d.min}°", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    list: List<Departure>?,
    error: Boolean,
    train: Boolean,
    nowMs: Long,
    modifier: Modifier,
) {
    Card(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            list == null && !error -> Text("A carregar…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            list.isNullOrEmpty() -> Text(
                if (error) "Sem ligação aos dados. Volta a tentar em 30 s." else "Sem partidas nas próximas horas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                val rows = if (train) list.take(4) else list.take(5)
                rows.forEach { d -> DepartureRow(d, train, nowMs) }
            }
        }
    }
}

@Composable
private fun DepartureRow(d: Departure, train: Boolean, nowMs: Long) {
    val mins = ((d.etaMs - nowMs) / 60_000L).coerceAtLeast(0)
    val cancelled = d.status == LiveStatus.SUPRIMIDO
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (train) {
            Text(hhmm(d.etaMs), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(76.dp))
        } else {
            // Carris: amarelo e azul da marca (como no Partidas).
            val bg = d.badgeColor?.let { Color(it) } ?: Color(0xFFFFC20E)
            val fg = d.badgeTextColor?.let { Color(it) } ?: Color(0xFF00467F)
            Box(
                Modifier.width(64.dp).clip(RoundedCornerShape(10.dp)).background(bg).padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) { Text(d.badge, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = fg) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                TText.title(d.destination),
                style = MaterialTheme.typography.titleMedium.copy(textDecoration = if (cancelled) TextDecoration.LineThrough else null),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (train && d.badge.isNotBlank()) {
                Text(d.badge + if (d.delayMin > 0) " · +${d.delayMin} min" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val label = when {
            cancelled -> "Suprimido"
            mins == 0L -> "agora"
            else -> "$mins min"
        }
        Text(
            label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = when {
                cancelled -> MaterialTheme.colorScheme.error
                d.status == LiveStatus.ATRASADO -> Color(0xFFFDD663)
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun AgendaCard(events: List<CalEvent>, calOk: Boolean, onAsk: () -> Unit, modifier: Modifier) {
    Card(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Event, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("Agenda", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        if (!calOk) {
            Text("Para ver a agenda, o Painel precisa de acesso ao calendário.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onAsk) { Text("Permitir") }
            return@Card
        }
        val today = LocalDate.now()
        val nowMs = System.currentTimeMillis()
        val upcoming = events.filter { it.end > nowMs }
        val groups = listOf(
            "Hoje" to upcoming.filter { dayOf(it) == today },
            "Amanhã" to upcoming.filter { dayOf(it) == today.plusDays(1) },
        )
        if (upcoming.isEmpty()) {
            Text("Nada marcado para hoje e amanhã.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        groups.forEach { (label, list) ->
            if (list.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                list.take(4).forEach { e ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(e.color)))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (e.allDay) "Todo o dia" else hhmm(e.begin),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(92.dp),
                        )
                        Text(e.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun dayOf(e: CalEvent): LocalDate =
    if (e.allDay) Instant.ofEpochMilli(e.begin).atZone(ZoneId.of("UTC")).toLocalDate()
    else Instant.ofEpochMilli(e.begin).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun MusicCard(tick: Long, modifier: Modifier) {
    val ctx = LocalContext.current
    val info = Media.info
    if (!Media.hasAccess(ctx) || info == null) {
        Card(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Música", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            if (!Media.hasAccess(ctx)) {
                Text("Para ver e controlar a música, ativa o Acesso a notificações do Painel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                }) { Text("Ativar") }
            } else {
                Text("Nada a tocar. Põe música no BitChord, YouTube ou Nuvio.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    // A capa do álbum é o fundo do mosaico, com um véu escuro para o texto se ler bem.
    Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxSize()) {
            if (info.art != null) {
                Image(info.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(18.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.70f)))))
            }
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(88.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.12f))
                            .clickable { Media.openApp(ctx) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (info.art != null) Image(info.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Outlined.MusicNote, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f).clickable { Media.openApp(ctx) }) {
                        Text(info.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(info.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (info.durationMs > 0) {
                    val progress = if (tick >= 0) (info.currentPosition().toFloat() / info.durationMs).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { Media.previous() }) { Icon(Icons.Outlined.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.width(20.dp))
                    FilledIconButton(
                        onClick = { Media.playPause() },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) {
                        Icon(if (info.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, "Reproduzir/pausar", modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(20.dp))
                    IconButton(onClick = { Media.next() }) { Icon(Icons.Outlined.SkipNext, "Seguinte", tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
            }
        }
    }
}

/** Casa: luzes e tomadas do Google Home (Tapo, Govee...). Toque liga/desliga; toque longo regula o brilho. */
@Composable
private fun HomeCard() {
    val ctx = LocalContext.current
    var brightnessFor by remember { mutableStateOf<HomeDev?>(null) }
    LaunchedEffect(Unit) { runCatching { HomeRepo.client(ctx) } }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("Casa", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (HomeRepo.permitted == true) {
                val onCount = HomeRepo.devices.count { it.on == true }
                Text(if (onCount == 0) "Tudo desligado" else "$onCount ligado(s)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        when (HomeRepo.permitted) {
            false -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    HomeRepo.error?.let { "Erro: $it" } ?: "Autoriza o Painel a ver e controlar os dispositivos do Google Home.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = { HomeRepo.requestPermission(ctx) }) { Text("Ligar ao Google Home") }
            }
            null -> Text("A ligar ao Google Home…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            true -> if (HomeRepo.devices.isEmpty()) {
                val msg = when {
                    HomeRepo.error != null -> "Erro: ${HomeRepo.error}"
                    !HomeRepo.loaded -> "A carregar dispositivos…"
                    HomeRepo.others.isNotEmpty() -> "Encontrei ${HomeRepo.others.size} dispositivo(s), mas nenhuma luz ou tomada: ${HomeRepo.others.take(4).joinToString()}"
                    else -> "Não há dispositivos autorizados. Toca em Ligar e escolhe as luzes."
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (HomeRepo.loaded || HomeRepo.error != null) {
                        FilledTonalButton(onClick = { HomeRepo.requestPermission(ctx) }) { Text("Ligar") }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(HomeRepo.devices, key = { it.id }) { d -> DeviceTile(d, onLong = { brightnessFor = d }) }
                }
            }
        }
    }
    brightnessFor?.let { d ->
        val live = HomeRepo.devices.firstOrNull { it.id == d.id } ?: d
        var value by remember(d.id) { mutableStateOf((live.levelPct ?: 100).toFloat()) }
        AlertDialog(
            onDismissRequest = { brightnessFor = null },
            title = { Text(live.name) },
            text = {
                Column {
                    Text("Brilho: ${value.toInt()}%")
                    Slider(value = value, onValueChange = { value = it }, onValueChangeFinished = { HomeRepo.setLevel(live, value.toInt()) }, valueRange = 1f..100f)
                }
            },
            confirmButton = { TextButton(onClick = { brightnessFor = null }) { Text("Fechar") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DeviceTile(d: HomeDev, onLong: () -> Unit) {
    val on = d.on == true
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .width(168.dp)
            .height(92.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = { HomeRepo.toggle(d) }, onLongClick = { if (d.levelPct != null) onLong() }),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(
                if (d.isLight) (if (on) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb) else Icons.Outlined.Power,
                null,
                tint = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(d.name, style = MaterialTheme.typography.titleSmall, color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    d.room.ifBlank { null },
                    when { d.on == null -> "—"; !on -> "Desligada"; d.levelPct != null -> "${d.levelPct}%"; else -> "Ligada" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (on) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Cena Noite: ecrã quase negro, relógio discreto e o próximo comboio. */
@Composable
private fun BoxScope.NightView(now: LocalDateTime, trains: List<Departure>?) {
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            now.format(hm),
            style = TextStyle(fontFamily = googleSansFlexAt(300, 100f), fontSize = 160.sp),
            color = Color(0xFF5F6368),
        )
        trains?.firstOrNull()?.let { t ->
            Text(
                "Próximo comboio ${hhmm(t.etaMs)} · ${TText.title(t.destination)}",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF3C4043),
            )
        }
    }
}
