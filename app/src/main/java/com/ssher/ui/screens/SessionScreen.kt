package com.ssher.ui.screens

import android.graphics.Typeface
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssher.terminal.SsherTerminalClients
import com.ssher.ui.SessionViewModel
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

/** Shared inset: equal top/bottom of the session header, and top of the terminal. */
private val SessionChromePaddingHorizontal: Dp = 16.dp
private val SessionChromePaddingVertical: Dp = 12.dp

@Composable
fun SessionScreen(
    hostId: String,
    name: String,
    host: String,
    port: Int,
    username: String,
    loadPassword: suspend (String) -> String?,
    onBack: () -> Unit,
    viewModel: SessionViewModel = viewModel(),
) {
    val state = viewModel.uiState
    var passwordPromptOpen by remember { mutableStateOf(false) }
    var resolvingPassword by remember { mutableStateOf(true) }
    var sessionPassword by remember { mutableStateOf<String?>(null) }
    var connectionId by remember { mutableIntStateOf(0) }
    val leftSession = remember { AtomicBoolean(false) }
    val onBackLatest = rememberUpdatedState(onBack)
    // Navigate home only after the session has finished (same path as Ctrl+D).
    val leaveToHostList = remember(leftSession, onBackLatest, viewModel) {
        {
            if (leftSession.compareAndSet(false, true)) {
                viewModel.clearSession()
                onBackLatest.value()
            }
        }
    }

    // Stay in the session; leave via Disconnect or when the remote shell exits (e.g. Ctrl+D).
    BackHandler(enabled = true) { }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    LaunchedEffect(hostId) {
        resolvingPassword = true
        leftSession.set(false)
        viewModel.disconnect()
        val saved = loadPassword(hostId)
        if (!saved.isNullOrEmpty()) {
            sessionPassword = saved
            passwordPromptOpen = false
            connectionId++
        } else {
            sessionPassword = null
            passwordPromptOpen = true
        }
        resolvingPassword = false
    }

    fun startWithPassword(value: String) {
        sessionPassword = value
        passwordPromptOpen = false
        connectionId++
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = SessionChromePaddingHorizontal,
                            vertical = SessionChromePaddingVertical,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "$username@$host:$port",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(
                        onClick = {
                            // Close the transport; onSessionFinished navigates (same as Ctrl+D).
                            // If already dead, leave immediately.
                            val session = viewModel.terminalSession
                            if (session == null || !session.isRunning) {
                                leaveToHostList()
                            } else {
                                viewModel.disconnect()
                            }
                        },
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        Text("Disconnect")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
        },
    ) { padding ->
        val password = sessionPassword
        if (!resolvingPassword && password != null && !passwordPromptOpen) {
            key(connectionId) {
                AndroidView(
                    factory = { ctx ->
                        val view = TerminalView(ctx, null).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            // setTextSize creates mRenderer; setTypeface requires it.
                            setTextSize(com.ssher.data.TerminalPreferences(ctx).fontSizeSp)
                            setTypeface(Typeface.MONOSPACE)
                            isFocusable = true
                            isFocusableInTouchMode = true
                        }
                        val baseClients = SsherTerminalClients(
                            context = ctx,
                            terminalView = view,
                            onSessionFinished = { leaveToHostList() },
                        )

                        lateinit var session: TerminalSession
                        val viewClient = object : TerminalViewClient by baseClients {
                            override fun onEmulatorSet() {
                                applyPaperScheme(session)
                                baseClients.onEmulatorSet()
                                view.requestFocus()
                            }
                        }
                        view.setTerminalViewClient(viewClient)
                        session = viewModel.createSession(
                            host = host,
                            port = port,
                            username = username,
                            password = password,
                            client = baseClients,
                        )
                        view.attachSession(session)
                        view
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(
                            start = SessionChromePaddingHorizontal,
                            end = SessionChromePaddingHorizontal,
                            top = SessionChromePaddingVertical,
                        ),
                    update = { view ->
                        if (!view.hasFocus()) view.requestFocus()
                    },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text(
                    when {
                        resolvingPassword -> "Loading credentials…"
                        passwordPromptOpen -> "Enter password to connect"
                        else -> state.error ?: "Waiting…"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (passwordPromptOpen && !resolvingPassword) {
        PasswordDialog(
            username = username,
            host = host,
            onDismiss = {
                passwordPromptOpen = false
                if (sessionPassword == null) onBack()
            },
            onConnect = { startWithPassword(it) },
        )
    }
}

private fun applyPaperScheme(session: TerminalSession) {
    val emulator = session.emulator ?: return
    val props = Properties().apply {
        setProperty("foreground", "#000000")
        setProperty("background", "#FFFFFF")
        setProperty("cursor", "#000000")
        setProperty("color0", "#000000")
        setProperty("color1", "#444444")
        setProperty("color2", "#555555")
        setProperty("color3", "#666666")
        setProperty("color4", "#777777")
        setProperty("color5", "#888888")
        setProperty("color6", "#999999")
        setProperty("color7", "#BBBBBB")
        setProperty("color8", "#333333")
        setProperty("color9", "#555555")
        setProperty("color10", "#666666")
        setProperty("color11", "#777777")
        setProperty("color12", "#888888")
        setProperty("color13", "#999999")
        setProperty("color14", "#AAAAAA")
        setProperty("color15", "#000000")
    }
    TerminalColors.COLOR_SCHEME.updateWith(props)
    emulator.mColors.reset()
}

@Composable
private fun PasswordDialog(
    username: String,
    host: String,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.onSurface,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password") },
        text = {
            Column {
                Text(
                    "Sign in as $username@$host",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = colors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(password) },
                enabled = password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
