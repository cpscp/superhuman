package com.carlos.liquid.lock

import android.app.KeyguardManager
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import android.hardware.camera2.CameraManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.liquid.data.BatteryRepo
import com.carlos.liquid.data.CalendarRepo
import com.carlos.liquid.data.ClockFont
import com.carlos.liquid.data.HealthRepo
import com.carlos.liquid.data.InlineKind
import com.carlos.liquid.data.LockControl
import com.carlos.liquid.data.LockSettings
import com.carlos.liquid.data.LockWidget
import com.carlos.liquid.data.NotifStyle
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.WeatherRepo
import com.carlos.liquid.glass.LocalGlass
import com.carlos.liquid.glass.liquidGlass
import com.carlos.liquid.ui.Fonts
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.home.IconImage
import com.carlos.liquid.ui.noRippleClick
import com.carlos.liquid.ui.rememberHaptic
import com.carlos.liquid.ui.widgets.AnalogDial
import com.carlos.liquid.ui.widgets.PT
import com.carlos.liquid.ui.widgets.Rings
import com.carlos.liquid.ui.widgets.cityOf
import com.carlos.liquid.ui.widgets.clockOf
import com.carlos.liquid.ui.widgets.eventTime
import com.carlos.liquid.ui.widgets.minutesTo
import com.carlos.liquid.ui.widgets.rememberDepartures
import com.carlos.liquid.ui.widgets.rememberNow
import com.carlos.liquid.ui.widgets.weatherIcon
import com.carlos.liquid.data.IconStyle
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.icons.IconSpec
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Ações que o ecrã bloqueado pede à atividade. */
class LockActions(
    val unlock: (after: (() -> Unit)?) -> Unit,
    val camera: () -> Unit,
    val startAfterUnlock: (android.content.Intent) -> Unit,
    val openNotif: (LNotif) -> Unit,
    val customize: () -> Unit,
)

fun clockWeight(ls: LockSettings): FontWeight = when (ls.clockFont) {
    ClockFont.INTER, ClockFont.CUSTOM -> FontWeight(((300 + ls.clockWeight * 600) / 100f).roundToInt() * 100)
    ClockFont.INTER_LIGHT -> FontWeight.Light
    ClockFont.ROUNDED -> if (ls.clockWeight > 0.5f) FontWeight.Black else FontWeight.Bold
    ClockFont.CONDENSED -> if (ls.clockWeight > 0.5f) FontWeight.Bold else FontWeight.Medium
    ClockFont.SERIF -> FontWeight.Normal
    ClockFont.MONO -> FontWeight.Medium
}

fun clockBaseSize(f: ClockFont) = when (f) { ClockFont.CONDENSED -> 128f; ClockFont.SERIF -> 112f; ClockFont.MONO -> 84f; else -> 100f }

/** Relógio grande; com Liquid Glass os números refratam o wallpaper. */
@Composable
fun LockClock(ls: LockSettings, now: Long, modifier: Modifier = Modifier) {
    val env = LocalGlass.current
    val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    val size = clockBaseSize(ls.clockFont) * ls.clockSize
    val family = Fonts.clock(ls.clockFont)
    val weight = clockWeight(ls)
    if (!ls.clockGlass || env.source == null) {
        LText(time, size.sp, weight, Color(ls.clockColor), modifier, family = family, maxLines = 1, letterSpacing = (-size * 0.035f).sp, lineHeight = (size * 1.05f).sp)
        return
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shader = remember(env.source) {
        BitmapShader(env.source!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val m = remember { Matrix() }
    Box(modifier) {
        // aresta luminosa ligeiramente deslocada (efeito de espessura do vidro)
        LText(time, size.sp, weight, Color.White.copy(alpha = 0.55f), Modifier.offset(y = (-1.5).dp), family = family, maxLines = 1, letterSpacing = (-size * 0.035f).sp, lineHeight = (size * 1.05f).sp)
        LText(
            time, size.sp, weight, Color.White,
            Modifier
                .onGloballyPositioned { origin = it.positionInWindow() }
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val b = env.source!!
                    m.setScale(env.screenW / b.width.toFloat(), env.screenH / b.height.toFloat())
                    m.postTranslate(-origin.x, -origin.y + 18f)
                    shader.setLocalMatrix(m)
                    drawRect(ShaderBrush(shader), blendMode = BlendMode.SrcIn)
                    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.4f))), blendMode = BlendMode.SrcAtop)
                },
            family = family, maxLines = 1, letterSpacing = (-size * 0.035f).sp, lineHeight = (size * 1.05f).sp,
        )
    }
}

