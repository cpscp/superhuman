@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pt.carlos.painel.ui

import android.app.KeyguardManager
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import pt.carlos.painel.data.Weather
import pt.carlos.painel.data.WeatherNow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.carlos.painel.PainelApp
import pt.carlos.painel.theme.PainelTheme
import pt.carlos.painel.theme.googleSansFlexAt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ecrã de bloqueio do Painel: relógio grande ao estilo Pixel, data e bateria. */
class LockScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            PainelTheme(PainelApp.instance.prefs) {
                BackHandler { }   // o botão Voltar não salta o bloqueio
                LockScreen(style = PainelApp.instance.prefs.clockStyle, onUnlock = { unlock(null) }, onHome = { unlock(homeIntent()) })
            }
        }
    }

    /** Google Home (controlos da casa); a Fase 3 traz os controlos para dentro do Painel. */
    private fun homeIntent(): Intent? =
        packageManager.getLaunchIntentForPackage("com.google.android.apps.chromecast.app")

    private fun unlock(then: Intent?) {
        val done = {
            then?.let { runCatching { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            finish()
        }
        val km = getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { done() }
            })
        } else done()
    }
}

private val pt = Locale("pt", "PT")
private val glow = Shadow(color = Color.Black.copy(alpha = 0.30f), offset = Offset(0f, 2f), blurRadius = 14f)

private fun weatherIcon(w: WeatherNow): Pair<ImageVector, Color> = when (w.code) {
    0, 1 -> if (w.isDay) Icons.Outlined.WbSunny to Color(0xFFFDD663) else Icons.Outlined.NightsStay to Color(0xFFE8EAED)
    2, 3, 45, 48 -> Icons.Outlined.Cloud to Color(0xFFE8EAED)
    in 51..67, in 80..82 -> Icons.Outlined.WaterDrop to Color(0xFF8AB4F8)
    in 71..77, 85, 86 -> Icons.Outlined.AcUnit to Color(0xFFE8EAED)
    else -> Icons.Outlined.FlashOn to Color(0xFFFDD663)
}

/** Ecrã de bloqueio ao estilo Pixel 11: relógio centrado em duas linhas, data e tempo, botão de desbloqueio e atalho da Casa. */
@Composable
private fun LockScreen(style: Int, onUnlock: () -> Unit, onHome: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var weather by remember { mutableStateOf<WeatherNow?>(null) }
    var charging by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            weather = Weather.now(ctx)
            val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (b != null) {
                val level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                charging = if (isCharging && level >= 0) "A carregar · ${level * 100 / scale}%" else ""
            }
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    val hh = now.format(DateTimeFormatter.ofPattern("HH"))
    val mm = now.format(DateTimeFormatter.ofPattern("mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEE, d MMM", pt)).replaceFirstChar { it.titlecase(pt) }

    val clockStyle = when (style) {
        1 -> TextStyle(fontFamily = googleSansFlexAt(250, 100f), fontSize = 190.sp, lineHeight = 170.sp, textAlign = TextAlign.Center, shadow = glow)
        2 -> TextStyle(fontFamily = googleSansFlexAt(700, 100f), fontSize = 190.sp, lineHeight = 170.sp, textAlign = TextAlign.Center, shadow = glow)
        else -> TextStyle(fontFamily = googleSansFlexAt(500, 100f), fontSize = 200.sp, lineHeight = 176.sp, textAlign = TextAlign.Center, shadow = glow)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
            .pointerInput(Unit) {
                var t = 0f
                detectVerticalDragGestures(onDragStart = { t = 0f }) { _, d ->
                    t += d
                    if (t < -150f) { t = 0f; onUnlock() }
                }
            }
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            Modifier.align(Alignment.Center).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(hh, style = clockStyle, color = Color.White)
            Text(mm, style = clockStyle, color = Color.White)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(date, style = MaterialTheme.typography.titleMedium.copy(shadow = glow), color = Color.White)
                weather?.let { w ->
                    val (icon, tint) = weatherIcon(w)
                    Spacer(Modifier.width(10.dp))
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${w.tempC}°C", style = MaterialTheme.typography.titleMedium.copy(shadow = glow), color = Color.White)
                }
            }
            if (charging.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(charging, style = MaterialTheme.typography.labelLarge.copy(shadow = glow), color = Color.White.copy(alpha = 0.85f))
            }
        }

        // Botão de desbloqueio ao centro, em baixo.
        Surface(
            onClick = onUnlock,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp).size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.LockOpen, "Desbloquear", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }

        // Atalho da Casa (canto inferior esquerdo), como no Pixel.
        Surface(
            onClick = onHome,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 32.dp).size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Home, "Casa", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
