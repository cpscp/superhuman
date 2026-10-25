package com.carlos.superhuman.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate

val CardShape = RoundedCornerShape(22.dp)
val InsetShape = RoundedCornerShape(16.dp)

@Composable
fun Modifier.noRippleClick(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

@Composable
fun SHCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = SH.c
    Column(
        modifier
            .clip(CardShape)
            .background(c.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun InsetBox(modifier: Modifier = Modifier, padding: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(InsetShape).background(SH.c.inset).padding(padding), content = content)
}

@Composable
fun CircleIconButton(icon: ImageVector, modifier: Modifier = Modifier, filled: Boolean = false, size: Dp = 42.dp, badge: Boolean = false, onClick: () -> Unit) {
    val c = SH.c
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) c.ink else c.card)
            .border(BorderStroke(1.dp, if (filled) c.ink else c.line), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (filled) c.onInk else c.ink, modifier = Modifier.size(size * 0.48f))
        if (badge) Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(8.dp).clip(CircleShape).background(c.accent))
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val c = SH.c
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (enabled) c.ink else c.ink3)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = c.onInk, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = Type.title, color = c.onInk)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val c = SH.c
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = Type.title, color = c.ink)
    }
}

@Composable
fun SmallPill(text: String, selected: Boolean = false, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = SH.c
    val bg by animateColorAsState(if (selected) c.ink else c.card, label = "pill")
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) c.ink else c.line, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(text, style = Type.small, color = if (selected) c.onInk else c.ink) }
}

/** Etiqueta colorida (calendário, prioridade, categoria). */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, style = Type.tiny, color = if (SH.c.dark) color else color.darken(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun Color.darken(f: Float = 0.62f) = Color(red * f, green * f, blue * f, alpha)

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = SH.c
    Row(modifier.clip(RoundedCornerShape(50)).background(c.card).border(1.dp, c.line, RoundedCornerShape(50)).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (sel) c.inset else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(o, style = Type.small, color = if (sel) c.ink else c.ink2) }
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, modifier: Modifier = Modifier, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Type.h2, color = SH.c.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Text(action, style = Type.small, color = SH.c.ink, modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
    }
}

@Composable
fun SHTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.body,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    leading: ImageVector? = null,
) {
    val c = SH.c
    Row(
        modifier.clip(InsetShape).background(c.inset).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leading != null) { Icon(leading, null, tint = c.ink2, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)) }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = style, color = c.ink3)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = style.copy(color = c.ink),
                singleLine = singleLine,
                minLines = minLines,
                keyboardOptions = keyboard,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Campo "invisível" para títulos grandes (como no ecrã "New task"). */
@Composable
fun BareTextField(value: String, onChange: (String) -> Unit, placeholder: String, style: TextStyle, modifier: Modifier = Modifier, singleLine: Boolean = false) {
    val c = SH.c
    Box(modifier) {
        if (value.isEmpty()) Text(placeholder, style = style, color = c.ink3)
        BasicTextField(
            value = value, onValueChange = onChange, textStyle = style.copy(color = c.ink), singleLine = singleLine,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent), modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Linha "chave → valor >" como no detalhe de task do vídeo. */
@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val c = SH.c
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.ink2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = Type.body, color = c.ink2, modifier = Modifier.weight(1f))
        if (trailing != null) trailing() else {
            Text(value, style = Type.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (onClick != null) { Spacer(Modifier.width(4.dp)); Icon(Icons.Rounded.ChevronRight, null, tint = c.ink3, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun LineDivider() = Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(SH.c.line))

@Composable
fun SHCheck(checked: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val c = SH.c
    Box(
        modifier.size(24.dp).clip(CircleShape)
            .background(if (checked) c.ink else Color.Transparent)
            .border(1.5.dp, if (checked) c.ink else c.ink3, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(Icons.Rounded.Check, null, tint = c.onInk, modifier = Modifier.size(15.dp)) }
}

@Composable
fun SHSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = SH.c
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onInk, checkedTrackColor = c.ink,
            uncheckedThumbColor = c.ink3, uncheckedTrackColor = c.inset, uncheckedBorderColor = c.line,
        ),
    )
}

@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, color: Color = SH.c.ink, height: Dp = 4.dp) {
    val c = SH.c
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(c.inset)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = Type.title, color = SH.c.ink)
        Spacer(Modifier.height(4.dp))
        Text(body, style = Type.small, color = SH.c.ink2)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, sub: String? = null) {
    val c = SH.c
    SHCard(modifier, padding = 14.dp) {
        if (icon != null) { Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.height(18.dp)) }
        Text(label, style = Type.small, color = c.ink2)
        Text(value, style = Type.h2, color = c.ink)
        if (sub != null) Text(sub, style = Type.tiny, color = c.ink3)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ChipRow(options: List<String>, selected: Set<String>, modifier: Modifier = Modifier, onToggle: (String) -> Unit) {
    // Quebra de linha em vez de scroll horizontal: nada fica cortado na margem.
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> SmallPill(o, o in selected) { onToggle(o) } }
    }
}

/** Deixa uma linha com scroll horizontal ocupar a largura toda do ecrã, mantendo a margem no início e no fim. */
fun Modifier.bleed(horizontal: Dp): Modifier = this.layout { measurable, constraints ->
    val px = horizontal.roundToPx()
    val wide = if (constraints.hasBoundedWidth) constraints.maxWidth + px * 2 else constraints.maxWidth
    val placeable = measurable.measure(constraints.copy(minWidth = wide, maxWidth = wide))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-px, 0) }
}

/** Esqueleto dos ecrãs secundários: botão voltar, título central e ações. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(start = 18.dp, end = 18.dp, bottom = 130.dp),
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SH.c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
                Text(title, style = Type.h2, color = SH.c.ink, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(title, style = Type.h1, color = SH.c.ink, modifier = Modifier.weight(1f).padding(end = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onDismiss: () -> Unit, footer: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = SH.c
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = c.card,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(c.line)) },
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Título fixo
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Type.h2, color = c.ink, modifier = Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Close, size = 36.dp, onClick = onDismiss)
            }
            // Conteúdo com scroll, para os botões no fim estarem sempre acessíveis
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = if (footer == null) 28.dp else 12.dp)
                    .then(if (footer == null) Modifier.navigationBarsPadding() else Modifier),
                content = content,
            )
            if (footer != null) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 16.dp).navigationBarsPadding().imePadding(), content = footer)
            }
        }
    }
}

fun pickDate(ctx: Context, initial: LocalDate = LocalDate.now(), onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(ctx, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
}

fun pickTime(ctx: Context, initialMinutes: Int = 9 * 60, onPicked: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, initialMinutes / 60, initialMinutes % 60, true).show()
}

fun parseMoney(s: String): Double? = s.trim().replace("€", "").replace(" ", "").replace(",", ".").toDoubleOrNull()

@Composable
fun MiniStatRow(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, value) ->
            SHCard(Modifier.weight(1f), padding = 12.dp) {
                Text(value, style = Type.h2, color = SH.c.ink, maxLines = 1)
                Text(label, style = Type.tiny, color = SH.c.ink2, maxLines = 1)
            }
        }
    }
}
