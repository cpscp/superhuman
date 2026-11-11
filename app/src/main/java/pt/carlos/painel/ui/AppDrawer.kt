@file:OptIn(ExperimentalFoundationApi::class)

package pt.carlos.painel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pt.carlos.painel.PainelApp
import pt.carlos.painel.data.AppEntry
import java.text.Normalizer

private fun norm(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

/** Gaveta de apps com pesquisa (sobe com um gesto ou pela barra de pesquisa). */
@Composable
fun AppDrawer(
    app: PainelApp,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onAddToHome: (AppEntry) -> Unit,
) {
    val repo = app.apps
    val prefs = app.prefs
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val close by rememberUpdatedState(onClose)

    val list = remember(repo.apps, query, prefs.hiddenApps) {
        val q = norm(query.trim())
        repo.apps.filter { it.key !in prefs.hiddenApps && (q.isEmpty() || norm(it.label).contains(q)) }
    }

    LaunchedEffect(focusSearch) {
        if (focusSearch) { delay(120); runCatching { focus.requestFocus() } }
    }

    // Puxar para baixo quando a lista já está no topo fecha a gaveta.
    val pullToClose = remember {
        object : NestedScrollConnection {
            var acc = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    acc += available.y
                    if (acc > 160f) { acc = 0f; close() }
                } else acc = 0f
                return Offset.Zero
            }
        }
    }

    fun launch(e: AppEntry) {
        keyboard?.hide()
        repo.launch(e)
        close()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(Unit) {
                        var t = 0f
                        detectVerticalDragGestures(onDragStart = { t = 0f }) { _, d ->
                            t += d
                            if (t > 80f) { t = 0f; close() }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                )
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Pesquisar apps") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { list.firstOrNull()?.let { launch(it) } }),
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier.weight(1f).nestedScroll(pullToClose),
            ) {
                items(list, key = { it.key }) { e ->
                    DrawerApp(e, app, onLaunch = { launch(it) }, onAddToHome = onAddToHome)
                }
            }
        }
    }
}

@Composable
private fun DrawerApp(e: AppEntry, app: PainelApp, onLaunch: (AppEntry) -> Unit, onAddToHome: (AppEntry) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        AppIcon(
            entry = e,
            showLabel = true,
            onWallpaper = false,
            modifier = Modifier.combinedClickable(onClick = { onLaunch(e) }, onLongClick = { menu = true }),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Adicionar ao ecrã inicial") },
                leadingIcon = { Icon(Icons.Outlined.AddToHomeScreen, null) },
                onClick = { menu = false; onAddToHome(e) },
            )
            val inDock = app.layout.inDock(e.key)
            DropdownMenuItem(
                text = { Text(if (inDock) "Tirar da dock" else "Fixar na dock") },
                leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                onClick = { menu = false; if (inDock) app.layout.dockRemove(e.key) else app.layout.dockAdd(e.key) },
            )
            val editIcon = LocalEditIcon.current
            DropdownMenuItem(
                text = { Text("Editar ícone") },
                leadingIcon = { Icon(Icons.Outlined.Palette, null) },
                onClick = { menu = false; editIcon(e) },
            )
            DropdownMenuItem(
                text = { Text("Ocultar app") },
                leadingIcon = { Icon(Icons.Outlined.VisibilityOff, null) },
                onClick = { menu = false; app.prefs.setHidden(e.key, true) },
            )
            DropdownMenuItem(
                text = { Text("Informações da app") },
                leadingIcon = { Icon(Icons.Outlined.Info, null) },
                onClick = { menu = false; app.apps.details(e) },
            )
            DropdownMenuItem(
                text = { Text("Desinstalar") },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = { menu = false; app.apps.uninstall(e) },
            )
        }
    }
}
