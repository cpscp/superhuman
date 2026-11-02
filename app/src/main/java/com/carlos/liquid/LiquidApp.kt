package com.carlos.liquid

import android.app.Application
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.Store
import com.carlos.liquid.lock.LockController
import com.carlos.liquid.ui.Fonts
import com.carlos.liquid.ui.widgets.WidgetHost

class LiquidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                java.io.File(filesDir, "crash.txt").writeText(
                    "Liquid ${packageManager.getPackageInfo(packageName, 0).versionName} · ${java.util.Date()}\n" +
                        "Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MODEL} · thread ${t.name}\n\n" + e.stackTraceToString().take(6000))
            }
            prev?.uncaughtException(t, e)
        }
        Store.init(this)
        runCatching { checkLastExit() }
        runCatching { Fonts.loadCustom(this) }
        AppRepo.init(this)
        WidgetHost.init(this)
        LockController.register(this)
    }

    /** Lê porque é que o Android fechou o Liquid da última vez (inclui falhas do motor gráfico). */
    private fun checkLastExit() {
        if (android.os.Build.VERSION.SDK_INT < 30) return
        val am = getSystemService(android.app.ActivityManager::class.java)
        val list = am.getHistoricalProcessExitReasons(packageName, 0, 5)
        if (list.isEmpty()) return
        val prefs = getSharedPreferences("liquid", MODE_PRIVATE)
        val handled = prefs.getLong("exitHandled", 0L)
        prefs.edit().putLong("exitHandled", list.maxOf { it.timestamp }).commit()
        val bad = setOf(android.app.ApplicationExitInfo.REASON_CRASH, android.app.ApplicationExitInfo.REASON_CRASH_NATIVE, android.app.ApplicationExitInfo.REASON_ANR)
        val e = list.filter { it.timestamp > handled && it.reason in bad }.maxByOrNull { it.timestamp } ?: return
        val native = e.reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE
        val reason = when (e.reason) { android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "falha nativa (motor gráfico)"; android.app.ApplicationExitInfo.REASON_ANR -> "bloqueou (ANR)"; else -> "exceção" }
        val raw = runCatching { e.traceInputStream?.use { it.readBytes() } }.getOrNull()
        val trace = when {
            raw == null -> ""
            native -> printable(raw)
            else -> String(raw).take(5000)
        }
        val f = java.io.File(filesDir, "crash.txt")
        if (!f.exists() || native) f.writeText(
            "Liquid ${runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()} · ${java.util.Date(e.timestamp)}\n" +
                "Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MODEL}\n" +
                "Motivo: $reason · ${e.description ?: ""}\n" +
                (if (native) "O vidro avançado foi desligado automaticamente (Definições → Aspeto).\n" else "") + "\n" + trace)
        if (native) Store.settings { it.copy(advancedGlass = false) }
    }

    private fun printable(b: ByteArray): String {
        val out = StringBuilder(); val cur = StringBuilder()
        for (x in b) {
            val c = x.toInt() and 0xff
            if (c in 32..126) cur.append(c.toChar()) else { if (cur.length >= 6) out.append(cur).append('\n'); cur.setLength(0) }
            if (out.length > 6000) break
        }
        return out.toString()
    }
}
