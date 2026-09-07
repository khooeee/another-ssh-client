package com.ssher.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssher.data.HostProfile
import com.ssher.data.HostRepository
import com.ssher.ui.HostListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreen(
    viewModel: HostListViewModel,
    onConnect: (HostProfile) -> Unit,
) {
    val hosts by viewModel.hosts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<HostProfile?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ssher", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "SSH for Daylight",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(hosts, key = { it.id }) { host ->
                    HostRow(
                        host = host,
                        onOpen = { onConnect(host) },
                        onEdit = {
                            editing = host
                            showEditor = true
                        },
                        onDelete = { viewModel.delete(host.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showEditor) {
        HostEditorDialog(
            initial = editing,
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(host.name, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append("${host.username}@${host.host}:${host.port}")
                    if (host.hasPassword) append("  ·  saved password")
                },
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

    val canSave = passwordLoaded &&
        name.isNotBlank() &&
        host.isNotBlank() &&
        username.isNotBlank() &&
        port.toIntOrNull()?.let { it in 1..65535 } == true

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
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
                            name = name.trim(),
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
