@file:OptIn(ExperimentalMaterial3Api::class)

package pt.carlos.painel.ui

import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.carlos.painel.PainelApp
import pt.carlos.painel.theme.PainelTheme
import pt.carlos.painel.wallpaper.WallPalette
import pt.carlos.painel.wallpaper.WallPalettes
import pt.carlos.painel.wallpaper.WallStyles
import pt.carlos.painel.wallpaper.WallpaperArt

class WallpaperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PainelTheme(PainelApp.instance.prefs) { WallpaperScreen(onBack = { finish() }) } }
    }
}

private data class Choice(val style: Int, val palette: WallPalette)

@Composable
private fun WallpaperScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var chosen by remember { mutableStateOf<Choice?>(null) }
    var applying by remember { mutableStateOf(false) }

    fun apply(c: Choice, which: Int) {
        applying = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) {
                runCatching {
                    val dm = ctx.resources.displayMetrics
                    val side = maxOf(dm.widthPixels, dm.heightPixels)   // quadrado: serve em horizontal e vertical
                    val bmp = WallpaperArt.render(c.style, c.palette, side, side)
                    WallpaperManager.getInstance(ctx).setBitmap(bmp, null, true, which)
                    bmp.recycle()
                }.isSuccess
            }
            applying = false
            chosen = null
            Toast.makeText(ctx, if (ok) "Papel de parede aplicado" else "Não foi possível aplicar", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Papéis de parede") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = 32.dp)) {
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Outlined.Image, null) },
                    headlineContent = { Text("As minhas fotos ou do sistema") },
                    supportingContent = { Text("Escolher uma imagem tua ou os papéis de parede do tablet") },
                    modifier = Modifier.clickable {
                        runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Papel de parede")) }
                    },
                )
            }
            WallStyles.forEachIndexed { styleIdx, styleName ->
                item(key = "t$styleIdx") {
                    Text(
                        styleName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 10.dp),
                    )
                }
                item(key = "r$styleIdx") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(WallPalettes.size) { i ->
                            val pal = WallPalettes[i]
                            Column(Modifier.width(240.dp)) {
                                Thumb(styleIdx, pal, Modifier.clickable { chosen = Choice(styleIdx, pal) })
                                Text(pal.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    chosen?.let { c ->
        AlertDialog(
            onDismissRequest = { if (!applying) chosen = null },
            title = { Text("${WallStyles[c.style]} · ${c.palette.name}") },
            text = {
                Column {
                    Thumb(c.style, c.palette, Modifier)
                    Spacer(Modifier.height(12.dp))
                    Text(if (applying) "A aplicar…" else "Onde queres usar este papel de parede?")
                }
            },
            confirmButton = {
                TextButton(enabled = !applying, onClick = { apply(c, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) }) { Text("Ambos") }
            },
            dismissButton = {
                Box {
                    androidx.compose.foundation.layout.Row {
                        TextButton(enabled = !applying, onClick = { apply(c, WallpaperManager.FLAG_SYSTEM) }) { Text("Ecrã inicial") }
                        TextButton(enabled = !applying, onClick = { apply(c, WallpaperManager.FLAG_LOCK) }) { Text("Bloqueio") }
                    }
                }
            },
        )
    }
}

@Composable
private fun Thumb(style: Int, pal: WallPalette, modifier: Modifier) {
    val img by produceState<ImageBitmap?>(initialValue = null, style, pal) {
        value = withContext(Dispatchers.Default) { WallpaperArt.render(style, pal, 480, 300).asImageBitmap() }
    }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        img?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)) }
    }
}
