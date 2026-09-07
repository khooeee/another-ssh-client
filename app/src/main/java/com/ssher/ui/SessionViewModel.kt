package com.ssher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssher.ssh.SshSession
import kotlinx.coroutines.launch

data class SessionUiState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val output: String = "",
    val error: String? = null,
)

class SessionViewModel : ViewModel() {
    private val session = SshSession()

    var uiState by mutableStateOf(SessionUiState())
        private set

    fun connect(host: String, port: Int, username: String, password: String) {
        if (uiState.connecting || uiState.connected) return
        uiState = SessionUiState(connecting = true, output = "Connecting to $host:$port…\n")
        viewModelScope.launch {
            runCatching {
                session.connect(
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    onOutput = { chunk ->
                        uiState = uiState.copy(
                            connecting = false,
                            connected = true,
                            output = (uiState.output + chunk).takeLast(100_000),
                            error = null,
                        )
                    },
                    onClosed = { reason ->
                        val suffix = reason?.let { "\n[disconnected] $it\n" } ?: "\n[disconnected]\n"
                        uiState = uiState.copy(
                            connecting = false,
                            connected = false,
                            output = uiState.output + suffix,
                        )
                    },
                )
                uiState = uiState.copy(connecting = false, connected = true, error = null)
            }.onFailure { error ->
                uiState = SessionUiState(
                    connecting = false,
                    connected = false,
                    output = uiState.output,
                    error = error.message ?: "Connection failed",
                )
            }
        }
    }

    fun send(line: String) {
        if (!session.isOpen) return
        session.write(line)
    }

    fun sendRaw(data: String) {
        if (!session.isOpen) return
        session.write(data)
    }

    fun disconnect() {
        session.disconnect()
        uiState = uiState.copy(connected = false, connecting = false)
    }

    override fun onCleared() {
        session.destroy()
        super.onCleared()
    }
}
