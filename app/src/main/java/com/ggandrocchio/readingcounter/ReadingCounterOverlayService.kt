package com.ggandrocchio.readingcounter

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button

class ReadingCounterOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var button: Button? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (button != null) return START_NOT_STICKY
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        button = Button(this).apply {
            text = "+1"
            contentDescription = "Aggiungi una pagina"
            setOnClickListener {
                startService(Intent(this@ReadingCounterOverlayService, ReadingCounterService::class.java).setAction(ReadingCounterService.ACTION_INCREMENT))
                text = "${ReadingCounterService.count(this@ReadingCounterOverlayService)}  +1"
                postDelayed({ if (this@ReadingCounterOverlayService.button === this) text = "+1" }, 900)
            }
        }
        val params = WindowManager.LayoutParams(
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
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
