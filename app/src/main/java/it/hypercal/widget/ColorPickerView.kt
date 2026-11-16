package it.hypercal.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Selettore colore: anteprima, codice esadecimale, colori rapidi e cursori Tonalità / Saturazione / Luminosità. */
class ColorPickerView(
    context: Context,
    initial: Int,
    private val onChange: (Int) -> Unit = {}
) : LinearLayout(context) {

    var color: Int = initial or (0xFF shl 24)
        private set

    private val dp = resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()

    private val hsv = FloatArray(3)
    private var updating = false

    private val previewDrawable = GradientDrawable().apply { shape = GradientDrawable.OVAL }
    private val hexField = EditText(context)
    private val sbH = SeekBar(context)
    private val sbS = SeekBar(context)
    private val sbV = SeekBar(context)

    private val seekListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            hsv[0] = sbH.progress.toFloat()
            hsv[1] = sbS.progress / 100f
            hsv[2] = sbV.progress / 100f
            color = Color.HSVToColor(hsv)
            refresh(updateSliders = false, updateHex = true)
            onChange(color)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private val presets = intArrayOf(
        0xFF3482FF.toInt(), 0xFFFF8A00.toInt(), 0xFF2DB84C.toInt(), 0xFFF2493B.toInt(),
        0xFF8E5CF6.toInt(), 0xFF12B5B0.toInt(), 0xFFFF4FA3.toInt()
    )

    init {
        orientation = VERTICAL
        Color.colorToHSV(color, hsv)

        // Anteprima + codice esadecimale
        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(View(context).apply {
            background = previewDrawable
            layoutParams = LinearLayout.LayoutParams(px(40), px(40)).apply { marginEnd = px(14) }
        })
        hexField.apply {
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(InputFilter.LengthFilter(7))
            setSingleLine()
            textSize = 16f
            minEms = 6
        }
        top.addView(hexField)
        addView(top)

        // Colori rapidi
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, px(12), 0, 0)
        }
        for (c in presets) {
            row.addView(View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                }
                layoutParams = LinearLayout.LayoutParams(px(32), px(32)).apply { marginEnd = px(8) }
                setOnClickListener { setColor(c) }
            })
        }
        addView(row)

        // Cursori
        addSlider("Tonalità", sbH, 359)
        addSlider("Saturazione", sbS, 100)
        addSlider("Luminosità", sbV, 100)
        sbH.background = track(
            intArrayOf(
                0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(),
                0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()
            )
        )

        hexField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                val t = s?.toString()?.trim().orEmpty().removePrefix("#")
                if (t.length == 6 && t.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                    color = Color.parseColor("#$t")
                    Color.colorToHSV(color, hsv)
                    refresh(updateSliders = true, updateHex = false)
                    onChange(color)
                }
            }
        })

        refresh(updateSliders = true, updateHex = true)
    }

    private fun addSlider(label: String, bar: SeekBar, max: Int) {
        addView(TextView(context).apply {
            text = label
            textSize = 12f
            setPadding(0, px(12), 0, px(2))
        })
        bar.max = max
        bar.progressTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bar.progressBackgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, px(30)
        )
        bar.setOnSeekBarChangeListener(seekListener)
        addView(bar)
    }

    private fun track(colors: IntArray): Drawable {
        val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
        g.cornerRadius = px(7).toFloat()
        return InsetDrawable(g, 0, px(8), 0, px(8))
    }

    private fun setColor(c: Int) {
        color = c or (0xFF shl 24)
        Color.colorToHSV(color, hsv)
        refresh(updateSliders = true, updateHex = true)
        onChange(color)
    }

    private fun refresh(updateSliders: Boolean, updateHex: Boolean) {
        updating = true
        previewDrawable.setColor(color)
        if (updateSliders) {
            sbH.progress = hsv[0].toInt().coerceIn(0, 359)
            sbS.progress = Math.round(hsv[1] * 100f)
            sbV.progress = Math.round(hsv[2] * 100f)
        }
        if (updateHex) {
            val txt = String.format("#%06X", color and 0xFFFFFF)
            if (hexField.text.toString() != txt) hexField.setText(txt)
        }
        sbS.background = track(
            intArrayOf(
                Color.HSVToColor(floatArrayOf(hsv[0], 0f, hsv[2])),
                Color.HSVToColor(floatArrayOf(hsv[0], 1f, hsv[2]))
            )
        )
        sbV.background = track(
            intArrayOf(
                Color.BLACK,
                Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))
            )
        )
        updating = false
    }
}
