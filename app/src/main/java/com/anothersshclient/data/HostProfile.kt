package com.anothersshclient.data

data class HostProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    /** Absolute or ~ path to cd into after connect. Blank/null = login default. */
    val startupDirectory: String? = null,
    /** True when a Keystore-encrypted password exists for this host. Never holds the secret itself. */
    val hasPassword: Boolean = false,
)
