package com.ggandrocchio.readingcounter

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.MotionEvent
import android.widget.Button

class ReadingCounterOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var button: Button? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (button != null) return START_NOT_STICKY
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        button = Button(this).apply {
            text = "+1"
            contentDescription = "Aggiungi una pagina"
            setTextColor(Color.WHITE)
            textSize = 18f
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xCC6750A4.toInt()); setStroke(2, 0x99FFFFFF.toInt()) }
            alpha = 0.78f
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
                text = "${ReadingCounterService.count(this@ReadingCounterOverlayService)}  +1"
                postDelayed({ if (this@ReadingCounterOverlayService.button === this) text = "+1" }, 900)
            }
        }
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 12 }
        windowManager?.addView(button, params)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        button?.let { windowManager?.removeView(it) }
        button = null
        params = null
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
