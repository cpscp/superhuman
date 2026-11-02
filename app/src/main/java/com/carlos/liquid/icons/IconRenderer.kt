package com.carlos.liquid.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.carlos.liquid.data.AppEntry
import com.carlos.liquid.data.IconStyle

/** Especificação final (já resolvida para claro/escuro). */
data class IconSpec(val style: IconStyle, val dark: Boolean, val tint: Int, val size: Int)

object IconCache {
    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val drawables = LruCache<String, Drawable>(400)

    fun get(key: String, spec: IconSpec): ImageBitmap? = cache.get(key + spec.hashCode())

    fun render(ctx: Context, e: AppEntry, spec: IconSpec): ImageBitmap {
        val ck = e.key + spec.hashCode()
        cache.get(ck)?.let { return it }
        val d = drawables.get(e.key) ?: (runCatching { e.info.getIcon(ctx.resources.displayMetrics.densityDpi) }.getOrNull()
            ?: ctx.packageManager.defaultActivityIcon).also { drawables.put(e.key, it) }
        val bmp = IconRenderer.render(d, spec).asImageBitmap()
        cache.put(ck, bmp)
        return bmp
    }

    fun renderDrawable(id: String, d: Drawable, spec: IconSpec): ImageBitmap {
        val ck = id + spec.hashCode()
        cache.get(ck)?.let { return it }
        val bmp = IconRenderer.render(d, spec).asImageBitmap()
        cache.put(ck, bmp)
        return bmp
    }

    fun clearMissing(keys: Set<String>) {
        cache.evictAll(); drawables.evictAll()
    }

    fun clear() { cache.evictAll() }
}

