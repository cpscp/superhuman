package pt.carlos.painel.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** Desenha os ícones com a forma do Pixel (círculo por defeito), com bordas suaves. */
object IconRenderer {

    fun shapePath(shape: Int, s: Float): Path = Path().apply {
        when (shape) {
            1 -> addRoundRect(0f, 0f, s, s, s * 0.30f, s * 0.30f, Path.Direction.CW)
            2 -> addRoundRect(0f, 0f, s, s, s * 0.16f, s * 0.16f, Path.Direction.CW)
            else -> addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW)
        }
    }

    /**
     * @param custom imagem escolhida por ti (substitui o ícone da app)
     * @param themed ícones temáticos Material You (usa a camada monocromática quando a app a tem)
     */
    fun render(d: Drawable, custom: Bitmap?, size: Int, shape: Int, themed: Boolean, bg: Int, fg: Int): Bitmap {
        val content = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(content)
        val s = size.toFloat()
        val e = (s * 0.25f).toInt()   // camadas adaptativas: 108dp com 72dp visíveis
        val full = Rect(-e, -e, size + e, size + e)

        when {
            custom != null -> {
                val side = minOf(custom.width, custom.height)
                val src = Rect((custom.width - side) / 2, (custom.height - side) / 2, (custom.width + side) / 2, (custom.height + side) / 2)
                c.drawBitmap(custom, src, Rect(0, 0, size, size), Paint(Paint.FILTER_BITMAP_FLAG))
            }
            d is AdaptiveIconDrawable -> {
                val mono = if (themed) d.monochrome else null
                if (mono != null) {
                    c.drawColor(bg)
                    val m = mono.mutate()
                    m.setTint(fg)
                    m.bounds = full
                    m.draw(c)
                } else {
                    d.background?.let { it.bounds = full; it.draw(c) }
                    d.foreground?.let { it.bounds = full; it.draw(c) }
                }
            }
            else -> {
                // Ícone antigo (não adaptativo): fundo e ícone reduzido, como faz o Pixel.
                c.drawColor(if (themed) bg else Color.WHITE)
                val inset = (s * 0.18f).toInt()
                d.setBounds(inset, inset, size - inset, size - inset)
                d.draw(c)
            }
        }

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val oc = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        oc.drawPath(shapePath(shape, s), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        oc.drawBitmap(content, 0f, 0f, paint)
        content.recycle()
        return out
    }
}
