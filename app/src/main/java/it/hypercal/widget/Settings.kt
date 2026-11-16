package it.hypercal.widget

import android.content.Context

/** Impostazioni condivise da tutti i widget. */
object Settings {

    /** [calendars] = null significa "tutti i calendari". */
    data class Values(
        val accent: Int,
        val sundayStart: Boolean,
        val dots: Boolean,
        val eventCount: Int,
        val calendars: Set<Long>? = null
    )

    val ACCENTS = intArrayOf(
        0xFF3482FF.toInt(), 0xFFFF8A00.toInt(), 0xFF2DB84C.toInt(),
        0xFFF2493B.toInt(), 0xFF8E5CF6.toInt(), 0xFF12B5B0.toInt()
    )
    val ACCENT_NAMES = arrayOf("Blu", "Arancione", "Verde", "Rosso", "Viola", "Turchese")

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)

    fun load(ctx: Context): Values {
        val p = prefs(ctx)
        val raw = p.getStringSet("calendars", null)
        return Values(
            accent = p.getInt("accent", 0).coerceIn(0, ACCENTS.size - 1),
            sundayStart = p.getBoolean("sunday_start", false),
            dots = p.getBoolean("dots", true),
            eventCount = p.getInt("event_count", 3).coerceIn(1, 4),
            calendars = raw?.mapNotNull { it.toLongOrNull() }?.toSet()
        )
    }

    fun save(ctx: Context, v: Values) {
        val e = prefs(ctx).edit()
            .putInt("accent", v.accent)
            .putBoolean("sunday_start", v.sundayStart)
            .putBoolean("dots", v.dots)
            .putInt("event_count", v.eventCount)
        if (v.calendars == null) e.remove("calendars")
        else e.putStringSet("calendars", v.calendars.map { it.toString() }.toSet())
        // ✅ FIX: commit() sincrono invece di apply() asincrono
        e.commit()
        // ✅ FIX: forza il refresh del widget dopo il salvataggio
        CalendarWidgetProvider.refreshAll(ctx)
    }
}
