package pt.carlos.painel.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean, val color: Int, val calendarId: Long)
data class CalInfo(val id: Long, val name: String, val account: String, val color: Int)

/** Agenda pelo calendário do Android (contas Google sincronizadas): sem API nem custos. */
object CalendarRepo {
    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun events(ctx: Context, from: Long, to: Long, hidden: Set<String>): List<CalEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission(ctx)) return@withContext emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from); ContentUris.appendId(it, to)
        }.build()
        val proj = arrayOf(
            CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.DISPLAY_COLOR, CalendarContract.Instances.CALENDAR_ID,
        )
        val out = ArrayList<CalEvent>()
        runCatching {
            ctx.contentResolver.query(uri, proj, "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val calId = c.getLong(5)
                    if (calId.toString() in hidden) continue
                    out += CalEvent(c.getString(0) ?: "(sem título)", c.getLong(1), c.getLong(2), c.getInt(3) == 1, c.getInt(4), calId)
                }
            }
        }
        out
    }

    suspend fun calendars(ctx: Context): List<CalInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission(ctx)) return@withContext emptyList()
        val out = ArrayList<CalInfo>()
        runCatching {
            ctx.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.CALENDAR_COLOR,
                ),
                null, null, null,
            )?.use { c -> while (c.moveToNext()) out += CalInfo(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getInt(3)) }
        }
        out
    }
}
