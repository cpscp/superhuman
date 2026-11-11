@file:OptIn(ExperimentalMaterial3Api::class)

package pt.carlos.painel.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.carlos.painel.PainelApp
import pt.carlos.painel.theme.PainelTheme

class DebloatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PainelTheme(PainelApp.instance.prefs) { DebloatScreen(onBack = { finish() }) } }
    }
}

private data class SysApp(val pkg: String, val label: String, val enabled: Boolean, val group: Int, val defaultOn: Boolean, val note: String)

/** Grupos: 0 parceiros (remover), 1 Lenovo, 2 Google opcionais, 3 sistema (não mexer). */
private object Bloat {
    /** Apps de parceiros pré-instaladas: seguras de remover. */
    val partners = setOf(
        "com.netflix.mediaclient", "com.netflix.partner.activation",
        "com.facebook.katana", "com.facebook.appmanager", "com.facebook.system", "com.facebook.services",
        "com.amazon.mShop.android.shopping", "com.amazon.appmanager", "com.amazon.avod.thirdpartyclient",
        "com.booking", "com.spotify.music", "com.microsoft.office.officehubrow", "com.microsoft.skydrive",
        "com.linkedin.android", "com.zhiliaoapp.musically", "com.ebay.mobile", "com.agoda.mobile.consumer",
        "com.tripadvisor.tripadvisor", "com.dropbox.android", "com.evernote", "com.einnovation.temu",
        "com.alibaba.aliexpresshd", "com.king.candycrushsaga", "com.king.candycrushsodasaga",
    )

    /** Google: marcadas = não precisas num tablet que tem o Painel como ecrã inicial. */
    val googleOptional = mapOf(
        "com.google.android.apps.kids.home" to (true to "Espaço Crianças"),
        "com.google.android.apps.mediahome.launcher" to (true to "Espaço de Entretenimento (outro ecrã inicial)"),
        "com.google.android.videos" to (true to "Google TV (filmes)"),
        "com.google.android.apps.magazines" to (true to "Google Notícias"),
        "com.google.android.apps.tachyon" to (false to "Google Meet"),
        "com.google.android.apps.podcasts" to (true to "Podcasts"),
        "com.google.android.apps.subscriptions.red" to (true to "Google One"),
        "com.google.android.apps.books" to (true to "Play Livros"),
        "com.google.android.apps.youtube.music" to (false to "YouTube Music (usas o BitChord)"),
        "com.google.android.apps.docs.editors.docs" to (false to "Docs"),
        "com.google.android.apps.docs.editors.sheets" to (false to "Sheets"),
        "com.google.android.apps.docs.editors.slides" to (false to "Slides"),
        "com.google.android.apps.googleassistant" to (false to "Assistente"),
        "com.google.android.feedback" to (false to "Feedback de erros"),
    )

    /** Nunca remover: o tablet ou o Painel dependem disto. */
    private val keepPrefixes = listOf(
        "android", "com.android.", "com.google.android.gms", "com.google.android.gsf", "com.google.android.ext.",
        "com.google.android.webview", "com.google.android.trichromelibrary", "com.google.android.inputmethod",
        "com.google.android.permissioncontroller", "com.google.android.packageinstaller", "com.google.android.modulemetadata",
        "com.google.android.networkstack", "com.google.android.cellbroadcast", "com.google.android.documentsui",
        "com.google.android.apps.chromecast.app", "com.google.android.calendar", "com.google.android.youtube",
        "com.android.vending", "pt.carlos.", "com.mediatek.", "vendor.", "com.qualcomm.",
    )

    fun group(pkg: String): Int = when {
        pkg in partners -> 0
        pkg.startsWith("com.lenovo.") || pkg.startsWith("com.tblenovo.") || pkg.startsWith("com.zui.") || pkg.startsWith("com.motorola.") -> 1
        pkg in googleOptional -> 2
        keepPrefixes.any { pkg == it || pkg.startsWith(it) } -> 3
        else -> 3
    }
}

@Composable
private fun DebloatScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val checked = remember { mutableStateMapOf<String, Boolean>() }

    val apps by produceState(initialValue = emptyList<SysApp>()) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA).map { ai ->
                val g = Bloat.group(ai.packageName)
                val google = Bloat.googleOptional[ai.packageName]
                SysApp(
                    pkg = ai.packageName,
                    label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(ai.packageName),
                    enabled = ai.enabled,
                    group = if ((ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && g == 3) 4 else g,   // 4 = instalada por ti
                    defaultOn = g == 0 || (g == 2 && google?.first == true),
                    note = google?.second ?: "",
                )
            }.sortedWith(compareBy({ it.group }, { it.label.lowercase() }))
        }
    }
    LaunchedEffect(apps) { apps.forEach { if (it.pkg !in checked && it.group <= 2) checked[it.pkg] = it.defaultOn } }

    fun selected() = apps.filter { it.group <= 2 && checked[it.pkg] == true }

    fun script(): String = buildString {
        appendLine("# Limpeza do tablet — gerado pelo Painel")
        appendLine("# Remover (só para o teu utilizador; reversível):")
        selected().forEach { appendLine("adb shell pm uninstall -k --user 0 ${it.pkg}") }
        appendLine()
        appendLine("# Para repor alguma (se precisares):")
        selected().forEach { appendLine("# adb shell cmd package install-existing ${it.pkg}") }
    }

    fun fullList(): String = buildString {
        appendLine("# Apps no tablet (pacote | nome | grupo)")
        val names = listOf("parceiro", "lenovo", "google-opcional", "sistema", "instalada")
        apps.forEach { appendLine("${it.pkg} | ${it.label} | ${names[it.group]}${if (!it.enabled) " | desativada" else ""}") }
    }

    fun share(text: String, title: String) {
        runCatching {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title))
        }
    }

    val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    val groupNames = listOf("Apps de parceiros (remover)", "Lenovo (a rever)", "Google (opcionais)")

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Limpeza do tablet") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = 32.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Marca o que queres remover. O Android só deixa remover apps do sistema a partir de um computador (ADB): " +
                            "o Painel gera os comandos certos para o teu tablet. Também podes desativar uma app tocando em Definições.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { clip.setText(AnnotatedString(script())); Toast.makeText(ctx, "Comandos copiados (${selected().size} apps)", Toast.LENGTH_SHORT).show() }) {
                            Text("Copiar comandos (${selected().size})")
                        }
                        FilledTonalButton(onClick = { share(script(), "Enviar comandos") }) { Text("Partilhar comandos") }
                        TextButton(onClick = { share(fullList(), "Enviar lista de apps") }) { Text("Enviar lista completa") }
                    }
                }
            }
            for (g in 0..2) {
                val list = apps.filter { it.group == g }
                if (list.isEmpty()) continue
                item(key = "h$g") {
                    Text(
                        groupNames[g] + " · ${list.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                items(list, key = { it.pkg }) { a ->
                    ListItem(
                        colors = rowColors,
                        leadingContent = { Checkbox(checked = checked[a.pkg] == true, onCheckedChange = { checked[a.pkg] = it }) },
                        headlineContent = { Text(a.label + if (!a.enabled) " (desativada)" else "") },
                        supportingContent = { Text(listOf(a.note, a.pkg).filter { it.isNotBlank() }.joinToString(" · ")) },
                        trailingContent = {
                            TextButton(onClick = {
                                runCatching {
                                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.pkg}")))
                                }
                            }) { Text("Definições") }
                        },
                    )
                }
            }
            item {
                val sys = apps.count { it.group == 3 }
                Text(
                    "$sys apps essenciais do sistema ficam fora da lista (não se mexe).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
