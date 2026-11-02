package com.carlos.liquid.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AS
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.carlos.liquid.data.AppRepo
import com.carlos.liquid.data.Appearance
import com.carlos.liquid.data.FontChoice
import com.carlos.liquid.data.GlassStyle
import com.carlos.liquid.data.NewApps
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.SwipeDown
import com.carlos.liquid.lock.LockActivity
import com.carlos.liquid.lock.LockController
import com.carlos.liquid.lock.NotifService
import com.carlos.liquid.ui.Fonts
import com.carlos.liquid.ui.GlassSheet
import com.carlos.liquid.ui.Group
import com.carlos.liquid.ui.LSlider
import com.carlos.liquid.ui.LText
import com.carlos.liquid.ui.LocalPalette
import com.carlos.liquid.ui.Row2
import com.carlos.liquid.ui.Segmented
import com.carlos.liquid.ui.SheetTitle
import com.carlos.liquid.ui.ToggleRow
import com.carlos.liquid.ui.home.CustomizeContent
import com.carlos.liquid.ui.home.WallpaperContent
import com.carlos.liquid.ui.home.IconImage
import com.carlos.liquid.ui.home.rememberIconSpec
import com.carlos.liquid.ui.noRippleClick
import com.carlos.liquid.ui.widgets.ActionButton
import com.carlos.liquid.ui.widgets.HealthPermissionBlock
import com.carlos.liquid.ui.widgets.WeatherCityConfig

/** Recompõe quando a atividade volta ao primeiro plano (para atualizar estados de permissões). */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

fun isDefaultHome(ctx: Context) = ctx.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
fun hasPerm(ctx: Context, p: String) = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