fun inlineText(k: InlineKind, ctx: android.content.Context, now: Long): String {
    val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
    val date = t.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT)).replaceFirstChar { it.uppercase() }
    val shortDate = t.format(DateTimeFormatter.ofPattern("EEE, d MMM", PT)).replaceFirstChar { it.uppercase() }
    return when (k) {
        InlineKind.DATE -> date
        InlineKind.WEATHER -> WeatherRepo.state.value?.let { "$shortDate · ${it.temp}° ${WeatherRepo.describe(it.code)}" } ?: date
        InlineKind.NEXT_EVENT -> CalendarRepo.state.value.firstOrNull { it.end > System.currentTimeMillis() }?.let { "$shortDate · ${it.title}" } ?: date
        InlineKind.ALARM -> CalendarRepo.nextAlarm(ctx)?.let { "$shortDate · ⏰ " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: date
        InlineKind.STEPS -> "$shortDate · ${HealthRepo.state.value.steps} passos"
        InlineKind.BATTERY -> BatteryRepo.state.value.firstOrNull()?.let { "$shortDate · ${it.level}%" } ?: date
    }
}

fun widgetSlots(w: LockWidget) = when (w) { LockWidget.NEXT_EVENT, LockWidget.TRANSIT -> 2; else -> 1 }

