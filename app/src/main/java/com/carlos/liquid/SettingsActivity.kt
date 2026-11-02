package com.carlos.liquid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carlos.liquid.settings.SettingsApp
import com.carlos.liquid.ui.LiquidRoot

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LiquidRoot(drawWallpaper = false) { SettingsApp(onClose = { finish() }); com.carlos.liquid.ui.CrashReport() } }
    }
}
