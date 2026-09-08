package com.anothersshclient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anothersshclient.data.HostProfile
import com.anothersshclient.data.HostRepository
import com.anothersshclient.ui.HostListViewModel
import com.anothersshclient.ui.RememberedHostSelection
import com.anothersshclient.ui.components.TypewriterBrandTitle
import kotlinx.coroutines.suspendCancellableCoroutine
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.coroutines.resume

private sealed interface HostListSelection {
    data object Fab : HostListSelection
    data object BackToSession : HostListSelection
    data class Host(val index: Int) : HostListSelection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreen(
    viewModel: HostListViewModel,
    openSessionCount: Int = 0,
    onConnect: (HostProfile) -> Unit,
    onOpenSessions: () -> Unit = {},
) {
    val hostsState by viewModel.hosts.collectAsStateWithLifecycle()
    val hostsFromStore = hostsState.orEmpty()
    var hosts by remember { mutableStateOf(hostsFromStore) }
    LaunchedEffect(hostsFromStore) {
        hosts = hostsFromStore
    }
    var editing by remember { mutableStateOf<HostProfile?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HostProfile?>(null) }

    val listState = rememberLazyListState()
    val screenFocusRequester = remember { FocusRequester() }
    val fabShape = FloatingActionButtonDefaults.shape
    val view = LocalView.current

    var selection by remember { mutableStateOf<HostListSelection?>(null) }
    var didInitialSelection by remember { mutableStateOf(false) }

    // Header divider is lazy index 0; host rows start at 1.
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex !in hosts.indices || toIndex !in hosts.indices) return@rememberReorderableLazyListState
        val selectedId = (selection as? HostListSelection.Host)
            ?.let { hosts.getOrNull(it.index)?.id }
        val updated = hosts.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        hosts = updated
        if (selectedId != null) {
            val newIndex = updated.indexOfFirst { it.id == selectedId }
            if (newIndex >= 0) selection = HostListSelection.Host(newIndex)
        }
        viewModel.reorder(updated)
    }

    fun rememberCurrentSelection(selected: HostListSelection?) {
        when (selected) {
            HostListSelection.Fab -> viewModel.rememberSelection(RememberedHostSelection.Fab)
            is HostListSelection.Host -> {
                val id = hosts.getOrNull(selected.index)?.id ?: return
                viewModel.rememberSelection(RememberedHostSelection.Host(id))
            }
            HostListSelection.BackToSession, null -> Unit
        }
    }

    fun selectionFromRemembered(loaded: List<HostProfile>): HostListSelection {
        if (loaded.isEmpty()) return HostListSelection.Fab
        return when (val remembered = viewModel.rememberedSelection) {
            RememberedHostSelection.Fab -> HostListSelection.Fab
            is RememberedHostSelection.Host -> {
                val index = loaded.indexOfFirst { it.id == remembered.id }
                if (index >= 0) HostListSelection.Host(index) else HostListSelection.Host(0)
            }
            null -> HostListSelection.Host(0)
        }
    }

    // App-owned selection: do not rely on Compose focus, which is flaky on cold start.
    LaunchedEffect(hostsState) {
        val loaded = hostsState ?: return@LaunchedEffect
        if (!didInitialSelection) {
            selection = selectionFromRemembered(loaded)
            didInitialSelection = true
            return@LaunchedEffect
        }
        val current = selection
        selection = when {
            loaded.isEmpty() && current is HostListSelection.Host -> HostListSelection.Fab
            current is HostListSelection.Host && current.index >= loaded.size -> {
                HostListSelection.Host(loaded.lastIndex)
            }
            current is HostListSelection.BackToSession && openSessionCount <= 0 -> {
                if (loaded.isEmpty()) HostListSelection.Fab else HostListSelection.Host(0)
            }
            current == null -> selectionFromRemembered(loaded)
            else -> current
        }
    }

    LaunchedEffect(openSessionCount) {
        if (openSessionCount <= 0 && selection is HostListSelection.BackToSession) {
            selection = if (hosts.isEmpty()) {
                HostListSelection.Fab
            } else {
                HostListSelection.Host(0)
            }
        }
    }

    LaunchedEffect(selection, hosts) {
        rememberCurrentSelection(selection)
        val selected = selection as? HostListSelection.Host ?: return@LaunchedEffect
        listState.scrollToItem(selected.index + 1)
    }

    LaunchedEffect(didInitialSelection, showEditor, pendingDelete) {
        if (!didInitialSelection || showEditor || pendingDelete != null) return@LaunchedEffect
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        awaitWindowFocus(view)
        repeat(10) {
            withFrameNanos { }
            try {
                screenFocusRequester.requestFocus()
                return@LaunchedEffect
            } catch (_: IllegalStateException) {
                // Not attached yet.
            }
        }
    }

    val dialogOpen = showEditor || pendingDelete != null
    val fabSelected = selection is HostListSelection.Fab

    fun moveSelectionDown() {
        when (val current = selection) {
            HostListSelection.BackToSession -> {
                selection = if (hosts.isNotEmpty()) {
                    HostListSelection.Host(0)
                } else {
                    HostListSelection.Fab
                }
            }
            is HostListSelection.Host -> {
                selection = if (current.index < hosts.lastIndex) {
                    HostListSelection.Host(current.index + 1)
                } else {
                    HostListSelection.Fab
                }
            }
            HostListSelection.Fab, null -> Unit
        }
    }

    fun moveSelectionUp() {
        when (val current = selection) {
            HostListSelection.Fab -> {
                if (hosts.isNotEmpty()) {
                    selection = HostListSelection.Host(hosts.lastIndex)
                } else if (openSessionCount > 0) {
                    selection = HostListSelection.BackToSession
                }
            }
            is HostListSelection.Host -> {
                selection = when {
                    current.index > 0 -> HostListSelection.Host(current.index - 1)
                    openSessionCount > 0 -> HostListSelection.BackToSession
                    else -> current
                }
            }
            HostListSelection.BackToSession, null -> Unit
        }
    }

    fun activateSelection() {
        when (val current = selection) {
            HostListSelection.Fab -> {
                editing = null
                showEditor = true
            }
            HostListSelection.BackToSession -> onOpenSessions()
            is HostListSelection.Host -> {
                hosts.getOrNull(current.index)?.let(onConnect)
            }
            null -> Unit
        }
    }

    Scaffold(
        modifier = Modifier
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (dialogOpen) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Escape -> {
                        if (openSessionCount <= 0) return@onPreviewKeyEvent false
                        onOpenSessions()
                        true
                    }
                    Key.DirectionDown -> {
                        moveSelectionDown()
                        true
                    }
                    Key.DirectionUp -> {
                        moveSelectionUp()
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        activateSelection()
                        true
                    }
                    else -> false
                }
            },
        topBar = {
            TopAppBar(
                title = {
                    TypewriterBrandTitle()
                },
                actions = {
                    if (openSessionCount > 0) {
                        val backSelected = selection is HostListSelection.BackToSession
                        val backShape = RoundedCornerShape(percent = 50)
                        TextButton(
                            onClick = {
                                selection = HostListSelection.BackToSession
                                onOpenSessions()
                            },
                            shape = backShape,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (backSelected) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    Color.Transparent
                                },
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            Text("Back to session")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selection = HostListSelection.Fab
                    editing = null
                    showEditor = true
                },
                shape = fabShape,
                containerColor = if (fabSelected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = if (fabSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.End,
                        ),
                    )
                    .focusProperties { canFocus = false }
                    .border(
                        width = if (fabSelected) 3.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = fabShape,
                    ),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add host")
            }
        },
    ) { padding ->
        if (hosts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text("No hosts yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add a server with an optional saved password (encrypted on device).",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .focusProperties { canFocus = false },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            ) {
                item(key = "host-list-top-divider") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                }
                items(hosts, key = { it.id }) { host ->
                    ReorderableItem(reorderableLazyListState, key = host.id) { isDragging ->
                        val index = hosts.indexOfFirst { it.id == host.id }
                        HostRow(
                            host = host,
                            selected = selection == HostListSelection.Host(index),
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    if (index >= 0) {
                                        selection = HostListSelection.Host(index)
                                    }
                                },
                            ),
                            onOpen = {
                                if (index >= 0) selection = HostListSelection.Host(index)
                                onConnect(host)
                            },
                            onEdit = {
                                if (index >= 0) selection = HostListSelection.Host(index)
                                editing = host
                                showEditor = true
                            },
                            onDelete = {
                                if (index >= 0) selection = HostListSelection.Host(index)
                                pendingDelete = host
                            },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { host ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete host?") },
            text = {
                Text(
                    "Remove “${host.name}” (${host.username}@${host.host}:${host.port})? " +
                        "Saved password will be deleted too. Open sessions are not closed.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(host.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    if (showEditor) {
        HostEditorDialog(
            initial = editing,
            existingHosts = hosts,
            loadPassword = { id -> viewModel.passwordFor(id) },
            onDismiss = { showEditor = false },
            onSave = { profile, password ->
                viewModel.save(profile, password)
                showEditor = false
            },
        )
    }
}

@Composable
private fun HostRow(
    host: HostProfile,
    selected: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .zIndex(if (isDragging) 1f else 0f)
            .then(dragHandleModifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false }
                .background(
                    if (selected || isDragging) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface,
                )
                .clickable(onClick = onOpen)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(host.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${host.username}@${host.host}:${host.port}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.focusProperties { canFocus = false },
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.focusProperties { canFocus = false },
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun HostEditorDialog(
    initial: HostProfile?,
    existingHosts: List<HostProfile>,
    loadPassword: suspend (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (HostProfile, String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var host by remember { mutableStateOf(initial?.host.orEmpty()) }
    var port by remember { mutableStateOf((initial?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var startupDirectory by remember { mutableStateOf(initial?.startupDirectory.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var passwordLoaded by remember { mutableStateOf(initial == null) }

    LaunchedEffect(initial?.id) {
        if (initial != null) {
            password = loadPassword(initial.id).orEmpty()
            passwordLoaded = true
        }
    }

    val trimmedName = name.trim()
    val nameTaken = trimmedName.isNotEmpty() &&
        existingHosts.any { other ->
            other.id != initial?.id && other.name.equals(trimmedName, ignoreCase = true)
        }

    val canSave = passwordLoaded &&
        trimmedName.isNotEmpty() &&
        !nameTaken &&
        host.isNotBlank() &&
        username.isNotBlank() &&
        port.toIntOrNull()?.let { it in 1..65535 } == true

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
        errorBorderColor = MaterialTheme.colorScheme.outline,
        errorLabelColor = MaterialTheme.colorScheme.onSurface,
        errorCursorColor = MaterialTheme.colorScheme.onSurface,
        errorSupportingTextColor = MaterialTheme.colorScheme.onSurface,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add host" else "Edit host") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Hostname / IP") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = startupDirectory,
                    onValueChange = { startupDirectory = it },
                    label = { Text("Start directory") },
                    singleLine = true,
                    colors = fieldColors,
                    supportingText = {
                        Text("Optional. cd here after connect (e.g. ~/code, \$HOME/code).")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                    supportingText = {
                        Text("Stored encrypted with Android Keystore. Leave blank to clear.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        HostProfile(
                            id = initial?.id ?: HostRepository.newId(),
                            name = trimmedName,
                            host = host.trim(),
                            port = port.toInt(),
                            username = username.trim(),
                            startupDirectory = startupDirectory.trim().ifEmpty { null },
                        ),
                        password,
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

private suspend fun awaitWindowFocus(view: android.view.View) {
    if (view.hasWindowFocus()) return
    suspendCancellableCoroutine { cont ->
        val listener = object : android.view.ViewTreeObserver.OnWindowFocusChangeListener {
            override fun onWindowFocusChanged(hasFocus: Boolean) {
                if (!hasFocus) return
                view.viewTreeObserver.removeOnWindowFocusChangeListener(this)
                if (cont.isActive) cont.resume(Unit)
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        cont.invokeOnCancellation {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
        }
        if (view.hasWindowFocus()) {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
