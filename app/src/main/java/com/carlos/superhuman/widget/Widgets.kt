package com.carlos.superhuman.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.carlos.superhuman.MainActivity
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.beersThisWeek
import com.carlos.superhuman.data.euro
import com.carlos.superhuman.data.fmtDay
import com.carlos.superhuman.data.fmtTime
import com.carlos.superhuman.data.monthSummary
import com.carlos.superhuman.data.tasksForToday
import com.carlos.superhuman.data.todayEpoch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private val WBg = Color(0xFFF7F6F4)
private val WInset = Color(0xFFE4E1DC)
private val WInk = Color(0xFF1B1A19)
private val WInk2 = Color(0xFF6E6A65)
private val WInk3 = Color(0xFFA8A39D)
private val WAccent = Color(0xFFEF5B2A)
private val WGood = Color(0xFF2FA866)
private val WBad = Color(0xFFE0483C)

private fun style(size: Int, color: Color = WInk, bold: Boolean = false) =
    TextStyle(color = ColorProvider(color), fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

object WidgetSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun refresh(ctx: Context) {
        scope.launch {
            runCatching { TodayWidget().updateAll(ctx) }
            runCatching { PulseWidget().updateAll(ctx) }
        }
    }
}

val TaskIdKey = ActionParameters.Key<String>("taskId")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Store.init(context)
        val id = parameters[TaskIdKey] ?: return
        Store.update { d ->
            d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(done = !it.done, completedAt = if (!it.done) System.currentTimeMillis() else null) else it })
        }
        Store.saveNow()
        TodayWidget().updateAll(context)
    }
}

class AddBeerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Store.init(context)
        val k = todayEpoch().toString()
        Store.update { it.copy(beers = it.beers + (k to ((it.beers[k] ?: 0) + 1))) }
        Store.saveNow()
        PulseWidget().updateAll(context)
    }
}

// ---------------- Widget "Hoje" ----------------
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent { TodayContent() }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

@Composable
private fun TodayContent() {
    val d by Store.state.collectAsState()
    val now = System.currentTimeMillis()
    val events = d.cachedEvents.filter { it.allDay || it.end > now }.take(3)
    val tasks = d.tasksForToday().take(4)
    Column(
        GlanceModifier.fillMaxSize().background(WBg).cornerRadius(26.dp).padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(fmtDay(LocalDate.now()), style = style(12, WInk2))
                Text("O teu dia", style = style(18, bold = true))
            }
            Box(GlanceModifier.background(WInk).cornerRadius(20.dp).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("${d.weather.temp.roundToInt()}° · sente ${d.weather.feels.roundToInt()}°", style = style(12, Color.White))
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        if (events.isEmpty()) Text("Sem mais eventos hoje", style = style(12, WInk3))
        events.forEach { e ->
            Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.width(3.dp).height(26.dp).background(Color(e.color or 0xFF000000.toInt())).cornerRadius(2.dp)) {}
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(e.title, style = style(13, bold = true), maxLines = 1)
                    Text((if (e.allDay) "Todo o dia" else "${fmtTime(e.begin)}–${fmtTime(e.end)}") + " · " + e.calendarLabel, style = style(11, WInk2), maxLines = 1)
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Text("Tasks", style = style(12, WInk2, bold = true))
        Spacer(GlanceModifier.height(4.dp))
        if (tasks.isEmpty()) Text("Tudo feito ✨", style = style(12, WInk3))
        tasks.forEach { t ->
            Row(
                GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)
                    .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to t.id))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(GlanceModifier.size(16.dp).background(WInset).cornerRadius(8.dp)) {}
                Spacer(GlanceModifier.width(8.dp))
                Text(t.title, style = style(13), maxLines = 1)
            }
        }
    }
}

// ---------------- Widget "Plafond + cervejas" ----------------
class PulseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        provideContent { PulseContent() }
    }
}

class PulseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PulseWidget()
}

@Composable
private fun PulseContent() {
    val d by Store.state.collectAsState()
    val m = d.monthSummary()
    val used = if (m.budget > 0) ((m.spent + m.fixed) / m.budget).toFloat().coerceIn(0f, 1f) else 0f
    val fb = d.beerFeedback()
    Column(
        GlanceModifier.fillMaxSize().background(WBg).cornerRadius(26.dp).padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text("Plafond restante", style = style(11, WInk2))
        Text(if (m.budget > 0) euro(m.remaining) else "Definir plafond", style = style(20, if (m.remaining < 0) WBad else WInk, bold = true))
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(
            progress = used,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
            color = ColorProvider(if (used > 0.9f) WBad else WInk),
            backgroundColor = ColorProvider(WInset),
        )
        Spacer(GlanceModifier.height(10.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text("🍺 ${d.beersThisWeek()} / ${d.settings.beerWeeklyLimit} semana", style = style(13, bold = true))
                Text(fb.title, style = style(11, if (fb.level == 0) WGood else if (fb.level == 1) WAccent else WBad), maxLines = 1)
            }
            Box(
                GlanceModifier.background(WInk).cornerRadius(18.dp).padding(horizontal = 12.dp, vertical = 7.dp)
                    .clickable(actionRunCallback<AddBeerAction>()),
            ) { Text("+1", style = style(13, Color.White, bold = true)) }
        }
    }
}
