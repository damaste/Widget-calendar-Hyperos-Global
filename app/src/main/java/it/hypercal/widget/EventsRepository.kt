package it.hypercal.widget

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/** Legge gli eventi dal provider di sistema (usato anche dal Calendario HyperOS). */
object EventsRepository {

    data class Item(val title: String, val begin: Long, val allDay: Boolean)

    data class CalendarInfo(val id: Long, val name: String, val account: String, val color: Int)

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun hasPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Elenco di tutti i calendari presenti sul dispositivo, ordinati per account. */
    fun listCalendars(ctx: Context): List<CalendarInfo> {
        if (!hasPermission(ctx)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val result = ArrayList<CalendarInfo>()
        try {
            ctx.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, null, null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, " +
                    "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val type = c.getString(3) ?: ""
                    val account = if (type == CalendarContract.ACCOUNT_TYPE_LOCAL) "Sul dispositivo"
                    else (c.getString(2) ?: "Account")
                    result.add(
                        CalendarInfo(
                            id = c.getLong(0),
                            name = c.getString(1)?.takeIf { it.isNotBlank() } ?: account,
                            account = account,
                            color = c.getInt(4) or (0xFF shl 24)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return result
    }

    /** Filtro SQL sui calendari scelti; null = nessun filtro (tutti). */
    private fun calendarFilter(ctx: Context): String? {
        val ids = Settings.load(ctx).calendars ?: return null
        if (ids.isEmpty()) return "${CalendarContract.Instances.CALENDAR_ID} IN (-1)"
        return "${CalendarContract.Instances.CALENDAR_ID} IN (${ids.joinToString(",")})"
    }

    fun upcoming(ctx: Context, limit: Int, days: Int = 14): List<Item> {
        if (!hasPermission(ctx)) return emptyList()
        val allowed = Settings.load(ctx).calendars
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + days * DAY_MS)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID
        )
        val result = ArrayList<Item>()
        try {
            ctx.contentResolver.query(
                uri, projection, calendarFilter(ctx), null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { c ->
                while (c.moveToNext() && result.size < limit) {
                    if (allowed != null && c.getLong(4) !in allowed) continue
                    val end = c.getLong(2)
                    if (end < now) continue
                    result.add(
                        Item(
                            title = c.getString(0)?.takeIf { it.isNotBlank() }
                                ?: ctx.getString(R.string.untitled),
                            begin = c.getLong(1),
                            allDay = c.getInt(3) == 1
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return result
    }

    /** Giorni del mese che hanno almeno un evento (anche eventi su più giorni). */
    fun daysWithEvents(ctx: Context, ym: YearMonth): Set<LocalDate> {
        if (!hasPermission(ctx)) return emptySet()
        val allowed = Settings.load(ctx).calendars
        val zone = ZoneId.systemDefault()
        val monthFirst = ym.atDay(1)
        val monthLast = ym.atEndOfMonth()
        val startMs = monthFirst.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = monthLast.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, startMs)
            ContentUris.appendId(it, endMs)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID
        )
        val days = HashSet<LocalDate>()
        try {
            ctx.contentResolver.query(uri, projection, calendarFilter(ctx), null, null)?.use { c ->
                while (c.moveToNext()) {
                    if (allowed != null && c.getLong(3) !in allowed) continue
                    val begin = c.getLong(0)
                    val end = c.getLong(1)
                    val allDay = c.getInt(2) == 1
                    val s: LocalDate
                    var e: LocalDate
                    if (allDay) {
                        s = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                        e = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
                    } else {
                        s = Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
                        e = if (end > begin) Instant.ofEpochMilli(end - 1).atZone(zone).toLocalDate() else s
                    }
                    if (e.isBefore(s)) e = s
                    var d = if (s.isBefore(monthFirst)) monthFirst else s
                    val last = if (e.isAfter(monthLast)) monthLast else e
                    while (!d.isAfter(last)) {
                        days.add(d)
                        d = d.plusDays(1)
                    }
                }
            }
        } catch (_: Exception) {
            return emptySet()
        }
        return days
    }
}
