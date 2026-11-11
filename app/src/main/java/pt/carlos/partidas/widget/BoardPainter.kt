package pt.carlos.partidas.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.DisplayMetrics
import pt.carlos.partidas.R
import pt.carlos.partidas.data.BoardLook
import pt.carlos.partidas.data.CacheEntry
import pt.carlos.partidas.data.Logos
import pt.carlos.partidas.data.Text
import pt.carlos.partidas.model.Board
import pt.carlos.partidas.model.Departure
import pt.carlos.partidas.model.Mode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Desenha o quadro do Glass TV em Bitmaps, com a Google Sans Flex
 * (o launcher ignora fontes da app em widgets).
 *  - [background]: quadro + coluna de informação (e a mensagem, se não houver partidas)
 *  - [tile]: um mosaico; os mosaicos vão numa grelha com scroll por cima do fundo
 * Tudo em dp: o canvas é escalado para píxeis.
 */
object BoardPainter {

    const val PAD_START = 14f
    const val PAD_TOP = 12f
    const val PAD_END = 10f
    const val PAD_BOTTOM = 12f
    const val GAP = 7f
    private const val INFO_GAP = 10f
    private const val MIN_TILE_W = 96f
    /** Altura mínima de um mosaico com minutos, hora, linha, destino e estado. */
    private const val ROW_MIN_H = 126f

    /** Largura da coluna de informação (dp). */
    fun infoWidth(wDp: Float) = if (wDp < 300f) 92f else 104f

    /** Geometria da zona dos mosaicos (dp). Linhas a mais ficam acessíveis com scroll. */
    data class Grid(val x: Float, val y: Float, val w: Float, val h: Float, val cols: Int, val rows: Int, val tileW: Float, val tileH: Float)

    fun grid(wDp: Float, hDp: Float): Grid {
        val x = PAD_START + infoWidth(wDp) + INFO_GAP
        val y = PAD_TOP
        val w = wDp - x - PAD_END
        val h = hDp - PAD_TOP - PAD_BOTTOM
        val cols = ((w + GAP) / (MIN_TILE_W + GAP)).toInt().coerceIn(1, 4)
        val rows = ((h + GAP) / (ROW_MIN_H + GAP)).toInt().coerceIn(1, 3)
        val tw = (w - GAP * (cols - 1)) / cols
        val th = (h - GAP * (rows - 1)) / rows
        return Grid(x, y, w, h, cols, rows, tw, th)
    }

    private class Fonts(val regular: Typeface, val medium: Typeface, val semibold: Typeface, val bold: Typeface)
    @Volatile private var fonts: Fonts? = null

    private fun fonts(ctx: Context): Fonts = fonts ?: Fonts(
        ctx.resources.getFont(R.font.gsf_regular),
        ctx.resources.getFont(R.font.gsf_medium),
        ctx.resources.getFont(R.font.gsf_semibold),
        ctx.resources.getFont(R.font.gsf_bold)
    ).also { fonts = it }

    private fun scale(ctx: Context) = min(ctx.resources.displayMetrics.density, 2.5f)

    private fun newBitmap(ctx: Context, wDp: Float, hDp: Float): Pair<Bitmap, Canvas> {
        val s = scale(ctx)
        val bmp = Bitmap.createBitmap(max(1, (wDp * s).roundToInt()), max(1, (hDp * s).roundToInt()), Bitmap.Config.ARGB_8888)
        // densidade certa → o ImageView mede o mosaico com o tamanho em dp correto
        bmp.density = (s * DisplayMetrics.DENSITY_DEFAULT).roundToInt()
        val cv = Canvas(bmp)
        cv.scale(s, s)
        return bmp to cv
    }

