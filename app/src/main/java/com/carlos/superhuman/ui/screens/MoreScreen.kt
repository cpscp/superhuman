package com.carlos.superhuman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.todayEpoch
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.ScreenScaffold
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type

private data class Module(val route: String, val arg: String?, val title: String, val icon: ImageVector, val sub: String)

@Composable
fun MoreScreen() {
    val d by Store.state.collectAsState()
    val modules = listOf(
        Module("assistant", null, "Assistente AI", Icons.Rounded.AutoAwesome, "Pede o que precisares"),
        Module("schedule", null, "Agenda", Icons.Rounded.CalendarMonth, "${d.cachedEvents.size} eventos hoje"),
        Module("super", null, "Supermercado", Icons.Rounded.ShoppingCart, "${d.shopping.count { !it.checked }} na lista"),
        Module("transport", null, "Transportes", Icons.Rounded.Train, "CP e Carris"),
        Module("health", null, "Saúde", Icons.Rounded.Favorite, "${d.beersThisWeek()} cervejas esta semana"),
        Module("notes", null, "Notas", Icons.Rounded.StickyNote2, "${d.notes.size} notas"),
        Module("casa", null, "Casa", Icons.Rounded.House, "${d.homeAdmin.count { !it.done }} pendentes"),
        Module("goals", null, "Objetivos", Icons.Rounded.Flag, "${d.goals.size} life goals"),
        Module("review", "daily", "Daily review", Icons.Rounded.Insights, if (d.reviews.any { it.key == "D-${todayEpoch()}" }) "Feita hoje ✓" else "Por fazer hoje"),
        Module("review", "weekly", "Weekly review", Icons.Rounded.Insights, "Às sextas"),
        Module("settings", null, "Definições", Icons.Rounded.Settings, "Localização, API, integrações"),
    )
    ScreenScaffold(title = "Tudo", onBack = null) {
        modules.chunked(2).forEach { row ->
            item(key = row.first().title) {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { m -> ModuleTile(m, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModuleTile(m: Module, modifier: Modifier) {
    val c = SH.c
    SHCard(modifier.height(132.dp), onClick = { Nav.go(m.route, m.arg) }) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.inset), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Icon(m.icon, null, tint = c.ink, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(m.title, style = Type.title, color = c.ink)
        Text(m.sub, style = Type.tiny, color = c.ink3, maxLines = 1)
    }
}
