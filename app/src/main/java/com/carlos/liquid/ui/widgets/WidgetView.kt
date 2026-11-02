package com.carlos.liquid.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.carlos.liquid.data.HomeItem
import com.carlos.liquid.data.Store
import com.carlos.liquid.data.WidgetKind

fun widgetName(k: WidgetKind): String = when (k) {
    WidgetKind.CLOCK_ANALOG -> "Relógio"
    WidgetKind.CLOCK_DIGITAL -> "Relógio digital"
    WidgetKind.WORLD_CLOCK -> "Relógio mundial"
    WidgetKind.CALENDAR -> "Calendário"
    WidgetKind.WEATHER -> "Meteorologia"
    WidgetKind.ACTIVITY -> "Atividade"
    WidgetKind.STEPS -> "Saúde"
    WidgetKind.BATTERY -> "Baterias"
    WidgetKind.TRANSIT -> "Transportes"
    WidgetKind.PHOTOS -> "Fotografias"
    WidgetKind.ANDROID -> "Widget"
}

fun widgetDescription(k: WidgetKind): String = when (k) {
    WidgetKind.CLOCK_ANALOG -> "Vê as horas num relógio analógico, ou várias cidades no tamanho médio."
    WidgetKind.CLOCK_DIGITAL -> "Horas grandes, data e próximo alarme."
    WidgetKind.WORLD_CLOCK -> "Horas noutras cidades do mundo."
    WidgetKind.CALENDAR -> "Os teus próximos eventos e o mês."
    WidgetKind.WEATHER -> "Condições atuais, previsão horária e de 5 dias."
    WidgetKind.ACTIVITY -> "Anéis Mover, Exercício e Passos do Health Connect."
    WidgetKind.STEPS -> "Passos, distância e semana."
    WidgetKind.BATTERY -> "Bateria do Pixel e dos dispositivos Bluetooth."
    WidgetKind.TRANSIT -> "Partidas em tempo real da Carris Metropolitana e da CP."
    WidgetKind.PHOTOS -> "As tuas fotografias, a mudar a cada hora."
    WidgetKind.ANDROID -> "Widget de outra app."
}

@Composable
fun WidgetView(item: HomeItem.Widget, w: Dp, h: Dp) {
    val zones = item.config["zones"]?.split(",")?.filter { it.isNotBlank() } ?: Store.s.worldClocks
    when (item.kind) {
        WidgetKind.CLOCK_ANALOG -> ClockAnalogWidget(item.size, w, h, zones)
        WidgetKind.CLOCK_DIGITAL -> ClockDigitalWidget(item.size, w, h)
        WidgetKind.WORLD_CLOCK -> WorldClockWidget(item.size, w, h, zones)
        WidgetKind.CALENDAR -> CalendarWidget(item.size, w, h)
        WidgetKind.WEATHER -> WeatherWidget(item.size, w, h)
        WidgetKind.ACTIVITY -> ActivityWidget(item.size, w, h)
        WidgetKind.STEPS -> StepsWidget(item.size, w, h)
        WidgetKind.BATTERY -> BatteryWidget(item.size, w, h)
        WidgetKind.TRANSIT -> TransitWidget(item.size, w, h, item.config)
        WidgetKind.PHOTOS -> PhotosWidget(w, h, item.id)
        WidgetKind.ANDROID -> AndroidWidget(item.androidId, w, h)
    }
}
