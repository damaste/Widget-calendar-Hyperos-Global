package it.hypercal.widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var permButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        col.addView(TextView(this).apply {
            text = getString(R.string.main_title)
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.accent))
        })
        col.addView(TextView(this).apply {
            text = getString(R.string.main_body)
            textSize = 16f
            setPadding(0, pad / 2, 0, pad / 2)
        })
        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, pad / 2, 0, pad / 2)
        }
        col.addView(status)

        permButton = Button(this).apply {
            text = getString(R.string.btn_permission)
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 1)
            }
        }
        col.addView(permButton)

        col.addView(Button(this).apply {
            text = getString(R.string.btn_settings)
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, ConfigActivity::class.java))
            }
        })

        val mgr = getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) {
            col.addView(Button(this).apply {
                text = getString(R.string.btn_add_widget)
                setOnClickListener {
                    mgr.requestPinAppWidget(
                        ComponentName(this@MainActivity, CalendarWidgetProvider::class.java),
                        null, null
                    )
                }
            })
        }

        col.addView(TextView(this).apply {
            text = getString(R.string.main_tip)
            textSize = 13f
            gravity = Gravity.START
            setPadding(0, pad, 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
        CalendarWidgetProvider.refreshAll(this)
    }

    private fun updateStatus() {
        val ok = checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        status.text = getString(if (ok) R.string.status_ok else R.string.status_ko)
        permButton.isEnabled = !ok
    }
}
