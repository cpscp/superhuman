package com.carlos.superhuman.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.device.CalendarRepo
import com.carlos.superhuman.net.Weather
import com.carlos.superhuman.ui.screens.AssistantScreen
import com.carlos.superhuman.ui.screens.CasaScreen
import com.carlos.superhuman.ui.screens.FinanceScreen
import com.carlos.superhuman.ui.screens.GoalDetailScreen
import com.carlos.superhuman.ui.screens.GoalsScreen
import com.carlos.superhuman.ui.screens.HealthScreen
import com.carlos.superhuman.ui.screens.HomeScreen
import com.carlos.superhuman.ui.screens.InvestScreen
import com.carlos.superhuman.ui.screens.MoreScreen
import com.carlos.superhuman.ui.screens.NoteEditScreen
import com.carlos.superhuman.ui.screens.NotesScreen
import com.carlos.superhuman.ui.screens.QuickAddSheet
import com.carlos.superhuman.ui.screens.ReviewScreen
import com.carlos.superhuman.ui.screens.ScheduleScreen
import com.carlos.superhuman.ui.screens.SearchScreen
import com.carlos.superhuman.ui.screens.SettingsScreen
import com.carlos.superhuman.ui.screens.SuperScreen
import com.carlos.superhuman.ui.screens.TaskDetailScreen
import com.carlos.superhuman.ui.screens.TasksScreen
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.SuperhumanTheme

@Composable
fun AppRoot() {
    val data by Store.state.collectAsState()
    SuperhumanTheme(mode = data.settings.darkMode) {
        val ctx = LocalContext.current
        var quickAdd by remember { mutableStateOf(false) }

        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            CalendarRepo.cacheToday(ctx)
        }
        LaunchedEffect(Unit) {
            val perms = mutableListOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
            if (!CalendarRepo.hasPermission(ctx)) permLauncher.launch(perms.toTypedArray())
            CalendarRepo.cacheToday(ctx)
            if (System.currentTimeMillis() - Store.data.weather.updatedAt > 20 * 60_000) Weather.refresh()
        }

        BackHandler(enabled = Nav.stack.size > 1 || Nav.current.name != "home") { Nav.back() }

        Box(Modifier.fillMaxSize().background(SH.c.bg)) {
            val route = Nav.current
            AnimatedContent(
                targetState = route,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "screens",
            ) { r ->
                when (r.name) {
                    "home" -> HomeScreen()
                    "tasks" -> TasksScreen()
                    "task" -> TaskDetailScreen(r.arg)
                    "finance" -> FinanceScreen()
                    "invest" -> InvestScreen()
                    "more" -> MoreScreen()
                    "schedule" -> ScheduleScreen()
                    "super" -> SuperScreen()
                    "health" -> HealthScreen()
                    "notes" -> NotesScreen()
                    "note" -> NoteEditScreen(r.arg)
                    "casa" -> CasaScreen()
                    "goals" -> GoalsScreen()
                    "goal" -> GoalDetailScreen(r.arg)
                    "assistant" -> AssistantScreen(r.arg)
                    "review" -> ReviewScreen(weekly = r.arg == "weekly")
                    "settings" -> SettingsScreen()
                    "search" -> SearchScreen()
                    else -> HomeScreen()
                }
            }

            AnimatedVisibility(
                visible = Nav.isTab,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomBar(current = route.name, onAdd = { quickAdd = true })
            }
        }

        if (quickAdd) QuickAddSheet(onDismiss = { quickAdd = false })
    }
}

@Composable
private fun BottomBar(current: String, onAdd: () -> Unit) {
    val c = SH.c
    val items: List<Pair<String, ImageVector>> = listOf(
        "home" to Icons.Rounded.Home,
        "tasks" to Icons.Rounded.CheckCircle,
        "finance" to Icons.Rounded.AccountBalanceWallet,
        "invest" to Icons.Rounded.ShowChart,
        "more" to Icons.Rounded.GridView,
    )
    Row(
        Modifier.navigationBarsPadding().padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .shadow(18.dp, RoundedCornerShape(50), ambientColor = c.ink.copy(alpha = 0.2f), spotColor = c.ink.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(50))
                .background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { (name, icon) ->
                val sel = current == name
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(if (sel) c.inset else c.card).clickable { Nav.go(name) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = if (sel) c.ink else c.ink3, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .shadow(18.dp, CircleShape)
                .size(60.dp)
                .clip(CircleShape)
                .background(c.ink)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = c.onInk, modifier = Modifier.size(28.dp)) }
    }
}
