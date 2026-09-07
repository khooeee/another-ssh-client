package com.ssher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssher.ui.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    var input by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    val inputFocus = remember { FocusRequester() }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    LaunchedEffect(hostId) {
        resolvingPassword = true
        val saved = loadPassword(hostId)
        if (!saved.isNullOrEmpty()) {
            passwordPromptOpen = false
            viewModel.connect(host, port, username, saved)
        } else {
            passwordPromptOpen = true
        }
        resolvingPassword = false
    }

    LaunchedEffect(state.output) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    LaunchedEffect(state.connected) {
        if (state.connected) {
            inputFocus.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "$username@$host:$port",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (state.connected || state.connecting) {
                                viewModel.disconnect()
                            } else {
                                passwordPromptOpen = true
                            }
                        },
                    ) {
                        Text(
                            when {
                                state.connecting -> "Cancel"
                                state.connected -> "Disconnect"
                                else -> "Connect"
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(12.dp),
            ) {
                Text(
                    text = buildString {
                        when {
                            resolvingPassword -> append("Loading credentials…\n")
                            else -> {
                                append(state.output)
                                state.error?.let { append("\nError: $it\n") }
                            }
                        }
                    }.ifBlank { "Waiting to connect…" },
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "> ",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    enabled = state.connected,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (input.isNotEmpty() || state.connected) {
                                viewModel.send(input + "\n")
                                input = ""
                            }
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(inputFocus)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when {
                                event.key == Key.Enter -> {
                                    viewModel.send(input + "\n")
                                    input = ""
                                    true
                                }
                                event.isCtrlPressed && event.key == Key.C -> {
                                    viewModel.sendRaw("\u0003")
                                    true
                                }
                                event.isCtrlPressed && event.key == Key.D -> {
                                    viewModel.sendRaw("\u0004")
                                    true
                                }
                                event.isCtrlPressed && event.key == Key.L -> {
                                    true
                                }
                                else -> false
                            }
                        },
                )
                TextButton(
                    onClick = {
                        viewModel.send(input + "\n")
                        input = ""
                    },
                    enabled = state.connected,
                ) {
                    Text("Send")
                }
            }
        }
    }

    if (passwordPromptOpen && !state.connected && !state.connecting && !resolvingPassword) {
        PasswordDialog(
            username = username,
            host = host,
            onDismiss = {
                passwordPromptOpen = false
                if (!state.connected) onBack()
            },
            onConnect = { password ->
                passwordPromptOpen = false
                viewModel.connect(host, port, username, password)
            },
        )
    }
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
