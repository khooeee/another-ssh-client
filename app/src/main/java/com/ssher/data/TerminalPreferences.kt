package com.ssher.data

import android.content.Context

class TerminalPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var fontSizeSp: Int
        get() = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        set(value) {
            prefs.edit().putInt(KEY_FONT_SIZE, value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)).apply()
        }

    companion object {
        const val MIN_FONT_SIZE = 8
        const val MAX_FONT_SIZE = 48
        const val DEFAULT_FONT_SIZE = 15

        private const val PREFS_NAME = "ssher_terminal"
        private const val KEY_FONT_SIZE = "font_size_sp"
    }
}
