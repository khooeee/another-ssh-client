package com.anothersshclient.ui

/**
 * Process-scoped flag: brand typewriter runs once per app launch.
 * Leaving mid-animation still marks it done so returning shows the full title.
 */
object BrandIntro {
    @Volatile
    var hasCompleted: Boolean = false
}
