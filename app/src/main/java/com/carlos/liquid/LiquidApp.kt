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
        runCatching { Fonts.loadCustom(this) }
        AppRepo.init(this)
        WidgetHost.init(this)
        LockController.register(this)
    }
}
