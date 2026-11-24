package com.ggandrocchio.readingcounter

import android.content.Context

internal enum class FloatingButtonColor(val label: String, val argb: Int) {
    PURPLE("Viola", 0xFF6750A4.toInt()),
    BLUE("Blu", 0xFF2558A5.toInt()),
    GREEN("Verde", 0xFF246647.toInt()),
    RED("Rosso", 0xFFAA3434.toInt()),
    SLATE("Grigio ardesia", 0xFF374151.toInt());

    fun save(context: Context) {
        ReadingCounterService.preferences(context).edit().putString(KEY, name).apply()
    }

    companion object {
        const val KEY = "floating_button_color"

        fun selected(context: Context): FloatingButtonColor {
            val saved = ReadingCounterService.preferences(context).getString(KEY, null)
            return entries.firstOrNull { it.name == saved } ?: PURPLE
        }
    }
}
