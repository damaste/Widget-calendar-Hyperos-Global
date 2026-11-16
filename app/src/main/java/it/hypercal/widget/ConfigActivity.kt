package it.hypercal.widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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

    private lateinit var current: Settings.Values
    private lateinit var rbMonday: RadioButton
    private lateinit var rbSunday: RadioButton
    private lateinit var swDots: Switch
    private lateinit var rgCount: RadioGroup
    private val countButtons = ArrayList<RadioButton>()
    private lateinit var calContainer: LinearLayout
    private val calChecks = LinkedHashMap<Long, CheckBox>()

    private fun px(v: Int) = (v * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        current = Settings.load(this)
        val cur = current
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

        // --- Calendari / account ---
        col.addView(section("Calendari da mostrare"))
        col.addView(TextView(this).apply {
            text = "Scegli da quali account e calendari prendere appuntamenti ed eventi " +
                "(pallini e pannello a destra)."
            textSize = 13f
        })
        calContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(calContainer)
        populateCalendars()

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

    private fun populateCalendars() {
        calContainer.removeAllViews()
        calChecks.clear()

        if (!EventsRepository.hasPermission(this)) {
            calContainer.addView(TextView(this).apply {
                text = "Per scegliere i calendari serve il permesso di lettura del calendario."
                setPadding(0, px(8), 0, px(4))
            })
            calContainer.addView(Button(this).apply {
                text = getString(R.string.btn_permission)
                setOnClickListener {
                    requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 2)
                }
            })
            return
        }

        val list = EventsRepository.listCalendars(this)
        if (list.isEmpty()) {
            calContainer.addView(TextView(this).apply {
                text = "Nessun calendario trovato sul dispositivo."
                setPadding(0, px(8), 0, px(4))
            })
            return
        }

        val selected = current.calendars
        var lastAccount: String? = null
        for (c in list) {
            if (c.account != lastAccount) {
                calContainer.addView(TextView(this).apply {
                    text = c.account
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, px(14), 0, px(2))
                })
                lastAccount = c.account
            }
            val cb = CheckBox(this).apply {
                text = c.name
                isChecked = selected == null || c.id in selected
                buttonTintList = ColorStateList.valueOf(c.color)
            }
            calChecks[c.id] = cb
            calContainer.addView(cb)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        populateCalendars()
        CalendarWidgetProvider.refreshAll(this)
    }

    private fun save() {
        val count = countButtons.indexOfFirst { it.id == rgCount.checkedRadioButtonId } + 1
        val calendars: Set<Long>? = when {
            calChecks.isEmpty() -> current.calendars          // elenco non disponibile: non cambiare
            calChecks.values.all { it.isChecked } -> null     // tutti (inclusi quelli futuri)
            else -> calChecks.filterValues { it.isChecked }.keys.toSet()
        }
        Settings.save(
            this,
            Settings.Values(
                accent = accent,
                sundayStart = rbSunday.isChecked,
                dots = swDots.isChecked,
                eventCount = count.coerceIn(1, 4),
                calendars = calendars
            )
        )
        CalendarWidgetProvider.refreshAll(this)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }
        finish()
    }
}
