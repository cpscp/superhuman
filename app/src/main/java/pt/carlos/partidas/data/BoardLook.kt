package pt.carlos.partidas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import pt.carlos.partidas.model.Departure
import pt.carlos.partidas.model.LiveStatus
import pt.carlos.partidas.model.Mode

/**
 * Cores e regras dos quadros — copiadas do Glass TV (Palette + BoardColors + StatusPill).
 * Comuns à app (Compose) e aos widgets (RemoteViews).
 */
object BoardLook {
    // Palette do Glass TV
    const val TEXT = 0xFFF5F5F7.toInt()
    const val CP_GREEN = 0xFF2F7D32.toInt()
    const val CP_GREEN_DEEP = 0xFF14501C.toInt()
    const val CARRIS_YELLOW = 0xFFFFC20E.toInt()
    const val CARRIS_YELLOW_DEEP = 0xFFE0A100.toInt()
    const val CARRIS_BLUE = 0xFF00467F.toInt()
    const val ORANGE = 0xFFFF9F0A.toInt()
    const val RED = 0xFFFF453A.toInt()
    const val INK = 0xFF1C1C1E.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    /** Fundo cinzento escuro da app. */
    const val APP_BG = 0xFF121212.toInt()

    /** Cores de um quadro: base da marca + mosaico em destaque + mosaicos normais + faixa. */
    data class Colors(
        val base: Int, val deep: Int, val fg: Int,
        val hiTile: Int, val hiFg: Int,
        val tile: Int, val tileFg: Int,
        val stripA: Int, val stripB: Int, val stripFg: Int
    )

    val CP = Colors(
        base = CP_GREEN, deep = CP_GREEN_DEEP, fg = WHITE,
        hiTile = 0xFFF5F5F7.toInt(), hiFg = CP_GREEN_DEEP,
        tile = 0x38000000, tileFg = WHITE,
        stripA = 0xFFF5F5F7.toInt(), stripB = 0x59000000, stripFg = CP_GREEN_DEEP
    )
    val CARRIS = Colors(
        base = CARRIS_YELLOW, deep = CARRIS_YELLOW_DEEP, fg = CARRIS_BLUE,
        hiTile = CARRIS_BLUE, hiFg = CARRIS_YELLOW,
        tile = 0x24000000, tileFg = CARRIS_BLUE,
        stripA = CARRIS_BLUE, stripB = 0x33000000, stripFg = CARRIS_YELLOW
    )

    fun colors(mode: Mode) = if (mode == Mode.TRAIN) CP else CARRIS

    fun minutesLeft(d: Departure, now: Long): Long = ((d.etaMs - now) / 60_000L).coerceAtLeast(0)

    /** Número grande do mosaico: "—" suprimido, "Já" a chegar, senão os minutos. */
    fun bigNumber(d: Departure, now: Long): String {
        val m = minutesLeft(d, now)
        return when {
            d.cancelled -> "—"
            m == 0L -> "Já"
            m >= 100 -> Text.hhmm(d.etaMs)
            else -> "$m"
        }
    }

    fun showMinSuffix(d: Departure, now: Long): Boolean {
        val m = minutesLeft(d, now)
        return !d.cancelled && m in 1..99
    }

    /** Texto da pílula de estado. */
    fun pillText(d: Departure): String =
        if (d.status == LiveStatus.ATRASADO && d.delayMin > 0) "ATRASADO +${d.delayMin}" else d.status.label.uppercase()

    /**
     * Cores da pílula (fundo, texto) — StatusPill do Glass TV.
     * [onLight] = o mosaico é claro.
     */
    fun pill(status: LiveStatus, onLight: Boolean): Pair<Int, Int> = when (status) {
        LiveStatus.PREVISTO -> (if (onLight) 0x26000000 else 0x33FFFFFF) to (if (onLight) INK else WHITE)
        LiveStatus.ATRASADO -> ORANGE to INK
        LiveStatus.SUPRIMIDO -> RED to WHITE
    }

    /** O mosaico é claro? (regra do Glass TV para escolher a pílula) */
    fun tileIsLight(c: Colors, highlighted: Boolean): Boolean =
        if (highlighted) c.fg == WHITE else c.fg != WHITE

    fun withAlpha(color: Int, alpha: Float): Int =
        ((alpha * 255).toInt().coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
}

/**
 * Logótipos (os mesmos do Glass TV) em app/src/main/assets/logos/: cp.png, carris.png.
 * O da CP é pintado de branco (monochrome, como na TV).
 */
object Logos {
    private val cache = HashMap<String, Bitmap?>()

    fun name(mode: Mode) = if (mode == Mode.TRAIN) "cp" else "carris"
    fun monochrome(mode: Mode) = mode == Mode.TRAIN

    @Synchronized
    fun get(ctx: Context, mode: Mode): Bitmap? = cache.getOrPut(name(mode)) { load(ctx, name(mode)) }

    private fun load(ctx: Context, base: String): Bitmap? {
        for (ext in listOf("png", "webp", "jpg")) {
            val bmp = runCatching {
                ctx.assets.open("logos/$base.$ext").use { BitmapFactory.decodeStream(it) }
            }.getOrNull() ?: continue
            val h = 96
            if (bmp.height <= h) return bmp
            val w = (bmp.width * h.toFloat() / bmp.height).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(bmp, w, h, true)
        }
        return null
    }
}
