package com.ssher.ssh

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.HostKeyVerifier

class SshSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: SSHClient? = null
    private var session: Session? = null
    private var shell: Session.Shell? = null
    private var readerJob: Job? = null
    private val open = AtomicBoolean(false)

    val isOpen: Boolean get() = open.get()

    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String,
        onOutput: (String) -> Unit,
        onClosed: (String?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        CryptoInit.ensureBouncyCastle()

        if (open.get()) {
            disconnect()
        }

        val ssh = SSHClient().apply {
            // First version trusts all host keys; pin keys in a later release.
            addHostKeyVerifier(TrustAllHostKeys)
            connectTimeout = 15_000
            timeout = 30_000
        }

        try {
            ssh.connect(host, port)
            ssh.authPassword(username, password)

            val sess = ssh.startSession()
            sess.allocateDefaultPTY()
            val sh = sess.startShell()

            client = ssh
            session = sess
            shell = sh
            open.set(true)

            readerJob = scope.launch {
                readLoop(sh.inputStream, onOutput, onClosed)
            }
        } catch (t: Throwable) {
            closeQuietly()
            throw t
        }
    }

    fun write(data: String) {
        val out: OutputStream = shell?.outputStream ?: return
        scope.launch {
            runCatching {
                out.write(data.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }
    }

    fun resize(columns: Int, rows: Int) {
        // sshj Shell does not expose easy resize after start; no-op for MVP.
    }

    fun disconnect() {
        open.set(false)
        readerJob?.cancel()
        closeQuietly()
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }

    private suspend fun readLoop(
        input: InputStream,
        onOutput: (String) -> Unit,
        onClosed: (String?) -> Unit,
    ) {
        val buffer = ByteArray(4096)
        try {
            while (scope.isActive && open.get()) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                val chunk = String(buffer, 0, read, Charsets.UTF_8)
                withContext(Dispatchers.Main) {
                    onOutput(AnsiStripper.strip(chunk))
                }
            }
            withContext(Dispatchers.Main) {
                onClosed(null)
            }
        } catch (io: IOException) {
            withContext(Dispatchers.Main) {
                onClosed(io.message)
            }
        } finally {
            open.set(false)
            closeQuietly()
        }
    }

    private fun closeQuietly() {
        runCatching { shell?.close() }
        runCatching { session?.close() }
        runCatching { client?.disconnect() }
        runCatching { client?.close() }
        shell = null
        session = null
        client = null
    }
}

private object TrustAllHostKeys : HostKeyVerifier {
    override fun verify(hostname: String?, port: Int, key: PublicKey?): Boolean = true

    override fun findExistingAlgorithms(hostname: String?, port: Int): MutableList<String> =
        mutableListOf()
}
