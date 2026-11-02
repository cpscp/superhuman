package com.carlos.liquid.glass

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class Blob(val x: Float, val y: Float, val r: Float, val color: Int)
data class Preset(val id: String, val name: String, val base: List<Int>, val blobs: List<Blob>, val darkBase: List<Int>, val darkBlobs: List<Blob>)

/** Wallpapers abstratos originais, com versão clara e escura (mudam com o modo escuro, como no iOS). */
object Wallpapers {
    private fun c(hex: Long) = hex.toInt()

    val presets = listOf(
        Preset("aurora", "Aurora",
            listOf(c(0xFF8EC5FC), c(0xFFE0C3FC)),
            listOf(Blob(0.15f, 0.2f, 0.7f, c(0xFF5AC8FA)), Blob(0.9f, 0.45f, 0.65f, c(0xFFBF5AF2)), Blob(0.3f, 0.9f, 0.7f, c(0xFFFF9FD2))),
            listOf(c(0xFF0B1026), c(0xFF1B1035)),
            listOf(Blob(0.15f, 0.2f, 0.7f, c(0xFF1E5AA8)), Blob(0.9f, 0.45f, 0.6f, c(0xFF5B2A86)), Blob(0.3f, 0.9f, 0.7f, c(0xFF8A2A62)))),
        Preset("lagoa", "Lagoa",
            listOf(c(0xFF84FAB0), c(0xFF8FD3F4)),
            listOf(Blob(0.8f, 0.15f, 0.6f, c(0xFF30D158)), Blob(0.1f, 0.55f, 0.7f, c(0xFF64D2FF)), Blob(0.75f, 0.95f, 0.6f, c(0xFF0A84FF))),
            listOf(c(0xFF02141A), c(0xFF06222B)),
            listOf(Blob(0.8f, 0.15f, 0.6f, c(0xFF0F6B4A)), Blob(0.1f, 0.55f, 0.7f, c(0xFF0B4F6C)), Blob(0.75f, 0.95f, 0.6f, c(0xFF123C8C)))),
        Preset("poente", "Poente",
            listOf(c(0xFFFFD194), c(0xFFFF9A8B)),
            listOf(Blob(0.2f, 0.15f, 0.65f, c(0xFFFFCC00)), Blob(0.85f, 0.5f, 0.7f, c(0xFFFF375F)), Blob(0.2f, 0.95f, 0.7f, c(0xFFAF52DE))),
            listOf(c(0xFF1A0B0B), c(0xFF250A1A)),
            listOf(Blob(0.2f, 0.15f, 0.6f, c(0xFF8A4B00)), Blob(0.85f, 0.5f, 0.7f, c(0xFF7A1430)), Blob(0.2f, 0.95f, 0.7f, c(0xFF4A1A70)))),
        Preset("oceano", "Oceano",
            listOf(c(0xFF4FACFE), c(0xFF00F2FE)),
            listOf(Blob(0.5f, 0.1f, 0.7f, c(0xFF0A84FF)), Blob(0.0f, 0.6f, 0.6f, c(0xFF5E5CE6)), Blob(1.0f, 0.85f, 0.7f, c(0xFF64D2FF))),
            listOf(c(0xFF020818), c(0xFF041A33)),
            listOf(Blob(0.5f, 0.1f, 0.7f, c(0xFF0A3D91)), Blob(0.0f, 0.6f, 0.6f, c(0xFF2C2A7A)), Blob(1.0f, 0.85f, 0.7f, c(0xFF0E5470)))),
        Preset("verde", "Bosque",
            listOf(c(0xFFD4FC79), c(0xFF96E6A1)),
            listOf(Blob(0.1f, 0.1f, 0.6f, c(0xFFA8E063)), Blob(0.9f, 0.6f, 0.7f, c(0xFF34C759)), Blob(0.3f, 1.0f, 0.6f, c(0xFF00C7BE))),
            listOf(c(0xFF07130A), c(0xFF0C1F12)),
            listOf(Blob(0.1f, 0.1f, 0.6f, c(0xFF3C5A12)), Blob(0.9f, 0.6f, 0.7f, c(0xFF145C2A)), Blob(0.3f, 1.0f, 0.6f, c(0xFF0B5A55)))),
        Preset("grafite", "Grafite",
            listOf(c(0xFFE6E9F0), c(0xFFEEF1F5)),
            listOf(Blob(0.2f, 0.25f, 0.6f, c(0xFFC7C7CC)), Blob(0.85f, 0.75f, 0.7f, c(0xFFAEAEB2))),
            listOf(c(0xFF0A0A0B), c(0xFF161618)),
            listOf(Blob(0.2f, 0.25f, 0.6f, c(0xFF2C2C2E)), Blob(0.85f, 0.75f, 0.7f, c(0xFF3A3A3C)))),
        Preset("sporting", "Leão",
            listOf(c(0xFFB8F5C8), c(0xFFE8FFF0)),
            listOf(Blob(0.15f, 0.2f, 0.7f, c(0xFF00A859)), Blob(0.9f, 0.7f, 0.7f, c(0xFF007A3D)), Blob(0.5f, 1.0f, 0.5f, c(0xFFFFD60A))),
            listOf(c(0xFF02120A), c(0xFF041C10)),
            listOf(Blob(0.15f, 0.2f, 0.7f, c(0xFF006B38)), Blob(0.9f, 0.7f, 0.7f, c(0xFF004D27)), Blob(0.5f, 1.0f, 0.5f, c(0xFF6B5A00)))),
        Preset("noite", "Noite",
            listOf(c(0xFF30CFD0), c(0xFF330867)),
            listOf(Blob(0.8f, 0.2f, 0.6f, c(0xFF5E5CE6)), Blob(0.2f, 0.7f, 0.7f, c(0xFF0A84FF))),
            listOf(c(0xFF000000), c(0xFF0A0A1A)),
            listOf(Blob(0.8f, 0.2f, 0.6f, c(0xFF1F1D66)), Blob(0.2f, 0.7f, 0.7f, c(0xFF04306B)))),
    )

