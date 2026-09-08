package com.anothersshclient.data

/**
 * App appearance preference. [System] follows the Android night-mode setting.
 */
enum class ThemeMode {
    Light,
    Dark,
    System,
    ;

    fun next(): ThemeMode = when (this) {
        Light -> Dark
        Dark -> System
        System -> Light
    }

    val label: String
        get() = when (this) {
            Light -> "Light"
            Dark -> "Dark"
            System -> "System"
        }
}
