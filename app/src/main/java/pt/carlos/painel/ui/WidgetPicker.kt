@file:OptIn(ExperimentalMaterial3Api::class)

package pt.carlos.painel.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class WidgetItem(val info: AppWidgetProviderInfo, val label: String)
private data class WidgetGroup(val pkg: String, val label: String, val widgets: List<WidgetItem>)

private fun norm(s: String) =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

/** Lista de todos os widgets instalados, agrupados por app, com pré-visualização. */
@Composable
fun WidgetPicker(onDismiss: () -> Unit, onPick: (AppWidgetProviderInfo) -> Unit) {
    val ctx = LocalContext.current
    val groups by produceState(initialValue = emptyList<WidgetGroup>()) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            AppWidgetManager.getInstance(ctx).installedProviders
                .groupBy { it.provider.packageName }
                .map { (pkg, list) ->
                    val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                    WidgetGroup(pkg, label, list.map { WidgetItem(it, it.loadLabel(pm)) })
                }
                .sortedBy { it.label.lowercase() }
        }
    }

    var query by remember { mutableStateOf("") }
    // Pesquisa pelo nome do widget ou da app (sem acentos): se a app bate, mostra todos os seus widgets.
    val shown = remember(groups, query) {
        val q = norm(query.trim())
        if (q.isEmpty()) groups
        else groups.mapNotNull { g ->
            if (norm(g.label).contains(q)) g
            else g.widgets.filter { norm(it.label).contains(q) }.takeIf { it.isNotEmpty() }?.let { g.copy(widgets = it) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "Widgets",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Pesquisar widgets ou apps") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Limpar") }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (groups.isNotEmpty() && shown.isEmpty()) {
            Text(
                "Nenhum widget encontrado para \"$query\".",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(shown, key = { it.pkg }) { g ->
                Text(
                    g.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(g.widgets, key = { it.info.provider.flattenToString() }) { w ->
                        WidgetCard(ctx, w.info, w.label) { onPick(w.info) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(ctx: Context, info: AppWidgetProviderInfo, label: String, onClick: () -> Unit) {
    val preview by produceState<ImageBitmap?>(initialValue = null, info) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val d = info.loadPreviewImage(ctx, 0) ?: info.loadIcon(ctx, 0)
                val w = d.intrinsicWidth.coerceIn(1, 480)
                val h = d.intrinsicHeight.coerceIn(1, 480)
                d.toBitmap(w, h).asImageBitmap()
            }.getOrNull()
        }
    }
    val size = if (info.targetCellWidth > 0) "${info.targetCellWidth} × ${info.targetCellHeight}" else ""
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.width(220.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(140.dp).padding(12.dp), contentAlignment = Alignment.Center) {
            preview?.let { Image(it, contentDescription = label, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (size.isNotEmpty()) Text(size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
