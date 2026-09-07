package com.ssher.data

data class HostProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
)
