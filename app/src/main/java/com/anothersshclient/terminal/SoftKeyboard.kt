package com.anothersshclient.terminal

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Explicit IME show — focus alone often does not reopen the keyboard after a HW keyboard disconnect. */
fun showSoftKeyboard(context: Context, view: View) {
    if (!view.hasFocus()) view.requestFocus()
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return
    // flags=0: explicit show request (Termux). SHOW_IMPLICIT is ignored when the system
    // still thinks a hardware keyboard is suppressing the IME.
    imm.showSoftInput(view, 0)
}

/**
 * True when the soft keyboard panel is actually on screen.
 *
 * [WindowInsets.isImeVisible] can be true with a hardware keyboard even when no soft
 * keyboard UI is shown; require a tall IME inset instead.
 */
@Composable
fun rememberSoftKeyboardVisible(): Boolean {
    val density = LocalDensity.current
    val minHeightPx = remember(density) { with(density) { 100.dp.roundToPx() } }
    return WindowInsets.ime.getBottom(density) >= minHeightPx
}
