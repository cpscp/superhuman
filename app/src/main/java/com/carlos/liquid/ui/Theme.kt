package com.carlos.liquid.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.carlos.liquid.R
import com.carlos.liquid.data.ClockFont
import com.carlos.liquid.data.FontChoice
import java.io.File
import java.nio.ByteBuffer

@Immutable
class Palette(val dark: Boolean) {
    val label = if (dark) Color.White else Color.Black
    val secondary = if (dark) Color(0x99EBEBF5) else Color(0x993C3C43)
    val tertiary = if (dark) Color(0x4DEBEBF5) else Color(0x4D3C3C43)
    val groupedBg = if (dark) Color.Black else Color(0xFFF2F2F7)
    val cell = if (dark) Color(0xFF1C1C1E) else Color.White
    val cell2 = if (dark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val separator = if (dark) Color(0x5C545458) else Color(0x4A3C3C43)
    val fill = if (dark) Color(0x3D767680) else Color(0x33787880)
    val accent = if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val red = if (dark) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val green = if (dark) Color(0xFF30D158) else Color(0xFF34C759)
    val orange = if (dark) Color(0xFFFF9F0A) else Color(0xFFFF9500)
    val yellow = if (dark) Color(0xFFFFD60A) else Color(0xFFFFCC00)
    val teal = if (dark) Color(0xFF40C8E0) else Color(0xFF30B0C7)
    val indigo = if (dark) Color(0xFF5E5CE6) else Color(0xFF5856D6)
    val purple = if (dark) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
    val pink = if (dark) Color(0xFFFF375F) else Color(0xFFFF2D55)
    val gray = Color(0xFF8E8E93)
    val blue = accent
}

val LocalPalette = staticCompositionLocalOf { Palette(false) }
val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

object Fonts {
    val inter = FontFamily(
        Font(R.font.inter_300, FontWeight.Light),
        Font(R.font.inter_400, FontWeight.Normal),
        Font(R.font.inter_500, FontWeight.Medium),
        Font(R.font.inter_600, FontWeight.SemiBold),
        Font(R.font.inter_700, FontWeight.Bold),
        Font(R.font.inter_800, FontWeight.ExtraBold),
        Font(R.font.inter_900, FontWeight.Black),
    )
    val rounded = FontFamily(Font(R.font.nunito_700, FontWeight.Bold), Font(R.font.nunito_900, FontWeight.Black))
    val serif = FontFamily(Font(R.font.serif_400, FontWeight.Normal))
    val condensed = FontFamily(Font(R.font.oswald_500, FontWeight.Medium), Font(R.font.oswald_700, FontWeight.Bold))
    val mono = FontFamily(Font(R.font.mono_500, FontWeight.Medium))

    @Volatile var custom: FontFamily? = null
        private set
    var customName: String = ""
        private set

    fun fontsDir(ctx: Context) = File(ctx.filesDir, "fonts")

    fun ui(choice: FontChoice): FontFamily = when (choice) {
        FontChoice.INTER -> inter
        FontChoice.SYSTEM -> FontFamily.Default
        FontChoice.CUSTOM -> custom ?: inter
    }

    fun clock(f: ClockFont): FontFamily = when (f) {
        ClockFont.INTER, ClockFont.INTER_LIGHT -> inter
        ClockFont.ROUNDED -> rounded
        ClockFont.SERIF -> serif
        ClockFont.CONDENSED -> condensed
        ClockFont.MONO -> mono
        ClockFont.CUSTOM -> custom ?: inter
    }

    /** Carrega os tipos de letra importados (ex.: SF-Pro.ttf). Suporta fontes variáveis e ficheiros por peso. */
    @OptIn(ExperimentalTextApi::class)
    fun loadCustom(ctx: Context) {
        val files = fontsDir(ctx).listFiles()?.filter { it.extension.lowercase() in setOf("ttf", "otf") }.orEmpty()
        if (files.isEmpty()) { custom = null; customName = ""; return }
        val list = ArrayList<Font>()
        val variable = files.firstOrNull { isVariable(it) }
        if (variable != null) {
            for (w in listOf(100, 200, 300, 400, 500, 600, 700, 800, 900)) list += Font(variable, FontWeight(w))
            customName = variable.nameWithoutExtension
        } else {
            val used = HashSet<Int>()
            for (f in files.sortedBy { it.name.length }) {
                val w = weightFromName(f.name)
                if (used.add(w)) list += Font(f, FontWeight(w))
            }
            customName = files.minByOrNull { it.name.length }!!.nameWithoutExtension.substringBefore('-')
        }
        custom = runCatching { FontFamily(list) }.getOrNull()
    }

    fun import(ctx: Context, uri: Uri, name: String): Boolean = runCatching {
        val dir = fontsDir(ctx); dir.mkdirs()
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "font.ttf" }
        ctx.contentResolver.openInputStream(uri)!!.use { inp -> File(dir, safe).outputStream().use { inp.copyTo(it) } }
        loadCustom(ctx)
        custom != null
    }.getOrDefault(false)

    fun clearCustom(ctx: Context) { fontsDir(ctx).deleteRecursively(); custom = null; customName = "" }

    private fun weightFromName(n: String): Int {
        val s = n.lowercase()
        return when {
            "black" in s || "heavy" in s -> 900
            "extrabold" in s || "ultrabold" in s -> 800
            "semibold" in s || "demibold" in s -> 600
            "bold" in s -> 700
            "medium" in s -> 500
            "ultralight" in s || "extralight" in s -> 200
            "thin" in s -> 100
            "light" in s -> 300
            else -> 400
        }
    }

    /** Verifica se o ficheiro tem a tabela 'fvar' (fonte variável). */
    private fun isVariable(f: File): Boolean = runCatching {
        val head = ByteArray(12 + 16 * 64)
        val n = f.inputStream().use { it.read(head) }
        val bb = ByteBuffer.wrap(head, 0, n)
        val numTables = bb.getShort(4).toInt() and 0xffff
        (0 until minOf(numTables, 64)).any { i ->
            val o = 12 + i * 16
            o + 4 <= n && String(head, o, 4, Charsets.US_ASCII) == "fvar"
        }
    }.getOrDefault(false)
}

/** Texto com o tipo de letra do launcher. */
@Composable
fun LText(
    text: String,
    size: TextUnit = 17.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalPalette.current.label,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    shadow: Boolean = false,
    family: FontFamily? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            fontFamily = family ?: LocalFont.current,
            textAlign = align ?: TextAlign.Unspecified,
            letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else (if (size.value >= 20f) (-0.02 * size.value).sp else TextUnit.Unspecified),
            lineHeight = lineHeight,
            shadow = if (shadow) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 4f) else null,
        ),
    )
}
