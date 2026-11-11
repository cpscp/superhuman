package pt.carlos.painel.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/** Aloja os widgets de qualquer app (Material You Widgets, Calendar, YouTube...). */
class PainelHost(ctx: Context) : AppWidgetHost(ctx, HOST_ID) {
    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
        PainelHostView(context)

    companion object { const val HOST_ID = 1026 }
}

/** Vista de widget que deteta o toque longo (para entrar no modo de edição) sem estragar os toques normais. */
class PainelHostView(ctx: Context) : AppWidgetHostView(ctx) {
    var onLongPress: (() -> Unit)? = null
    private val handlerMain = Handler(Looper.getMainLooper())
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longFired = false
    private val longRunnable = Runnable {
        longFired = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longFired = false
                downX = ev.x; downY = ev.y
                handlerMain.postDelayed(longRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) handlerMain.removeCallbacks(longRunnable)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handlerMain.removeCallbacks(longRunnable)
        }
        return longFired
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (longFired) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) longFired = false
            return true
        }
        return super.onTouchEvent(event)
    }
}
