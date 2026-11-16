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

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun hasPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun upcoming(ctx: Context, limit: Int, days: Int = 14): List<Item> {
        if (!hasPermission(ctx)) return emptyList()
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + days * DAY_MS)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )
        val result = ArrayList<Item>()
        try {
            ctx.contentResolver.query(
                uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { c ->
                while (c.moveToNext() && result.size < limit) {
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
            CalendarContract.Instances.ALL_DAY
        )
        val days = HashSet<LocalDate>()
        try {
            ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
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