fun openNotifAccess(ctx: Context) {
    val i = if (Build.VERSION.SDK_INT >= 30) Intent(AS.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(AS.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(ctx, NotifService::class.java).flattenToString())
    else Intent(AS.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
        runCatching { ctx.startActivity(Intent(AS.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

fun openOverlay(ctx: Context) = runCatching {
    ctx.startActivity(Intent(AS.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Passos de configuração (no primeiro arranque e nas Definições). */
@Composable
fun ColumnScope.SetupRows() {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    @Suppress("UNUSED_VARIABLE") val tick = rememberResumeTick()
    val role = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val cal = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val ok = "Ativo"
    Row2("Launcher predefinido", Icons.Rounded.Home, p.accent, value = if (isDefaultHome(ctx)) ok else "Definir", chevron = !isDefaultHome(ctx)) {
        runCatching { role.launch(ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)) }
            .onFailure { runCatching { ctx.startActivity(Intent(AS.ACTION_HOME_SETTINGS)) } }
    }
    Row2("Notificações", Icons.Rounded.Notifications, p.red, subtitle = "Indicadores nos ícones e ecrã bloqueado",
        value = if (NotifService.enabled(ctx)) ok else "Permitir", chevron = true) { openNotifAccess(ctx) }
    Row2("Sobrepor a outras apps", Icons.Rounded.Layers, p.indigo, subtitle = "Necessário para mostrar o ecrã bloqueado",
        value = if (LockController.canShow(ctx)) ok else "Permitir", chevron = true) { openOverlay(ctx) }
    Row2("Calendário", Icons.Rounded.CalendarMonth, p.red, value = if (hasPerm(ctx, Manifest.permission.READ_CALENDAR)) ok else "Permitir", chevron = true) {
        cal.launch(Manifest.permission.READ_CALENDAR)
    }
    Row2("Bluetooth (baterias)", Icons.Rounded.Widgets, p.accent, value = if (hasPerm(ctx, Manifest.permission.BLUETOOTH_CONNECT)) ok else "Permitir", chevron = true, divider = false) {
        cal.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }
}

@Composable
fun BoxScope.SetupSheet(onDone: () -> Unit) {
    val p = LocalPalette.current
    GlassSheet(true, onDismiss = onDone, maxHeightFraction = 0.92f) {
        SheetTitle("Bem-vindo ao Liquid", "Concluir", onDone)
        LText("Dá estas permissões para o launcher funcionar como no iOS. Podes alterar tudo depois em Definições Liquid.", 15.sp, color = p.secondary, modifier = Modifier.padding(bottom = 14.dp))
        Column(Modifier.clip(RoundedCornerShape(22.dp)).background(p.cell)) { SetupRows() }
        Spacer(Modifier.height(14.dp))
        HealthPermissionBlock()
        Spacer(Modifier.height(10.dp))
        LText("Dica: mantém premido o ecrã principal para editar, e em «Editar» → Personalizar escolhes o estilo dos ícones.", 13.sp, color = p.secondary)
    }
}

// ================================ App de Definições ================================

@Composable
fun SettingsApp(onClose: () -> Unit) {
    val stack = remember { mutableStateListOf("root") }
    androidx.activity.compose.BackHandler(stack.size > 1) { stack.removeAt(stack.size - 1) }
    AnimatedContent(stack.last(), transitionSpec = {
        val forward = targetState != "root"
        if (forward) slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 } else slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
    }, label = "nav") { screen ->
        Screen(if (screen == "root") "Definições" else titleOf(screen), back = if (screen == "root") "Fechar" else "Definições", onBack = { if (stack.size > 1) stack.removeAt(stack.size - 1) else onClose() }) {
            when (screen) {
                "root" -> Root { stack.add(it) }
                "appearance" -> AppearanceScreen()
                "home" -> HomeScreenSettings()
                "wallpaper" -> Box(Modifier.padding(horizontal = 16.dp)) { Column { WallpaperContent() } }
                "lock" -> LockSettingsScreen()
                "widgets" -> WidgetsSettings()
                "gestures" -> GesturesSettings()
                "font" -> FontSettings()
                "hidden" -> HiddenApps()
                "backup" -> BackupSettings()
                "about" -> About()
            }
        }
    }
}

fun titleOf(s: String) = when (s) {
    "appearance" -> "Aspeto e Liquid Glass"; "home" -> "Ecrã principal e ícones"; "wallpaper" -> "Wallpaper"; "lock" -> "Ecrã bloqueado"
    "widgets" -> "Widgets"; "gestures" -> "Gestos"; "font" -> "Tipo de letra"; "hidden" -> "Apps ocultas"; "backup" -> "Cópia de segurança"; else -> "Acerca do Liquid"
}

@Composable
fun Screen(title: String, back: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.groupedBg).verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp).noRippleClick(onBack), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = p.accent, modifier = Modifier.size(20.dp))
            LText(back, 17.sp, color = p.accent)
        }
        LText(title, 34.sp, FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, bottom = 14.dp, top = 2.dp))
        content()
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp))
    }
}