@Composable
fun LockWidgetView(w: LockWidget, ls: LockSettings) {
    val ctx = LocalContext.current
    val fg = Color.White
    val now = rememberNow(30_000)
    val wide = widgetSlots(w) == 2
    Box(
        Modifier.height(66.dp).width(if (wide) 140.dp else 66.dp).liquidGlass(corner = if (wide) 22.dp else 33.dp, bezel = 14.dp, strength = 0.7f),
        contentAlignment = Alignment.Center,
    ) {
        when (w) {
            LockWidget.BATTERY -> {
                val b by BatteryRepo.state.collectAsState()
                val lvl = b.firstOrNull()?.level ?: 0
                GaugeRing(lvl / 100f, fg) { LText("$lvl", 18.sp, FontWeight.Bold, fg) }
            }
            LockWidget.WEATHER -> {
                val wx by WeatherRepo.state.collectAsState()
                val x = wx
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (x != null) {
                        Icon(weatherIcon(x.code, x.isDay), null, tint = fg, modifier = Modifier.size(20.dp))
                        LText("${x.temp}°", 17.sp, FontWeight.Bold, fg)
                    } else LText("—", 17.sp, FontWeight.Bold, fg)
                }
            }
            LockWidget.ACTIVITY -> {
                val h by HealthRepo.state.collectAsState()
                val s = Store.s
                Rings(50.dp, listOf((h.kcal / s.moveGoal).toFloat(), h.exerciseMin.toFloat() / s.exerciseGoal, h.steps.toFloat() / s.stepsGoal),
                    listOf(Color.White, Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.5f)), 6.dp)
            }
            LockWidget.STEPS -> {
                val h by HealthRepo.state.collectAsState()
                GaugeRing((h.steps.toFloat() / Store.s.stepsGoal).coerceIn(0f, 1f), fg) {
                    LText(if (h.steps >= 1000) "${h.steps / 1000}k" else "${h.steps}", 16.sp, FontWeight.Bold, fg)
                }
            }
            LockWidget.ALARM -> {
                val a = CalendarRepo.nextAlarm(ctx)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Alarm, null, tint = fg, modifier = Modifier.size(18.dp))
                    LText(a?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—", 14.sp, FontWeight.Bold, fg)
                }
            }
            LockWidget.WORLD_CLOCK -> {
                val z = Store.s.worldClocks.getOrNull(1) ?: "Europe/London"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.runtime.CompositionLocalProvider(com.carlos.liquid.ui.widgets.LocalWStyle provides com.carlos.liquid.ui.widgets.WStyle(IconStyle.CLEAR, true, Color.White)) {
                        AnalogDial(36.dp, rememberNow(), ZoneId.of(z), numbers = false, seconds = false)
                    }
                    LText(cityOf(z).take(3).uppercase(), 11.sp, FontWeight.Bold, fg)
                }
            }
            LockWidget.CALENDAR_DAY -> {
                val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LText(t.format(DateTimeFormatter.ofPattern("EEE", PT)).uppercase().take(3), 11.sp, FontWeight.Bold, fg)
                    LText(t.dayOfMonth.toString(), 22.sp, FontWeight.Bold, fg)
                }
            }
            LockWidget.NEXT_EVENT -> {
                val ev by CalendarRepo.state.collectAsState()
                val e = ev.firstOrNull { it.end > System.currentTimeMillis() }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Event, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        LText(e?.title ?: "Sem eventos", 14.sp, FontWeight.Bold, fg, maxLines = 1)
                        LText(e?.let { eventTime(it) } ?: "Calendário livre", 12.sp, color = fg.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
            }
            LockWidget.TRANSIT -> {
                val parts = ls.transitStop.split(":", limit = 3)
                val (deps, _) = rememberDepartures(parts.getOrNull(0) ?: "carris", parts.getOrNull(1).orEmpty(), "")
                val d = deps.firstOrNull()
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    LText(parts.getOrNull(2)?.ifBlank { null } ?: "Escolhe a paragem", 12.sp, FontWeight.SemiBold, fg.copy(alpha = 0.85f), maxLines = 1)
                    if (d != null) {
                        val m = minutesTo(d.epoch)
                        LText("${d.line} · ${if (m <= 0) "agora" else if (m < 60) "$m min" else clockOf(d.epoch)}", 15.sp, FontWeight.Bold, fg, maxLines = 1)
                    } else LText("—", 15.sp, FontWeight.Bold, fg)
                }
            }
        }
    }
}

