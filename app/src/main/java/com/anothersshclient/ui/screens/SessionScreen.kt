package com.anothersshclient.ui.screens

import android.graphics.Typeface
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anothersshclient.session.PendingOpen
import com.anothersshclient.session.SessionManager
import com.anothersshclient.ssh.SshTransport
import com.anothersshclient.terminal.AppTerminalClients
import com.anothersshclient.terminal.NoOpTerminalSessionClient
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.util.Properties

private val SessionChromePaddingHorizontal: Dp = 16.dp
private val SessionChromePaddingVertical: Dp = 12.dp

@Composable
fun SessionScreen(
    sessionManager: SessionManager,
    loadPassword: suspend (String) -> String?,
    onLeaveToHosts: () -> Unit,
) {
    val sessions by sessionManager.sessions.collectAsStateWithLifecycle()
    val activeId by sessionManager.activeId.collectAsStateWithLifecycle()
    val pendingFlow by sessionManager.pendingOpen.collectAsStateWithLifecycle()
    val active = sessions.find { it.id == activeId }

    var awaitingPasswordFor by remember { mutableStateOf<PendingOpen?>(null) }
    var isStarting by remember { mutableStateOf(false) }
    var hadLiveSession by remember { mutableStateOf(false) }

    val onLeaveLatest = rememberUpdatedState(onLeaveToHosts)

    BackHandler {
        sessionManager.clearPending()
        awaitingPasswordFor = null
        isStarting = false
        onLeaveLatest.value()
    }

    LaunchedEffect(sessions.size) {
        if (sessions.isNotEmpty()) hadLiveSession = true
    }

    LaunchedEffect(sessions.size, pendingFlow, awaitingPasswordFor, isStarting) {
        if (hadLiveSession &&
            sessions.isEmpty() &&
            pendingFlow == null &&
            awaitingPasswordFor == null &&
            !isStarting
        ) {
            onLeaveLatest.value()
        }
    }

    // Collect pending opens in a stable effect. Keying on pendingFlow cancels mid-flight when
    // consumePending() clears it, which left isStarting=true and a stuck "Connecting…" screen.
    LaunchedEffect(Unit) {
        sessionManager.pendingOpen.collect { pending ->
            if (pending == null) return@collect
            sessionManager.consumePending()
            isStarting = true
            try {
                val saved = loadPassword(pending.hostProfileId)
                if (saved.isNullOrEmpty()) {
                    awaitingPasswordFor = pending
                } else {
                    startSession(sessionManager, pending, saved)
                }
            } finally {
                isStarting = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (event.key != Key.Tab || !event.isCtrlPressed) return@onPreviewKeyEvent false
            sessionManager.selectAdjacent(forward = !event.isShiftPressed)
            true
        },
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
                        Text(
                            text = active?.let { sessionManager.label(it) } ?: "Sessions",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (active != null) {
                            Text(
                                "${active.username}@${active.host}:${active.port}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            sessionManager.clearPending()
                            awaitingPasswordFor = null
                            isStarting = false
                            onLeaveToHosts()
                        },
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        Text("New Session")
                    }
                    TextButton(
                        onClick = {
                            val id = activeId
                            if (id == null) onLeaveToHosts() else sessionManager.disconnect(id)
                        },
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        Text("Disconnect")
                    }
                }

                if (sessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SessionChromePaddingHorizontal),
                    ) {
                        // Manila folder edge — continuous under inactive tabs / empty space.
                        HorizontalDivider(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .zIndex(0f),
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 1.dp,
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .zIndex(1f)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            sessions.forEachIndexed { index, session ->
                                FilingCabinetTab(
                                    label = sessionManager.label(session),
                                    selected = session.id == activeId,
                                    onClick = { sessionManager.setActive(session.id) },
                                    modifier = Modifier
                                        .widthIn(min = 72.dp)
                                        .offset(x = if (index > 0) (-1).dp else 0.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        val terminal = active?.terminalSession
        if (terminal != null) {
            key(activeId) {
                AndroidView(
                    factory = { ctx ->
                        val view = TerminalView(ctx, null).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setTextSize(
                                com.anothersshclient.data.TerminalPreferences(ctx).fontSizeSp,
                            )
                            setTypeface(Typeface.MONOSPACE)
                            isFocusable = true
                            isFocusableInTouchMode = true
                        }
                        val baseClients = AppTerminalClients(
                            context = ctx,
                            terminalView = view,
                            onFinished = { finished -> sessionManager.onTerminalFinished(finished) },
                        )
                        val viewClient = object : TerminalViewClient by baseClients {
                            override fun onEmulatorSet() {
                                applyPaperScheme(terminal)
                                baseClients.onEmulatorSet()
                                view.requestFocus()
                            }

                            override fun onKeyDown(
                                keyCode: Int,
                                e: KeyEvent,
                                session: TerminalSession,
                            ): Boolean {
                                if (keyCode == KeyEvent.KEYCODE_TAB && e.isCtrlPressed) {
                                    sessionManager.selectAdjacent(forward = !e.isShiftPressed)
                                    return true
                                }
                                return baseClients.onKeyDown(keyCode, e, session)
                            }
                        }
                        view.setTerminalViewClient(viewClient)
                        terminal.updateTerminalSessionClient(baseClients)
                        view.attachSession(terminal)
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
                        awaitingPasswordFor != null -> "Enter password to connect"
                        isStarting -> "Connecting…"
                        sessions.isNotEmpty() -> "Select a session"
                        else -> "Opening…"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    val passwordTarget = awaitingPasswordFor
    if (passwordTarget != null) {
        PasswordDialog(
            username = passwordTarget.username,
            host = passwordTarget.host,
            onDismiss = {
                awaitingPasswordFor = null
                if (sessions.isEmpty()) onLeaveToHosts()
            },
            onConnect = { password ->
                awaitingPasswordFor = null
                isStarting = true
                try {
                    startSession(sessionManager, passwordTarget, password)
                } finally {
                    isStarting = false
                }
            },
        )
    }
}

@Composable
private fun FilingCabinetTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corner = 8.dp
    val stroke = 1.dp
    val background = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .zIndex(if (selected) 2f else 1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .focusProperties { canFocus = false },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(
                    color = background,
                    shape = RoundedCornerShape(topStart = corner, topEnd = corner),
                )
                .drawBehind {
                    val strokePx = stroke.toPx()
                    val inset = strokePx / 2f
                    val radius = corner.toPx()
                    // Open bottom for every tab — the folder baseline is the shared edge.
                    val path = Path().apply {
                        moveTo(inset, size.height)
                        lineTo(inset, radius)
                        quadraticTo(inset, inset, radius, inset)
                        lineTo(size.width - radius, inset)
                        quadraticTo(size.width - inset, inset, size.width - inset, radius)
                        lineTo(size.width - inset, size.height)
                    }
                    drawPath(
                        path = path,
                        color = outline,
                        style = Stroke(width = strokePx),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        if (selected) {
            // Punch a gap in the folder baseline so the tab opens into the content.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = stroke)
                    .height(stroke)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}

private fun startSession(
    sessionManager: SessionManager,
    pending: PendingOpen,
    password: String,
) {
    val transport = SshTransport(
        host = pending.host,
        port = pending.port,
        username = pending.username,
        password = password,
    )
    val terminal = TerminalSession(
        transport,
        /* transcriptRows */ 2000,
        object : TerminalSessionClient by NoOpTerminalSessionClient {
            override fun onSessionFinished(finishedSession: TerminalSession) {
                sessionManager.onTerminalFinished(finishedSession)
            }
        },
    )
    sessionManager.register(pending, terminal)
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
