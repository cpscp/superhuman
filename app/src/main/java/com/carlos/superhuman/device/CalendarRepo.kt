package com.carlos.superhuman.device

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.carlos.superhuman.data.CachedEvent
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.endMillis
import com.carlos.superhuman.data.startMillis
import java.time.LocalDate
import java.util.TimeZone

data class CalInfo(val id: Long, val name: String, val account: String, val color: Int, val writable: Boolean)

data class CalEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val calendarId: Long,
    val calendarName: String,
    val color: Int,
    val location: String,
) {
    /** Etiqueta curta definida pelo utilizador (ex.: "Trabalho"), ou o nome do calendário. */
    val label: String get() = Store.data.settings.calendarLabels[calendarId.toString()] ?: calendarName
}

object CalendarRepo {
    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun calendars(ctx: Context): List<CalInfo> {
        if (!hasPermission(ctx)) return emptyList()
        val out = mutableListOf<CalInfo>()
        val proj = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        runCatching {
            ctx.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, proj, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    out += CalInfo(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "Calendário",
                        account = c.getString(2) ?: "",
                        color = c.getInt(3),
                        writable = c.getInt(4) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                    )
                }
            }
        }
        return out
    }

    fun events(ctx: Context, day: LocalDate): List<CalEvent> = eventsBetween(ctx, day.startMillis(), day.endMillis())

    fun eventsBetween(ctx: Context, start: Long, end: Long): List<CalEvent> {
        if (!hasPermission(ctx)) return emptyList()
        val hidden = Store.data.settings.hiddenCalendarIds.toSet()
        val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(b, start)
        ContentUris.appendId(b, end)
        val proj = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.EVENT_LOCATION,
        )
        val out = mutableListOf<CalEvent>()
        runCatching {
            ctx.contentResolver.query(b.build(), proj, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val calId = c.getLong(5)
                    if (calId in hidden) continue
                    out += CalEvent(
                        id = c.getLong(0),
                        title = c.getString(1) ?: "(sem título)",
                        begin = c.getLong(2),
                        end = c.getLong(3),
                        allDay = c.getInt(4) == 1,
                        calendarId = calId,
                        calendarName = c.getString(6) ?: "",
                        color = c.getInt(7),
                        location = c.getString(8) ?: "",
                    )
                }
            }
        }
        return out
    }

    fun insert(ctx: Context, calendarId: Long, title: String, begin: Long, end: Long, description: String = ""): Boolean = runCatching {
        val v = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, begin)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI, v) != null
    }.getOrDefault(false)

    /** Guarda os eventos de hoje para os widgets e para o assistente. */
    fun cacheToday(ctx: Context) {
        val list = events(ctx, LocalDate.now()).map {
            CachedEvent(it.title, it.begin, it.end, it.allDay, it.label, it.color)
        }
        Store.update { it.copy(cachedEvents = list) }
    }

    fun defaultCalendar(ctx: Context): CalInfo? {
        val all = calendars(ctx).filter { it.writable }
        val pref = Store.data.settings.defaultCalendarId
        return all.firstOrNull { it.id == pref } ?: all.firstOrNull { it.account.contains("@gmail") } ?: all.firstOrNull()
    }
}