@Composable
private fun GaugeRing(f: Float, c: Color, content: @Composable () -> Unit) {
    Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(50.dp)) {
            val sw = 5.dp.toPx()
            drawArc(c.copy(alpha = 0.3f), 135f, 270f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(c, 135f, 270f * f.coerceIn(0f, 1f), false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

fun controlIcon(c: LockControl, torchOn: Boolean, dnd: Boolean): ImageVector? = when (c) {
    LockControl.NONE -> null
    LockControl.FLASHLIGHT -> if (torchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff
    LockControl.CAMERA -> Icons.Rounded.PhotoCamera
    LockControl.CLOCK -> Icons.Rounded.Alarm
    LockControl.CALCULATOR -> Icons.Rounded.Calculate
    LockControl.DO_NOT_DISTURB -> Icons.Rounded.DoNotDisturbOn
}

fun ago(t: Long): String {
    val m = (System.currentTimeMillis() - t) / 60000
    return when { m < 1 -> "agora"; m < 60 -> "há $m min"; m < 1440 -> "há ${m / 60} h"; else -> "há ${m / 1440} d" }
}

@Composable
fun LockScreen(editing: Boolean, actions: LockActions, onEditZone: (String) -> Unit) {
    val s by Store.settings.collectAsState()
    val ls = s.lock
    val ctx = LocalContext.current
    val now = rememberNow(1000)
    val notifs by NotifService.notifs.collectAsState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = rememberHaptic()
    val km = remember { ctx.getSystemService(KeyguardManager::class.java) }
    val locked = km.isKeyguardLocked && km.isKeyguardSecure
    var expanded by remember { mutableStateOf(false) }
    val drag = remember { Animatable(0f) }
    var torch by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(NotifService.dnd()) }
    val cam = remember { ctx.getSystemService(CameraManager::class.java) }
    DisposableEffect(Unit) {
        val cb = object : CameraManager.TorchCallback() { override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torch = enabled } }
        runCatching { cam.registerTorchCallback(cb, null) }
        onDispose { runCatching { cam.unregisterTorchCallback(cb) } }
    }
    val spec = remember { IconSpec(IconStyle.DEFAULT, false, 0, 96) }

    fun control(c: LockControl) {
        haptic(true)
        when (c) {
            LockControl.FLASHLIGHT -> runCatching { cam.setTorchMode(cam.cameraIdList.first { id -> cam.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }, !torch) }
            LockControl.CAMERA -> actions.camera()
            LockControl.CLOCK -> actions.startAfterUnlock(android.content.Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))
            LockControl.CALCULATOR -> actions.startAfterUnlock(android.content.Intent.makeMainSelectorActivity(android.content.Intent.ACTION_MAIN, android.content.Intent.CATEGORY_APP_CALCULATOR))
            LockControl.DO_NOT_DISTURB -> { NotifService.toggleDnd(); dnd = !dnd }
            LockControl.NONE -> {}
        }
    }

    val threshold = with(density) { 140.dp.toPx() }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(editing) {
                if (editing) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (drag.value < -threshold) { drag.animateTo(-size.height.toFloat(), spring(stiffness = 600f)); actions.unlock(null); drag.snapTo(0f) }
                            else drag.animateTo(0f, spring(dampingRatio = 0.7f))
                        }
                    },
                ) { ch, dy -> ch.consume(); scope.launch { drag.snapTo((drag.value + dy).coerceAtMost(0f)) } }
            }
            .pointerInput(editing) {
                detectTapGestures(onLongPress = { if (!editing) { haptic(true); actions.customize() } }, onTap = { expanded = false })
            },
    ) {
        val p = (-drag.value / threshold).coerceIn(0f, 1.5f)
        Column(
            Modifier.fillMaxSize()
                .graphicsLayer { translationY = drag.value * 0.6f; alpha = (1f - p * 0.6f).coerceIn(0f, 1f); val sc = 1f - p * 0.04f; scaleX = sc; scaleY = sc }
                .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(6.dp))
            Icon(if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            EditZone(editing, "inline", onEditZone) {
                LText(inlineText(ls.inline, ctx, now), 19.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.92f), maxLines = 1, shadow = true)
            }
            EditZone(editing, "clock", onEditZone) { LockClock(ls, now) }
            Spacer(Modifier.height(6.dp))
            if (ls.widgets.isNotEmpty() || editing) EditZone(editing, "widgets", onEditZone) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var slots = 0
                    ls.widgets.forEach { w -> if (slots + widgetSlots(w) <= 4) { slots += widgetSlots(w); LockWidgetView(w, ls) } }
                    if (ls.widgets.isEmpty()) LText("+ Adicionar widgets", 15.sp, FontWeight.SemiBold, Color.White, Modifier.padding(16.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            if (!editing) Notifications(notifs, ls, expanded || ls.notifStyle == NotifStyle.LIST, locked, spec, onExpand = { expanded = true }, onOpen = actions.openNotif)
            else EditZone(true, "notifs", onEditZone) { LText("Notificações: " + when (ls.notifStyle) { NotifStyle.COUNT -> "contagem"; NotifStyle.STACK -> "pilha"; NotifStyle.LIST -> "lista" }, 15.sp, FontWeight.SemiBold, Color.White, Modifier.padding(16.dp)) }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                ControlButton(controlIcon(ls.leftControl, torch, dnd), active = (ls.leftControl == LockControl.FLASHLIGHT && torch) || (ls.leftControl == LockControl.DO_NOT_DISTURB && dnd), editing, { onEditZone("controls") }) { control(ls.leftControl) }
                Spacer(Modifier.weight(1f))
                ControlButton(controlIcon(ls.rightControl, torch, dnd), active = (ls.rightControl == LockControl.FLASHLIGHT && torch) || (ls.rightControl == LockControl.DO_NOT_DISTURB && dnd), editing, { onEditZone("controls") }) { control(ls.rightControl) }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(134.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EditZone(editing: Boolean, zone: String, onEdit: (String) -> Unit, content: @Composable () -> Unit) {
    if (!editing) { content(); return }
    Box(
        Modifier.padding(4.dp).clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .noRippleClick { onEdit(zone) }.padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ControlButton(icon: ImageVector?, active: Boolean, editing: Boolean, onEdit: () -> Unit, onClick: () -> Unit) {
    if (icon == null) { Spacer(Modifier.size(52.dp)); return }
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier.size(52.dp).graphicsLayer { val sc = if (pressed) 1.18f else 1f; scaleX = sc; scaleY = sc }
            .liquidGlass(corner = 26.dp, tint = if (active) Color.White.copy(alpha = 0.9f) else null, bezel = 12.dp)
            .pointerInput(editing) {
                detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { if (editing) onEdit() }, onLongPress = { if (!editing) onClick() })
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun Notifications(list: List<LNotif>, ls: LockSettings, expanded: Boolean, locked: Boolean, spec: IconSpec, onExpand: () -> Unit, onOpen: (LNotif) -> Unit) {
    if (list.isEmpty()) return
    val showText = ls.showPreviews || !locked
    when {
        !expanded && ls.notifStyle == NotifStyle.COUNT -> {
            Box(Modifier.liquidGlass(corner = 20.dp, bezel = 10.dp).noRippleClick(onExpand).padding(horizontal = 16.dp, vertical = 10.dp)) {
                LText("${list.size} ${if (list.size == 1) "notificação" else "notificações"}", 15.sp, FontWeight.SemiBold, Color.White)
            }
        }
        !expanded -> {
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).noRippleClick(onExpand)) {
                if (list.size > 2) Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = 16.dp).height(60.dp).liquidGlass(corner = 22.dp, strength = 0.4f))
                if (list.size > 1) Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).offset(y = 8.dp).height(70.dp).liquidGlass(corner = 22.dp, strength = 0.5f))
                NotifCard(list.first(), showText, spec, onOpen, if (list.size > 1) "+${list.size - 1}" else null)
            }
            Spacer(Modifier.height(16.dp))
        }
        else -> Column(
            Modifier.fillMaxWidth().height((list.size.coerceAtMost(4) * 86).dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { list.take(30).forEach { n -> androidx.compose.runtime.key(n.key) { NotifCard(n, showText, spec, onOpen, null) } } }
    }
}

@Composable
private fun NotifCard(n: LNotif, showText: Boolean, spec: IconSpec, onOpen: (LNotif) -> Unit, more: String?) {
    var dx by remember { mutableFloatStateOf(0f) }
    val appKey = remember(n.pkg) { AppRepo.apps.value.firstOrNull { it.pkg == n.pkg }?.key }
    Row(
        Modifier.fillMaxWidth()
            .offset { IntOffset(dx.roundToInt(), 0) }
            .graphicsLayer { alpha = (1f - kotlin.math.abs(dx) / 700f).coerceIn(0f, 1f) }
            .liquidGlass(corner = 24.dp, bezel = 14.dp, strength = 0.6f)
            .pointerInput(n.key) {
                detectHorizontalDragGestures(onDragEnd = { if (kotlin.math.abs(dx) > 260f && n.clearable) NotifService.dismiss(n.key) else dx = 0f }) { _, d -> dx += d }
            }
            .noRippleClick { onOpen(n) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp)) { if (appKey != null) IconImage(appKey, 38.dp, spec) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row {
                LText(n.title, 15.sp, FontWeight.SemiBold, Color.White, Modifier.weight(1f), maxLines = 1)
                LText(more ?: ago(n.time), 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
            LText(if (showText) n.text else n.app, 14.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 2)
        }
    }
}

@Suppress("unused") private val keepAlign = TextAlign.Center
@Suppress("unused") private val keepPath = PathEffect.cornerPathEffect(1f)
@Suppress("unused") private fun keepBox(b: BoxScope) = b
