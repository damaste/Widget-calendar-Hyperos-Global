package it.hypercal.widget

import android.content.Context

/** Impostazioni condivise da tutti i widget. */
object Settings {

    data class Values(
        val accent: Int,
        val sundayStart: Boolean,
        val dots: Boolean,
        val eventCount: Int
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
        return Values(
            accent = p.getInt("accent", 0).coerceIn(0, ACCENTS.size - 1),
            sundayStart = p.getBoolean("sunday_start", false),
            dots = p.getBoolean("dots", true),
            eventCount = p.getInt("event_count", 3).coerceIn(1, 4)
        )
    }

    fun save(ctx: Context, v: Values) {
        prefs(ctx).edit()
            .putInt("accent", v.accent)
            .putBoolean("sunday_start", v.sundayStart)
            .putBoolean("dots", v.dots)
            .putInt("event_count", v.eventCount)
            .apply()
    }
}
