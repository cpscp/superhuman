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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.graphics.Brush
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
                LockScreen(style = PainelApp.instance.prefs.clockStyle, onUnlock = ::unlock)
            }
        }
    }

    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { finish() }
            })
        } else finish()
    }
}

private val pt = Locale("pt", "PT")
private val glow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 2f), blurRadius = 12f)

@Composable
private fun LockScreen(style: Int, onUnlock: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var battery by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (b != null) {
                val level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val pct = if (level >= 0) level * 100 / scale else -1
                battery = if (pct < 0) "" else if (charging) "$pct% · a carregar" else "$pct%"
            }
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    val hh = now.format(DateTimeFormatter.ofPattern("HH"))
    val mm = now.format(DateTimeFormatter.ofPattern("mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", pt)).replaceFirstChar { it.titlecase(pt) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.35f))))
            .pointerInput(Unit) {
                var t = 0f
                detectVerticalDragGestures(onDragStart = { t = 0f }) { _, d ->
                    t += d
                    if (t < -150f) { t = 0f; onUnlock() }
                }
            }
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(Modifier.padding(start = 56.dp, top = 40.dp)) {
            Text(date, style = MaterialTheme.typography.headlineSmall.copy(shadow = glow), color = Color.White)
            Spacer(Modifier.height(8.dp))
            when (style) {
                1 -> Text(
                    "$hh:$mm",
                    style = TextStyle(fontFamily = googleSansFlexAt(200), fontSize = 150.sp, lineHeight = 150.sp, shadow = glow),
                    color = Color.White,
                )
                2 -> Text(
                    "$hh:$mm",
                    style = TextStyle(fontFamily = googleSansFlexAt(700), fontSize = 130.sp, lineHeight = 130.sp, shadow = glow),
                    color = Color.White,
                )
                else -> Column {
                    val big = TextStyle(fontFamily = googleSansFlexAt(400), fontSize = 170.sp, lineHeight = 150.sp, shadow = glow)
                    Text(hh, style = big, color = MaterialTheme.colorScheme.primaryContainer)
                    Text(mm, style = big, color = Color.White)
                }
            }
            if (battery.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(battery, style = MaterialTheme.typography.titleMedium.copy(shadow = glow), color = Color.White.copy(alpha = 0.9f))
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Lock, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text("Desliza para cima para desbloquear", style = MaterialTheme.typography.labelLarge.copy(shadow = glow), color = Color.White)
        }
    }
}
