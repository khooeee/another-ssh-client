package com.anothersshclient.ui.theme

/**
 * Light/dark hint for remote CLIs that probe the terminal (e.g. Cursor agent).
 * Today the app is paper-only (always light); flip [appTerminalTheme] when dark mode lands.
 */
enum class TerminalTheme {
    Light,
    Dark,
    ;

    /** `TERM_THEME` value consumed by Cursor CLI and similar tools. */
    val termThemeEnv: String
        get() = when (this) {
            Light -> "light"
            Dark -> "dark"
        }

    /** Classic `COLORFGBG` fg;bg indices (0 = black, 15 = white). */
    val colorFgBg: String
        get() = when (this) {
            Light -> "0;15"
            Dark -> "15;0"
        }

    /** Shell line to inject after the remote shell connects (echoed in the transcript). */
    fun shellExportCommand(): String =
        "export TERM_THEME=$termThemeEnv COLORFGBG='$colorFgBg'\n"
}

/** Source of truth for what theme hint new SSH sessions should advertise. */
fun appTerminalTheme(): TerminalTheme = TerminalTheme.Light
