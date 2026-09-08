package com.anothersshclient.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class TerminalPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var fontSizeSp: Int
        get() = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        set(value) {
            prefs.edit().putInt(KEY_FONT_SIZE, value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)).apply()
        }

    var themeMode: ThemeMode
        get() = ThemeMode.entries.find { it.name == prefs.getString(KEY_THEME_MODE, null) }
            ?: ThemeMode.Light
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    fun themeModeFlow(): Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_THEME_MODE) {
                trySend(themeMode)
            }
        }
        trySend(themeMode)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    companion object {
        const val MIN_FONT_SIZE = 8
        const val MAX_FONT_SIZE = 48
        const val DEFAULT_FONT_SIZE = 15

        private const val PREFS_NAME = "another_ssh_client_terminal"
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