    fun screenSize(ctx: Context): Pair<Int, Int> {
        val wm = ctx.getSystemService(WindowManager::class.java)
        val b = wm.maximumWindowMetrics.bounds
        return b.width() to b.height()
    }

    fun photoFile(ctx: Context, id: String) = File(ctx.filesDir, "wallpapers/$id.jpg")

    suspend fun load(ctx: Context, id: String, dark: Boolean): Bitmap = withContext(Dispatchers.Default) {
        val (w, h) = screenSize(ctx)
        if (id.startsWith("photo:")) {
            val f = photoFile(ctx, id.removePrefix("photo:"))
            val b = runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()
            if (b != null) return@withContext cropTo(b, w, h)
        }
        val p = presets.firstOrNull { it.id == id } ?: presets[0]
        generate(p, dark, w, h)
    }

    fun generate(p: Preset, dark: Boolean, w: Int, h: Int): Bitmap {
        // desenha em baixa resolução e amplia (é um degradê, fica igual e é muito mais rápido)
        val sw = (w / 4).coerceAtLeast(64); val sh = (h / 4).coerceAtLeast(128)
        val b = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val base = if (dark) p.darkBase else p.base
        c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, sw.toFloat(), sh.toFloat(), base[0], base[1], Shader.TileMode.CLAMP)
        })
        val dim = max(sw, sh).toFloat()
        for (bl in if (dark) p.darkBlobs else p.blobs) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(bl.x * sw, bl.y * sh, bl.r * dim, intArrayOf(bl.color, Color.argb(0, Color.red(bl.color), Color.green(bl.color), Color.blue(bl.color))), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
        }
        val blurred = Blur.stack(b, 6)
        val out = Bitmap.createScaledBitmap(blurred, w, h, true)
        if (blurred !== b) blurred.recycle()
        b.recycle()
        return out
    }

    fun cropTo(src: Bitmap, w: Int, h: Int): Bitmap {
        val scale = max(w.toFloat() / src.width, h.toFloat() / src.height)
        val m = Matrix().apply { postScale(scale, scale); postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f) }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        if (out !== src) src.recycle()
        return out
    }

    /** Copia a foto escolhida para a pasta da app (ajustada ao ecrã). */
    suspend fun importPhoto(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val (w, h) = screenSize(ctx)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            while (opts.outWidth / (sample * 2) >= w && opts.outHeight / (sample * 2) >= h) sample *= 2
            val b = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@runCatching null
            val cropped = cropTo(b, w, h)
            val id = System.currentTimeMillis().toString()
            val f = photoFile(ctx, id); f.parentFile?.mkdirs()
            f.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            "photo:$id"
        }.getOrNull()
    }

    fun photoIds(ctx: Context): List<String> =
        File(ctx.filesDir, "wallpapers").listFiles()?.filter { it.extension == "jpg" }?.sortedByDescending { it.lastModified() }?.map { "photo:" + it.nameWithoutExtension }.orEmpty()

    fun deletePhoto(ctx: Context, id: String) { photoFile(ctx, id.removePrefix("photo:")).delete() }

    suspend fun applySystem(ctx: Context, home: Bitmap?, lock: Bitmap?) = withContext(Dispatchers.IO) {
        runCatching {
            val wm = WallpaperManager.getInstance(ctx)
            if (home != null) wm.setBitmap(home, null, true, WallpaperManager.FLAG_SYSTEM)
            if (lock != null) wm.setBitmap(lock, null, true, WallpaperManager.FLAG_LOCK)
        }
    }

    /** Luminância média de uma zona (para escolher ícones da barra de estado claros/escuros). */
    fun luminance(b: Bitmap, top: Float, bottom: Float): Float {
        val y0 = (b.height * top).roundToInt().coerceIn(0, b.height - 1)
        val y1 = (b.height * bottom).roundToInt().coerceIn(y0 + 1, b.height)
        var sum = 0.0; var n = 0
        val stepX = (b.width / 24).coerceAtLeast(1); val stepY = ((y1 - y0) / 12).coerceAtLeast(1)
        var y = y0
        while (y < y1) { var x = 0; while (x < b.width) {
            val p = b.getPixel(x, y); sum += (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)) / 255.0; n++; x += stepX }; y += stepY }
        return if (n == 0) 0.5f else (sum / n).toFloat()
    }
}

