package com.carlos.liquid.lock

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.carlos.liquid.data.BatteryRepo
import com.carlos.liquid.data.CalendarRepo
import com.carlos.liquid.data.HealthRepo
import com.carlos.liquid.data.WeatherRepo
import com.carlos.liquid.ui.GlassPill
import com.carlos.liquid.ui.GlassSheet
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LiquidRoot
import kotlinx.coroutines.launch

class LockActivity : ComponentActivity() {
    private var editing by mutableStateOf(false)
    private var zone by mutableStateOf<String?>(null)
    private var openedForEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        handle(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    zone != null -> zone = null
                    editing && openedForEdit -> finishLock()
                    editing -> editing = false
                    else -> {}
                }
            }
        })
        val actions = LockActions(
            unlock = { after -> unlock(true, after) },
            camera = {
                val km = getSystemService(KeyguardManager::class.java)
                val secure = km.isKeyguardLocked
                runCatching {
                    startActivity(Intent(if (secure) MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                if (!secure) finishLock()
            },
            startAfterUnlock = { i -> unlock(true) { runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } },
            openNotif = { n ->
                unlock(true) {
                    runCatching {
                        val opts = ActivityOptions.makeBasic().apply {
                            if (Build.VERSION.SDK_INT >= 34) setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        }.toBundle()
                        n.intent?.send(this, 0, null, null, null, null, opts)
                        if (n.clearable) NotifService.dismiss(n.key)
                    }
                }
            },
            customize = { unlock(false) { editing = true } },
        )
        setContent {
            LiquidRoot(lock = true) {
                LaunchedEffect(Unit) {
                    BatteryRepo.refresh(this@LockActivity)
                    launch { WeatherRepo.refresh(this@LockActivity) }
                    launch { CalendarRepo.refresh(this@LockActivity) }
                    launch { HealthRepo.refresh(this@LockActivity) }
                }
                LockScreen(editing, actions) { zone = it }
                if (editing && zone == null) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlassPill(height = 36.dp, onClick = { zone = "wallpaper" }) { LText("Wallpaper", 15.sp, FontWeight.SemiBold, Color.White) }
                            Spacer(Modifier.weight(1f))
                            GlassPill(height = 36.dp, onClick = { if (openedForEdit) finishLock() else editing = false }) { LText("OK", 15.sp, FontWeight.Bold, Color.White) }
                        }
                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp), horizontalArrangement = Arrangement.Center) {
                            GlassPill(height = 36.dp, onClick = { zone = "notifs" }) { LText("Notificações", 15.sp, FontWeight.SemiBold, Color.White) }
                            Spacer(Modifier.width(8.dp))
                            GlassPill(height = 36.dp, onClick = { zone = "controls" }) { LText("Controlos", 15.sp, FontWeight.SemiBold, Color.White) }
                        }
                    }
                }
                val z = zone
                GlassSheet(z != null, onDismiss = { zone = null }) { if (z != null) LockZoneSheet(z) { zone = null } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        if (i?.getBooleanExtra("edit", false) == true) { editing = true; openedForEdit = true }
    }

    /** Pede o desbloqueio (PIN/impressão digital) e depois executa [after]. */
    private fun unlock(finishAfter: Boolean, after: (() -> Unit)? = null) {
        val km = getSystemService(KeyguardManager::class.java)
        val done = {
            after?.invoke()
            if (finishAfter) finishLock()
        }
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { lifecycleScope.launch { done() } }
                override fun onDismissError() { if (finishAfter) finishLock() }
                override fun onDismissCancelled() {}
            })
        } else done()
    }

    private fun finishLock() {
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, android.R.anim.fade_out)
    }
}
