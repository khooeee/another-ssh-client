package com.ssher.data

data class HostProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    /** True when a Keystore-encrypted password exists for this host. Never holds the secret itself. */
    val hasPassword: Boolean = false,
)
