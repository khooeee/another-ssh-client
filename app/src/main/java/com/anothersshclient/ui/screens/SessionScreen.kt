package com.anothersshclient.ui.screens

import android.graphics.Typeface
import android.os.Build
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anothersshclient.session.OpenSession
import com.anothersshclient.session.PendingOpen
import com.anothersshclient.session.SessionManager
import com.anothersshclient.ssh.SshTransport
import com.anothersshclient.terminal.AppTerminalClients
import com.anothersshclient.terminal.ExtraKeysState
import com.anothersshclient.terminal.NoOpTerminalSessionClient
import com.anothersshclient.ui.components.ExtraKeysBar
import com.anothersshclient.ui.theme.LocalTerminalTheme
import com.anothersshclient.ui.theme.TerminalTheme
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.util.Properties
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val SessionChromePaddingHorizontal: Dp = 16.dp
private val SessionChromePaddingVertical: Dp = 12.dp

@OptIn(ExperimentalFoundationApi::class)
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
    val terminalTheme = LocalTerminalTheme.current
    val terminalThemeLatest = rememberUpdatedState(terminalTheme)

    var awaitingPasswordFor by remember { mutableStateOf<PendingOpen?>(null) }
    var isStarting by remember { mutableStateOf(false) }
    var hadLiveSession by remember { mutableStateOf(false) }
    var renamingSession by remember { mutableStateOf<OpenSession?>(null) }
    val extraKeys = remember { ExtraKeysState() }
    var terminalViewRef by remember { mutableStateOf<TerminalView?>(null) }
    val renamingLatest = rememberUpdatedState(renamingSession)

    val onLeaveLatest = rememberUpdatedState(onLeaveToHosts)

    fun leaveToNewSession() {
        sessionManager.clearPending()
        awaitingPasswordFor = null
        isStarting = false
        onLeaveLatest.value()
    }

    BackHandler {
        leaveToNewSession()
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
                    startSession(sessionManager, pending, saved, terminalThemeLatest.value)
                }
            } finally {
                isStarting = false
            }
        }
    }

    LaunchedEffect(terminalTheme, sessions) {
        for (open in sessions) {
            val session = open.terminalSession ?: continue
            applyTerminalScheme(session, terminalTheme)
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (!event.isCtrlPressed) return@onPreviewKeyEvent false
            when {
                event.key == Key.Tab -> {
                    sessionManager.selectAdjacent(forward = !event.isShiftPressed)
                    true
                }
                event.key == Key.N && event.isShiftPressed -> {
                    leaveToNewSession()
                    true
                }
                event.key == Key.R && event.isShiftPressed -> {
                    active?.let { renamingSession = it }
                    true
                }
                else -> false
            }
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
                            modifier = if (active != null) {
                                Modifier.combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                    onDoubleClick = { renamingSession = active },
                                )
                            } else {
                                Modifier
                            },
                        )
                        if (active != null) {
                            Text(
                                "${active.username}@${active.host}:${active.port}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    TextButton(
                        onClick = { leaveToNewSession() },
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
                    val outline = MaterialTheme.colorScheme.outline
                    val edgeStroke = with(LocalDensity.current) { 1.dp.toPx() }
                    var stripCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    var selectedTabGap by remember(activeId) { mutableStateOf<Rect?>(null) }
                    val tabListState = rememberLazyListState()
                    val reorderableTabState = rememberReorderableLazyListState(tabListState) { from, to ->
                        sessionManager.reorder(from.index, to.index)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { stripCoords = it },
                    ) {
                        LazyRow(
                            state = tabListState,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                            contentPadding = PaddingValues(horizontal = SessionChromePaddingHorizontal),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            items(sessions, key = { it.id }) { session ->
                                ReorderableItem(reorderableTabState, key = session.id) { isDragging ->
                                    val index = sessions.indexOfFirst { it.id == session.id }
                                    val selected = session.id == activeId
                                    FilingCabinetTab(
                                        label = sessionManager.label(session),
                                        selected = selected,
                                        isDragging = isDragging,
                                        // Share side walls: only the first tab draws a leading edge.
                                        drawLeadingEdge = index == 0,
                                        onClick = { sessionManager.setActive(session.id) },
                                        onRename = { renamingSession = session },
                                        dragHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = {
                                                sessionManager.setActive(session.id)
                                            },
                                        ),
                                        modifier = Modifier
                                            .onGloballyPositioned { tabCoords ->
                                                if (!selected) return@onGloballyPositioned
                                                val strip = stripCoords ?: return@onGloballyPositioned
                                                if (strip.isAttached && tabCoords.isAttached) {
                                                    selectedTabGap = strip.localBoundingBoxOf(tabCoords)
                                                }
                                            },
                                    )
                                }
                            }
                        }
                        Canvas(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(1.dp),
                        ) {
                            val y = size.height / 2f
                            val gap = selectedTabGap
                            if (gap == null) {
                                drawLine(
                                    color = outline,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = edgeStroke,
                                )
                            } else {
                                if (gap.left > 0f) {
                                    drawLine(
                                        color = outline,
                                        start = Offset(0f, y),
                                        end = Offset(gap.left, y),
                                        strokeWidth = edgeStroke,
                                    )
                                }
                                if (gap.right < size.width) {
                                    drawLine(
                                        color = outline,
                                        start = Offset(gap.right, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = edgeStroke,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        val terminal = active?.terminalSession
        if (terminal != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            ) {
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
                                // Keyboard focus (e.g. Enter from host list) otherwise draws Android's
                                // default focus highlight as a dark wash over the paper terminal.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    defaultFocusHighlightEnabled = false
                                }
                            }
                            val baseClients = AppTerminalClients(
                                context = ctx,
                                terminalView = view,
                                onFinished = { finished -> sessionManager.onTerminalFinished(finished) },
                                extraKeys = extraKeys,
                            )
                            val viewClient = object : TerminalViewClient by baseClients {
                                override fun onEmulatorSet() {
                                    applyTerminalScheme(terminal, terminalThemeLatest.value)
                                    baseClients.onEmulatorSet()
                                    if (renamingLatest.value == null) {
                                        view.requestFocus()
                                    }
                                }

                                override fun onKeyDown(
                                    keyCode: Int,
                                    e: KeyEvent,
                                    session: TerminalSession,
                                ): Boolean {
                                    val ctrl = e.isCtrlPressed || baseClients.readControlKey()
                                    val shift = e.isShiftPressed || baseClients.readShiftKey()
                                    if (!ctrl) {
                                        return baseClients.onKeyDown(keyCode, e, session)
                                    }
                                    when (keyCode) {
                                        KeyEvent.KEYCODE_TAB -> {
                                            sessionManager.selectAdjacent(forward = !shift)
                                            extraKeys.consumeOneShot()
                                            return true
                                        }
                                        KeyEvent.KEYCODE_N -> {
                                            if (shift) {
                                                leaveToNewSession()
                                                extraKeys.consumeOneShot()
                                                return true
                                            }
                                        }
                                        KeyEvent.KEYCODE_R -> {
                                            if (shift) {
                                                sessionManager.activeSession()?.let { renamingSession = it }
                                                extraKeys.consumeOneShot()
                                                return true
                                            }
                                        }
                                    }
                                    return baseClients.onKeyDown(keyCode, e, session)
                                }
                            }
                            view.setTerminalViewClient(viewClient)
                            terminal.updateTerminalSessionClient(baseClients)
                            view.attachSession(terminal)
                            terminalViewRef = view
                            view
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(
                                start = SessionChromePaddingHorizontal,
                                end = SessionChromePaddingHorizontal,
                                top = SessionChromePaddingVertical,
                            ),
                        update = { view ->
                            terminalViewRef = view
                            // Don't steal focus from rename dialog / other chrome.
                            if (renamingLatest.value == null && !view.hasFocus()) {
                                view.requestFocus()
                            }
                        },
                    )
                }
                ExtraKeysBar(
                    state = extraKeys,
                    terminalView = terminalViewRef,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
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
                    startSession(sessionManager, passwordTarget, password, terminalThemeLatest.value)
                } finally {
                    isStarting = false
                }
            },
        )
    }

    renamingSession?.let { session ->
        RenameSessionDialog(
            initialName = session.title,
            otherTitles = sessions
                .filter { it.id != session.id }
                .map { it.title },
            onDismiss = { renamingSession = null },
            onRename = { newName ->
                sessionManager.rename(session.id, newName)
                renamingSession = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilingCabinetTab(
    label: String,
    selected: Boolean,
    isDragging: Boolean,
    drawLeadingEdge: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val stroke = 1.dp
    val background = if (selected || isDragging) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .zIndex(when {
                isDragging -> 3f
                selected -> 2f
                else -> 1f
            })
            .then(dragHandleModifier)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                // Long-press is drag-to-reorder (same as host list); double-tap renames.
                onDoubleClick = onRename,
            )
            .focusProperties { canFocus = false },
    ) {
        // Keep fill above the baseline so inactive tabs don't paint over the edge.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = stroke)
                .background(color = background),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .drawBehind {
                    val strokePx = stroke.toPx()
                    val inset = strokePx / 2f
                    val path = Path()
                    if (drawLeadingEdge) {
                        path.moveTo(inset, size.height)
                        path.lineTo(inset, inset)
                        path.lineTo(size.width - inset, inset)
                    } else {
                        // Previous tab owns the shared wall; start along the top edge.
                        path.moveTo(0f, inset)
                        path.lineTo(size.width - inset, inset)
                    }
                    path.lineTo(size.width - inset, size.height)
                    drawPath(
                        path = path,
                        color = outline,
                        style = Stroke(width = strokePx),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun RenameSessionDialog(
    initialName: String,
    otherTitles: List<String>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember(initialName) {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }
    val trimmed = name.text.trim()
    val nameTaken = trimmed.isNotEmpty() &&
        !trimmed.equals(initialName, ignoreCase = true) &&
        otherTitles.any { it.equals(trimmed, ignoreCase = true) }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
        errorBorderColor = MaterialTheme.colorScheme.outline,
        errorLabelColor = MaterialTheme.colorScheme.onSurface,
        errorCursorColor = MaterialTheme.colorScheme.onSurface,
        errorSupportingTextColor = MaterialTheme.colorScheme.onSurface,
    )

    val canRename = trimmed.isNotEmpty() && !nameTaken

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename session") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                isError = nameTaken,
                supportingText = if (nameTaken) {
                    { Text("That name already exists. Choose another one.") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canRename) onRename(name.text)
                    },
                ),
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.key != Key.Enter && event.key != Key.NumPadEnter) {
                            return@onPreviewKeyEvent false
                        }
                        if (canRename) onRename(name.text)
                        true
                    },
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(name.text) },
                enabled = canRename,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

private fun startSession(
    sessionManager: SessionManager,
    pending: PendingOpen,
    password: String,
    terminalTheme: TerminalTheme,
) {
    val transport = SshTransport(
        host = pending.host,
        port = pending.port,
        username = pending.username,
        password = password,
        terminalTheme = terminalTheme,
        startupDirectory = pending.startupDirectory,
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

private fun applyTerminalScheme(session: TerminalSession, theme: TerminalTheme) {
    val emulator = session.emulator ?: return
    val props = Properties().apply {
        when (theme) {
            TerminalTheme.Light -> {
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
            TerminalTheme.Dark -> {
                setProperty("foreground", "#FFFFFF")
                setProperty("background", "#000000")
                setProperty("cursor", "#FFFFFF")
                setProperty("color0", "#000000")
                setProperty("color1", "#888888")
                setProperty("color2", "#999999")
                setProperty("color3", "#AAAAAA")
                setProperty("color4", "#BBBBBB")
                setProperty("color5", "#CCCCCC")
                setProperty("color6", "#DDDDDD")
                setProperty("color7", "#EEEEEE")
                setProperty("color8", "#666666")
                setProperty("color9", "#999999")
                setProperty("color10", "#AAAAAA")
                setProperty("color11", "#BBBBBB")
                setProperty("color12", "#CCCCCC")
                setProperty("color13", "#DDDDDD")
                setProperty("color14", "#EEEEEE")
                setProperty("color15", "#FFFFFF")
            }
        }
    }
    TerminalColors.COLOR_SCHEME.updateWith(props)
    emulator.mColors.reset()
    session.onColorsChanged()
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
