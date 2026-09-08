package com.anothersshclient.session

import com.anothersshclient.data.HostProfile
import com.termux.terminal.TerminalSession
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OpenSession(
    val id: String,
    val hostProfileId: String,
    val title: String,
    val host: String,
    val port: Int,
    val username: String,
    @Volatile var terminalSession: TerminalSession? = null,
)

data class PendingOpen(
    val hostProfileId: String,
    val title: String,
    val host: String,
    val port: Int,
    val username: String,
)

/**
 * App-scoped registry of live SSH sessions. Survives leaving the session screen.
 * Each connect creates a new session — including multiple to the same host.
 */
class SessionManager {
    private val _sessions = MutableStateFlow<List<OpenSession>>(emptyList())
    val sessions: StateFlow<List<OpenSession>> = _sessions.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _pendingOpen = MutableStateFlow<PendingOpen?>(null)
    val pendingOpen: StateFlow<PendingOpen?> = _pendingOpen.asStateFlow()

    fun queueOpen(profile: HostProfile) {
        _pendingOpen.value = PendingOpen(
            hostProfileId = profile.id,
            title = profile.name,
            host = profile.host,
            port = profile.port,
            username = profile.username,
        )
    }

    fun consumePending(): PendingOpen? {
        val pending = _pendingOpen.value ?: return null
        _pendingOpen.value = null
        return pending
    }

    fun clearPending() {
        _pendingOpen.value = null
    }

    fun label(session: OpenSession): String = session.title

    fun rename(id: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        val current = _sessions.value.find { it.id == id } ?: return
        if (current.title.equals(trimmed, ignoreCase = true)) {
            if (current.title != trimmed) {
                _sessions.update { list ->
                    list.map { session ->
                        if (session.id == id) session.copy(title = trimmed) else session
                    }
                }
            }
            return
        }
        val taken = _sessions.value.any { session ->
            session.id != id && session.title.equals(trimmed, ignoreCase = true)
        }
        if (taken) return
        _sessions.update { list ->
            list.map { session ->
                if (session.id == id) session.copy(title = trimmed) else session
            }
        }
    }

    fun activeSession(): OpenSession? =
        _sessions.value.find { it.id == _activeId.value }

    fun setActive(id: String) {
        if (_sessions.value.any { it.id == id }) {
            _activeId.value = id
        }
    }

    /** Cycle the active session. No-op with fewer than two sessions; still safe to call. */
    fun selectAdjacent(forward: Boolean) {
        val list = _sessions.value
        if (list.size < 2) return
        val idx = list.indexOfFirst { it.id == _activeId.value }
        if (idx < 0) return
        val next = if (forward) {
            (idx + 1) % list.size
        } else {
            (idx - 1 + list.size) % list.size
        }
        _activeId.value = list[next].id
    }

    fun register(
        pending: PendingOpen,
        terminalSession: TerminalSession,
    ): OpenSession {
        val open = OpenSession(
            id = UUID.randomUUID().toString(),
            hostProfileId = pending.hostProfileId,
            title = uniqueTitle(pending.title),
            host = pending.host,
            port = pending.port,
            username = pending.username,
            terminalSession = terminalSession,
        )
        _sessions.update { it + open }
        _activeId.value = open.id
        return open
    }

    fun disconnect(id: String) {
        val open = _sessions.value.find { it.id == id } ?: return
        val terminal = open.terminalSession
        if (terminal != null && terminal.isRunning) {
            terminal.finishIfRunning()
        } else {
            remove(id)
        }
    }

    fun disconnectActive() {
        val id = _activeId.value ?: return
        disconnect(id)
    }

    /** Remote shell exited or transport closed. */
    fun onTerminalFinished(terminal: TerminalSession) {
        val id = _sessions.value.find { it.terminalSession === terminal }?.id ?: return
        remove(id)
    }

    fun sessionCountForHost(hostProfileId: String): Int =
        _sessions.value.count { it.hostProfileId == hostProfileId }

    private fun uniqueTitle(base: String): String {
        val root = base.trim().ifEmpty { "Session" }
        if (_sessions.value.none { it.title.equals(root, ignoreCase = true) }) {
            return root
        }
        var n = 2
        while (true) {
            val candidate = "$root $n"
            if (_sessions.value.none { it.title.equals(candidate, ignoreCase = true) }) {
                return candidate
            }
            n++
        }
    }

    private fun remove(id: String) {
        val list = _sessions.value
        val index = list.indexOfFirst { it.id == id }
        val wasActive = _activeId.value == id
        _sessions.update { sessions -> sessions.filterNot { it.id == id } }
        if (!wasActive || index < 0) return
        val remaining = _sessions.value
        if (remaining.isEmpty()) {
            _activeId.value = null
            return
        }
        // Prefer the tab that shifted into this index (former next); if we closed the last, take previous.
        _activeId.value = remaining.getOrNull(index)?.id ?: remaining.last().id
    }
}
