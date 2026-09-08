package com.anothersshclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.anothersshclient.terminal.ExtraKeysState
import com.termux.view.TerminalView

@Composable
fun ExtraKeysBar(
    state: ExtraKeysState,
    terminalView: TerminalView?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .focusProperties { canFocus = false },
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExtraKeyButton(label = "ESC", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_ESCAPE)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "TAB", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_TAB)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "PASTE", onClick = {
                terminalView?.currentSession?.onPasteTextFromClipboard()
                state.consumeOneShot()
            })
            ExtraKeyButton(
                label = "CTRL",
                active = state.ctrl,
                onClick = { state.toggleCtrl() },
            )
            ExtraKeyButton(
                label = "ALT",
                active = state.alt,
                onClick = { state.toggleAlt() },
            )
            ExtraKeyButton(
                label = "SHIFT",
                active = state.shift,
                onClick = { state.toggleShift() },
            )
            ExtraKeyButton(label = "←", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_DPAD_LEFT)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "↑", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_DPAD_UP)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "↓", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_DPAD_DOWN)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "→", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_DPAD_RIGHT)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "HOME", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_MOVE_HOME)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "END", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_MOVE_END)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "PGUP", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_PAGE_UP)
                state.consumeOneShot()
            })
            ExtraKeyButton(label = "PGDN", onClick = {
                sendKey(terminalView, KeyEvent.KEYCODE_PAGE_DOWN)
                state.consumeOneShot()
            })
        }
    }
}

@Composable
private fun ExtraKeyButton(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val background = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = content,
        ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .focusProperties { canFocus = false },
    )
}

private fun sendKey(terminalView: TerminalView?, keyCode: Int) {
    val view = terminalView ?: return
    val now = System.currentTimeMillis()
    val down = KeyEvent(
        now,
        now,
        KeyEvent.ACTION_DOWN,
        keyCode,
        0,
        0,
        KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,
    )
    val up = KeyEvent(
        now,
        now,
        KeyEvent.ACTION_UP,
        keyCode,
        0,
        0,
        KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,
    )
    view.onKeyDown(keyCode, down)
    view.onKeyUp(keyCode, up)
    if (!view.hasFocus()) view.requestFocus()
}
