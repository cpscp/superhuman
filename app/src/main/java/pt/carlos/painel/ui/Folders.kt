@file:OptIn(ExperimentalFoundationApi::class)

package pt.carlos.painel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pt.carlos.painel.PainelApp
import pt.carlos.painel.data.Item

private val FolderShadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1.5f), blurRadius = 6f)

/** Pasta no ecrã inicial: círculo com até quatro apps em miniatura, como no Pixel. */
@Composable
fun FolderIcon(item: Item, app: PainelApp, onOpen: () -> Unit, onLongPress: () -> Unit, iconSize: Dp = 60.dp) {
    val previews = item.folderApps.mapNotNull { app.apps.byKey[it] }.take(4)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(iconSize)) {
                Column(
                    Modifier.fillMaxSize().padding(iconSize * 0.16f),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    previews.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { e -> Image(e.icon, null, Modifier.size(iconSize * 0.30f)) }
                        }
                    }
                }
            }
            if (app.prefs.showLabels) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.name.ifBlank { "Pasta" },
                    style = MaterialTheme.typography.labelLarge.copy(shadow = FolderShadow),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 104.dp),
                )
            }
        }
    }
}

/** Pasta aberta: nome editável e as apps; toque longo numa app para a tirar da pasta. */
@Composable
fun FolderDialog(item: Item, app: PainelApp, onDismiss: () -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name.ifBlank { "Pasta" }) }
    val focus = LocalFocusManager.current
    val apps = item.folderApps.mapNotNull { app.apps.byKey[it] }
    Dialog(onDismissRequest = { app.layout.renameFolder(item.id, name); onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { app.layout.renameFolder(item.id, name); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(apps, key = { it.key }) { e ->
                        var menu by remember { mutableStateOf(false) }
                        val editIcon = LocalEditIcon.current
                        Box(Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            AppIcon(
                                entry = e,
                                showLabel = true,
                                onWallpaper = false,
                                modifier = Modifier.combinedClickable(
                                    onClick = { app.layout.renameFolder(item.id, name); app.apps.launch(e); onDismiss() },
                                    onLongClick = { menu = true },
                                ),
                            )
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Tirar da pasta") },
                                    leadingIcon = { Icon(Icons.Outlined.Logout, null) },
                                    onClick = {
                                        menu = false
                                        app.layout.removeFromFolder(item.id, e.key, app.layout.cols, app.layout.rows)
                                        if (apps.size <= 2) onDismiss()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Editar ícone") },
                                    leadingIcon = { Icon(Icons.Outlined.Palette, null) },
                                    onClick = { menu = false; editIcon(e) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Informações da app") },
                                    leadingIcon = { Icon(Icons.Outlined.Info, null) },
                                    onClick = { menu = false; app.apps.details(e) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