object Blur {
    /** Desfoca numa versão reduzida (rápido) — usada como fonte do vidro. */
    fun glassSource(src: Bitmap, factor: Int = 10, radius: Int = 10): Bitmap {
        val w = (src.width / factor).coerceAtLeast(8); val h = (src.height / factor).coerceAtLeast(8)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        return stack(small, radius)
    }

    /** Stack blur (Mario Klingemann), in-place numa cópia mutável. */
    fun stack(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap
        val w = bitmap.width; val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1; val hm = h - 1; val wh = w * h; val div = radius + radius + 1
        val r = IntArray(wh); val g = IntArray(wh); val b = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int; var x: Int; var y: Int; var i: Int; var p: Int; var yp: Int; var yi: Int; var yw: Int
        val vmin = IntArray(max(w, h))
        var divsum = (div + 1) shr 1; divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0; while (i < 256 * divsum) { dv[i] = i / divsum; i++ }
        yw = 0; yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int; var stackstart: Int; var sir: IntArray; var rbs: Int
        val r1 = radius + 1
        var routsum: Int; var goutsum: Int; var boutsum: Int; var rinsum: Int; var ginsum: Int; var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0; gsum = 0; rsum = 0; boutsum = 0; goutsum = 0; routsum = 0; binsum = 0; ginsum = 0; rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + minOf(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16; sir[1] = p and 0x00ff00 shr 8; sir[2] = p and 0x0000ff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] } else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16; sir[1] = p and 0x00ff00 shr 8; sir[2] = p and 0x0000ff
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi++; x++
            }
            yw += w; y++
        }
        x = 0
        while (x < w) {
            bsum = 0; gsum = 0; rsum = 0; boutsum = 0; goutsum = 0; routsum = 0; binsum = 0; ginsum = 0; rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] } else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                yi += w; y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
