package pt.carlos.partidas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import pt.carlos.partidas.R
import pt.carlos.partidas.data.BoardLook
import pt.carlos.partidas.data.BoardStore
import pt.carlos.partidas.data.DepartureCache
import pt.carlos.partidas.data.WidgetLinks
import pt.carlos.partidas.ui.MainActivity
import pt.carlos.partidas.ui.WidgetConfigActivity

/**
 * Widget = imagem do quadro desenhada pelo [BoardPainter] (Inter, tamanho real do widget)
 * + área tocável do botão ↻. Tocar no resto abre o quadro na app.
 */
object WidgetRenderer {

    fun allIds(ctx: Context): IntArray =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, DeparturesWidget::class.java))

    fun renderAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        allIds(ctx).forEach { render(ctx, mgr, it) }
    }

    /** Redesenha os widgets que mostram este quadro. */
    fun renderBoard(ctx: Context, boardId: String) {
        val mgr = AppWidgetManager.getInstance(ctx)
        WidgetLinks.widgetsFor(ctx, boardId, allIds(ctx)).forEach { render(ctx, mgr, it) }
    }

    /**
     * Tamanho real do widget em retrato (dp). O launcher dá MIN_WIDTH/MAX_HEIGHT para retrato
     * (MAX_WIDTH é a largura em paisagem — era isso que punha 4 mosaicos num widget estreito).
     */
    private fun sizeDp(ctx: Context, mgr: AppWidgetManager, id: Int): SizeF {
        val o = mgr.getAppWidgetOptions(id)
        @Suppress("DEPRECATION")
        val sizes = o.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        val portrait = ctx.resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (!sizes.isNullOrEmpty()) {
            // em retrato: o tamanho mais estreito; em paisagem: o mais largo
            val s = if (portrait) sizes.minBy { it.width } else sizes.maxBy { it.width }
            if (s.width > 0 && s.height > 0) return s
        }
        val w = if (portrait) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val h = if (portrait) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        return SizeF(if (w > 0) w.toFloat() else 340f, if (h > 0) h.toFloat() else 190f)
    }

    /** Até quantas partidas a grelha do widget mostra (com scroll). */
    private const val MAX_TILES = 10
    /** Pedaço da fila seguinte que fica à vista, para se perceber que há scroll. */
    private const val PEEK = 18f

    fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_image)
        val board = WidgetLinks.get(ctx, id)?.let { BoardStore.get(ctx, it) }
        val size = sizeDp(ctx, mgr, id)
        val now = System.currentTimeMillis()
        val cache = board?.let { DepartureCache.get(ctx, it.id) }
        val refreshing = board != null && DepartureCache.isRefreshing(ctx, board.id)
        val items = cache?.items?.filter { it.etaMs >= now - 30_000L }.orEmpty().take(MAX_TILES)

        val message = when {
            board == null -> "Toca para escolher um quadro"
            items.isNotEmpty() -> null
            cache?.error != null -> "Sem dados\n${cache.error}"
            cache == null || refreshing -> "A carregar…"
            else -> "Sem partidas nas próximas horas"
        }

        runCatching {
            BoardPainter.background(ctx, board, cache, now, size.width, size.height, refreshing, message)
        }.getOrNull()?.let { rv.setImageViewBitmap(R.id.board, it) }

        if (board == null || message != null) {
            rv.setViewVisibility(R.id.grid, View.GONE)
        } else {
            val g = BoardPainter.grid(size.width, size.height)
            val col = BoardLook.colors(board.mode)
            // se há mais partidas do que as que cabem, a última fila fica a espreitar (há scroll)
            val tileH = if (items.size > g.cols * g.rows)
                (g.h - BoardPainter.GAP * g.rows - PEEK) / g.rows
            else g.tileH
            val builder = RemoteViews.RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                .setViewTypeCount(1)
            items.forEachIndexed { i, d ->
                val item = RemoteViews(ctx.packageName, R.layout.widget_tile)
                runCatching { BoardPainter.tile(ctx, d, i == 0, col, now, g.tileW, tileH) }
                    .getOrNull()?.let { item.setImageViewBitmap(R.id.tile, it) }
                item.setOnClickFillInIntent(R.id.tile, Intent())
                builder.addItem((d.etaMs / 1000L) * 31 + (d.badge + d.destination + d.tags).hashCode().toLong(), item)
            }
            rv.setViewVisibility(R.id.grid, View.VISIBLE)
            rv.setViewLayoutMargin(R.id.grid, RemoteViews.MARGIN_START, g.x, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutMargin(R.id.grid, RemoteViews.MARGIN_TOP, g.y, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutMargin(R.id.grid, RemoteViews.MARGIN_END, BoardPainter.PAD_END, TypedValue.COMPLEX_UNIT_DIP)
            rv.setViewLayoutMargin(R.id.grid, RemoteViews.MARGIN_BOTTOM, BoardPainter.PAD_BOTTOM, TypedValue.COMPLEX_UNIT_DIP)
            rv.setInt(R.id.grid, "setNumColumns", g.cols)
            rv.setRemoteAdapter(R.id.grid, builder.build())
            rv.setPendingIntentTemplate(R.id.grid, openTemplate(ctx, board.id))
        }

        if (board == null) {
            rv.setViewVisibility(R.id.refresh, View.GONE)
            rv.setOnClickPendingIntent(R.id.card, configIntent(ctx, id))
        } else {
            rv.setViewVisibility(R.id.refresh, View.VISIBLE)
            // a área do ↻ acompanha a largura da coluna de informação
            val start = BoardPainter.infoWidth(size.width) - 18f
            rv.setViewLayoutMargin(R.id.refresh, RemoteViews.MARGIN_START, start, TypedValue.COMPLEX_UNIT_DIP)
            rv.setOnClickPendingIntent(R.id.refresh, refreshIntent(ctx, id))
            rv.setOnClickPendingIntent(R.id.card, openIntent(ctx, board.id))
        }
        mgr.updateAppWidget(id, rv)
    }

    /** Toque num mosaico (dentro da grelha) → abre o quadro na app. Tem de ser MUTABLE (fill-in). */
    private fun openTemplate(ctx: Context, boardId: String): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_BOARD, boardId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(ctx, 20_000 + boardId.hashCode() % 10_000, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private fun refreshIntent(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, DeparturesWidget::class.java)
            .setAction(DeparturesWidget.ACTION_REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        return PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openIntent(ctx: Context, boardId: String): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_BOARD, boardId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(ctx, boardId.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun configIntent(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(ctx, 10_000 + id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
