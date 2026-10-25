package com.carlos.superhuman.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Settings
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.device.CalendarRepo
import com.carlos.superhuman.net.Place
import com.carlos.superhuman.net.Strava
import com.carlos.superhuman.net.Weather
import com.carlos.superhuman.service.WalletListener
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.InsetBox
import com.carlos.superhuman.ui.components.LineDivider
import com.carlos.superhuman.ui.components.PrimaryButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHSwitch
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.components.SecondaryButton
import com.carlos.superhuman.ui.components.SectionHeader
import com.carlos.superhuman.ui.components.Segmented
import com.carlos.superhuman.ui.components.SmallPill
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import com.carlos.superhuman.work.Workers
import kotlinx.coroutines.launch

private fun setS(block: (Settings) -> Settings) = Store.update { it.copy(settings = block(it.settings)) }

@Composable
fun SettingsScreen() {
    val d by Store.state.collectAsState()
    val s = d.settings
    val c = SH.c
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var placeQuery by remember { mutableStateOf("") }
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    val calendars = remember { CalendarRepo.calendars(ctx) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.exportJson().toByteArray()) }
            Toast.makeText(ctx, "Backup exportado", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() }.getOrNull().orEmpty()
            Toast.makeText(ctx, if (Store.importJson(text)) "Backup restaurado" else "Ficheiro inválido", Toast.LENGTH_SHORT).show()
        }
    }

    ScreenScaffold(title = "Definições", onBack = { Nav.back() }) {
        item {
            SectionHeader("Geral")
            SHTextField(s.userName, { v -> setS { it.copy(userName = v) } }, "O teu nome", Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Segmented(listOf("Sistema", "Claro", "Escuro"), s.darkMode, Modifier.fillMaxWidth()) { m -> setS { it.copy(darkMode = m) } }
        }

        item {
            SectionHeader("Localização do tempo")
            Text("Atual: ${s.locationName}", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SHTextField(placeQuery, { placeQuery = it }, "Procurar cidade", Modifier.weight(1f), leading = Icons.Rounded.Search)
                Spacer(Modifier.width(8.dp))
                SmallPill("Procurar") { scope.launch { places = Weather.searchPlaces(placeQuery) } }
            }
            places.forEach { p ->
                Text(
                    "${p.name}, ${p.admin} · ${p.country}", style = Type.body, color = c.ink,
                    modifier = Modifier.fillMaxWidth().clickable {
                        setS { it.copy(locationName = p.name, latitude = p.lat, longitude = p.lon) }
                        places = emptyList(); placeQuery = ""
                        scope.launch { Weather.refresh() }
                    }.padding(vertical = 10.dp),
                )
            }
        }

        item {
            SectionHeader("Calendários")
            Text("Escolhe a etiqueta de cada calendário (ex.: Trabalho, Pessoal), quais mostrar e onde criar slots.", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(10.dp))
            if (calendars.isEmpty()) Text("Sem calendários (dá permissão de calendário).", style = Type.small, color = c.bad)
            InsetBox(padding = 0.dp) {
                calendars.forEachIndexed { i, cal ->
                    if (i > 0) LineDivider()
                    val key = cal.id.toString()
                    var label by remember(cal.id) { mutableStateOf(s.calendarLabels[key] ?: "") }
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(cal.color or 0xFF000000.toInt())))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cal.name, style = Type.body, color = c.ink)
                                Text(cal.account, style = Type.tiny, color = c.ink3)
                            }
                            SHSwitch(cal.id !in s.hiddenCalendarIds) { on ->
                                setS { it.copy(hiddenCalendarIds = if (on) it.hiddenCalendarIds - cal.id else it.hiddenCalendarIds + cal.id) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SHTextField(label, { v ->
                                label = v
                                setS { st -> st.copy(calendarLabels = if (v.isBlank()) st.calendarLabels - key else st.calendarLabels + (key to v)) }
                            }, "Etiqueta (ex.: Trabalho)", Modifier.weight(1f))
                            if (cal.writable) {
                                Spacer(Modifier.width(8.dp))
                                SmallPill(if (s.defaultCalendarId == cal.id) "Padrão ✓" else "Padrão", s.defaultCalendarId == cal.id) {
                                    setS { it.copy(defaultCalendarId = cal.id) }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Assistente AI · Claude API")
            Text("A chave fica guardada só neste telemóvel. Cria-a em console.anthropic.com.", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(8.dp))
            KeyField(s.claudeApiKey) { v -> setS { it.copy(claudeApiKey = v.trim()) } }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("claude-sonnet-5-5" to "Sonnet", "claude-opus-5-5" to "Opus", "claude-haiku-5-5" to "Haiku").forEach { (id, label) ->
                    SmallPill(label, s.claudeModel == id) { setS { it.copy(claudeModel = id) } }
                }
            }
        }

        item {
            SectionHeader("Invest")
            Text("URL do feed diário (publicado automaticamente). Se falhar, a app gera a análise com a tua chave.", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(8.dp))
            SHTextField(s.investFeedUrl, { v -> setS { it.copy(investFeedUrl = v.trim()) } }, "https://raw.githubusercontent.com/…/latest.json", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            ToggleRow("Gerar com a Claude se o feed não estiver atualizado", s.investAutoGenerate) { v -> setS { it.copy(investAutoGenerate = v) } }
            Spacer(Modifier.height(8.dp))
            SHTextField(s.investProfile, { v -> setS { it.copy(investProfile = v) } }, "Perfil de investidor", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
        }

        item {
            SectionHeader("Google Wallet")
            ToggleRow("Acesso a notificações ativo", WalletListener.isEnabled(ctx)) {
                ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ToggleRow("Confirmar automaticamente (sem rever)", s.walletAutoConfirm) { v -> setS { it.copy(walletAutoConfirm = v) } }
            Spacer(Modifier.height(8.dp))
            SHTextField(
                s.walletPackages.joinToString(", "),
                { v -> setS { it.copy(walletPackages = v.split(",").map { p -> p.trim() }.filter { p -> p.isNotBlank() }) } },
                "Apps a ler (pacotes)", Modifier.fillMaxWidth(),
            )
            Text("Podes juntar a app do teu banco (ex.: o pacote da app do banco) para captar também pagamentos com cartão físico.", style = Type.tiny, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
        }

        item {
            SectionHeader("Saúde")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Limite semanal de cervejas", style = Type.body, color = c.ink, modifier = Modifier.weight(1f))
                SmallPill("−") { setS { it.copy(beerWeeklyLimit = (it.beerWeeklyLimit - 1).coerceAtLeast(0)) } }
                Text("  ${s.beerWeeklyLimit}  ", style = Type.title, color = c.ink)
                SmallPill("+") { setS { it.copy(beerWeeklyLimit = it.beerWeeklyLimit + 1) } }
            }
            Spacer(Modifier.height(12.dp))
            Text("Strava (app em strava.com/settings/api, callback domain = localhost)", style = Type.small, color = c.ink2)
            Spacer(Modifier.height(8.dp))
            SHTextField(s.stravaClientId, { v -> setS { it.copy(stravaClientId = v.trim()) } }, "Client ID", Modifier.fillMaxWidth(), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(8.dp))
            KeyField(s.stravaClientSecret, "Client Secret") { v -> setS { it.copy(stravaClientSecret = v.trim()) } }
            if (d.strava.refreshToken.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Desligar Strava (${d.strava.athlete})", Modifier.fillMaxWidth()) { Strava.disconnect() }
            }
        }

        item {
            SectionHeader("Revisões")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily review", style = Type.body, color = c.ink, modifier = Modifier.weight(1f))
                listOf(21 to 0, 21 to 30, 22 to 0, 22 to 30).forEach { (h, m) ->
                    Spacer(Modifier.width(4.dp))
                    SmallPill(String.format("%02d:%02d", h, m), s.dailyReviewHour == h && s.dailyReviewMinute == m) {
                        setS { it.copy(dailyReviewHour = h, dailyReviewMinute = m) }; Workers.scheduleAll(ctx)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weekly (sexta)", style = Type.body, color = c.ink, modifier = Modifier.weight(1f))
                listOf(16, 17, 18, 19).forEach { h ->
                    Spacer(Modifier.width(4.dp))
                    SmallPill("$h:00", s.weeklyReviewHour == h) { setS { it.copy(weeklyReviewHour = h) }; Workers.scheduleAll(ctx) }
                }
            }
        }

        item {
            SectionHeader("Backup")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Exportar", Modifier.weight(1f)) { exportLauncher.launch("superhuman-backup.json") }
                SecondaryButton("Restaurar", Modifier.weight(1f)) { importLauncher.launch(arrayOf("application/json", "*/*")) }
            }
            Spacer(Modifier.height(10.dp))
            SHCard(Modifier.fillMaxWidth()) {
                Text("Superhuman 1.0", style = Type.title, color = c.ink)
                Text("Dados guardados localmente no telemóvel.", style = Type.small, color = c.ink2)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.body, color = SH.c.ink, modifier = Modifier.weight(1f))
        SHSwitch(value, onChange)
    }
}

/** Campo para chaves/segredos (mostra só o fim da chave). */
@Composable
private fun KeyField(value: String, placeholder: String = "sk-ant-…", onChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(value.isBlank()) }
    var draft by remember { mutableStateOf("") }
    val c = SH.c
    if (!editing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("••••••••" + value.takeLast(4), style = Type.body, color = c.ink, modifier = Modifier.weight(1f))
            SmallPill("Alterar") { editing = true; draft = "" }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SHTextField(draft, { draft = it }, placeholder, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            SmallPill("Guardar") { onChange(draft); editing = draft.isBlank() }
        }
    }
}
