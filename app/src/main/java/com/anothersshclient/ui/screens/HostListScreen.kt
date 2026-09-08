package com.anothersshclient.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anothersshclient.data.HostProfile
import com.anothersshclient.data.HostRepository
import com.anothersshclient.ui.HostListViewModel
import com.anothersshclient.ui.components.TypewriterBrandTitle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreen(
    viewModel: HostListViewModel,
    openSessionCount: Int = 0,
    onConnect: (HostProfile) -> Unit,
    onOpenSessions: () -> Unit = {},
) {
    val hostsState by viewModel.hosts.collectAsStateWithLifecycle()
    val hosts = hostsState.orEmpty()
    var editing by remember { mutableStateOf<HostProfile?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HostProfile?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstHostFocusRequester = remember { FocusRequester() }
    val lastHostFocusRequester = remember { FocusRequester() }
    val fabFocusRequester = remember { FocusRequester() }
    val fabInteraction = remember { MutableInteractionSource() }
    val fabFocused by fabInteraction.collectIsFocusedAsState()
    val fabShape = FloatingActionButtonDefaults.shape
    var didInitialFocus by remember { mutableStateOf(false) }
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(hostsState, windowInfo.isWindowFocused) {
        if (didInitialFocus) return@LaunchedEffect
        val loaded = hostsState ?: return@LaunchedEffect
        if (!windowInfo.isWindowFocused) return@LaunchedEffect
        if (showEditor || pendingDelete != null) return@LaunchedEffect

        // Wait until focus targets are attached; retry a few frames for cold start.
        repeat(5) {
            withFrameNanos { }
            try {
                if (loaded.isEmpty()) {
                    fabFocusRequester.requestFocus()
                } else {
                    listState.scrollToItem(1)
                    firstHostFocusRequester.requestFocus()
                }
                didInitialFocus = true
                return@LaunchedEffect
            } catch (_: IllegalStateException) {
                // FocusRequester not attached yet; try again next frame.
            }
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (event.key != Key.Escape) return@onPreviewKeyEvent false
            if (openSessionCount <= 0) return@onPreviewKeyEvent false
            onOpenSessions()
            true
        },
        topBar = {
            TopAppBar(
                title = {
                    TypewriterBrandTitle()
                },
                actions = {
                    if (openSessionCount > 0) {
                        TextButton(onClick = onOpenSessions) {
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
                    editing = null
                    showEditor = true
                },
                interactionSource = fabInteraction,
                shape = fabShape,
                containerColor = if (fabFocused) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = if (fabFocused) {
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
                    .focusRequester(fabFocusRequester)
                    .border(
                        width = if (fabFocused) 3.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = fabShape,
                    )
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.key != Key.DirectionUp) {
                            return@onPreviewKeyEvent false
                        }
                        if (hosts.isEmpty()) return@onPreviewKeyEvent false
                        scope.launch {
                            // LazyColumn index 0 is the top divider; hosts start at 1.
                            listState.scrollToItem(hosts.size)
                            withFrameNanos { }
                            if (hosts.size == 1) {
                                firstHostFocusRequester.requestFocus()
                            } else {
                                lastHostFocusRequester.requestFocus()
                            }
                        }
                        true
                    },
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
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            ) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                }
                itemsIndexed(hosts, key = { _, host -> host.id }) { index, host ->
                    val rowFocusRequester = when {
                        hosts.size == 1 -> firstHostFocusRequester
                        index == 0 -> firstHostFocusRequester
                        index == hosts.lastIndex -> lastHostFocusRequester
                        else -> null
                    }
                    HostRow(
                        host = host,
                        rowFocusRequester = rowFocusRequester,
                        onOpen = { onConnect(host) },
                        onEdit = {
                            editing = host
                            showEditor = true
                        },
                        onDelete = { pendingDelete = host },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    rowFocusRequester: FocusRequester? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (rowFocusRequester != null) {
                    Modifier.focusRequester(rowFocusRequester)
                } else {
                    Modifier
                },
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
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
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
