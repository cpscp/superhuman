package com.carlos.superhuman

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.widget.WidgetSync
import com.carlos.superhuman.work.Workers

class SuperhumanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Store.onChanged = { ctx -> WidgetSync.refresh(ctx) }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_REVIEW, "Revisões", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_INVEST, "Invest diário", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_MONEY, "Despesas detetadas", NotificationManager.IMPORTANCE_LOW))
        Workers.scheduleAll(this)
    }

    companion object {
        const val CHANNEL_REVIEW = "reviews"
        const val CHANNEL_INVEST = "invest"
        const val CHANNEL_MONEY = "money"
    }
}