object IconRenderer {
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }

    fun render(d: Drawable, spec: IconSpec): Bitmap {
        val s = spec.size
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val adaptive = d as? AdaptiveIconDrawable
        when (spec.style) {
            IconStyle.DEFAULT -> drawDefault(c, d, adaptive, s, spec.dark)
            IconStyle.DARK -> drawDark(c, d, adaptive, s)
            IconStyle.TINTED -> drawTinted(c, d, adaptive, s, spec.tint, spec.dark)
            IconStyle.CLEAR -> drawClear(c, d, adaptive, s, spec.dark)
        }
        if (spec.style != IconStyle.CLEAR) {
            // recorta para a superelipse com antialiasing
            val mask = Squircle.path(s.toFloat(), s.toFloat())
            val m = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            Canvas(m).drawPath(mask, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
            c.drawBitmap(m, 0f, 0f, maskPaint)
            m.recycle()
            // brilho subtil no topo (efeito vidro)
            val hl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = s * 0.012f
                shader = LinearGradient(0f, 0f, 0f, s.toFloat(), Color.argb(if (spec.dark || spec.style != IconStyle.DEFAULT) 70 else 40, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            val inset = s * 0.006f
            val p = Squircle.path(s - 2 * inset, s - 2 * inset, left = inset, top = inset)
            c.drawPath(p, hl)
        }
        return out
    }

    private fun layer(c: Canvas, d: Drawable?, s: Int, filter: android.graphics.ColorFilter? = null) {
        d ?: return
        val pad = s / 4
        d.setBounds(-pad, -pad, s + pad, s + pad)
        d.colorFilter = filter
        d.draw(c)
        d.colorFilter = null
    }

    private fun legacy(c: Canvas, d: Drawable, s: Int, filter: android.graphics.ColorFilter? = null, bg: Int = Color.WHITE) {
        c.drawColor(bg)
        val inset = (s * 0.14f).toInt()
        d.setBounds(inset, inset, s - inset, s - inset)
        d.colorFilter = filter
        d.draw(c)
        d.colorFilter = null
    }

    private fun drawDefault(c: Canvas, d: Drawable, a: AdaptiveIconDrawable?, s: Int, dark: Boolean) {
        if (a != null) { layer(c, a.background, s); layer(c, a.foreground, s) } else legacy(c, d, s)
    }

    /** Escuro: fundo grafite, glifo com a cor da marca (como no iOS). */
    private fun drawDark(c: Canvas, d: Drawable, a: AdaptiveIconDrawable?, s: Int) {
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, s.toFloat(), Color.rgb(58, 58, 60), Color.rgb(22, 22, 24), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), bg)
        if (a == null) {
            val cm = ColorMatrix().apply { setScale(0.82f, 0.82f, 0.82f, 1f) }
            val inset = (s * 0.06f).toInt()
            d.setBounds(inset, inset, s - inset, s - inset); d.colorFilter = ColorMatrixColorFilter(cm); d.draw(c); d.colorFilter = null
            return
        }
        val bgColor = average(a.background, s)
        val fg = a.foreground
        val fgStats = stats(fg, s)
        if (fgStats.lum > 0.82f && saturation(bgColor) > 0.18f) {
            // glifo branco num fundo de cor → pinta o glifo com a cor da marca
            layer(c, fg, s, LightingColorFilter(brighten(bgColor), 0))
        } else layer(c, fg, s)
    }

    /** Tingido: glifo monocromático numa só cor. */
    private fun drawTinted(c: Canvas, d: Drawable, a: AdaptiveIconDrawable?, s: Int, tint: Int, dark: Boolean) {
        val hsv = FloatArray(3); Color.colorToHSV(tint, hsv)
        val bgTop: Int; val bgBottom: Int
        if (dark) {
            bgTop = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1] * 0.85f, 0.26f)); bgBottom = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 0.08f))
        } else {
            bgTop = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1] * 0.14f, 1f)); bgBottom = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1] * 0.26f, 0.95f))
        }
        c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, s.toFloat(), bgTop, bgBottom, Shader.TileMode.CLAMP) })
        val glyphColor = if (dark) Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f)) else Color.HSVToColor(floatArrayOf(hsv[0], minOf(1f, hsv[1] * 1.1f), 0.62f))
        glyph(c, d, a, s, glyphColor)
    }

    /** Transparente: só o glifo; o vidro é desenhado por baixo pelo launcher. */
    private fun drawClear(c: Canvas, d: Drawable, a: AdaptiveIconDrawable?, s: Int, dark: Boolean) {
        val color = if (dark) Color.argb(240, 255, 255, 255) else Color.argb(235, 255, 255, 255)
        glyph(c, d, a, s, color)
    }

    private fun glyph(c: Canvas, d: Drawable, a: AdaptiveIconDrawable?, s: Int, color: Int) {
        val r = Color.red(color) / 255f; val g = Color.green(color) / 255f; val b = Color.blue(color) / 255f
        val mono = if (a != null && Build.VERSION.SDK_INT >= 33) a.monochrome else null
        if (mono != null) {
            val f = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                0f, 0f, 0f, 0f, r * 255,
                0f, 0f, 0f, 0f, g * 255,
                0f, 0f, 0f, 0f, b * 255,
                0f, 0f, 0f, Color.alpha(color) / 255f, 0f,
            )))
            layer(c, mono, s, f)
            return
        }
        // sem camada monocromática: luminância do primeiro plano → cor única
        val k = 0.55f
        val lr = 0.299f * (1 - k); val lg = 0.587f * (1 - k); val lb = 0.114f * (1 - k)
        val f = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            r * lr, r * lg, r * lb, 0f, r * k * 255,
            g * lr, g * lg, g * lb, 0f, g * k * 255,
            b * lr, b * lg, b * lb, 0f, b * k * 255,
            0f, 0f, 0f, Color.alpha(color) / 255f, 0f,
        )))
        if (a != null) {
            // se o primeiro plano não tem contraste próprio (ex.: tudo branco), usa só o alfa
            layer(c, a.foreground, s, f)
        } else {
            val tmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val tc = Canvas(tmp)
            val inset = (s * 0.18f).toInt()
            d.setBounds(inset, inset, s - inset, s - inset); d.colorFilter = f; d.draw(tc); d.colorFilter = null
            c.drawBitmap(tmp, 0f, 0f, null); tmp.recycle()
        }
    }

    private data class Stats(val lum: Float, val color: Int)

    private fun stats(d: Drawable?, s: Int): Stats {
        d ?: return Stats(0f, Color.BLACK)
        val n = 48
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val pad = n / 4
        d.setBounds(-pad, -pad, n + pad, n + pad); d.draw(c)
        var rs = 0L; var gs = 0L; var bs = 0L; var cnt = 0L
        val px = IntArray(n * n); b.getPixels(px, 0, n, 0, 0, n, n)
        for (p in px) if (Color.alpha(p) > 140) { rs += Color.red(p); gs += Color.green(p); bs += Color.blue(p); cnt++ }
        b.recycle()
        if (cnt == 0L) return Stats(0f, Color.BLACK)
        val col = Color.rgb((rs / cnt).toInt(), (gs / cnt).toInt(), (bs / cnt).toInt())
        return Stats((0.299f * Color.red(col) + 0.587f * Color.green(col) + 0.114f * Color.blue(col)) / 255f, col)
    }

    private fun average(d: Drawable?, s: Int): Int = stats(d, s).color

    private fun saturation(c: Int): Float { val h = FloatArray(3); Color.colorToHSV(c, h); return h[1] * h[2] }

    private fun brighten(c: Int): Int {
        val h = FloatArray(3); Color.colorToHSV(c, h)
        h[2] = maxOf(h[2], 0.85f); h[1] = minOf(1f, h[1] * 1.1f)
        return Color.HSVToColor(h)
    }
}