@Composable
private fun Root(go: (String) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val tick = rememberResumeTick()
    Group {
        Row2("Liquid", Icons.Rounded.AccountCircle, Color(0xFF5E5CE6), subtitle = "Launcher estilo iOS 26 para o Pixel", divider = false)
    }
    if (!isDefaultHome(ctx)) {
        Group(footer = "Toca para escolher o Liquid como app de ecrã principal.") {
            Row2("Definir como launcher predefinido", Icons.Rounded.Home, p.accent, chevron = true, divider = false) {
                runCatching { ctx.startActivity(Intent(AS.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
    Group("Configuração") { SetupRows() }
    Group {
        Row2("Aspeto e Liquid Glass", Icons.Rounded.BrightnessAuto, p.accent, chevron = true) { go("appearance") }
        Row2("Ecrã principal e ícones", Icons.Rounded.GridView, p.indigo, chevron = true) { go("home") }
        Row2("Wallpaper", Icons.Rounded.Wallpaper, p.teal, chevron = true) { go("wallpaper") }
        Row2("Ecrã bloqueado", Icons.Rounded.Lock, Color(0xFF1C1C1E), chevron = true) { go("lock") }
        Row2("Widgets", Icons.Rounded.Widgets, p.orange, chevron = true, divider = false) { go("widgets") }
    }
    Group {
        Row2("Gestos", Icons.Rounded.Gesture, p.green, chevron = true) { go("gestures") }
        Row2("Tipo de letra", Icons.Rounded.TextFields, p.gray, chevron = true) { go("font") }
        Row2("Apps ocultas", Icons.Rounded.VisibilityOff, p.purple, chevron = true) { go("hidden") }
        Row2("Cópia de segurança", Icons.Rounded.Backup, p.blue, chevron = true) { go("backup") }
        Row2("Acerca do Liquid", Icons.Rounded.Info, p.gray, chevron = true, divider = false) { go("about") }
    }
}

@Composable
private fun AppearanceScreen() {
    val s by Store.settings.collectAsState()
    val p = LocalPalette.current
    Group("Aspeto") {
        Box(Modifier.padding(16.dp)) { Segmented(listOf("Claro", "Escuro", "Automático"), s.appearance.ordinal) { i -> Store.settings { it.copy(appearance = Appearance.entries[i]) } } }
    }
    Group("Liquid Glass", footer = "Transparente mostra mais o wallpaper; Tingido dá mais contraste e legibilidade (como no iOS 26.1).") {
        Box(Modifier.padding(16.dp)) { Segmented(listOf("Transparente", "Tingido"), s.glass.ordinal) { i -> Store.settings { it.copy(glass = GlassStyle.entries[i]) } } }
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
            LText("Refração das bordas", 15.sp, color = p.secondary)
            LSlider(s.refraction, { v -> Store.settings { it.copy(refraction = v) } }, range = 0f..1.6f)
        }
    }
    Group {
        ToggleRow("Vibração ao tocar", s.haptics, divider = false) { v -> Store.settings { it.copy(haptics = v) } }
    }
}

@Composable
private fun HomeScreenSettings() {
    val s by Store.settings.collectAsState()
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp).clip(RoundedCornerShape(26.dp)).background(LocalPalette.current.cell).padding(16.dp)) {
        CustomizeContent(onWallpaper = {})
    }
    Group("Grelha") {
        Box(Modifier.padding(16.dp)) { Segmented(listOf("4 colunas", "5 colunas"), if (s.columns == 5) 1 else 0) { i -> Store.settings { it.copy(columns = if (i == 1) 5 else 4) } } }
        Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) { Segmented(listOf("6 linhas", "7 linhas"), if (s.rows == 7) 1 else 0) { i -> Store.settings { it.copy(rows = if (i == 1) 7 else 6) } } }
    }
    Group("Páginas") {
        ToggleRow("Vista Hoje (à esquerda)", s.todayView) { v -> Store.settings { it.copy(todayView = v) } }
        ToggleRow("Biblioteca de apps (à direita)", s.appLibrary) { v -> Store.settings { it.copy(appLibrary = v) } }
        ToggleRow("Dock", s.dock) { v -> Store.settings { it.copy(dock = v) } }
        ToggleRow("Botão «Pesquisar» em vez de pontos", s.searchPill, divider = false) { v -> Store.settings { it.copy(searchPill = v) } }
    }
    Group("Apps novas") {
        Box(Modifier.padding(16.dp)) { Segmented(listOf("Ecrã principal", "Só Biblioteca"), s.newApps.ordinal) { i -> Store.settings { it.copy(newApps = NewApps.entries[i]) } } }
    }
}

@Composable
private fun LockSettingsScreen() {
    val s by Store.settings.collectAsState()
    val ctx = LocalContext.current
    val p = LocalPalette.current
    @Suppress("UNUSED_VARIABLE") val tick = rememberResumeTick()
    val ready = NotifService.enabled(ctx) && LockController.canShow(ctx)
    Group(footer = "O ecrã bloqueado do Liquid aparece por cima do bloqueio do Android. O teu PIN e a impressão digital continuam a proteger o telemóvel: ao deslizar para cima é pedido o desbloqueio normal.") {
        ToggleRow("Usar o ecrã bloqueado do Liquid", s.lock.enabled, divider = false) { v ->
            Store.lock { it.copy(enabled = v) }
            if (v && !LockController.canShow(ctx)) openOverlay(ctx) else if (v && !NotifService.enabled(ctx)) openNotifAccess(ctx)
        }
    }
    if (s.lock.enabled && !ready) Group("Falta") {
        if (!LockController.canShow(ctx)) Row2("Permitir «Sobrepor a outras apps»", chevron = true) { openOverlay(ctx) }
        if (!NotifService.enabled(ctx)) Row2("Permitir acesso a notificações", chevron = true, divider = false) { openNotifAccess(ctx) }
    }
    Group {
        Row2("Personalizar ecrã bloqueado", Icons.Rounded.Lock, Color(0xFF1C1C1E), subtitle = "Relógio, widgets, notificações e controlos", chevron = true, divider = false) {
            ctx.startActivity(Intent(ctx, LockActivity::class.java).putExtra("edit", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    Group(footer = "Também podes manter premido o ecrã bloqueado para o personalizar.") {
        Row2("Pré-visualizar agora", chevron = true, divider = false) {
            ctx.startActivity(Intent(ctx, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun WidgetsSettings() {
    val s by Store.settings.collectAsState()
    val p = LocalPalette.current
    Group("Saúde", footer = "Os dados vêm do Health Connect (Google Fit, Mi Fitness, Strava, …).") {
        Column(Modifier.padding(16.dp)) {
            HealthPermissionBlock()
            Spacer(Modifier.height(12.dp))
            LText("Objetivo Mover: ${s.moveGoal} kcal", 15.sp)
            LSlider(s.moveGoal.toFloat(), { v -> Store.settings { it.copy(moveGoal = (v / 10).toInt() * 10) } }, range = 100f..1500f)
            LText("Objetivo Exercício: ${s.exerciseGoal} min", 15.sp)
            LSlider(s.exerciseGoal.toFloat(), { v -> Store.settings { it.copy(exerciseGoal = v.toInt()) } }, range = 10f..120f)
            LText("Objetivo de passos: ${s.stepsGoal}", 15.sp)
            LSlider(s.stepsGoal.toFloat(), { v -> Store.settings { it.copy(stepsGoal = (v / 500).toInt() * 500) } }, range = 2000f..25000f)
        }
    }
    Group("Meteorologia") { Column(Modifier.padding(16.dp)) { WeatherCityConfig() } }
    Group(footer = "Para os transportes: adiciona o widget «Transportes» e mantém-no premido → Editar widget. Os widgets da app Partidas também podem ser adicionados em «Widgets de outras apps».") {
        Row2("Widgets Android", Icons.Rounded.Widgets, p.orange, subtitle = "No modo de edição: + → Widgets de outras apps", divider = false)
    }
}

@Composable
private fun GesturesSettings() {
    val s by Store.settings.collectAsState()
    Group("Deslizar para baixo no ecrã principal") {
        Box(Modifier.padding(16.dp)) { Segmented(listOf("Pesquisa", "Notificações"), s.swipeDown.ordinal) { i -> Store.settings { it.copy(swipeDown = SwipeDown.entries[i]) } } }
    }
    Group(footer = "Como no iPhone: a partir do canto superior esquerdo abre as notificações; do canto superior direito abre as definições rápidas.") {
        ToggleRow("Cantos superiores", s.topEdges, divider = false) { v -> Store.settings { it.copy(topEdges = v) } }
    }
}

@Composable
private fun FontSettings() {
    val s by Store.settings.collectAsState()
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var version by remember { mutableIntStateOf(0) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var ok = false
        uris.forEach { u ->
            val name = runCatching {
                ctx.contentResolver.query(u, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull() ?: "font.ttf"
            if (Fonts.import(ctx, u, name)) ok = true
        }
        if (ok) { Store.settings { it.copy(font = FontChoice.CUSTOM) }; version++ }
        else if (uris.isNotEmpty()) Toast.makeText(ctx, "Ficheiro de tipo de letra inválido (usa .ttf ou .otf)", Toast.LENGTH_LONG).show()
    }
    Group("Tipo de letra do launcher") {
        com.carlos.liquid.ui.CheckRow("Inter (incluída — a mais parecida com a SF Pro)", s.font == FontChoice.INTER) { Store.settings { it.copy(font = FontChoice.INTER) } }
        com.carlos.liquid.ui.CheckRow("Sistema (Google Sans)", s.font == FontChoice.SYSTEM) { Store.settings { it.copy(font = FontChoice.SYSTEM) } }
        com.carlos.liquid.ui.CheckRow("Importada" + if (Fonts.custom != null) " — ${Fonts.customName}" else " (nenhuma)", s.font == FontChoice.CUSTOM, divider = false) {
            if (Fonts.custom != null) Store.settings { it.copy(font = FontChoice.CUSTOM) }
        }
    }
    Group(footer = "Como usar a SF Pro: descarrega «SF Pro» em developer.apple.com/fonts no computador, abre o .dmg com o 7‑Zip, extrai o ficheiro SF-Pro.ttf (ou os .otf Regular/Medium/Semibold/Bold) e copia-o para o telemóvel. Depois toca em Importar e escolhe-o. A licença da Apple só permite o uso pessoal em produtos Apple, por isso o Liquid não a inclui.") {
        Row2("Importar tipo de letra (.ttf / .otf)", chevron = true) { pick.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream", "*/*")) }
        Row2("Remover tipo de letra importado", destructive = true, divider = false) {
            Fonts.clearCustom(ctx); Store.settings { it.copy(font = FontChoice.INTER) }; version++
        }
    }
}

@Composable
private fun HiddenApps() {
    val s by Store.settings.collectAsState()
    val apps by AppRepo.apps.collectAsState()
    val pal = LocalPalette.current
    val spec = rememberIconSpec(s, pal.dark, 96)
    Group(footer = "As apps ocultas não aparecem na Biblioteca de apps nem na pesquisa.") {
        apps.forEachIndexed { i, a ->
            Row2(a.label, divider = i < apps.size - 1, trailing = {
                com.carlos.liquid.ui.IOSSwitch(a.key in s.hidden) { v -> Store.settings { it.copy(hidden = if (v) it.hidden + a.key else it.hidden - a.key) } }
            }, icon = null)
        }
    }
}

@Composable
private fun BackupSettings() {
    val ctx = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.exportAll().toByteArray()) } }
            .onSuccess { Toast.makeText(ctx, "Cópia guardada", Toast.LENGTH_SHORT).show() }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = runCatching { ctx.contentResolver.openInputStream(uri)?.use { Store.importAll(it.readBytes().decodeToString()) } ?: false }.getOrDefault(false)
            Toast.makeText(ctx, if (ok) "Cópia restaurada" else "Ficheiro inválido", Toast.LENGTH_SHORT).show()
        }
    }
    Group(footer = "Guarda a disposição do ecrã principal, widgets e todas as definições num ficheiro (ex.: no Google Drive).") {
        Row2("Exportar cópia de segurança", chevron = true) { export.launch("liquid-backup.json") }
        Row2("Restaurar a partir de ficheiro", chevron = true, divider = false) { import.launch(arrayOf("application/json", "*/*")) }
    }
    Group {
        Row2("Repor disposição inicial", destructive = true, divider = false) {
            Store.layout { com.carlos.liquid.data.HomeLayout() }
            Toast.makeText(ctx, "Volta ao ecrã principal para a nova disposição", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun About() {
    val ctx = LocalContext.current
    val v = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0"
    Group {
        Row2("Versão", value = v)
        Row2("Tipo de letra", value = "Inter (SIL OFL)")
        Row2("Meteorologia", value = "Open‑Meteo")
        Row2("Transportes", value = "Carris Metropolitana · CP · IP", divider = false)
    }
    Group(footer = "Inspirado no design do iOS 26. Ícones, wallpapers e símbolos são originais ou do Material Symbols; não inclui recursos da Apple.") {
        Row2("Feito para o Pixel 9a", icon = Icons.Rounded.Favorite, iconBg = Color(0xFFFF2D55), divider = false)
    }
}

@Suppress("unused") private val keepPlace = Icons.Rounded.Place
