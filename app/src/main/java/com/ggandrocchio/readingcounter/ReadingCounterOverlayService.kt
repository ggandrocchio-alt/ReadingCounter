package com.ggandrocchio.readingcounter

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.MotionEvent
import android.widget.Button
import java.text.NumberFormat

class ReadingCounterOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var button: Button? = null
    private var params: WindowManager.LayoutParams? = null
    private val numberFormat = NumberFormat.getIntegerInstance()
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            ReadingCounterService.COUNT_KEY -> updateCount()
            FloatingButtonColor.KEY -> updateColor()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (button != null) return START_NOT_STICKY
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val floatingButton = Button(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
            elevation = 8f
            val touch = FloatArray(4)
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { touch[0] = event.rawX; touch[1] = event.rawY; touch[2] = (params?.x ?: 0).toFloat(); touch[3] = (params?.y ?: 0).toFloat(); true }
                    MotionEvent.ACTION_MOVE -> { params?.let { lp -> lp.x = (touch[2] + touch[0] - event.rawX).toInt(); lp.y = (touch[3] + event.rawY - touch[1]).toInt(); windowManager?.updateViewLayout(view, lp) }; true }
                    MotionEvent.ACTION_UP -> { if (kotlin.math.abs(event.rawX-touch[0]) < 8 && kotlin.math.abs(event.rawY-touch[1]) < 8) view.performClick(); true }
                    else -> true
                }
            }
            setOnClickListener {
                startService(Intent(this@ReadingCounterOverlayService, ReadingCounterService::class.java).setAction(ReadingCounterService.ACTION_INCREMENT))
            }
        }
        button = floatingButton
        ReadingCounterService.preferences(this).registerOnSharedPreferenceChangeListener(preferenceListener)
        updateCount()
        updateColor()
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 12 }
        windowManager?.addView(floatingButton, params)
        return START_NOT_STICKY
    }

    private fun updateCount() {
        val count = numberFormat.format(ReadingCounterService.count(this))
        button?.apply {
            text = count
            contentDescription = getString(R.string.floating_counter_description, count)
        }
    }

    private fun updateColor() {
        button?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(FloatingButtonColor.selected(this@ReadingCounterOverlayService).argb)
            setStroke(2, 0x99FFFFFF.toInt())
        }
    }

    override fun onDestroy() {
        ReadingCounterService.preferences(this).unregisterOnSharedPreferenceChangeListener(preferenceListener)
        button?.let { windowManager?.removeView(it) }
        button = null
        params = null
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
