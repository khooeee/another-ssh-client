package com.ssher.ssh

import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalTransport
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.connection.channel.direct.SessionChannel
import net.schmizz.sshj.transport.verification.HostKeyVerifier

/**
 * sshj shell transport feeding a Termux [TerminalSession].
 */
class SshTransport(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
) : TerminalTransport {

    private var client: SSHClient? = null
    private var session: Session? = null
    private var sessionChannel: SessionChannel? = null
    private var out: OutputStream? = null
    private val writeExec = Executors.newSingleThreadExecutor()
    private val closed = AtomicBoolean(false)

    override fun start(
        terminalSession: TerminalSession,
        columns: Int,
        rows: Int,
        cellWidthPixels: Int,
        cellHeightPixels: Int,
    ) {
        Thread({
            try {
                CryptoInit.ensureBouncyCastle()
                feed(terminalSession, "Connecting to $host:$port…\r\n")

                val ssh = SSHClient().apply {
                    addHostKeyVerifier(TrustAllHostKeys)
                    connectTimeout = 15_000
                    // Interactive shells must not use a read timeout.
                    timeout = 0
                }
                ssh.connect(host, port)
                ssh.authPassword(username, password)
                runCatching { ssh.connection.keepAlive.keepAliveInterval = 30 }

                val sess = ssh.startSession()
                sess.allocatePTY(
                    "xterm-256color",
                    columns,
                    rows,
                    cellWidthPixels,
                    cellHeightPixels,
                    emptyMap(),
                )
                val shell = sess.startShell()

                client = ssh
                session = sess
                sessionChannel = sess as? SessionChannel
                out = shell.outputStream

                val input = shell.inputStream
                val buf = ByteArray(8192)
                while (!closed.get()) {
                    val n = input.read(buf)
                    if (n < 0) break
                    if (n > 0) terminalSession.processToEmulator(buf, n)
                }
                terminalSession.onTransportFinished(0)
            } catch (e: Exception) {
                if (!closed.get()) {
                    val msg = "\r\nConnection failed: ${e.message ?: e.javaClass.simpleName}\r\n"
                    feed(terminalSession, msg)
                    terminalSession.onTransportFinished(1)
                }
            }
        }, "ssher-ssh-$host").start()
    }

    override fun write(data: ByteArray, offset: Int, count: Int) {
        if (closed.get()) return
        val copy = data.copyOfRange(offset, offset + count)
        writeExec.execute {
            runCatching {
                out?.write(copy)
                out?.flush()
            }
        }
    }

    override fun updateSize(columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int) {
        if (closed.get()) return
        val channel = sessionChannel ?: return
        writeExec.execute {
            runCatching {
                channel.changeWindowDimensions(columns, rows, cellWidthPixels, cellHeightPixels)
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        writeExec.execute {
            runCatching { session?.close() }
            runCatching { client?.disconnect() }
            runCatching { client?.close() }
        }
        writeExec.shutdown()
        runCatching { writeExec.awaitTermination(2, TimeUnit.SECONDS) }
    }

    private fun feed(session: TerminalSession, message: String) {
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        session.processToEmulator(bytes, bytes.size)
    }
}

private object TrustAllHostKeys : HostKeyVerifier {
    override fun verify(hostname: String?, port: Int, key: PublicKey?): Boolean = true

    override fun findExistingAlgorithms(hostname: String?, port: Int): MutableList<String> =
        mutableListOf()
}
