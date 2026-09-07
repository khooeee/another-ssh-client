package com.ssher.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import com.ssher.data.TerminalPreferences
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

/** Minimal Termux clients: keep focus on the terminal, route clipboard, ignore modifier extras. */
class SsherTerminalClients(
    private val context: Context,
    private val terminalView: TerminalView,
    private val onSessionFinished: () -> Unit,
    private val preferences: TerminalPreferences = TerminalPreferences(context),
) : TerminalViewClient, TerminalSessionClient {

    private var fontSizeSp: Int = preferences.fontSizeSp

    fun applySavedFontSize() {
        fontSizeSp = preferences.fontSizeSp
        terminalView.setTextSize(fontSizeSp)
    }

    override fun onScale(scale: Float): Float {
        // TerminalView accumulates gesture scale into `scale` and expects us to
        // return 1f after applying a font change (Termux behavior).
        if (scale < 0.9f || scale > 1.1f) {
            fontSizeSp = (fontSizeSp + if (scale > 1f) 1 else -1)
                .coerceIn(TerminalPreferences.MIN_FONT_SIZE, TerminalPreferences.MAX_FONT_SIZE)
            terminalView.setTextSize(fontSizeSp)
            preferences.fontSizeSp = fontSizeSp
            return 1f
        }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent) {
        terminalView.requestFocus()
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) = Unit

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean = false
    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() = Unit

    override fun onTextChanged(changedSession: TerminalSession) {
        if (!terminalView.isAttachedToWindow) return
        terminalView.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) = Unit

    override fun onSessionFinished(finishedSession: TerminalSession) {
        onSessionFinished()
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
        session?.write(text)
    }

    override fun onBell(session: TerminalSession) = Unit

    override fun onColorsChanged(session: TerminalSession) {
        if (!terminalView.isAttachedToWindow) return
        terminalView.onScreenUpdated()
    }

    override fun onTerminalCursorStateChange(state: Boolean) = Unit
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) = Unit
    override fun getTerminalCursorStyle(): Int? = null

    override fun logError(tag: String, message: String) = Unit
    override fun logWarn(tag: String, message: String) = Unit
    override fun logInfo(tag: String, message: String) = Unit
    override fun logDebug(tag: String, message: String) = Unit
    override fun logVerbose(tag: String, message: String) = Unit
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception?) = Unit
    override fun logStackTrace(tag: String, e: Exception?) = Unit
}
