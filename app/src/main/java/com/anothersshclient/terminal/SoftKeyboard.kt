package com.anothersshclient.terminal

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/** Explicit IME show — focus alone often does not reopen the keyboard after a HW keyboard disconnect. */
fun showSoftKeyboard(context: Context, view: View) {
    if (!view.hasFocus()) view.requestFocus()
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return
    // flags=0: explicit show request (Termux). SHOW_IMPLICIT is ignored when the system
    // still thinks a hardware keyboard is suppressing the IME.
    imm.showSoftInput(view, 0)
}