    /**
     * Quadro + coluna de informação. Se [message] não for null, desenha-a na zona dos mosaicos
     * (a carregar, sem dados, sem partidas, por configurar).
     */
    fun background(
        ctx: Context, board: Board?, cache: CacheEntry?, now: Long,
        wDp: Float, hDp: Float, refreshing: Boolean, message: String?
    ): Bitmap {
        val (bmp, cv) = newBitmap(ctx, wDp, hDp)
        val f = fonts(ctx)
        val col = board?.let { BoardLook.colors(it.mode) } ?: BoardLook.CP
        val fg = col.fg

        // ---------- fundo: cor sólida da marca (Material 3, sem vidro) ----------
        val outer = RectF(0f, 0f, wDp, hDp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = col.base
        cv.drawRoundRect(outer, 28f, 28f, p)
        p.shader = null
        p.style = Paint.Style.FILL

        // ---------- coluna de informação ----------
        val iw = infoWidth(wDp)
        val x = PAD_START
        var y = PAD_TOP
        val logo = board?.let { Logos.get(ctx, it.mode) }
        if (logo != null) {
            val lh = 24f
            val lw = min(iw - 34f, logo.width * lh / logo.height)
            val lp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            if (board != null && Logos.monochrome(board.mode)) lp.colorFilter = PorterDuffColorFilter(fg, PorterDuff.Mode.SRC_IN)
            val drawH = lw * logo.height / logo.width
            cv.drawBitmap(logo, null, RectF(x, y + (lh - drawH) / 2f, x + lw, y + (lh + drawH) / 2f), lp)
        } else {
            drawLine(cv, if (board == null || board.mode == Mode.TRAIN) "Partidas" else "Carris", x, y, iw - 30f, tp(f.bold, 17f, fg))
        }
        if (board != null) {
            ctx.getDrawable(R.drawable.ic_refresh)?.mutate()?.let { d ->
                d.setTint(fg)
                d.alpha = if (refreshing) 110 else 255
                val s = 20f
                val rx = x + iw - s - 2f
                d.setBounds(rx.toInt(), (y + 2f).toInt(), (rx + s).toInt(), (y + 2f + s).toInt())
                d.draw(cv)
            }
        }
        y += 24f + 8f

        // título: parte antes do hífen ("Alcântara" / "-Mar")
        val title = Text.breakBeforeHyphens(board?.displayTitle ?: "Partidas")
        y += drawBlock(cv, title, x, y, iw, tp(f.bold, if (wDp < 300f) 15f else 16f, fg), maxLines = 2)
        if (hDp >= 150f) {
            y += 1f
            drawLine(cv, board?.subtitle ?: "Toca para escolher", x, y, iw, tp(f.medium, 12f, withA(fg, .75f)))
        }

        // relógio + TEMPO REAL, em baixo
        val liveText = when {
            board == null -> ""
            refreshing -> "A ATUALIZAR…"
            cache?.error != null && cache.items.isNotEmpty() -> "OFFLINE · ${Text.hhmm(cache.fetchedAt)}"
            else -> "TEMPO REAL"
        }
        val liveY = hDp - PAD_BOTTOM - 13f
        if (liveText.isNotEmpty()) {
            p.color = fg
            cv.drawCircle(x + 3.5f, liveY + 6.5f, 3.5f, p)
            drawLine(cv, liveText, x + 12f, liveY, iw - 12f, tp(f.semibold, 10f, withA(fg, .85f), spacing = .06f))
        }
        val clockP = tp(f.bold, if (wDp < 300f) 24f else 26f, fg, spacing = -.02f)
        drawLine(cv, Text.hhmm(now), x, liveY - 3f - lineH(clockP), iw, clockP)

        // ---------- mensagem na zona dos mosaicos ----------
        if (message != null) {
            val g = grid(wDp, hDp)
            p.color = col.tile
            cv.drawRoundRect(RectF(g.x, g.y, g.x + g.w, g.y + g.h), 16f, 16f, p)
            val lay = layout(message, tp(f.medium, 13f, col.tileFg), g.w - 20f, 5, Layout.Alignment.ALIGN_CENTER)
            cv.save(); cv.translate(g.x + 10f, g.y + (g.h - lay.height) / 2f); lay.draw(cv); cv.restore()
        }
        return bmp.asShared()
    }

    /** Um mosaico com [wDp]×[hDp]; o que não cabe sai (detalhe, depois linha/carreira). */
    fun tile(ctx: Context, d: Departure, highlighted: Boolean, col: BoardLook.Colors, now: Long, wDp: Float, hDp: Float): Bitmap {
        val (bmp, cv) = newBitmap(ctx, wDp, hDp)
        drawTile(cv, fonts(ctx), RectF(0f, 0f, wDp, hDp), d, highlighted, col, now)
        return bmp.asShared()
    }

    private fun drawTile(cv: Canvas, f: Fonts, r: RectF, d: Departure, hi: Boolean, col: BoardLook.Colors, now: Long) {
        val bg = if (hi) col.hiTile else col.tile
        val fg = if (hi) col.hiFg else col.tileFg
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = bg
        cv.drawRoundRect(r, 16f, 16f, p)

        val pad = 9f
        val x = r.left + pad
        val w = r.width() - pad * 2
        var y = r.top + 8f

        // tamanhos proporcionais ao mosaico
        val bigSize = (w * .30f).coerceIn(22f, 34f)
        val bigP = tp(f.bold, bigSize, fg, spacing = -.02f)
        val minP = tp(f.medium, (bigSize * .42f).coerceIn(11f, 14f), withA(fg, .75f))
        val timeP = tp(f.medium, 12.5f, withA(fg, .85f))
        val oldP = tp(f.semibold, 11f, withA(fg, .5f)).apply { isStrikeThruText = true }
        val badgeP = tp(f.bold, 10.5f, if (hi) col.hiTile else col.base)
        val destP = tp(f.bold, (w * .15f).coerceIn(13f, 17f), fg)
        val detailP = tp(f.medium, 10.5f, withA(fg, .7f))
        val pillP = tp(f.semibold, 9.5f, 0, spacing = .05f)

        val pillH = lineH(pillP) + 6f
        val bottom = r.bottom - 8f
        val usable = bottom - pillH - 4f

        // 1) minutos
        val bigW = drawLine(cv, BoardLook.bigNumber(d, now), x, y, w, bigP)
        if (BoardLook.showMinSuffix(d, now)) cv.drawText(" min", x + bigW, y - bigP.fontMetrics.ascent, minP)
        y += lineH(bigP)

        // 2) hora (+ hora antiga riscada)
        val tP = if (d.cancelled) TextPaint(timeP).apply { isStrikeThruText = true } else timeP
        val timeW = drawLine(cv, Text.hhmm(d.etaMs), x, y, w, tP)
        d.oldTimeMs?.takeIf { !d.cancelled }?.let {
            drawLine(cv, Text.hhmm(it), x + timeW + 6f, y + (lineH(timeP) - lineH(oldP)) / 2f, w - timeW - 6f, oldP)
        }
        y += lineH(timeP)

        // espaço para linha/carreira, destino e detalhe
        val badgeH = lineH(badgeP) + 2f
        val destH = lineH(destP)
        val detailH = lineH(detailP)
        val left = usable - y
        val showBadge = d.badge.isNotBlank() && left >= destH + badgeH + 6f
        val afterBadge = left - (if (showBadge) badgeH + 6f else 2f) - destH
        val detailLines = when {
            d.detail.isBlank() -> 0
            afterBadge >= detailH * 2 -> 2
            afterBadge >= detailH -> 1
            else -> 0
        }
        if (showBadge) {
            y += 5f
            val bw = min(badgeP.measureText(d.badge) + 10f, w)
            p.color = fg
            cv.drawRoundRect(RectF(x, y, x + bw, y + badgeH), 5f, 5f, p)
            drawLine(cv, d.badge, x + 5f, y + 1f, bw - 10f, badgeP)
            y += badgeH + 1f
        } else y += 2f
        if (left >= destH) {
            drawLine(cv, d.destination.ifBlank { "—" }, x, y, w, destP)
            y += destH
        }
        if (detailLines > 0) drawBlock(cv, d.detail, x, y, w, detailP, detailLines)

        // pílula de estado em baixo
        val onLight = BoardLook.tileIsLight(col, hi)
        val (pbg, pfg) = BoardLook.pill(d.status, onLight)
        pillP.color = pfg
        val text = TextUtils.ellipsize(BoardLook.pillText(d), pillP, w - 12f, TextUtils.TruncateAt.END).toString()
        val pw = pillP.measureText(text) + 12f
        val py = bottom - pillH
        p.color = pbg
        cv.drawRoundRect(RectF(x, py, x + pw, py + pillH), 6f, 6f, p)
        drawLine(cv, text, x + 6f, py + 3f, pw, pillP)
    }

    // ---------- texto ----------

    private fun tp(face: Typeface, size: Float, color: Int, spacing: Float = 0f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = size
        this.color = color
        letterSpacing = spacing
    }

    private fun lineH(p: Paint): Float = p.fontMetrics.let { it.descent - it.ascent }

    /** Uma linha, cortada com "…" se não couber. Devolve a largura desenhada. */
    private fun drawLine(cv: Canvas, s: String, x: Float, top: Float, maxW: Float, p: TextPaint): Float {
        if (maxW <= 0f) return 0f
        val t = TextUtils.ellipsize(s, p, maxW, TextUtils.TruncateAt.END).toString()
        cv.drawText(t, x, top - p.fontMetrics.ascent, p)
        return p.measureText(t)
    }

    /** Bloco com várias linhas. Devolve a altura ocupada. */
    private fun drawBlock(cv: Canvas, s: String, x: Float, top: Float, maxW: Float, p: TextPaint, maxLines: Int): Float {
        val lay = layout(s, p, maxW, maxLines, Layout.Alignment.ALIGN_NORMAL)
        cv.save(); cv.translate(x, top); lay.draw(cv); cv.restore()
        return lay.height.toFloat()
    }

    private fun layout(s: String, p: TextPaint, w: Float, maxLines: Int, align: Layout.Alignment): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, p, max(1, w.toInt()))
            .setAlignment(align)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()

    private fun white(a: Float) = withA(Color.WHITE, a)
    private fun withA(c: Int, a: Float) = BoardLook.withAlpha(c, a * (Color.alpha(c) / 255f))
}
