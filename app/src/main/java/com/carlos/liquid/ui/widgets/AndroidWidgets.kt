package com.carlos.liquid.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.carlos.liquid.ui.LText

/** Alojamento dos widgets Android normais (ex.: os da app Partidas). */
object WidgetHost {
    const val HOST_ID = 2611
    lateinit var host: AppWidgetHost
    lateinit var manager: AppWidgetManager
    private var ready = false

    fun init(ctx: Context) {
        if (ready) return
        manager = AppWidgetManager.getInstance(ctx)
        host = AppWidgetHost(ctx.applicationContext, HOST_ID)
        ready = true
    }

    fun start() = runCatching { host.startListening() }
    fun stop() = runCatching { host.stopListening() }

    fun providers(ctx: Context): List<AppWidgetProviderInfo> = runCatching {
        manager.installedProviders.sortedBy { it.loadLabel(ctx.packageManager).lowercase() }
    }.getOrDefault(emptyList())

    fun delete(id: Int) { if (id >= 0) runCatching { host.deleteAppWidgetId(id) } }
}

@Composable
fun AndroidWidget(appWidgetId: Int, w: Dp, h: Dp) {
    val info = runCatching { WidgetHost.manager.getAppWidgetInfo(appWidgetId) }.getOrNull()
    if (info == null) {
        WidgetFrame(w, h) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LText("Widget indisponível", 13.sp, color = LocalWStyle.current.fg2) } }
        return
    }
    AndroidView(
        modifier = Modifier.size(w, h).clip(RoundedCornerShape(WidgetCorner)),
        factory = { ctx -> WidgetHost.host.createView(ctx.applicationContext, appWidgetId, info).apply { setPadding(0, 0, 0, 0) } },
        update = { v ->
            @Suppress("DEPRECATION")
            runCatching { v.updateAppWidgetSize(Bundle(), w.value.toInt(), h.value.toInt(), w.value.toInt(), h.value.toInt()) }
        },
    )
}
