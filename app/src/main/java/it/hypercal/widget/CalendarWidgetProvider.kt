package it.hypercal.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, manager, it) }
        scheduleNextRefresh(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val edit = prefs(context).edit()
        appWidgetIds.forEach { edit.remove(key(it)) }
        edit.apply()
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PREV, ACTION_NEXT, ACTION_TODAY -> {
                val id = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val current = prefs(context).getInt(key(id), 0)
                    val next = when (intent.action) {
                        ACTION_PREV -> current - 1
                        ACTION_NEXT -> current + 1
                        else -> 0
                    }
                    prefs(context).edit().putInt(key(id), next).apply()
                    render(context, AppWidgetManager.getInstance(context), id)
                }
            }
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                refreshAll(context)
                scheduleNextRefresh(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_REFRESH = "it.hypercal.widget.REFRESH"
        const val ACTION_PREV = "it.hypercal.widget.PREV"
        const val ACTION_NEXT = "it.hypercal.widget.NEXT"
        const val ACTION_TODAY = "it.hypercal.widget.TODAY"

        private val IT: Locale = Locale.ITALIAN
        private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences("widget_state", Context.MODE_PRIVATE)

        private fun key(id: Int) = "offset_$id"

        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, CalendarWidgetProvider::class.java))
            ids.forEach { render(ctx, mgr, it) }
        }

        private fun refreshIntent(ctx: Context): PendingIntent {
            val i = Intent(ctx, CalendarWidgetProvider::class.java).setAction(ACTION_REFRESH)
            return PendingIntent.getBroadcast(
                ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Sveglia (inesatta, non richiede permessi) subito dopo la mezzanotte. */
        private fun scheduleNextRefresh(ctx: Context) {
            val next = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).plusMinutes(1)
                .toInstant().toEpochMilli()
            ctx.getSystemService(AlarmManager::class.java)
                .set(AlarmManager.RTC, next, refreshIntent(ctx))
        }

        private fun cap(s: String) = s.replaceFirstChar { it.titlecase(IT) }

        private fun openCalendarAt(ctx: Context, millis: Long, req: Int): PendingIntent {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$millis"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun broadcastFor(ctx: Context, action: String, id: Int, req: Int): PendingIntent {
            val i = Intent(ctx, CalendarWidgetProvider::class.java)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            return PendingIntent.getBroadcast(
                ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun formatEvent(ctx: Context, item: EventsRepository.Item, today: LocalDate): String {
            val zone = ZoneId.systemDefault()
            var date: LocalDate
            var time = ""
            if (item.allDay) {
                date = Instant.ofEpochMilli(item.begin).atZone(ZoneOffset.UTC).toLocalDate()
            } else {
                val dt = Instant.ofEpochMilli(item.begin).atZone(zone)
                date = dt.toLocalDate()
                time = dt.format(TIME_FMT)
            }
            if (date.isBefore(today)) date = today
            val prefix = when (date) {
                today -> ""
                today.plusDays(1) -> ctx.getString(R.string.tomorrow)
                else -> "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, IT)} ${date.dayOfMonth}"
            }
            val head = listOf(prefix, time).filter { it.isNotEmpty() }.joinToString(" ")
            return if (head.isEmpty()) item.title else "$head · ${item.title}"
        }

        private fun rid(ctx: Context, name: String) =
            ctx.resources.getIdentifier(name, "id", ctx.packageName)

        private fun contrastOn(color: Int): Int {
            val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255.0
            return if (lum > 0.65) 0xFF1C1C1E.toInt() else Color.WHITE
        }

        private fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val cfg = Settings.load(ctx)
            val monthColor = cfg.monthColor
            val dotColor = cfg.dotColor
            val offset = prefs(ctx).getInt(key(id), 0)
            val today = LocalDate.now()
            val ym = YearMonth.from(today).plusMonths(offset.toLong())
            val zone = ZoneId.systemDefault()
            val rv = RemoteViews(
                ctx.packageName,
                if (cfg.sundayStart) R.layout.widget_calendar_sun else R.layout.widget_calendar
            )

            // Intestazione mese (es. "Novembre 2026")
            val monthName = cap(ym.month.getDisplayName(TextStyle.FULL, IT))
            val title = "$monthName ${ym.year}"
            rv.setTextViewText(R.id.tv_month, title)
            // Mesi con nome lungo: testo più piccolo, così l'anno non viene tagliato
            val titleSp = when {
                title.length <= 10 -> 17f
                title.length <= 12 -> 14f
                title.length == 13 -> 13f
                else -> 12f
            }
            rv.setTextViewTextSize(R.id.tv_month, TypedValue.COMPLEX_UNIT_SP, titleSp)
            rv.setTextColor(R.id.tv_month, monthColor)
            rv.setTextColor(R.id.tv_weekday, monthColor)
            rv.setOnClickPendingIntent(R.id.tv_month, broadcastFor(ctx, ACTION_TODAY, id, id * 64 + 50))
            rv.setOnClickPendingIntent(R.id.btn_prev, broadcastFor(ctx, ACTION_PREV, id, id * 64 + 51))
            rv.setOnClickPendingIntent(R.id.btn_next, broadcastFor(ctx, ACTION_NEXT, id, id * 64 + 52))

            // Griglia
            val first = ym.atDay(1)
            val lead = if (cfg.sundayStart) first.dayOfWeek.value % 7 else first.dayOfWeek.value - 1
            val weeks = (lead + ym.lengthOfMonth() + 6) / 7
            for (r in 0 until 6) {
                rv.setViewVisibility(rid(ctx, "row_$r"), if (r < weeks) View.VISIBLE else View.GONE)
            }
            // Mesi con 6 righe: righe più basse, quindi numeri più piccoli e più in alto
            val tight = weeks >= 6
            val density = ctx.resources.displayMetrics.density
            val eventDays = if (cfg.dots) EventsRepository.daysWithEvents(ctx, ym) else emptySet()
            for (i in 0 until 42) {
                val cellId = rid(ctx, "cell_$i")
                val txtId = rid(ctx, "txt_$i")
                val dotId = rid(ctx, "dot_$i")
                val pillId = rid(ctx, "pill_$i")
                val date = first.plusDays((i - lead).toLong())
                if (YearMonth.from(date) == ym) {
                    val isToday = date == today
                    rv.setTextViewText(txtId, date.dayOfMonth.toString())
                    rv.setTextViewTextSize(txtId, TypedValue.COMPLEX_UNIT_SP, if (tight) 10f else 12f)
                    rv.setViewPadding(txtId, 0, 0, 0, (5 * density).toInt())
                    if (isToday) {
                        rv.setViewVisibility(pillId, View.VISIBLE)
                        rv.setInt(pillId, "setColorFilter", monthColor)
                        rv.setTextColor(txtId, contrastOn(monthColor))
                    }
                    if (date in eventDays) {
                        rv.setViewVisibility(dotId, View.VISIBLE)
                        rv.setInt(
                            dotId, "setColorFilter",
                            if (isToday) contrastOn(monthColor) else dotColor
                        )
                    }
                    val millis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                    rv.setOnClickPendingIntent(cellId, openCalendarAt(ctx, millis, id * 64 + i))
                } else {
                    rv.setTextViewText(txtId, "")
                }
            }

            // Pannello destro: oggi + prossimi eventi
            rv.setTextViewText(R.id.tv_weekday, cap(today.dayOfWeek.getDisplayName(TextStyle.FULL, IT)))
            rv.setTextViewText(R.id.tv_day_num, today.dayOfMonth.toString())
            if (!EventsRepository.hasPermission(ctx)) {
                rv.setTextViewText(R.id.tv_events, ctx.getString(R.string.no_permission))
                val open = PendingIntent.getActivity(
                    ctx, id * 64 + 53, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                rv.setOnClickPendingIntent(R.id.panel_right, open)
            } else {
                val events = EventsRepository.upcoming(ctx, cfg.eventCount)
                val text = if (events.isEmpty()) ctx.getString(R.string.no_events)
                else events.joinToString("\n") { formatEvent(ctx, it, today) }
                rv.setTextViewText(R.id.tv_events, text)
                rv.setOnClickPendingIntent(
                    R.id.panel_right, openCalendarAt(ctx, System.currentTimeMillis(), id * 64 + 54)
                )
            }

            mgr.updateAppWidget(id, rv)
        }
    }
}
