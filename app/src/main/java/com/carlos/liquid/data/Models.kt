package com.carlos.liquid.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().take(10)

@Serializable enum class Appearance { LIGHT, DARK, AUTO }

/** Estilos de ícones iguais aos do iOS 18/26. */
@Serializable enum class IconStyle { DEFAULT, DARK, CLEAR, TINTED }

/** Modo claro/escuro/automático aplicado ao estilo de ícones (botão sol/lua do iOS). */
@Serializable enum class IconMode { AUTO, LIGHT, DARK }

/** iOS 26.1: Liquid Glass "Transparente" ou "Tingido". */
@Serializable enum class GlassStyle { CLEAR, TINTED }

@Serializable enum class WidgetSize { SMALL, MEDIUM, LARGE }

@Serializable enum class WidgetKind {
    CLOCK_ANALOG, CLOCK_DIGITAL, WORLD_CLOCK, CALENDAR, WEATHER, ACTIVITY, STEPS, BATTERY, TRANSIT, PHOTOS, ANDROID
}

@Serializable enum class FontChoice { INTER, SYSTEM, CUSTOM }

@Serializable enum class ClockFont { INTER, INTER_LIGHT, ROUNDED, SERIF, CONDENSED, MONO, CUSTOM }

@Serializable enum class InlineKind { DATE, WEATHER, NEXT_EVENT, ALARM, STEPS, BATTERY }

@Serializable enum class LockWidget { BATTERY, WEATHER, ACTIVITY, STEPS, NEXT_EVENT, ALARM, TRANSIT, WORLD_CLOCK, CALENDAR_DAY }

@Serializable enum class NotifStyle { COUNT, STACK, LIST }

@Serializable enum class LockControl { NONE, FLASHLIGHT, CAMERA, CLOCK, CALCULATOR, DO_NOT_DISTURB }

@Serializable enum class SwipeDown { SPOTLIGHT, NOTIFICATIONS }

@Serializable enum class NewApps { HOME, LIBRARY }

@Serializable
data class LockSettings(
    val enabled: Boolean = false,
    val sameWallpaper: Boolean = true,
    val wallpaperId: String = "aurora",
    val clockFont: ClockFont = ClockFont.INTER,
    val clockWeight: Float = 0.75f,       // 0 = fino … 1 = extra negrito
    val clockColor: Long = 0xFFFFFFFF,
    val clockGlass: Boolean = true,       // números em Liquid Glass (iOS 26)
    val clockSize: Float = 1f,            // 0.7 … 1.5
    val inline: InlineKind = InlineKind.DATE,
    val widgets: List<LockWidget> = listOf(LockWidget.BATTERY, LockWidget.WEATHER, LockWidget.ACTIVITY, LockWidget.NEXT_EVENT),
    val notifStyle: NotifStyle = NotifStyle.STACK,
    val showPreviews: Boolean = true,
    val leftControl: LockControl = LockControl.FLASHLIGHT,
    val rightControl: LockControl = LockControl.CAMERA,
    val dim: Float = 0.12f,
    val transitStop: String = "",          // "carris:060361:Nome" ou "cp:9431039:Nome"
)

@Serializable
data class Settings(
    val appearance: Appearance = Appearance.AUTO,
    val glass: GlassStyle = GlassStyle.CLEAR,
    val refraction: Float = 1f,

    val iconStyle: IconStyle = IconStyle.DEFAULT,
    val iconMode: IconMode = IconMode.AUTO,
    val tintHue: Float = 210f,
    val tintSat: Float = 0.65f,
    val largeIcons: Boolean = false,
    val showLabels: Boolean = true,
    val columns: Int = 4,
    val rows: Int = 6,
    val searchPill: Boolean = true,
    val badges: Boolean = true,
    val todayView: Boolean = true,
    val appLibrary: Boolean = true,
    val dock: Boolean = true,
    val newApps: NewApps = NewApps.HOME,

    val wallpaperId: String = "aurora",
    val dimInDark: Boolean = true,
    val syncSystemWallpaper: Boolean = true,

    val swipeDown: SwipeDown = SwipeDown.SPOTLIGHT,
    val topEdges: Boolean = true,          // topo esquerdo = notificações, topo direito = definições rápidas
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,

    val font: FontChoice = FontChoice.INTER,
    val hidden: Set<String> = emptySet(),

    val cityName: String = "Lisboa",
    val lat: Double = 38.7223,
    val lon: Double = -9.1393,
    val useLocation: Boolean = false,
    val moveGoal: Int = 500,
    val exerciseGoal: Int = 30,
    val stepsGoal: Int = 10000,
    val worldClocks: List<String> = listOf("Europe/Lisbon", "Europe/London", "America/New_York", "Asia/Tokyo"),

    val lock: LockSettings = LockSettings(),
    val onboarded: Boolean = false,
)

@Serializable
sealed class HomeItem {
    abstract val id: String

    @Serializable @SerialName("app")
    data class App(override val id: String = newId(), val key: String) : HomeItem()

    @Serializable @SerialName("folder")
    data class Folder(override val id: String = newId(), val name: String, val apps: List<String>) : HomeItem()

    @Serializable @SerialName("widget")
    data class Widget(
        override val id: String = newId(),
        val kind: WidgetKind,
        val size: WidgetSize = WidgetSize.SMALL,
        val config: Map<String, String> = emptyMap(),
        val androidId: Int = -1,
    ) : HomeItem()
}

@Serializable
data class Page(val id: String = newId(), val items: List<HomeItem> = emptyList(), val hidden: Boolean = false)

@Serializable
data class HomeLayout(
    val pages: List<Page> = listOf(Page()),
    val dock: List<HomeItem> = emptyList(),
    val today: List<HomeItem.Widget> = emptyList(),
    val initialized: Boolean = false,
)

fun HomeItem.span(cols: Int): Pair<Int, Int> = when (this) {
    is HomeItem.Widget -> when (size) {
        WidgetSize.SMALL -> 2 to 2
        WidgetSize.MEDIUM -> minOf(4, cols) to 2
        WidgetSize.LARGE -> minOf(4, cols) to 4
    }
    else -> 1 to 1
}
