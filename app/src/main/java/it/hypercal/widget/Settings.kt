package it.hypercal.widget

import android.content.Context

/** Impostazioni condivise da tutti i widget. */
object Settings {

    /** [calendars] = null significa "tutti i calendari". */
    data class Values(
        val monthColor: Int,
        val dotColor: Int,
        val sundayStart: Boolean,
        val dots: Boolean,
        val eventCount: Int,
        val calendars: Set<Long>? = null
    )

    /** Colori predefiniti (usati anche per migrare la vecchia impostazione a indice). */
    val ACCENTS = intArrayOf(
        0xFF3482FF.toInt(), 0xFFFF8A00.toInt(), 0xFF2DB84C.toInt(),
        0xFFF2493B.toInt(), 0xFF8E5CF6.toInt(), 0xFF12B5B0.toInt()
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)

    fun load(ctx: Context): Values {
        val p = prefs(ctx)
        val legacy = ACCENTS[p.getInt("accent", 0).coerceIn(0, ACCENTS.size - 1)]
        val month = p.getInt("month_color", legacy) or (0xFF shl 24)
        val dot = p.getInt("dot_color", legacy) or (0xFF shl 24)
        val raw = p.getStringSet("calendars", null)
        return Values(
            monthColor = month,
            dotColor = dot,
            sundayStart = p.getBoolean("sunday_start", false),
            dots = p.getBoolean("dots", true),
            eventCount = p.getInt("event_count", 4).coerceIn(1, 6),
            calendars = raw?.mapNotNull { it.toLongOrNull() }?.toSet()
        )
    }

    fun save(ctx: Context, v: Values) {
        val e = prefs(ctx).edit()
            .putInt("month_color", v.monthColor)
            .putInt("dot_color", v.dotColor)
            .putBoolean("sunday_start", v.sundayStart)
            .putBoolean("dots", v.dots)
            .putInt("event_count", v.eventCount)
        if (v.calendars == null) e.remove("calendars")
        else e.putStringSet("calendars", v.calendars.map { it.toString() }.toSet())
        e.apply()
    }
}
