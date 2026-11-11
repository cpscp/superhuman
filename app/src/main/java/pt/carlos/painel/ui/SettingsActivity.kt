@file:OptIn(ExperimentalMaterial3Api::class)

package pt.carlos.painel.ui

import android.app.role.RoleManager
import android.Manifest
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import pt.carlos.painel.data.CalInfo
import pt.carlos.painel.data.CalendarRepo
import pt.carlos.painel.data.GeoPlace
import pt.carlos.painel.data.Weather
import pt.carlos.painel.media.Media
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pt.carlos.painel.PainelApp
import pt.carlos.painel.lock.LockController
import pt.carlos.painel.theme.PainelTheme
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PainelTheme(PainelApp.instance.prefs) { SettingsScreen(onBack = { finish() }) }
        }
    }
}

private val Seeds = listOf(
    0xFF1A73E8, 0xFF0B8043, 0xFFD93025, 0xFFF29900,
    0xFF8E24AA, 0xFF00897B, 0xFF5F6368, 0xFFE8710A,
).map { it.toInt() }

private fun isDefaultHome(ctx: Context) =
    runCatching { ctx.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(false)

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val app = PainelApp.instance
    val prefs = app.prefs
    val ctx = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isDefault by remember { mutableStateOf(isDefaultHome(ctx)) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = isDefaultHome(ctx)
    }
    val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Definições") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = pad) {
            item { Section("Sistema") }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Ecrã inicial do tablet") },
                    supportingContent = {
                        Text(if (isDefault) "O botão Home abre sempre o Painel" else "O botão Home ainda abre outro launcher")
                    },
                    trailingContent = {
                        if (isDefault) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        else Button(onClick = {
                            val rm = ctx.getSystemService(RoleManager::class.java)
                            val ok = runCatching {
                                if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); true
                                } else false
                            }.getOrDefault(false)
                            if (!ok) runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                        }) { Text("Definir") }
                    },
                )
            }

            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Limpeza do tablet") },
                    supportingContent = { Text("Apps da Lenovo, de parceiros e da Google que podes remover") },
                    modifier = Modifier.clickable { ctx.startActivity(Intent(ctx, DebloatActivity::class.java)) },
                )
            }

            item { Section("Aspeto") }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Tema") },
                    supportingContent = {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            val labels = listOf("Sistema", "Claro", "Escuro")
                            labels.forEachIndexed { i, l ->
                                SegmentedButton(
                                    selected = prefs.themeMode == i,
                                    onClick = { prefs.themeMode = i },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = labels.size),
                                ) { Text(l) }
                            }
                        }
                    },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Cores do papel de parede") },
                    supportingContent = { Text("Material You: as cores seguem o papel de parede") },
                    trailingContent = { Switch(checked = prefs.dynamicColor, onCheckedChange = { prefs.dynamicColor = it; app.apps.restyle() }) },
                )
            }
            if (!prefs.dynamicColor) {
                item {
                    ListItem(
                        colors = rowColors,
                        headlineContent = { Text("Cor principal") },
                        supportingContent = {
                            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Seeds.forEach { c ->
                                    val selected = prefs.seed == c
                                    Box(
                                        Modifier
                                            .size(40.dp)
                                            .background(Color(c), CircleShape)
                                            .then(
                                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { prefs.seed = c; app.apps.restyle() }
                                    )
                                }
                            }
                        },
                    )
                }
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Papel de parede") },
                    supportingContent = { Text("Coleção ao estilo Pixel ou as tuas fotos") },
                    modifier = Modifier.clickable { ctx.startActivity(Intent(ctx, WallpaperActivity::class.java)) },
                )
            }


            item { Section("Ícones") }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Forma dos ícones") },
                    supportingContent = {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            val labels = listOf("Círculo", "Squircle", "Quadrado")
                            labels.forEachIndexed { i, l ->
                                SegmentedButton(
                                    selected = prefs.iconShape == i,
                                    onClick = { prefs.iconShape = i; app.apps.restyle() },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = labels.size),
                                ) { Text(l) }
                            }
                        }
                    },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Ícones temáticos") },
                    supportingContent = { Text("Ícones nas cores do tema, como no Pixel (apps que o suportam)") },
                    trailingContent = { Switch(checked = prefs.themedIcons, onCheckedChange = { prefs.themedIcons = it; app.apps.restyle() }) },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Nome e imagem de cada app") },
                    supportingContent = { Text("Toque longo num ícone › Editar ícone") },
                )
            }


            item { Section("Modo Display") }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Abrir") },
                    supportingContent = { Text("Botão \"Modo Display\" no ecrã inicial ou nas Definições rápidas (Modo Display)") },
                    modifier = Modifier.clickable { ctx.startActivity(Intent(ctx, DisplayActivity::class.java)) },
                )
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Comboio (CP)", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(prefs.trainStation, { prefs.trainStation = it }, label = { Text("Estação") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(prefs.trainDirection, { prefs.trainDirection = it }, label = { Text("Sentido") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Text("Autocarros (Carris)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(prefs.busTitle, { prefs.busTitle = it }, label = { Text("Nome do quadro") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(prefs.busStops, { prefs.busStops = it }, label = { Text("Paragens (números, separados por vírgulas)") }, singleLine = true, modifier = Modifier.weight(2f))
                    }
                }
            }
            item {
                val scope = rememberCoroutineScope()
                var query by remember { mutableStateOf("") }
                var results by remember { mutableStateOf<List<GeoPlace>>(emptyList()) }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Tempo: ${prefs.weatherPlace}", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Mudar localidade") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { scope.launch { results = Weather.search(query) } }),
                        trailingIcon = { TextButton(onClick = { scope.launch { results = Weather.search(query) } }) { Text("Procurar") } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    results.forEach { g ->
                        ListItem(
                            colors = rowColors,
                            headlineContent = { Text(g.name) },
                            supportingContent = { Text(g.region) },
                            modifier = Modifier.clickable {
                                prefs.setWeatherPlace(g.name, g.lat, g.lon)
                                results = emptyList(); query = ""
                            },
                        )
                    }
                }
            }
            item {
                var calOk by remember { mutableStateOf(CalendarRepo.hasPermission(ctx)) }
                var cals by remember { mutableStateOf<List<CalInfo>>(emptyList()) }
                val calLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { calOk = it }
                LaunchedEffect(calOk) { if (calOk) cals = CalendarRepo.calendars(ctx) }
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("Calendários na agenda", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                    if (!calOk) {
                        ListItem(
                            colors = rowColors,
                            headlineContent = { Text("Acesso ao calendário") },
                            supportingContent = { Text("Necessário para mostrar a agenda") },
                            trailingContent = { Button(onClick = { calLauncher.launch(Manifest.permission.READ_CALENDAR) }) { Text("Permitir") } },
                        )
                    }
                    cals.forEach { c ->
                        ListItem(
                            colors = rowColors,
                            leadingContent = { Box(Modifier.size(14.dp).background(Color(c.color), CircleShape)) },
                            headlineContent = { Text(c.name) },
                            supportingContent = { Text(c.account) },
                            trailingContent = {
                                Switch(
                                    checked = c.id.toString() !in prefs.hiddenCalendars,
                                    onCheckedChange = { on -> prefs.setCalendarHidden(c.id, !on) },
                                )
                            },
                        )
                    }
                }
            }
            item {
                val access = Media.hasAccess(ctx)
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Música (BitChord, YouTube, Nuvio)") },
                    supportingContent = { Text(if (access) "Ativo: o Display mostra e controla o que está a tocar" else "Precisa de \"Acesso a notificações\" para o Painel") },
                    trailingContent = {
                        if (!access) Button(onClick = { runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }) { Text("Ativar") }
                        else Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Modo noite automático") },
                    supportingContent = { Text("Das ${prefs.nightStart}h às ${prefs.nightEnd}h: ecrã escuro, só relógio e próximo comboio") },
                    trailingContent = { Switch(checked = prefs.nightAuto, onCheckedChange = { prefs.nightAuto = it }) },
                )
            }
            if (prefs.nightAuto) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Começa às ${prefs.nightStart}h", style = MaterialTheme.typography.bodyMedium)
                        Slider(value = prefs.nightStart.toFloat(), onValueChange = { prefs.nightStart = it.roundToInt() }, valueRange = 18f..23f, steps = 4)
                        Text("Acaba às ${prefs.nightEnd}h", style = MaterialTheme.typography.bodyMedium)
                        Slider(value = prefs.nightEnd.toFloat(), onValueChange = { prefs.nightEnd = it.roundToInt() }, valueRange = 5f..9f, steps = 3)
                    }
                }
            }

            item { Section("Ecrã de bloqueio") }
            item {
                var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
                val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    overlayOk = Settings.canDrawOverlays(ctx)
                }
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Ecrã de bloqueio do Painel") },
                    supportingContent = {
                        Text(
                            when {
                                !prefs.lockScreen -> "Como no Pixel 11: relógio grande, data, tempo e atalho da Casa"
                                !overlayOk -> "Falta autorizar \"Sobrepor a outras apps\" (toca aqui)"
                                else -> "Ativo. Deslizar para cima desbloqueia (pede o PIN, se tiveres)"
                            }
                        )
                    },
                    trailingContent = {
                        Switch(checked = prefs.lockScreen, onCheckedChange = { on ->
                            prefs.lockScreen = on
                            LockController.sync(ctx)
                            if (on && !Settings.canDrawOverlays(ctx)) {
                                overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                            }
                        })
                    },
                    modifier = Modifier.clickable(enabled = prefs.lockScreen && !overlayOk) {
                        overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                    },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Estilo do relógio") },
                    supportingContent = {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            val labels = listOf("Pixel", "Fino", "Forte")
                            labels.forEachIndexed { i, l ->
                                SegmentedButton(
                                    selected = prefs.clockStyle == i,
                                    onClick = { prefs.clockStyle = i },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = labels.size),
                                ) { Text(l) }
                            }
                        }
                    },
                )
            }

            item { Section("Ecrã inicial") }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Colunas: ${prefs.columns}") },
                    supportingContent = {
                        Slider(
                            value = prefs.columns.toFloat(),
                            onValueChange = { prefs.columns = it.roundToInt() },
                            valueRange = 4f..8f,
                            steps = 3,
                        )
                    },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Linhas: ${prefs.rows}") },
                    supportingContent = {
                        Slider(
                            value = prefs.rows.toFloat(),
                            onValueChange = { prefs.rows = it.roundToInt() },
                            valueRange = 3f..6f,
                            steps = 2,
                        )
                    },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Nomes das apps") },
                    trailingContent = { Switch(checked = prefs.showLabels, onCheckedChange = { prefs.showLabels = it }) },
                )
            }
            item {
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Data no topo") },
                    trailingContent = { Switch(checked = prefs.showGlance, onCheckedChange = { prefs.showGlance = it }) },
                )
            }

            item { Section("Apps ocultas") }
            val hidden = prefs.hiddenApps.toList()
            if (hidden.isEmpty()) {
                item {
                    ListItem(colors = rowColors, headlineContent = {
                        Text("Nenhuma. Toca sem largar numa app da gaveta para a ocultar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    })
                }
            } else {
                items(hidden, key = { it }) { key ->
                    val label = app.apps.byKey[key]?.label ?: key.substringBefore('/')
                    ListItem(
                        colors = rowColors,
                        headlineContent = { Text(label) },
                        trailingContent = { TextButton(onClick = { prefs.setHidden(key, false) }) { Text("Mostrar") } },
                    )
                }
            }

            item { Section("Sobre") }
            item {
                val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
                ListItem(
                    colors = rowColors,
                    headlineContent = { Text("Painel $version") },
                    supportingContent = { Text("Tipo de letra: Google Sans Flex (SIL Open Font License)") },
                )
            }
            item {
                val f = java.io.File(ctx.filesDir, "ultimo-erro.txt")
                if (f.exists()) {
                    val txt = remember { f.readText() }
                    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                    ListItem(
                        colors = rowColors,
                        headlineContent = { Text("Último erro registado") },
                        supportingContent = { Text(txt.lineSequence().take(3).joinToString("\n"), maxLines = 3) },
                        trailingContent = {
                            TextButton(onClick = { clip.setText(androidx.compose.ui.text.AnnotatedString(txt)) }) { Text("Copiar") }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Column(Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

