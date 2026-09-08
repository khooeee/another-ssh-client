package com.anothersshclient.data

data class HostProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    /** Shell command typed after connect (e.g. `cd ~/code && tmux a`). Blank/null = none. */
    val startupCommand: String? = null,
    /** True when a Keystore-encrypted password exists for this host. Never holds the secret itself. */
    val hasPassword: Boolean = false,
)
