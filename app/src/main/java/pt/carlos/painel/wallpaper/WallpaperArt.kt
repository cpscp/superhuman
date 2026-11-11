package pt.carlos.painel.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Paleta de um papel de parede: fundo, três tons e um tom claro de apoio. */
data class WallPalette(val name: String, val bg: Int, val a: Int, val b: Int, val c: Int, val soft: Int)

val WallPalettes = listOf(
    WallPalette("Azul", 0xFFD9E7FF.toInt(), 0xFF7FA8F5.toInt(), 0xFF3D6FD8.toInt(), 0xFF1B3C8C.toInt(), 0xFFB9D3FF.toInt()),
    WallPalette("Verde", 0xFFDDEFE0.toInt(), 0xFF8BC79A.toInt(), 0xFF3F8F5A.toInt(), 0xFF1D4F33.toInt(), 0xFFC4E6CC.toInt()),
    WallPalette("Areia", 0xFFF6EBDD.toInt(), 0xFFE7B98A.toInt(), 0xFFC97B45.toInt(), 0xFF7A3E1E.toInt(), 0xFFF0D5B8.toInt()),
    WallPalette("Rosa", 0xFFFBE3EA.toInt(), 0xFFF4A3BC.toInt(), 0xFFD45C86.toInt(), 0xFF7D2148.toInt(), 0xFFF8C9D8.toInt()),
    WallPalette("Lilás", 0xFFECE3FB.toInt(), 0xFFB9A2EE.toInt(), 0xFF7B5BD0.toInt(), 0xFF3D2580.toInt(), 0xFFD8CCF6.toInt()),
    WallPalette("Noite", 0xFF15181E.toInt(), 0xFF2D3440.toInt(), 0xFF4F6280.toInt(), 0xFF8FA6C8.toInt(), 0xFF222833.toInt()),
)

val WallStyles = listOf("Formas", "Ondas", "Arcos", "Bolhas", "Gradiente")

/**
 * Papéis de parede originais ao estilo Pixel (formas Material, cores suaves), gerados no tablet.
 * Não usam nenhuma imagem da Google.
 */
object WallpaperArt {

    fun render(style: Int, pal: WallPalette, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rnd = Random(style * 31 + WallPalettes.indexOf(pal) * 7 + 3)
        when (style) {
            0 -> shapes(c, pal, w.toFloat(), h.toFloat())
            1 -> waves(c, pal, w.toFloat(), h.toFloat(), rnd)
            2 -> arcs(c, pal, w.toFloat(), h.toFloat())
            3 -> bubbles(c, pal, w.toFloat(), h.toFloat(), rnd)
            else -> gradient(c, pal, w.toFloat(), h.toFloat())
        }
        return bmp
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    /** Formas Material 3: círculo, "bolacha" com recortes, cápsula. */
    private fun shapes(c: Canvas, p: WallPalette, w: Float, h: Float) {
        val m = min(w, h)
        c.drawColor(p.bg)
        c.drawCircle(w * 0.12f, h * 0.95f, m * 0.62f, fill(p.a))
        c.drawPath(cookie(w * 0.80f, h * 0.22f, m * 0.40f, 9), fill(p.b))
        c.save()
        c.rotate(-35f, w * 0.55f, h * 0.62f)
        val pill = RectF(w * 0.40f, h * 0.55f, w * 0.40f + m * 0.75f, h * 0.55f + m * 0.24f)
        c.drawRoundRect(pill, m * 0.12f, m * 0.12f, fill(p.soft))
        c.restore()
        c.drawCircle(w * 0.64f, h * 0.84f, m * 0.10f, fill(p.c))
    }

    private fun cookie(cx: Float, cy: Float, r: Float, lobes: Int): Path {
        val path = Path()
        val steps = 360
        for (i in 0..steps) {
            val t = 2 * PI * i / steps
            val rr = r * (1f + 0.07f * cos(lobes * t).toFloat())
            val x = cx + rr * cos(t).toFloat()
            val y = cy + rr * sin(t).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun waves(c: Canvas, p: WallPalette, w: Float, h: Float, rnd: Random) {
        c.drawColor(p.bg)
        val colors = listOf(p.soft, p.a, p.b, p.c)
        colors.forEachIndexed { i, col ->
            val base = h * (0.38f + i * 0.17f)
            val amp = h * (0.05f + rnd.nextFloat() * 0.04f)
            val freq = (1.2f + rnd.nextFloat() * 0.8f) * 2f * PI.toFloat() / w
            val phase = rnd.nextFloat() * 6f
            val path = Path()
            path.moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                path.lineTo(x, base + amp * sin(x * freq + phase))
                x += 8f
            }
            path.lineTo(w, h)
            path.close()
            c.drawPath(path, fill(col))
        }
    }

    private fun arcs(c: Canvas, p: WallPalette, w: Float, h: Float) {
        c.drawColor(p.bg)
        val r0 = max(w, h) * 1.05f
        val colors = listOf(p.soft, p.a, p.b, p.c, p.a)
        colors.forEachIndexed { i, col -> c.drawCircle(w, h, r0 * (1f - i * 0.18f), fill(col)) }
        c.drawCircle(0f, 0f, min(w, h) * 0.28f, fill(p.soft))
    }

    private fun bubbles(c: Canvas, p: WallPalette, w: Float, h: Float, rnd: Random) {
        c.drawColor(p.soft)
        val m = min(w, h)
        val colors = listOf(p.a, p.b, p.c, p.bg)
        repeat(22) { i ->
            val paint = fill(colors[i % colors.size]).apply { alpha = 150 + rnd.nextInt(105) }
            c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, m * (0.04f + rnd.nextFloat() * 0.22f), paint)
        }
    }

    private fun gradient(c: Canvas, p: WallPalette, w: Float, h: Float) {
        val base = Paint().apply { shader = LinearGradient(0f, 0f, w, h, p.bg, p.soft, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w, h, base)
        val m = max(w, h)
        listOf(Triple(0.15f, 0.20f, p.a), Triple(0.85f, 0.35f, p.b), Triple(0.45f, 0.95f, p.c)).forEach { (fx, fy, col) ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(w * fx, h * fy, m * 0.55f, col, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, w, h, paint)
        }
    }
}
