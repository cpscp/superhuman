package pt.carlos.partidas.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.carlos.partidas.data.BoardLook
import pt.carlos.partidas.data.CacheEntry
import pt.carlos.partidas.data.Logos
import pt.carlos.partidas.model.Board
import pt.carlos.partidas.model.Departure
import pt.carlos.partidas.model.LiveStatus
import pt.carlos.partidas.model.Mode
import pt.carlos.partidas.data.Text as T

/** Google Sans Flex em todo o quadro. */
object PixelFonts {
    val display: FontFamily get() = GoogleSans
    val text: FontFamily get() = GoogleSans
}

/** Tipografia do Glass TV (Type.*), em Google Sans Flex. */
private object Type {
    private val white = Color(0xFFF5F5F7)
    private fun s(size: Int, w: FontWeight, track: Float = 0f) =
        TextStyle(fontFamily = GoogleSans, fontSize = size.sp, fontWeight = w, letterSpacing = track.sp, color = white)
    val Headline = s(17, FontWeight.SemiBold)
    val Body = s(15, FontWeight.Normal)
    val Caption = s(13, FontWeight.Medium, 0.1f)
    val Tiny = s(11, FontWeight.SemiBold, 0.6f)
    val Board = s(30, FontWeight.Bold, -0.5f)
}

private fun c(argb: Int) = Color(argb)

/**
 * Quadro de partidas igual ao do Glass TV: cor da marca com reflexo de vidro, coluna de
 * informação à esquerda (logo, título, sentido/paragens, relógio, TEMPO REAL), mosaicos à direita
 * (o primeiro em destaque, desliza para ver até 10) e faixa inferior em duas cores.
 *
 * [visibleTiles] mosaicos visíveis de uma vez; [infoWidth] largura da coluna da esquerda.
 */
