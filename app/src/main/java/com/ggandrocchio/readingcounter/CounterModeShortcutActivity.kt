package com.ggandrocchio.readingcounter

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

internal fun overlayPermissionIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

class CounterModeShortcutActivity : Activity() {
    private var openedOverlaySettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedOverlaySettings = savedInstanceState?.getBoolean("openedOverlaySettings") == true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || isFinishing) return

        if (!openedOverlaySettings && !Settings.canDrawOverlays(this)) {
            openedOverlaySettings = true
            try {
                startActivity(overlayPermissionIntent(this))
                return
            } catch (_: ActivityNotFoundException) {
                // The counter can still run on devices without an overlay settings screen.
            }
        }

        ReadingCounterService.start(this)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("openedOverlaySettings", openedOverlaySettings)
        super.onSaveInstanceState(outState)
    }
}
