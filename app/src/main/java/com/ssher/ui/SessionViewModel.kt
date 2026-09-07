package com.ssher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ssher.ssh.SshTransport
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

data class SessionUiState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val error: String? = null,
)

class SessionViewModel : ViewModel() {
    var uiState by mutableStateOf(SessionUiState())
        private set

    var terminalSession: TerminalSession? = null
        private set

    fun createSession(
        host: String,
        port: Int,
        username: String,
        password: String,
        client: TerminalSessionClient,
    ): TerminalSession {
        disconnect()
        uiState = SessionUiState(connecting = true)
        val transport = SshTransport(host, port, username, password)
        val session = TerminalSession(transport, /* transcriptRows */ 2000, client)
        terminalSession = session
        // Mark connected once the transport has a chance to start; real readiness is when
        // emulator receives data / user can type. updateSize() from TerminalView starts transport.
        uiState = uiState.copy(connecting = false, connected = true, error = null)
        return session
    }

    fun markDisconnected() {
        uiState = uiState.copy(connected = false, connecting = false)
    }

    fun disconnect() {
        val session = terminalSession ?: return
        // Keep the session reference until finish callbacks run; only stop the transport.
        session.finishIfRunning()
        uiState = uiState.copy(connected = false, connecting = false)
    }

    fun clearSession() {
        terminalSession = null
        uiState = uiState.copy(connected = false, connecting = false)
    }

    override fun onCleared() {
        terminalSession?.finishIfRunning()
        terminalSession = null
        super.onCleared()
    }
}