@Composable
fun DepartureBoard(
    board: Board,
    cache: CacheEntry?,
    now: Long,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 214.dp,
    visibleTiles: Int = 2,
    infoWidth: Dp = 112.dp,
    onRefresh: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val col = BoardLook.colors(board.mode)
    val fg = c(col.fg)
    val logo = remember(board.mode) { Logos.get(ctx, board.mode) }
    val items = cache?.items?.filter { it.etaMs >= now - 30_000L }.orEmpty()
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier
            .height(height)
            .clip(shape)
            // Material 3: cor sólida da marca, sem vidro nem reflexos.
            .background(c(col.base))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(Modifier.weight(1f).fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp)) {
            // coluna de informação
            Column(Modifier.width(infoWidth).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (logo != null) {
                            Image(
                                logo.asImageBitmap(), if (board.mode == Mode.TRAIN) "CP" else "Carris",
                                Modifier.height(26.dp), contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                                colorFilter = if (Logos.monochrome(board.mode)) ColorFilter.tint(fg) else null
                            )
                        } else {
                            Text(if (board.mode == Mode.TRAIN) "CP" else "Carris", style = Type.Headline.copy(color = fg, fontWeight = FontWeight.Bold))
                        }
                    }
                    if (onRefresh != null) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onRefresh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (refreshing) "…" else "↻", style = Type.Headline.copy(color = fg))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(T.breakBeforeHyphens(board.displayTitle), style = Type.Headline.copy(color = fg, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(board.subtitle, style = Type.Caption.copy(color = fg.copy(alpha = .75f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Text(T.hhmm(now), style = Type.Board.copy(color = fg, fontSize = 26.sp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(fg); Spacer(Modifier.width(6.dp))
                    val live = when {
                        refreshing -> "A ATUALIZAR…"
                        cache?.error != null && items.isNotEmpty() -> "OFFLINE · ${T.hhmm(cache.fetchedAt)}"
                        else -> "TEMPO REAL"
                    }
                    Text(live, style = Type.Tiny.copy(color = fg.copy(alpha = .8f)), maxLines = 1)
                }
            }
            Spacer(Modifier.width(10.dp))
            // mosaicos
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(c(col.tile)), Alignment.Center) {
                        val fgT = c(col.tileFg)
                        when {
                            cache?.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Text("Sem dados", style = Type.Headline.copy(color = fgT))
                                Text(cache.error, style = Type.Caption.copy(color = fgT.copy(alpha = .7f)), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            cache == null || refreshing -> Text("A carregar…", style = Type.Body.copy(color = fgT.copy(alpha = .8f)))
                            else -> Text("Sem partidas nas próximas horas", style = Type.Body.copy(color = fgT.copy(alpha = .8f)))
                        }
                    }
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val gap = 8.dp
                        val tileW = (maxWidth - gap * (visibleTiles - 1)) / visibleTiles
                        val padded: List<Departure?> =
                            if (items.size >= visibleTiles) items else items + List(visibleTiles - items.size) { null }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxSize()) {
                            items(padded.size) { i ->
                                Tile(Modifier.width(tileW).fillMaxHeight(), padded[i], highlighted = i == 0 && padded[i] != null, col = col, now = now)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(modifier: Modifier, s: Departure?, highlighted: Boolean, col: BoardLook.Colors, now: Long) {
    val bg = c(if (highlighted) col.hiTile else col.tile)
    val fg = c(if (highlighted) col.hiFg else col.tileFg)
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(bg)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        if (s == null) {
            Spacer(Modifier.weight(1f))
            Text("—", style = Type.Headline.copy(color = fg.copy(alpha = .35f)))
            Spacer(Modifier.weight(1f))
            return@Column
        }
        val cancelled = s.cancelled
        // tempo que falta, em destaque
        Row(verticalAlignment = Alignment.Bottom) {
            Text(BoardLook.bigNumber(s, now), style = Type.Board.copy(color = fg, fontSize = 30.sp, lineHeight = 30.sp), maxLines = 1)
            if (BoardLook.showMinSuffix(s, now)) Text(" min", style = Type.Caption.copy(color = fg.copy(alpha = .75f)), modifier = Modifier.padding(bottom = 3.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                T.hhmm(s.etaMs), maxLines = 1,
                style = Type.Caption.copy(color = fg.copy(alpha = .85f), textDecoration = if (cancelled) TextDecoration.LineThrough else null)
            )
            s.oldTimeMs?.takeIf { !cancelled }?.let {
                Text("  ${T.hhmm(it)}", style = Type.Tiny.copy(color = fg.copy(alpha = .5f), textDecoration = TextDecoration.LineThrough))
            }
        }
        Spacer(Modifier.height(6.dp))
        if (s.badge.isNotBlank()) {
            Text(
                s.badge, maxLines = 1,
                style = Type.Tiny.copy(color = c(if (highlighted) col.hiTile else col.base), fontWeight = FontWeight.Bold),
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(fg).padding(horizontal = 5.dp, vertical = 1.dp)
            )
            Spacer(Modifier.height(3.dp))
        }
        Text(s.destination.ifBlank { "—" }, style = Type.Headline.copy(color = fg, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(s.detail, style = Type.Tiny.copy(color = fg.copy(alpha = .7f), fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        StatusPill(s, onLight = BoardLook.tileIsLight(col, highlighted))
    }
}

@Composable
private fun StatusPill(d: Departure, onLight: Boolean) {
    val (bg, fg) = BoardLook.pill(d.status, onLight)
    Text(
        BoardLook.pillText(d), style = Type.Tiny.copy(color = c(fg), fontSize = 10.sp), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c(bg)).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/** Ponto a pulsar = dados em tempo real (como no Glass TV). */
@Composable
private fun LiveDot(color: Color) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(1f, .25f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(color))
}

/** Usado no editor para mostrar o estado com as mesmas cores. */
fun statusColor(s: LiveStatus): Color = when (s) {
    LiveStatus.PREVISTO -> Color(0xFFF5F5F7)
    LiveStatus.ATRASADO -> Color(BoardLook.ORANGE)
    LiveStatus.SUPRIMIDO -> Color(BoardLook.RED)
}
