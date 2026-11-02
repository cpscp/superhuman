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
        Store.init(this)
        runCatching { Fonts.loadCustom(this) }
        AppRepo.init(this)
        WidgetHost.init(this)
        LockController.register(this)
    }
}
