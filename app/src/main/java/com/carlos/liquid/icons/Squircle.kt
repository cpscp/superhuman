package com.carlos.liquid.icons

import android.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Superelipse (cantos contínuos) igual à forma dos ícones da Apple. */
object Squircle {
    fun path(w: Float, h: Float, n: Double = 5.0, left: Float = 0f, top: Float = 0f): Path {
        val p = Path()
        val steps = 160
        val a = w / 2.0; val b = h / 2.0
        for (i in 0..steps) {
            val t = 2.0 * Math.PI * i / steps
            val c = cos(t); val s = sin(t)
            val x = a * sign(c) * abs(c).pow(2.0 / n) + a + left
            val y = b * sign(s) * abs(s).pow(2.0 / n) + b + top
            if (i == 0) p.moveTo(x.toFloat(), y.toFloat()) else p.lineTo(x.toFloat(), y.toFloat())
        }
        p.close()
        return p
    }
}

object SquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Squircle.path(size.width, size.height).asComposePath())
}
