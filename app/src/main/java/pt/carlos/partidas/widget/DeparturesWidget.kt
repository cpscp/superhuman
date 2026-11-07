package pt.carlos.partidas.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import pt.carlos.partidas.data.DepartureCache
import pt.carlos.partidas.data.WidgetLinks

class DeparturesWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetRenderer.render(ctx, mgr, it) }
        Scheduler.ensure(ctx)
        RefreshWorker.refresh(ctx, null, replace = false)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        WidgetRenderer.render(ctx, mgr, id)
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ids.forEach { WidgetLinks.remove(ctx, it) }
    }

    override fun onEnabled(ctx: Context) = Scheduler.ensure(ctx)

    override fun onDisabled(ctx: Context) = Scheduler.cancel(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        when (intent.action) {
            ACTION_REFRESH -> WidgetLinks.get(ctx, id)?.let { boardId ->
                DepartureCache.markRefreshing(ctx, boardId)
                WidgetRenderer.renderBoard(ctx, boardId)
                RefreshWorker.refresh(ctx, arrayOf(boardId))
            }

            // A cada mudança de minuto: redesenha (relógio e "x min", sem rede) e, com o ecrã
            // ligado, se os dados tiverem mais de 3 min, pede dados novos.
            ACTION_TICK -> {
                if (WidgetRenderer.allIds(ctx).isEmpty()) return
                Scheduler.scheduleNextTick(ctx)
                WidgetRenderer.renderAll(ctx)
                val now = System.currentTimeMillis()
                val stale = RefreshWorker.linkedBoards(ctx).any { b ->
                    !DepartureCache.isRefreshing(ctx, b) &&
                        (DepartureCache.get(ctx, b)?.fetchedAt ?: 0L) < now - STALE_MS
                }
                // com o ecrã desligado (ex.: música a tocar) só redesenha; não vai à rede
                val screenOn = ctx.getSystemService(android.os.PowerManager::class.java).isInteractive
                if (stale && screenOn) RefreshWorker.refresh(ctx, null, replace = false)
            }

            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetRenderer.renderAll(ctx)
                Scheduler.scheduleNextTick(ctx)
            }

            // O launcher confirmou o widget pedido a partir da app.
            ACTION_PIN_DONE -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                WidgetLinks.adoptPending(ctx, id)
                WidgetRenderer.render(ctx, AppWidgetManager.getInstance(ctx), id)
                WidgetLinks.get(ctx, id)?.let { RefreshWorker.refresh(ctx, arrayOf(it)) }
                Scheduler.ensure(ctx)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "pt.carlos.partidas.REFRESH"
        const val ACTION_TICK = "pt.carlos.partidas.TICK"
        const val ACTION_PIN_DONE = "pt.carlos.partidas.PIN_DONE"
        private const val STALE_MS = 3 * 60_000L
    }
}

object Scheduler {
    private fun tickIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, 99,
        Intent(ctx, DeparturesWidget::class.java).setAction(DeparturesWidget.ACTION_TICK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun ensure(ctx: Context) {
        scheduleNextTick(ctx)
        RefreshWorker.schedulePeriodic(ctx)
    }

    /**
     * Próxima mudança de minuto (o relógio faz parte da imagem do widget).
     * Alarme RTC sem WAKEUP: com o ecrã desligado não acorda o telefone; quando o ecrã liga,
     * o alarme em atraso dispara logo e o relógio fica certo.
     */
    fun scheduleNextTick(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val next = now - now % 60_000L + 60_000L + 200L
        val pi = tickIntent(ctx)
        if (am.canScheduleExactAlarms()) am.setExact(AlarmManager.RTC, next, pi)
        else am.setWindow(AlarmManager.RTC, next, 15_000L, pi)
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(tickIntent(ctx))
        RefreshWorker.cancelAll(ctx)
    }
}
