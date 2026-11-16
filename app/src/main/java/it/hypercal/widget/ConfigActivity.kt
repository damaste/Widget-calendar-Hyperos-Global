package it.hypercal.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Schermata di configurazione: si apre quando si aggiunge il widget o dall'app. */
class ConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var accent = 0
    private val swatches = ArrayList<TextView>()
    private val dp by lazy { resources.displayMetrics.density }

    private lateinit var rbMonday: RadioButton
    private lateinit var rbSunday: RadioButton
    private lateinit var swDots: Switch
    private lateinit var rgCount: RadioGroup
    private val countButtons = ArrayList<RadioButton>()

    private fun px(v: Int) = (v * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val cur = Settings.load(this)
        accent = cur.accent

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(24), px(48), px(24), px(24))
        }

        col.addView(TextView(this).apply {
            text = getString(R.string.config_title)
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })

        // --- Colore ---
        col.addView(section(getString(R.string.config_color)))
        val swatchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Settings.ACCENTS.forEachIndexed { i, color ->
            val tv = TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                contentDescription = Settings.ACCENT_NAMES[i]
                layoutParams = LinearLayout.LayoutParams(px(44), px(44)).apply { marginEnd = px(10) }
                setOnClickListener { accent = i; refreshSwatches() }
            }
            swatches.add(tv)
            swatchRow.addView(tv)
        }
        col.addView(swatchRow)
        refreshSwatches()

        // --- Primo giorno della settimana ---
        col.addView(section(getString(R.string.config_week_start)))
        val rgStart = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        rbMonday = RadioButton(this).apply {
            id = View.generateViewId(); text = getString(R.string.config_monday)
        }
        rbSunday = RadioButton(this).apply {
            id = View.generateViewId(); text = getString(R.string.config_sunday)
        }
        rgStart.addView(rbMonday)
        rgStart.addView(rbSunday)
        rgStart.check(if (cur.sundayStart) rbSunday.id else rbMonday.id)
        col.addView(rgStart)

        // --- Pallini ---
        swDots = Switch(this).apply {
            text = getString(R.string.config_dots)
            isChecked = cur.dots
            setPadding(0, px(20), 0, px(4))
        }
        col.addView(swDots)

        // --- Numero eventi ---
        col.addView(section(getString(R.string.config_event_count)))
        rgCount = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        for (n in 1..4) {
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = n.toString()
                setPadding(px(4), 0, px(20), 0)
            }
            countButtons.add(rb)
            rgCount.addView(rb)
        }
        rgCount.check(countButtons[cur.eventCount - 1].id)
        col.addView(rgCount)

        // --- Salva ---
        col.addView(Button(this).apply {
            text = getString(R.string.config_save)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(28) }
            setOnClickListener { save() }
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun section(title: String) = TextView(this).apply {
        text = title
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Settings.ACCENTS[0])
        setPadding(0, px(24), 0, px(8))
    }

    private fun refreshSwatches() {
        swatches.forEachIndexed { i, tv -> tv.text = if (i == accent) "✓" else "" }
    }

    private fun save() {
        val count = countButtons.indexOfFirst { it.id == rgCount.checkedRadioButtonId } + 1
        Settings.save(
            this,
            Settings.Values(
                accent = accent,
                sundayStart = rbSunday.isChecked,
                dots = swDots.isChecked,
                eventCount = count.coerceIn(1, 4)
            )
        )
        CalendarWidgetProvider.refreshAll(this)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }
        finish()
    }
}
