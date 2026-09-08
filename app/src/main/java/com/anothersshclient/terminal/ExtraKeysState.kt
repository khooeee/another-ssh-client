package com.anothersshclient.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Sticky modifiers for the on-screen extra-keys bar.
 * Toggles stay on until tapped again, or until [consumeOneShot] after a key is sent.
 */
class ExtraKeysState {
    var ctrl by mutableStateOf(false)
        private set
    var alt by mutableStateOf(false)
        private set
    var shift by mutableStateOf(false)
        private set

    val anyActive: Boolean
        get() = ctrl || alt || shift

    fun toggleCtrl() {
        ctrl = !ctrl
    }

    fun toggleAlt() {
        alt = !alt
    }

    fun toggleShift() {
        shift = !shift
    }

    /** Clear sticky modifiers after they have been applied to a key. */
    fun consumeOneShot() {
        ctrl = false
        alt = false
        shift = false
    }
}
